package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.travel.MirrorBond;
import com.tarkrakov.tearsofink.pantheon.PantheonGatekeeper;
import com.tarkrakov.tearsofink.pantheon.PantheonAltar;
import com.tarkrakov.tearsofink.travel.Travel;
import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import com.tarkrakov.tearsofink.weapon.combat.WeaponLoadouts;
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

	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void tearsofink$usePantheonKey(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
		if ((Object) this != player.inventoryMenu || slot != PersonalSlots.INVENTORY_MENU_START + PersonalSlots.PANTHEON_KEY) return;
		ci.cancel();
		if (type == ClickType.PICKUP && button == 1 && getCarried().isEmpty()
				&& player instanceof ServerPlayer server && server.containerMenu == server.inventoryMenu
				&& PersonalSlots.available(server, PersonalSlots.PANTHEON_KEY)) {
			PantheonGatekeeper.useKey(server);
		}
	}

	/** Hidden extra slots cannot be operated through a forged/stale inventory click. */
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void tearsofink$denyHiddenSlots(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
		if ((Object) this == player.inventoryMenu
				&& slot >= PersonalSlots.INVENTORY_MENU_START && slot < WeaponLoadouts.MENU_START + 2
				&& !PersonalSlots.fullPanel(player)) ci.cancel();
	}

	/** Also prevent shift-click merging into any pre-existing non-relic stack. */
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void tearsofink$relicQuickMove(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer sp) || type != ClickType.QUICK_MOVE) return;
		AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
		if (slotId < 0 || slotId >= menu.slots.size()) return;
		Slot slot = menu.slots.get(slotId);
		if (!(slot.container instanceof Inventory) || PantheonAltar.isRelic(slot.getItem())) return;
		if (menu.slots.stream().anyMatch(s -> PantheonAltar.isRelicChest(s.container))) {
			ci.cancel();
			sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tears-of-ink.pantheon.relic_only"), true);
		}
	}

	@Inject(method = "clicked", at = @At("HEAD"))
	private void tearsofink$stashAttempt(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer sp) || !Travel.bound(sp)) return;
		AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
		if (slotId < 0 || slotId >= self.slots.size()) return;
		Slot slot = self.slots.get(slotId);
		boolean foreign = !(slot.container instanceof Inventory) && !(slot.container instanceof PersonalSlots.Items);
		boolean attempt = switch (type) {
			case PICKUP, QUICK_CRAFT -> foreign && Travel.isMirror(getCarried());
			case QUICK_MOVE -> !(self instanceof InventoryMenu) && !foreign && Travel.isMirror(slot.getItem());
			case SWAP -> foreign && button >= 0 && button < 9 && Travel.isMirror(sp.getInventory().getItem(button));
			default -> false;
		};
		if (attempt) MirrorBond.refuseStash(sp);
	}
}
