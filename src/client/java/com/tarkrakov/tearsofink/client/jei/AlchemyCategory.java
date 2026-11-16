package com.tarkrakov.tearsofink.client.jei;

import com.tarkrakov.tearsofink.alchemy.AlchemyRecipe;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.item.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI: Алхимический стол — до 3 ингредиентов + Пустой флакон → зелье (порядок неважен). */
public class AlchemyCategory implements IRecipeCategory<RecipeHolder<AlchemyRecipe>> {
	static final int W = 120, H = 44;
	private final IGuiHelper gui;
	private final IDrawable icon, blank;

	public AlchemyCategory(IGuiHelper gui) {
		this.gui = gui;
		this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.ALCHEMY_TABLE));
		this.blank = gui.createBlankDrawable(W, H);
	}

	@Override
	public RecipeType<RecipeHolder<AlchemyRecipe>> getRecipeType() {
		return TearsOfInkJei.ALCHEMY;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("jei.tears-of-ink.alchemy");
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

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
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlchemyRecipe> holder, IFocusGroup focuses) {
		AlchemyRecipe r = holder.value();
		for (int i = 0; i < 3; i++) {
			var slot = builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 1).setBackground(gui.getSlotDrawable(), -1, -1);
			if (i < r.ingredients().size()) slot.addIngredients(r.ingredients().get(i));
		}
		builder.addSlot(RecipeIngredientRole.INPUT, 19, 26).setBackground(gui.getSlotDrawable(), -1, -1)
				.addItemStack(new ItemStack(ModItems.EMPTY_FLASK));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 14).setBackground(gui.getSlotDrawable(), -1, -1)
				.addItemStack(r.result());
	}

	@Override
	public void draw(RecipeHolder<AlchemyRecipe> recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
		JeiArrow.draw(g, 62, 16);
	}
}
