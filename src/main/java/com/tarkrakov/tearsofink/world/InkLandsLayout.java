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

/**
 * Раскладка Чернильных Земель: одна область на мир.
 * От края к центру: Бледнолесье → (круглые пятна Кроволесья) → Чернолесье. Вся вода внутри — Смоляное озеро.
 * Магистральные дороги идут от края к центру.
 * Центр выбирается автоматически (на суше) и одинаков для одного и того же сида мира.
 */
public final class InkLandsLayout {
	// ===== Размеры (в блоках) =====
	/** На каком расстоянии от точки 0,0 ищем место для центра. */
	public static final int[] CENTER_DISTANCES = {2000, 1600, 2400};
	/** Радиус всей области. */
	public static final int RADIUS = 900;
	/** Радиус ядра — Чернолесья. */
	public static final int CORE_RADIUS = 300;
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

	// ===== Дороги =====
	/** Сколько магистралей ведёт к центру. */
	public static final int ROADS = 5;
	/** Полуширина дороги (дорога шириной ~2×этого). */
	public static final double ROAD_HALF_WIDTH = 1.6;
	/** Насколько дорога петляет (в радианах) и масштаб изгибов. */
	public static final double ROAD_MEANDER = 0.22;
	public static final double ROAD_MEANDER_SCALE = 160.0;
	/** Дороги начинаются чуть за краем области и доходят почти до центра. */
	public static final double ROAD_START_FRACTION = 1.08;
	public static final int ROAD_END_RADIUS = 30;

	private final Climate.Sampler sampler;
	private final BlockPos center;
	private final long seed;
	private final SimplexNoise edgeNoise;
	private final SimplexNoise detailNoise;
	private final SimplexNoise roadNoise;
	private final double[] roadAngles = new double[ROADS];

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

	// ----- Дороги -----
	/** Лежит ли точка на одной из магистралей. */
	public boolean isRoad(int x, int z) {
		double dx = x - center.getX();
		double dz = z - center.getZ();
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < ROAD_END_RADIUS || d > RADIUS * ROAD_START_FRACTION) return false;
		double angle = Math.atan2(dz, dx);
		for (int i = 0; i < ROADS; i++) {
			double roadAngle = roadAngles[i] + roadNoise.getValue(d / ROAD_MEANDER_SCALE, i * 10.0) * ROAD_MEANDER;
			double diff = Mth.wrapDegrees((angle - roadAngle) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
			if (Math.abs(diff) < Math.PI / 2 && Math.abs(Math.sin(diff)) * d < ROAD_HALF_WIDTH) return true;
		}
		return false;
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
