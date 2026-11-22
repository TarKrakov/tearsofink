package com.tarkrakov.tearsofink.client.fog;

import com.tarkrakov.tearsofink.world.ModBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;

/**
 * Густой туман Чернильных пустошей.
 * Сила тумана зависит от близости к биому: снаружи он сгущается по мере приближения,
 * внутри — доходит до полной силы в нескольких блоках от границы.
 */
public final class InkFog {
	// ===== Настройки — крутить здесь =====
	/** Где туман начинается (в блоках) при полной силе. */
	public static final float FOG_START = 1.5f;
	/** Дальность видимости (в блоках) при полной силе — для каждой зоны своя. */
	public static final float FOG_END_PALE = 10.0f;
	public static final float FOG_END_BLOOD = 12.0f;
	public static final float FOG_END_INK = 8.0f;
	public static final float FOG_END_LAKE = 10.0f;
	/** Как быстро меняется дальность при переходе между зонами (блоков в секунду). */
	public static final float FOG_END_CHANGE_PER_SECOND = 2.0f;
	/** Видимость в самой густой мгле за краем Земель. */
	public static final float EDGE_FOG_END = 2.0f;
	/** 0 — внутри Земель, 1 — глубоко в стене тумана: там ясность от фонарей не действует. */
	private static float edgeFactor = 0f;
	private static final net.minecraft.resources.ResourceKey<Biome> VESTIBULE = net.minecraft.resources.ResourceKey.create(
			net.minecraft.core.registries.Registries.BIOME, com.tarkrakov.tearsofink.TearsOfInk.id("vestibule"));
	/** За сколько блоков ДО границы туман начинает сгущаться. */
	public static final int OUTSIDE_RAMP = 40;
	/** Сколько блоков ВНУТРИ биома нужно пройти до полной силы тумана. */
	public static final int INSIDE_RAMP = 12;
	/** Какая сила тумана ровно на границе (0..1). */
	public static final float STRENGTH_AT_BORDER = 0.75f;
	/** Как быстро сила тумана догоняет нужную (в долях за секунду). */
	public static final float SMOOTHING_PER_SECOND = 0.6f;

	private static final int DIRECTIONS = 16;
	private static final long SAMPLE_INTERVAL_NANOS = 200_000_000L;

	/** 0 = тумана нет, 1 = туман в полную силу. */
	private static float strength = 0f;
	private static float fullEnd = FOG_END_PALE;
	private static float fullEndTarget = FOG_END_PALE;
	/** Видимость от фонарей (мировых и ручного), плавно догоняет нужную. */
	private static float clearEnd = 0f;
	private static float battlefieldClarity = 0f;
	/** Как быстро туман расступается у фонаря (доля в секунду). */
	public static final float CLEAR_SMOOTHING_PER_SECOND = 1.5f;
	private static float target = 0f;
	private static long lastNanos = 0L;
	private static long lastSampleNanos = 0L;

	private InkFog() {}

	/** Обновляет силу тумана. Вызывается раз в кадр. */
	public static void tick(Camera camera) {
		long now = System.nanoTime();
		float dt = lastNanos == 0L ? 0f : Math.min((now - lastNanos) / 1_000_000_000f, 0.25f);
		lastNanos = now;

		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			strength = target = 0f;
			com.tarkrakov.tearsofink.client.fog.BattlefieldClarity.reset();
			return;
		}
		if (now - lastSampleNanos > SAMPLE_INTERVAL_NANOS) {
			lastSampleNanos = now;
			target = computeTarget(level, camera.getBlockPosition());
			if (level.hasChunkAt(camera.getBlockPosition())) {
				Float end = zoneFogEnd(level.getBiome(camera.getBlockPosition()));
				if (end != null) fullEndTarget = end;
				// за краем Земель туман сгущается до непроглядного (дальше — петля назад); фонари его не рассеивают
				edgeFactor = 0f;
				if (com.tarkrakov.tearsofink.world.ModDimensions.isLands(level)) {
					double depth = com.tarkrakov.tearsofink.world.InkLandsLayout.edgeDepth(camera.getPosition().x, camera.getPosition().z);
					if (depth < 0) {
						float t = (float) Math.min(1.0, -depth / (com.tarkrakov.tearsofink.world.InkLandsBorder.LOOP_DEPTH * 0.8));
						fullEndTarget = net.minecraft.util.Mth.lerp(t, fullEndTarget, EDGE_FOG_END);
						edgeFactor = t;
					}
				}
			}
			LanternClarity.sample(level, camera.getPosition());
		}
		float clearTarget = LanternClarity.targetFogEnd(fullEnd);
		clearEnd += (clearTarget - clearEnd) * Math.min(1f, dt * CLEAR_SMOOTHING_PER_SECOND);
		float endStep = dt * FOG_END_CHANGE_PER_SECOND;
		if (fullEnd < fullEndTarget) fullEnd = Math.min(fullEndTarget, fullEnd + endStep);
		else if (fullEnd > fullEndTarget) fullEnd = Math.max(fullEndTarget, fullEnd - endStep);

