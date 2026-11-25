package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.battlefield.BattlefieldMemoryMotes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.WhiteAshParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the original biome's minecraft:white_ash and its 0.04 probability
 * unchanged everywhere outside a *confirmed* ancient battlefield.
 * Uses the exact same irregular 0..1 edge mask as the rising memory motes.
 * It does NOT touch ash, spores, rain, or other mods' particle types.
 */
@Mixin(ParticleEngine.class)
public abstract class BattlefieldWhiteAshMixin {
    @Shadow @Final private RandomSource random;
    @Shadow protected ClientLevel level;

    @Inject(
            method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void toi$replaceWhiteAsh(ParticleOptions options,
                                     double x, double y, double z,
                                     double xd, double yd, double zd,
                                     CallbackInfoReturnable<Particle> cir) {
        if (options.getType() != ParticleTypes.WHITE_ASH) return;
        float field = BattlefieldMemoryMotes.opennessAt(level, x, z);
        if (field >= 1f || (field > 0f && random.nextFloat() < field))
            cir.setReturnValue(null);
    }

    // Existing falling particles must also fade on crossing the field edge;
    // spawn suppression alone would leave them drifting there for seconds.
    @Inject(method = "tickParticle(Lnet/minecraft/client/particle/Particle;)V",
            at = @At("TAIL"))
    private void toi$fadeWhiteAsh(Particle particle, CallbackInfo ci) {
        if (!(particle instanceof WhiteAshParticle)) return;
        var access = (BattlefieldParticleAccess) particle;
        float field = BattlefieldMemoryMotes.opennessAt(level, access.toi$getX(), access.toi$getZ());
        access.toi$setAlpha(1f - field);
    }
}
