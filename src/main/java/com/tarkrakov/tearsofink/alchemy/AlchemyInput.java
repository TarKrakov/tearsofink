package com.tarkrakov.tearsofink.alchemy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** Что лежит на алхимическом столе: до 3 ингредиентов + флакон. */
public record AlchemyInput(List<ItemStack> ingredients, ItemStack flask) implements RecipeInput {
	@Override
	public ItemStack getItem(int index) {
		return index < ingredients.size() ? ingredients.get(index) : flask;
	}

	@Override
	public int size() {
		return ingredients.size() + 1;
	}
}
