package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Своя вкладка «Tears of Ink» в творческом инвентаре. */
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
						output.accept(ModBlocks.INK_LOG);
						output.accept(ModBlocks.STRIPPED_INK_LOG);
						output.accept(ModBlocks.INK_WOOD);
						output.accept(ModBlocks.STRIPPED_INK_WOOD);
						output.accept(ModBlocks.INK_PLANKS);
						output.accept(ModBlocks.INK_STAIRS);
						output.accept(ModBlocks.INK_SLAB);
						output.accept(ModBlocks.INK_FENCE);
						output.accept(ModBlocks.INK_FENCE_GATE);
						output.accept(ModBlocks.INK_DOOR);
						output.accept(ModBlocks.INK_TRAPDOOR);
						output.accept(ModBlocks.INK_PRESSURE_PLATE);
						output.accept(ModBlocks.INK_BUTTON);
						output.accept(ModBlocks.INK_LEAVES);
						output.accept(ModBlocks.INK_SAPLING);
					})
					.build());

	private ModCreativeTab() {}

	public static void init() {}
}
