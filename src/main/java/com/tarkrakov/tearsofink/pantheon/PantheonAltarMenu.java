package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The slot mirrors the altar. Only server-side transfers can change its canonical contents. */
public final class PantheonAltarMenu extends AbstractContainerMenu {
    public static final int START = 0, STOP = 1;
    public static final int RELIC_X = 80, RELIC_Y = 24;
    private static final int INSERT = 1, EXTRACT = 2, BEGIN = 4, END = 8, ACTIVE = 16, RETAINED = 32;

    private final ServerLevel level;
    private final ContainerLevelAccess access;
    private final SimpleContainer relic = new SimpleContainer(1);
    private final DataSlot flags = DataSlot.standalone();
    private final Player owner;

    public PantheonAltarMenu(int id, Inventory inventory) { this(id, inventory, null); }

    PantheonAltarMenu(int id, Inventory inventory, ServerLevel level) {
        super(PantheonAltar.MENU, id);
        this.level = level;
        this.owner = inventory.player;
        access = level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, PantheonAltar.POS);
        addSlot(new Slot(relic, 0, RELIC_X, RELIC_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 117 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 175));
        updateView();
        addDataSlot(flags);
    }

    private void updateView() {
        if (level == null) return;
        PantheonTrial current = PantheonAltar.placed(level);
        relic.setItem(0, current == null ? ItemStack.EMPTY : current.relicStack());
        flags.set((PantheonAltar.canInsert(level) ? INSERT : 0) | (PantheonAltar.canExtract(level) ? EXTRACT : 0)
                | (PantheonCombat.canStart(level) ? BEGIN : 0)
                | (owner instanceof ServerPlayer sp && PantheonCombat.canStop(sp) ? END : 0)
                | (PantheonCombat.activeCombat() || PantheonArena.preparing() ? ACTIVE : 0)
                | (PantheonCombat.sceneOwned() && !PantheonCombat.activeCombat() ? RETAINED : 0));
    }

    public boolean canEditRelic() { return (flags.get() & (INSERT | EXTRACT)) != 0; }
    public boolean canStart() { return (flags.get() & BEGIN) != 0; }
    public boolean canStop() { return (flags.get() & END) != 0; }
    public boolean ritualActive() { return (flags.get() & ACTIVE) != 0; }
    public boolean sceneRetained() { return (flags.get() & RETAINED) != 0; }

    @Override public void broadcastChanges() {
        updateView();
        super.broadcastChanges();
    }

    @Override public boolean stillValid(Player player) {
        return player.isAlive() && !player.isSpectator() && (level == null || player.level() == level)
                && stillValid(access, player, ModBlocks.PANTHEON_ALTAR);
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer sp) || level == null
                || sp.containerMenu != this || !stillValid(sp)) return false;
        if (button == START && PantheonCombat.canStart(level)) {
            PantheonCombat.start(sp, level, PantheonAltar.placed(level));
        } else if (button == STOP && PantheonCombat.stop(sp, level)) {
            // The scene remains under the same relic after STOP.
        } else return false;
        broadcastChanges();
        return true;
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (level != null && (!stillValid(player) || player.containerMenu != this)) return;
        if (index != 0) {
            super.clicked(index, button, type, player);
            return;
        }
        if (level == null || !(player instanceof ServerPlayer sp)) return;
        if (type == ClickType.PICKUP && (button == 0 || button == 1)) {
            PantheonAltar.transfer(sp, level, getCarried(), this::setCarried);
        } else if (type == ClickType.SWAP && (button >= 0 && button < 9 || button == 40)) {
            Inventory inventory = player.getInventory();
            PantheonAltar.transfer(sp, level, inventory.getItem(button), stack -> inventory.setItem(button, stack));
        } else if (type == ClickType.QUICK_MOVE) {
            int free = player.getInventory().getFreeSlot();
            if (free >= 0) PantheonAltar.transfer(sp, level, ItemStack.EMPTY, stack -> player.getInventory().setItem(free, stack));
        }
        updateView();
        broadcastFullState();
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (level == null || !(player instanceof ServerPlayer sp) || !stillValid(player) || player.containerMenu != this
                || index <= 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem() && slot.mayPickup(player) && PantheonAltar.isRelic(slot.getItem())) {
            PantheonAltar.transfer(sp, level, slot.getItem(), slot::setByPlayer);
            updateView();
            broadcastFullState();
        }
        // Vanilla repeats quickMove until the source stops changing. One request transfers one relic.
        return ItemStack.EMPTY;
    }
}
