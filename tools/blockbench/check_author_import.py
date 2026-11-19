#!/usr/bin/env python3
"""Ensure installed first-person author clips are baked from the submitted .bbmodel."""
import json
from pathlib import Path
from import_weapon_animation import bake

ROOT = Path(__file__).resolve().parents[2]
SCENE = ROOT / "blockbench/pale_linear_greatsword_animation.bbmodel"
RESOURCE = ROOT / "src/main/resources/assets/tears-of-ink/weapon_anims/pale_linear_greatsword_bb_author.json"
COMBO = ROOT / "src/main/resources/data/tears-of-ink/weapon_combos/pale_linear_greatsword.json"

project = json.loads(SCENE.read_text(encoding="utf-8"))
resource = json.loads(RESOURCE.read_text(encoding="utf-8"))
installed = resource["animations"]
# Gameplay may intentionally retime an author clip without changing its geometric poses.
# Keep that transformation explicit and validate it instead of silently losing it on re-import.
time_scales = resource.get("time_scale_from_source", {})
combo = json.loads(COMBO.read_text(encoding="utf-8"))
mapping = {
    "attack1": "bb_author_attack1",
    "attack2": "bb_author_attack2",
    "attack3": "bb_author_attack3",
    "r_ability_swing": "bb_author_r_swing",
    "r_ability_dash": "bb_author_r_dash",
    "r_ability_recover": "bb_author_r_recover",
    "block_start": "bb_author_guard_start",
    "block_hold": "bb_author_guard_hold",
    "block_end": "bb_author_guard_end",
}
assert set(mapping.values()) <= set(installed)
assert set(time_scales) <= set(mapping), ("unknown retiming source", time_scales)
assert {"attack_full", "r_ability_full", "block_full"} <= {a["name"] for a in project["animations"]}
assert [s["anim"] for s in combo["steps"]] == [mapping[f"attack{i}"] for i in (1, 2, 3)]
for source_name, live_name in mapping.items():
    animation = next(a for a in project["animations"] if a["name"] == source_name)
    animated_bones = {b["name"] for b in animation.get("animators", {}).values() if b.get("keyframes")}
    assert animated_bones <= {"FP_right_arm", "FP_right_sword", "F5_right_arm", "F5_right_sword"}, (source_name, animated_bones)
    # Verify the previously accepted FP sampling contract independently of newer F5 keys.
    fp_project = dict(project, animations=[dict(animation, animators={uid: track for uid, track in animation["animators"].items() if not track["name"].startswith("F5_")})])
    # A non-colliding validation prefix: the installed bb_author_* name is already loaded by run.load().
    check_name = "bb_validation_" + source_name
    baked = bake(fp_project, source_name, check_name, 60)
    expected_clip = baked["animations"][check_name]
    expected = expected_clip["keyframes"]
    actual = installed[live_name]["keyframes"]
    scale = time_scales.get(source_name, 1)
    assert isinstance(scale, (int, float)) and 0 < scale <= 1, (source_name, "invalid time scale")
    assert abs(installed[live_name]["length"] - round(expected_clip["length"] * scale, 8)) < 1e-8, (live_name, "length")
    assert len(actual) == len(expected), (live_name, "sample count")
    for i, (a, b) in enumerate(zip(actual, expected)):
        assert abs(a["t"] - round(b["t"] * scale, 8)) < 1e-8, (live_name, i, "time")
        for channel in ("rotation", "translation"):
            assert max(abs(x-y) for x,y in zip(a[channel], b[channel])) < 1e-5, (live_name, i, channel)
    assert installed[live_name]["third"]["source"] == live_name + "_shoulder"
    assert installed[live_name]["third"]["wrist_source"] == live_name + "_wrist"
    assert installed[live_name]["third"]["arm_space"] and installed[live_name]["third"]["wrist_xyz"]
for step in combo["steps"]:
    assert abs(installed[step["anim"]]["impact"] - step["hit"]/20) < 1e-7
for name in ("bb_author_guard_start", "bb_author_guard_hold"):
    clip = installed[name]
    assert clip["length"] + clip["hold"] + clip["return"] > 1.5
print("PASS: nine live FP clips match the official Blockbench importer (including declared timing scales), with explicit authored F5 and unchanged server impacts")
