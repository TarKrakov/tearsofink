package com.tarkrakov.tearsofink.weapon.shield;

/**
 * Числа Хлопка и Резонанса (R с двумя щитами Души). Заглушки Claude — баланс потом.
 * Урон и лечение: 2 = одно сердце. Время — тики (20 = 1 с). «power» = 1 + POWER_PER_CHARGE × заряд стойки.
 */
public final class ResonanceTuning {
	// --- общее ---
	/** Цена Душой. */
	public static final int SOUL_COST = 20;
	/** Общая перезарядка всех Резонансов. */
	public static final int COOLDOWN_TICKS = 200;
	/** Хлопок: столько тиков от нажатия до звона (под движение clap в bastion.json). */
	public static final int CLAP_TICKS = 6;
	/** Каждая единица заряда стойки (до 5) — столько к силе Резонанса; заряд при этом тратится. */
	public static final float POWER_PER_CHARGE = 0.15f;
	/** Сбить замах врага (кольца, Лавина). */
	public static final int STAGGER_TICKS = 20;

	// --- рывки ---
	public static final double DASH_SPEED = 0.9;
	public static final int DASH_TICKS = 7;

	/** Таран + Таран — Лавина: длинный неудержимый рывок. */
	public static final double AVALANCHE_SPEED = 1.05;
	public static final int AVALANCHE_TICKS = 14;
	public static final float AVALANCHE_DAMAGE = 6f;
	public static final double AVALANCHE_KNOCKBACK = 2.0;

	/** Таран + Отпор — Наступление: рывок, в конце или при столкновении — хлопок о землю кольцом. */
	public static final double ADVANCE_RADIUS = 4.5;
	public static final float ADVANCE_DAMAGE = 6f;
	public static final double ADVANCE_KNOCKBACK = 1.3;

	/** Таран + Светлый — Фаланга: рывок, спереди не пробить, задетых ослепляет. */
	public static final float PHALANX_DAMAGE = 4f;
	public static final double PHALANX_KNOCKBACK = 1.2;
	public static final int PHALANX_BLIND_TICKS = 50;
	/** Сколько ещё после рывка спереди не пробить. */
	public static final int PHALANX_AFTER_TICKS = 20;
	/** Полуугол «спереди». */
	public static final double PHALANX_COVER_DEGREES = 70;

	/** Таран + Милосердие — Заступник: рывок, своих на пути лечит, себя — немного в конце. */
	public static final float INTERCESSOR_HEAL = 6f;
	public static final int INTERCESSOR_REGEN_TICKS = 60;
	public static final float INTERCESSOR_SELF_HEAL = 3f;

	// --- вокруг себя ---
	/** Светлый + Светлый — Солнце: свои — сияние (−40% урона), враги — ослеплены. */
	public static final double SUN_RADIUS = 8;
	public static final int SUN_TICKS = 160;
	public static final int SUN_BLIND_TICKS = 60;

	/** Милосердие + Милосердие — Благодать: сильное лечение, снять дурные эффекты. */
	public static final double GRACE_RADIUS = 8;
	public static final float GRACE_HEAL = 10f;

	/** Отпор + Отпор — Ударная волна: кольцо вокруг. */
	public static final double SHOCKWAVE_RADIUS = 6;
	public static final float SHOCKWAVE_DAMAGE = 5f;
	public static final double SHOCKWAVE_KNOCKBACK = 1.5;

	/** Светлый + Милосердие — Ореол: аура — свои рядом лечатся и получают сопротивление. */
	public static final int HALO_TICKS = 160;
	public static final double HALO_RADIUS = 6;
	public static final float HALO_HEAL_PER_SECOND = 1.5f;

	/** Светлый + Отпор — Вспышка: конус — отброс, ослепление, замедление. */
	public static final double FLASH_RANGE = 7;
	public static final double FLASH_HALF_ANGLE = 50;
	public static final float FLASH_DAMAGE = 3f;
	public static final double FLASH_KNOCKBACK = 1.2;
	public static final int FLASH_BLIND_TICKS = 60;

	/** Милосердие + Отпор — Покров: импульс — свои лечатся, враги отлетают. */
	public static final double VEIL_RADIUS = 6;
	public static final float VEIL_HEAL = 5f;
	public static final float VEIL_DAMAGE = 2f;
	public static final double VEIL_KNOCKBACK = 1.2;

	private ResonanceTuning() {}
}
