package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.prologue.DiaryEntries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.tarkrakov.tearsofink.sound.ModSounds;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;

/**
 * Дневник Историка. Авторский atlas gui/diary_base.png 256x256 и подвижное перо:
 * переплёт и кольца слева, перо сверху справа, внизу печать, справа от книги
 * свободный декоративный край; шесть вкладок снаружи корешка слева.
 *
 * Записи не меняют id и формат сохранений. Текст переносится на следующие листы
 * без уменьшения шрифта; заголовки тоже переносятся, а не упираются в перо.
 */
public class DiaryScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/diary_base.png");
	private static final ResourceLocation FEATHER = TearsOfInk.id("textures/gui/diary_feather.png");
	private final long openedAt = System.nanoTime();

	private static final int W = 256, H = 256;
	/** Temporary tab atlas: rows in DiarySection order, columns normal/hover/active. */
	private static final ResourceLocation TABS = TearsOfInk.id("textures/gui/diary_tabs.png");
	private static final int TAB_W = 24, TAB_H = 24, TAB_X = -18, TAB_ACTIVE_X = -22;
	private static final int TAB_TOP = 38, TAB_STEP = 30, LEFT_EXTRA = 22;
	/** Безопасная колонка пергамента: за кольцами и левее пера. */
	private static final int TEXT_X = 64, TEXT_W = 130;
	private static final int TITLE_Y = 23, UNTITLED_Y = 27, TITLE_LINE = 12, AFTER_TITLE = 8;
	private static final int LINE = 11, LAST_TEXT_BOTTOM = 220, FONT_H = 9;
	/** Управление только на нижнем поле бумаги, в стороне от печати. */
	private static final int AW = 18, AH = 12, PREV_X = 65, NEXT_X = 174, ARROW_Y = 225, NUMBER_Y = 227;
	private static final int TEXT = 0x2A1E2C, TITLE = 0x5A1018, FADED = 0x6E5E62, NUMBER = 0x5A4A40;

	/** Записи, открытые у игрока (присылает сервер). */
	public static List<String> entries = new ArrayList<>();

	/** Один физический лист: строки заголовка (только на первом листе записи) и текст. */
	private record Sheet(List<FormattedCharSequence> heading, List<FormattedCharSequence> lines, int color) {}

	private final List<Sheet> sheets = new ArrayList<>();
	private List<String> builtFrom = null;
	private int page;
	private DiarySection section = DiarySection.CHRONICLES;
	private DiarySection builtSection;
	private final Map<DiarySection, Integer> remembered = new EnumMap<>(DiarySection.class);
	private final Map<DiarySection, Integer> counts = new EnumMap<>(DiarySection.class);

	public DiaryScreen() {
		super(Component.translatable("diary.tears-of-ink.title"));
	}

	/** Уместить целый лист на экране; не растягивать пиксель-арт на больших мониторах. */
	private float bookScale() {
		return Math.min(1f, Math.min((width - 8f) / (W + LEFT_EXTRA), (height - 8f) / H));
	}

	private float left() { return (width - (W + LEFT_EXTRA) * bookScale()) * 0.5f + LEFT_EXTRA * bookScale(); }
	private float top() { return (height - H * bookScale()) * 0.5f; }
	private double localX(double mouseX) { return (mouseX - left()) / bookScale(); }
	private double localY(double mouseY) { return (mouseY - top()) / bookScale(); }

	private static int textStart(int headingLines) {
		return headingLines == 0 ? UNTITLED_Y : TITLE_Y + headingLines * TITLE_LINE + AFTER_TITLE;
	}

	private static int rows(int headingLines) {
		return Math.max(1, (LAST_TEXT_BOTTOM - FONT_H - textStart(headingLines)) / LINE + 1);
	}

	/** Preserve discovery order inside each section. */
	private void build() {
		if (section == builtSection && entries.equals(builtFrom)) return;
		builtFrom = List.copyOf(entries);
		builtSection = section;
		counts.clear();
		for (String id : builtFrom) counts.merge(DiarySection.of(id), 1, Integer::sum);
		sheets.clear();
		boolean caterpillars = false;
		for (String id : builtFrom) {
			if (DiarySection.of(id) != section) continue;
			if (id.equals(DiaryEntries.CATERPILLAR) || id.startsWith(DiaryEntries.CATERPILLAR + "_")) {
				if (caterpillars) continue;
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
		List<FormattedCharSequence> heading = font.split(title, TEXT_W);
		List<FormattedCharSequence> all = font.split(text, TEXT_W);
		int i = 0;
		boolean first = true;
		do {
			List<FormattedCharSequence> sheetHeading = first ? heading : List.of();
			int n = Math.min(rows(sheetHeading.size()), all.size() - i);
			sheets.add(new Sheet(sheetHeading, all.subList(i, i + n), open ? TEXT : FADED));
			i += n;
			first = false;
		} while (i < all.size());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		build();

		float scale = bookScale();
		double mx = localX(mouseX), my = localY(mouseY);
		DiarySection hovered = tabAt(mx, my);
		g.pose().pushPose();
		g.pose().translate(left(), top(), 0);
		g.pose().scale(scale, scale, 1f);
		g.blit(PAGE, 0, 0, 0, 0, W, H, W, H);
		// Мягкое движение свободного пера вокруг верхней точки крепления, без смены размеров текста.
		double seconds = (System.nanoTime() - openedAt) / 1_000_000_000.0;
		PaperMotion.layer(g, FEATHER, 181, 1, 60, 150, 182.5f, 2.5f,
				PaperMotion.sway(seconds, 4.8, 1.25, 0.0, 0.10));

		drawTabs(g, hovered);
		if (sheets.isEmpty()) {
			FitText.drawCentered(g, font, sectionTitle(section), TEXT_X + TEXT_W / 2, TITLE_Y, TEXT_W, TITLE);
			FitText.draw(g, font, Component.translatable("diary.tears-of-ink.section.empty").withStyle(ChatFormatting.ITALIC),
					TEXT_X, TITLE_Y + 23, TEXT_W, LAST_TEXT_BOTTOM - TITLE_Y - 23, FADED);
			g.pose().popPose();
			if (hovered != null) tabTooltip(g, hovered, mouseX, mouseY);
			return;
		}

		page = Math.max(0, Math.min(page, sheets.size() - 1));
		Sheet sheet = sheets.get(page);
		int y = TITLE_Y;
		for (FormattedCharSequence h : sheet.heading()) {
			g.drawString(font, h, TEXT_X + (TEXT_W - font.width(h)) / 2, y, TITLE, false);
			y += TITLE_LINE;
		}
		y = textStart(sheet.heading().size());
		for (FormattedCharSequence line : sheet.lines()) {
			g.drawString(font, line, TEXT_X, y, sheet.color(), false);
			y += LINE;
		}

		String num = (page + 1) + " / " + sheets.size();
		g.drawString(font, num, TEXT_X + (TEXT_W - font.width(num)) / 2, NUMBER_Y, NUMBER, false);
		if (page > 0) arrow(g, "diary_prev", PREV_X, mx, my);
		if (page < sheets.size() - 1) arrow(g, "diary_next", NEXT_X, mx, my);
		g.pose().popPose();
		if (hovered != null) tabTooltip(g, hovered, mouseX, mouseY);
	}

	private static Component sectionTitle(DiarySection section) {
		return Component.translatable("diary.tears-of-ink.section." + section.id);
	}
	private static int tabY(DiarySection section) { return TAB_TOP + section.ordinal() * TAB_STEP; }
	private int tabX(DiarySection tab) { return tab == section ? TAB_ACTIVE_X : TAB_X; }

	private DiarySection tabAt(double x, double y) {
		for (DiarySection tab : DiarySection.values()) {
			int tx = tabX(tab), ty = tabY(tab);
			if (x >= tx && x < tx + TAB_W && y >= ty && y < ty + TAB_H) return tab;
		}
		return null;
	}

	private void drawTabs(GuiGraphics g, DiarySection hovered) {
		for (DiarySection tab : DiarySection.values()) {
			int state = tab == section ? 2 : tab == hovered ? 1 : 0;
			g.blit(TABS, tabX(tab), tabY(tab), state * TAB_W, tab.ordinal() * TAB_H,
					TAB_W, TAB_H, 72, 144);
		}
	}

	private void tabTooltip(GuiGraphics g, DiarySection tab, int mouseX, int mouseY) {
		g.renderTooltip(font, Component.translatable("diary.tears-of-ink.section.tooltip",
				sectionTitle(tab), counts.getOrDefault(tab, 0)), mouseX, mouseY);
	}

	private void select(DiarySection next) {
		if (next == section) return;
		remembered.put(section, page);
		section = next;
		page = remembered.getOrDefault(next, 0);
		build();
		page = Math.max(0, Math.min(page, sheets.size() - 1));
		playTurn();
	}

	private void arrow(GuiGraphics g, String name, int x, double mx, double my) {
		boolean hover = mx >= x && mx < x + AW && my >= ARROW_Y && my < ARROW_Y + AH;
		g.blit(TearsOfInk.id("textures/gui/" + name + (hover ? "_hover" : "") + ".png"),
				x, ARROW_Y, 0, 0, AW, AH, AW, AH);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(g);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0) {
			double x = localX(mx), y = localY(my);
			DiarySection tab = tabAt(x, y);
			if (tab != null) { select(tab); return true; }
			if (y >= ARROW_Y && y < ARROW_Y + AH) {
				if (page > 0 && x >= PREV_X && x < PREV_X + AW) { turn(-1); return true; }
				if (page < sheets.size() - 1 && x >= NEXT_X && x < NEXT_X + AW) { turn(1); return true; }
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == 264 || key == 265) {
			DiarySection[] tabs = DiarySection.values();
			int next = Math.max(0, Math.min(tabs.length - 1, section.ordinal() + (key == 264 ? 1 : -1)));
			select(tabs[next]);
			return true;
		}
		if (key == 262 && page < sheets.size() - 1) { turn(1); return true; }
		if (key == 263 && page > 0) { turn(-1); return true; }
		return super.keyPressed(key, scan, mods);
	}

	private void turn(int d) {
		page += d;
		playTurn();
	}

	private void playTurn() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.DIARY_PAGE_TURN, 1.0f));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
