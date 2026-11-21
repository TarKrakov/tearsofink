#!/usr/bin/env python3
"""D-129: verify the real author's active vertical-cloth standard and bare companion."""
import base64
import json
import re
import struct
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
active = json.loads((ROOT/"blockbench/battlefielf_regular_standart1.bbmodel").read_text(encoding="utf-8"))
bare = json.loads((ROOT/"blockbench/battlefield_regular_standart_withoutbanner1.bbmodel").read_text(encoding="utf-8"))
assert len(active["elements"]) == 31 and len(bare["elements"]) == 26
assert active["textures"][0]["uv_width"] == active["textures"][0]["uv_height"] == 128
assert len(active["groups"]) == 7
names = [g["name"] for g in active["groups"]]
assert names == ["pole","flag_root","flag1","flag2","flag3","flag4","flag_main"]
assert [next(g for g in active["groups"] if g["name"] == n)["origin"] for n in names[1:6]] == [
    [0,72,-2.5],[0,62,-2.5],[0,51,-2.5],[0,40,-2.5],[0,33,-2.5]]
u2i = {e["uuid"]:i for i,e in enumerate(active["elements"])}
ordered = {}
def visit(entries, group=None):
    for q in entries:
        if isinstance(q,str):
            ordered.setdefault(group,[]).append(u2i[q])
        else:
            g = next(g for g in active["groups"] if g["uuid"] == q["uuid"])
            visit(q["children"],g["name"])
visit(active["outliner"])
assert len(ordered["pole"]) == 26
assert [ordered[x] for x in names[1:6]] == [[24],[25],[26],[27],[28]]
geo=(ROOT/"src/main/java/com/tarkrakov/tearsofink/world/battlefield/BattlefieldBannerGeometry.java").read_text(encoding="utf-8")
section=geo.split("public static final Cuboid[] PARTS = {",1)[1].split("\n    };",1)[0]
blocks=re.findall(r"new Cuboid\((.*?)\)",section,re.S)
assert len(blocks)==31
for i,(e,b) in enumerate(zip(active["elements"],blocks)):
    actual=[float(x) for x in re.findall(r"(-?\d+(?:\.\d+)?)f",b)]
    expected=list(e["from"]+e["to"])
    for face in ("north","east","south","west","up","down"):
        expected.extend(e["faces"][face]["uv"])
    assert actual==expected, f"Cube {i} differs from author original"
# D-131: the second authored regular standard is intentionally flagless.
# It shares EXACTLY the 26 pole shapes/UVs with the main authored standard.
# No retexture, repack or invented stand-in art is permitted.
assert len(bare["elements"]) == 26
assert bare["textures"][0]["uv_width"] == 128
pole = [active["elements"][i] for i in ordered["pole"]]
assert len(pole) == 26
def cube_fingerprint(e):
    return (tuple(e["from"]),tuple(e["to"]),tuple(tuple(e["faces"][f]["uv"])
            for f in ("north","east","south","west","up","down")))
assert sorted(map(cube_fingerprint,bare["elements"])) == sorted(map(cube_fingerprint,pole)), (
    "Bare regular standard differs from existing authored pole, cannot share geometry")
bare_png=(ROOT/"blockbench/regular_standart_1_withoutbanner_texture.png").read_bytes()
bare_pack=(ROOT/"src/main/resources/assets/tears-of-ink/textures/block/battlefield_regular_standart_withoutbanner1.png").read_bytes()
assert bare_png == bare_pack and bare_png.startswith(b"\x89PNG\r\n\x1a\n")
assert struct.unpack(">II",bare_png[16:24]) == (256,256)
assert "bareFootprint(Direction facing)" in geo
assert "BARE_FOOTPRINTS" in geo
assert "CLOTH_PARTS = {24, 25, 26, 27, 28}" in geo
assert "TOP_HINGES = {{0f, 72f, -2.5f}" in geo
assert "256; sample++" in geo and "joint >= 0" in geo
# D-130: check the real new front-biased wind curve against the *actual* author
# model, not just the textual presence of an amplitude constant. Sample the same
# nested top-hinge matrices as client/server through a full wind period.
from math import cos, sin, pi
def wind_array(name):
    return [float(v) for v in re.findall(r"(-?\d+(?:\.\d+)?)f", geo.split(name + " = {",1)[1].split("}",1)[0])]
