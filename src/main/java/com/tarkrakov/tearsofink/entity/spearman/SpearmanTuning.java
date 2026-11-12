package com.tarkrakov.tearsofink.entity.spearman;

/** Цифры Бледного копейщика — крутить здесь (20 тиков = 1 с). */
public final class SpearmanTuning {
	public static final double HEALTH = 24;
	/** Урон тычка (на обычной сложности). */
	public static final double DAMAGE = 6;
	/** Скорость (зомби 0.23) — чуть медленнее игрока. */
	public static final double SPEED = 0.21;
	/** С какого расстояния замечает игрока. */
	public static final double FOLLOW_RANGE = 16;

	/** Досягаемость копья и с какой дистанции он начинает тычок. */
	public static final double REACH = 3.5, THRUST_START = 3.0;
	/** Держится на таком расстоянии (не лезет вплотную). */
	public static final double KEEP_DISTANCE = 2.6;
	/** Раз в сколько тиков может ударить (~2 с). */
	public static final int ATTACK_COOLDOWN = 40;
	/** Длина анимации thrust и момент урона в ней (тики). */
	public static final int THRUST_TICKS = 17, THRUST_HIT_TICK = 10;

	/** Отпрыжка: не чаще раза в N тиков; если игрок ближе CLOSE или только что ударил. */
	public static final int DODGE_COOLDOWN = 100;
	public static final double DODGE_CLOSE = 2.0;
	/** Длина анимации dodge, момент отрыва (тики) и сила толчка. */
	public static final int DODGE_TICKS = 16, DODGE_PUSH_TICK = 4;
	public static final double DODGE_BACK = 0.75, DODGE_UP = 0.38;

	/** Патрули: появляются у дорог в Бледнолесье, ходят между фонарными столбами. */
	public static final int PATROL_SPAWN_INTERVAL = 200;
	/** Сколько копейщиков максимум вокруг игрока (в радиусе PATROL_COUNT_RADIUS). */
	public static final int PATROL_MAX_NEAR = 3, PATROL_COUNT_RADIUS = 64;
	/** На каком расстоянии от игрока появляются (вне его видимости в тумане). */
	public static final int PATROL_SPAWN_MIN = 28, PATROL_SPAWN_MAX = 56;
	/** Отрезок дороги, по которому ходит патруль (между столбами). */
	public static final int PATROL_SEGMENT = 64;

	private SpearmanTuning() {}
}
