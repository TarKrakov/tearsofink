package com.tarkrakov.tearsofink.client.pantheon;

import com.tarkrakov.tearsofink.pantheon.PantheonAltarMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla inventory panel and widgets; no new authored texture is required. */
public final class PantheonAltarScreen extends AbstractContainerScreen<PantheonAltarMenu> {
    private static final ResourceLocation CHEST = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final String LANG = "gui.tears-of-ink.pantheon.ritual.";
    private Button start, stop;

    public PantheonAltarScreen(PantheonAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 199;
        inventoryLabelY = 105;
    }
    @Override protected void init() {
        super.init();
        start = addRenderableWidget(Button.builder(Component.translatable(LANG + "start"),
                b -> click(PantheonAltarMenu.START)).bounds(leftPos + 8, topPos + 56, 160, 20).build());
        stop = addRenderableWidget(Button.builder(Component.translatable(LANG + "stop"),
                b -> click(PantheonAltarMenu.STOP)).bounds(leftPos + 8, topPos + 78, 160, 20).build());
        refresh();
    }
    private void click(int button) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }
    @Override protected void containerTick() {
        super.containerTick();
        refresh();
    }
    private void refresh() {
        if (start != null) start.active = menu.canStart();
        if (stop != null) stop.active = menu.canStop();
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable(LANG + "relic"), 8, 28, 0x404040, false);
        Component status = Component.translatable(LANG + (menu.sceneRetained() ? "retained" : menu.ritualActive() ? "active"
                : !menu.canEditRelic() ? "unavailable" : menu.canStart() ? "ready" : "empty"));
        g.drawString(font, status, 8, 44, 0x404040, false);
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // Use the top frame and the exact vanilla inventory/hotbar section.
        g.blit(CHEST, x, y, 0, 0, imageWidth, 17);
        g.fill(x, y + 17, x + imageWidth, y + 103, 0xff373737);
        g.fill(x + 1, y + 17, x + imageWidth - 1, y + 103, 0xffc6c6c6);
        g.fill(x + 1, y + 17, x + 3, y + 103, 0xffffffff);
        g.fill(x + imageWidth - 3, y + 17, x + imageWidth - 1, y + 103, 0xff555555);
        g.blit(CHEST, x, y + 103, 0, 126, imageWidth, 96);
        int sx = x + PantheonAltarMenu.RELIC_X, sy = y + PantheonAltarMenu.RELIC_Y;
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xffffffff);
        g.fill(sx - 1, sy - 1, sx + 16, sy + 16, 0xff555555);
        g.fill(sx, sy, sx + 16, sy + 16, menu.canEditRelic() ? 0xff8b8b8b : 0xff666666);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        refresh();
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
