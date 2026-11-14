package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Мобы не выбирают целью того, кто стоит в храме (мирная зона). */
@Mixin(LivingEntity.class)
public abstract class SanctuaryAttackMixin {
	@Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
	private void tearsofink$sanctuary(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
		if (!Sanctuary.mayAttack((LivingEntity) (Object) this, target)) cir.setReturnValue(false);
	}
}
