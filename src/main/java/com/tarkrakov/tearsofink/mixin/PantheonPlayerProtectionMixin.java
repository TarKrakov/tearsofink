package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PantheonPlayerProtectionMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void tearsofink$keepPantheonEquipment(CallbackInfo ci) {
        if (PantheonTemple.isTemple(((Player) (Object) this).level())) ci.cancel();
    }

    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void tearsofink$keepPantheonExperience(CallbackInfoReturnable<Integer> cir) {
        if (PantheonTemple.isTemple(((Player) (Object) this).level())) cir.setReturnValue(0);
    }
}
