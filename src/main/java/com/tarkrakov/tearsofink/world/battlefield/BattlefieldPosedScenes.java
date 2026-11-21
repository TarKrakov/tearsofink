package com.tarkrakov.tearsofink.world.battlefield;

import java.util.List;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Scene;

/**
 * D-123: meaningfully POSED scene, not just cardinal directions of block models.
 * All measurements are Minecraft blocks relative to the same local episode
 * axis: positive forward = toward the front of the scene, positive right = right.
 * Angles are actual independent Euler angles in degrees, applied Y -> Z -> X
 * around the model's *ground contact*, not Minecraft blockstate rotations.
 * Banners remain the author's animated blocks; only static props are posed.
 *
 * This is a temporary small-scene preview. Massive fields will require an
 * author-approved static baking strategy, not thousands of rendered block entities.
 */
public final class BattlefieldPosedScenes {
    private BattlefieldPosedScenes() {}

    public record Pose(Prop prop, float forward, float right, float sink,
                       float yaw, float pitch, float roll, boolean required) {
        public boolean banner() { return prop == Prop.BANNER; }
    }

    public static List<Pose> plan(Scene scene, int variant) {
        int mirror = (variant & 1) == 0 ? 1 : -1;
        float skew = (variant & 2) == 0 ? -0.16f : 0.16f;
        float broken = (variant & 4) == 0 ? -4f : 4f;
        return switch (scene) {
            case LAST_STAND -> List.of(
                    // The shield is no longer upright: sunken and bent back.
                    new Pose(Prop.SHIELD, 0f, 0f, -0.29f,
                            (18f + broken) * mirror, -9f, -19f * mirror, true),
                    // Leaning spear top reaches the shield: a genuine relationship.
                    new Pose(Prop.SPEAR, 0.48f, (-0.66f + skew) * mirror, -0.10f,
                            -31f * mirror, 24f, -31f * mirror, true),
                    // A second spear lies at a steep angle across the line.
                    new Pose(Prop.SPEAR, -0.72f, 1.22f * mirror, -0.15f,
                            56f * mirror, 69f, 13f * mirror, false),
                    // A thrown crossbow, close to the shield but not another post.
                    new Pose(Prop.CROSSBOW, 1.05f, 0.74f * mirror, -0.13f,
                            (74f + broken) * mirror, 16f, -12f * mirror, false),
                    // The banner marks the flank, not the centre of the pile.
                    new Pose(Prop.BANNER, -1.30f, -4.25f * mirror, 0f,
                            0f, 0f, 0f, false)
            );
            case BANNER_LINE -> List.of(
                    // The two banners trace a broken diagonal with a traversable gap.
                    new Pose(Prop.BANNER, -1.05f, -4.5f * mirror, 0f,
                            0f, 0f, 0f, true),
                    new Pose(Prop.BANNER, 1.15f, 4.5f * mirror, 0f,
                            0f, 0f, 0f, true),
                    // Shield and spear describe what happened along that line.
                    new Pose(Prop.SHIELD, 0.28f, -0.18f * mirror, -0.36f,
                            (-26f + broken) * mirror, 12f, 27f * mirror, true),
                    new Pose(Prop.SPEAR, 0.75f, (0.74f + skew) * mirror, -0.10f,
                            38f * mirror, 42f, -31f * mirror, false)
            );
        };
    }
}
