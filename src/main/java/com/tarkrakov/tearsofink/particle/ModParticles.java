package com.tarkrakov.tearsofink.particle;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Все частицы мода. */
public final class ModParticles {
	/** Большие клубы тумана, стоящие над Чернильными пустошами. */
	public static final SimpleParticleType FOG_BANK = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE, TearsOfInk.id("fog_bank"), FabricParticleTypes.simple(true));

	/** Холодные искорки вокруг Слёзницы. */
	public static final SimpleParticleType TEAR_GLINT = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE, TearsOfInk.id("tear_glint"), FabricParticleTypes.simple());

	/** Капли крови (Багряное дерево) и чернил (Смоляное дерево): висит → падает → брызги. */
	public static final SimpleParticleType BLOOD_DRIP_HANG = simple("blood_drip_hang");
	public static final SimpleParticleType BLOOD_DRIP_FALL = simple("blood_drip_fall");
	public static final SimpleParticleType BLOOD_DRIP_LAND = simple("blood_drip_land");
	public static final SimpleParticleType INK_DRIP_HANG = simple("ink_drip_hang");
	public static final SimpleParticleType INK_DRIP_FALL = simple("ink_drip_fall");
	public static final SimpleParticleType INK_DRIP_LAND = simple("ink_drip_land");

	/** Огонёк, танцующий на месте в камере фонаря (лист огня автора, 8 кадров). */
	public static final SimpleParticleType FLAME_DANCE = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE, TearsOfInk.id("flame_dance"), FabricParticleTypes.simple(true));

	/** Частицы из листов автора: дым (рывок, гашение фонаря), искры (зажигание), клочки пергамента (книга навыка). */
	public static final SimpleParticleType INK_SMOKE = simple("ink_smoke");
	public static final SimpleParticleType SPARK_BURST = simple("spark_burst");
	public static final SimpleParticleType PAPER_SCRAP = simple("paper_scrap");

	private static SimpleParticleType simple(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, TearsOfInk.id(name), FabricParticleTypes.simple());
	}

	private ModParticles() {}

	public static void init() {}
}
