package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
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
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.getCount() == 1 && PersonalSlots.available(owner, which)
                            && PersonalSlots.accepts(which, stack);
                }
                @Override public boolean mayPickup(Player player) {
                    return player == owner && PersonalSlots.available(owner, which);
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void tearsofink$quickMovePersonal(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < PersonalSlots.INVENTORY_MENU_START ||
                index >= PersonalSlots.INVENTORY_MENU_START + PersonalSlots.COUNT) return;
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
        slot.setByPlayer(ItemStack.EMPTY);
        slot.onTake(player, original);
        cir.setReturnValue(original);
    }
}
