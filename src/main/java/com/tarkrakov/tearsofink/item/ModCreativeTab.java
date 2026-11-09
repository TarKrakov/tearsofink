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
					})
					.build());

	private ModCreativeTab() {}

	public static void init() {}
}
