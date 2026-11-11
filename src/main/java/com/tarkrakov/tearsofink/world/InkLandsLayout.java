package com.tarkrakov.tearsofink.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.function.IntBinaryOperator;

/**
 * Раскладка Чернильных Земель: одна область на мир.
 * От края к центру: Бледнолесье → (круглые пятна Кроволесья) → Чернолесье. Вся вода внутри — Смоляное озеро.
 * Магистральные дороги идут от края к центру.
 * Центр выбирается автоматически (на суше) и одинаков для одного и того же сида мира.
 */
public final class InkLandsLayout {
	// ===== Размеры (в блоках) =====
	/** На каком расстоянии от точки 0,0 ищем место для центра. */
	public static final int[] CENTER_DISTANCES = {1400, 1200, 1700};
	/** Радиус всей области. */
	public static final int RADIUS = 700;
	/** Радиус ядра — Чернолесья. */
	public static final int CORE_RADIUS = 240;
	/** Насколько рвутся края области (±). */
	public static final int EDGE_WOBBLE = 110;
	/** Масштаб «рваности» краёв: больше — плавнее изгибы. */
	public static final double EDGE_SCALE = 300.0;

	// ===== Кроволесье: круглые пятна (позже — вокруг Багряных Вестников) =====
	/** Размер ячейки сетки: в каждой ячейке может быть одно пятно. */
	public static final int BLOOD_CELL = 200;
	/** Шанс, что в ячейке есть пятно. */
	public static final double BLOOD_CHANCE = 0.6;
	/** Радиус пятна: от MIN до MAX. */
	public static final int BLOOD_MIN_RADIUS = 40, BLOOD_MAX_RADIUS = 65;
	/** Лёгкая неровность края пятна (±). */
	public static final int BLOOD_WOBBLE = 7;
	/** Пятна только в средней полосе: от ядра + запас до этой доли радиуса области. */
	public static final int BLOOD_INNER_MARGIN = 50;
	public static final double BLOOD_OUTER_FRACTION = 0.88;

	// ===== Низина Кроволесья =====
	/** Насколько дно долины ниже окружающей земли. */
	public static final int VALLEY_DEPTH = 11;
	/** Ширина плавного склона вокруг пятна (за его краем). */
	public static final int VALLEY_SLOPE = 36;

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

		if (isWater(original)) return ModBiomes.TAR_LAKE;
		if (d < CORE_RADIUS) return ModBiomes.INKWOOD;
		if (isBlood(x, z)) return ModBiomes.BLOODWOOD;
		return ModBiomes.PALE_FOREST;
	}

	// ----- Кроволесье -----
	private boolean isBlood(int x, int z) {
		int cellX = Math.floorDiv(x, BLOOD_CELL);
		int cellZ = Math.floorDiv(z, BLOOD_CELL);
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob == null) continue;
				double bx = x - blob[0], bz = z - blob[1];
				double wobble = detailNoise.getValue(x / 30.0, z / 30.0) * BLOOD_WOBBLE;
				if (Math.sqrt(bx * bx + bz * bz) + wobble < blob[2]) return true;
			}
		}
		return false;
	}

	/** Пятно в ячейке: {x, z, радиус} или null. */
	private long[] bloodBlob(int cellX, int cellZ) {
		RandomSource r = RandomSource.create(seed * 31 + cellX * 341873128712L + cellZ * 132897987541L);
		if (r.nextDouble() > BLOOD_CHANCE) return null;
		long bx = (long) ((cellX + 0.2 + r.nextDouble() * 0.6) * BLOOD_CELL);
		long bz = (long) ((cellZ + 0.2 + r.nextDouble() * 0.6) * BLOOD_CELL);
		long radius = BLOOD_MIN_RADIUS + r.nextInt(BLOOD_MAX_RADIUS - BLOOD_MIN_RADIUS + 1);
		double dx = bx - center.getX(), dz = bz - center.getZ();
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < CORE_RADIUS + BLOOD_INNER_MARGIN + radius || d > RADIUS * BLOOD_OUTER_FRACTION - radius) return null;
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
		return roadAngles[road] + roadNoise.getValue(d / ROAD_MEANDER_SCALE, road * 10.0) * ROAD_MEANDER;
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
		double dx = x - center.getX();
		double dz = z - center.getZ();
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < ROAD_END_RADIUS || d > RADIUS * ROAD_START_FRACTION) return null;
		double angle = Math.atan2(dz, dx);
		for (int i = 0; i < ROADS; i++) {
			double diff = Mth.wrapDegrees((angle - roadAngle(i, d)) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
			if (Math.abs(diff) > Math.PI / 2 || Math.abs(Math.sin(diff)) * d > ROAD_HALF_WIDTH + 14) continue;
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
			if (dist <= ROAD_HALF_WIDTH) return new double[]{i, bestT, dist};
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

	private static boolean isWater(Holder<Biome> biome) {
		return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)
				|| biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
	}

	/** Ищем точку на суше на кольце вокруг 0,0. Начальный угол зависит от сида мира. */
	private static BlockPos findCenter(Climate.Sampler sampler) {
		Climate.TargetPoint origin = sampler.sample(0, 16, 0);
		long hash = origin.temperature() * 31L + origin.humidity() * 17L + origin.weirdness();
		double startAngle = Math.floorMod(hash, 360L) * Math.PI / 180.0;
		for (int distance : CENTER_DISTANCES) {
			for (int i = 0; i < 24; i++) {
				double angle = startAngle + i * Math.PI / 12.0;
				int x = (int) Math.round(Math.cos(angle) * distance);
				int z = (int) Math.round(Math.sin(angle) * distance);
				Climate.TargetPoint p = sampler.sample(x >> 2, 16, z >> 2);
				if (Climate.unquantizeCoord(p.continentalness()) > 0.15f) {
					return new BlockPos(x, 0, z);
				}
			}
		}
		return new BlockPos((int) (Math.cos(startAngle) * CENTER_DISTANCES[0]), 0,
				(int) (Math.sin(startAngle) * CENTER_DISTANCES[0]));
	}
}
