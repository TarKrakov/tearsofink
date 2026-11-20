package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Three personal, one-item inventory slots backed by a persistent player attachment.
 * Normal container packets synchronize the three slots; no client-authoritative item moves.
 * The mirror is accessible outside the Lands so players can return to the adventure.
 */
public final class PersonalSlots {
    public static final int DIARY = 0, MIRROR = 1, KEY = 2, COUNT = 3;
    public static final int INVENTORY_MENU_START = 46;

    private static final AttachmentType<List<ItemStack>> DATA = AttachmentRegistry.<List<ItemStack>>builder()
            .persistent(ItemStack.OPTIONAL_CODEC.listOf())
            .copyOnDeath()
            .initializer(() -> List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY))
            .buildAndRegister(TearsOfInk.id("personal_items"));

    private PersonalSlots() {}

    public static boolean accepts(int index, ItemStack stack) {
        if (stack.isEmpty()) return true;
        return switch (index) {
            case DIARY -> stack.is(ModItems.DIARY);
            case MIRROR -> stack.is(ModItems.DIM_MIRROR);
            case KEY -> stack.is(ModItems.INK_TEAR) || stack.is(ModItems.SHARD_OF_HOPE);
            default -> false;
        };
    }

    public static boolean available(Player player, int index) {
        // Never trap the travel mirror in another dimension.
        return index == MIRROR || ModDimensions.isLands(player.level());
    }

    public static ItemStack get(Player player, int index) {
        if (index < 0 || index >= COUNT) return ItemStack.EMPTY;
        List<ItemStack> stacks = player.getAttachedOrCreate(DATA);
        return stacks.size() == COUNT ? stacks.get(index) : ItemStack.EMPTY;
    }

    public static boolean holds(Player player, int index) {
        return !get(player, index).isEmpty();
    }

    private static void put(Player player, int index, ItemStack stack) {
        if (index < 0 || index >= COUNT || (!stack.isEmpty() && (!accepts(index, stack) || stack.getCount() != 1)))
            return;
        List<ItemStack> old = player.getAttachedOrCreate(DATA);
        ArrayList<ItemStack> next = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++)
            next.add(i == index ? (stack.isEmpty() ? ItemStack.EMPTY : stack.copy()) :
                    i < old.size() ? old.get(i).copy() : ItemStack.EMPTY);
        player.setAttached(DATA, List.copyOf(next));
    }

    public static final class Items implements Container {
        private final Player owner;
        public Items(Player owner) { this.owner = owner; }

        @Override public int getContainerSize() { return COUNT; }
        @Override public boolean isEmpty() {
            for (int i = 0; i < COUNT; i++) if (!getItem(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int index) { return get(owner,index); }

        @Override public ItemStack removeItem(int index, int amount) {
            if (amount <= 0 || index < 0 || index >= COUNT) return ItemStack.EMPTY;
            ItemStack item = getItem(index);
            if (!item.isEmpty()) put(owner, index, ItemStack.EMPTY);
            return item;
        }

        @Override public ItemStack removeItemNoUpdate(int index) { return removeItem(index, 1); }
        @Override public void setItem(int index, ItemStack stack) { put(owner, index, stack); }
        @Override public void setChanged() { /* put() updates the persistent attachment */ }
        @Override public boolean stillValid(Player player) { return player == owner; }
        @Override public boolean canPlaceItem(int index, ItemStack stack) {
            return available(owner,index) && accepts(index, stack);
        }
        @Override public void clearContent() {
            for (int i = 0; i < COUNT; i++) put(owner,i,ItemStack.EMPTY);
        }
    }
}
