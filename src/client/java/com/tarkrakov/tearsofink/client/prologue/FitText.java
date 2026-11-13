package com.tarkrakov.tearsofink.client.prologue;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Текст в рамке: переносится по словам и, если не влезает по высоте, плавно уменьшается (не меньше 60%). */
public final class FitText {
	private static final float MIN_SCALE = 0.6f;

	private FitText() {}

	public static void draw(GuiGraphics g, Font font, Component text, int x, int y, int width, int height, int color) {
		float scale = 1f;
		List<FormattedCharSequence> lines = font.split(text, width);
		while (lines.size() * 10 * scale > height && scale > MIN_SCALE) {
			scale -= 0.05f;
			lines = font.split(text, (int) (width / scale));
		}
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(scale, scale, 1f);
		int yy = 0;
		for (FormattedCharSequence line : lines) {
			g.drawString(font, line, 0, yy, color, false);
			yy += 10;
		}
		g.pose().popPose();
	}

	/** Одна строка по центру; если шире рамки — уменьшается. */
	public static void drawCentered(GuiGraphics g, Font font, Component text, int centerX, int y, int width, int color) {
		int w = font.width(text);
		float scale = w > width ? Math.max(MIN_SCALE, (float) width / w) : 1f;
		g.pose().pushPose();
		g.pose().translate(centerX - w * scale / 2f, y, 0);
		g.pose().scale(scale, scale, 1f);
		g.drawString(font, text, 0, 0, color, false);
		g.pose().popPose();
	}
}
