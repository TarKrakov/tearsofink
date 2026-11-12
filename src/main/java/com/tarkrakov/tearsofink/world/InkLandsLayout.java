package com.tarkrakov.tearsofink.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.function.IntBinaryOperator;

/**
 * Раскладка Чернильных Земель: одна область на мир.
 * От края к центру: Бледнолесье → (круглые пятна Кроволесья) → Чернолесье.
 * Одно крупное Смоляное Озеро на стыке Бледнолесья и Чернолесья: чёрная вода, глубже к середине,
 * пепельный пляж вокруг (деревня рыболюдов будет плавучей). Дороги его огибают.
 * Магистральные дороги идут от края к центру.
 * Центр выбирается автоматически (на суше) и одинаков для одного и того же сида мира.
 */
public final class InkLandsLayout {
	// ===== Размеры (в блоках) =====
	/** На каком расстоянии от точки 0,0 ищем место для центра. */
	public static final int[] CENTER_DISTANCES = {1400, 1200, 1700, 2100, 2600, 3200};
	/** Радиус всей области. */
	public static final int RADIUS = 800;
	/** Радиус ядра — Чернолесья. */
	public static final int CORE_RADIUS = 324;
	/** Насколько рвутся края области (±). */
	public static final int EDGE_WOBBLE = 110;
	/** Масштаб «рваности» краёв: больше — плавнее изгибы. */
	public static final double EDGE_SCALE = 300.0;

	// ===== Кроволесье: круглые пятна (позже — вокруг Багряных Вестников) =====
	/** Размер ячейки сетки: в каждой ячейке может быть одно пятно. */
	public static final int BLOOD_CELL = 240;
	/** Шанс, что в ячейке есть пятно. */
	public static final double BLOOD_CHANCE = 0.6;
	/** Радиус пятна: от MIN до MAX. */
	public static final int BLOOD_MIN_RADIUS = 60, BLOOD_MAX_RADIUS = 98;
	/** Лёгкая неровность края пятна (±). */
	public static final int BLOOD_WOBBLE = 7;
	/** Пятна только в средней полосе: от ядра + запас до этой доли радиуса области. */
	public static final int BLOOD_INNER_MARGIN = 15;
	public static final double BLOOD_OUTER_FRACTION = 0.92;

	// ===== Смоляное Озеро (одно на область) =====
	/** Расстояние центра Озера от центра области: на стыке Бледнолесья и Чернолесья, ближе к Чернолесью. */
	public static final int LAKE_DISTANCE = 400;
	/** Радиус водной глади (до пляжа). */
	public static final int LAKE_WATER_RADIUS = 130;
	/** Ширина пляжа вокруг воды. */
	public static final int LAKE_BEACH = 14;
	/** Плавный склон от пляжа к обычной земле (там уже лес). */
	public static final int LAKE_SLOPE = 45;
	/** Зона «Смоляное озеро» (чёрная вода, туман Озера, без деревьев): вода + пляж + полоса берега. */
	public static final int LAKE_ZONE_RADIUS = LAKE_WATER_RADIUS + LAKE_BEACH + 20;
	/** Глубина у самого берега и наибольшая — в середине. */
	public static final int LAKE_SHORE_DEPTH = 2, LAKE_DEPTH = 16;
	/** Неровность берега (±) и её масштаб (больше — плавнее изгибы). */
	public static final int LAKE_WOBBLE = 12;
	public static final double LAKE_WOBBLE_SCALE = 90.0;
	/** Дороги огибают Озеро на таком расстоянии от его центра (идут вдоль берега). */
	public static final int LAKE_ROAD_AVOID = LAKE_WATER_RADIUS + LAKE_BEACH + LAKE_WOBBLE + 6;

	// ===== Низина Кроволесья =====
	/** Насколько дно долины ниже окружающей земли. */
	public static final int VALLEY_DEPTH = 11;
	/** Ширина плавного склона вокруг пятна (за его краем). */
	public static final int VALLEY_SLOPE = 40;

