package com.tarkrakov.tearsofink.client.alchemy;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.alchemy.AlchemyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Окно Алхимического стола. Фон — gui/alchemy_table.png (176×166, рисует автор). */
public class AlchemyScreen extends AbstractContainerScreen<AlchemyMenu> {
	private static final ResourceLocation BACKGROUND = TearsOfInk.id("textures/gui/alchemy_table.png");

	public AlchemyScreen(AlchemyMenu menu, Inventory inventory, Component title) {
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
