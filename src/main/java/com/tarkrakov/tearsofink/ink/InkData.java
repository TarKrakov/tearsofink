package com.tarkrakov.tearsofink.ink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Душа и Чернила игрока. Хранится на игроке, переживает выход из игры и смерть.
 * Душа по частям (обеты, 15 ноя): максимум = 100 − проданное + «сверх» (обмен у Торговки и пробуждения), до 150.
 *
 * @param sold     сколько шагов души продано (0…10; шаг — {@link #SOUL_STEP})
 * @param extra    Душа сверх 100: обменяна у Торговки + пробуждения (0…50)
 * @param soul     текущая Душа (тратится оружием Души, восстанавливается до {@link #soulMax()})
 * @param inkBonus прибавка к запасу Чернил от фонтанов и сердец, минус обменянное на Душу (чернила за проданную душу — отдельно)
 * @param ink      текущие Чернила (тратятся рывком и оружием Чернил, восстанавливаются до {@link #inkMax()})
 * @param inside   игрок сейчас в Чернильных Землях (силы работают)
 * @param taint    сколько шагов души заняли Чернильные сердца (−5 к максимуму за шаг, Чернил не даёт — их дало сердце)
 */
public record InkData(int sold, int extra, float soul, int inkBonus, float ink, boolean inside, int taint) {
	// ===== Настройки — крутить здесь =====
	/** Душа на старте. */
	public static final int SOUL_START = 100;
	/** Ниже этого душу продать нельзя. */
	public static final int SOUL_MIN = 50;
	/** Выше этого Душа не растёт (обмен + пробуждения). */
	public static final int SOUL_CAP = 150;
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
			Codec.INT.optionalFieldOf("sold").forGetter(d -> java.util.Optional.of(d.sold())),
			Codec.INT.optionalFieldOf("soul_max").forGetter(d -> java.util.Optional.empty()), // старые миры: максимум Души
			Codec.INT.optionalFieldOf("soul_extra", 0).forGetter(InkData::extra),
			Codec.FLOAT.fieldOf("soul").forGetter(InkData::soul),
			Codec.INT.fieldOf("ink_bonus").forGetter(InkData::inkBonus),
			Codec.FLOAT.fieldOf("ink").forGetter(InkData::ink),
			Codec.BOOL.optionalFieldOf("inside", false).forGetter(InkData::inside),
			Codec.INT.optionalFieldOf("taint", 0).forGetter(InkData::taint)
	).apply(i, (sold, legacyMax, extra, soul, bonus, ink, inside, taint) -> new InkData(
			sold.orElseGet(() -> legacyMax.map(m -> (SOUL_START - Math.min(SOUL_START, m)) / SOUL_STEP).orElse(0)),
			extra, soul, bonus, ink, inside, taint)));

	public static InkData fresh() {
		return new InkData(0, 0, SOUL_START, 0, INK_START, false, 0);
	}

	/** Максимум Души: 100 − проданное + сверх. */
	public int soulMax() {
		return SOUL_START - (sold + taint) * SOUL_STEP + extra;
	}

	/** Сколько шагов души продано. */
	public int soldSteps() {
		return sold;
	}

	/** Запас Чернил: старт + фонтаны и сердца + проданная душа, но не выше потолка. */
	public int inkMax() {
		return Math.max(0, Math.min(INK_CAP, INK_START + inkBonus + sold * INK_PER_SOUL_STEP));
	}

	/** Приводит значения в допустимые рамки. */
	public InkData clamped() {
		int s = Math.max(0, Math.min((SOUL_START - SOUL_MIN) / SOUL_STEP, sold));
		int e = Math.max(0, Math.min(SOUL_CAP - SOUL_START, extra));
		int bonus = Math.max(-INK_START, inkBonus);
		int t = Math.max(0, Math.min((SOUL_START - SOUL_MIN) / SOUL_STEP - s, taint)); // продано + занято сердцами — не ниже 50
		InkData d = new InkData(s, e, soul, bonus, ink, inside, t);
		return new InkData(s, e, Math.max(0, Math.min(d.soulMax(), soul)), bonus,
				Math.max(0, Math.min(d.inkMax(), ink)), inside, t);
	}

	/** Максимум Души без «сверх» (100 − проданное): ставит, сколько продано. */
	public InkData withSoulMax(int value) {
		return withSold((SOUL_START - Math.min(SOUL_START, value)) / SOUL_STEP);
	}

	public InkData withSold(int value) {
		return new InkData(value, extra, soul, inkBonus, ink, inside, taint).clamped();
	}

	public InkData withTaint(int value) {
		return new InkData(sold, extra, soul, inkBonus, ink, inside, value).clamped();
	}

	public InkData withExtra(int value) {
		return new InkData(sold, value, soul, inkBonus, ink, inside, taint).clamped();
	}

	public InkData withSoul(float value) {
		return new InkData(sold, extra, value, inkBonus, ink, inside, taint).clamped();
	}

	public InkData withInkBonus(int value) {
		return new InkData(sold, extra, soul, value, ink, inside, taint).clamped();
	}

	public InkData withInk(float value) {
		return new InkData(sold, extra, soul, inkBonus, value, inside, taint).clamped();
	}

	public InkData withInside(boolean value) {
		return new InkData(sold, extra, soul, inkBonus, ink, value, taint);
	}

	/** Полностью восстановленные шкалы. */
	public InkData refilled() {
		return new InkData(sold, extra, soulMax(), inkBonus, inkMax(), inside, taint);
	}
}
