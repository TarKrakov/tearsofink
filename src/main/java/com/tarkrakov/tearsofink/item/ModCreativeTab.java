package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Своя вкладка «Tears of Ink» в творческом инвентаре.
 * Правило автора: всё из мода — только здесь.
 */
public final class ModCreativeTab {
	public static final CreativeModeTab TAB = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			TearsOfInk.id("main"),
			FabricItemGroup.builder()
					.title(Component.translatable("itemGroup.tears-of-ink.main"))
					.icon(() -> new ItemStack(ModBlocks.ASHEN_SOIL))
					.displayItems((params, output) -> {
						output.accept(ModItems.SEALED_LETTER);
						output.accept(ModItems.INVITATION_LETTER);
						output.accept(ModItems.DIM_MIRROR);
						output.accept(ModItems.WORN_SATCHEL);
						output.accept(ModItems.PILGRIM_BUNDLE);
						output.accept(ModItems.DIARY);
						output.accept(ModItems.INK_LONGKNIFE);
						output.accept(ModItems.SALVATION_PENDANT);
						output.accept(ModItems.DIM_HAND_LANTERN);
						output.accept(ModItems.GUIDE_SPAWN_EGG);
						output.accept(ModBlocks.ASHEN_SOIL);
						output.accept(ModBlocks.ASH);
						output.accept(ModBlocks.ASHEN_PATH);
						output.accept(ModBlocks.INK_PUDDLE);
						output.accept(ModBlocks.BLOOD_SOIL);
						output.accept(ModBlocks.TAR_SOIL);
						output.accept(ModBlocks.PALE_GRASS);
						output.accept(ModBlocks.TALL_PALE_GRASS);
						output.accept(ModBlocks.SCARLET_GRASS);
						output.accept(ModBlocks.TALL_SCARLET_GRASS);
						output.accept(ModBlocks.INK_GRASS);
						output.accept(ModBlocks.TALL_INK_GRASS);
						output.accept(ModBlocks.SHORE_REED);
						output.accept(ModBlocks.TALL_SHORE_REED);
						output.accept(ModBlocks.TEARBLOOM);
						output.accept(ModBlocks.BLOODROOT);
						output.accept(ModItems.BLOODROOT_CLOT);
						output.accept(ModBlocks.FLESHBERRY);
						output.accept(ModItems.FLESHBERRY_PULP);
						output.accept(ModItems.ROASTED_FLESHBERRY);
						output.accept(ModBlocks.INKROOT);
						output.accept(ModItems.INKROOT_ROOT);
						output.accept(ModBlocks.TARWEED);
						output.accept(ModItems.TARWEED_EXTRACT);
						output.accept(ModBlocks.WATERBLOOM);
						output.accept(ModItems.WATERBLOOM_JUICE);
						output.accept(ModItems.BOGLEAF);
						output.accept(ModBlocks.DUCKWEED);
						output.accept(ModItems.DUCKWEED_PODS);
						output.accept(ModBlocks.ASPHODELITE_ROCK);
						output.accept(ModBlocks.RUIN_BRICKS);
						output.accept(ModBlocks.CRACKED_RUIN_BRICKS);
						output.accept(ModBlocks.OVERGROWN_RUIN_BRICKS);
						output.accept(ModBlocks.RUIN_TILES);
						output.accept(ModBlocks.CRACKED_RUIN_TILES);
						output.accept(ModBlocks.RUIN_PATTERN);
						output.accept(ModBlocks.CRACKED_RUIN_PATTERN);
						output.accept(ModBlocks.RUIN_PILLAR);
						output.accept(ModBlocks.CRACKED_RUIN_PILLAR);
						output.accept(ModBlocks.RUIN_BRICK_STAIRS);
						output.accept(ModBlocks.RUIN_BRICK_SLAB);
						output.accept(ModBlocks.RUIN_BRICK_WALL);
						output.accept(ModBlocks.CRACKED_RUIN_BRICK_STAIRS);
						output.accept(ModBlocks.CRACKED_RUIN_BRICK_SLAB);
						output.accept(ModBlocks.CRACKED_RUIN_BRICK_WALL);
						output.accept(ModBlocks.OVERGROWN_RUIN_BRICK_STAIRS);
						output.accept(ModBlocks.OVERGROWN_RUIN_BRICK_SLAB);
						output.accept(ModBlocks.OVERGROWN_RUIN_BRICK_WALL);
						output.accept(ModBlocks.RUIN_TILE_SLAB);
						output.accept(ModBlocks.CRACKED_RUIN_TILE_SLAB);
						output.accept(ModBlocks.ASPHODELITE_ORE);
						output.accept(ModBlocks.DEEPSLATE_ASPHODELITE_ORE);
						output.accept(ModItems.ASPHODA);
						output.accept(ModBlocks.ASPHODELITE_COAL_ORE);
						output.accept(ModBlocks.ASPHODELITE_IRON_ORE);
						output.accept(ModBlocks.ASPHODELITE_COPPER_ORE);
						output.accept(ModBlocks.ASPHODELITE_GOLD_ORE);
						output.accept(ModBlocks.ASPHODELITE_REDSTONE_ORE);
						output.accept(ModBlocks.ASPHODELITE_LAPIS_ORE);
						output.accept(ModBlocks.ASPHODELITE_DIAMOND_ORE);
						output.accept(ModBlocks.ASPHODELITE_EMERALD_ORE);
						output.accept(ModItems.PIGMENT);
						output.accept(ModItems.FULFILLED_CONTRACT);
						output.accept(ModBlocks.PALE_LOG);
						output.accept(ModBlocks.STRIPPED_PALE_LOG);
						output.accept(ModBlocks.PALE_WOOD);
						output.accept(ModBlocks.STRIPPED_PALE_WOOD);
						output.accept(ModBlocks.PALE_PLANKS);
						output.accept(ModBlocks.PALE_STAIRS);
						output.accept(ModBlocks.PALE_SLAB);
						output.accept(ModBlocks.PALE_FENCE);
						output.accept(ModBlocks.PALE_FENCE_GATE);
						output.accept(ModBlocks.PALE_DOOR);
						output.accept(ModBlocks.PALE_TRAPDOOR);
						output.accept(ModBlocks.PALE_PRESSURE_PLATE);
						output.accept(ModBlocks.PALE_BUTTON);
						ModBlocks.SCARLET.all().forEach(output::accept);
						output.accept(ModBlocks.GOLDEN_SCARLET_LEAVES);
						ModBlocks.INK.all().forEach(output::accept);
						output.accept(ModBlocks.PALE_LEAVES);
						output.accept(ModBlocks.PALE_SAPLING);
						output.accept(ModBlocks.INK_LANTERN);
						output.accept(ModBlocks.STONE_LANTERN);
						output.accept(ModBlocks.ETERNAL_LANTERN);
						output.accept(ModBlocks.INK_FOUNTAIN);
						output.accept(ModItems.HAND_LANTERN);
						output.accept(ModItems.BRIGHT_HAND_LANTERN);
						output.accept(ModItems.RAM_SHIELD);
						output.accept(ModItems.RADIANT_SHIELD);
						output.accept(ModItems.MERCY_SHIELD);
						output.accept(ModItems.WORN_RAM_SHIELD);
						output.accept(ModItems.WORN_MERCY_SHIELD);
						output.accept(ModItems.DIM_SCIMITAR);
						output.accept(ModItems.REPEL_SHIELD);
						output.accept(ModItems.WAVE_GREATSWORD);
						output.accept(ModItems.CLEAVE_GREATSWORD);
						output.accept(ModItems.ROARING_AXE);
						output.accept(ModItems.RAPID_KNIFE);
						output.accept(ModItems.BLOOD_KATANA);
						output.accept(ModItems.BLOOD_OBSI_CLUB);
						output.accept(ModItems.BLOOD_TALVAR);
						output.accept(ModItems.BLOOD_DUSSAK);
						for (var line : com.tarkrakov.tearsofink.hunt.HunterType.values()) {
							for (int tier = 0; tier < com.tarkrakov.tearsofink.hunt.HunterWeaponItem.TIERS; tier++) {
								output.accept(((com.tarkrakov.tearsofink.hunt.HunterWeaponItem) ModItems.HUNTER_GREATSWORD).make(line, tier));
								output.accept(((com.tarkrakov.tearsofink.hunt.HunterWeaponItem) ModItems.HUNTER_SABER).make(line, tier));
							}
						}
						output.accept(ModItems.ATONEMENT);
						output.accept(ModItems.CATERPILLAR_SPAWN_EGG);
						output.accept(ModItems.PALE_SPEARMAN_SPAWN_EGG);
						output.accept(ModItems.PALE_SHIELDBEARER_SPAWN_EGG);
						output.accept(ModItems.PALE_CROSSBOWMAN_SPAWN_EGG);
						output.accept(ModItems.SILK_THREAD);
						output.accept(ModItems.BLOOD_CLOT);
						output.accept(ModItems.BLOOD_CRYSTAL);
						output.accept(ModBlocks.BLOOD_PUMP);
						output.accept(ModBlocks.BLOOD_TANK);
						output.accept(ModItems.INK);
						output.accept(ModItems.RAW_CATERPILLAR_MEAT);
						output.accept(ModItems.COOKED_CATERPILLAR_MEAT);
						output.accept(ModBlocks.ALCHEMY_TABLE);
						output.accept(ModItems.EMPTY_FLASK);
						output.accept(ModItems.TEARBLOOM_BREW);
						output.accept(ModItems.STRONG_TEARBLOOM_BREW);
						output.accept(ModItems.SKILL_BOOK_DASH);
						output.accept(ModItems.SKILL_BOOK_INK_JUMP);
						output.accept(ModItems.SKILL_BOOK_STRIDE);
						output.accept(ModItems.SKILL_BOOK_ASCENSION);
					})
					.build());

	private ModCreativeTab() {}

	public static void init() {
	}

}
