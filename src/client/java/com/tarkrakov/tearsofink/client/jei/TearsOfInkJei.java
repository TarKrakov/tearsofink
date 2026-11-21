package com.tarkrakov.tearsofink.client.jei;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.alchemy.AlchemyMenu;
import com.tarkrakov.tearsofink.alchemy.AlchemyRecipe;
import com.tarkrakov.tearsofink.alchemy.ModAlchemy;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.client.alchemy.AlchemyScreen;
import com.tarkrakov.tearsofink.client.forge.InkAnvilScreen;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.weapon.forge.ForgingRecipe;
import com.tarkrakov.tearsofink.weapon.forge.InkAnvilMenu;
import com.tarkrakov.tearsofink.weapon.forge.ModForge;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * Связка с JEI (Just Enough Items). Мод работает и без JEI — этот класс JEI сам находит через fabric.mod.json
 * ("jei_mod_plugin"), если JEI установлен.
 * Что даёт: окна рецептов Чернильной наковальни (ковка) и Алхимического стола, кнопка «+» переносит рецепт в окно,
 * клик по стрелке в окне открывает его рецепты, страницы-подсказки «откуда это» (тексты — jei.tears-of-ink.info.*).
 */
public class TearsOfInkJei implements IModPlugin {
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final RecipeType<RecipeHolder<ForgingRecipe>> FORGING =
			RecipeType.create(TearsOfInk.MOD_ID, "forging", (Class) RecipeHolder.class);
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final RecipeType<RecipeHolder<AlchemyRecipe>> ALCHEMY =
			RecipeType.create(TearsOfInk.MOD_ID, "alchemy", (Class) RecipeHolder.class);


	@Override
	public ResourceLocation getPluginUid() {
		return TearsOfInk.id("jei");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new ForgingCategory(gui), new AlchemyCategory(gui));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		var level = Minecraft.getInstance().level;
		if (level != null) {
			var recipes = level.getRecipeManager();
			registration.addRecipes(FORGING, recipes.getAllRecipesFor(ModForge.TYPE));
			registration.addRecipes(ALCHEMY, recipes.getAllRecipesFor(ModAlchemy.TYPE));
		}
		// страницы «откуда это»: у любого предмета мода, для которого в языковом файле есть jei.tears-of-ink.info.<имя>
		for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
			if (!TearsOfInk.MOD_ID.equals(key.getNamespace())) continue;
			String lang = "jei.tears-of-ink.info." + key.getPath();
			if (net.minecraft.locale.Language.getInstance().has(lang))
				registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK, Component.translatable(lang));
		}
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.INK_ANVIL), FORGING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ALCHEMY_TABLE), ALCHEMY);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		// наковальня: 0 — результат, 1–9 — сетка, 10–45 — инвентарь
		registration.addRecipeTransferHandler(InkAnvilMenu.class, ModForge.MENU, FORGING, 1, 9, 10, 36);
		// стол: 0–2 — ингредиенты, 3 — флакон, 4 — результат, 5–40 — инвентарь
		registration.addRecipeTransferHandler(AlchemyMenu.class, ModAlchemy.MENU, ALCHEMY, 0, 4, 5, 36);
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addRecipeClickArea(InkAnvilScreen.class, 90, 35, 26, 17, FORGING);
		registration.addRecipeClickArea(AlchemyScreen.class, 88, 36, 28, 18, ALCHEMY);
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		// Служебные и выведенные из обращения предметы (ID остаются для сохранений).
		List<ItemStack> hidden = List.of(
				new ItemStack(ModItems.INK_BLOT),
				new ItemStack(ModItems.HUNTER_GREATSWORD), new ItemStack(ModItems.HUNTER_SABER),
				new ItemStack(ModItems.PALE_SWORD), new ItemStack(ModItems.PALE_GREATSWORD),
				new ItemStack(ModItems.BLOOD_SWORD), new ItemStack(ModItems.BLOOD_GREATSWORD),
				new ItemStack(ModItems.INK_SWORD), new ItemStack(ModItems.INK_GREATSWORD),
				new ItemStack(ModItems.BLOOD_KATANA), new ItemStack(ModItems.BLOOD_OBSI_CLUB));
		runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
	}

	static ItemStack stack(Item item) {
		return new ItemStack(item);
	}
}
