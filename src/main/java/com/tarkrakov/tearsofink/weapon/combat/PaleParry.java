package com.tarkrakov.tearsofink.weapon.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared server cooldown across both Pale blades, independent of the chosen loadout. */
public final class PaleParry {
    public static final int WINDOW_TICKS = 8, COOLDOWN_TICKS = 20;
    private static final Map<ServerPlayer, Long> READY = new WeakHashMap<>();

    private PaleParry() {}
    public static boolean active(long now, long until) { return now < until; }
    public static boolean ready(long now, long readyAt) { return now >= readyAt; }

    static boolean tryStart(ServerPlayer p) {
        long now = p.server.getTickCount();
        if (!ready(now, READY.getOrDefault(p, 0L))) return false;
        READY.put(p, now + COOLDOWN_TICKS);
        return true;
    }

    static int[] hud(ServerPlayer p, boolean active) {
        long now = p.server.getTickCount();
        return new int[]{(int)Math.max(0,READY.getOrDefault(p,0L)-now),COOLDOWN_TICKS,active?2:0};
    }

    static void success(ServerPlayer p) {
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.ANVIL_LAND,SoundSource.PLAYERS,.4f,1.9f);
        var at = p.getEyePosition().add(p.getLookAngle().scale(.7));
        p.serverLevel().sendParticles(ParticleTypes.CRIT,at.x,at.y,at.z,10,.2,.2,.2,.06);
    }
}
