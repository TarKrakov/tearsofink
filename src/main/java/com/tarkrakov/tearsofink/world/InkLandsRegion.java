package com.tarkrakov.tearsofink.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * Регистрирует наши биомы в мире через TerraBlender, но в «недостижимой» точке климата:
 * так они попадают в список биомов мира (нужно для генерации деревьев, руд и т. п.),
 * но сами по климату нигде не появляются. Где именно они стоят — решает {@link InkLandsLayout}.
 */
public class InkLandsRegion extends Region {
	private static final float UNREACHABLE = 10f;

	public InkLandsRegion(ResourceLocation name, int weight) {
		super(name, RegionType.OVERWORLD, weight);
	}

	@Override
	public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
		// Внутри этого региона — обычный ванильный мир без изменений.
		this.addModifiedVanillaOverworldBiomes(mapper, builder -> {});
		Climate.ParameterPoint nowhere = Climate.parameters(UNREACHABLE, UNREACHABLE, UNREACHABLE,
				UNREACHABLE, UNREACHABLE, UNREACHABLE, 1f);
		for (ResourceKey<Biome> biome : ModBiomes.ALL) {
			mapper.accept(Pair.of(nowhere, biome));
		}
	}
}
