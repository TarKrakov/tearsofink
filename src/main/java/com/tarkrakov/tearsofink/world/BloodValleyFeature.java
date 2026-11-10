package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * «Вдавливает» пятна Кроволесья в землю: плоское дно и плавный склон вокруг.
 * Работает до деревьев и руд, поэтому всё остальное появляется уже на новом рельефе.
 */
public class BloodValleyFeature extends Feature<NoneFeatureConfiguration> {
	/** Сколько блоков пепла под верхним слоем почвы. */
	private static final int ASH_LAYERS = 3;

	// ===== Диагностика (показывается в /tearsofink blood) =====
	public static final java.util.concurrent.atomic.AtomicLong CALLS = new java.util.concurrent.atomic.AtomicLong();
	public static final java.util.concurrent.atomic.AtomicLong CHUNKS_WITH_VALLEY = new java.util.concurrent.atomic.AtomicLong();
	public static final java.util.concurrent.atomic.AtomicLong COLUMNS_LOWERED = new java.util.concurrent.atomic.AtomicLong();
	public static final java.util.concurrent.atomic.AtomicLong COLUMNS_SKIPPED_WATER = new java.util.concurrent.atomic.AtomicLong();
	public static volatile String lastError = "нет";
	public static volatile String lastSample = "нет";

	public BloodValleyFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		CALLS.incrementAndGet();
		try {
			return placeInner(context);
		} catch (Throwable t) {
			lastError = t.toString();
			com.tarkrakov.tearsofink.TearsOfInk.LOGGER.error("Ошибка в низине Кроволесья", t);
			return false;
		}
	}

	private boolean placeInner(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		ChunkGenerator generator = context.chunkGenerator();
		if (!(generator.getBiomeSource() instanceof InkLandsAccess access)) return false;
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		InkLandsLayout layout = access.tearsofink$layout(randomState.sampler());
		if (layout == null) return false;

		BlockPos origin = context.origin();
		int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState ash = ModBlocks.ASH.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean changed = false;

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = minX + dx, z = minZ + dz;
				int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				pos.set(x, surface, z);
				if (!level.getBlockState(pos).getFluidState().isEmpty()) { COLUMNS_SKIPPED_WATER.incrementAndGet(); continue; }
				double target = layout.valleyHeight(x, z, surface,
						(bx, bz) -> generator.getBaseHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1);
				int y = Mth.floor(target);
				if (dx == 8 && dz == 8) lastSample = "x" + x + " z" + z + ": поверхность " + surface + " → цель " + y;
				if (y == surface) continue;
				if (y < surface) COLUMNS_LOWERED.incrementAndGet();

				BlockState top = topSoil(level, pos.set(x, y, z));
				if (y < surface) {
					// срезаем всё выше нового уровня
					for (int yy = y + 1; yy <= surface; yy++) {
						pos.setY(yy);
						if (level.getBlockState(pos).getFluidState().isEmpty()) level.setBlock(pos, air, 2);
					}
				} else {
					// засыпаем ямку до нового уровня
					for (int yy = surface + 1; yy < y; yy++) {
						pos.setY(yy);
						level.setBlock(pos, ash, 2);
					}
				}
				pos.setY(y);
				level.setBlock(pos, top, 2);
				for (int k = 1; k <= ASH_LAYERS; k++) {
					pos.setY(y - k);
					BlockState under = level.getBlockState(pos);
					if (under.isAir() || under.is(ModBlocks.ASPHODELITE_ROCK)) level.setBlock(pos, ash, 2);
				}
				changed = true;
			}
		}
		if (changed) CHUNKS_WITH_VALLEY.incrementAndGet();
		return changed;
	}

	private static BlockState topSoil(WorldGenLevel level, BlockPos pos) {
		var biome = level.getBiome(pos);
		if (biome.is(ModBiomes.BLOODWOOD)) return ModBlocks.BLOOD_SOIL.defaultBlockState();
		if (biome.is(ModBiomes.INKWOOD)) return ModBlocks.TAR_SOIL.defaultBlockState();
		return ModBlocks.ASHEN_SOIL.defaultBlockState();
	}
}
