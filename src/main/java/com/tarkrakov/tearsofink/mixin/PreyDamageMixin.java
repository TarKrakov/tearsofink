package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.hunt.Hunt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Урон цели Охоты умножается одинаково для всех: ванильные удары, стрелы, удары солдат. */
@Mixin(LivingEntity.class)
public abstract class PreyDamageMixin {
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float tearsofink$preyDamage(float amount, DamageSource source) {
		return amount * Hunt.outgoingMultiplier(source.getEntity());
	}
}
