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

    public static float tick(ClientLevel level, Vec3 camera, float dt) {
        double target = 0;
        if (enabled && ModDimensions.isLands(level)) {
            double u = (camera.x - cx) / 79d;
            double v = (camera.z - cz) / 70d;
            double angle = Math.atan2(v, u);
            double boundary = 1.0 + .085 * Math.sin(3 * angle + (seed & 1023) * .031)
                    + .057 * Math.sin(7 * angle - (seed >>> 10 & 1023) * .028)
                    + .030 * Math.cos(11 * angle + (seed >>> 20 & 1023) * .022);
            target = Math.max(0, Math.min(1, (boundary - Math.hypot(u, v)) / .25));
        }
        // This has its OWN fast blend; the old biome fog lerp (2 blocks/s)
        // must not make the field take 40 seconds to become visible.
        strength += ((float) target - strength) * Math.min(1f, dt * 1.45f);
        return strength;
    }
}
