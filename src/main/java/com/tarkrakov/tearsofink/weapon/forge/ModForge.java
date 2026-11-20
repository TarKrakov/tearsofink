package com.tarkrakov.tearsofink.weapon.forge;

import com.mojang.serialization.MapCodec;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/** Ковка: тип рецептов Чернильной наковальни и её окно. */
public final class ModForge {
	public static final RecipeType<ForgingRecipe> TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE,
			TearsOfInk.id("forging"), new RecipeType<>() {
				@Override
				public String toString() {
					return "tears-of-ink:forging";
				}
			});

	public static final RecipeSerializer<ForgingRecipe> SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			TearsOfInk.id("forging"), new RecipeSerializer<>() {
				@Override
				public MapCodec<ForgingRecipe> codec() {
					return ForgingRecipe.CODEC;
				}

				@Override
				public StreamCodec<RegistryFriendlyByteBuf, ForgingRecipe> streamCodec() {
					return ForgingRecipe.STREAM_CODEC;
				}
			});

	public static final RecipeSerializer<ShieldRestorationRecipe> SHIELD_RESTORATION = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			TearsOfInk.id("shield_restoration"), new RecipeSerializer<>() {
				@Override public MapCodec<ShieldRestorationRecipe> codec() { return ShieldRestorationRecipe.CODEC; }
				@Override public StreamCodec<RegistryFriendlyByteBuf, ShieldRestorationRecipe> streamCodec() { return ShieldRestorationRecipe.STREAM_CODEC; }
			});

	public static final MenuType<InkAnvilMenu> MENU = Registry.register(BuiltInRegistries.MENU,
			TearsOfInk.id("ink_anvil"), new MenuType<>(InkAnvilMenu::new, FeatureFlags.VANILLA_SET));

	public static final MenuType<CursedAltarMenu> ALTAR_MENU = Registry.register(BuiltInRegistries.MENU,
			TearsOfInk.id("cursed_altar"), new MenuType<>(CursedAltarMenu::new, FeatureFlags.VANILLA_SET));

	private ModForge() {}

	public static void init() {
		Gear.init();
		AscensionFont.init();
	}
}
