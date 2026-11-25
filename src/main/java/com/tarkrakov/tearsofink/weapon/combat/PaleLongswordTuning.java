package com.tarkrakov.tearsofink.weapon.combat;

/** Working local balance. 20 ticks = 1 second. Damage = weapon attribute × multiplier. */
public final class PaleLongswordTuning {
    private PaleLongswordTuning() {}
    // Source hit/duration are Blockbench ticks at 1x. Cooldown is real server ticks.
    public record Strike(float damage, double reach, double arc, double knockback,
                         int sourceHit, int sourceDuration, int cooldown, float tempo) {
        public int hit() { return Math.max(1, Math.round(sourceHit / tempo)); }
        public int duration() { return Math.max(1, (int) Math.ceil(sourceDuration / (double) tempo - 1e-6)); }
    }
    public static final Strike SINGLE = new Strike(1.65f, 3.4, 100, .65, 18, 25, 100, 2f);
    public static final Strike DUAL = new Strike(2.0f, 3.6, 150, .85, 11, 25, 150, 5f/3f);
    public static final float BLOCK_REDUCTION = .5f;
    public static final double BLOCK_ARC = 130;
    // Quick raise/lower, original breathing cycle while held.
    public static final float BLOCK_TRANSITION_TEMPO = 2f;
    public static final int BLOCK_COOLDOWN = 0, BLOCK_START = Math.round(10 / BLOCK_TRANSITION_TEMPO),
            BLOCK_LOOP = 10, BLOCK_END = Math.round(10 / BLOCK_TRANSITION_TEMPO);
}
