#!/usr/bin/env python3
"""Reject banner renderer / texture drift from the original author Blockbench art."""
from __future__ import annotations

import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BB = ROOT / "blockbench/first_flag.bbmodel"
PNG = ROOT / "blockbench/first_flag_texture.png"
PACK_PNG = ROOT / "src/main/resources/assets/tears-of-ink/textures/block/first_flag_texture.png"
RENDER = ROOT / "src/client/java/com/tarkrakov/tearsofink/client/battlefield/BattlefieldBannerRenderer.java"
FACES = ("north", "east", "south", "west", "up", "down")

doc = json.loads(BB.read_text(encoding="utf-8"))
assert doc["meta"]["model_format"] == "free", "Not the original Generic Model"
assert doc["resolution"] == {"width": 64, "height": 64}, "UV logical size has changed"
groups = {g["uuid"]: g["name"] for g in doc["groups"]}
elements = {c["uuid"]: c for c in doc["elements"]}
parts: list[tuple[str, dict]] = []


def descend(children, active=""):
    for entry in children:
        if isinstance(entry, str):
            parts.append((active, elements[entry]))
        else:
            descend(entry["children"], groups[entry["uuid"]])


descend(doc["outliner"])
names = [name for name, _ in parts]
assert names == ["pole", "cloth_root", "cloth1", "cloth2", "cloth3", "cloth4", "cloth5"], names
text = RENDER.read_text(encoding="utf-8")
table = text.split("private static final Box[] PARTS = {", 1)[1].split("};", 1)[0]
blocks = re.findall(r"new Box\((.*?)\)", table, flags=re.S)
assert len(blocks) == len(parts), f"Expected 7 boxes; found {len(blocks)}"

for i, ((name, cube), block) in enumerate(zip(parts, blocks)):
    actual = [float(s) for s in re.findall(r"(-?\d+(?:\.\d+)?)f", block)]
    expected = list(cube["from"] + cube["to"])
    for face in FACES:
        expected.extend(cube["faces"][face]["uv"])
    assert len(actual) == 30, f"{name}: wrong box/UV array length ({len(actual)})"
    assert actual == expected, f"{name} (part {i}): source BB cuboid/UV mismatch"

seams = [float(part["to"][0]) for _, part in parts[1:]]
joint_data = text.split("private static final float[] JOINT_X = {", 1)[1].split("}", 1)[0]
joints = [float(x) for x in re.findall(r"(-?\d+(?:\.\d+)?)f", joint_data)]
assert seams == joints, f"Cloth seam pivots are wrong: source {seams}, renderer {joints}"

source = PNG.read_bytes()
assert source.startswith(b"\x89PNG\r\n\x1a\n")
assert struct.unpack(">II", source[16:24]) == (128, 128), "Authored texture size changed"
assert PACK_PNG.read_bytes() == source, "Author texture was altered in the runtime copy"
assert "BlockEntityRenderer" in text and "getGameTime()" in text
print("First battlefield banner: seven authored cubes, six correct pivots, UVs and PNG match.")


# Prevent regression to the user's rejected all-air 7x7x5 placement rule.
placement = (ROOT / "src/main/java/com/tarkrakov/tearsofink/command/BattlefieldDecorCommands.java").read_text(encoding="utf-8")
assert 'for (int radius = 0; radius <= 4; radius++)' in placement, "Banner preview must search nearby terrain"
assert "getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES" in placement, "Missing terrain height sampling"
assert "getBlockState(base).isAir()" in placement, "Unsafe replacement at banner anchor"
assert "isFaceSturdy(level, base.below(), Direction.UP)" in placement, "Banner needs solid support"
assert "hasSolidObstruction" in placement and "getCollisionShape(level, pos).isEmpty()" in placement, "No actual model footprint checks"
assert "getCounterClockWise()" in placement, "No rotated cloth footprint check"
assert placement.count("level.setBlock(base, state, Block.UPDATE_CLIENTS)") == 1, "Unexpected writes in banner preview"
assert "7x7 блоков" not in placement, "Old whole-area rejection returned"
print("Battlefield banner placement: terrain-aware candidate search, no bulk clearing.")

# First banner wind parameters: restrained increase without touching art.
# Keep the pole fixed, six monotonically increasing cloth amplitudes and a
# smooth 3–4 second wave. No new per-block/server tickers are necessary.
wind = re.search(r"private static final float\[\] AMPLITUDE = \{([^}]+)\}", text)
assert wind is not None, "Missing banner wind amplitudes"
amplitudes = [float(m) for m in re.findall(r"([0-9]+(?:\.[0-9]+)?)f", wind.group(1))]
assert len(amplitudes) == 6
assert all(0.0 < amplitudes[i] < amplitudes[i + 1] for i in range(5))
assert amplitudes[0] <= 0.025 and amplitudes[-1] <= 0.065, "Wave is too strong for the accepted pilot"
cycle = re.search(r"Math\.sin\(time \* \(Math\.PI \* 2d / ([0-9]+)d\)", text)
assert cycle is not None and 65 <= int(cycle.group(1)) <= 80, "Cloth cycle outside gentle range"
assert "drawBox(PARTS[0], stack.last(), consumer, packedLight, packedOverlay);" in text
print("Banner wave: six restrained increasing amplitudes, no pole animation, gentle cycle.")
