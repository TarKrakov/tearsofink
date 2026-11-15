package com.tarkrakov.tearsofink.weapon.blood;

/** Числа линии Крови: кровотечение, кровавые дуги, цена здоровьем. Код → перезапуск. */
public final class BloodTuning {
	// ===== Кровотечение =====
	/** Шанс обычного удара оружием Крови повесить (или усилить) кровотечение. */
	public static final float BLEED_CHANCE = 0.25f;
	/** Длительность (тики, 5 с) и сколько ступеней максимум (I–III). */
	public static final int BLEED_TICKS = 100, BLEED_MAX = 3;
	/** Урон раз в секунду по ступеням I / II / III. */
	public static final float[] BLEED_DAMAGE = {1.0f, 1.5f, 2.0f};
	/** Доля урона кровотечения, что возвращается здоровьем тому, кто его наложил (заплатил кровью — вернул из врага). */
	public static final float BLEED_LIFESTEAL = 0.3f;

	// ===== Тальвар =====
	/** R — кровавая дуга: цена здоровьем (2 = одно сердце), перезарядка, урон, ступени кровотечения. */
	public static final float ARC_HEALTH = 2f, ARC_DAMAGE = 4f;
	public static final int ARC_COOLDOWN = 50, ARC_BLEED = 1;
	/** Дуга короткая и широкая: летит столько блоков, ширина — в каждую сторону от центра. */
	public static final double ARC_RANGE = 6, ARC_SPEED = 0.8, ARC_HALF_WIDTH = 1.6;
	/** ПКМ парой — кровавый крест и отскок назад. */
	public static final float CROSS_HEALTH = 4f, CROSS_DAMAGE = 6f;
	public static final int CROSS_COOLDOWN = 100, CROSS_BLEED = 2;
	public static final double CROSS_BACKSTEP = 0.9;

	private BloodTuning() {}
}
