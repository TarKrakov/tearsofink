package com.tarkrakov.tearsofink.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Листва чернильного дерева: с неё иногда капают чернила. */
public class InkLeavesBlock extends LeavesBlock {
	/** Шанс капли за тик отрисовки (1 из N). */
	public static final int DRIP_CHANCE = 15;

	public InkLeavesBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		super.animateTick(state, level, pos, random);
		if (random.nextInt(DRIP_CHANCE) != 0) return;
		BlockPos below = pos.below();
		BlockState belowState = level.getBlockState(below);
		if (belowState.canOcclude() && belowState.isFaceSturdy(level, below, Direction.UP)) return;
		// Пока ванильная «слеза» (фиолетовая капля). Позже заменим на свою чёрную каплю чернил.
		level.addParticle(ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
				pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(), 0, 0, 0);
	}
}
