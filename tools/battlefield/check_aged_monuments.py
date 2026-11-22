#!/usr/bin/env python3
"""D-134: verify aged battlefield-only resources and two 8-pose giant sword archetypes."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / "src/main/resources/assets/tears-of-ink"
MODELS = (
    ("pale_longsword", "pale_longsword"),
    ("pale_linear_great", "pale_linear_greatsword"),
    ("blood_talvar", "blood_talvar"),
    ("blood_dussak", "blood_dussak"),
    ("spire_torturer_greatsword", "spire_torturer_greatsword"),
    ("spire_roaring_axe", "spire_roaring_axe"),
    ("spire_shadow_scimitar", "spire_shadow_scimitar"),
    ("spire_rapid_knife", "spire_rapid_knife"),
)
SHIELDS = ("ram", "repel", "mercy", "radiant")
PATROL = ("spear", "shield", "crossbow")
PALE = ("pale_spearman", "pale_shieldbearer", "pale_crossbowman")

def read(rel):
    return json.loads((A / rel).read_text(encoding="utf-8"))

source = (ROOT / "tools/battlefield/GenerateAgedTextures.java").read_text(encoding="utf-8")
states = read("blockstates/battlefield_weapon_mesh.json")["variants"]
for i, (name, texture) in enumerate(MODELS):
    aged = read(f"models/block/battlefield_weapon_{name}_aged.json")
    item = read(f"models/item/{name}.json")
    assert aged["parent"] == f"tears-of-ink:block/battlefield_weapon_{name}"
    assert aged["textures"] == {k: v + "_aged" for k, v in item["textures"].items()}
    assert states[f"source={i}"]["model"] == f"tears-of-ink:block/battlefield_weapon_{name}_aged"
    assert f'"{texture}"' in source
for i, name in enumerate(SHIELDS):
    shield = read(f"models/item/shield/{name}.json")
    aged = read(f"models/block/battlefield_soul_shield_{name}.json")
    assert aged["parent"] == f"tears-of-ink:item/shield/{name}"
    assert aged["textures"] == {k: v + "_aged" for k, v in shield["textures"].items()}
    assert states[f"source={8+i}"]["model"] == f"tears-of-ink:block/battlefield_soul_shield_{name}"
    assert f'"{name}_shield"' in source
atlas = json.loads((ROOT / "src/main/resources/assets/minecraft/atlases/blocks.json").read_text())
for name, tex in zip(PATROL, PALE):
    aged = read(f"models/block/battlefield_{name}_aged.json")
    assert aged["parent"] == f"tears-of-ink:block/battlefield_{name}"
    assert aged["textures"]["0"] == f"tears-of-ink:entity/{tex}_aged"
    assert { "type":"minecraft:single", "resource":f"tears-of-ink:entity/{tex}_aged",
             "sprite":f"tears-of-ink:entity/{tex}_aged"} in atlas["sources"]
    bs = read(f"blockstates/battlefield_{name}.json")
    assert all(v["model"] == f"tears-of-ink:block/battlefield_{name}_aged"
               for v in bs["variants"].values())
    assert f'"{tex}"' in source

geo = (ROOT / "src/main/java/com/tarkrakov/tearsofink/world/battlefield/BattlefieldMonumentGeometry.java").read_text()
styles = re.findall(r"new Style\(\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.-]+)f,\s*([\d.]+)f\)", geo)
assert len(styles) == 8, styles
assert any(float(v[1]) == 0 for v in styles) and any(float(v[1]) == 45 for v in styles)
for scale, tilt, yaw, burial in styles:
    assert 6 <= float(scale) <= 11
    assert 0 <= float(tilt) <= 45
    assert abs(float(yaw)) <= 15
    assert 0 < float(burial) < 2.5
assert 'PALE_LINEAR_GREATSWORD' in geo and 'SPIRE_TORTURER_GREATSWORD' in geo
assert "TIP_DOWN_Z_DEGREES" in (ROOT / "src/client/java/com/tarkrakov/tearsofink/client/battlefield/BattlefieldMonumentRenderer.java").read_text()
command = (ROOT / "src/main/java/com/tarkrakov/tearsofink/command/BattlefieldDecorCommands.java").read_text()
assert 'monumentCommand("linear"' in command and 'monumentCommand("cleaving"' in command
assert 'Commands.literal("tableau")' in command and "tableauContacts" in command
assert 'public static Relic[] tableau' in geo and "groundSink(sink, pitch, roll)" in geo
assert 'Contacts' in (ROOT / "src/main/java/com/tarkrakov/tearsofink/block/BattlefieldMonumentBlockEntity.java").read_text()
assert (A / "blockstates/battlefield_monument.json").is_file()
assert "generateBattlefieldAgedResources" in (ROOT / "build.gradle").read_text()
print("Validated 15 aged atlases, all source UV inheritance, 2 giant meshes and 8 safe poses.")
