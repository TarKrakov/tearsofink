package com.tarkrakov.tearsofink.alchemy;

import com.tarkrakov.tearsofink.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Рецепт алхимического стола: 1–3 ингредиента (порядок не важен) + Пустой флакон → зелье.
 * Рецепты — JSON в {@code data/tears-of-ink/recipe/}, тип {@code tears-of-ink:alchemy}:
 * <pre>{"type": "tears-of-ink:alchemy", "ingredients": [{"item": "..."}], "result": {"id": "...", "count": 1}}</pre>
 */
public record AlchemyRecipe(List<Ingredient> ingredients, ItemStack result) implements Recipe<AlchemyInput> {
	public static final MapCodec<AlchemyRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.listOf(1, 3).fieldOf("ingredients").forGetter(AlchemyRecipe::ingredients),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AlchemyRecipe::result)
	).apply(i, AlchemyRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), AlchemyRecipe::ingredients,
			ItemStack.STREAM_CODEC, AlchemyRecipe::result,
			AlchemyRecipe::new);

	@Override
	public boolean matches(AlchemyInput input, Level level) {
		if (!input.flask().is(ModItems.EMPTY_FLASK)) return false;
		List<ItemStack> present = new ArrayList<>();
		for (ItemStack s : input.ingredients()) if (!s.isEmpty()) present.add(s);
		if (present.size() != ingredients.size()) return false;
		// без учёта порядка: каждому ингредиенту — свой предмет
		boolean[] used = new boolean[present.size()];
		for (Ingredient ing : ingredients) {
			boolean found = false;
			for (int k = 0; k < present.size(); k++) {
				if (!used[k] && ing.test(present.get(k))) {
					used[k] = true;
					found = true;
					break;
				}
			}
			if (!found) return false;
		}
		return true;
	}

	@Override
	public ItemStack assemble(AlchemyInput input, HolderLookup.Provider registries) {
		return result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		NonNullList<Ingredient> list = NonNullList.create();
		list.addAll(ingredients);
		list.add(Ingredient.of(ModItems.EMPTY_FLASK));
		return list;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModAlchemy.SERIALIZER;
	}

	@Override
	public RecipeType<?> getType() {
		return ModAlchemy.TYPE;
	}
}
