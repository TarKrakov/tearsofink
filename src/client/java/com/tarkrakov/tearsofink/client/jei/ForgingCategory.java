package com.tarkrakov.tearsofink.client.jei;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.weapon.forge.ForgingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI: ковка на Чернильной наковальне — сетка 3×3 → оружие (ступень Тусклое). */
public class ForgingCategory implements IRecipeCategory<RecipeHolder<ForgingRecipe>> {
	static final int W = 120, H = 60;
	private final IGuiHelper gui;
	private final IDrawable icon, blank;

	public ForgingCategory(IGuiHelper gui) {
		this.gui = gui;
		this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.INK_ANVIL));
		this.blank = gui.createBlankDrawable(W, H);
	}

	@Override
	public RecipeType<RecipeHolder<ForgingRecipe>> getRecipeType() {
		return TearsOfInkJei.FORGING;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("jei.tears-of-ink.forging");
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	// без @Override: в разных версиях JEI размер задаётся то фоном, то шириной/высотой
	public IDrawable getBackground() {
		return blank;
	}

	public int getWidth() {
		return W;
	}

	public int getHeight() {
		return H;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ForgingRecipe> holder, IFocusGroup focuses) {
		ForgingRecipe r = holder.value();
		var ingredients = r.getIngredients();
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				var slot = builder.addSlot(RecipeIngredientRole.INPUT, 1 + col * 18, 1 + row * 18)
						.setBackground(gui.getSlotDrawable(), -1, -1);
				if (row < r.height() && col < r.width()) {
					Ingredient ing = ingredients.get(col + row * r.width());
					if (!ing.isEmpty()) slot.addIngredients(ing);
				}
			}
		}
		var level = Minecraft.getInstance().level;
		ItemStack out = level == null ? ItemStack.EMPTY : r.getResultItem(level.registryAccess());
		builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 19).setBackground(gui.getSlotDrawable(), -1, -1).addItemStack(out);
	}

	@Override
	public void draw(RecipeHolder<ForgingRecipe> recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
		JeiArrow.draw(g, 62, 21);
	}
}
