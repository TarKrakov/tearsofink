package com.tarkrakov.tearsofink.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Листва, с которой капает жидкость (кровь у Багряного дерева, чернила у Смоляного). */
public class DrippingLeavesBlock extends LeavesBlock {
	/** Шанс капли за тик отрисовки (1 из N). */
	public static final int DRIP_CHANCE = 12;
	private final Supplier<ParticleOptions> drip;

	public DrippingLeavesBlock(Supplier<ParticleOptions> drip, Properties properties) {
		super(properties);
		this.drip = drip;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		super.animateTick(state, level, pos, random);
		if (random.nextInt(DRIP_CHANCE) != 0) return;
		BlockPos below = pos.below();
		BlockState belowState = level.getBlockState(below);
		if (belowState.canOcclude() && belowState.isFaceSturdy(level, below, Direction.UP)) return;
		level.addParticle(drip.get(), pos.getX() + 0.1 + random.nextDouble() * 0.8, pos.getY() - 0.05,
				pos.getZ() + 0.1 + random.nextDouble() * 0.8, 0, 0, 0);
	}
}
