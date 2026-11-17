package com.tarkrakov.tearsofink;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.lantern.ModBlockEntities;
import com.tarkrakov.tearsofink.command.ModCommands;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.effect.ModEffects;
import com.tarkrakov.tearsofink.alchemy.ModAlchemy;
import com.tarkrakov.tearsofink.skill.Skills;
import com.tarkrakov.tearsofink.weapon.ModEntities;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.weapon.WeaponAbilities;
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
		com.tarkrakov.tearsofink.world.ModDimensions.init();
		com.tarkrakov.tearsofink.travel.Travel.init();
		com.tarkrakov.tearsofink.prologue.Prologue.init();
		com.tarkrakov.tearsofink.world.AwayFromRoadFilter.init();
		com.tarkrakov.tearsofink.world.herald.BloodHerald.init();
		com.tarkrakov.tearsofink.trader.SoulTrader.init();
		com.tarkrakov.tearsofink.weapon.combat.Combat.init();
		com.tarkrakov.tearsofink.weapon.combat.HiltStrike.init();
		com.tarkrakov.tearsofink.weapon.combat.AxeMoves.init();
		com.tarkrakov.tearsofink.weapon.combat.DaggerMoves.init();
		com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.init();
		com.tarkrakov.tearsofink.weapon.forge.ModForge.init();
		com.tarkrakov.tearsofink.hunt.MadOrders.init();
		com.tarkrakov.tearsofink.hunt.Harvest.init();
		com.tarkrakov.tearsofink.sound.ModSounds.init();
		com.tarkrakov.tearsofink.vow.SoulFeats.init();
		com.tarkrakov.tearsofink.vow.InkChallenges.init();
		com.tarkrakov.tearsofink.weapon.shield.ShieldStance.init();
		com.tarkrakov.tearsofink.weapon.shield.Resonance.init();
		com.tarkrakov.tearsofink.vow.Vows.init();
		com.tarkrakov.tearsofink.vow.Aura.init();
		com.tarkrakov.tearsofink.entity.faction.VanillaAggro.init();
		com.tarkrakov.tearsofink.world.ruins.Ruins.init();
		com.tarkrakov.tearsofink.world.sanctuary.Sanctuary.init();
		com.tarkrakov.tearsofink.hunt.Hunt.init();
		com.tarkrakov.tearsofink.weapon.blood.Parry.init();
		com.tarkrakov.tearsofink.world.AwayFromHeraldFilter.init();
		ModBlocks.init();
		ModBlockEntities.init();
		ModItems.init();
		ModCreativeTab.init();
		ModParticles.init();
		ModEffects.init();
		ModAlchemy.init();
		InkPowers.init();
		Skills.init();
		ModEntities.init();
		ModMobs.init();
		WeaponAbilities.init();
		ModCommands.init();
		com.tarkrakov.tearsofink.command.DebugCommands.init();
		com.tarkrakov.tearsofink.command.StructureCommands.init();
		LOGGER.info("Tears of Ink: чернила проливаются в мир.");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
