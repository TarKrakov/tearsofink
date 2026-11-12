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

	public BloodValleyFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		try {
			return placeInner(context);
		} catch (Throwable t) {
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
				double target = layout.valleyHeight(x, z, surface,
						(bx, bz) -> generator.getBaseHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1);
				int y = Mth.floor(target);
				if (y == surface) continue;

				BlockState top = topSoil(level, pos.set(x, y, z));
				if (y < surface) {
					// срезаем всё выше нового уровня
					for (int yy = y + 1; yy <= surface; yy++) {
						pos.setY(yy);
						// и землю, и воду: иначе вода остаётся висеть над низиной
						level.setBlock(pos, air, 2);
					}
					WorldgenCleanup.removeFloatingPlants(level, x, surface + 1, z);
					WorldgenCleanup.sealFluidsAround(level, x, z, y - 1, surface, null);
				} else {
					// засыпаем ямку до нового уровня (сплошной породой — она не сыплется)
					for (int yy = surface + 1; yy < y; yy++) {
						pos.setY(yy);
						level.setBlock(pos, ModBlocks.ASPHODELITE_ROCK.defaultBlockState(), 2);
					}
				}
				pos.setY(y);
				level.setBlock(pos, top, 2);
				// пепел под почвой — снизу вверх и только на опоре (иначе посыплется)
				for (int k = ASH_LAYERS; k >= 1; k--) {
					pos.setY(y - k);
					BlockState under = level.getBlockState(pos);
					if (under.isAir() || !under.getFluidState().isEmpty() || under.is(ModBlocks.ASPHODELITE_ROCK)) {
						level.setBlock(pos, WorldgenCleanup.supported(level, pos, ash), 2);
					}
				}
				changed = true;
			}
		}
		BloodFauna.populate(level, layout, minX, minZ, context.random());
		return changed;
	}

	private static BlockState topSoil(WorldGenLevel level, BlockPos pos) {
		var biome = level.getBiome(pos);
		if (biome.is(ModBiomes.BLOODWOOD)) return ModBlocks.BLOOD_SOIL.defaultBlockState();
		if (biome.is(ModBiomes.INKWOOD)) return ModBlocks.TAR_SOIL.defaultBlockState();
		return ModBlocks.ASHEN_SOIL.defaultBlockState();
	}
}
