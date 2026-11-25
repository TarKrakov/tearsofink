package com.tarkrakov.tearsofink.ink;

/**
 * Рабочие цифры роста Пути Чернил D-037. 20 тиков = 1 секунда.
 * Пассивная регенерация и пополнение ударами обычного оружия сюда не переносятся.
 */
public final class InkTuning {
	/** Ранги по фактическому МАКСИМУМУ Чернил. */
	public static final int MARKED_AT = 50, TAINTED_AT = 100, HOLLOW_AT = 150;
	/** Бесплатный обычный рывок: 18 / 16 / 14 тиков в зависимости от ранга. */
	public static final int DASH_COST = 0, DASH_COOLDOWN = 18, MARKED_DASH_COOLDOWN = 16, TAINTED_DASH_COOLDOWN = 14;
	/** Порченый: второй прыжок 3 Чернила вместо 5. */
	public static final float TAINTED_JUMP_COST = 3f;
	/** Пустой: за одно настоящее уклонение от вражеской атаки во время рывка. */
	public static final float HOLLOW_DODGE_REFUND = 3f;

	private InkTuning() {}
}
