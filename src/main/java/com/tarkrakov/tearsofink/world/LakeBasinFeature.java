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
 * Выкапывает Смоляное Озеро: чаша с чёрной водой, островок в центре (под деревню рыболюдов),
 * пепельный пляж по кругу и плавный склон к обычной земле. Работает первым — всё остальное растёт уже на новом рельефе.
 */
public class LakeBasinFeature extends Feature<NoneFeatureConfiguration> {
	private static final int SUBSOIL_LAYERS = 3;

	public LakeBasinFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		try {
			return placeInner(context);
		} catch (Throwable t) {
			com.tarkrakov.tearsofink.TearsOfInk.LOGGER.error("Ошибка в Смоляном Озере", t);
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
		// быстрый отказ: чанк далеко от Озера
		BlockPos lc = layout.lakeCenter();
		int reach = InkLandsLayout.LAKE_WATER_RADIUS + InkLandsLayout.LAKE_BEACH + InkLandsLayout.LAKE_SLOPE
				+ InkLandsLayout.LAKE_WOBBLE + 24;
		if (Math.abs(minX + 8 - lc.getX()) > reach || Math.abs(minZ + 8 - lc.getZ()) > reach) return false;

		double waterLevel = layout.lakeWaterLevel(
				(bx, bz) -> generator.getBaseHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1,
				generator.getSeaLevel());
		int water = (int) waterLevel;
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState waterBlock = Blocks.WATER.defaultBlockState();
		BlockState ash = ModBlocks.ASH.defaultBlockState();
		BlockState tar = ModBlocks.TAR_SOIL.defaultBlockState();
		BlockState ashenSoil = ModBlocks.ASHEN_SOIL.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean changed = false;

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = minX + dx, z = minZ + dz;
				int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				double[] shape = layout.lakeShape(x, z, surface, waterLevel);
				if (shape == null) continue;
				int y = Mth.floor(shape[0]);
				boolean underWater = shape[1] > 0;
				int type = (int) shape[2];
				if (type == 3 && y == surface) continue;

				BlockState top = switch (type) {
					case 0 -> tar;          // дно
					case 1, 2 -> ash;       // остров и пляж
					default -> ashenSoil;   // склон
				};
				// срезать всё выше (и землю, и воду, и растения)
				for (int yy = y + 1; yy <= Math.max(surface, water) + 1; yy++) {
					pos.set(x, yy, z);
					if (!level.getBlockState(pos).isAir()) level.setBlock(pos, air, 2);
				}
				// засыпать, если было ниже
				for (int yy = surface + 1; yy < y; yy++) level.setBlock(pos.set(x, yy, z), ash, 2);
				level.setBlock(pos.set(x, y, z), top, 2);
				for (int k = 1; k <= SUBSOIL_LAYERS; k++) {
					pos.set(x, y - k, z);
					BlockState under = level.getBlockState(pos);
					if (under.isAir() || !under.getFluidState().isEmpty() || under.is(ModBlocks.ASPHODELITE_ROCK)) {
						level.setBlock(pos, type == 0 ? tar : ash, 2);
					}
				}
				if (underWater) {
					for (int yy = y + 1; yy <= water; yy++) level.setBlock(pos.set(x, yy, z), waterBlock, 2);
				}
				if (surface > y) WorldgenCleanup.removeFloatingPlants(level, x, Math.max(surface, water) + 2, z);
				changed = true;
			}
		}
		return changed;
	}
}
