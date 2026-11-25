package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import com.tarkrakov.tearsofink.prologue.PrologueItems;
import com.tarkrakov.tearsofink.client.prologue.PersonalPanelBounds;
import com.tarkrakov.tearsofink.weapon.combat.WeaponLoadouts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
        tearsofink$drawLoadout(graphics);
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

    @Unique
    private void tearsofink$drawLoadout(GuiGraphics graphics) {
        int y = topPos + WeaponLoadouts.SLOT_Y;
        Rect2i bounds = tearsofink$loadoutPanelBounds();
        graphics.fill(leftPos, bounds.getY(), leftPos + imageWidth, bounds.getY() + bounds.getHeight(), 0xFF27222C);
        graphics.fill(leftPos, bounds.getY(), leftPos + imageWidth, bounds.getY() + 1, 0xFFAC9272);
        graphics.drawString(font, Component.translatable("gui.tears-of-ink.loadout.active_label"),
                leftPos + 7, y, 0xB0A29F, false);
        graphics.drawString(font, Component.translatable("gui.tears-of-ink.loadout.reserve"),
                leftPos + 7, y + 10, 0xE5D6BF, false);
        for (int slotX : new int[]{WeaponLoadouts.MAIN_X, WeaponLoadouts.OFF_X}) {
            int x = leftPos + slotX;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8A7967);
            graphics.fill(x, y, x + 16, y + 16, 0xFF433C44);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void tearsofink$loadoutHelp(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        if (!menu.getCarried().isEmpty() || mouseY < topPos + WeaponLoadouts.SLOT_Y
                || mouseY >= topPos + WeaponLoadouts.SLOT_Y + 16) return;
        int x = mouseX - leftPos;
        if (x >= 7 && x < WeaponLoadouts.MAIN_X - 2) {
            graphics.renderTooltip(font, Component.translatable("gui.tears-of-ink.loadout.active"), mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem()) {
            int index = hoveredSlot.index - WeaponLoadouts.MENU_START;
            if (index == 0 || index == 1) {
                graphics.renderTooltip(font, Component.translatable("gui.tears-of-ink.loadout.hand." + index), mouseX, mouseY);
            }
        }
    }

    @Override
    public Rect2i tearsofink$loadoutPanelBounds() {
        return new Rect2i(leftPos, topPos + WeaponLoadouts.SLOT_Y - 5, imageWidth, 26);
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
        Rect2i loadout = tearsofink$loadoutPanelBounds();
        if (x >= loadout.getX() && x < loadout.getX() + loadout.getWidth()
                && y >= loadout.getY() && y < loadout.getY() + loadout.getHeight()) {
            cir.setReturnValue(false);
            return;
        }
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
