package com.tarkrakov.tearsofink.ink;

import com.tarkrakov.tearsofink.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Цена проданной души: за каждые 5 проданных еда насыщает и зелья лечат слабее.
 * Действует только внутри Чернильных Земель (снаружи силы и штрафы спят).
 */
public final class SoulPenalty {
	// ===== Настройки — крутить здесь =====
	/** На сколько слабее еда и лечение за каждый проданный шаг души (0.04 = 4%). */
	public static final float PER_STEP = 0.04f;
	/** Высокая Душа (обеты): на столько сильнее еда и лечение за каждые +5 сверх 100. */
	public static final float HIGH_PER_STEP = 0.04f;
	/** Во сколько раз слабее штраф под Отваром Слёзницы. */
	public static final float SOLACE_FACTOR = 0.5f;

	private SoulPenalty() {}

	/** С этой стадии (максимум Души ≤ этого) сырое мясо гусеницы насыщает лучше жареного. */
	public static final int RAW_MEAT_STAGE = 70;

	/** Сырое мясо гусеницы на стадии «Порченый» и ниже: как жареное и даже лучше. */
	public static net.minecraft.world.food.FoodProperties rawMeatBonus(net.minecraft.world.entity.player.Player entity,
			net.minecraft.world.item.ItemStack stack, net.minecraft.world.food.FoodProperties food) {
		if (!(entity instanceof ServerPlayer player) || !stack.is(com.tarkrakov.tearsofink.item.ModItems.RAW_CATERPILLAR_MEAT)) return food;
		InkData data = InkPowers.get(player);
		if (!data.inside() || data.soulMax() > RAW_MEAT_STAGE) return food;
		return new net.minecraft.world.food.FoodProperties(FoodBalance.CORRUPTED_MEAT_NUTRITION, FoodBalance.CORRUPTED_MEAT_SATURATION, food.canAlwaysEat(), food.eatSeconds(),
				food.usingConvertsTo(), food.effects());
	}

	/** Множитель еды и лечения для этого существа: 1 — без штрафа, 0.6 — при 50 душах, 1.4 — при 150. */
	public static float multiplier(LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) return 1f;
		InkData data = InkPowers.get(player);
		if (!data.inside()) return 1f;
		int steps = (data.soulMax() - InkData.SOUL_START) / InkData.SOUL_STEP; // + сверх 100, − ниже
		if (steps > 0) return 1f + HIGH_PER_STEP * steps;
		if (steps >= 0) return 1f;
		float penalty = PER_STEP * -steps;
		// Отвар Слёзницы — штраф вдвое слабее
		if (player.hasEffect(ModEffects.TEAR_SOLACE)) penalty *= SOLACE_FACTOR;
		return Math.max(0f, 1f - penalty);
	}
}
