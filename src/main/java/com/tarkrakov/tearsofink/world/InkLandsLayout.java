package com.tarkrakov.tearsofink.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Раскладка Чернильных Земель: одна область на мир.
 * От края к центру: Бледнолесье → (проблески Кроволесья) → Чернолесье. Вся вода внутри — Смоляное озеро.
 * Центр выбирается автоматически (на суше) и одинаков для одного и того же сида мира.
 */
public final class InkLandsLayout {
	// ===== Настройки размеров (в блоках) =====
	/** На каком расстоянии от точки 0,0 ищем место для центра. */
	public static final int[] CENTER_DISTANCES = {2000, 1600, 2400};
	/** Радиус всей области. */
	public static final int RADIUS = 1200;
	/** Радиус ядра — Чернолесья. */
	public static final int CORE_RADIUS = 400;
	/** Насколько рвутся края (в блоках, ±). */
	public static final int EDGE_WOBBLE = 140;
	/** Масштаб «рваности» краёв: больше — плавнее изгибы. */
	public static final double EDGE_SCALE = 380.0;
	/** Пятна Кроволесья: размер пятен и как часто они встречаются (порог 0..1, выше — реже). */
	public static final double BLOOD_SCALE = 110.0;
	public static final double BLOOD_THRESHOLD = 0.55;
	/** Кроволесье только в средней полосе: от ядра+запас до этой доли радиуса. */
	public static final double BLOOD_OUTER_FRACTION = 0.85;
	public static final int BLOOD_INNER_MARGIN = 60;

	private final Climate.Sampler sampler;
	private final BlockPos center;
	private final SimplexNoise edgeNoise;
	private final SimplexNoise bloodNoise;

	private InkLandsLayout(Climate.Sampler sampler, BlockPos center) {
		this.sampler = sampler;
		this.center = center;
		RandomSource random = RandomSource.create(center.asLong() ^ 0x7E4125L);
		this.edgeNoise = new SimplexNoise(random);
		this.bloodNoise = new SimplexNoise(random);
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

	/**
	 * Какой биом поставить в точке (координаты в блоках).
	 * @return наш биом или {@code null}, если точка вне Чернильных Земель.
	 */
	public ResourceKey<Biome> pick(int x, int z, Holder<Biome> original) {
		double dx = x - center.getX();
		double dz = z - center.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance > RADIUS + EDGE_WOBBLE) return null;

		double wobble = edgeNoise.getValue(x / EDGE_SCALE, z / EDGE_SCALE) * EDGE_WOBBLE;
		double d = distance + wobble;
		if (d > RADIUS) return null;

		if (isWater(original)) return ModBiomes.TAR_LAKE;
		if (d < CORE_RADIUS) return ModBiomes.INKWOOD;
		if (d > CORE_RADIUS + BLOOD_INNER_MARGIN && d < RADIUS * BLOOD_OUTER_FRACTION) {
			double blood = (bloodNoise.getValue(x / BLOOD_SCALE, z / BLOOD_SCALE) + 1) / 2;
			if (blood > BLOOD_THRESHOLD) return ModBiomes.BLOODWOOD;
		}
		return ModBiomes.PALE_FOREST;
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
