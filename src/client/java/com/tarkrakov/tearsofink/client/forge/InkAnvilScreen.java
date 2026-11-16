package com.tarkrakov.tearsofink.client.forge;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.weapon.forge.InkAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Окно Чернильной наковальни. Фон — gui/ink_anvil.png (176×166; сетка 3×3 с (30,17), результат (124,35) — заглушка Claude). */
public class InkAnvilScreen extends AbstractContainerScreen<InkAnvilMenu> {
	private static final ResourceLocation BACKGROUND = TearsOfInk.id("textures/gui/ink_anvil.png");

	public InkAnvilScreen(InkAnvilMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		g.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
	}
}
