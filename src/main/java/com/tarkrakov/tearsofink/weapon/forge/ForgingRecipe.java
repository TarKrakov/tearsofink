package com.tarkrakov.tearsofink.weapon.forge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * Рецепт Чернильной наковальни: раскладка 3×3, как у верстака (JSON — data/tears-of-ink/recipe/forge_*.json,
 * тип "tears-of-ink:forging", поля pattern / key / result как у "minecraft:crafting_shaped"). Правка → /reload.
 * Выкованное оружие — ступень Тусклое.
 */
public class ForgingRecipe implements Recipe<CraftingInput> {
	public static final MapCodec<ForgingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
	).apply(i, ForgingRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, ForgingRecipe> STREAM_CODEC = StreamCodec.composite(
			ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
			ItemStack.STREAM_CODEC, r -> r.result,
			ForgingRecipe::new);

	final ShapedRecipePattern pattern;
	final ItemStack result;

	public ForgingRecipe(ShapedRecipePattern pattern, ItemStack result) {
		this.pattern = pattern;
		this.result = result;
	}

	/** Для JEI: размер раскладки (ингредиенты — getIngredients(), строками). */
	public int width() {
		return pattern.width();
	}

	public int height() {
		return pattern.height();
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return pattern.matches(input);
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack out = result.copy();
		if (Gear.ours(out)) Gear.setTier(out, 0);
		return out;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= pattern.width() && height >= pattern.height();
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return pattern.ingredients();
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModForge.SERIALIZER;
	}

	@Override
	public RecipeType<?> getType() {
		return ModForge.TYPE;
	}
}
