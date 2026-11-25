package com.tarkrakov.tearsofink.block;

import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Слёзница: светится и окружена холодными искорками. */
public class TearbloomBlock extends FlowerBlock {
	/** Шанс искорки за тик отрисовки (1 из N). Меньше — чаще. */
	public static final int GLINT_CHANCE = 3;

	public TearbloomBlock(Holder<MobEffect> effect, float seconds, Properties properties) {
		super(effect, seconds, properties);
	}

	@Override
	protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
		return super.mayPlaceOn(floor, level, pos)
				|| level instanceof Level world && PantheonTemple.isTemple(world) && floor.is(ModBlocks.ASHEN_PATH);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(GLINT_CHANCE) != 0) return;
		double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
		double y = pos.getY() + 0.3 + random.nextDouble() * 0.5;
		double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
		level.addParticle(ModParticles.TEAR_GLINT, x, y, z, 0, 0, 0);
	}
}
