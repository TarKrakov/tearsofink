#!/usr/bin/env python3
"""D-128: validate active regular-army standard; historical filename retained by CI."""
import base64
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
model = json.loads((ROOT / "blockbench/battlefield_regular_standart_withoutbanner1.bbmodel").read_text(encoding="utf-8"))
assert model["meta"]["model_format"] == "free"
assert len(model["elements"]) == 26 and len(model["groups"]) == 1 and model["groups"][0]["name"] == "pole"
assert model["textures"][0]["uv_width"] == model["textures"][0]["uv_height"] == 128
assert model["outliner"][0]["children"] == [e["uuid"] for e in model["elements"]]
geo = (ROOT / "src/main/java/com/tarkrakov/tearsofink/world/battlefield/BattlefieldBannerGeometry.java").read_text(encoding="utf-8")
section = geo.split("public static final Cuboid[] PARTS = {", 1)[1].split("\n    };", 1)[0]
blocks = re.findall(r"new Cuboid\((.*?)\)", section, re.S)
assert len(blocks) == 26
for i, (cube, block) in enumerate(zip(model["elements"], blocks)):
    actual = [float(x) for x in re.findall(r"(-?\d+(?:\.\d+)?)f", block)]
    expected = list(cube["from"] + cube["to"])
    for face in ("north", "east", "south", "west", "up", "down"):
        expected.extend(cube["faces"][face]["uv"])
    assert actual == expected, f"Author geometry/UV drift in cube {i}"
png = (ROOT / "blockbench/regular_standart_1_withoutbanner_texture.png").read_bytes()
packed = (ROOT / "src/main/resources/assets/tears-of-ink/textures/block/battlefield_regular_standart_withoutbanner1.png").read_bytes()
assert png == packed and png.startswith(b"\x89PNG\r\n\x1a\n")
assert struct.unpack(">II", png[16:24]) == (256, 256)
assert base64.b64decode(model["textures"][0]["source"].split(",", 1)[1]) == png
render = (ROOT / "src/client/java/com/tarkrakov/tearsofink/client/battlefield/BattlefieldBannerRenderer.java").read_text(encoding="utf-8")
assert "BattlefieldBannerGeometry.PARTS" in render
assert "BattlefieldBannerGeometry.UV_SIZE" in render
assert "first_flag_texture" not in render and "getGameTime()" not in render
preview = (ROOT / "src/main/java/com/tarkrakov/tearsofink/command/BattlefieldDecorCommands.java").read_text(encoding="utf-8")
zone = (ROOT / "src/main/java/com/tarkrakov/tearsofink/command/BattlefieldZonePreview.java").read_text(encoding="utf-8")
assert "BattlefieldBannerGeometry.footprint(facing)" in preview and "BattlefieldBannerGeometry.footprint(facing)" in zone
assert "for (int radius = 0; radius <= 4; radius++)" in preview
assert "isFaceSturdy(level, base.below(), Direction.UP)" in preview
assert "getCollisionShape(level, pos).isEmpty()" in preview
assert "case EAST" in geo and "case NORTH" in geo and "b.y1() / 16f" in geo
print("D-128: exact 26 cuboids + UV atlas + author PNG; 4-facing shared footprint; no fake cloth animation.")
