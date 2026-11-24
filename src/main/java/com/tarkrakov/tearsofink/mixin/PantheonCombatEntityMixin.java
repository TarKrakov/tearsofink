package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class PantheonCombatEntityMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void tearsofink$separateCombatants(Entity other, CallbackInfo ci) {
        if (!PantheonCombat.permitsInteraction((Entity) (Object) this, other)
                || !PantheonCombat.permitsInteraction(other, (Entity) (Object) this)) ci.cancel();
    }
}
