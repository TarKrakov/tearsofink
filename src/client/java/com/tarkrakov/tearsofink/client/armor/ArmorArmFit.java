package com.tarkrakov.tearsofink.client.armor;

/** Wide Steve arms span four model pixels. Slim Alex arms preserve the torso-facing
 * edge but are one pixel narrower on the outside. Geometry is scaled only when
 * equipped; the author's BB and the unmodified item preview retain their shape. */
public final class ArmorArmFit {
    private ArmorArmFit() {}

    public static final float SLIM_X_SCALE = 3.0f / 4.0f;
    private static final float INSIDE_EDGE_PIXELS = 1.0f;

    /** Offset in Minecraft arm-local units, applied after the arm bone pose. */
    public static float slimOffsetX(boolean rightArm) {
        return (rightArm ? 1f : -1f) * (1f - SLIM_X_SCALE) * INSIDE_EDGE_PIXELS / 16f;
    }

    /** The same affine transform applied to each vertex by the render pose. */
    public static float slimLocalX(float wideLocalX, boolean rightArm) {
        return slimOffsetX(rightArm) + SLIM_X_SCALE * wideLocalX;
    }
}
