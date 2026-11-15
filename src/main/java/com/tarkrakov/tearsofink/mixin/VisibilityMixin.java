package com.tarkrakov.tearsofink.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Свет и тьма путей (vow/Aura): светящегося Путь Души мобы замечают дальше, проклятого Чернилами — ближе.
 * Тот же множитель, на котором в игре работает подкрадывание. Только игроки в Землях (Aura сама проверяет).
 */
@Mixin(LivingEntity.class)
public abstract class VisibilityMixin {
	@Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
	private void tearsofink$auraVisibility(Entity looker, CallbackInfoReturnable<Double> cir) {
		double m = com.tarkrakov.tearsofink.vow.Aura.visibility((LivingEntity) (Object) this, looker);
		if (m != 1) cir.setReturnValue(cir.getReturnValue() * m);
	}
}
