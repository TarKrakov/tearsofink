package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import com.tarkrakov.tearsofink.weapon.combat.WeaponLoadouts;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Append real server-validated slots to the ordinary player inventory menu. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
    protected InventoryMenuMixin() { super(null, 0); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tearsofink$personalSlots(Inventory inventory, boolean server, Player owner, CallbackInfo ci) {
        var container = new PersonalSlots.Items(owner);
        for (int index = 0; index < PersonalSlots.COUNT; index++) {
            final int which = index;
            addSlot(new Slot(container, index, PersonalSlots.SLOT_X, PersonalSlots.SLOT_Y + PersonalSlots.SLOT_STEP * index) {
                @Override public boolean isActive() {
                    return PersonalSlots.available(owner, which);
                }
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.getCount() == 1 && PersonalSlots.canMove(owner, which)
                            && PersonalSlots.accepts(which, stack);
                }
                @Override public boolean mayPickup(Player player) {
                    return player == owner && PersonalSlots.canMove(owner, which);
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        var reserve = new WeaponLoadouts.Items(owner);
        addSlot(tearsofink$reserveSlot(reserve, 0, WeaponLoadouts.MAIN_X));
        addSlot(tearsofink$reserveSlot(reserve, 1, WeaponLoadouts.OFF_X));
    }

    @Unique
    private Slot tearsofink$reserveSlot(WeaponLoadouts.Items container, int index, int x) {
        return new Slot(container, index, x, WeaponLoadouts.SLOT_Y) {
            @Override public boolean isActive() { return PersonalSlots.fullPanel(container.owner()); }
            @Override public boolean mayPlace(ItemStack stack) { return isActive() && container.canPlaceItem(index, stack); }
            @Override public boolean mayPickup(Player player) { return isActive() && container.stillValid(player); }
            @Override public int getMaxStackSize() { return 1; }
        };
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void tearsofink$quickMoveExtraSlot(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < PersonalSlots.INVENTORY_MENU_START ||
                index >= WeaponLoadouts.MENU_START + 2) return;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack item = slot.getItem();
        ItemStack original = item.copy();
        if (!moveItemStackTo(item, 9, 45, false)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (item.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        cir.setReturnValue(original);
    }
}
