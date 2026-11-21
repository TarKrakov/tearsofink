#!/usr/bin/env python3
"""Deterministic author-art extraction for static battlefield test blocks.

Default mode checks committed resources without changing any file.
Use --write only when approved patrol BB models are intentionally updated.
Original .bbmodel, entity textures, entity models and animations are never written.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets"
SOURCES = (
    # bottom_y is the exported (pre-flip) model minimum, in 1/16 block units.
    # For flipped X=180 models: rendered Y becomes 16-preflipY. The large
    # shield anchor is one block above terrain; its rendered range is 0..40
    # from the ground (model space -16..24 relative to its anchor).
    ("spear", "blockbench/pale_soldier_spearman.bbmodel", "spear", "pale_spearman", 0.70, 3, -1.0, False),
    ("shield", "blockbench/pale_soldier_shieldman.bbmodel", "Shield", "pale_shieldbearer", 1.60, 2, -8.0, True),
    ("crossbow", "blockbench/pale_soldier_crossbowman.bbmodel", "crossbow", "pale_crossbowman", 1.0, 7, 11.0, True),
)


def subtree(data: dict, bone: str) -> list[dict]:
    groups = {g["uuid"]: g for g in data["groups"]}
    elements = {e["uuid"]: e for e in data["elements"]}
    targets = []

    def find(nodes: list) -> None:
        for node in nodes:
            if isinstance(node, dict):
                if groups[node["uuid"]]["name"] == bone:
                    targets.append(node)
                else:
                    find(node.get("children", []))

    def collect(nodes: list) -> list[dict]:
        found = []
        for node in nodes:
            if isinstance(node, str):
                element = elements[node]
                if element.get("export", True) and element.get("visibility", True):
                    found.append(element)
            else:
                found.extend(collect(node.get("children", [])))
        return found

    find(data["outliner"])
    assert len(targets) == 1, f"Expected exactly one bone {bone}: {len(targets)}"
    return collect(targets[0].get("children", []))


def rotated_cube(cube: dict) -> dict:
    origin = cube.get("origin", [0, 0, 0])
    angle = cube.get("rotation", [0, 0, 0])[1]
    assert angle in (0, -90), f"Unsupported cube Y rotation {angle}; review UV before exporting"
    assert cube.get("rotation", [0, 0, 0])[0] == 0
    assert cube.get("rotation", [0, 0, 0])[2] == 0
    corners = []
    for x in (cube["from"][0], cube["to"][0]):
        for y in (cube["from"][1], cube["to"][1]):
            for z in (cube["from"][2], cube["to"][2]):
                if angle == -90:
                    x1, z1 = origin[0] - (z - origin[2]), origin[2] + (x - origin[0])
                else:
                    x1, z1 = x, z
                corners.append((x1, y, z1))
    bounds = {
        "from": [min(c[i] for c in corners) for i in range(3)],
        "to": [max(c[i] for c in corners) for i in range(3)],
    }
    face_map = (
        {"north": "east", "east": "south", "south": "west", "west": "north",
         "up": "up", "down": "down"} if angle == -90 else {}
    )
    faces = {}
    for direction, face in cube["faces"].items():
        if face.get("texture", 0) is None:
            continue
        f = {"uv": face["uv"], "texture": "#0"}
        if angle == -90 and direction in ("up", "down"):
            f["rotation"] = 90
        faces[face_map.get(direction, direction)] = f
    bounds["faces"] = faces
    return bounds


def model(data: dict, bone: str, texture: str, scale: float, count: int, bottom_y: float) -> dict:
    cubes = subtree(data, bone)
    assert len(cubes) == count, f"Unexpected {bone} cube count {len(cubes)} != {count}"
    shapes = [rotated_cube(c) for c in cubes]
    lower = [min(s["from"][i] for s in shapes) for i in range(3)]
    upper = [max(s["to"][i] for s in shapes) for i in range(3)]
    shift = [
        8 - (lower[0] + upper[0]) * scale / 2,
        bottom_y - lower[1] * scale,
        8 - (lower[2] + upper[2]) * scale / 2,
    ]
    dims = [data["resolution"]["width"], data["resolution"]["height"]]

    def rounded(n: float) -> float:
        return round(n, 4)

    elements = []
    for shape in shapes:
        faces = {}
        for direction, f in shape["faces"].items():
            face = {"uv": [rounded(n * 16 / dims[i % 2]) for i, n in enumerate(f["uv"])], "texture": "#0"}
            if "rotation" in f:
                face["rotation"] = f["rotation"]
            faces[direction] = face
        element = {
            "from": [rounded(n * scale + shift[i]) for i, n in enumerate(shape["from"])],
            "to": [rounded(n * scale + shift[i]) for i, n in enumerate(shape["to"])],
            "faces": faces,
        }
        assert all(-16 <= lo <= hi <= 32 for lo, hi in zip(element["from"], element["to"]))
        elements.append(element)
    sprite = f"tears-of-ink:entity/{texture}"
    return {"ambientocclusion": False, "textures": {"0": sprite, "particle": sprite}, "elements": elements}


def blockstate(name: str, flip_x: bool) -> dict:
    variants = {}
    for i, d in enumerate(("north", "east", "south", "west")):
        value = {"model": f"tears-of-ink:block/battlefield_{name}", "y": i * 90}
        if flip_x:
            value["x"] = 180  # rotate geometry AND UV; do not rebuild author texture
        variants[f"facing={d}"] = value
    return {"variants": variants}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true", help="regenerate only derived block JSON files")
    args = parser.parse_args()
    outputs = {}
    for name, path, bone, texture, scale, count, bottom_y, flip_x in SOURCES:
        data = json.loads((ROOT / path).read_text(encoding="utf-8"))
        outputs[ASSETS / f"tears-of-ink/models/block/battlefield_{name}.json"] = model(data, bone, texture, scale, count, bottom_y)
        outputs[ASSETS / f"tears-of-ink/blockstates/battlefield_{name}.json"] = blockstate(name, flip_x)

    atlas = {
        "sources": [
            {"type": "minecraft:single", "resource": f"tears-of-ink:entity/{texture}",
             "sprite": f"tears-of-ink:entity/{texture}"}
            for _, _, _, texture, _, _, _, _ in SOURCES
        ]
    }
    outputs[ASSETS / "minecraft/atlases/blocks.json"] = atlas

    for path, expected in outputs.items():
        if args.write:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(expected, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        else:
            actual = json.loads(path.read_text(encoding="utf-8"))
            assert actual == expected, f"Stale battlefield asset: {path.relative_to(ROOT)}"
    print(f"Battlefield patrol decor: {len(SOURCES)} authored bones, {len(outputs)} resources {'updated' if args.write else 'verified'}")


if __name__ == "__main__":
    main()
