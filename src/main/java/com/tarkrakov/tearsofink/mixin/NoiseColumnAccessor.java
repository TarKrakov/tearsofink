package com.tarkrakov.tearsofink.mixin;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.OptionalInt;
import java.util.function.Predicate;

@Mixin(NoiseBasedChunkGenerator.class)
public interface NoiseColumnAccessor {
	@Invoker("iterateNoiseColumn")
	OptionalInt tearsofink$iterateNoiseColumn(LevelHeightAccessor level, RandomState random, int x, int z,
			MutableObject<NoiseColumn> column, Predicate<BlockState> stop);
}
