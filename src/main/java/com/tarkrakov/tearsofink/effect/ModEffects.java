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

	/** «Утешение Слёзницы» — от Отвара Слёзницы: штраф за проданную душу вдвое слабее. */
	public static final Holder<MobEffect> TEAR_SOLACE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			TearsOfInk.id("tear_solace"), new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x9FE3E0));

	/** «Сияние» — светлый щит активен: входящий урон меньше. Значок + щит светлеет. */
	public static final Holder<MobEffect> RADIANCE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			TearsOfInk.id("radiance"), new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF2F0D8));

	/** «Милосердие» — аура Щита Милосердия включена. */
	public static final Holder<MobEffect> MERCY = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			TearsOfInk.id("mercy"), new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF4C9D6));

	private ModEffects() {}

	public static void init() {}

	/** Эффект, который ничего не делает сам — только показывает значок. */
	private static class MarkerEffect extends MobEffect {
		MarkerEffect(MobEffectCategory category, int color) {
			super(category, color);
		}
	}
}
