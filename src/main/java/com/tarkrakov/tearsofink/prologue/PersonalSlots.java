package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.WeakHashMap;

import java.util.ArrayList;
import java.util.List;

/** Real inventory menu slots backed by a persistent player attachment. */
public final class PersonalSlots {
    public static final int DIARY = 0, MIRROR = 1, KEY = 2, PANTHEON_KEY = 3, COUNT = 4;
    public static final int INVENTORY_MENU_START = 46;
    /** Layout relative to the ordinary inventory GUI, shared by menu slots and client rendering. */
    public static final int PANEL_X = -71, PANEL_Y = 9, PANEL_WIDTH = 67, PANEL_HEIGHT = 109;
    public static final int SLOT_X = PANEL_X + 5, SLOT_Y = 20, SLOT_STEP = 23;

    private static final AttachmentType<List<ItemStack>> DATA = AttachmentRegistry.<List<ItemStack>>builder()
            .persistent(ItemStack.OPTIONAL_CODEC.listOf())
            .copyOnDeath()
            .initializer(() -> List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY))
            .buildAndRegister(TearsOfInk.id("personal_items"));

    private static final AttachmentType<Boolean> MIGRATED = AttachmentRegistry.<Boolean>builder()
            .persistent(Codec.BOOL).copyOnDeath().initializer(() -> false)
            .buildAndRegister(TearsOfInk.id("personal_items_migrated"));

    /** Detect actual dimension changes, not routine mirror use. Weak keys avoid retaining
     * old player instances after disconnects and respawns. */
    private static final Map<ServerPlayer, ResourceKey<Level>> LAST_WORLD = new WeakHashMap<>();

    private PersonalSlots() {}

    /** Called once a second on the server, after item-use teleportation is complete.
     * Outside the Lands the mirror moves to ordinary inventory, so no hidden slot traps it.
     * A full inventory retains the exact original stack and retries when space opens. */
    public static void syncMirrorLocation(ServerPlayer player) {
        ResourceKey<Level> world = player.level().dimension();
        ResourceKey<Level> previous = LAST_WORLD.put(player, world);
        var inv = player.getInventory();
        if (ModDimensions.isLands(player.level())) {
            if (world.equals(previous) || holds(player, MIRROR)) return;
            // Restore the dedicated slot only on entering the Lands. Never steal
            // the mirror from a player's hand while they are actively using it.
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.is(ModItems.DIM_MIRROR) || stack.getCount() != 1) continue;
                put(player, MIRROR, stack);
                inv.setItem(i, ItemStack.EMPTY);
                inv.setChanged();
                return;
            }
        } else {
            ItemStack mirror = get(player, MIRROR);
            if (!mirror.is(ModItems.DIM_MIRROR)) return;
            // Only remove the saved stack after inventory insertion succeeded.
            if (inv.add(mirror.copy())) {
                put(player, MIRROR, ItemStack.EMPTY);
                inv.setChanged();
            }
        }
    }

    /** One-time, server-side migration of starter gifts when arriving in the Lands.
     * Existing characters keep exact item stacks; repeated visits do not steal from the hotbar. */
    public static void migrateStarterItems(ServerPlayer player) {
        if (!ModDimensions.isLands(player.level()) || player.getAttachedOrCreate(MIGRATED)) return;
        var inventory = player.getInventory();
        for (int slot = 0; slot < PANTHEON_KEY; slot++) {
            if (holds(player,slot)) continue;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item.getCount() != 1 || !accepts(slot,item) || item.isEmpty()) continue;
                put(player,slot,item);
                inventory.setItem(i,ItemStack.EMPTY);
                break;
            }
        }
        player.setAttached(MIGRATED,true);
    }

    public static boolean accepts(int index, ItemStack stack) {
        if (stack.isEmpty()) return true;
        return switch (index) {
            case DIARY -> stack.is(ModItems.DIARY);
            case MIRROR -> stack.is(ModItems.DIM_MIRROR);
            case KEY -> stack.is(ModItems.INK_TEAR) || stack.is(ModItems.SHARD_OF_HOPE);
            case PANTHEON_KEY -> stack.is(ModItems.PANTHEON_KEY);
            default -> false;
        };
    }

    public static boolean fullPanel(Player player) {
        return ModDimensions.isLands(player.level()) || PantheonTemple.isTemple(player.level());
    }

    public static boolean available(Player player, int index) {
        if (index < 0 || index >= COUNT) return false;
        if (index == PANTHEON_KEY) return fullPanel(player) || holds(player, index);
        return fullPanel(player) && (index != MIRROR || ModDimensions.isLands(player.level()));
    }

    public static boolean canMove(Player player, int index) {
        return index != PANTHEON_KEY && available(player, index);
    }

    public static void ensurePantheonKey(ServerPlayer player) {
        if (!holds(player, PANTHEON_KEY)) put(player, PANTHEON_KEY, new ItemStack(ModItems.PANTHEON_KEY));
    }

    public static ItemStack get(Player player, int index) {
        if (index < 0 || index >= COUNT) return ItemStack.EMPTY;
        List<ItemStack> stacks = player.getAttachedOrCreate(DATA);
        if (stacks.size() == 3) {
            stacks = List.of(stacks.get(DIARY), stacks.get(MIRROR), stacks.get(KEY), ItemStack.EMPTY);
            player.setAttached(DATA, stacks);
        }
        return index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
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
            return canMove(owner, index) && accepts(index, stack);
        }
        @Override public void clearContent() {
            for (int i = 0; i < COUNT; i++) put(owner,i,ItemStack.EMPTY);
        }
    }
}
