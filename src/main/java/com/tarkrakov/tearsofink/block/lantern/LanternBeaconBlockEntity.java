package com.tarkrakov.tearsofink.block.lantern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Невидимый «маячок» внутри фонаря. Данных не хранит — нужен, чтобы игра быстро
 * находила фонари рядом (не перебирая все блоки вокруг).
 */
public class LanternBeaconBlockEntity extends BlockEntity {
	public LanternBeaconBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LANTERN_BEACON, pos, state);
	}
}
