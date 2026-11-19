package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.prologue.DiaryEntries;
import com.tarkrakov.tearsofink.prologue.FateProgress;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Дневник Историка. Авторский atlas gui/diary_base.png 256x256 и подвижное перо:
 * переплёт и кольца слева, перо сверху справа, внизу печать, справа от книги
 * свободный декоративный край; семь вкладок снаружи корешка слева, Судьба сверху.
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
	private static final ResourceLocation FATE_TAB = TearsOfInk.id("textures/gui/diary_fate_tab.png");
	private static final int TAB_W = 24, TAB_H = 24, TAB_X = -18, TAB_ACTIVE_X = -22;
	private static final int TAB_TOP = 22, TAB_STEP = 30, LEFT_EXTRA = 22;
	/** Безопасная колонка пергамента: за кольцами и левее пера. */
	private static final int TEXT_X = 64, TEXT_W = 130;
	private static final int TITLE_Y = 23, UNTITLED_Y = 27, TITLE_LINE = 12, AFTER_TITLE = 8;
	private static final int LINE = 11, LAST_TEXT_BOTTOM = 220, FONT_H = 9;
	/** Управление только на нижнем поле бумаги, в стороне от печати. */
	private static final int AW = 18, AH = 12, PREV_X = 65, NEXT_X = 174, ARROW_Y = 225, NUMBER_Y = 227;
	private static final int TEXT = 0x2A1E2C, TITLE = 0x5A1018, FADED = 0x6E5E62, NUMBER = 0x5A4A40;
	private static final int PAGE_WINDOW = 5, PAGE_CELL = 14, ELLIPSIS_CELL = 8;
	private static final int NAV_LEFT = PREV_X + AW, NAV_RIGHT = NEXT_X;
	private static final int READ_TICKS = 10;

	private static final Set<String> readPages = new HashSet<>();
	private static boolean readReady;
	public static void receiveRead(List<String> pages) {
		readPages.clear();
		readPages.addAll(pages);
		readReady = true;
	}
	public static void resetRead() {
		readPages.clear();
		readReady = false;
	}

	/** Записи, открытые у игрока (присылает сервер). */
	public static List<String> entries = new ArrayList<>();
	/** -1 until the personal snapshot arrives. */
	public static int fateMask = -1;
	private int builtFate = Integer.MIN_VALUE;

	/** Один физический лист: строки заголовка (только на первом листе записи) и текст. */
	private record Sheet(List<FormattedCharSequence> heading, List<FormattedCharSequence> lines, int color,
			String entry, int part, boolean open) {
		private String readKey() { return entry == null ? null : DiaryEntries.readKey(entry, part); }
	}
	private record PageAnchor(String entry, int part) {}
	private record PageButton(int x, int width, int target, String label, boolean ellipsis, int from, int to) {}

	private List<Sheet> sheets = new ArrayList<>();
	private final Map<DiarySection, List<Sheet>> sectionSheets = new EnumMap<>(DiarySection.class);
	private String readingKey;
	private int readingTicks;
	private List<String> builtFrom = null;
	private int page;
	private DiarySection section = entries.contains(DiaryEntries.FIRST_STEPS) || (fateMask >= 0 && FateProgress.Goal.ARRIVAL.done(fateMask))
			? DiarySection.FATE : DiarySection.CHRONICLES;
	private final Map<DiarySection, Integer> remembered = new EnumMap<>(DiarySection.class);
	private final Map<DiarySection, PageAnchor> anchors = new EnumMap<>(DiarySection.class);
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

	private PageAnchor currentAnchor() {
		if (page < 0 || page >= sheets.size()) return null;
		Sheet sheet = sheets.get(page);
		return sheet.entry() == null ? null : new PageAnchor(sheet.entry(), sheet.part());
	}

	private void restorePage(PageAnchor anchor, int fallback) {
		page = Math.max(0, Math.min(fallback, sheets.size() - 1));
		if (anchor == null) return;
		for (int i = 0; i < sheets.size(); i++) {
			Sheet sheet = sheets.get(i);
			if (anchor.entry().equals(sheet.entry())) {
				page = i;
				if (sheet.part() == anchor.part()) return;
			}
		}
	}

	/** Stable chapter order includes one opaque placeholder for every undiscovered chapter. */
	private void build() {
		if (entries.equals(builtFrom) && fateMask == builtFate) return;
		PageAnchor anchor = currentAnchor();
		int fallback = section == DiarySection.FATE && builtFate != fateMask ? 0 : page;
		builtFrom = List.copyOf(entries);
		builtFate = fateMask;
		counts.clear();
		for (String id : builtFrom) counts.merge(DiarySection.of(id), 1, Integer::sum);
		sectionSheets.clear();
		for (DiarySection tab : DiarySection.values()) {
			sheets = new ArrayList<>();
			if (tab == DiarySection.FATE) {
				buildFate();
			} else {
				Set<String> ordered = new LinkedHashSet<>(tab.entries());
				for (String id : builtFrom) if (DiarySection.of(id) == tab) ordered.add(id);
				for (String id : ordered) addEntry(id, builtFrom.contains(id));
			}
			sectionSheets.put(tab, sheets);
		}
		sheets = sectionSheets.get(section);
		restorePage(anchor, fallback);
	}

	private boolean unread(Sheet sheet) {
		return readReady && sheet.open() && sheet.entry() != null && !readPages.contains(sheet.readKey());
	}

	private boolean sectionUnread(DiarySection tab) {
		for (Sheet sheet : sectionSheets.getOrDefault(tab, List.of())) if (unread(sheet)) return true;
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		build();
		if (sheets.isEmpty() || !unread(sheets.get(page))) {
			readingKey = null;
			readingTicks = 0;
			return;
		}
		Sheet sheet = sheets.get(page);
		String key = sheet.readKey();
		if (!key.equals(readingKey)) {
			readingKey = key;
			readingTicks = 0;
		}
		if (++readingTicks >= READ_TICKS && ClientPlayNetworking.canSend(DiaryEntries.ReadPage.TYPE)) {
			ClientPlayNetworking.send(new DiaryEntries.ReadPage(sheet.entry(), sheet.part()));
			readPages.add(key);
		}
	}

	/** Current goals first, followed by the complete crossed-out history. */
	private void buildFate() {
		String prefix = "diary.tears-of-ink.fate.";
		var heading = font.split(sectionTitle(DiarySection.FATE), TEXT_W);
		List<FormattedCharSequence> body = new ArrayList<>();
		if (fateMask < 0) {
			body.addAll(font.split(Component.translatable(prefix + "sync"), TEXT_W));
			sheets.add(new Sheet(heading, List.copyOf(body), TEXT, null, 0, true));
			return;
		}
		List<FateProgress.Goal> done = new ArrayList<>();
		for (var goal : FateProgress.Goal.values()) if (goal.done(fateMask)) done.add(goal);
		body.addAll(font.split(Component.translatable(prefix + "summary", done.size(), FateProgress.Goal.values().length), TEXT_W));
		body.add(FormattedCharSequence.EMPTY);
		var next = FateProgress.next(fateMask);
		if (next.isEmpty()) {
			body.addAll(font.split(Component.translatable(prefix + "end"), TEXT_W));
		} else {
			body.addAll(font.split(Component.translatable(prefix + "next").withStyle(style -> style.withColor(TITLE)), TEXT_W));
			for (var goal : next) {
				body.addAll(font.split(Component.translatable(prefix + goal.id + ".title").withStyle(ChatFormatting.BOLD), TEXT_W));
				body.addAll(font.split(Component.translatable(prefix + goal.id + ".hint"), TEXT_W));
			}
		}
		int capacity = rows(heading.size());
		if (!done.isEmpty() && body.size() + 3 <= capacity) {
			body.add(FormattedCharSequence.EMPTY);
			body.addAll(font.split(Component.translatable(prefix + "done").withStyle(style -> style.withColor(FADED)), TEXT_W));
			for (int i = Math.max(0, done.size() - 2); i < done.size(); i++) {
				var line = completedLine(prefix, done.get(i));
				if (body.size() + line.size() <= capacity) body.addAll(line);
			}
		}
		appendFateSheets(heading, body);
		if (!done.isEmpty()) {
			List<FormattedCharSequence> history = new ArrayList<>();
			for (var goal : done) history.addAll(completedLine(prefix, goal));
			appendFateSheets(font.split(Component.translatable(prefix + "history"), TEXT_W), history);
		}
	}

	private List<FormattedCharSequence> completedLine(String prefix, FateProgress.Goal goal) {
		return font.split(Component.translatable(prefix + goal.id + ".title")
				.withStyle(style -> style.withColor(FADED).withStrikethrough(true)), TEXT_W);
	}

	private void appendFateSheets(List<FormattedCharSequence> heading, List<FormattedCharSequence> body) {
		int offset = 0;
		do {
			var title = offset == 0 ? heading : List.<FormattedCharSequence>of();
			int end = Math.min(body.size(), offset + rows(title.size()));
			sheets.add(new Sheet(title, List.copyOf(body.subList(offset, end)), TEXT, null, 0, true));
			offset = end;
		} while (offset < body.size());
	}

	private void addEntry(String id, boolean open) {
		// Do not reveal names of undiscovered creatures or story topics.
		Component title = open || DiaryEntries.CATERPILLAR_PAGES.contains(id)
				? Component.translatable("diary.tears-of-ink.entry." + id + ".title")
				: Component.literal("???");
		Component text = open ? Component.translatable("diary.tears-of-ink.entry." + id + ".text")
				: Component.translatable("diary.tears-of-ink.illegible").withStyle(ChatFormatting.ITALIC);
		List<FormattedCharSequence> heading = font.split(title, TEXT_W);
		List<FormattedCharSequence> all = font.split(text, TEXT_W);
		int i = 0;
		boolean first = true;
		int part = 0;
		do {
			List<FormattedCharSequence> sheetHeading = first ? heading : List.of();
			int n = Math.min(rows(sheetHeading.size()), all.size() - i);
			sheets.add(new Sheet(sheetHeading, all.subList(i, i + n), open ? TEXT : FADED, id, part++, open));
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

		drawNumbers(g, mx, my);
		if (page > 0) arrow(g, "diary_prev", PREV_X, mx, my);
		if (page < sheets.size() - 1) arrow(g, "diary_next", NEXT_X, mx, my);
		g.pose().popPose();
		if (hovered != null) tabTooltip(g, hovered, mouseX, mouseY);
	}

	private static Component sectionTitle(DiarySection section) {
		return Component.translatable("diary.tears-of-ink.section." + section.id);
	}
	private static int tabIndex(DiarySection section) { return section == DiarySection.FATE ? 0 : section.ordinal() + 1; }
	private static int tabY(DiarySection section) { return TAB_TOP + tabIndex(section) * TAB_STEP; }
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
			if (tab == DiarySection.FATE) g.blit(FATE_TAB, tabX(tab), tabY(tab), state * TAB_W, 0, TAB_W, TAB_H, 72, 24);
			else g.blit(TABS, tabX(tab), tabY(tab), state * TAB_W, tab.ordinal() * TAB_H,
					TAB_W, TAB_H, 72, 144);
			if (sectionUnread(tab)) dot(g, tabX(tab) + TAB_W - 4, tabY(tab) + 3);
		}
	}

	private void tabTooltip(GuiGraphics g, DiarySection tab, int mouseX, int mouseY) {
		g.renderTooltip(font, tab == DiarySection.FATE
				? Component.translatable("diary.tears-of-ink.fate.tooltip")
				: Component.translatable("diary.tears-of-ink.section.tooltip",
						sectionTitle(tab), counts.getOrDefault(tab, 0)), mouseX, mouseY);
	}

	private void select(DiarySection next) {
		build();
		if (next == section) return;
		remembered.put(section, page);
		PageAnchor anchor = currentAnchor();
		if (anchor != null) anchors.put(section, anchor);
		section = next;
		sheets = sectionSheets.getOrDefault(next, List.of());
		restorePage(anchors.get(next), remembered.getOrDefault(next, 0));
		resetReading();
		playTurn();
	}

	/** Five numbers at most; the ellipses are also usable to jump between windows. */
	private List<PageButton> pageButtons() {
		int total = sheets.size(), start = page / PAGE_WINDOW * PAGE_WINDOW;
		int end = Math.min(total, start + PAGE_WINDOW);
		int width = (end - start) * PAGE_CELL + (start > 0 ? ELLIPSIS_CELL : 0) + (end < total ? ELLIPSIS_CELL : 0);
		int x = NAV_LEFT + (NAV_RIGHT - NAV_LEFT - width) / 2;
		List<PageButton> buttons = new ArrayList<>();
		if (start > 0) {
			buttons.add(new PageButton(x, ELLIPSIS_CELL, start - 1, "…", true, 0, start));
			x += ELLIPSIS_CELL;
		}
		for (int i = start; i < end; i++) {
			buttons.add(new PageButton(x, PAGE_CELL, i, Integer.toString(i + 1), false, i, i + 1));
			x += PAGE_CELL;
		}
		if (end < total) buttons.add(new PageButton(x, ELLIPSIS_CELL, end, "…", true, end, total));
		return buttons;
	}

	private static void dot(GuiGraphics g, int x, int y) {
		// Pixel-circle with a dark outline; temporary art matches the burgundy book accents.
		g.fill(x - 2, y - 1, x + 3, y + 2, 0xFF54242A);
		g.fill(x - 1, y - 2, x + 2, y + 3, 0xFF54242A);
		g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFE3A34B);
	}

	private void drawNumbers(GuiGraphics g, double mx, double my) {
		for (PageButton b : pageButtons()) {
			boolean hover = mx >= b.x() && mx < b.x() + b.width() && my >= ARROW_Y && my < ARROW_Y + AH;
			boolean locked = !b.ellipsis() && !sheets.get(b.target()).open();
			boolean current = !b.ellipsis() && b.target() == page;
			if (!b.ellipsis()) {
				int background = locked ? (hover ? 0xFF9C9290 : 0xFFB3AAA7) : (hover ? 0xFFE2C5A1 : 0xFFD1B58D);
				g.fill(b.x() + 1, ARROW_Y, b.x() + b.width() - 1, ARROW_Y + AH, background);
				if (current) g.hLine(b.x() + 1, b.x() + b.width() - 2, ARROW_Y + AH - 1, 0xFF681C2A);
			}
			FitText.drawCentered(g, font, Component.literal(b.label()), b.x() + b.width() / 2,
					NUMBER_Y, b.width() - 2, hover || current ? TITLE : NUMBER);
			boolean fresh = false;
			for (int i = b.from(); i < b.to(); i++) if (unread(sheets.get(i))) { fresh = true; break; }
			if (fresh) dot(g, b.x() + b.width() - 2, ARROW_Y - 1);
		}
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
			build();
			double x = localX(mx), y = localY(my);
			DiarySection tab = tabAt(x, y);
			if (tab != null) { select(tab); return true; }
			if (y >= ARROW_Y && y < ARROW_Y + AH) {
				if (page > 0 && x >= PREV_X && x < PREV_X + AW) { turn(-1); return true; }
				if (page < sheets.size() - 1 && x >= NEXT_X && x < NEXT_X + AW) { turn(1); return true; }
				for (PageButton b : pageButtons()) if (x >= b.x() && x < b.x() + b.width()) {
					goTo(b.target());
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		build();
		if (key == 264 || key == 265) {
			DiarySection[] tabs = DiarySection.values();
			int next = Math.max(0, Math.min(tabs.length - 1, tabIndex(section) + (key == 264 ? 1 : -1)));
			select(next == 0 ? DiarySection.FATE : tabs[next - 1]);
			return true;
		}
		if (key == 262 && page < sheets.size() - 1) { turn(1); return true; }
		if (key == 263 && page > 0) { turn(-1); return true; }
		return super.keyPressed(key, scan, mods);
	}

	private void resetReading() {
		readingKey = null;
		readingTicks = 0;
	}

	private void goTo(int destination) {
		if (destination == page || destination < 0 || destination >= sheets.size()) return;
		page = destination;
		resetReading();
		playTurn();
	}

	private void turn(int d) { goTo(page + d); }

	private void playTurn() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.DIARY_PAGE_TURN, 1.0f));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
