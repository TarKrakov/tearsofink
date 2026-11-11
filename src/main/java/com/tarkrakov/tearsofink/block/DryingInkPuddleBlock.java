package com.tarkrakov.tearsofink.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Чернильная лужица из следа чернильной гусеницы: через 1–2 минуты высыхает. */
public class DryingInkPuddleBlock extends InkPuddleBlock {
	/** Сколько тиков до высыхания (20 = 1 секунда): от MIN до MIN+SPREAD. */
	public static final int DRY_MIN = 60 * 20, DRY_SPREAD = 60 * 20;

	public DryingInkPuddleBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		level.scheduleTick(pos, this, DRY_MIN + level.getRandom().nextInt(DRY_SPREAD));
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.removeBlock(pos, false);
	}
}
