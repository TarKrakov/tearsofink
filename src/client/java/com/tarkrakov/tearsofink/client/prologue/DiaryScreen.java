package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Дневник: одна запись — одна страница (gui/diary.png, страница — левый верхний угол 192×232 картинки 256×256),
 * стрелки листания gui/diary_next|prev(_hover).png 18×12.
 */
public class DiaryScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/diary.png");
	private static final int W = 192, H = 232, M = 18, AW = 18, AH = 12;
	/** Записи, открытые у этого игрока (присылает сервер). */
	public static List<String> entries = new ArrayList<>();

	private int page;

	public DiaryScreen() {
		super(Component.translatable("diary.tears-of-ink.title"));
	}

	private int l() { return (width - W) / 2; }
	private int t() { return (height - H) / 2; }
	private int nextX() { return l() + W - M - AW; }
	private int prevX() { return l() + M; }
	private int arrowY() { return t() + H - M - AH + 4; }

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int l = l(), t = t();
		g.blit(PAGE, l, t, 0, 0, W, H, 256, 256);
		if (entries.isEmpty()) {
			g.drawString(font, Component.translatable("diary.tears-of-ink.empty"), l + M, t + M, 0x3A2226, false);
			return;
		}
		page = Math.max(0, Math.min(page, entries.size() - 1));
		String id = entries.get(page);
		Component title = Component.translatable("diary.tears-of-ink.entry." + id + ".title");
		g.drawString(font, title, l + (W - font.width(title)) / 2, t + M, 0x5A1018, false);
		int y = t + M + 16;
		for (FormattedCharSequence line : font.split(Component.translatable("diary.tears-of-ink.entry." + id + ".text"), W - M * 2)) {
			g.drawString(font, line, l + M, y, 0x2A1E2C, false);
			y += 10;
		}
		String num = (page + 1) + " / " + entries.size();
		g.drawString(font, num, l + (W - font.width(num)) / 2, arrowY() + 2, 0x5A4A40, false);
		if (page > 0) arrow(g, "diary_prev", prevX(), mouseX, mouseY);
		if (page < entries.size() - 1) arrow(g, "diary_next", nextX(), mouseX, mouseY);
	}

	private void arrow(GuiGraphics g, String name, int x, int mx, int my) {
		boolean hover = mx >= x && mx < x + AW && my >= arrowY() && my < arrowY() + AH;
		g.blit(TearsOfInk.id("textures/gui/" + name + (hover ? "_hover" : "") + ".png"), x, arrowY(), 0, 0, AW, AH, AW, AH);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(g);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0 && my >= arrowY() && my < arrowY() + AH) {
			if (page > 0 && mx >= prevX() && mx < prevX() + AW) { turn(-1); return true; }
			if (page < entries.size() - 1 && mx >= nextX() && mx < nextX() + AW) { turn(1); return true; }
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == 262 && page < entries.size() - 1) { turn(1); return true; }  // →
		if (key == 263 && page > 0) { turn(-1); return true; }                  // ←
		return super.keyPressed(key, scan, mods);
	}

	private void turn(int d) {
		page += d;
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1f));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
