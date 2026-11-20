#!/usr/bin/env python3
"""Verify authored armor BB texture links, slot groups and 3D item previews.
Does not change artist resources or require external Python dependencies.
"""
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/tears-of-ink"
MODELS = {
    "pale_heavy_armor": (
        "pale_closed_helmet", "pale_cuirass", "pale_plate_leggings", "pale_sabatons"),
    "pale_medium_armor": (
        "pale_chain_hood", "pale_brigandine", "pale_reinforced_leggings", "pale_leather_boots"),
    "pale_light_armor": (
        "pale_headband", "pale_quilted_jacket", "pale_pants", "pale_shoes"),
}
SLOT_GROUPS = {
    "helmet": "head",
    "chestplate": "body",
    "chestplate_right_arm": "right_arm",
    "chestplate_left_arm": "left_arm",
    "leggings_waist": "body",
    "leggings_right": "right_leg",
    "leggings_left": "left_leg",
    "boot_right": "right_leg",
    "boot_left": "left_leg",
}
BONES = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}
SLOT_TYPE = {
    "helmet": "head", "chestplate": "chest",
    "chestplate_right_arm": "chest", "chestplate_left_arm": "chest",
    "leggings_waist": "legs", "leggings_right": "legs", "leggings_left": "legs",
    "boot_right": "feet", "boot_left": "feet",
}


def validate(model_name, ids):
    model_path = ROOT / "blockbench" / f"{model_name}.bbmodel"
    model = json.loads(model_path.read_text(encoding="utf-8"))
    textures = model["textures"]
    assert len(textures) == 1, f"{model_name}: expected one texture"
    tex = textures[0]
    filename = tex["name"]
    assert re.fullmatch(r"[a-z0-9_.-]+\\.png", filename), (model_name, filename)
    png_path = ASSETS / "textures/armor" / filename
    assert png_path.is_file(), f"{model_name}: missing PNG {png_path}"
    png = png_path.read_bytes()
    assert png[:16] == b"\\x89PNG\\r\\n\\x1a\\n\\x00\\x00\\x00\\rIHDR", (model_name, "bad PNG")
    width, height = struct.unpack(">II", png[16:24])
    assert width == tex["width"] and height == tex["height"], (model_name, "PNG dimensions")
    resolution = model["resolution"]
    u = tex.get("uv_width", resolution["width"])
    v = tex.get("uv_height", resolution["height"])
    groups = {x["uuid"]: x for x in model["groups"]}
    cubes = {x["uuid"]: x for x in model["elements"]}
    counts = {"head": 0, "chest": 0, "legs": 0, "feet": 0}

    def walk(nodes, bone=None, slot=None):
        for node in nodes:
            if isinstance(node, str):
                assert node in cubes, (model_name, "unknown cube", node)
                if slot is None:
                    continue  # Mannequin/reference geometry
                cube = cubes[node]
                if cube.get("export", True) is False:
                    continue
                assert cube.get("type", "cube") == "cube", (model_name, "unsupported element")
                assert bone == SLOT_GROUPS[slot], (model_name, "wrong bone", slot)
                counts[SLOT_TYPE[slot]] += 1
                for face in cube.get("faces", {}).values():
                    if face.get("enabled", True) is False or face.get("texture") is None:
                        continue
                    assert face["texture"] == 0, (model_name, "texture index")
                    u0, v0, u1, v1 = face["uv"]
                    assert 0 <= min(u0, u1) and max(u0, u1) <= u, (model_name, "UV U range")
                    assert 0 <= min(v0, v1) and max(v0, v1) <= v, (model_name, "UV V range")
            else:
                group = groups[node["uuid"]]
                name = group["name"]
                if group.get("export", True) is False:
                    continue
                next_bone = name if name in BONES else bone
                next_slot = None if name in BONES else name if name in SLOT_GROUPS else slot
                walk(node["children"], next_bone, next_slot)

    walk(model["outliner"])
    assert all(counts.values()), (model_name, "missing slot geometry", counts)
    for item_id in ids:
        json_path = ASSETS / "models/item" / f"{item_id}.json"
        item = json.loads(json_path.read_text(encoding="utf-8"))
        assert item["parent"] == "tears-of-ink:item/authored_armor_preview", (item_id, "item preview")
    print(f"{model_name}: OK — {filename}, {width}x{height}, slots {counts}")


if __name__ == "__main__":
    for model, items in MODELS.items():
        validate(model, items)
    print("Authored armor models and textures OK")
