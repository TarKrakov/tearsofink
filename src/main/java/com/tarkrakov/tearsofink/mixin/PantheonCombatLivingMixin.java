package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class PantheonCombatLivingMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void tearsofink$separateEffects(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        Entity target = (Entity) (Object) this;
        if (source != null && !target.level().isClientSide && !PantheonCombat.permitsInteraction(target, source))
            cir.setReturnValue(false);
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void tearsofink$noTrialLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (PantheonCombat.isTrialTarget((Entity) (Object) this)) ci.cancel();
    }
}
