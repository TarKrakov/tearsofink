package com.tarkrakov.tearsofink.world;

/** D-097 / D-099: origin penalty for wearable protection and weapon damage. */
public final class EquipmentAffinity {
    /** External equipment in Ink Lands: 60% penalty. */
    public static final double EXTERNAL_IN_LANDS = 0.40;
    /** Tears of Ink equipment outside Ink Lands: original 40% penalty. */
    public static final double MOD_OUTSIDE_LANDS = 0.60;
    private EquipmentAffinity() {}

    public static double multiplier(boolean inLands, boolean modEquipment) {
        if (inLands == modEquipment) return 1.0;
        return inLands ? EXTERNAL_IN_LANDS : MOD_OUTSIDE_LANDS;
    }
}
