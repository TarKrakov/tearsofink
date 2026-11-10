package com.tarkrakov.tearsofink.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.List;
import java.util.function.Consumer;

/**
 * Регион TerraBlender: кусок мира, где вместо равнин появляются Чернильные пустоши.
 * Чем больше вес (weight), тем чаще такие регионы встречаются.
 */
public class InkWastesRegion extends Region {
	/** Какие ванильные биомы в нашем регионе превращаются в Чернильные пустоши. */
	private static final List<ResourceKey<Biome>> REPLACED = List.of(
			Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW,
			Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
			Biomes.DARK_FOREST, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);

	public InkWastesRegion(ResourceLocation name, int weight) {
		super(name, RegionType.OVERWORLD, weight);
	}

	@Override
	public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
		// В нашем регионе пустоши занимают место всех «умеренных» биомов —
		// так получается крупный сплошной массив, а горы, реки, болота и океаны остаются.
		this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
			for (ResourceKey<Biome> biome : REPLACED) {
				builder.replaceBiome(biome, ModBiomes.INK_WASTES);
			}
		});
	}
}
