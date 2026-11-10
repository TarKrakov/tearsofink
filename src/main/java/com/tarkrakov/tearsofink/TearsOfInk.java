package com.tarkrakov.tearsofink;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.command.ModCommands;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.item.ModCreativeTab;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.world.ModTreeTypes;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TearsOfInk implements ModInitializer {
	public static final String MOD_ID = "tears-of-ink";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModTreeTypes.init();
		ModBlocks.init();
		ModItems.init();
		ModCreativeTab.init();
		ModParticles.init();
		InkPowers.init();
		ModCommands.init();
		LOGGER.info("Tears of Ink: чернила проливаются в мир.");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