	// ===== Дороги =====
	/** Сколько магистралей ведёт к центру. */
	public static final int ROADS = 5;
	/** Полуширина дороги (дорога шириной ~2×этого). */
	public static final double ROAD_HALF_WIDTH = 2.6;
	/** Насколько дорога петляет (в радианах) и масштаб изгибов. */
	public static final double ROAD_MEANDER = 0.22;
	public static final double ROAD_MEANDER_SCALE = 160.0;
	/** Дороги начинаются чуть за краем области и доходят почти до центра. */
	public static final double ROAD_START_FRACTION = 1.08;
	public static final int ROAD_END_RADIUS = 30;
	/** Насколько дорога может подниматься над рельефом насыпью (вниз — режет без ограничений). */
	public static final int ROAD_MAX_FILL = 6;
	/** Максимальный уклон дороги: подъём в блоках на 1 блок пути. */
	public static final double ROAD_MAX_GRADE = 0.2;
	/** Мост идёт на столько блоков выше воды. */
	public static final int BRIDGE_CLEARANCE = 2;
	/** Шаг выборки высоты вдоль дороги и ширина сглаживания (в шагах). */
	public static final int ROAD_PROFILE_STEP = 8;
	public static final int ROAD_SMOOTHING = 8;
	/** Фонарные столбы вдоль дорог — каждые столько блоков, по очереди слева и справа. */
	public static final int ROAD_LANTERN_SPACING = 64;

	private final Climate.Sampler sampler;
	private final BlockPos center;
	private final long seed;
	private final SimplexNoise edgeNoise;
	private final SimplexNoise detailNoise;
	private final SimplexNoise roadNoise;
	private final double[] roadAngles = new double[ROADS];
	/** Сглаженная высота каждой дороги вдоль её длины (считается один раз). */
	private volatile double[][] roadProfiles;
	/** Центр Смоляного Озера. */
	private final BlockPos lakeCenter;
	/** Уровень воды Озера (считается один раз). */
	private volatile Double lakeWaterLevel;
	/** Высота дна для каждого пятна Кроволесья (считается один раз на пятно). */
	private final java.util.concurrent.ConcurrentHashMap<Long, Double> valleyFloors = new java.util.concurrent.ConcurrentHashMap<>();

	private InkLandsLayout(Climate.Sampler sampler, BlockPos center) {
		this.sampler = sampler;
		this.center = center;
		this.seed = center.asLong() ^ 0x7E4125L;
		RandomSource random = RandomSource.create(seed);
		this.edgeNoise = new SimplexNoise(random);
		this.detailNoise = new SimplexNoise(random);
		this.roadNoise = new SimplexNoise(random);
		double start = random.nextDouble() * Mth.TWO_PI;
		for (int i = 0; i < ROADS; i++) {
			roadAngles[i] = start + i * Mth.TWO_PI / ROADS + (random.nextDouble() - 0.5) * 0.5;
		}
		this.lakeCenter = placeLake();
	}

	/** Озеро — посередине самого широкого промежутка между соседними дорогами. */
	private BlockPos placeLake() {
		double[] sorted = new double[ROADS];
		for (int i = 0; i < ROADS; i++) sorted[i] = Mth.positiveModulo(roadAngles[i], Mth.TWO_PI);
		java.util.Arrays.sort(sorted);
		double bestGap = -1, bestMid = 0;
		for (int i = 0; i < ROADS; i++) {
			double a = sorted[i], b = i + 1 < ROADS ? sorted[i + 1] : sorted[0] + Mth.TWO_PI;
			if (b - a > bestGap) {
				bestGap = b - a;
				bestMid = (a + b) / 2;
			}
		}
		return new BlockPos(center.getX() + (int) Math.round(Math.cos(bestMid) * LAKE_DISTANCE), 0,
				center.getZ() + (int) Math.round(Math.sin(bestMid) * LAKE_DISTANCE));
	}

	public BlockPos lakeCenter() {
		return lakeCenter;
	}

	/** Быстрая проверка без шума: может ли точка быть ближе radius к Озеру (с учётом неровности берега). */
	public boolean nearLake(int x, int z, double radius) {
		double dx = x - lakeCenter.getX(), dz = z - lakeCenter.getZ();
		double r = radius + LAKE_WOBBLE + 1;
		return dx * dx + dz * dz < r * r;
	}

	/** Расстояние до центра Озера с учётом неровного берега. */
	public double lakeDistance(int x, int z) {
		double dx = x - lakeCenter.getX(), dz = z - lakeCenter.getZ();
		return Math.sqrt(dx * dx + dz * dz) + detailNoise.getValue(x / LAKE_WOBBLE_SCALE + 100, z / LAKE_WOBBLE_SCALE) * LAKE_WOBBLE;
	}

