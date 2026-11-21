package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;

/** Source item JSON dimensions (pixels). Only invisible field decor is 1.5x. */
public final class BattlefieldSoulShieldCatalog {
    private BattlefieldSoulShieldCatalog() {}
    public static final float SCALE = 1.5f;

    // Indices 0..7 belong to the existing eight blade/axe proxies, unchanged.
    public enum Source {
        RAM(Prop.SOUL_RAM_SHIELD, -10.5f, 10.5f, -11f, 13f, -1.375f, 3.875f),
        REPEL(Prop.SOUL_REPEL_SHIELD, -12f, 12f, -9.75f, 12.75f, -3.25f, 4.25f),
        MERCY(Prop.SOUL_MERCY_SHIELD, -12f, 12f, -10f, 14f, -2.3125f, 4.0625f),
        RADIANT(Prop.SOUL_RADIANT_SHIELD, -12f, 12f, -10f, 14f, -1.375f, 3.875f);

        public final Prop prop;
        public final float minX, maxX, minY, maxY, minZ, maxZ;
        Source(Prop prop, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
            this.prop = prop;
            this.minX = minX; this.maxX = maxX;
            this.minY = minY; this.maxY = maxY;
            this.minZ = minZ; this.maxZ = maxZ;
        }
        public int meshIndex() { return 8 + ordinal(); }
        public float centerX() { return (minX + maxX) * .5f; }
        public float centerZ() { return (minZ + maxZ) * .5f; }
        public float visibleHeight() { return (maxY - minY) * SCALE / 16f; }
    }

    public static boolean isShield(Prop prop) {
        return switch (prop) {
            case SOUL_RAM_SHIELD, SOUL_REPEL_SHIELD, SOUL_MERCY_SHIELD, SOUL_RADIANT_SHIELD -> true;
            default -> false;
        };
    }

    public static Source fromProp(Prop prop) {
        return switch (prop) {
            case SOUL_RAM_SHIELD -> Source.RAM;
            case SOUL_REPEL_SHIELD -> Source.REPEL;
            case SOUL_MERCY_SHIELD -> Source.MERCY;
            case SOUL_RADIANT_SHIELD -> Source.RADIANT;
            default -> throw new IllegalArgumentException("Not a Soul shield: " + prop);
        };
    }
}
