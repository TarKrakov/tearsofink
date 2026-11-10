package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tarkrakov.tearsofink.world.InkLandsAccess;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

/**
 * Рисуем Чернильные Земли поверх обычного мира: оборачиваем выбор биома целиком
 * (включая изменения TerraBlender/BOP) и внутри нашей области подменяем результат.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin implements InkLandsAccess {
	/** null — ещё не проверяли; пустая карта — это не обычный мир (наших биомов в нём нет). */
	@Unique
	private volatile Map<ResourceKey<Biome>, Holder<Biome>> tearsofink$biomes;
	@Unique
	private volatile InkLandsLayout tearsofink$layout;

	@WrapMethod(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;")
	private Holder<Biome> tearsofink$inkLands(int x, int y, int z, Climate.Sampler sampler,
											  Operation<Holder<Biome>> original) {
		Holder<Biome> biome = original.call(x, y, z, sampler);
		InkLandsLayout layout = tearsofink$layout(sampler);
		if (layout == null) return biome;
		// x, y, z здесь в «четвертях» (1 = 4 блока)
		ResourceKey<Biome> ours = layout.pick(x << 2, z << 2, biome);
		return ours == null ? biome : tearsofink$biomes.get(ours);
	}

	@Override
	public InkLandsLayout tearsofink$layout(Climate.Sampler sampler) {
		Map<ResourceKey<Biome>, Holder<Biome>> biomes = tearsofink$biomes;
		if (biomes == null) {
			biomes = new HashMap<>();
			for (Holder<Biome> holder : ((BiomeSource) (Object) this).possibleBiomes()) {
				for (ResourceKey<Biome> key : ModBiomes.ALL) {
					if (holder.is(key)) biomes.put(key, holder);
				}
			}
			if (biomes.size() != ModBiomes.ALL.size()) biomes = Map.of();
			tearsofink$biomes = biomes;
		}
		if (biomes.isEmpty()) return null;

		InkLandsLayout layout = tearsofink$layout;
		if (layout == null || layout.sampler() != sampler) {
			synchronized (this) {
				layout = tearsofink$layout;
				if (layout == null || layout.sampler() != sampler) {
					layout = InkLandsLayout.create(sampler);
					tearsofink$layout = layout;
				}
			}
		}
		return layout;
	}
}
