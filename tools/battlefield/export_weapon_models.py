#!/usr/bin/env python3
"""Export ONLY approved authored item meshes into read-only decorative block models.

The artist's item models, item display transforms, textures and PNGs remain
untouched. Usage:
  python3 tools/battlefield/export_weapon_models.py
  python3 tools/battlefield/export_weapon_models.py --check

The baked static block models reuse the same JSON elements, UVs and textures.
No geometry or material is guessed, created or retextured.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "src/main/resources/assets/tears-of-ink"
SOURCES = ["pale_longsword","pale_linear_great","blood_talvar","blood_dussak","spire_torturer_greatsword","spire_roaring_axe","spire_shadow_scimitar","spire_rapid_knife"]
IDS = ["PALE_LONGSWORD","PALE_LINEAR_GREATSWORD","BLOOD_TALVAR","BLOOD_DUSSAK","SPIRE_TORTURER_GREATSWORD","SPIRE_ROARING_AXE","SPIRE_SHADOW_SCIMITAR","SPIRE_RAPID_KNIFE"]
SOUL_SHIELDS = ["ram", "repel", "mercy", "radiant"]


def model_for(name):
    src = json.loads((BASE / "models/item" / (name + ".json")).read_text(encoding="utf-8"))
    if not src.get("elements") or not src.get("textures"):
        raise ValueError(f"Not a real 3D author model: {name}")
    for cube in src["elements"]:
        if cube.get("rotation", {}).get("angle", 0) not in (0, 0.0):
            raise ValueError(f"Non-zero source element rotation needs explicit export review: {name}")
    for token in src["textures"].values():
        if token.startswith("tears-of-ink:item/"):
            image = BASE / "textures/item" / (token.split("/", 1)[1] + ".png")
            if not image.is_file():
                raise ValueError(f"Missing original artist texture: {image}")
    return {"ambientocclusion": False, "textures": src["textures"], "elements": src["elements"]}


def blockstates():
    variants = {
        "source=" + str(i): {"model": "tears-of-ink:block/battlefield_weapon_" + name}
        for i, name in enumerate(SOURCES)
    }
    variants.update({
        "source=" + str(i + 8): {"model": "tears-of-ink:block/battlefield_soul_shield_" + name}
        for i, name in enumerate(SOUL_SHIELDS)
    })
    return {"variants": variants}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    payloads = {BASE / "models/block" / ("battlefield_weapon_" + name + ".json"): model_for(name)
                for name in SOURCES}
    # Block models inherit the actual complete 3D source without copying,
    # reinterpreting or editing ANY of the artist's elements or UVs.
    for name in SOUL_SHIELDS:
        model_for("shield/" + name)  # Verify real 3D elements and source PNG.
        payloads[BASE / "models/block" / ("battlefield_soul_shield_" + name + ".json")] = {
            "ambientocclusion": False,
            "parent": "tears-of-ink:item/shield/" + name,
        }
    payloads[BASE / "blockstates/battlefield_weapon_mesh.json"] = blockstates()
    for path, expected in payloads.items():
        if args.check:
            if not path.exists() or json.loads(path.read_text(encoding="utf-8")) != expected:
                raise SystemExit(f"Not synced with original artist source: {path}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(expected, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Verified {len(SOURCES)} original weapon meshes and {len(SOUL_SHIELDS)} original 3D Soul shields with their textures.")


if __name__ == "__main__":
    main()
