package com.tarkrakov.tearsofink.weapon.blood;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Кровотечение: раз в секунду урон по ступени (I–III); часть урона лечит того, кто его наложил. */
public class BleedingEffect extends MobEffect {
	public BleedingEffect() {
		super(MobEffectCategory.HARMFUL, 0x8A0F1A);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		Bleeding.tick(entity, amplifier);
		return true;
	}
}
