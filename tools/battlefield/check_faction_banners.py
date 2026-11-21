#!/usr/bin/env python3
"""D-133: fail CI if either faction's actual authored 3D flags, joints, UV
or PNGs are replaced by guesses, or if a bare staff is the wrong faction.
This is a source/rig check, NOT a visual Minecraft acceptance test.
"""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[2]
JAVA = (ROOT / "src/main/java/com/tarkrakov/tearsofink/world/battlefield/BattlefieldFactionBannerGeometry.java").read_text(encoding="utf-8")
CLIENT = (ROOT / "src/client/java/com/tarkrakov/tearsofink/client/battlefield/BattlefieldBannerRenderer.java").read_text(encoding="utf-8")
BLOCK = (ROOT / "src/main/java/com/tarkrakov/tearsofink/block/BattlefieldBannerBlock.java").read_text(encoding="utf-8")
COMMAND = (ROOT / "src/main/java/com/tarkrakov/tearsofink/command/BattlefieldDecorCommands.java").read_text(encoding="utf-8")
ZONE = (ROOT / "src/main/java/com/tarkrakov/tearsofink/command/BattlefieldZonePreview.java").read_text(encoding="utf-8")
POSED = (ROOT / "src/main/java/com/tarkrakov/tearsofink/world/battlefield/BattlefieldPosedScenes.java").read_text(encoding="utf-8")
RECIPES = (ROOT / "src/main/java/com/tarkrakov/tearsofink/world/battlefield/BattlefieldFactionScenes.java").read_text(encoding="utf-8")

for faction, full_name, bare_name, art_texture, packed in [
    ("SOUL", "battlefield_soul_flag_1", "battlefield_soul_flag_withoutbannet1",
        "soul_flag_1_texture.png", "battlefield_soul_flag1.png"),
    ("HUNTER", "battlefield_hunter_flag_1", "battlefield_hunter_flag_withoutbanner1",
        "hunter_flag_1_texture.png", "battlefield_hunter_flag1.png"),
]:
    full = json.loads((ROOT / "blockbench" / (full_name + ".bbmodel")).read_text(encoding="utf-8"))
    bare = json.loads((ROOT / "blockbench" / (bare_name + ".bbmodel")).read_text(encoding="utf-8"))
    assert full["resolution"]["width"] == 128
    assert full["textures"][0]["name"] == art_texture
    # Author edited bare variants with stale canvas metadata (16/64).
    # Their UVs are copied from the FULL 128 UV atlas: never rescale them.
    for name, model, expected in [("FULL", full, 24 if faction == "SOUL" else 17),
                                  ("BARE", bare, 13 if faction == "SOUL" else 9)]:
        assert len(model["elements"]) == expected, (faction, name, "cube count")
        section = JAVA.split(f"private static final Model {faction}_{name} =", 1)[1].split("});", 1)[0]
        assert len(re.findall(r"new Part\(", section)) == expected, (faction, name, "export count")
        # Rebuild the joint-path index from the real Blockbench hierarchy.
        groups = {g["uuid"]: g for g in model["groups"]}
        by_uuid = {e["uuid"]: i for i, e in enumerate(model["elements"])}
        paths, joints, joints_by_uuid = {}, [], {}
        def traverse(item, parents):
            if isinstance(item, str):
                assert item in by_uuid, (faction, name, "orphaned cube")
                paths[by_uuid[item]] = tuple(parents)
                return
            g = groups[item["uuid"]]
            moved = parents
            if g["name"].startswith("flag") and g["name"] != "flag_main":
                if g["uuid"] not in joints_by_uuid:
                    joints_by_uuid[g["uuid"]] = len(joints)
                    joints.append((g["origin"], len(parents), round((g["origin"][1] - 65) / 70, 5)))
                moved = (*parents, joints_by_uuid[g["uuid"]])
            for child in item.get("children", []):
                traverse(child, moved)
        for top in model["outliner"]:
            traverse(top, ())
        assert len(paths) == expected
        def jfloat(v):
            return format(float(v), ".12g") + "f"
        for j, depth, phase in joints:
            signature = "new Joint(" + ", ".join(jfloat(v) for v in j)
            signature += ", " + str(depth) + ", " + jfloat(phase) + ")"
            assert signature in section, (faction, name, "authored joint pivot", signature)
        assert len(re.findall(r"new Joint\\(", section)) == len(joints), (faction, name, "joint count")
        for i, cuboid in enumerate(model["elements"]):
            # Exact source bounds, ALL six UV faces and every bone ancestry:
            # a duplicated/recolored stand-in must fail this test.
            bounds = ", ".join(jfloat(v) for v in cuboid["from"] + cuboid["to"])
            faces = ["north", "east", "south", "west", "up", "down"]
            uv = ", ".join("{" + ", ".join(jfloat(v) for v in cuboid["faces"][face]["uv"]) + "}"
                           for face in faces)
            path_ids = ", ".join(str(i) for i in paths[i])
            signature = "new BattlefieldBannerGeometry.Cuboid(" + bounds
            signature += ", new float[][]{" + uv + "}), new int[]{" + path_ids + "})"
            assert signature in section, (faction, name, "author cubes/UV/joint path", i)
        assert all(not element.get("rotation", {}).get("angle") for element in model["elements"])
    pole_cubes = {
        (tuple(e["from"]),tuple(e["to"]),
         tuple(tuple(e["faces"][f]["uv"]) for f in ("north","east","south","west","up","down")))
        for e in full["elements"]
    }
    assert all(
        (tuple(e["from"]),tuple(e["to"]),
         tuple(tuple(e["faces"][f]["uv"]) for f in ("north","east","south","west","up","down"))) in pole_cubes
        for e in bare["elements"]
    ), (faction, "bare flag not the same authored staff")
    src = (ROOT / "blockbench" / art_texture).read_bytes()
    out = (ROOT / "src/main/resources/assets/tears-of-ink/textures/block" / packed).read_bytes()
    assert src == out and src.startswith(b"\x89PNG\r\n\x1a\n"), (faction, "PNG changed")
    assert f"battlefield_{faction.lower()}_flag1.png" in CLIENT
    assert f"Commands.literal(\"{'paladin' if faction=='SOUL' else 'hunter'}_standard\")" in COMMAND
    assert f"Commands.literal(\"{'paladin' if faction=='SOUL' else 'hunter'}_bare\")" in COMMAND

assert 'EnumProperty.create("faction", Faction.class)' in BLOCK
assert '.setValue(FACTION, Faction.REGULAR)' in BLOCK
assert 'IntegerProperty.create("layout", 0, 3)' in (
    ROOT / "src/main/java/com/tarkrakov/tearsofink/block/BattlefieldSceneBlock.java"
).read_text(encoding="utf-8")
assert "BattlefieldFactionBannerGeometry.windYaw(j, phase)" in CLIENT
assert "BattlefieldFactionBannerGeometry.windRoll(j, phase)" in CLIENT
assert "BattlefieldFactionBannerGeometry.footprint(faction, bare, facing)" in COMMAND
assert "BattlefieldFactionBannerGeometry.footprint(faction, bare, facing)" in ZONE
assert "FACTIONAL = buildCache(3)" in POSED
assert all(x in RECIPES for x in ["PALADIN_BANNER", "PALADIN_BANNER_BARE",
                                   "HUNTER_BANNER", "HUNTER_BANNER_BARE"])
print("D-133: all 63 author cuboids, original full/bare UV, both PNG, "
      "moving bone paths, save-safe faction state and preview commands present.")
