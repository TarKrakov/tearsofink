package com.tarkrakov.tearsofink.world.battlefield;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashSet;
import java.util.Set;

/**
 * Rare giant swords. Identical transforms drive client rendering and server safety sampling.
 * An authored weapon model is enlarged, NEVER rebuilt or added as a combat weapon.
 */
public final class BattlefieldMonumentGeometry {
    private BattlefieldMonumentGeometry() {}

    public enum Blade {
        LINEAR(BattlefieldWeaponCatalog.Source.PALE_LINEAR_GREATSWORD, "Бледный линейный двуручник"),
        CLEAVING(BattlefieldWeaponCatalog.Source.SPIRE_TORTURER_GREATSWORD, "Меч Рассечения");
        public final BattlefieldWeaponCatalog.Source source;
        public final String label;
        Blade(BattlefieldWeaponCatalog.Source source, String label) {
            this.source = source; this.label = label;
        }
    }

    public record Style(float scale, float tilt, float yaw, float burial) {
        public Style {
            if (scale < 1 || tilt < 0 || tilt > 45 || Math.abs(yaw) > 15 || burial < 0)
                throw new IllegalArgumentException("Unsafe monumental sword pose");
        }
    }

    private static final Style[] STYLES = {
        new Style(7.5f,  0f, -5f, 1.0f),
        new Style(9.0f,  7f,  8f, 1.2f),
        new Style(8.5f, 13f, -8f, 1.2f),
        new Style(10.0f, 19f, 10f, 1.5f),
        new Style(8.0f, 26f, -12f, 1.3f),
        new Style(9.5f, 32f,  7f, 1.6f),
        new Style(10.0f, 39f, -6f, 1.7f),
        new Style(8.5f, 45f, 11f, 1.6f)
    };

    public static int variants() { return STYLES.length; }
    public static Style style(int variant) { return STYLES[variant]; }

    /**
     * Source tip is on the high X+Y diagonal. Apply -135° tip-down, scale,
     * lean along the local X/Y plane and small Y yaw, then the cardinal scene axis.
     * Returned coordinates are offsets from the monument's base; buried tip can be negative Y.
     */
    private static double[] point(Blade blade, int variant, Direction facing,
                                  double sourceX, double sourceY, double sourceZ) {
        var m = blade.source;
        var p = style(variant);
        double x = m.tipDownX(sourceX, sourceY) * p.scale();
        double y = m.tipDownY(sourceX, sourceY) * p.scale();
        double z = (sourceZ - m.centerZ()) * p.scale() / 16d;
        double lean = Math.toRadians(p.tilt());
        double tx = x * Math.cos(lean) - y * Math.sin(lean);
        double ty = x * Math.sin(lean) + y * Math.cos(lean);
        double yaw = Math.toRadians(180d - facing.toYRot() + p.yaw());
        double wx = tx * Math.cos(yaw) + z * Math.sin(yaw);
        double wz = -tx * Math.sin(yaw) + z * Math.cos(yaw);
        return new double[] {wx, ty - p.burial(), wz};
    }

    /**
     * Conservative narrow swept column of the sword including a thickened crossguard.
     * No false assumption that a giant model fits in its 1-block anchor. Used by
     * operator previews; full worldgen needs its own reservation and chunk boundary logic.
     */
    public static Set<BlockPos> occupiedCells(Blade blade, int variant, Direction facing) {
        var m = blade.source;
        var p = style(variant);
        Set<BlockPos> cells = new HashSet<>();
        // The actual item mesh lies on its long X+Y diagonal, not an axis-aligned block.
        for (int i = 0; i <= 64; i++) {
            double t = i / 64d;
            double sx = m.maxX + (m.minX - m.maxX) * t;
            double sy = m.maxY + (m.minY - m.maxY) * t;
            for (int width = -2; width <= 2; width++) {
                for (int depth = -1; depth <= 1; depth++) {
                    // Perpendicular to source diagonal; enlarged width covers crossguard.
                    double lateral = width * p.scale() * .10d;
                    double dz = depth * p.scale() * .04d;
                    double[] world = point(blade, variant, facing,
                            sx + lateral * 11.3137085d, sy - lateral * 11.3137085d,
                            m.centerZ() + dz * 16d / p.scale());
                    cells.add(new BlockPos((int) Math.floor(world[0]), (int) Math.floor(world[1]),
                            (int) Math.floor(world[2])));
                }
            }
        }
        return Set.copyOf(cells);
    }
}
