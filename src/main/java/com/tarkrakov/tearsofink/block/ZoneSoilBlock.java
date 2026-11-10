package com.tarkrakov.tearsofink.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Почва зоны. Костная мука выращивает на ней и вокруг траву этой зоны (как ванильный блок травы). */
public class ZoneSoilBlock extends Block implements BonemealableBlock {
	private final Supplier<Block> grass;
	private final Supplier<Block> tallGrass;

	public ZoneSoilBlock(Properties properties, Supplier<Block> grass, Supplier<Block> tallGrass) {
		super(properties);
		this.grass = grass;
		this.tallGrass = tallGrass;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return level.getBlockState(pos.above()).isAir();
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		for (int i = 0; i < 48; i++) {
			BlockPos p = pos.offset(random.nextInt(7) - 3, 1, random.nextInt(7) - 3);
			if (!level.getBlockState(p).isAir() || !(level.getBlockState(p.below()).getBlock() instanceof ZoneSoilBlock)) continue;
			if (random.nextInt(5) == 0 && level.getBlockState(p.above()).isAir()) {
				DoublePlantBlock.placeAt(level, tallGrass.get().defaultBlockState(), p, 2);
			} else if (random.nextInt(2) == 0) {
				level.setBlock(p, grass.get().defaultBlockState(), 2);
			}
		}
	}
}
