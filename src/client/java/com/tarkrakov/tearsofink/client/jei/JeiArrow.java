package com.tarkrakov.tearsofink.client.jei;

import net.minecraft.client.gui.GuiGraphics;

/** Простая серая стрелка «→» (22×15) — без чужих текстур. */
final class JeiArrow {
	private JeiArrow() {}

	static void draw(GuiGraphics g, int x, int y) {
		int c = 0xFF8B8B8B;
		g.fill(x, y + 5, x + 14, y + 10, c);
		for (int i = 0; i < 8; i++) g.fill(x + 14 + i, y + i, x + 15 + i, y + 15 - i, c);
	}
}
