package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
abstract class PantheonCombatMobMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void tearsofink$guardTrialTarget(LivingEntity target, CallbackInfo ci) {
        if (target != null && !PantheonCombat.permitsInteraction(target, (Mob) (Object) this)) ci.cancel();
    }
}
