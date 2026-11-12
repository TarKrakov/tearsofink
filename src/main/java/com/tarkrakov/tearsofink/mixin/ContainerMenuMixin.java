package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.travel.MirrorBond;
import com.tarkrakov.tearsofink.travel.Travel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Попытка отложить зеркальце в чужой контейнер — реплика Г-на Проводника (само перекладывание запрещает SlotMixin). */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuMixin {
	@Shadow public abstract net.minecraft.world.item.ItemStack getCarried();

	@Inject(method = "clicked", at = @At("HEAD"))
	private void tearsofink$stashAttempt(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer sp) || !Travel.bound(sp)) return;
		AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
		if (slotId < 0 || slotId >= self.slots.size()) return;
		Slot slot = self.slots.get(slotId);
		boolean foreign = !(slot.container instanceof Inventory);
		boolean attempt = switch (type) {
			case PICKUP, QUICK_CRAFT -> foreign && Travel.isMirror(getCarried());
			case QUICK_MOVE -> !(self instanceof InventoryMenu) && !foreign && Travel.isMirror(slot.getItem());
			case SWAP -> foreign && button >= 0 && button < 9 && Travel.isMirror(sp.getInventory().getItem(button));
			default -> false;
		};
		if (attempt) MirrorBond.refuseStash(sp);
	}
}
