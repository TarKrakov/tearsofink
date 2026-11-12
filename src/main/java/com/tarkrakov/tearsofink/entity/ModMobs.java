package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.ModBiomes;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/** Существа мода: регистрация, характеристики, где появляются. */
public final class ModMobs {
	public static final EntityType<CaterpillarEntity> CATERPILLAR = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			TearsOfInk.id("caterpillar"),
			EntityType.Builder.of(CaterpillarEntity::new, MobCategory.CREATURE)
					.sized(0.45f, 0.3f).clientTrackingRange(8).build("caterpillar"));

	private ModMobs() {}

	public static void init() {
		FabricDefaultAttributeRegistry.register(CATERPILLAR, CaterpillarEntity.createAttributes());
		SpawnPlacements.register(CATERPILLAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(type, level, reason, pos, random) -> {
					BlockPos below = pos.below();
					return level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP);
				});
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(ModBiomes.PALE_FOREST), MobCategory.CREATURE,
				CATERPILLAR, EntityTuning.PALE_WEIGHT, 2, 4);
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(ModBiomes.INKWOOD), MobCategory.CREATURE,
				CATERPILLAR, EntityTuning.INK_WEIGHT, 2, 4);
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(ModBiomes.BLOODWOOD), MobCategory.CREATURE,
				CATERPILLAR, EntityTuning.BLOOD_WEIGHT, 2, 3);
	}
}
