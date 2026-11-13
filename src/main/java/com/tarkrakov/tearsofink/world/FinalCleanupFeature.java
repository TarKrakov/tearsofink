package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Последняя уборка чанка (стоит в конце списка фич всех зон):
 * 1) над полотном магистралей — свободно ROAD_CLEARANCE блоков: никакой листвы, веток, травы, лиан
 *    (столбы с фонарями и перила мостов остаются);
 * 2) Трясинник, оставшийся висеть без воды под собой, убирается.
 */
public class FinalCleanupFeature extends Feature<NoneFeatureConfiguration> {
	/** Сколько блоков над дорогой должно быть свободно (с запасом под ездовых — жабье такси!). */
	public static final int ROAD_CLEARANCE = 6;

	public FinalCleanupFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		try {
			return placeInner(context);
		} catch (Throwable t) {
			com.tarkrakov.tearsofink.TearsOfInk.LOGGER.error("Ошибка в финальной уборке чанка", t);
			return false;
		}
	}

	private boolean placeInner(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		ChunkGenerator generator = context.chunkGenerator();
		if (!(generator.getBiomeSource() instanceof InkLandsAccess access)) return false;
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		InkLandsLayout layout = access.tearsofink$layout(randomState.sampler());
		if (layout == null) return false;

		BlockPos origin = context.origin();
		int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean changed = false;
		// Трясинник растёт только у Озера — дальше его не ищем
		boolean nearLake = layout.nearLake(minX + 8, minZ + 8, InkLandsLayout.LAKE_ZONE_RADIUS + 24);

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = minX + dx, z = minZ + dz;
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

				// ----- висячий Трясинник -----
				for (int y = top; nearLake && y > top - 24 && y > level.getMinBuildHeight(); y--) {
					if (level.getBlockState(pos.set(x, y, z)).is(ModBlocks.BOGLEAF)
							&& !level.getFluidState(pos.set(x, y - 1, z)).is(net.minecraft.tags.FluidTags.WATER)) {
						level.setBlock(pos.set(x, y, z), air, 2);
						changed = true;
					}
				}

				// ----- просвет над дорогой -----
				if (!layout.isRoad(x, z)) continue;
				int road = roadTop(level, pos, x, z, top);
				if (road == Integer.MIN_VALUE) continue;
				for (int y = road + 1; y <= road + ROAD_CLEARANCE; y++) {
					BlockState s = level.getBlockState(pos.set(x, y, z));
					if (s.isAir() || !s.getFluidState().isEmpty() || keep(s)) continue;
					level.setBlock(pos, air, 2);
					changed = true;
				}
			}
		}
		return changed;
	}

	/**
	 * Просвет над дорогой в одной точке (x, z): ROAD_CLEARANCE блоков воздуха над полотном.
	 * Нужен и постройкам, которые встают после уборки (Вестник: дорога прорезает арку в кроне).
	 */
	public static boolean clearAboveRoad(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int top) {
		int road = Integer.MIN_VALUE;
		for (int y = top; y > top - 220 && y > level.getMinBuildHeight(); y--) { // крона высокая — ищем глубже обычного
			BlockState s = level.getBlockState(pos.set(x, y, z));
			if (s.is(ModBlocks.ASHEN_PATH) || s.is(ModBlocks.PALE_PLANKS)) { road = y; break; }
		}
		if (road == Integer.MIN_VALUE) return false;
		boolean changed = false;
		for (int y = road + 1; y <= road + ROAD_CLEARANCE; y++) {
			BlockState s = level.getBlockState(pos.set(x, y, z));
			if (s.isAir() || !s.getFluidState().isEmpty() || keep(s)) continue;
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			changed = true;
		}
		return changed;
	}

	/** Наше «дорожное» — не трогаем: столбы, фонари, перила. */
	private static boolean keep(BlockState s) {
		return s.is(ModBlocks.PALE_FENCE) || s.is(ModBlocks.INK_LANTERN) || s.is(ModBlocks.ETERNAL_LANTERN)
				|| s.is(ModBlocks.STONE_LANTERN);
	}

	/** Верх полотна (тропа или настил моста) под точкой; ищем сверху вниз. */
	private static int roadTop(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int from) {
		for (int y = from; y > from - 48 && y > level.getMinBuildHeight(); y--) {
			BlockState s = level.getBlockState(pos.set(x, y, z));
			if (s.is(ModBlocks.ASHEN_PATH) || s.is(ModBlocks.PALE_PLANKS)) return y;
		}
		return Integer.MIN_VALUE;
	}
}
