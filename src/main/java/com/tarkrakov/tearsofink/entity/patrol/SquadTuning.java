package com.tarkrakov.tearsofink.entity.patrol;

/** Цифры щитовика, арбалетчика и отрядов — крутить здесь (20 тиков = 1 с). */
public final class SquadTuning {
	// ===== Щитовик (танк) =====
	public static final double TANK_HEALTH = 40, TANK_SPEED = 0.23, TANK_ARMOR = 4, TANK_KNOCKBACK_RES = 0.6;
	/** Щит: держит до SHIELD_MAX тиков, потом опускает минимум на SHIELD_REST; поднимает, если игрок ближе SHIELD_RANGE. */
	public static final int SHIELD_MAX = 100, SHIELD_REST = 60;
	public static final double SHIELD_RANGE = 10;
	/** Какая доля урона спереди проходит сквозь щит (0.2 = режет 80%). Снаряды спереди не проходят вовсе. */
	public static final float SHIELD_PASS = 0.2f;
	/** Скорость шага со щитом (доля обычной). */
	public static final double SHIELD_WALK = 0.75;
	/** Удар топором выбивает щит на столько тиков. */
	public static final int SHIELD_AXE_DISABLE = 60;
	/** Удар щитом: дистанция, урон, отброс, длина анимации и момент удара, перезарядка. */
	public static final double BASH_RANGE = 2.6, BASH_DAMAGE = 8, BASH_KNOCKBACK = 1.4;
	public static final int BASH_TICKS = 24, BASH_HIT_TICK = 14, BASH_COOLDOWN = 50; // анимация 1,5 с, удар на 0,7 с; хвост анимации срезан — после 1,2 с уже ходит
	/** Рывок: когда (дистанция), замах, бег, скорость, урон, отброс, тупит после промаха, перезарядка. */
	public static final double CHARGE_MIN = 6, CHARGE_MAX = 14, CHARGE_SPEED = 0.55, CHARGE_DAMAGE = 10, CHARGE_KNOCKBACK = 2.0;
	public static final int CHARGE_WINDUP = 24, CHARGE_RUN = 30, CHARGE_STUN = 25, CHARGE_COOLDOWN = 160; // замах = charge_start 1,2 с
	/** Рывок сносит «мягкие» блоки не твёрже этого (доски 2.0, земля 0.5, листья 0.2). Камень (1.5+) — см. правило в коде. */
	public static final float CHARGE_BREAK_HARDNESS = 2.0f;
	/** Во время бега чуть доворачивает за игроком: градусов за тик. */
	public static final float CHARGE_TURN_DEG = 2.5f;

	// ===== Арбалетчик =====
	public static final double ARCHER_HEALTH = 16, ARCHER_SPEED = 0.25;
	/** Держит дистанцию: отходит, если ближе KEEP_MIN, подходит, если дальше KEEP_MAX. */
	public static final double KEEP_MIN = 8, KEEP_MAX = 14, SHOOT_RANGE = 24;
	/** Цикл: прицел → выстрел → перезарядка (тики). */
	public static final int AIM_TICKS = 20, SHOOT_TICKS = 10, RELOAD_TICKS = 50; // под анимации автора: aim 1 с, shoot 0,5 с, reload 2,5 с; выстрел раз в 4 с
	/** Болт: урон, скорость, разброс. */
	public static final double BOLT_DAMAGE = 4, BOLT_SPEED = 2.6;
	public static final float BOLT_SPREAD = 3f;
	/** Отпрыжка: если игрок ближе CLOSE; один раз за цикл выстрела (потом обязан выстрелить). */
	public static final double ARCHER_DODGE_CLOSE = 3.5, ARCHER_DODGE_BACK = 0.8, ARCHER_DODGE_UP = 0.4;
	public static final int ARCHER_DODGE_TICKS = 10, ARCHER_DODGE_PUSH_TICK = 3, ARCHER_DODGE_COOLDOWN = 40; // dodge 0,5 с

	// ===== Против мобов (не игроков) =====
	/** Множитель урона всех атак патруля по мобам (не по игрокам). */
	public static final float VS_MOBS_DAMAGE = 2.0f;
	/** Арбалетчик по мобам стреляет чаще: время прицела и перезарядки × это (0.5 = вдвое быстрее). */
	public static final float VS_MOBS_FIRE_RATE = 0.5f;
	/** Щитовик и копейщик бросаются на тех, кто напал на их арбалетчиков (в этом радиусе от стрелка). */
	public static final double BODYGUARD_RADIUS = 7;

	// ===== Гашение фонарей =====
	/** Вожак отряда замечает горящий фонарь в этом радиусе и идёт его гасить (Вечные не трогают). */
	public static final int DOUSE_RADIUS = 14;
	/** Как часто вожак осматривается (тики). */
	public static final int DOUSE_SCAN = 40;

	// ===== Отряды =====
	/** Составы отрядов и их веса: T — щитовик, S — копейщик, C — арбалетчик. Порядок = строй (первый ведёт). */
	public static final String[] SQUADS = {"TSCC", "TCCC", "SSCC", "TTC", "TSC", "S"};
	public static final int[] SQUAD_WEIGHTS = {40, 15, 15, 10, 15, 5};
	/** Шаг строя в колонне (блоков между солдатами). */
	public static final double FORMATION_STEP = 2.5;

	private SquadTuning() {}
}
