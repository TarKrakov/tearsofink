package com.tarkrakov.tearsofink.weapon.forge;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * Окно Чернильной наковальни: сетка 3×3 + результат, как у верстака.
 * Ковка — рецепты "tears-of-ink:forging". Заточка — оружие Контрастное/Рубрики + Пигмент по одному в клетку (каждый — четверть шкалы).
 * Забрал результат — из каждой занятой клетки уходит по одному предмету.
 */
public class InkAnvilMenu extends AbstractContainerMenu {
	private static final int RESULT_SLOT = 0, GRID_START = 1, GRID_END = 10, INV_START = 10, INV_END = 46;

	private final TransientCraftingContainer grid = new TransientCraftingContainer(this, 3, 3);
	private final ResultContainer result = new ResultContainer();
	private final ContainerLevelAccess access;
	private final Player player;

	public InkAnvilMenu(int id, Inventory inventory) {
		this(id, inventory, ContainerLevelAccess.NULL);
	}

	public InkAnvilMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(ModForge.MENU, id);
		this.access = access;
		this.player = inventory.player;
		addSlot(new Slot(result, 0, 124, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public void onTake(Player taker, ItemStack stack) {
				for (int i = 0; i < grid.getContainerSize(); i++) {
					if (!grid.getItem(i).isEmpty()) grid.removeItem(i, 1);
				}
				access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.2f));
				slotsChanged(grid);
				if (taker instanceof net.minecraft.server.level.ServerPlayer sp)
					com.tarkrakov.tearsofink.prologue.FateProgress.obtained(sp, stack);
				super.onTake(taker, stack);
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				addSlot(new Slot(grid, col + row * 3, 30 + col * 18, 17 + row * 18));
			}
		}
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
		CraftingInput input = grid.asCraftInput();
		ItemStack out = level.getRecipeManager().getRecipeFor(ModForge.TYPE, input, level)
				.map(r -> r.value().assemble(input, level.registryAccess()))
				.orElseGet(this::sharpening);
		result.setItem(0, out);
		broadcastChanges();
	}

	/** Заточка: ровно одно наше оружие + только Пигмент (по одному в клетку). */
	private ItemStack sharpening() {
		ItemStack weapon = ItemStack.EMPTY;
		int pigments = 0;
		for (int i = 0; i < grid.getContainerSize(); i++) {
			ItemStack s = grid.getItem(i);
			if (s.isEmpty()) continue;
			if (s.is(ModItems.PIGMENT)) pigments++;
			else if (Gear.ours(s) && weapon.isEmpty()) weapon = s;
			else return ItemStack.EMPTY;
		}
		if (weapon.isEmpty() || pigments == 0) return ItemStack.EMPTY;
		ItemStack out = weapon.copyWithCount(1);
		return Gear.sharpen(out, pigments) ? out : ItemStack.EMPTY;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		access.execute((level, pos) -> clearContainer(player, grid));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, ModBlocks.INK_ANVIL);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index == RESULT_SLOT) {
			if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
			slot.onTake(player, copy);
			return copy;
		}
		if (index < GRID_END) {
			if (!moveItemStackTo(stack, INV_START, INV_END, false)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, GRID_START, GRID_END, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return copy;
	}
}
