package com.tarkrakov.tearsofink.ink;

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

	private SoulPenalty() {}

	/** Множитель еды и лечения для этого существа: 1 — без штрафа, 0.6 — при 50 душах. */
	public static float multiplier(LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) return 1f;
		InkData data = InkPowers.get(player);
		if (!data.inside() || data.soldSteps() <= 0) return 1f;
		return Math.max(0f, 1f - PER_STEP * data.soldSteps());
	}
}
