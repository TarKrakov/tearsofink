package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Источник биомов измерения Чернильных Земель: где какая зона — решает {@link InkLandsLayout}.
 * За краем области — то же Бледнолесье (туда всё равно не дойти: туман-петля возвращает назад).
 */
public class InkLandsBiomeSource extends BiomeSource implements InkLandsAccess {
	public static final MapCodec<InkLandsBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegistryOps.retrieveElement(ModBiomes.PALE_FOREST),
			RegistryOps.retrieveElement(ModBiomes.BLOODWOOD),
			RegistryOps.retrieveElement(ModBiomes.INKWOOD),
			RegistryOps.retrieveElement(ModBiomes.TAR_LAKE)
	).apply(i, InkLandsBiomeSource::new));

	private final Holder<Biome> pale;
	private final Map<ResourceKey<Biome>, Holder<Biome>> biomes;
	private volatile InkLandsLayout layout;

	public InkLandsBiomeSource(Holder<Biome> pale, Holder<Biome> blood, Holder<Biome> ink, Holder<Biome> lake) {
		this.pale = pale;
		this.biomes = Map.of(ModBiomes.PALE_FOREST, pale, ModBiomes.BLOODWOOD, blood, ModBiomes.INKWOOD, ink, ModBiomes.TAR_LAKE, lake);
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return biomes.values().stream();
	}

	@Override
	public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
		ResourceKey<Biome> key = tearsofink$layout(sampler).pick(x << 2, z << 2, null);
		return key == null ? pale : biomes.get(key);
	}

	@Override
	public InkLandsLayout tearsofink$layout(Climate.Sampler sampler) {
		InkLandsLayout l = layout;
		if (l == null || l.sampler() != sampler) {
			synchronized (this) {
				l = layout;
				if (l == null || l.sampler() != sampler) {
					l = InkLandsLayout.create(sampler);
					layout = l;
				}
			}
		}
		return l;
	}
}
