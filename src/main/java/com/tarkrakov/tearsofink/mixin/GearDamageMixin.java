package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.weapon.forge.Gear;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Ступень нашего оружия (×1 / ×1,15 / ×1,3) и дебафф ванильного (×0,7) — только в Землях. */
@Mixin(LivingEntity.class)
public abstract class GearDamageMixin {
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float tearsofink$gearDamage(float amount, DamageSource source) {
		return Gear.outgoing((LivingEntity) (Object) this, source, amount);
	}
}
