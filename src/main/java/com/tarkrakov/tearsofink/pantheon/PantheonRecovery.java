package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Owns any stacks that vanilla must eject when a menu closes with a full inventory. */
public final class PantheonRecovery {
    private static final AttachmentType<List<ItemStack>> OVERFLOW = AttachmentRegistry.<List<ItemStack>>builder()
            .persistent(ItemStack.CODEC.listOf()).copyOnDeath()
            .buildAndRegister(TearsOfInk.id("pantheon_overflow"));

    private PantheonRecovery() {}

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
