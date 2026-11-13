package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.WeaponStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Рядом с подсказкой оружия мода — вторая панелька с характеристиками (WeaponStats). */
@Mixin(AbstractContainerScreen.class)
public abstract class WeaponStatsMixin {
	@Shadow @Nullable protected Slot hoveredSlot;

	@Inject(method = "renderTooltip", at = @At("TAIL"))
	private void tearsofink$statsPanel(GuiGraphics g, int x, int y, CallbackInfo ci) {
		if (hoveredSlot != null && hoveredSlot.hasItem() && ((AbstractContainerScreen<?>) (Object) this).getMenu().getCarried().isEmpty()) {
			WeaponStats.render(g, hoveredSlot.getItem(), x, y);
		}
	}
}
