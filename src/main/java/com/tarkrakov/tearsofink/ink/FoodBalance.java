package com.tarkrakov.tearsofink.ink;

/** D-100: named food tuning; saturation is an absolute amount, not a builder modifier. */
public final class FoodBalance {
    public static final float VANILLA_IN_LANDS = 0.70f;
    public static final int ROASTED_FLESHBERRY_NUTRITION = 5;
    public static final float ROASTED_FLESHBERRY_SATURATION_MODIFIER = 0.6f; // 2 * 5 * .6 = 6
    public static final int CORRUPTED_MEAT_NUTRITION = 8;
    public static final float CORRUPTED_MEAT_SATURATION = 14f;
    private FoodBalance() {}

    public static float originMultiplier(boolean inLands, boolean vanilla) {
        return inLands && vanilla ? VANILLA_IN_LANDS : 1f;
    }

    public static float multiplier(float soulMultiplier, boolean inLands, boolean vanilla) {
        return soulMultiplier * originMultiplier(inLands, vanilla);
    }

    public static int nutrition(int base, float multiplier) {
        return base > 0 ? Math.max(1, Math.round(base * multiplier)) : 0;
    }

    public static float saturation(float base, float multiplier) {
        return base * multiplier;
    }
}
