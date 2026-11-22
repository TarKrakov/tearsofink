package com.tarkrakov.tearsofink.client.fog;

import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldDiscovery;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** The battle's dead stillness opens a window through the pale forest fog. */
public final class BattlefieldClarity {
    private static int cx, cz;
    private static long seed;
    private static boolean enabled;
    private static float strength;
    private BattlefieldClarity() {}

    public static void receive(BattlefieldDiscovery.FieldSync state) {
        cx = state.x(); cz = state.z(); seed = state.seed(); enabled = state.active();
    }
    public static void reset() { enabled = false; strength = 0; }

    /**
     * Exact server-side irregular field mask at the PARTICLE's own position,
     * not the camera position. This leaves the encircling forest thick with fog
     * even as the old drifting particles inside the field quickly fade out.
     */
    public static float opennessAt(double x, double z) {
        if (!enabled) return 0f;
        double u = (x - cx) / 79d;
        double v = (z - cz) / 70d;
        double angle = Math.atan2(v, u);
        double boundary = 1.0 + .085 * Math.sin(3 * angle + (seed & 1023) * .031)
                + .057 * Math.sin(7 * angle - (seed >>> 10 & 1023) * .028)
                + .030 * Math.cos(11 * angle + (seed >>> 20 & 1023) * .022);
        return (float) Math.max(0, Math.min(1, (boundary - Math.hypot(u, v)) / .24));
    }

    /**
     * Stable seed-dependent islands of drifting wisps, not uniformly
     * scattered particles. At most a few small 6-8 block pockets in the
     * exposed field; position-only, no ticks or per-chunk persistent data.
     */
    public static float wispPatchAt(double x, double z) {
        float open = opennessAt(x, z);
        if (!enabled || open < .57f) return 0f;
        final int cellSize = 20;
        int cellX = (int) Math.floor((x - cx) / cellSize);
        int cellZ = (int) Math.floor((z - cz) / cellSize);
        float coverage = 0f;
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
            int ix = cellX + i, iz = cellZ + j;
            long hash = seed ^ (ix * 341873128712L) ^ (iz * 132897987541L);
            hash ^= hash >>> 30;
            hash *= 0xbf58476d1ce4e5b9L;
            hash ^= hash >>> 27;
            hash *= 0x94d049bb133111ebL;
            hash ^= hash >>> 31;
            if ((hash & 7L) >= 3L) continue;
            double ox = ((hash >>> 4) & 255L) / 255d;
            double oz = ((hash >>> 12) & 255L) / 255d;
            double centerX = cx + (ix + .18 + .64 * ox) * cellSize;
            double centerZ = cz + (iz + .18 + .64 * oz) * cellSize;
            double radius = 5.2 + ((hash >>> 20) & 255L) / 255d * 2.7;
            double dist = Math.hypot(x - centerX, z - centerZ);
            if (dist >= radius) continue;
            float t = (float) (1d - dist / radius);
            coverage = Math.max(coverage, t * t * (3f - 2f * t));
        }
        return coverage * Math.min(1f, (open - .57f) * 6f);
    }

    /** Prewarm emerging soil motes just BEFORE the player reaches the edge. */
    public static boolean nearField(double x, double z, double margin) {
        return enabled && Math.abs(x - cx) <= 79d + margin
                && Math.abs(z - cz) <= 70d + margin;
    }

    public static float particleMultiplier(double x, double z) {
        // A rare residual wisp inside, a natural fog wall at the outside edge.
        float openness = opennessAt(x, z);
        return 1f - 0.97f * openness * openness * (3f - 2f * openness);
    }

    public static float tick(ClientLevel level, Vec3 camera, float dt) {
        double target = enabled && ModDimensions.isLands(level)
                ? opennessAt(camera.x, camera.z) : 0f;
        // This has its OWN fast blend; the old biome fog lerp (2 blocks/s)
        // must not make the field take 40 seconds to become visible.
        strength += ((float) target - strength) * Math.min(1f, dt * 1.45f);
        return strength;
    }
}