		battlefieldClarity = com.tarkrakov.tearsofink.client.fog.BattlefieldClarity.tick(
				level, camera.getPosition(), dt);
		float step = dt * SMOOTHING_PER_SECOND;
		if (strength < target) strength = Math.min(target, strength + step);
		else if (strength > target) strength = Math.max(target, strength - step);
	}

	public static float strength() {
		return strength;
	}

	/** Не мешаем ванильному туману под водой, в лаве, в снегу и при слепоте/тьме. */
	/** Туман выключен командой /inkfog (для строительства). Только у этого игрока, до перезапуска игры. */
	public static boolean disabled = false;

	public static boolean shouldApply(Camera camera) {
		if (disabled || strength <= 0f) return false;
		if (camera.getFluidInCamera() != FogType.NONE) return false;
		return !(camera.getEntity() instanceof LivingEntity living
				&& (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)));
	}

	/** Дальность видимости для данной силы тумана: плавно от ванильной до нашей. */
	public static float fogEnd(float vanillaEnd, float renderDistance) {
		float clear = Math.max(clearEnd * (1f - edgeFactor),
				78f * battlefieldClarity * (1f - edgeFactor));
		// A finite, local widening inside the generated battle; no globally clearer Pale Forest.
		float full = Math.min(renderDistance, Math.max(fullEnd, clear));
		if (vanillaEnd <= full) return vanillaEnd;
		// Экспоненциальная интерполяция: туман на глаз сгущается равномерно.
		return (float) (vanillaEnd * Math.pow(full / vanillaEnd, strength));
	}

	public static boolean isInk(ClientLevel level, BlockPos pos) {
		return level.hasChunkAt(pos) && level.getBiome(pos).is(ModBiomes.INK_LANDS);
	}

	/** Дальность тумана для зоны или null, если это не Чернильные Земли. */
	public static Float zoneFogEnd(Holder<Biome> biome) {
		if (biome.is(VESTIBULE)) return 9f;
		if (biome.is(ModBiomes.EDGE)) return 4f;
		if (biome.is(ModBiomes.INKWOOD)) return FOG_END_INK;
		if (biome.is(ModBiomes.BLOODWOOD)) return FOG_END_BLOOD;
		if (biome.is(ModBiomes.TAR_LAKE)) return FOG_END_LAKE;
		if (biome.is(ModBiomes.PALE_FOREST)) return FOG_END_PALE;
		return null;
	}

	private static float computeTarget(ClientLevel level, BlockPos center) {
		// Земли — своё измерение: туман там везде
		if (com.tarkrakov.tearsofink.world.ModDimensions.isLands(level) || com.tarkrakov.tearsofink.prologue.Vestibule.is(level)) return 1f;
		if (isInk(level, center)) {
			// Внутри: как глубоко мы зашли (расстояние до ближайшей «не-пустоши»)
			int depth = nearest(level, center, INSIDE_RAMP, 4, false);
			return STRENGTH_AT_BORDER + (1f - STRENGTH_AT_BORDER) * depth / INSIDE_RAMP;
		}
		// Снаружи: как близко пустоши
		int distance = nearest(level, center, OUTSIDE_RAMP, 8, true);
		if (distance >= OUTSIDE_RAMP) return 0f;
		return STRENGTH_AT_BORDER * (1f - (float) distance / OUTSIDE_RAMP);
	}

	/** Ближайшее расстояние (с шагом step), на котором биом «является/не является» пустошами. */
	private static int nearest(ClientLevel level, BlockPos center, int maxRadius, int step, boolean lookingForInk) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int r = step; r <= maxRadius; r += step) {
			for (int i = 0; i < DIRECTIONS; i++) {
				double angle = Math.PI * 2 * i / DIRECTIONS;
				pos.set(center.getX() + Math.round(Math.cos(angle) * r), center.getY(),
						center.getZ() + Math.round(Math.sin(angle) * r));
				if (isInk(level, pos) == lookingForInk) return r;
			}
		}
		return maxRadius;
	}
}
