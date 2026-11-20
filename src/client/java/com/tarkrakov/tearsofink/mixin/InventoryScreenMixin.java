package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import com.tarkrakov.tearsofink.prologue.PrologueItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Small panel just to the right of the vanilla inventory; no new artwork. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void tearsofink$personalPanel(GuiGraphics graphics, float partial, int mouseX, int mouseY, CallbackInfo ci) {
        int x = leftPos + 178, y = topPos + 9;
        graphics.fill(x, y, x + 67, y + 86, 0xD927222C);
        graphics.fill(x, y, x + 67, y + 1, 0xFFAC9272);
        graphics.fill(x, y + 85, x + 67, y + 86, 0xFFAC9272);
        graphics.drawString(font, Component.translatable("gui.tears-of-ink.personal"), x + 4, y + 3, 0xE5D6BF, false);
        for (int i = 0; i < PersonalSlots.COUNT; i++) {
            int sy = topPos + 19 + 23 * i;
            graphics.fill(leftPos + 182, sy, leftPos + 201, sy + 19, 0xFF8A7967);
            graphics.fill(leftPos + 183, sy + 1, leftPos + 200, sy + 18, 0xFF433C44);
            graphics.drawString(font, Component.translatable("gui.tears-of-ink.personal." + i),
                    leftPos + 204, sy + 5, 0xF2E1CD, false);
        }
    }

    /** The personal panel must not be mistaken for an outside-inventory drop zone. */
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void tearsofink$panelIsInside(double x, double y, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (x >= left + 178 && x < left + 245 && y >= top + 9 && y < top + 95)
            cir.setReturnValue(false);
    }

    /** Right-click the bound Diary slot with an empty cursor to read in place. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tearsofink$openDiary(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 1 || !menu.getCarried().isEmpty() || !menu.slots.get(PersonalSlots.INVENTORY_MENU_START).hasItem())
            return;
        if (mouseX >= leftPos + 183 && mouseX < leftPos + 201
                && mouseY >= topPos + 20 && mouseY < topPos + 38) {
            PrologueItems.openDiary.accept(minecraft.player);
            cir.setReturnValue(true);
        }
    }
}
