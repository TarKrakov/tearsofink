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

	/** Бледный копейщик — патруль дорог Бледнолесья. */
	public static final EntityType<com.tarkrakov.tearsofink.entity.spearman.PaleSpearmanEntity> PALE_SPEARMAN = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, TearsOfInk.id("pale_spearman"),
			EntityType.Builder.of(com.tarkrakov.tearsofink.entity.spearman.PaleSpearmanEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.7f).clientTrackingRange(10).build("pale_spearman"));

	/** Бледный щитовик — танк отряда. */
	public static final EntityType<com.tarkrakov.tearsofink.entity.patrol.PaleShieldbearerEntity> PALE_SHIELDBEARER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, TearsOfInk.id("pale_shieldbearer"),
			EntityType.Builder.of(com.tarkrakov.tearsofink.entity.patrol.PaleShieldbearerEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 2.1f).clientTrackingRange(10).build("pale_shieldbearer"));
	/** Бледный арбалетчик. */
	public static final EntityType<com.tarkrakov.tearsofink.entity.patrol.PaleCrossbowmanEntity> PALE_CROSSBOWMAN = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, TearsOfInk.id("pale_crossbowman"),
			EntityType.Builder.of(com.tarkrakov.tearsofink.entity.patrol.PaleCrossbowmanEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.7f).clientTrackingRange(10).build("pale_crossbowman"));
	/** Господин Проводник (Преддверие). */
	public static final EntityType<com.tarkrakov.tearsofink.prologue.GuideEntity> GUIDE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, TearsOfInk.id("guide"),
			EntityType.Builder.of(com.tarkrakov.tearsofink.prologue.GuideEntity::new, MobCategory.MISC)
					.sized(0.8f, 2.6f).clientTrackingRange(10).build("guide"));
	/** Торговка душами (проступает у фонтанов). */
	public static final EntityType<com.tarkrakov.tearsofink.trader.SoulTraderEntity> SOUL_TRADER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, TearsOfInk.id("soul_trader"),
			EntityType.Builder.of(com.tarkrakov.tearsofink.trader.SoulTraderEntity::new, MobCategory.MISC)
					.sized(0.7f, 2.3f).clientTrackingRange(10).build("soul_trader"));
	/** Арбалетный болт. */
	public static final EntityType<com.tarkrakov.tearsofink.entity.patrol.PaleBoltEntity> PALE_BOLT = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, TearsOfInk.id("pale_bolt"),
			EntityType.Builder.<com.tarkrakov.tearsofink.entity.patrol.PaleBoltEntity>of(com.tarkrakov.tearsofink.entity.patrol.PaleBoltEntity::new, MobCategory.MISC)
					.sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(20).build("pale_bolt"));

	private ModMobs() {}

	public static void init() {
		FabricDefaultAttributeRegistry.register(CATERPILLAR, CaterpillarEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(PALE_SPEARMAN, com.tarkrakov.tearsofink.entity.spearman.PaleSpearmanEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(PALE_SHIELDBEARER, com.tarkrakov.tearsofink.entity.patrol.PaleShieldbearerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(PALE_CROSSBOWMAN, com.tarkrakov.tearsofink.entity.patrol.PaleCrossbowmanEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GUIDE, com.tarkrakov.tearsofink.prologue.GuideEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SOUL_TRADER, com.tarkrakov.tearsofink.trader.SoulTraderEntity.createAttributes());
		com.tarkrakov.tearsofink.entity.patrol.SquadPatrols.init();
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
