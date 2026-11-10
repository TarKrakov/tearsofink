package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/** Ряска на стволах и ветвях: свисает с боков брёвен вниз на 1–4 блока. */
public class DuckweedDecorator extends TreeDecorator {
	public static final MapCodec<DuckweedDecorator> CODEC = Codec.floatRange(0f, 1f).fieldOf("probability")
			.xmap(DuckweedDecorator::new, d -> d.probability);

	private final float probability;

	public DuckweedDecorator(float probability) {
		this.probability = probability;
	}

	@Override
	protected TreeDecoratorType<?> type() {
		return ModTreeTypes.DUCKWEED_DECORATOR;
	}

	@Override
	public void place(Context context) {
		RandomSource random = context.random();
		for (BlockPos log : context.logs()) {
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				if (random.nextFloat() >= probability) continue;
				BlockPos.MutableBlockPos pos = log.relative(dir).mutable();
				if (!context.isAir(pos)) continue;
				BlockState vine = ModBlocks.DUCKWEED.defaultBlockState()
						.setValue(VineBlock.getPropertyForFace(dir.getOpposite()), true);
				int length = 1 + random.nextInt(4);
				for (int i = 0; i < length && context.isAir(pos); i++) {
					context.setBlock(pos, vine);
					pos.move(Direction.DOWN);
				}
			}
		}
	}
}
