package com.tarkrakov.tearsofink.weapon.ranged;

/** Holy pistol: existing accepted balance, HP before protection, ticks at 20 TPS. */
public final class PistolTuning {
    private PistolTuning() {}
    public static final float DAMAGE = 8f, WILD_DAMAGE = 10f;
    public static final int PELLETS = 6, PELLET_LIFETIME = 6;
    public static final int SHOT_COST = 1, WILD_COST = 2;
    public static final int RELOAD = 38, DUAL_RELOAD = 60;
    public static final int WILD_SOUL = 12, WILD_COOLDOWN = 100;
    // Speed in blocks/tick; SPREAD is vanilla projectile inaccuracy, not degrees.
    public static final float SPEED = 3.2f, HIP_SPREAD = 18f, AIM_SPREAD = 10f;
    public static final int AIM_DISTANCE = 8;
    public static final double RELOAD_SLOW = .18;
    public static final double WILD_RANGE = 3.3, WILD_RANGE_SQUARED = 10.89;
    public static final double WILD_FORWARD_DOT = .45;
    public static final double WILD_KNOCKBACK = 1.1, WILD_RECOIL = .75, WILD_LIFT = .14;
}
