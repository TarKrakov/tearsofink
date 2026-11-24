package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The server slot is a live view of the single persisted altar, shared by every open menu. */
public final class PantheonAltarMenu extends AbstractContainerMenu {
    public static final int START = 0, STOP = 1;
    public static final int RELIC_X = 80, RELIC_Y = 24;
    private static final int EDIT = 1, BEGIN = 2, END = 4, ACTIVE = 8;
    private static final int INVENTORY_START = 1, INVENTORY_END = 37;

    private final ServerLevel level;
    private final ContainerLevelAccess access;
    private final Container relic;
    private final DataSlot flags = DataSlot.standalone();
    private final Player owner;

    public PantheonAltarMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    PantheonAltarMenu(int id, Inventory inventory, ServerLevel level) {
        super(PantheonAltar.MENU, id);
        this.level = level;
        this.owner = inventory.player;
        access = level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, PantheonAltar.POS);
        relic = level == null ? new SimpleContainer(1) : new RelicContainer(level);
        addSlot(new Slot(relic, 0, RELIC_X, RELIC_Y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return editable() && PantheonAltar.isRelic(stack);
            }
            @Override public boolean mayPickup(Player player) {
                return editable() && super.mayPickup(player);
            }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 117 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 175));
        updateFlags();
        addDataSlot(flags);
    }

    private boolean editable() {
        return level == null ? (flags.get() & EDIT) != 0 : PantheonAltar.editable(level);
    }
    private boolean startAllowed() {
        return editable() && !relic.getItem(0).isEmpty();
    }
    private boolean stopAllowed() {
        if (!(owner instanceof ServerPlayer player) || level == null || PantheonEditor.active()) return false;
        if (PantheonCombat.active()) return PantheonCombat.canStop(player);
        return PantheonTemple.ready(level) && !PantheonArena.preparing() && !PantheonArena.neutralReady();
    }
    private void updateFlags() {
        if (level == null) return;
        flags.set((editable() ? EDIT : 0) | (startAllowed() ? BEGIN : 0)
                | (stopAllowed() ? END : 0)
                | ((PantheonCombat.active() || !PantheonArena.neutralReady()) ? ACTIVE : 0));
    }
    public boolean canEditRelic() { return (flags.get() & EDIT) != 0; }
    public boolean canStart() { return (flags.get() & BEGIN) != 0; }
    public boolean canStop() { return (flags.get() & END) != 0; }
    public boolean ritualActive() { return (flags.get() & ACTIVE) != 0; }

    @Override public void broadcastChanges() {
        updateFlags();
        super.broadcastChanges();
    }

    @Override public boolean stillValid(Player player) {
        return player.isAlive() && !player.isSpectator() && (level == null || player.level() == level)
                && stillValid(access, player, ModBlocks.PANTHEON_ALTAR);
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer sp) || level == null
                || sp.containerMenu != this || !stillValid(sp)) return false;
        if (button == START && startAllowed()) {
            PantheonCombat.start(sp, level, PantheonAltar.placed(level));
        } else if (button == STOP && stopAllowed()) {
            if (PantheonCombat.active()) PantheonCombat.stop(sp, level);
            else PantheonArena.request(sp.createCommandSourceStack(), PantheonArenaScenes.Scene.NEUTRAL);
        } else return false;
        broadcastChanges();
        return true;
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (level != null && (!stillValid(player) || player.containerMenu != this)) return;
        if (index == 0 && type == ClickType.CLONE) return;
        super.clicked(index, button, type, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !editable() || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) return ItemStack.EMPTY;
        } else {
            if (!PantheonAltar.isRelic(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, copy);
        return copy;
    }

    /** No clearContainer on close: the relic is stored in the altar, not owned by this menu. */
    private static final class RelicContainer implements Container {
        private final ServerLevel level;
        RelicContainer(ServerLevel level) { this.level = level; }
        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return getItem(0).isEmpty(); }
        @Override public int getMaxStackSize() { return 1; }
        @Override public ItemStack getItem(int slot) {
            PantheonTrial trial = slot == 0 ? PantheonAltar.placed(level) : null;
            return trial == null ? ItemStack.EMPTY : trial.relicStack();
        }
        @Override public ItemStack removeItem(int slot, int count) {
            if (slot != 0 || count <= 0 || !PantheonAltar.editable(level)) return ItemStack.EMPTY;
            ItemStack old = getItem(0);
            return !old.isEmpty() && PantheonAltar.setRelic(level, ItemStack.EMPTY) ? old : ItemStack.EMPTY;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, 1); }
        @Override public void setItem(int slot, ItemStack stack) {
            if (slot == 0) PantheonAltar.setRelic(level, stack);
        }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && PantheonAltar.editable(level) && PantheonAltar.isRelic(stack);
        }
        @Override public boolean stillValid(Player player) {
            return player.level() == level && player.distanceToSqr(
                    PantheonAltar.POS.getX() + 0.5, PantheonAltar.POS.getY() + 0.5,
                    PantheonAltar.POS.getZ() + 0.5) <= 64;
        }
        @Override public void setChanged() { /* Persistent chunk attachment is changed by setRelic. */ }
        @Override public void clearContent() { PantheonAltar.setRelic(level, ItemStack.EMPTY); }
    }
}
