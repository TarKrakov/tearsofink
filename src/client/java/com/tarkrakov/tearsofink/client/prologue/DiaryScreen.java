package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.prologue.DiaryEntries;
import net.minecraft.ChatFormatting;
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
 * Дневник: у каждого существа — своя страница (позже на ней будет рисунок). Длинный текст переносится
 * на следующие страницы того же существа, шрифт не уменьшается. Гусеницы — глава из 4 страниц
 * (общее + 3 вида); не открытые виды показаны неразборчивыми.
 * Страница — gui/diary.png (левый верхний угол 192×232 картинки 256×256), стрелки gui/diary_next|prev(_hover).png 18×12.
 */
public class DiaryScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/diary.png");
	private static final int W = 192, H = 232, M = 18, AW = 18, AH = 12, LINE = 10, TITLE_GAP = 16;
	private static final int TEXT = 0x2A1E2C, TITLE = 0x5A1018, FADED = 0x6E5E62;
	/** Записи, открытые у этого игрока (присылает сервер). */
	public static List<String> entries = new ArrayList<>();

	/** Одна физическая страница: чья запись, заголовок (только на первой), строки. */
	private record Sheet(Component title, List<FormattedCharSequence> lines, int color) {}

	private final List<Sheet> sheets = new ArrayList<>();
	private List<String> builtFrom = null;
	private int page;

	public DiaryScreen() {
		super(Component.translatable("diary.tears-of-ink.title"));
	}

	private int l() { return (width - W) / 2; }
	private int t() { return (height - H) / 2; }
	private int nextX() { return l() + W - M - AW; }
	private int prevX() { return l() + M; }
	private int arrowY() { return t() + H - M - AH + 4; }
	private int textW() { return W - M * 2; }
	/** Строк на странице: с заголовком и без. */
	private int rows(boolean titled) { return (H - M - (titled ? TITLE_GAP : 0) - (M + AH + 2)) / LINE; }

	/** Раскладка записей по страницам (заново — когда сервер прислал новые записи). */
	private void build() {
		if (entries.equals(builtFrom)) return;
		builtFrom = List.copyOf(entries);
		sheets.clear();
		boolean caterpillars = false;
		for (String id : entries) {
			if (id.equals(DiaryEntries.CATERPILLAR) || id.startsWith(DiaryEntries.CATERPILLAR + "_")) {
				if (caterpillars) continue; // глава гусениц — одним блоком, в момент первой встречи
				caterpillars = true;
				for (String part : DiaryEntries.CATERPILLAR_PAGES) addEntry(part, entries.contains(part));
			} else {
				addEntry(id, true);
			}
		}
	}

	private void addEntry(String id, boolean open) {
		Component title = Component.translatable("diary.tears-of-ink.entry." + id + ".title");
		Component text = open ? Component.translatable("diary.tears-of-ink.entry." + id + ".text")
				: Component.translatable("diary.tears-of-ink.illegible").withStyle(ChatFormatting.ITALIC);
		List<FormattedCharSequence> all = font.split(text, textW());
		int i = 0;
		boolean first = true;
		do {
			int n = Math.min(rows(first), all.size() - i);
			sheets.add(new Sheet(first ? title : null, all.subList(i, i + n), open ? TEXT : FADED));
			i += n;
			first = false;
		} while (i < all.size());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		build();
		int l = l(), t = t();
		g.blit(PAGE, l, t, 0, 0, W, H, 256, 256);
		if (sheets.isEmpty()) {
			FitText.draw(g, font, Component.translatable("diary.tears-of-ink.empty"), l + M, t + M, textW(), H - M * 2, 0x3A2226);
			return;
		}
		page = Math.max(0, Math.min(page, sheets.size() - 1));
		Sheet sheet = sheets.get(page);
		int y = t + M;
		if (sheet.title() != null) {
			FitText.drawCentered(g, font, sheet.title(), l + W / 2, y, textW(), TITLE);
			y += TITLE_GAP;
		}
		for (FormattedCharSequence line : sheet.lines()) {
			g.drawString(font, line, l + M, y, sheet.color(), false);
			y += LINE;
		}
		String num = (page + 1) + " / " + sheets.size();
		g.drawString(font, num, l + (W - font.width(num)) / 2, arrowY() + 2, 0x5A4A40, false);
		if (page > 0) arrow(g, "diary_prev", prevX(), mouseX, mouseY);
		if (page < sheets.size() - 1) arrow(g, "diary_next", nextX(), mouseX, mouseY);
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
			if (page < sheets.size() - 1 && mx >= nextX() && mx < nextX() + AW) { turn(1); return true; }
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == 262 && page < sheets.size() - 1) { turn(1); return true; }  // →
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
