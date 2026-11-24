package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
abstract class PantheonCombatProjectileMixin {
    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void tearsofink$guardTrialProjectile(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!target.level().isClientSide && !PantheonCombat.permitsInteraction(target, (Projectile) (Object) this))
            cir.setReturnValue(false);
    }
}
