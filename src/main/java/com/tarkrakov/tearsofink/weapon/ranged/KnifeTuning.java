package com.tarkrakov.tearsofink.weapon.ranged;

/** Working balance for the Blood Dagger; HP before protection, ticks at 20 TPS. */
public final class KnifeTuning {
    private KnifeTuning() {}
    public static final float DAMAGE = 3.5f;
    public static final float BLEED_CHANCE = .40f, BLOOD_BLEED_CHANCE = .90f;
    public static final int COST = 1, BLOOD_COST = 2, BLOOD_COOLDOWN = 60;
    public static final int CAPACITY = 48, REFILL_PER_INGOT = 24;
    // Initial speed in blocks/tick, vanilla projectile inaccuracy, maximum flight ticks.
    public static final float SPEED = 1.5f, SPREAD = 1.7f;
    public static final int LIFETIME = 10;
}
