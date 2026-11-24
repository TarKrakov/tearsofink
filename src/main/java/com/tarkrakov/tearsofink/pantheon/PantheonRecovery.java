package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Owns any stacks that vanilla must eject when a menu closes with a full inventory. */
public final class PantheonRecovery {
    private static final AttachmentType<List<ItemStack>> OVERFLOW = AttachmentRegistry.<List<ItemStack>>builder()
            .persistent(ItemStack.CODEC.listOf()).copyOnDeath()
            .buildAndRegister(TearsOfInk.id("pantheon_overflow"));

    private static final String MENU_ITEMS = "TearsOfInkPantheonMenuItems";

    private PantheonRecovery() {}

    static void init() {}

    public static void writeCursor(ServerPlayer player, CompoundTag tag) {
        if (!PantheonTemple.isTemple(player.level())) return;
        var items = new ListTag();
        ItemStack cursor = player.containerMenu.getCarried();
        if (!cursor.isEmpty()) items.add(cursor.save(player.registryAccess()));
        // The crafting output at zero is derived. The four input stacks are player-owned.
        for (int slot = 1; slot <= 4; slot++) {
            ItemStack input = player.inventoryMenu.getSlot(slot).getItem();
            if (!input.isEmpty()) items.add(input.save(player.registryAccess()));
        }
        if (!items.isEmpty()) tag.put(MENU_ITEMS, items);
    }

    public static void readCursor(ServerPlayer player, CompoundTag tag) {
        for (Tag item : tag.getList(MENU_ITEMS, Tag.TAG_COMPOUND)) {
            ItemStack.parse(player.registryAccess(), item).ifPresent(stack -> store(player, stack));
        }
    }

    public static void store(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        List<ItemStack> saved = player.getAttached(OVERFLOW);
        var pending = saved == null ? new ArrayList<ItemStack>() : new ArrayList<>(saved);
        pending.add(stack.copy());
        player.setAttached(OVERFLOW, List.copyOf(pending));
    }

    static void deliver(ServerPlayer player) {
        List<ItemStack> saved = player.getAttached(OVERFLOW);
        if (saved == null) return;
        var pending = new ArrayList<ItemStack>();
        for (ItemStack stack : saved) {
            ItemStack remainder = stack.copy();
            player.getInventory().add(remainder);
            if (!remainder.isEmpty()) pending.add(remainder);
        }
        player.setAttached(OVERFLOW, pending.isEmpty() ? null : List.copyOf(pending));
    }
}
