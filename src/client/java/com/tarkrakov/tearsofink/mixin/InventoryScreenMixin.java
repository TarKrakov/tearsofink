package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import com.tarkrakov.tearsofink.prologue.PrologueItems;
import com.tarkrakov.tearsofink.world.ModDimensions;
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

/** Personal keepsakes to the left of the vanilla inventory, clear of JEI on the right. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void tearsofink$personalPanel(GuiGraphics graphics, float partial, int mouseX, int mouseY, CallbackInfo ci) {
        if (minecraft == null || minecraft.level == null || !ModDimensions.isLands(minecraft.level)) return;
        int x = leftPos + PersonalSlots.PANEL_X, y = topPos + PersonalSlots.PANEL_Y;
        graphics.fill(x, y, x + PersonalSlots.PANEL_WIDTH, y + PersonalSlots.PANEL_HEIGHT, 0xD927222C);
        graphics.fill(x, y, x + PersonalSlots.PANEL_WIDTH, y + 1, 0xFFAC9272);
        graphics.fill(x, y + PersonalSlots.PANEL_HEIGHT - 1,
                x + PersonalSlots.PANEL_WIDTH, y + PersonalSlots.PANEL_HEIGHT, 0xFFAC9272);
        graphics.drawString(font, Component.translatable("gui.tears-of-ink.personal"), x + 4, y + 3, 0xE5D6BF, false);
        for (int i = 0; i < PersonalSlots.COUNT; i++) {
            int sx = leftPos + PersonalSlots.SLOT_X;
            int sy = topPos + PersonalSlots.SLOT_Y + PersonalSlots.SLOT_STEP * i;
            graphics.fill(sx - 1, sy - 1, sx + 18, sy + 18, 0xFF8A7967);
            graphics.fill(sx, sy, sx + 17, sy + 17, 0xFF433C44);
            graphics.drawString(font, Component.translatable("gui.tears-of-ink.personal." + i),
                    sx + 21, sy + 4, 0xF2E1CD, false);
        }
    }

    /** The personal panel must not be mistaken for an outside-inventory drop zone. */
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void tearsofink$panelIsInside(double x, double y, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft == null || minecraft.level == null || !ModDimensions.isLands(minecraft.level)) return;
        if (x >= left + PersonalSlots.PANEL_X && x < left + PersonalSlots.PANEL_X + PersonalSlots.PANEL_WIDTH
                && y >= top + PersonalSlots.PANEL_Y && y < top + PersonalSlots.PANEL_Y + PersonalSlots.PANEL_HEIGHT)
            cir.setReturnValue(false);
    }

    /** Right-click the bound Diary slot with an empty cursor to read in place. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tearsofink$openDiary(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft == null || minecraft.level == null || !ModDimensions.isLands(minecraft.level)) return;
        if (button != 1 || !menu.getCarried().isEmpty() || !menu.slots.get(PersonalSlots.INVENTORY_MENU_START).hasItem())
            return;
        if (mouseX >= leftPos + PersonalSlots.SLOT_X && mouseX < leftPos + PersonalSlots.SLOT_X + 18
                && mouseY >= topPos + PersonalSlots.SLOT_Y && mouseY < topPos + PersonalSlots.SLOT_Y + 18) {
            PrologueItems.openDiary.accept(minecraft.player);
            cir.setReturnValue(true);
        }
    }
}
