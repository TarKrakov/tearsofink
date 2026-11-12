package com.tarkrakov.tearsofink.block.blood;

/** Настройки Кровосборника — крутить здесь (20 тиков = 1 секунда). */
public final class BloodTuning {
	/** Помпа делает удар раз в столько тиков (40 = 2 с — как анимация автора). Анимация `pump` растягивается под это время. */
	public static final int PUMP_INTERVAL = 40;
	/** Кровь капает в Накопитель каждый N-й удар (2 → +1 крови за 4 с ≈ 1,5 Сгустка в минуту с одной помпы). */
	public static final int STROKES_PER_BLOOD = 2;
	/** На каком тике цикла поршень достигает низа в анимации (0,2 с = 4 тика) — в этот момент и капает кровь. */
	public static final int SLAM_OFFSET = 4;
	/** Сколько крови вмещает Накопитель. */
	public static final int TANK_CAPACITY = 64;
	/** Сколько крови = 1 Сгусток крови при сборе. */
	public static final int BLOOD_PER_CLOT = 10;
	/** Шанс, что за удар в Накопителе застынет Кристалл крови (1 из N), и сколько их может лежать максимум. */
	public static final int CRYSTAL_ONE_IN = 200, CRYSTAL_MAX = 3;
	/** Ниже этого гусеницы не пьют — идут есть Плотеягоду. */
	public static final int CATERPILLAR_MIN = 10;
	/** Сколько отпивает гусеница за раз. */
	public static final int CATERPILLAR_SIP = 2;
	/** В каком радиусе гусеница ищет Накопитель. */
	public static final int CATERPILLAR_SEARCH = 12;
	/** Насколько часто кровавая гусеница выбирает Плотеягоду, даже если есть что пить (0–100 %). */
	public static final int BERRY_INSTEAD_PERCENT = 30;

	private BloodTuning() {}
}
