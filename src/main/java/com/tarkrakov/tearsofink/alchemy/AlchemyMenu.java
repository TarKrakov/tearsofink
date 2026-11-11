package com.tarkrakov.tearsofink.alchemy;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

/**
 * Окно алхимического стола: 3 ячейки ингредиентов + ячейка флакона → результат.
 * Забрал результат — из каждой занятой ячейки уходит по одному предмету.
 */
public class AlchemyMenu extends AbstractContainerMenu {
	// ===== Расположение ячеек (пиксели фона окна) — менять вместе с картинкой gui/alchemy_table.png =====
	public static final int[][] INGREDIENT_POS = {{30, 26}, {48, 26}, {66, 26}};
	public static final int[] FLASK_POS = {48, 52};
	public static final int[] RESULT_POS = {124, 39};

	private static final int INPUTS = 4, FLASK = 3;
	private static final int RESULT_SLOT = 4, INV_START = 5, INV_END = 41;

	private final SimpleContainer inputs = new SimpleContainer(INPUTS);
	private final ResultContainer result = new ResultContainer();
	private final ContainerLevelAccess access;
	private final Player player;

	public AlchemyMenu(int id, Inventory inventory) {
		this(id, inventory, ContainerLevelAccess.NULL);
	}

	public AlchemyMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(ModAlchemy.MENU, id);
		this.access = access;
		this.player = inventory.player;
		inputs.addListener(this::slotsChanged);

		for (int i = 0; i < 3; i++) {
			addSlot(new Slot(inputs, i, INGREDIENT_POS[i][0], INGREDIENT_POS[i][1]) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return !stack.is(ModItems.EMPTY_FLASK);
				}
			});
		}
		addSlot(new Slot(inputs, FLASK, FLASK_POS[0], FLASK_POS[1]) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ModItems.EMPTY_FLASK);
			}
		});
		addSlot(new Slot(result, 0, RESULT_POS[0], RESULT_POS[1]) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public void onTake(Player taker, ItemStack stack) {
				for (int i = 0; i < INPUTS; i++) {
					if (!inputs.getItem(i).isEmpty()) inputs.removeItem(i, 1);
				}
				super.onTake(taker, stack);
			}
		});

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, 142));
		}
	}

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		if (!(player.level() instanceof ServerLevel level)) return;
		AlchemyInput input = new AlchemyInput(
				List.of(inputs.getItem(0), inputs.getItem(1), inputs.getItem(2)), inputs.getItem(FLASK));
		Optional<RecipeHolder<AlchemyRecipe>> recipe = level.getRecipeManager().getRecipeFor(ModAlchemy.TYPE, input, level);
		result.setItem(0, recipe.map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY));
		broadcastChanges();
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		access.execute((level, pos) -> clearContainer(player, inputs));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, ModBlocks.ALCHEMY_TABLE);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();

		if (index == RESULT_SLOT) {
			// Shift-клик по результату — сварить один раз
			if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
			slot.onTake(player, copy);
			return copy;
		}
		if (index < RESULT_SLOT) {
			if (!moveItemStackTo(stack, INV_START, INV_END, false)) return ItemStack.EMPTY;
		} else if (stack.is(ModItems.EMPTY_FLASK)) {
			if (!moveItemStackTo(stack, FLASK, FLASK + 1, false)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, 3, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return copy;
	}
}
