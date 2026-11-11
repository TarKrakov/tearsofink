package com.tarkrakov.tearsofink.effect;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Эффекты мода (значки среди ванильных эффектов). */
public final class ModEffects {
	/**
	 * «Утрата души» — значок-напоминание: продана душа, еда и лечение слабее.
	 * Уровень I–X = сколько шагов по 5 продано. Сам штраф считает {@link com.tarkrakov.tearsofink.ink.SoulPenalty}.
	 */
	public static final Holder<MobEffect> SOUL_LOSS = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			TearsOfInk.id("soul_loss"), new MarkerEffect(MobEffectCategory.HARMFUL, 0x3B2F4D));

	private ModEffects() {}

	public static void init() {}

	/** Эффект, который ничего не делает сам — только показывает значок. */
	private static class MarkerEffect extends MobEffect {
		MarkerEffect(MobEffectCategory category, int color) {
			super(category, color);
		}
	}
}