	/**
	 * Рельеф Озера в точке: {высота верхнего твёрдого блока, 1 если над ним вода (до уровня воды), тип: 0 дно, 2 пляж, 3 склон}
	 * или null, если точка вне влияния Озера.
	 */
	public double[] lakeShape(int x, int z, double natural, double waterLevel) {
		double r = lakeDistance(x, z);
		double shore = LAKE_WATER_RADIUS, beachEnd = shore + LAKE_BEACH, slopeEnd = beachEnd + LAKE_SLOPE;
		if (r >= slopeEnd) return null;
		if (r < shore) {
			// у берега мелко, к середине всё глубже; середина — ровное глубокое дно
			double u = Mth.clamp((shore - r) / (shore * 0.75), 0, 1);
			u = u * u * (3 - 2 * u);
			double depth = LAKE_SHORE_DEPTH + (LAKE_DEPTH - LAKE_SHORE_DEPTH) * u;
			return new double[]{waterLevel - Math.round(depth), 1, 0};
		}
		if (r < beachEnd) {
			double rise = (r - shore) / LAKE_BEACH < 0.5 ? 1 : 2;
			return new double[]{waterLevel + rise, 0, 2};
		}
		double t = (r - beachEnd) / LAKE_SLOPE;
		t = t * t * (3 - 2 * t);
		return new double[]{Mth.lerp(t, waterLevel + 2, natural), 0, 3};
	}

	/** Уровень воды Озера: средняя высота земли вокруг, минус немного; не ниже уровня моря. */
	public double lakeWaterLevel(IntBinaryOperator baseHeight, int seaLevel) {
		Double cached = lakeWaterLevel;
		if (cached != null) return cached;
		double sum = 0;
		int n = 0;
		for (int a = 0; a < 12; a++) {
			double angle = a * Mth.TWO_PI / 12;
			for (double f : new double[]{0.5, 1.0, 1.3}) {
				sum += baseHeight.applyAsInt((int) (lakeCenter.getX() + Math.cos(angle) * LAKE_WATER_RADIUS * f),
						(int) (lakeCenter.getZ() + Math.sin(angle) * LAKE_WATER_RADIUS * f));
				n++;
			}
		}
		double level = Math.max(seaLevel, Math.round(sum / n) - 4);
		lakeWaterLevel = level;
		return level;
	}

	public static InkLandsLayout create(Climate.Sampler sampler) {
		return new InkLandsLayout(sampler, findCenter(sampler));
	}

	public Climate.Sampler sampler() {
		return sampler;
	}

	public BlockPos center() {
		return center;
	}

	/** Расстояние до центра с учётом рваного края. */
	private double edgeDistance(int x, int z) {
		double dx = x - center.getX();
		double dz = z - center.getZ();
		return Math.sqrt(dx * dx + dz * dz) + edgeNoise.getValue(x / EDGE_SCALE, z / EDGE_SCALE) * EDGE_WOBBLE;
	}

	/**
	 * Насколько глубоко точка внутри Чернильных Земель (в блоках, с учётом рваного края).
	 * Больше нуля — внутри, меньше нуля — снаружи.
	 */
	public double depthInside(int x, int z) {
		return RADIUS - edgeDistance(x, z);
	}

	/**
	 * Какой биом поставить в точке (координаты в блоках).
	 * @return наш биом или {@code null}, если точка вне Чернильных Земель.
	 */
	public ResourceKey<Biome> pick(int x, int z, Holder<Biome> original) {
		double dx = x - center.getX();
		double dz = z - center.getZ();
		if (dx * dx + dz * dz > (double) (RADIUS + EDGE_WOBBLE) * (RADIUS + EDGE_WOBBLE)) return null;
		double d = edgeDistance(x, z);
		if (d > RADIUS) return null;

		// Смоляное Озеро — одно, крупное; прочая вода внутри области — просто вода своей зоны
		if (nearLake(x, z, LAKE_ZONE_RADIUS) && lakeDistance(x, z) < LAKE_ZONE_RADIUS) return ModBiomes.TAR_LAKE;
		if (d < CORE_RADIUS) return ModBiomes.INKWOOD;
		if (isBlood(x, z)) return ModBiomes.BLOODWOOD;
		return ModBiomes.PALE_FOREST;
	}

