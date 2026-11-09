package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** Ключи биомов мода. Сами биомы описаны в data/tears-of-ink/worldgen/biome/*.json */
public final class ModBiomes {
	/** Чернильные пустоши. */
	public static final ResourceKey<Biome> INK_WASTES =
			ResourceKey.create(Registries.BIOME, TearsOfInk.id("ink_wastes"));

	private ModBiomes() {}
}
