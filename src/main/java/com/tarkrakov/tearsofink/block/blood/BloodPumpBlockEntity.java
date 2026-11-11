package com.tarkrakov.tearsofink.block.blood;

import com.tarkrakov.tearsofink.block.lantern.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Помпа: раз в PUMP_INTERVAL тиков качает кровь в свой Накопитель.
 * Ритм берётся от времени мира — поэтому анимация у всех игроков совпадает с ударами.
 */
public class BloodPumpBlockEntity extends BlockEntity {
	public BloodPumpBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BLOOD_PUMP, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BloodPumpBlockEntity pump) {
		if (level.getGameTime() % BloodTuning.PUMP_INTERVAL != 0) return;
		BlockPos tankPos = BloodPumpBlock.tankPos(pos, state);
		if (level.getBlockEntity(tankPos) instanceof BloodTankBlockEntity tank) {
			tank.pumpIn(level.random);
			// звук-заглушка: глухой удар поршня
			level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.35f, 0.55f);
		}
	}
}