base = wind_array("WIND_BASE_LEAN")
outward = wind_array("WIND_OUTWARD_RANGE")
assert len(base) == len(outward) == 5
assert all(0 < lean < amp for lean,amp in zip(base,outward))
assert outward[0] > 0.1 and sum(outward) > 0.4, "Forward billow is not strong enough"
assert "Math.sin(phase - joint * WIND_PHASE_STEP)" in geo
assert "windAngle(joint, phase)" in geo
step = float(re.search(r"WIND_PHASE_STEP = ([0-9.]+)f", geo).group(1))
hinges = [next(g["origin"] for g in active["groups"] if g["name"] == n)
          for n in ("flag_root","flag1","flag2","flag3","flag4")]
farthest_back = float("-inf")
for sample in range(1024):
    phase = 2 * pi * sample / 1024
    for segment, part in enumerate(active["elements"][24:29]):
        for y in (part["from"][1], part["to"][1]):
            for z in (part["from"][2], part["to"][2]):
                yy, zz = y, z
                for joint in range(segment, -1, -1):
                    w = 0.5 * (1 + sin(phase - joint * step))
                    angle = base[joint] + outward[joint] * w * w
                    py, pz = hinges[joint][1], hinges[joint][2]
                    dy, dz = yy - py, zz - pz
                    yy, zz = py + dy * cos(angle) - dz * sin(angle), pz + dy * sin(angle) + dz * cos(angle)
                farthest_back = max(farthest_back, zz)
shaft_back = active["elements"][0]["from"][2]  # pole begins at Z=-1.25
assert farthest_back < shaft_back - 0.25, (
    f"Cloth crosses its pole: closest Z={farthest_back}, pole starts Z={shaft_back}")
print(f"D-130: 1024 sampled full-cloth wind poses avoid pole (max Z={farthest_back:.2f}px, pole Z={shaft_back:.2f}px).")
png=(ROOT/"blockbench/regular_standart_1_texture.png").read_bytes()
packed=(ROOT/"src/main/resources/assets/tears-of-ink/textures/block/battlefield_regular_standart1.png").read_bytes()
assert png==packed and png.startswith(b"\x89PNG\r\n\x1a\n")
assert struct.unpack(">II",png[16:24])==(256,256)
# Blockbench keeps its own embedded texture snapshot; the separately exported
# authored PNG is the runtime source of truth. They can differ without art edits.
embedded = base64.b64decode(active["textures"][0]["source"].split(",",1)[1])
assert embedded.startswith(b"\x89PNG\r\n\x1a\n")
assert struct.unpack(">II",embedded[16:24]) == (256,256)
if embedded != png:
    print("Note: embedded BBModel PNG differs from the author's separate PNG; using the exact separate PNG.")
renderer=(ROOT/"src/client/java/com/tarkrakov/tearsofink/client/battlefield/BattlefieldBannerRenderer.java").read_text(encoding="utf-8")
assert "battlefield_regular_standart1.png" in renderer and "getGameTime()" in renderer
assert "BattlefieldBannerGeometry.POLE_PARTS" in renderer
assert "BattlefieldBannerGeometry.CLOTH_PARTS" in renderer
assert "Axis.XP.rotation(angle)" in renderer
assert "BattlefieldBannerGeometry.TOP_HINGES[i]" in renderer
assert "BattlefieldBannerGeometry.windAngle(i, phase)" in renderer
assert "WIND_AMPLITUDES" not in renderer
assert "first_flag_texture" not in renderer and "Axis.YP.rotation(angle)" not in renderer
assert "battlefield_regular_standart_withoutbanner1.png" in renderer
assert "BattlefieldBannerBlock.BARE" in renderer and "if (bare)" in renderer
place=(ROOT/"src/main/java/com/tarkrakov/tearsofink/command/BattlefieldDecorCommands.java").read_text(encoding="utf-8")
zone=(ROOT/"src/main/java/com/tarkrakov/tearsofink/command/BattlefieldZonePreview.java").read_text(encoding="utf-8")
assert "BattlefieldBannerGeometry.footprint(facing)" in place and "BattlefieldBannerGeometry.footprint(facing)" in zone
assert "for (int radius = 0; radius <= 4; radius++)" in place
assert "literal(\"regular_standard\")" in place and "literal(\"regular_bare\")" in place
assert "bare ? BattlefieldBannerGeometry.bareFootprint(facing)" in place
assert "bare ? BattlefieldBannerGeometry.bareFootprint(facing)" in zone
assert "scene.ordinal() >= 20 ? 2 : 1" in zone
assert 'Commands.literal("preview")' in zone
assert 'Commands.literal("place")' in zone
assert 'Commands.literal("list")' in zone
assert "isFaceSturdy(level, base.below(), Direction.UP)" in place
assert "getCollisionShape(level, pos).isEmpty()" in place
print("D-129: all 31 source cubes/UVs, 5 true top-hinges, exact author PNG and sweep-safe placement verified.")
