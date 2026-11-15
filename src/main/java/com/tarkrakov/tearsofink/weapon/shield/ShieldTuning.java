package com.tarkrakov.tearsofink.weapon.shield;

/** Числа стойки двух щитов Души (Путь Души, этап 1). Код → перезапуск. */
public final class ShieldTuning {
	// ===== Блок =====
	/** Сколько Души стоит принятый удар: столько за единицу урона (2 = сердце). */
	public static final float SOUL_PER_DAMAGE = 1.0f;
	/** Душа кончилась — щиты пробиты: опускаются и недоступны столько тиков (20 = 1 с). */
	public static final int BREAK_TICKS = 60;
	/** Спереди и с боков блок полный — в пределах стольких градусов от взгляда; со спины — доля урона. */
	public static final float COVER_DEGREES = 120f, BACK_FRACTION = 0.5f;

	// ===== Своевременный блок =====
	/** Поднял стойку не раньше стольких тиков до удара — Душа не тратится, нападающий сбит, болт летит обратно. */
	public static final int PERFECT_TICKS = 5;
	/** Сбитый своевременным блоком теряется на столько тиков. */
	public static final int PERFECT_STAGGER = 20;

	// ===== Заряд светом и выброс =====
	/** Каждый принятый удар — +1 заряд, не больше стольких. */
	public static final int CHARGE_MAX = 5;
	/** Выброс (отпустил ПКМ с зарядом): конус — дальность и полуширина в градусах. */
	public static final double BURST_RANGE = 4.5;
	public static final float BURST_HALF_ANGLE = 60f;
	/** Урон и отбрасывание выброса: основа + за каждый заряд. */
	public static final float BURST_DAMAGE = 1f, BURST_DAMAGE_PER_CHARGE = 1.5f;
	public static final float BURST_KNOCKBACK = 0.4f, BURST_KNOCKBACK_PER_CHARGE = 0.2f;
	/** Выброс сбивает замах на столько тиков. */
	public static final int BURST_STAGGER = 20;
	// --- Превозмогание (15 ноя): вытерпленное становится ударом ---
	/** Выброс бьёт ещё и такой долей урона, который щиты поглотили в этой стойке… */
	public static final float ABSORB_TO_BURST = 0.4f;
	/** …но не больше этой прибавки. */
	public static final float ABSORB_BURST_CAP = 20f;
	/** Ответ: после своевременного блока столько тиков следующий удар Бастиона — ×RIPOSTE_MULT и сбивает. */
	public static final int RIPOSTE_TICKS = 20;
	public static final float RIPOSTE_MULT = 2f;
	/** Сильнее против сильных: враги с запасом здоровья от STRONG_HP получают от Бастиона и выброса ×STRONG_MULT. */
	public static final float STRONG_HP = 30f, STRONG_MULT = 1.3f;

	/** На полном заряде принятый удар звучит хором (не чаще раза в столько тиков) и с такой громкостью. */
	public static final int CHOIR_EVERY = 50;
	public static final float CHOIR_VOLUME = 0.45f;

	// ===== Свет =====
	/** Свечение щитов в темноте (0–15): основа + за каждый заряд. */
	public static final int GLOW = 9, GLOW_PER_CHARGE = 1;
	/** Видимость в тумане с двумя щитами (как ручной фонарь: тусклый 18, обычный 26) + за каждый заряд. */
	public static final float CLARITY = 18f, CLARITY_PER_CHARGE = 4f;

	private ShieldTuning() {}
}
