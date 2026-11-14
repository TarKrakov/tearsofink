package com.tarkrakov.tearsofink.hunt;

/** Все цифры Охоты — крутить здесь (код → перезапуск). Список целей — data/tears-of-ink/hunt_targets/*.json (→ /reload). */
public final class HuntTuning {
	/** Перерыв между Охотами и длительность (тики, 20 = 1 с): 15 мин и 5 мин. */
	public static final int INTERVAL = 15 * 60 * 20;
	public static final int DURATION = 5 * 60 * 20;
	/** Первая Охота после запуска сервера. */
	public static final int FIRST_DELAY = 15 * 60 * 20;
	/** Цель появляется на таком расстоянии от выбранного игрока. */
	public static final int SPAWN_MIN = 65, SPAWN_MAX = 90;
	/** Охотник держится на таком расстоянии от цели, пока ждёт. */
	public static final int WAIT_MIN = 10, WAIT_MAX = 16;
	/** Сколько охотник ждёт игроков, прежде чем напасть сам. */
	public static final int HUNTER_WAIT = 90 * 20;
	/** Игрок ближе этого к цели — охотник тоже вступает в бой. */
	public static final int PLAYER_ENGAGE = 24;
	/** Никого из игроков ближе этого к цели — исход разыгрывается «вдали», в цифрах. */
	public static final int REMOTE = 112;
	/** Доля урона среди всех игроков, чтобы получить контракт (10%). */
	public static final float CONTRIBUTION = 0.10f;

	/** Ранги охотника: Новичок, Опытный, Легендарный — вес выпадения. */
	public static final int[] RANK_WEIGHT = {60, 30, 10};
	/** Здоровье охотника по рангу (± разброс). */
	public static final float[] HUNTER_HP = {36, 60, 90};
	public static final float HUNTER_HP_SPREAD = 0.15f;
	/** Урон охотника по рангу (умножается на множитель удара связки). */
	public static final float[] HUNTER_DAMAGE = {3.5f, 5, 7};
	public static final float HUNTER_DAMAGE_SPREAD = 0.15f;
	/** Скорость передвижения охотника. */
	public static final double HUNTER_SPEED = 0.3;
	/** Цель тем крепче, чем выше ранг охотника: множитель здоровья и урона цели. */
	public static final float[] PREY_HP = {3.0f, 4.0f, 5.5f};
	public static final float[] PREY_DAMAGE = {1.8f, 2.2f, 2.8f};
	/** Шанс охотника победить цель «вдали» (по рангу). */
	public static final float[] REMOTE_HUNTER_WINS = {0.45f, 0.6f, 0.75f};
	/** Чернильный охотник после победы бросается на игроков — столько тиков, потом растворяется. */
	public static final int INK_FRENZY = 60 * 20;
	// ===== Оружие в руках =====
	/** Один меч / пара одинаковых / разные (клинок в правой, двуручник в левой) — %. */
	public static final int[] LOADOUT_WEIGHT = {50, 35, 15};

	// ===== Приёмы и рывок =====
	/** Перезарядки приёмов по рангу (множитель от игрока): Новичок медленнее, Легендарный быстрее. */
	public static final float[] RANK_COOLDOWN = {1.3f, 1.0f, 0.75f};
	/** Охотник Шпиля играет от приёмов — перезарядки у него ещё короче. */
	public static final float SPIRE_COOLDOWN = 0.75f;
	/** Урон приёмов охотника по рангу (множитель). */
	public static final float[] SKILL_DAMAGE = {0.8f, 1.0f, 1.25f};
	/** Урон Волны и Рассечения у охотника (как у игрока). */
	public static final float WAVE_DAMAGE = 6f, CLEAVE_DAMAGE = 12f;
	/** Приёмы Скимитаров у охотников (Волна, Чернильный Росчерк) — перезарядка дольше в столько раз. */
	public static final float SCIMITAR_SKILL_COOLDOWN = 2.0f;
	/** Гильотина охотника: сколько ударов, прежде чем казнь. */
	public static final int GUILLOTINE_CHOPS_MIN = 3, GUILLOTINE_CHOPS_MAX = 6;
	/** Чернильный рывок: перезарядка по рангу (тики: 7 / 5 / 4 с), скорость и длительность (≈ 4,5 блока). */
	public static final int[] DASH_COOLDOWN = {140, 100, 80};
	public static final double DASH_SPEED = 0.9;
	public static final int DASH_TICKS = 5;

	// ===== Стили боя =====
	/** Кровавый: враг убегает дальше этого — рывок вдогонку. */
	public static final double BLOOD_CHASE = 6;
	/** Чернильный: враг дальше этого — рывок к нему. */
	public static final double INK_DASH_FROM = 4.5;
	/** Шпиль: держит дистанцию столько блоков после 1–2 ударов. */
	public static final double SPIRE_KEEP_MIN = 5, SPIRE_KEEP_MAX = 7;
	/** Шпиль: шанс подбить врага ударом и сила подброса. */
	public static final float SPIRE_LAUNCH_CHANCE = 0.25f;
	/** Шпиль: шанс после отхода не стрелять издалека, а полезть в драку (3–4 удара подряд). */
	public static final float SPIRE_BRAWL_CHANCE = 0.35f;
	/** Шпиль: сколько держит дистанцию (тики, + случайно до SPIRE_KEEP_RANDOM). */
	public static final int SPIRE_KEEP_TICKS = 40, SPIRE_KEEP_RANDOM = 30;
	public static final double SPIRE_LAUNCH = 0.45;

	/** Шанс получить у Торговки оружие охотника за контракт. */
	public static final float CONTRACT_WEAPON_CHANCE = 0.3f;

	private HuntTuning() {}
}
