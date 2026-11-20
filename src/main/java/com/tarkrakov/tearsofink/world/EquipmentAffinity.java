package com.tarkrakov.tearsofink.world;

/** D-097: one symmetric coefficient for wearable protection and weapon damage. */
public final class EquipmentAffinity {
    public static final double FOREIGN_MULTIPLIER = 0.60;
    private EquipmentAffinity() {}

    public static double multiplier(boolean inLands, boolean modEquipment) {
        return inLands == modEquipment ? 1.0 : FOREIGN_MULTIPLIER;
    }
}
