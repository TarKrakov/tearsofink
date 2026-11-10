package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * Прокладывает магистральные дороги Чернильных Земель.
 * Полотно идёт по сглаженному рельефу: холмы до 5 блоков срезаются, ямы до 5 блоков засыпаются.
 */
public class InkRoadsFeature extends Feature<NoneFeatureConfiguration> {
	/** Сколько блоков воздуха оставить над дорогой (чтобы не упираться головой). */
	private static final int HEADROOM = 3;

	public InkRoadsFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		ChunkGenerator generator = context.chunkGenerator();
		if (!(generator.getBiomeSource() instanceof InkLandsAccess access)) return false;
		ServerLevel server = level.getLevel();
		RandomState randomState = server.getChunkSource().randomState();
		InkLandsLayout layout = access.tearsofink$layout(randomState.sampler());
		if (layout == null) return false;

		BlockPos origin = context.origin();
		int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
		BlockState road = ModBlocks.ASHEN_PATH.defaultBlockState();
		BlockState fill = ModBlocks.ASPHODELITE_ROCK.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean placed = false;

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = minX + dx, z = minZ + dz;
				double[] hit = layout.roadAt(x, z);
				if (hit == null) continue;

				int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				pos.set(x, surface, z);
				if (!level.getBlockState(pos).getFluidState().isEmpty()) continue; // через воду не идём

				double profile = layout.roadHeight((int) hit[0], hit[1],
						(bx, bz) -> generator.getBaseHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1);
				int target = Mth.clamp((int) Math.round(profile),
						surface - InkLandsLayout.ROAD_MAX_CUT_FILL, surface + InkLandsLayout.ROAD_MAX_CUT_FILL);

				// Насыпь: заполняем от земли до полотна
				for (int y = surface + 1; y < target; y++) {
					pos.setY(y);
					level.setBlock(pos, fill, 2);
				}
				// Полотно
				pos.setY(target);
				level.setBlock(pos, road, 2);
				// Срез холма и место над головой
				int clearTo = Math.max(surface, target + HEADROOM);
				for (int y = target + 1; y <= clearTo; y++) {
					pos.setY(y);
					BlockState state = level.getBlockState(pos);
					if (!state.isAir() && state.getFluidState().isEmpty()) level.setBlock(pos, air, 2);
				}
				placed = true;
			}
		}
		return placed;
	}
}
