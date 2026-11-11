package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import com.tarkrakov.tearsofink.effect.ModEffects;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * Своя вкладка «Tears of Ink» в творческом инвентаре.
 * Правило автора: всё из мода — только здесь (зелья мода убираем из ванильных вкладок).
 */
public final class ModCreativeTab {
	public static final CreativeModeTab TAB = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			TearsOfInk.id("main"),
			FabricItemGroup.builder()
					.title(Component.translatable("itemGroup.tears-of-ink.main"))
					.icon(() -> new ItemStack(ModBlocks.ASHEN_SOIL))
					.displayItems((params, output) -> {
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
						output.accept(ModItems.REPEL_SHIELD);
						output.accept(ModItems.WAVE_GREATSWORD);
						output.accept(ModItems.CLEAVE_GREATSWORD);
						output.accept(ModItems.ATONEMENT);
						for (Holder<Potion> potion : ModEffects.POTIONS) {
							for (Item kind : new Item[]{Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW}) {
								output.accept(PotionContents.createItemStack(kind, potion));
							}
						}
						output.accept(ModItems.SKILL_BOOK_DASH);
					})
					.build());

	private ModCreativeTab() {}

	public static void init() {
		// зелья мода игра сама кладёт в «Еда и напитки» и «Бой» — убираем оттуда
		for (var tab : java.util.List.of(CreativeModeTabs.FOOD_AND_DRINKS, CreativeModeTabs.COMBAT)) {
			ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> {
				entries.getDisplayStacks().removeIf(ModCreativeTab::isModPotion);
				entries.getSearchTabStacks().removeIf(ModCreativeTab::isModPotion);
			});
		}
	}

	private static boolean isModPotion(ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		return contents != null && contents.potion().isPresent() && ModEffects.POTIONS.contains(contents.potion().get());
	}
}
