package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.lantern.FogClearing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Магистральные дороги Чернильных Земель.
 *
 * Полотно всегда идёт ровно по плавному профилю (высота зависит только от места вдоль дороги),
 * поэтому дорога непрерывна: никаких обрывов и ступенек на стыках чанков и зон.
 * <ul>
 *   <li><b>По земле</b>: тропа; горы прорезаются выемкой (с запасом по бокам), ямы — каменной насыпью
 *       с откосом на обочине.</li>
 *   <li><b>Мост</b> (над водой или эстакада над глубокой низиной) — целыми отрезками: настил из бледных досок,
 *       перила строго по краю (вне прохода), каждые {@link #BENT} блоков — пролёт: столбы-опоры до земли,
 *       балка под настилом и столбик в перилах.</li>
 * </ul>
 * Фонарные столбы стоят на обочине, не на проходе.
 */
public class InkRoadsFeature extends Feature<NoneFeatureConfiguration> {
	/** Сколько блоков воздуха оставить над дорогой. */
	private static final int HEADROOM = 4;
	/** Пролёт моста: опоры и балки каждые столько блоков вдоль дороги. */
	private static final int BENT = 8;
	/** Перила моста — полоса такой ширины сразу за краем полотна. */
	private static final double RAIL_BAND = 1.0;

	public InkRoadsFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	private record Ctx(WorldGenLevel level, InkLandsLayout layout, ChunkGenerator generator, RandomState randomState, int seaLevel) {
		double height(int road, double along) {
			return layout.roadHeight(road, along,
					(bx, bz) -> generator.getBaseHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1,
					(bx, bz) -> generator.getBaseHeight(bx, bz, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState) - 1,
					seaLevel);
		}
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		ChunkGenerator generator = context.chunkGenerator();
		if (!(generator.getBiomeSource() instanceof InkLandsAccess access)) return false;
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		InkLandsLayout layout = access.tearsofink$layout(randomState.sampler());
		if (layout == null) return false;
		Ctx ctx = new Ctx(level, layout, generator, randomState, generator.getSeaLevel());

		BlockPos origin = context.origin();
		int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
		BlockState path = ModBlocks.ASHEN_PATH.defaultBlockState();
		BlockState rock = ModBlocks.ASPHODELITE_ROCK.defaultBlockState();
		BlockState deck = ModBlocks.PALE_PLANKS.defaultBlockState();
		BlockState pillar = ModBlocks.PALE_LOG.defaultBlockState();
		BlockState rail = ModBlocks.PALE_FENCE.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean placed = false;

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = minX + dx, z = minZ + dz;
				double[] hit = layout.roadNear(x, z, InkLandsLayout.ROAD_SHOULDER);
				if (hit == null) continue;
				int road = (int) hit[0];
				double along = hit[1], fromAxis = hit[2];
				boolean lane = fromAxis <= InkLandsLayout.ROAD_HALF_WIDTH;

				int y = (int) Math.round(ctx.height(road, along));
				int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
				boolean bridge = layout.roadBridge(road, along);
				boolean bent = Math.floorMod((int) Math.floor(along), BENT) == 0;

				if (bridge) {
					if (!lane && fromAxis > InkLandsLayout.ROAD_HALF_WIDTH + RAIL_BAND) continue;
					// ----- Мост -----
					level.setBlock(pos.set(x, y, z), deck, 2);
					if (lane) {
						clear(level, pos, x, z, y + 1, Math.max(surface, y + HEADROOM));
						if (bent && y - 1 > ground) level.setBlock(pos.set(x, y - 1, z), beam(layout, road, along), 2);
					} else {
						// перила по краю; на пролёте — столбик и опора до земли
						level.setBlock(pos.set(x, y + 1, z), bent ? pillar : rail, 2);
						clear(level, pos, x, z, y + 2, Math.max(surface, y + HEADROOM));
						if (bent) for (int yy = y - 1; yy > ground; yy--) level.setBlock(pos.set(x, yy, z), pillar, 2);
					}
				} else if (lane) {
					// ----- Полотно по земле: насыпь (в т.ч. через ручьи) или выемка -----
					for (int yy = ground + 1; yy < y; yy++) level.setBlock(pos.set(x, yy, z), rock, 2);
					level.setBlock(pos.set(x, y, z), path, 2);
					clear(level, pos, x, z, y + 1, Math.max(surface, y + HEADROOM));
					if (surface > y) WorldgenCleanup.removeFloatingPlants(level, x, surface + 1, z);
				} else {
					// ----- Обочина: откос насыпи на ступень ниже полотна или расширение выемки -----
					if (surface < y - 1) {
						for (int yy = ground + 1; yy < y - 1; yy++) level.setBlock(pos.set(x, yy, z), rock, 2);
						level.setBlock(pos.set(x, y - 1, z), rock, 2);
						clear(level, pos, x, z, y, y + HEADROOM);
					} else {
						if (level.getBlockState(pos.set(x, Math.min(surface, y), z)).isAir()) level.setBlock(pos, rock, 2);
						clear(level, pos, x, z, Math.min(surface, y) + 1, Math.max(surface, y + HEADROOM));
						if (surface > y) WorldgenCleanup.removeFloatingPlants(level, x, surface + 1, z);
					}
				}
				placed = true;
			}
		}
		if (placed) placeLanternPosts(ctx, minX, minZ, rail, rock);
		return placed;
	}

	/** Балка под настилом — бревно поперёк дороги. */
	private static BlockState beam(InkLandsLayout layout, int road, double along) {
		double[] dir = layout.roadDirection(road, along);
		Direction.Axis across = Math.abs(dir[0]) > Math.abs(dir[1]) ? Direction.Axis.Z : Direction.Axis.X;
		return ModBlocks.STRIPPED_PALE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, across);
	}

	/**
	 * Фонарные столбы: забор в 2 блока + Чернильный фонарь сверху, на обочине.
	 * В Бледнолесье и Кроволесье горят, в Чернолесье потухшие, на Смоляном озере — вперемешку.
	 */
	private static void placeLanternPosts(Ctx ctx, int minX, int minZ, BlockState post, BlockState rock) {
		WorldGenLevel level = ctx.level();
		InkLandsLayout layout = ctx.layout();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int maxK = (int) (InkLandsLayout.RADIUS * InkLandsLayout.ROAD_START_FRACTION / InkLandsLayout.ROAD_LANTERN_SPACING) + 1;
		for (int road = 0; road < InkLandsLayout.ROADS; road++) {
			for (int k = 1; k <= maxK; k++) {
				int[] p = layout.roadLanternPost(road, k);
				if (p == null || p[0] < minX || p[0] >= minX + 16 || p[1] < minZ || p[1] >= minZ + 16) continue;
				double along = (double) k * InkLandsLayout.ROAD_LANTERN_SPACING;
				int y = (int) Math.round(ctx.height(road, along));
				boolean bridge = layout.roadBridge(road, along);
				int x = p[0], z = p[1];

				if (!bridge) {
					// подножие: камень от земли до уровня полотна
					int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
					int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
					for (int yy = Math.min(surface, ground) + 1; yy <= y; yy++) level.setBlock(pos.set(x, yy, z), rock, 2);
				}

				var biome = level.getBiome(pos.set(x, y + 1, z));
				boolean lit;
				if (biome.is(ModBiomes.INKWOOD)) lit = false;
				else if (biome.is(ModBiomes.TAR_LAKE)) lit = Math.floorMod(road * 31 + k * 17, 2) == 0;
				else lit = true;

				level.setBlock(pos.set(x, y + 1, z), post, 2);
				level.setBlock(pos.set(x, y + 2, z), post, 2);
				level.setBlock(pos.set(x, y + 3, z), ModBlocks.INK_LANTERN.defaultBlockState()
						.setValue(FogClearing.LIT, lit).setValue(FogClearing.PLAYER_MADE, false), 2);
			}
		}
	}

	private static void clear(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int from, int to) {
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int y = from; y <= to; y++) {
			if (!level.getBlockState(pos.set(x, y, z)).isAir()) level.setBlock(pos, air, 2);
		}
		// в стенках выемки могли открыться подземные воды — заделать
		WorldgenCleanup.sealFluidsAround(level, x, z, from - 1, to, null);
	}
}
