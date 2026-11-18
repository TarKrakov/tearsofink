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
 * Дневник Историка. Новый авторский atlas gui/diary.png целиком 256x256:
 * переплёт и кольца слева, перо сверху справа, внизу печать, справа от книги
 * прозрачное место для будущих разделов (пока ни кнопок, ни категорий нет).
 *
 * Записи не меняют id и формат сохранений. Текст переносится на следующие листы
 * без уменьшения шрифта; заголовки тоже переносятся, а не упираются в перо.
 */
public class DiaryScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/diary.png");

	private static final int W = 256, H = 256;
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

	public DiaryScreen() {
		super(Component.translatable("diary.tears-of-ink.title"));
	}

	/** Уместить целый лист на экране; не растягивать пиксель-арт на больших мониторах. */
	private float bookScale() {
		return Math.min(1f, Math.min((width - 8f) / W, (height - 8f) / H));
	}

	private float left() { return (width - W * bookScale()) * 0.5f; }
	private float top() { return (height - H * bookScale()) * 0.5f; }
	private double localX(double mouseX) { return (mouseX - left()) / bookScale(); }
	private double localY(double mouseY) { return (mouseY - top()) / bookScale(); }

	private static int textStart(int headingLines) {
		return headingLines == 0 ? UNTITLED_Y : TITLE_Y + headingLines * TITLE_LINE + AFTER_TITLE;
	}

	private static int rows(int headingLines) {
		return Math.max(1, (LAST_TEXT_BOTTOM - FONT_H - textStart(headingLines)) / LINE + 1);
	}

	/** Переразметка нужна только когда сервер прислал новые записи. */
	private void build() {
		if (entries.equals(builtFrom)) return;
		builtFrom = List.copyOf(entries);
		sheets.clear();
		boolean caterpillars = false;
		for (String id : entries) {
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
		int mx = (int) localX(mouseX), my = (int) localY(mouseY);
		g.pose().pushPose();
		g.pose().translate(left(), top(), 0);
		g.pose().scale(scale, scale, 1f);
		g.blit(PAGE, 0, 0, 0, 0, W, H, W, H);

		if (sheets.isEmpty()) {
			FitText.draw(g, font, Component.translatable("diary.tears-of-ink.empty"),
					TEXT_X, UNTITLED_Y, TEXT_W, LAST_TEXT_BOTTOM - UNTITLED_Y, TEXT);
			g.pose().popPose();
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
	}

	private void arrow(GuiGraphics g, String name, int x, int mx, int my) {
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
			if (y >= ARROW_Y && y < ARROW_Y + AH) {
				if (page > 0 && x >= PREV_X && x < PREV_X + AW) { turn(-1); return true; }
				if (page < sheets.size() - 1 && x >= NEXT_X && x < NEXT_X + AW) { turn(1); return true; }
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == 262 && page < sheets.size() - 1) { turn(1); return true; }
		if (key == 263 && page > 0) { turn(-1); return true; }
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
