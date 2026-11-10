package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/** Трава у корней дерева (Чернильник у смоляных деревьев): несколько кустиков вокруг основания ствола. */
public class RootHerbDecorator extends TreeDecorator {
	public static final MapCodec<RootHerbDecorator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.floatRange(0f, 1f).fieldOf("probability").forGetter(d -> d.probability),
			BlockStateProvider.CODEC.fieldOf("plant").forGetter(d -> d.plant)
	).apply(i, RootHerbDecorator::new));

	private final float probability;
	private final BlockStateProvider plant;

	public RootHerbDecorator(float probability, BlockStateProvider plant) {
		this.probability = probability;
		this.plant = plant;
	}

	@Override
	protected TreeDecoratorType<?> type() {
		return ModTreeTypes.ROOT_HERB_DECORATOR;
	}

	@Override
	public void place(Context context) {
		if (context.logs().isEmpty()) return;
		RandomSource random = context.random();
		if (random.nextFloat() >= probability) return;
		BlockPos base = context.logs().get(0);
		for (BlockPos log : context.logs()) if (log.getY() < base.getY()) base = log;
		int count = 1 + random.nextInt(3);
		for (int i = 0; i < count * 4 && count > 0; i++) {
			BlockPos pos = base.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
			if (!context.isAir(pos)) continue;
			if (!context.level().isStateAtPosition(pos.below(), s -> s.is(BlockTags.DIRT))) continue;
			BlockState state = plant.getState(random, pos);
			context.setBlock(pos, state);
			count--;
		}
	}
}