	// ----- Кроволесье -----
	private boolean isBlood(int x, int z) {
		int cellX = Math.floorDiv(x, BLOOD_CELL);
		int cellZ = Math.floorDiv(z, BLOOD_CELL);
		double wobble = Double.NaN; // шум считаем, только если рядом есть пятно
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob == null) continue;
				double bx = x - blob[0], bz = z - blob[1];
				double dist = Math.sqrt(bx * bx + bz * bz);
				if (dist - BLOOD_WOBBLE >= blob[2]) continue;
				if (Double.isNaN(wobble)) wobble = detailNoise.getValue(x / 30.0, z / 30.0) * BLOOD_WOBBLE;
				if (dist + wobble < blob[2]) return true;
			}
		}
		return false;
	}

	/** Пятна по ячейкам (считаются один раз — выбор зоны спрашивает их очень часто). */
	private final java.util.concurrent.ConcurrentHashMap<Long, long[]> bloodBlobs = new java.util.concurrent.ConcurrentHashMap<>();
	private static final long[] NO_BLOB = new long[0];

	/** Пятно в ячейке: {x, z, радиус} или null. */
	private long[] bloodBlob(int cellX, int cellZ) {
		long[] blob = bloodBlobs.computeIfAbsent(((long) cellX << 32) ^ (cellZ & 0xffffffffL), k -> {
			long[] b = computeBloodBlob(cellX, cellZ);
			return b == null ? NO_BLOB : b;
		});
		return blob == NO_BLOB ? null : blob;
	}

	private long[] computeBloodBlob(int cellX, int cellZ) {
		RandomSource r = RandomSource.create(seed * 31 + cellX * 341873128712L + cellZ * 132897987541L);
		if (r.nextDouble() > BLOOD_CHANCE) return null;
		long bx = (long) ((cellX + 0.2 + r.nextDouble() * 0.6) * BLOOD_CELL);
		long bz = (long) ((cellZ + 0.2 + r.nextDouble() * 0.6) * BLOOD_CELL);
		long radius = BLOOD_MIN_RADIUS + r.nextInt(BLOOD_MAX_RADIUS - BLOOD_MIN_RADIUS + 1);
		double dx = bx - center.getX(), dz = bz - center.getZ();
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < CORE_RADIUS + BLOOD_INNER_MARGIN + radius || d > RADIUS * BLOOD_OUTER_FRACTION - radius) return null;
		double lx = bx - lakeCenter.getX(), lz = bz - lakeCenter.getZ();
		if (Math.sqrt(lx * lx + lz * lz) < LAKE_ZONE_RADIUS + radius + VALLEY_SLOPE) return null; // не наезжать на Озеро
		return new long[]{bx, bz, radius};
	}

	// ----- Низина Кроволесья -----
	/**
	 * Высота земли с учётом низин Кроволесья: плоское дно внутри пятна и плавный склон вокруг.
	 * @param natural естественная высота в этой точке
	 * @param baseHeight высота рельефа (x, z) → y — нужна, чтобы одинаково посчитать дно для всех чанков
	 * @return новая высота или {@code natural}, если точка вне влияния низин
	 */
	public double valleyHeight(int x, int z, double natural, IntBinaryOperator baseHeight) {
		int cellX = Math.floorDiv(x, BLOOD_CELL);
		int cellZ = Math.floorDiv(z, BLOOD_CELL);
		double result = natural;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob == null) continue;
				double bx = x - blob[0], bz = z - blob[1];
				double dist = Math.sqrt(bx * bx + bz * bz);
				if (dist > blob[2] + VALLEY_SLOPE) continue;
				double floor = valleyFloor(cellX + i, cellZ + j, blob, baseHeight);
				double t = Mth.clamp((dist - blob[2]) / VALLEY_SLOPE, 0, 1);
				t = t * t * (3 - 2 * t); // плавный (smoothstep) склон
				result = Math.min(result, Mth.lerp(t, floor, natural));
			}
		}
		return result;
	}

	private double valleyFloor(int cellX, int cellZ, long[] blob, IntBinaryOperator baseHeight) {
		return valleyFloors.computeIfAbsent(((long) cellX << 32) ^ (cellZ & 0xffffffffL), k -> {
			double sum = 0;
			int n = 0;
			for (int a = 0; a < 8; a++) {
				double angle = a * Mth.TWO_PI / 8;
				for (double f : new double[]{0, 0.5, 1.0}) {
					sum += baseHeight.applyAsInt((int) (blob[0] + Math.cos(angle) * blob[2] * f),
							(int) (blob[1] + Math.sin(angle) * blob[2] * f));
					n++;
				}
			}
			return sum / n - VALLEY_DEPTH;
		});
	}

	// ----- Дороги -----
	/** Точка, где начинается дорога номер index (0..ROADS-1) — чуть за краем области. */
	public BlockPos roadStart(int index) {
		double d = RADIUS * 1.04;
		double angle = roadAngles[Math.floorMod(index, ROADS)] + roadNoise.getValue(d / ROAD_MEANDER_SCALE, Math.floorMod(index, ROADS) * 10.0) * ROAD_MEANDER;
		return new BlockPos(center.getX() + (int) Math.round(Math.cos(angle) * d), 0,
				center.getZ() + (int) Math.round(Math.sin(angle) * d));
	}

	private double roadAngle(int road, double d) {
		double a = roadAngles[road] + roadNoise.getValue(d / ROAD_MEANDER_SCALE, road * 10.0) * ROAD_MEANDER;
		a = avoidLake(a, d);
		// и стволы Багряных Вестников (в центрах пятен Кроволесья)
		int px = center.getX() + (int) Math.round(Math.cos(a) * d), pz = center.getZ() + (int) Math.round(Math.sin(a) * d);
		int cellX = Math.floorDiv(px, BLOOD_CELL), cellZ = Math.floorDiv(pz, BLOOD_CELL);
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob != null) a = avoidCircle(a, d, blob[0], blob[1], com.tarkrakov.tearsofink.world.herald.BloodHerald.ROAD_AVOID);
			}
		}
		return a;
	}

	/** Если дорога на этом радиусе попадает в Озеро — сдвигаем её вбок, на берег (в ту сторону, где она и так шла). */
	private double avoidLake(double a, double d) {
		if (lakeCenter == null) return a;
		return avoidCircle(a, d, lakeCenter.getX(), lakeCenter.getZ(), LAKE_ROAD_AVOID);
	}

	/** Сдвинуть дорогу (угол a на радиусе d от центра области) так, чтобы она обошла круг (cx, cz, r). */
	private double avoidCircle(double a, double d, double cx, double cz, double r) {
		double lx = cx - center.getX(), lz = cz - center.getZ();
		double dist = Math.sqrt(lx * lx + lz * lz), phi = Math.atan2(lz, lx);
		double c = (d * d + dist * dist - r * r) / (2 * d * dist);
		if (c >= 1 || c <= -1) return a; // на этом радиусе круг Озера дорогу не задевает
		double theta = Math.acos(c);
		double diff = Mth.wrapDegrees((a - phi) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
		if (Math.abs(diff) >= theta) return a;
		return phi + (diff >= 0 ? theta : -theta);
	}

	/**
	 * Где стоит k-й фонарный столб дороги (на краю полотна).
	 * @return {x, z} или null, если столб вне Чернильных Земель
	 */
	public int[] roadLanternPost(int road, int k) {
		double t = (double) k * ROAD_LANTERN_SPACING;
		if (t < ROAD_END_RADIUS + 10 || t > RADIUS * ROAD_START_FRACTION) return null;
		double a = roadAngle(road, t);
		double side = (k % 2 == 0) ? 1 : -1;
		double offset = ROAD_HALF_WIDTH - 0.6;
		int x = (int) Math.floor(center.getX() + Math.cos(a) * t - Math.sin(a) * offset * side);
		int z = (int) Math.floor(center.getZ() + Math.sin(a) * t + Math.cos(a) * offset * side);
		if (depthInside(x, z) < 0) return null;
		return new int[]{x, z};
	}

	/** Лежит ли точка на одной из магистралей. */
	public boolean isRoad(int x, int z) {
		return roadAt(x, z) != null;
	}

	/** Если точка на дороге — {номер дороги, расстояние вдоль дороги от центра, расстояние до осевой}, иначе null. */
	public double[] roadAt(int x, int z) {
		return roadAt(x, z, ROAD_HALF_WIDTH);
	}

	/** Ближе ли точка к осевой дороги, чем полуширина + extra (для запрета деревьев у обочины). */
	public boolean nearRoad(int x, int z, double extra) {
		return roadAt(x, z, ROAD_HALF_WIDTH + extra) != null;
	}

	private double[] roadAt(int x, int z, double halfWidth) {
		double dx = x - center.getX();
		double dz = z - center.getZ();
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < ROAD_END_RADIUS || d > RADIUS * ROAD_START_FRACTION) return null;
		double angle = Math.atan2(dz, dx);
		for (int i = 0; i < ROADS; i++) {
			double diff = Mth.wrapDegrees((angle - roadAngle(i, d)) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
			if (Math.abs(diff) > Math.PI / 2 || Math.abs(Math.sin(diff)) * d > halfWidth + 14) continue;
			// настоящее расстояние до осевой линии (с учётом изгибов)
			double best = Double.MAX_VALUE, bestT = d;
			for (double t = d - 10; t <= d + 10; t += 1.0) {
				if (t < ROAD_END_RADIUS) continue;
				double a = roadAngle(i, t);
				double cx = center.getX() + Math.cos(a) * t - x;
				double cz = center.getZ() + Math.sin(a) * t - z;
				double dist2 = cx * cx + cz * cz;
				if (dist2 < best) { best = dist2; bestT = t; }
			}
			double dist = Math.sqrt(best);
			if (dist <= halfWidth) return new double[]{i, bestT, dist};
		}
		return null;
	}

	/**
	 * Высота полотна дороги в этом месте.
	 * @param surface высота поверхности (с водой) (x, z) → y
	 * @param floor высота дна/твёрдой земли (x, z) → y
	 * @param seaLevel уровень моря
	 */
	public double roadHeight(int road, double d, IntBinaryOperator surface, IntBinaryOperator floor, int seaLevel) {
		double[][] profiles = roadProfiles;
		if (profiles == null) {
			synchronized (this) {
				profiles = roadProfiles;
				if (profiles == null) {
					profiles = buildRoadProfiles(surface, floor);
					roadProfiles = profiles;
				}
			}
		}
		double[] profile = profiles[road];
		double t = (d - ROAD_END_RADIUS) / ROAD_PROFILE_STEP;
		int i = Mth.clamp((int) Math.floor(t), 0, profile.length - 2);
		return Mth.lerp(Mth.clamp(t - i, 0, 1), profile[i], profile[i + 1]);
	}

	private double[][] buildRoadProfiles(IntBinaryOperator surface, IntBinaryOperator floor) {
		int samples = (int) Math.ceil((RADIUS * ROAD_START_FRACTION - ROAD_END_RADIUS) / ROAD_PROFILE_STEP) + 2;
		double maxStep = ROAD_MAX_GRADE * ROAD_PROFILE_STEP;
		double[][] result = new double[ROADS][];
		for (int road = 0; road < ROADS; road++) {
			double[] raw = new double[samples];
			boolean[] water = new boolean[samples];
			for (int i = 0; i < samples; i++) {
				double d = ROAD_END_RADIUS + i * ROAD_PROFILE_STEP;
				double a = roadAngle(road, d);
				int px = center.getX() + (int) Math.round(Math.cos(a) * d);
				int pz = center.getZ() + (int) Math.round(Math.sin(a) * d);
				int top = surface.applyAsInt(px, pz);
				int ground = floor.applyAsInt(px, pz);
				water[i] = top > ground;
				raw[i] = water[i] ? top + BRIDGE_CLEARANCE : valleyHeight(px, pz, top, surface);
			}
			// сглаживание
			double[] p = new double[samples];
			for (int i = 0; i < samples; i++) {
				double sum = 0;
				int n = 0;
				for (int k = -ROAD_SMOOTHING; k <= ROAD_SMOOTHING; k++) {
					int j = i + k;
					if (j < 0 || j >= samples) continue;
					sum += raw[j];
					n++;
				}
				p[i] = water[i] ? Math.max(sum / n, raw[i]) : Math.min(sum / n, raw[i] + ROAD_MAX_FILL);
			}
			// пологий уклон: срезаем слишком крутые подъёмы (дорога режет горы)
			for (int pass = 0; pass < 2; pass++) {
				for (int i = 1; i < samples; i++) if (!water[i]) p[i] = Math.min(p[i], p[i - 1] + maxStep);
				for (int i = samples - 2; i >= 0; i--) if (!water[i]) p[i] = Math.min(p[i], p[i + 1] + maxStep);
			}
			// плавный подъём к мостам
			for (int i = 1; i < samples; i++) p[i] = Math.max(p[i], p[i - 1] - maxStep);
			for (int i = samples - 2; i >= 0; i--) p[i] = Math.max(p[i], p[i + 1] - maxStep);
			result[road] = p;
		}
		return result;
	}

	/** Ближе ли точка radius блоков к центру какого-нибудь пятна (там стоит Багряный Вестник). */
	public boolean nearHerald(int x, int z, double radius) {
		int cellX = Math.floorDiv(x, BLOOD_CELL), cellZ = Math.floorDiv(z, BLOOD_CELL);
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob == null) continue;
				double dx = x - blob[0], dz = z - blob[1];
				if (dx * dx + dz * dz < radius * radius) return true;
			}
		}
		return false;
	}

	/** Ближайшее к точке пятно Кроволесья: {x, z, радиус} или null. */
	public long[] nearestBloodBlob(int x, int z) {
		int cellX = Math.floorDiv(x, BLOOD_CELL), cellZ = Math.floorDiv(z, BLOOD_CELL);
		long[] best = null;
		double bestD = Double.MAX_VALUE;
		for (int i = -6; i <= 6; i++) {
			for (int j = -6; j <= 6; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob == null) continue;
				double dd = (blob[0] - x) * (double) (blob[0] - x) + (blob[1] - z) * (double) (blob[1] - z);
				if (dd < bestD) { bestD = dd; best = blob; }
			}
		}
		return best;
	}

	/** Ищем точку на суше на кольце вокруг 0,0. Начальный угол зависит от сида мира. */
	/**
	 * Где поставить центр: перебираем места на кольцах вокруг 0,0 и берём то, где внутри области
	 * меньше всего моря (иначе море «вливается» в Земли и топит Бледнолесье). Ближайшее к спавну
	 * место совсем без моря выигрывает сразу. Одинаково для одного сида мира.
	 */
	private static BlockPos findCenter(Climate.Sampler sampler) {
		Climate.TargetPoint origin = sampler.sample(0, 16, 0);
		long hash = origin.temperature() * 31L + origin.humidity() * 17L + origin.weirdness();
		double startAngle = Math.floorMod(hash, 360L) * Math.PI / 180.0;
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		for (int distance : CENTER_DISTANCES) {
			for (int i = 0; i < 24; i++) {
				double angle = startAngle + i * Math.PI / 12.0;
				int x = (int) Math.round(Math.cos(angle) * distance);
				int z = (int) Math.round(Math.sin(angle) * distance);
				if (Climate.unquantizeCoord(sampler.sample(x >> 2, 16, z >> 2).continentalness()) < 0.15f) continue;
				double score = seaFraction(sampler, x, z);
				if (score < bestScore) {
					bestScore = score;
					best = new BlockPos(x, 0, z);
				}
			}
			if (bestScore <= 0.0) break; // нашли место совсем без моря — дальше не ищем
		}
		if (best != null) return best;
		return new BlockPos((int) (Math.cos(startAngle) * CENTER_DISTANCES[0]), 0,
				(int) (Math.sin(startAngle) * CENTER_DISTANCES[0]));
	}

	/** Доля моря и побережья внутри области с центром x, z (0 — сплошная суша). */
	private static double seaFraction(Climate.Sampler sampler, int cx, int cz) {
		int sea = 0, total = 0;
		for (int ring = 1; ring <= 5; ring++) {
			double r = (RADIUS + EDGE_WOBBLE) * ring / 5.0;
			int points = 8 + ring * 6;
			for (int k = 0; k < points; k++) {
				double a = k * Math.PI * 2 / points;
				int x = cx + (int) Math.round(Math.cos(a) * r), z = cz + (int) Math.round(Math.sin(a) * r);
				if (Climate.unquantizeCoord(sampler.sample(x >> 2, 16, z >> 2).continentalness()) < -0.11f) sea++;
				total++;
			}
		}
		return (double) sea / total;
	}
}
