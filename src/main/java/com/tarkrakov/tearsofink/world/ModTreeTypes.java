package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

/** Свои «рисовальщики» стволов и особые фичи генерации (дороги). */
public final class ModTreeTypes {
	public static final TrunkPlacerType<PaleTrunkPlacer> PALE_TRUNK_PLACER = Registry.register(
			BuiltInRegistries.TRUNK_PLACER_TYPE, TearsOfInk.id("pale_trunk_placer"),
			new TrunkPlacerType<>(PaleTrunkPlacer.CODEC));

	/** Магистральные дороги Чернильных Земель. */
	public static final Feature<NoneFeatureConfiguration> INK_ROADS = Registry.register(
			BuiltInRegistries.FEATURE, TearsOfInk.id("ink_roads"), new InkRoadsFeature(NoneFeatureConfiguration.CODEC));

	private ModTreeTypes() {}

	public static void init() {}
}
