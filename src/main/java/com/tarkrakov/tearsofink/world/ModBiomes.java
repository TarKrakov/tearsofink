package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/** Биомы Чернильных Земель. Сами биомы описаны в data/tears-of-ink/worldgen/biome/*.json */
public final class ModBiomes {
	/** Бледнолесье — внешнее кольцо, самая большая зона. */
	public static final ResourceKey<Biome> PALE_FOREST = key("pale_forest");
	/** Кроволесье — рваные пятна в средней части. */
	public static final ResourceKey<Biome> BLOODWOOD = key("bloodwood");
	/** Чернолесье — ядро, где стоит Шпиль. */
	public static final ResourceKey<Biome> INKWOOD = key("inkwood");
	/** Смоляное озеро — вся вода внутри Чернильных Земель. */
	public static final ResourceKey<Biome> TAR_LAKE = key("tar_lake");

	/** Край Земель — голая пепельная земля в стене тумана (ни деревьев, ни дорог; дальше не пройти). */
	public static final ResourceKey<Biome> EDGE = key("ink_edge");

	public static final List<ResourceKey<Biome>> ALL = List.of(PALE_FOREST, BLOODWOOD, INKWOOD, TAR_LAKE);

	/** Тег «это Чернильные Земли» (все четыре биома). */
	public static final TagKey<Biome> INK_LANDS = TagKey.create(Registries.BIOME, TearsOfInk.id("ink_lands"));

	private static ResourceKey<Biome> key(String name) {
		return ResourceKey.create(Registries.BIOME, TearsOfInk.id(name));
	}

	private ModBiomes() {}
}
