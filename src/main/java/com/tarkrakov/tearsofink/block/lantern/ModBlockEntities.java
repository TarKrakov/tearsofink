package com.tarkrakov.tearsofink.block.lantern;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<LanternBeaconBlockEntity> LANTERN_BEACON = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, TearsOfInk.id("lantern_beacon"),
			BlockEntityType.Builder.of(LanternBeaconBlockEntity::new,
					ModBlocks.INK_LANTERN, ModBlocks.STONE_LANTERN, ModBlocks.ETERNAL_LANTERN).build(null));

	private ModBlockEntities() {}

	public static void init() {
		Lanterns.registerSneakDouse();
	}
}
