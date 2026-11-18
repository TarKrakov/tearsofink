package com.tarkrakov.tearsofink.weapon.shield;

import com.tarkrakov.tearsofink.weapon.SoulShieldItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Audio-only settings. Never use these modifiers for combat calculations. */
public final class ShieldAudio {
    private ShieldAudio() {}

    public static float power(Player p) {
        if (ShieldStance.dual(p)) return SoulShieldItem.pairPower(p);
        if (p.getUseItem().getItem() instanceof SoulShieldItem sh) return sh.power();
        if (p.getMainHandItem().getItem() instanceof SoulShieldItem sh) return sh.power();
        if (p.getOffhandItem().getItem() instanceof SoulShieldItem sh) return sh.power();
        return 1f;
    }

    public static float volume(float base, float power) { return base * (0.55f + 0.45f * power); }
    public static float pitch(float power) { return 0.70f + 0.30f * power; }

    public static void play(ServerPlayer p, SoundEvent sound, float volume) {
        at(p, p, sound, volume);
    }

    public static void at(ServerPlayer p, Entity source, SoundEvent sound, float volume) {
        withPower(p, source, sound, volume, power(p));
    }

    public static void withPower(ServerPlayer p, Entity source, SoundEvent sound, float volume, float power) {
        if (p.isSilent()) return;
        p.serverLevel().playSound(null, source.getX(), source.getY() + source.getBbHeight() * 0.55, source.getZ(),
                sound, SoundSource.PLAYERS, volume(volume, power), pitch(power));
    }
}
