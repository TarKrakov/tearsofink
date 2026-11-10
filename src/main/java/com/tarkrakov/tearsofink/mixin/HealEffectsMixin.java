package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tarkrakov.tearsofink.ink.SoulPenalty;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Проданная душа: зелья лечения и эффект регенерации лечат слабее. */
@Mixin(targets = {"net.minecraft.world.effect.HealOrHarmMobEffect", "net.minecraft.world.effect.RegenerationMobEffect"})
public abstract class HealEffectsMixin {
	@WrapOperation(method = "*", require = 1,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
	private void tearsofink$soulPenalty(LivingEntity entity, float amount, Operation<Void> original) {
		original.call(entity, amount * SoulPenalty.multiplier(entity));
	}
}
