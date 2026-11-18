package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.mixin.NoiseColumnAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public record RoadTerrain(ChunkGenerator generator, LevelHeightAccessor level, RandomState random) {
	private static final Heightmap.Types SURFACE = Heightmap.Types.WORLD_SURFACE_WG;
	private static final Heightmap.Types FLOOR = Heightmap.Types.OCEAN_FLOOR_WG;

	record Sample(int surface, boolean water) {}

	int surface(int x, int z) {
		return generator.getBaseHeight(x, z, SURFACE, level, random) - 1;
	}

	Sample sample(int x, int z) {
		if (!(generator instanceof NoiseBasedChunkGenerator)) {
			int top = surface(x, z);
			return new Sample(top, top >= generator.getBaseHeight(x, z, FLOOR, level, random));
		}
		// FLOOR accepts a subset of SURFACE. If the highest surface block is solid,
		// the two heights coincide. Otherwise scanning the column again adds no information.
		boolean[] overFloor = {false};
		int top = ((NoiseColumnAccessor) generator).tearsofink$iterateNoiseColumn(level, random, x, z, null, state -> {
			if (!SURFACE.isOpaque().test(state)) return false;
			overFloor[0] = !FLOOR.isOpaque().test(state);
			return true;
		}).orElse(level.getMinBuildHeight()) - 1;
		return new Sample(top, overFloor[0] && top >= level.getMinBuildHeight());
	}
}
