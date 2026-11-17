package com.tarkrakov.tearsofink.ink;

/**
 * Рабочие цифры роста Пути Чернил D-037. 20 тиков = 1 секунда.
 * Пассивная регенерация и пополнение ударами обычного оружия сюда не переносятся.
 */
public final class InkTuning {
	/** Ранги по фактическому МАКСИМУМУ Чернил. */
	public static final int MARKED_AT = 50, TAINTED_AT = 100, HOLLOW_AT = 150;
	/** Отмеченный: стоимость обычного рывка вместо 5 Чернил. */
	public static final float MARKED_DASH_COST = 4f;
	/** Порченый: рывок 20 тиков вместо 25; второй прыжок 3 Чернила вместо 5. */
	public static final int TAINTED_DASH_COOLDOWN = 20;
	public static final float TAINTED_JUMP_COST = 3f;
	/** Пустой: за одно настоящее уклонение от вражеской атаки во время рывка. */
	public static final float HOLLOW_DODGE_REFUND = 3f;

	private InkTuning() {}
}
