#!/usr/bin/env python3
"""D-109: the owl visibly flaps in the air; no movement or other authored clips change."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
animations = (root / "src/client/java/com/tarkrakov/tearsofink/client/entity/PaleOwlAnimations.java").read_text()
model = (root / "src/client/java/com/tarkrakov/tearsofink/client/entity/PaleOwlModel.java").read_text()
entity = (root / "src/main/java/com/tarkrakov/tearsofink/entity/PaleOwlEntity.java").read_text()
tuning = (root / "src/main/java/com/tarkrakov/tearsofink/entity/PaleOwlTuning.java").read_text()

fly = animations.split("public static final AnimationDefinition FLY =", 1)[1].split(
    "public static final AnimationDefinition DIVE =", 1)[0]
assert "Builder.withLength(0.55F).looping()" in fly
for wing, positions in (
    ("Owl_wing_l", (55.0, 78.0, -18.0, 55.0)),
    ("Owl_wing_r", (-55.0, -78.0, 18.0, -55.0)),
):
    rotation = fly.split(f'.addAnimation("{wing}"', 1)[1].split('.addAnimation("', 1)[0]
    assert rotation.count("new Keyframe(") == 4, wing
    for time, angle in zip(("0.0", "0.10", "0.27", "0.55"), positions):
        assert f"new Keyframe({time}F, KeyframeAnimations.degreeVec(0.0F, 0.0F, {angle:.1f}F)" in rotation, (wing, time, angle)

assert "case FLY -> PaleOwlAnimations.FLY;" in model
assert "case DIVE -> PaleOwlAnimations.DIVE;" in model
assert "KeyframeAnimations.animate(this, clip(PaleOwlEntity.phaseOf(current))" in model
assert "FLIGHT_SPEED = 0.23, DIVE_SPEED = 0.34, LAND_SPEED = 0.13" in tuning
assert "private void tickFlight()" in entity
print("D-109: rapid, symmetric owl wingbeats and unchanged flight movement OK")
