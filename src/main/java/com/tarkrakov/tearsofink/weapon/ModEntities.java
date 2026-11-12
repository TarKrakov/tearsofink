package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<InkWaveEntity> INK_WAVE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			TearsOfInk.id("ink_wave"),
			EntityType.Builder.<InkWaveEntity>of(InkWaveEntity::new, MobCategory.MISC)
					.sized(1.6f, 0.9f).clientTrackingRange(6).updateInterval(1).noSave().build("ink_wave"));

	public static final EntityType<InkBlotEntity> INK_BLOT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			TearsOfInk.id("ink_blot"),
			EntityType.Builder.<InkBlotEntity>of(InkBlotEntity::new, MobCategory.MISC)
					.sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(5).build("ink_blot"));

	private ModEntities() {}

	public static void init() {}
}
