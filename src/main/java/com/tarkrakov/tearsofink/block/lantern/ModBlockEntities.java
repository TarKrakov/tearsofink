package com.tarkrakov.tearsofink.block.lantern;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<com.tarkrakov.tearsofink.block.BattlefieldSceneBlockEntity> BATTLEFIELD_SCENE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, TearsOfInk.id("battlefield_scene"),
			BlockEntityType.Builder.of(com.tarkrakov.tearsofink.block.BattlefieldSceneBlockEntity::new, ModBlocks.BATTLEFIELD_SCENE).build(null));

	public static final BlockEntityType<com.tarkrakov.tearsofink.block.BattlefieldBannerBlockEntity> BATTLEFIELD_BANNER = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, TearsOfInk.id("battlefield_banner"),
			BlockEntityType.Builder.of(com.tarkrakov.tearsofink.block.BattlefieldBannerBlockEntity::new, ModBlocks.BATTLEFIELD_BANNER).build(null));

	public static final BlockEntityType<LanternBeaconBlockEntity> LANTERN_BEACON = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, TearsOfInk.id("lantern_beacon"),
			BlockEntityType.Builder.of(LanternBeaconBlockEntity::new,
					ModBlocks.INK_LANTERN, ModBlocks.STONE_LANTERN, ModBlocks.ETERNAL_LANTERN).build(null));

	public static final BlockEntityType<com.tarkrakov.tearsofink.block.blood.BloodPumpBlockEntity> BLOOD_PUMP = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, TearsOfInk.id("blood_pump"),
			BlockEntityType.Builder.of(com.tarkrakov.tearsofink.block.blood.BloodPumpBlockEntity::new, ModBlocks.BLOOD_PUMP).build(null));
	public static final BlockEntityType<com.tarkrakov.tearsofink.block.blood.BloodTankBlockEntity> BLOOD_TANK = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, TearsOfInk.id("blood_tank"),
			BlockEntityType.Builder.of(com.tarkrakov.tearsofink.block.blood.BloodTankBlockEntity::new, ModBlocks.BLOOD_TANK).build(null));

	public static final BlockEntityType<com.tarkrakov.tearsofink.world.herald.HeraldHeartBlockEntity> HERALD_HEART = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, TearsOfInk.id("herald_heart"),
			BlockEntityType.Builder.of(com.tarkrakov.tearsofink.world.herald.HeraldHeartBlockEntity::new, ModBlocks.HERALD_HEART).build(null));

	private ModBlockEntities() {}

	public static void init() {
		Lanterns.registerSneakDouse();
	}
}
