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
	public static final float[] HUNTER_HP = {55, 80, 110};
	public static final float HUNTER_HP_SPREAD = 0.15f;
	/** Урон охотника по рангу (умножается на множитель удара связки). */
	public static final float[] HUNTER_DAMAGE = {4.4f, 5.2f, 6.2f};
	public static final float HUNTER_DAMAGE_SPREAD = 0.15f;
	/** Защита охотника за каждую надетую деталь брони (Новичок 2, Опытный 4, Легендарный 6 деталей). */
	public static final float ARMOR_PER_PIECE = 1f;
	/** Скорость передвижения охотника. */
	public static final double HUNTER_SPEED = 0.3;
	/**
	 * Цель тем крепче, чем выше ранг охотника: множитель здоровья и урона цели.
	 * Урон умножается у ВСЕХ целей одинаково (ванильные удары, стрелы, удары солдат).
	 * Своя сила каждой цели — поле "power" в hunt_targets/*.json.
	 */
	public static final float[] PREY_HP = {3.6f, 4.2f, 5.1f};
	public static final float[] PREY_DAMAGE = {1.45f, 1.7f, 2.0f};
	/** Шанс охотника победить цель «вдали» (по рангу). */
	public static final float[] REMOTE_HUNTER_WINS = {0.45f, 0.6f, 0.75f};
	/** Чернильный охотник после победы бросается на игроков — столько тиков, потом растворяется. */
	public static final int INK_FRENZY = 60 * 20;
	// ===== Особые цели =====
	/** Вес: обычная, резвая, гигант, разделённая. */
	public static final int[] PREFIX_WEIGHT = {40, 25, 20, 15};
	/** Резвая: быстрее ходит; после удара вблизи — ещё один быстрый удар (доля урона) через столько тиков. */
	public static final float SWIFT_SPEED = 1.4f, SWIFT_ECHO = 0.2f;
	public static final int SWIFT_ECHO_DELAY = 6;
	/** Гигант: здоровье, урон, размер. */
	public static final float GIANT_HP = 1.2f, GIANT_DAMAGE = 1.05f, GIANT_SCALE = 1.5f;
	/** Разделённая: две цели, у каждой такая доля силы и размер. */
	public static final float SPLIT_STATS = 0.6f, SPLIT_SCALE = 0.75f;
	/** За особую цель — больше Пигмента каждому, кто получил контракт. */
	public static final int PREFIX_BONUS_PIGMENT_MIN = 2, PREFIX_BONUS_PIGMENT_MAX = 3;

	// ===== Броня (навески gear_* модели) =====
	/** Сколько мест занято бронёй по рангу: {от, до}. Новичок 2, Опытный 4, Легендарный 6. */
	public static final int[][] GEAR_SLOTS = {{2, 2}, {4, 4}, {6, 6}};

	// ===== Оружие в руках =====
	/** Один меч / пара одинаковых / разные (клинок в правой, двуручник в левой) — %. */
	public static final int[] LOADOUT_WEIGHT = {50, 35, 15};

	// ===== Приёмы и рывок =====
	/** Перезарядки приёмов по рангу (множитель от игрока): Новичок медленнее, Легендарный быстрее. */
	public static final float[] RANK_COOLDOWN = {1.2f, 1.0f, 0.8f};
	/** Охотник Шпиля играет от приёмов — перезарядки у него ещё короче. */
	public static final float SPIRE_COOLDOWN = 0.75f;
	/** Урон приёмов охотника по рангу (множитель). */
	public static final float[] SKILL_DAMAGE = {0.85f, 1.0f, 1.2f};
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

	// ===== Чужие враги =====
	/** Кто напал на охотника (не цель) — он отвечает столько тиков (8 с), потом возвращается к цели. */
	public static final int THREAT_TICKS = 160;
	/** Дальше этого от обидчика — бросает его и возвращается к цели. */
	public static final double THREAT_FORGET = 18;
	/** Чернильный сам бросается на врагов (патрули, монстры) ближе этого. */
	public static final double FERAL_AGGRO = 6;

	// ===== Уворот от рывка щитоносца =====
	/** За сколько тиков до конца замаха охотник уходит вбок (по рангу: Новичок поздно, Легендарный заранее). */
	public static final int[] DODGE_LEAD = {3, 6, 10};
	/** Уже бежит на него ближе этого — тоже уворот. */
	public static final double DODGE_RUN_DIST = 7;
	/** Рывок на откате — шаг-прыжок вбок с такой силой. */
	public static final double SIDESTEP = 0.55;

	// ===== Стили боя =====
	/** Кровавый: враг убегает дальше этого — рывок вдогонку. */
	public static final double BLOOD_CHASE = 6;
	/** Чернильный: враг дальше этого — рывок к нему. */
	public static final double INK_DASH_FROM = 4.5;
	/** Шпиль: держит дистанцию столько блоков после 1–2 ударов. */
	public static final double SPIRE_KEEP_MIN = 5, SPIRE_KEEP_MAX = 7;
	/** Шпиль: шанс подбить врага ударом и сила подброса. */
	public static final float SPIRE_LAUNCH_CHANCE = 0.25f;
	/** Шпиль играет от приёмов: урон его приёмов и здоровье — выше (иначе отходы стоят ему боя). */
	public static final float SPIRE_SKILL_DAMAGE = 1.1f, SPIRE_HP = 1.05f;
	/** Стрелки (скелет, арбалетчик): враг дальше этого — охотник догоняет рывком; Шпиль против стрелка не отходит. */
	public static final double CHASE_RANGED = 4;
	/** Шпиль: шанс после отхода не стрелять издалека, а полезть в драку (3–4 удара подряд). */
	public static final float SPIRE_BRAWL_CHANCE = 0.35f;
	/** Шпиль: сколько держит дистанцию (тики, + случайно до SPIRE_KEEP_RANDOM). */
	public static final int SPIRE_KEEP_TICKS = 40, SPIRE_KEEP_RANDOM = 30;
	public static final double SPIRE_LAUNCH = 0.45;

	/** Шанс получить у Торговки оружие охотника за контракт. */
	public static final float CONTRACT_WEAPON_CHANCE = 0.3f;

	private HuntTuning() {}
}
