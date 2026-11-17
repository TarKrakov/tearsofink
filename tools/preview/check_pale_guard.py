"""Shift+R guard: first-person / F5 pose consistency and server tuning contract.
Run from repository root: python3 tools/preview/check_pale_guard.py
Does not replace the artistic review or real Minecraft combat tests.
"""
import json
import math
from pathlib import Path

import numpy as np
from tl import load, sample, fp_item_m, fp_offset

ARCH = "pale_linear_greatsword"
a, model, _, _, grip = load(ARCH)
anims = a["animations"]
root = Path(__file__).resolve().parents[2]
balance = json.loads((root / "src/main/resources/data/tears-of-ink/weapon_moves" /
                      (ARCH + ".json")).read_text(encoding="utf-8"))["shift_guard"]

assert 0 < balance["single_reduction"] < balance["dual_reduction"] < 1
assert 1 <= balance["single_arc"] <= balance["dual_arc"] <= 360
cases = (
    ("guard", 1, "single", 1.0),
    ("guard_off", -1, "single", 1.0),
    ("dual_guard", 1, "dual", 1.2),
    ("dual_guard_off", -1, "dual", 1.2),
)
for name, hand, mode, seconds in cases:
    clip = anims[name]
    assert clip["third"]["arm_space"], (name, "F5 must follow the authored shoulder")
    assert not clip["third"].get("mirror"), (name, "each hand has its own author pose")
    src = clip["third"]["source"]
    assert src in anims, (name, "missing third-person shoulder clip")
    shoulder = anims[src]
    assert shoulder["length"] == clip["length"]
    assert clip["length"] >= seconds and shoulder["length"] >= seconds
    assert int(round(seconds * 20)) == balance[f"{mode}_duration_ticks"]
    for c in (clip, shoulder):
        times = [key["t"] for key in c["keyframes"]]
        assert times[0] == 0 and all(x < y for x, y in zip(times, times[1:])), name
        assert times[-1] <= c["length"], name
        assert all(np.isfinite([*key["rotation"], *key.get("translation", [0, 0, 0])]).all()
                   for key in c["keyframes"]), name
    display = model["display"]["firstperson_" + ("right" if hand == 1 else "left") + "hand"]
    for t in np.linspace(0, clip["length"], 31):
        rot, pos = sample(clip, float(t))
        matrix = fp_item_m(display, rot, pos, grip, hand, view_offset=fp_offset(model))
        assert np.isfinite(matrix).all(), (name, t)
    arm0, _ = sample(shoulder, 0)
    armGuard, _ = sample(shoulder, .35)
    assert np.linalg.norm(armGuard - arm0) > 10, (name, "guard should visibly lift the blade")
    rot0, pos0 = sample(clip, 0)
    assert np.allclose(rot0, 0) and np.allclose(pos0, 0), (name, "neutral entry")
    rotEnd, posEnd = sample(clip, clip["length"])
    assert np.allclose(rotEnd, 0, atol=.01) and np.allclose(posEnd, 0, atol=.01), (name, "neutral return")
print("PASS: four independent Shift+R hand clips, inherited author Display, F5 shoulder, timing and guard data")
