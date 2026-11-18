#!/usr/bin/env python3
"""Validate advancement icon resources, including the exact texture filenames."""
import json
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "src/main/resources"
advancements = root / "data/tears-of-ink/advancement"
assets = root / "assets/tears-of-ink"
problems = []

for file in sorted(advancements.glob("*.json")):
    advancement = json.loads(file.read_text(encoding="utf-8"))
    item_id = advancement.get("display", {}).get("icon", {}).get("id", "")
    if not item_id.startswith("tears-of-ink:"):
        continue
    item_name = item_id.split(":", 1)[1]
    model_file = assets / "models/item" / (item_name + ".json")
    if not model_file.is_file():
        problems.append(f"{file.name}: missing model {model_file.relative_to(root)}")
        continue
    if not item_name.startswith("icon_"):
        continue  # Equipment item: its parent model may render differently.
    model = json.loads(model_file.read_text(encoding="utf-8"))
    layer = model.get("textures", {}).get("layer0")
    if not layer or not layer.startswith("tears-of-ink:item/"):
        problems.append(f"{model_file.name}: no valid advancement texture layer0")
        continue
    filename = layer.split(":", 1)[1].removeprefix("item/") + ".png"
    png = assets / "textures/item" / filename
    if not png.is_file():
        problems.append(f"{model_file.name}: missing PNG {png.relative_to(root)}")

if problems:
    for problem in problems:
        print("ERROR:", problem, file=sys.stderr)
    sys.exit(1)
print("Advancement icon model and texture paths: OK")
