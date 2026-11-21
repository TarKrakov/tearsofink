package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;

/**
 * Only author-allowed 3D weapons: six real contracted-hunter weapons
 * (Hunt.spireLoadout/bloodLoadout) and two completed standard Pale swords.
 * Individual source models/textures and their item display transforms stay
 * untouched. Decorative block models are generated faithful copies.
 * Bounds are source item JSON coordinates in 1/16th blocks, not made-up art.
 */
public final class BattlefieldWeaponCatalog {
    private BattlefieldWeaponCatalog() {}

    // In ALL eight source item meshes the guard/grip sits at the low X+Y
    // diagonal and the blade tip / axe head at the high diagonal.
    // Previously we wrongly pivoted on the grip and used +45 degrees.
    // Keep this angle shared with the client renderer and geometry check.
    public static final float TIP_DOWN_Z_DEGREES = -135f;
    private static final double SQRT_HALF = Math.sqrt(0.5d);

    public enum Source {
        PALE_LONGSWORD(Prop.PALE_LONGSWORD, "pale_longsword", 0.500f, 13.000f, 0.500f, 13.000f, 8.000f, 9.000f),
        PALE_LINEAR_GREATSWORD(Prop.PALE_LINEAR_GREATSWORD, "pale_linear_great", 6.500f, 20.500f, 0.500f, 14.500f, 7.000f, 8.500f),
        BLOOD_TALVAR(Prop.BLOOD_TALVAR, "blood_talvar", 0.500f, 12.500f, 0.500f, 15.500f, 7.000f, 8.000f),
        BLOOD_DUSSAK(Prop.BLOOD_DUSSAK, "blood_dussak", 1.000f, 11.500f, 1.000f, 15.500f, 8.000f, 9.000f),
        SPIRE_TORTURER_GREATSWORD(Prop.SPIRE_TORTURER_GREATSWORD, "spire_torturer_greatsword", 8.000f, 23.500f, 0.000f, 15.500f, 8.000f, 9.500f),
        SPIRE_ROARING_AXE(Prop.SPIRE_ROARING_AXE, "spire_roaring_axe", 4.500f, 20.000f, 0.500f, 15.000f, 7.750f, 9.250f),
        SPIRE_SHADOW_SCIMITAR(Prop.SPIRE_SHADOW_SCIMITAR, "spire_shadow_scimitar", 8.000f, 22.500f, 0.000f, 16.000f, 8.000f, 9.000f),
        SPIRE_RAPID_KNIFE(Prop.SPIRE_RAPID_KNIFE, "spire_rapid_knife", 1.000f, 14.000f, 0.500f, 15.000f, 8.000f, 8.800f);

        public final Prop prop;
        public final String source;
        public final float minX, maxX, minY, maxY, minZ, maxZ;

        Source(Prop prop, String source, float minX, float maxX,
               float minY, float maxY, float minZ, float maxZ) {
            this.prop = prop;
            this.source = source;
            this.minX = minX; this.maxX = maxX;
            this.minY = minY; this.maxY = maxY;
            this.minZ = minZ; this.maxZ = maxZ;
        }

        /** Original item-space dimensions are in pixels, not blocks. */
        public float centerZ() { return (minZ + maxZ) / 2f; }

        /**
         * Contact is the end OPPOSITE the grip. The baked renderer translates
         * by the high diagonal (maxX, maxY), then rotates -135° around Z.
         * Using the same exact transform here prevents the server's terrain
         * validation from disagreeing with the client model.
         */
        public double tipDownX(double sourceX, double sourceY) {
            double u = (sourceX - maxX) / 16d;
            double v = (sourceY - maxY) / 16d;
            return (v - u) * SQRT_HALF;
        }

        public double tipDownY(double sourceX, double sourceY) {
            double u = (sourceX - maxX) / 16d;
            double v = (sourceY - maxY) / 16d;
            return -(u + v) * SQRT_HALF;
        }

        /**
         * Near-horizontal discarded weapons should REST on the terrain,
         * not push the hilt through it. For upright/sunken blades preserve
         * the scene's requested depth as long as the grip is visible.
         * Called once when the 112 immutable scene recipes are cached.
         */
        public float groundSink(float requested, float pitch, float roll) {
            double upright = Math.cos(Math.toRadians(pitch))
                    * Math.cos(Math.toRadians(roll));
            float gripHeight = (float) (approximateLength() * Math.max(0.0d, upright));
            float depth = Math.max(0.02f, gripHeight * 0.40f);
            // Very shallow angles represent discarded weapons; burying even
            // a tenth of their length would visibly hide the handle.
            if (upright < 0.30d) depth = Math.min(depth, 0.10f);
            return Math.max(requested, -depth);
        }
        public float approximateLength() {
            return (float) (Math.hypot(maxX-minX, maxY-minY) / 16d);
        }
    }

    public static Source fromProp(Prop prop) {
        return switch (prop) {
            case PALE_LONGSWORD -> Source.PALE_LONGSWORD;
            case PALE_LINEAR_GREATSWORD -> Source.PALE_LINEAR_GREATSWORD;
            case BLOOD_TALVAR -> Source.BLOOD_TALVAR;
            case BLOOD_DUSSAK -> Source.BLOOD_DUSSAK;
            case SPIRE_TORTURER_GREATSWORD -> Source.SPIRE_TORTURER_GREATSWORD;
            case SPIRE_ROARING_AXE -> Source.SPIRE_ROARING_AXE;
            case SPIRE_SHADOW_SCIMITAR -> Source.SPIRE_SHADOW_SCIMITAR;
            case SPIRE_RAPID_KNIFE -> Source.SPIRE_RAPID_KNIFE;
            default -> throw new IllegalArgumentException("Not an approved weapon: " + prop);
        };
    }

    public static boolean isWeapon(Prop prop) {
        return switch (prop) {
            case SHIELD, SPEAR, CROSSBOW, BANNER, BANNER_BARE -> false;
            default -> true;
        };
    }
}
