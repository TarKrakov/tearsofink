package com.tarkrakov.tearsofink.alchemy;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/** Алхимия мода: тип рецептов, их чтение из JSON и окно стола. */
public final class ModAlchemy {
	public static final RecipeType<AlchemyRecipe> TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE,
			TearsOfInk.id("alchemy"), new RecipeType<>() {
				@Override
				public String toString() {
					return "tears-of-ink:alchemy";
				}
			});

	public static final RecipeSerializer<AlchemyRecipe> SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			TearsOfInk.id("alchemy"), new RecipeSerializer<>() {
				@Override
				public MapCodec<AlchemyRecipe> codec() {
					return AlchemyRecipe.CODEC;
				}

				@Override
				public StreamCodec<RegistryFriendlyByteBuf, AlchemyRecipe> streamCodec() {
					return AlchemyRecipe.STREAM_CODEC;
				}
			});

	public static final MenuType<AlchemyMenu> MENU = Registry.register(BuiltInRegistries.MENU,
			TearsOfInk.id("alchemy_table"), new MenuType<>(AlchemyMenu::new, FeatureFlags.VANILLA_SET));

	private ModAlchemy() {}

	public static void init() {}
}
