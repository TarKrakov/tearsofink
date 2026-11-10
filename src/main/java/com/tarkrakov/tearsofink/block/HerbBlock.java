package com.tarkrakov.tearsofink.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Трава Чернильных Земель. Растёт на земле, а если задан extraSoil — ещё и на этих блоках (камень руин и т. п.). */
public class HerbBlock extends FlowerBlock {
	private final TagKey<Block> extraSoil;

	public HerbBlock(Holder<MobEffect> effect, float seconds, Properties properties, TagKey<Block> extraSoil) {
		super(effect, seconds, properties);
		this.extraSoil = extraSoil;
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return super.mayPlaceOn(state, level, pos) || (extraSoil != null && state.is(extraSoil));
	}
}
