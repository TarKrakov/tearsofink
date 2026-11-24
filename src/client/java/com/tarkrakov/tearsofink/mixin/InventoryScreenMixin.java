package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import com.tarkrakov.tearsofink.prologue.PrologueItems;
import com.tarkrakov.tearsofink.client.prologue.PersonalPanelBounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
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
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> implements PersonalPanelBounds {
    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void tearsofink$personalPanel(GuiGraphics graphics, float partial, int mouseX, int mouseY, CallbackInfo ci) {
        Rect2i panel = tearsofink$personalPanelBounds();
        if (panel == null) return;
        int x = panel.getX(), y = panel.getY(), bottom = y + panel.getHeight();
        graphics.fill(x, y, x + PersonalSlots.PANEL_WIDTH, bottom, 0xD927222C);
        graphics.fill(x, y, x + PersonalSlots.PANEL_WIDTH, y + 1, 0xFFAC9272);
        graphics.fill(x, bottom - 1, x + PersonalSlots.PANEL_WIDTH, bottom, 0xFFAC9272);
        graphics.drawString(font, Component.translatable("gui.tears-of-ink.personal"), x + 4, y + 3, 0xE5D6BF, false);
        int first = PersonalSlots.fullPanel(minecraft.player) ? 0 : PersonalSlots.PANTHEON_KEY;
        for (int i = first; i < PersonalSlots.COUNT; i++) {
            int sx = leftPos + PersonalSlots.SLOT_X;
            int sy = topPos + PersonalSlots.SLOT_Y + PersonalSlots.SLOT_STEP * i;
            graphics.fill(sx - 1, sy - 1, sx + 18, sy + 18, 0xFF8A7967);
            graphics.fill(sx, sy, sx + 17, sy + 17, 0xFF433C44);
            graphics.drawString(font, Component.translatable("gui.tears-of-ink.personal." + i),
                    sx + 21, sy + 4, PersonalSlots.available(minecraft.player, i) ? 0xF2E1CD : 0x918581, false);
        }
    }

    @Override
    public Rect2i tearsofink$personalPanelBounds() {
        if (minecraft == null || minecraft.player == null
                || !PersonalSlots.available(minecraft.player, PersonalSlots.PANTHEON_KEY)) return null;
        int offset = PersonalSlots.fullPanel(minecraft.player) ? 0 : PersonalSlots.PANTHEON_KEY * PersonalSlots.SLOT_STEP;
        return new Rect2i(leftPos + PersonalSlots.PANEL_X, topPos + PersonalSlots.PANEL_Y + offset,
                PersonalSlots.PANEL_WIDTH, PersonalSlots.PANEL_HEIGHT - offset);
    }

    /** The personal panel must not be mistaken for an outside-inventory drop zone. */
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void tearsofink$panelIsInside(double x, double y, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        var panel = tearsofink$personalPanelBounds();
        if (panel != null && x >= left + PersonalSlots.PANEL_X && x < panel.getX() + panel.getWidth()
                && y >= panel.getY() && y < panel.getY() + panel.getHeight()) cir.setReturnValue(false);
    }

    /** Right-click the bound Diary slot with an empty cursor to read in place. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tearsofink$openDiary(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft == null || minecraft.player == null || !PersonalSlots.fullPanel(minecraft.player)) return;
        if (button != 1 || !menu.getCarried().isEmpty() || !menu.slots.get(PersonalSlots.INVENTORY_MENU_START).hasItem())
            return;
        if (mouseX >= leftPos + PersonalSlots.SLOT_X && mouseX < leftPos + PersonalSlots.SLOT_X + 18
                && mouseY >= topPos + PersonalSlots.SLOT_Y && mouseY < topPos + PersonalSlots.SLOT_Y + 18) {
            PrologueItems.openDiary.accept(minecraft.player);
            cir.setReturnValue(true);
        }
    }
}
