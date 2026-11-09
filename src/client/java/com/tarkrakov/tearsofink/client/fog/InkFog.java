package com.tarkrakov.tearsofink.client.fog;

import com.tarkrakov.tearsofink.world.ModBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;

/**
 * Густой туман Чернильных пустошей.
 * Работает только на клиенте и только внутри нашего биома.
 * Плавно сгущается при входе в биом и рассеивается при выходе.
 */
public final class InkFog {
	// ===== Настройки (в блоках) — крутить здесь =====
	/** Где туман начинается. */
	public static final float FOG_START = 1.5f;
	/** Дальше этого расстояния не видно ничего. */
	public static final float FOG_END = 10.0f;
	/** Сколько секунд длится переход при входе/выходе из биома. */
	public static final float TRANSITION_SECONDS = 3.0f;

	/** 0 = тумана нет, 1 = туман в полную силу. */
	private static float strength = 0f;
	private static long lastNanos = 0L;

	private InkFog() {}

	/** Обновляет силу тумана. Вызывается при настройке тумана, раз в кадр. */
	public static void tick(Camera camera) {
		long now = System.nanoTime();
		float dt = lastNanos == 0L ? 0f : Math.min((now - lastNanos) / 1_000_000_000f, 0.25f);
		lastNanos = now;

		float target = isInInkWastes(camera) ? 1f : 0f;
		float step = dt / TRANSITION_SECONDS;
		if (strength < target) strength = Math.min(target, strength + step);
		else if (strength > target) strength = Math.max(target, strength - step);
	}

	public static float strength() {
		return strength;
	}

	/** Не мешаем ванильному туману под водой, в лаве, в снегу и при слепоте/тьме. */
	public static boolean shouldApply(Camera camera) {
		if (strength <= 0f) return false;
		if (camera.getFluidInCamera() != FogType.NONE) return false;
		if (camera.getEntity() instanceof LivingEntity living
				&& (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
			return false;
		}
		return true;
	}

	private static boolean isInInkWastes(Camera camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		return mc.level.getBiome(camera.getBlockPosition()).is(ModBiomes.INK_WASTES);
	}
}
