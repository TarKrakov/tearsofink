package com.tarkrakov.tearsofink.effect;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import com.tarkrakov.tearsofink.block.ModBlocks;

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

	// ===== Зелья (варочная стойка) =====
	/** Отвар Слёзницы: 3 минуты; с редстоуном — 8 минут. */
	public static final Holder<Potion> TEARBLOOM_BREW = Registry.registerForHolder(BuiltInRegistries.POTION,
			TearsOfInk.id("tearbloom_brew"), new Potion("tearbloom_brew", new MobEffectInstance(TEAR_SOLACE, 3 * 60 * 20)));
	public static final Holder<Potion> LONG_TEARBLOOM_BREW = Registry.registerForHolder(BuiltInRegistries.POTION,
			TearsOfInk.id("long_tearbloom_brew"), new Potion("tearbloom_brew", new MobEffectInstance(TEAR_SOLACE, 8 * 60 * 20)));

	private ModEffects() {}

	public static void init() {
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
			builder.addMix(Potions.AWKWARD, ModBlocks.TEARBLOOM.asItem(), TEARBLOOM_BREW);
			builder.addMix(TEARBLOOM_BREW, Items.REDSTONE, LONG_TEARBLOOM_BREW);
		});
	}

	/** Эффект, который ничего не делает сам — только показывает значок. */
	private static class MarkerEffect extends MobEffect {
		MarkerEffect(MobEffectCategory category, int color) {
			super(category, color);
		}
	}
}
