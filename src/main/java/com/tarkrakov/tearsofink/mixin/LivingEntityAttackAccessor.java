package com.tarkrakov.tearsofink.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Шкала атаки: после некоторых ударов связки она заполняется быстрее (speed). */
@Mixin(LivingEntity.class)
public interface LivingEntityAttackAccessor {
	@Accessor("attackStrengthTicker")
	void tearsofink$setAttackStrengthTicker(int ticks);

	@Accessor("attackStrengthTicker")
	int tearsofink$getAttackStrengthTicker();
}
