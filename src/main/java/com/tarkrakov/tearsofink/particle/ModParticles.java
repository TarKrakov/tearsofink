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

	private ModParticles() {}

	public static void init() {}
}
