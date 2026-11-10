package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
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
 * Магистральные дороги Чернильных Земель.
 * Полотно идёт пологим профилем: горы прорезаются выемкой, ямы засыпаются (до 6 блоков),
 * над водой строится мост из бледного дерева с опорами и перилами.
 */
public class InkRoadsFeature extends Feature<NoneFeatureConfiguration> {
	/** Сколько блоков воздуха оставить над дорогой. */
	private static final int HEADROOM = 4;
	/** Опора моста — каждые столько блоков вдоль дороги. */
	private static final int PILLAR_SPACING = 7;

	public InkRoadsFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		ChunkGenerator generator = context.chunkGenerator();
		if (!(generator.getBiomeSource() instanceof InkLandsAccess access)) return false;
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		InkLandsLayout layout = access.tearsofink$layout(randomState.sampler());
		if (layout == null) return false;

		BlockPos origin = context.origin();
		int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
		int seaLevel = generator.getSeaLevel();
		BlockState road = ModBlocks.ASHEN_PATH.defaultBlockState();
		BlockState fill = ModBlocks.ASPHODELITE_ROCK.defaultBlockState();
		BlockState deck = ModBlocks.PALE_PLANKS.defaultBlockState();
		BlockState pillar = ModBlocks.PALE_LOG.defaultBlockState();
		BlockState rail = ModBlocks.PALE_FENCE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean placed = false;

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = minX + dx, z = minZ + dz;
				double[] hit = layout.roadAt(x, z);
				if (hit == null) continue;
				int roadIndex = (int) hit[0];
				double along = hit[1], fromAxis = hit[2];

				int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
				boolean overWater = surface > ground;
				double profile = layout.roadHeight(roadIndex, along,
						(bx, bz) -> generator.getBaseHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1,
						(bx, bz) -> generator.getBaseHeight(bx, bz, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState) - 1,
						seaLevel);

				if (overWater) {
					// ----- Мост -----
					int y = Math.max((int) Math.round(profile), surface + 1);
					pos.set(x, y, z);
					level.setBlock(pos, deck, 2);
					clearAbove(level, pos, y, y + HEADROOM, air);
					if (fromAxis > InkLandsLayout.ROAD_HALF_WIDTH - 1.0 && ((int) along) % 2 == 0) {
						level.setBlock(pos.setY(y + 1), rail, 2);
					}
					if (fromAxis < 1.0 && ((int) along) % PILLAR_SPACING == 0) {
						for (int yy = y - 1; yy > ground; yy--) level.setBlock(pos.setY(yy), pillar, 2);
					}
				} else {
					// ----- Дорога по земле -----
					int y = Math.min((int) Math.round(profile), surface + InkLandsLayout.ROAD_MAX_FILL);
					for (int yy = surface + 1; yy < y; yy++) level.setBlock(pos.set(x, yy, z), fill, 2);
					level.setBlock(pos.set(x, y, z), road, 2);
					clearAbove(level, pos, y, Math.max(surface, y + HEADROOM), air);
				}
				placed = true;
			}
		}
		return placed;
	}

	private static void clearAbove(WorldGenLevel level, BlockPos.MutableBlockPos pos, int from, int to, BlockState air) {
		for (int y = from + 1; y <= to; y++) {
			pos.setY(y);
			BlockState state = level.getBlockState(pos);
			if (!state.isAir() && state.getFluidState().isEmpty()) level.setBlock(pos, air, 2);
		}
	}
}
