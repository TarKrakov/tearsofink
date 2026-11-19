package com.tarkrakov.tearsofink.weapon.combat;

/** Working local balance. 20 ticks = 1 second. Damage = weapon attribute × multiplier. */
public final class PaleLongswordTuning {
    private PaleLongswordTuning() {}
    public record Strike(float damage, double reach, double arc, double knockback, int hit, int duration, int cooldown) {}
    public static final Strike SINGLE = new Strike(1.65f, 3.4, 100, .65, 18, 25, 100);
    public static final Strike DUAL = new Strike(2.0f, 3.6, 150, .85, 11, 25, 150);
    public static final float BLOCK_REDUCTION = .5f;
    public static final double BLOCK_ARC = 130;
    public static final int BLOCK_COOLDOWN = 80, BLOCK_START = 10, BLOCK_LOOP = 10, BLOCK_END = 10;
}
