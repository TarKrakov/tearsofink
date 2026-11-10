package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

/** Круглая полянка растений (Слёзница растёт только так): круг радиусом 3–5, густой в центре, реже к краю. */
public class HerbCircleFeature extends Feature<SimpleBlockConfiguration> {
	private static final int MIN_RADIUS = 3, MAX_RADIUS = 5;

	public HerbCircleFeature(Codec<SimpleBlockConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos center = context.origin();
		int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
		boolean placed = false;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz) / radius;
				if (d > 1.0) continue;
				// густо в центре (~85%), к краю ~35%
				if (random.nextDouble() > 0.85 - 0.5 * d) continue;
				int x = center.getX() + dx, z = center.getZ() + dz;
				pos.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
				if (!level.isEmptyBlock(pos)) continue;
				BlockState state = context.config().toPlace().getState(random, pos);
				if (!state.canSurvive(level, pos)) continue;
				level.setBlock(pos, state, 2);
				placed = true;
			}
		}
		return placed;
	}
}
