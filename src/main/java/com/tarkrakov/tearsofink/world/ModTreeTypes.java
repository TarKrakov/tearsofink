package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

/** Свои «рисовальщики» стволов и листвы для деревьев. */
public final class ModTreeTypes {
	public static final TrunkPlacerType<InkTrunkPlacer> INK_TRUNK_PLACER = Registry.register(
			BuiltInRegistries.TRUNK_PLACER_TYPE, TearsOfInk.id("ink_trunk_placer"),
			new TrunkPlacerType<>(InkTrunkPlacer.CODEC));

	private ModTreeTypes() {}

	public static void init() {}
}
