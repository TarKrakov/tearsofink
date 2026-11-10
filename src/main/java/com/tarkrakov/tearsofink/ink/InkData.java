package com.tarkrakov.tearsofink.ink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Душа и Чернила игрока. Хранится на игроке, переживает выход из игры и смерть.
 *
 * @param soulMax  максимум Души (100 — ничего не продано, 50 — продано всё)
 * @param soul     текущая Душа (тратится оружием Души, восстанавливается до soulMax)
 * @param inkBonus прибавка к запасу Чернил от фонтанов и сердец (чернила за проданную душу считаются отдельно)
 * @param ink      текущие Чернила (тратятся рывком и оружием Чернил, восстанавливаются до {@link #inkMax()})
 * @param inside   игрок сейчас в Чернильных Землях (силы работают)
 */
public record InkData(int soulMax, float soul, int inkBonus, float ink, boolean inside) {
	// ===== Настройки — крутить здесь =====
	/** Душа на старте. */
	public static final int SOUL_START = 100;
	/** Ниже этого душу продать нельзя. */
	public static final int SOUL_MIN = 50;
	/** Душа продаётся шагами по столько. */
	public static final int SOUL_STEP = 5;
	/** Чернила на старте. */
	public static final int INK_START = 15;
	/** Сколько запаса Чернил даёт каждый проданный шаг души. */
	public static final int INK_PER_SOUL_STEP = 10;
	/** Потолок запаса Чернил — выше не поднять ничем. */
	public static final int INK_CAP = 200;
	/** Восстановление в секунду. */
	public static final float INK_REGEN_PER_SECOND = 1.0f;
	public static final float SOUL_REGEN_PER_SECOND = 1.0f;

	public static final Codec<InkData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("soul_max").forGetter(InkData::soulMax),
			Codec.FLOAT.fieldOf("soul").forGetter(InkData::soul),
			Codec.INT.fieldOf("ink_bonus").forGetter(InkData::inkBonus),
			Codec.FLOAT.fieldOf("ink").forGetter(InkData::ink),
			Codec.BOOL.optionalFieldOf("inside", false).forGetter(InkData::inside)
	).apply(i, InkData::new));

	public static InkData fresh() {
		return new InkData(SOUL_START, SOUL_START, 0, INK_START, false);
	}

	/** Сколько шагов души продано. */
	public int soldSteps() {
		return (SOUL_START - soulMax) / SOUL_STEP;
	}

	/** Запас Чернил: старт + фонтаны и сердца + проданная душа, но не выше потолка. */
	public int inkMax() {
		return Math.min(INK_CAP, INK_START + inkBonus + soldSteps() * INK_PER_SOUL_STEP);
	}

	/** Приводит значения в допустимые рамки. */
	public InkData clamped() {
		int sMax = Math.max(SOUL_MIN, Math.min(SOUL_START, soulMax));
		int bonus = Math.max(0, inkBonus);
		InkData d = new InkData(sMax, soul, bonus, ink, inside);
		return new InkData(sMax, Math.max(0, Math.min(sMax, soul)), bonus,
				Math.max(0, Math.min(d.inkMax(), ink)), inside);
	}

	public InkData withSoulMax(int value) {
		return new InkData(value, soul, inkBonus, ink, inside).clamped();
	}

	public InkData withSoul(float value) {
		return new InkData(soulMax, value, inkBonus, ink, inside).clamped();
	}

	public InkData withInkBonus(int value) {
		return new InkData(soulMax, soul, value, ink, inside).clamped();
	}

	public InkData withInk(float value) {
		return new InkData(soulMax, soul, inkBonus, value, inside).clamped();
	}

	public InkData withInside(boolean value) {
		return new InkData(soulMax, soul, inkBonus, ink, value);
	}

	/** Полностью восстановленные шкалы. */
	public InkData refilled() {
		return new InkData(soulMax, soulMax, inkBonus, inkMax(), inside);
	}
}
