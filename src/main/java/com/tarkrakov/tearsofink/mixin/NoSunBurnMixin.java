package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** В Чернильных Землях ночные монстры (зомби, скелеты, фантомы) не горят днём — они здесь особые. */
@Mixin(Mob.class)
public abstract class NoSunBurnMixin {
	@Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
	private void tearsofink$noBurnInLands(CallbackInfoReturnable<Boolean> cir) {
		if (ModDimensions.hasLandsRules(((Mob) (Object) this).level())) cir.setReturnValue(false);
	}
}
