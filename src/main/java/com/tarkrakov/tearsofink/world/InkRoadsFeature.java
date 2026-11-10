package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Прокладывает магистральные дороги Чернильных Земель (по маске из {@link InkLandsLayout}). */
public class InkRoadsFeature extends Feature<NoneFeatureConfiguration> {
	public InkRoadsFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		if (!(context.chunkGenerator().getBiomeSource() instanceof InkLandsAccess access)) return false;
		ServerLevel server = level.getLevel();
		InkLandsLayout layout = access.tearsofink$layout(server.getChunkSource().randomState().sampler());
		if (layout == null) return false;

		BlockPos origin = context.origin();
		int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
		BlockState road = ModBlocks.ASHEN_PATH.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean placed = false;
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = minX + dx, z = minZ + dz;
				if (!layout.isRoad(x, z)) continue;
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				pos.set(x, top - 1, z);
				BlockState ground = level.getBlockState(pos);
				if (!ground.getFluidState().isEmpty()) continue;
				if (!(ground.is(BlockTags.DIRT) || ground.is(ModBlocks.ASH) || ground.is(Blocks.SAND)
						|| ground.is(Blocks.GRAVEL))) continue;
				level.setBlock(pos, road, 2);
				// убираем траву/цветы прямо над дорогой
				BlockState above = level.getBlockState(pos.above());
				if (above.canBeReplaced() && above.getFluidState().isEmpty()) {
					level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
				}
				placed = true;
			}
		}
		return placed;
	}
}
