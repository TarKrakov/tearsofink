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

	public static final EntityType<com.tarkrakov.tearsofink.weapon.blood.BloodArcEntity> BLOOD_ARC = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			TearsOfInk.id("blood_arc"),
			EntityType.Builder.<com.tarkrakov.tearsofink.weapon.blood.BloodArcEntity>of(com.tarkrakov.tearsofink.weapon.blood.BloodArcEntity::new, MobCategory.MISC)
					.sized(1.6f, 0.9f).clientTrackingRange(6).updateInterval(1).noSave().build("blood_arc"));

	public static final EntityType<com.tarkrakov.tearsofink.weapon.shield.ShieldPhantomEntity> SHIELD_PHANTOM = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			TearsOfInk.id("shield_phantom"), EntityType.Builder.<com.tarkrakov.tearsofink.weapon.shield.ShieldPhantomEntity>of(com.tarkrakov.tearsofink.weapon.shield.ShieldPhantomEntity::new, MobCategory.MISC)
				.sized(1.2f,1.2f).clientTrackingRange(6).updateInterval(1).noSave().build("shield_phantom"));
	private ModEntities() {}

	public static void init() {}
}
