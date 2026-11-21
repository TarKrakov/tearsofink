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
assert "CLOTH_PARTS = {24, 25, 26, 27, 28}" in geo
assert "TOP_HINGES = {{0f, 72f, -2.5f}" in geo
assert "256; sample++" in geo and "joint >= 0" in geo
assert "WIND_AMPLITUDES = {0.03f, 0.043f, 0.055f, 0.073f, 0.091f}" in geo
png=(ROOT/"blockbench/regular_standart_1_texture.png").read_bytes()
packed=(ROOT/"src/main/resources/assets/tears-of-ink/textures/block/battlefield_regular_standart1.png").read_bytes()
assert png==packed and png.startswith(b"\x89PNG\r\n\x1a\n")
assert struct.unpack(">II",png[16:24])==(256,256)
assert base64.b64decode(active["textures"][0]["source"].split(",",1)[1])==png
renderer=(ROOT/"src/client/java/com/tarkrakov/tearsofink/client/battlefield/BattlefieldBannerRenderer.java").read_text(encoding="utf-8")
assert "battlefield_regular_standart1.png" in renderer and "getGameTime()" in renderer
assert "BattlefieldBannerGeometry.POLE_PARTS" in renderer
assert "BattlefieldBannerGeometry.CLOTH_PARTS" in renderer
assert "Axis.XP.rotation(angle)" in renderer
assert "BattlefieldBannerGeometry.TOP_HINGES[i]" in renderer
assert "first_flag_texture" not in renderer and "Axis.YP.rotation(angle)" not in renderer
place=(ROOT/"src/main/java/com/tarkrakov/tearsofink/command/BattlefieldDecorCommands.java").read_text(encoding="utf-8")
zone=(ROOT/"src/main/java/com/tarkrakov/tearsofink/command/BattlefieldZonePreview.java").read_text(encoding="utf-8")
assert "BattlefieldBannerGeometry.footprint(facing)" in place and "BattlefieldBannerGeometry.footprint(facing)" in zone
assert "for (int radius = 0; radius <= 4; radius++)" in place
assert "isFaceSturdy(level, base.below(), Direction.UP)" in place
assert "getCollisionShape(level, pos).isEmpty()" in place
print("D-129: all 31 source cubes/UVs, 5 true top-hinges, exact author PNG and sweep-safe placement verified.")
