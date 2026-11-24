package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.travel.Travel;
import com.tarkrakov.tearsofink.pantheon.PantheonAltar;
import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Тусклое зеркальце кладётся только в инвентарь самого игрока — ни в сундуки, ни в рюкзаки, ни в печи. */
@Mixin(Slot.class)
public abstract class SlotMixin {
	@Shadow @Final public Container container;

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void tearsofink$mirrorStaysWithYou(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (PantheonAltar.isRelicChest(this.container) && !PantheonAltar.isRelic(stack)) {
			cir.setReturnValue(false);
			return;
		}
		if (Travel.isMirror(stack) && !(this.container instanceof Inventory) && !(this.container instanceof PersonalSlots.Items)) cir.setReturnValue(false);
	}
}
