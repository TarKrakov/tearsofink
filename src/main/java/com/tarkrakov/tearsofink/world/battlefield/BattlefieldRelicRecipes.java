package com.tarkrakov.tearsofink.world.battlefield;

import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;

/** Deterministic ancient weapon beds, all meshes are pre-existing aged author art.
 * Fewer anchors than blades: a single inert scene renders 7..18 relics.
 */
public final class BattlefieldRelicRecipes {
    public record Relic(int mesh, float forward, float right, float sink,
                        float yaw, float pitch, float roll) {
        public boolean shield() { return mesh >= 8 && mesh < 12; }
        public boolean projectile() { return mesh == 12 || mesh == 13; }
    }
    private BattlefieldRelicRecipes() {}
    private static long mix(long n) {
        n ^= n >>> 30; n *= 0xbf58476d1ce4e5b9L;
        n ^= n >>> 27; n *= 0x94d049bb133111ebL;
        return n ^ n >>> 31;
    }
    private static double unit(long n) { return (mix(n) >>> 11) * 0x1.0p-53; }

    /**
     * Styles are geographical roles rather than uniform random picks:
     * 0 embedded blade grove, 1 abandoned weapons, 2 battle crush,
     * 3 shield-bearing defenders, 4 sparse peripheral traces.
     */
    public static List<Relic> generate(BlockPos at, int style, int variant) {
        long seed = mix(at.asLong() ^ (style * 0x9e3779b97f4a7c15L)
                ^ (variant * 0x632be59bd9b4e019L));
        int count = switch (style) {
            case 0 -> 14 + (variant & 3);
            case 1 -> 10 + (variant & 3);
            case 2 -> 16 + (variant & 3);
            case 3 -> 12 + (variant & 3);
            case 5 -> 17 + (variant & 3);
            case 6 -> 14 + (variant & 3);
            case 7 -> 19 + (variant & 3);
            default -> 7 + (variant & 3);
        };
        List<Relic> result = new ArrayList<>(count);
        if (style >= 5) {
            // Original styles 0..4 stay deterministic for saved Contacts arrays.
            // New styles reuse a single inert anchor for an entire volley.
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * unit(seed + i * 13L + 1);
                double distance = 1.0 + 5.6 * Math.sqrt(unit(seed + i * 13L + 2));
                float forward = (float) (Math.cos(angle) * distance);
                float right = (float) (Math.sin(angle) * distance * (style == 5 ? .48 : 1.0));
                int mesh = style == 5 ? 12 : style == 6 ? 13
                        : (unit(seed + i * 13L + 3) < .55 ? 12 : 13);
                float yaw = (float) (unit(seed + i * 13L + 4) * 300 - 150);
                float pitch = (float) (7 + unit(seed + i * 13L + 5) * 71);
                float roll = (float) (unit(seed + i * 13L + 6) * 38 - 19);
                result.add(new Relic(mesh, forward, right, -.14f, yaw, pitch, roll));
            }
            return List.copyOf(result);
        }
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * unit(seed + i * 11L + 1);
            double distance = 1.7 + 7.0 * Math.sqrt(unit(seed + i * 11L + 2));
            // A long, broken line rather than a uniform disk in the battle crush.
            double forward = Math.cos(angle) * distance;
            double right = Math.sin(angle) * distance * (style == 2 ? .61 : 1);
            int source = (int) Math.floor(unit(seed + i * 11L + 3) * 8);
            int mesh;
            if (style == 3 && i % 6 == 0) {
                mesh = 8 + (int) Math.floor(unit(seed + i * 11L + 4) * 4);
            } else {
                // Different stretches remember their weapons, not an equal rainbow.
                mesh = switch (style) {
                    case 0 -> new int[]{1, 4, 0, 7, 1, 4, 5, 0}[source];
                    case 1 -> new int[]{6, 7, 3, 0, 4, 5, 2, 1}[source];
                    case 2 -> new int[]{0, 1, 3, 4, 5, 6, 7, 2}[source];
                    case 3 -> new int[]{0, 1, 1, 4, 0, 5, 6, 7}[source];
                    default -> new int[]{0, 3, 1, 6, 2, 7, 5, 4}[source];
                };
            }
            boolean shield = mesh >= 8;
            float yaw = (float) (unit(seed + i * 11L + 5) * 300 - 150);
            // Fallen edges and upright weapons coexist; mostly fallen in the
            // rear and mostly embedded around the blade groves.
            float pitch = shield
                    ? (float) (20 + unit(seed + i * 11L + 6) * 65)
                    : switch (style) {
                        case 0 -> (float) (8 + unit(seed + i * 11L + 6) * 35);
                        case 1 -> (float) (56 + unit(seed + i * 11L + 6) * 30);
                        case 2 -> (float) (18 + unit(seed + i * 11L + 6) * 68);
                        case 3 -> (float) (24 + unit(seed + i * 11L + 6) * 54);
                        default -> (float) (46 + unit(seed + i * 11L + 6) * 40);
                    };
            float roll = (float) ((unit(seed + i * 11L + 7) * 60 - 30));
            float requested = shield ? -.12f : -.09f;
            float sink = shield ? requested
                    : BattlefieldWeaponCatalog.Source.values()[mesh].groundSink(requested, pitch, roll);
            result.add(new Relic(mesh, (float)forward, (float)right, sink, yaw, pitch, roll));
        }
        return List.copyOf(result);
    }
}
