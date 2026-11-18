package com.tarkrakov.tearsofink.client.trader;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.prologue.FitText;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.SoulPenalty;
import com.tarkrakov.tearsofink.hunt.HuntContracts;
import com.tarkrakov.tearsofink.trader.SoulTrader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Окно Торговки душами на авторском листе gui/diary.png (256×256). Текст и кнопки
 * остаются в колонке пергамента, длинные страницы прокручиваются колёсиком/клавишами.
 * Главная страница: продать душу / отдать контракты / доска контрактов / уйти. Страница продажи — что теряешь, и подтверждение.
 * Доска контрактов — три вкладки (лёгкий, средний, сложный), активный контракт сверху.
 */
public class SoulTraderScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/diary.png");
	private static final int W = 256, H = 256, M = 64, LINE = 12;
	private static final int TEXT_W = W - M * 2, TITLE_Y = 23, BODY_Y = 43, BODY_BOTTOM = 216;
	private static final int TEXT = 0x2A1E2C, TITLE = 0x1E4E55, OPTION = 0x3A2A30, HOVER = 0x1E7A82, FADED = 0x8A7C7A, WARN = 0x6A1420;

	private SoulTrader.Open data;
	private boolean selling;
	/** Доска контрактов (null — главная страница). */
	private HuntContracts.BoardData board;
	private long boardAt;
	private int tab;
	private final List<Option> options = new ArrayList<>();
	private int scroll;
	private int contentExtent = BODY_Y;
	private int maxScroll;

	private record Option(Component text, int x, int y, int w, int h, Runnable click, boolean enabled) {}

	private SoulTraderScreen(SoulTrader.Open data) {
		super(Component.translatable("trader.tears-of-ink.title"));
		this.data = data;
	}

	/** Сервер прислал данные: открыть окно или обновить уже открытое. */
	public static void open(SoulTrader.Open data) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof SoulTraderScreen s && s.data.entity() == data.entity()) {
			s.data = data;
			s.selling = false;
			s.scroll = 0;
			return;
		}
		mc.setScreen(new SoulTraderScreen(data));
	}

	/** Сервер прислал доску контрактов: показать её в уже открытом окне. */
	public static void board(HuntContracts.BoardData d) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof SoulTraderScreen s && s.data.entity() == d.entity()) {
			boolean first = s.board == null;
			s.board = d;
			s.boardAt = System.currentTimeMillis();
			if (first) { // первая свободная вкладка
				s.scroll = 0;
				for (HuntContracts.Entry e : d.entries()) {
					if (!e.taken()) {
						s.tab = e.rank();
						break;
					}
				}
			}
		}
	}

	private static String clock(int seconds) {
		seconds = Math.max(0, seconds);
		return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
	}

	
	/** Сохраняем размеры авторского атласа, масштабируя весь GUI лишь на маленьких окнах. */
	private float paperScale() {
		return Math.min(1f, Math.min((width - 8f) / W, (height - 8f) / H));
	}
	private float left() { return (width - W * paperScale()) / 2f; }
	private float top() { return (height - H * paperScale()) / 2f; }
	private double localX(double mouseX) { return (mouseX - left()) / paperScale(); }
	private double localY(double mouseY) { return (mouseY - top()) / paperScale(); }
	private void resetScroll() { scroll = 0; maxScroll = 0; }


	private void send(int action) {
		ClientPlayNetworking.send(new SoulTrader.Action(data.entity(), action));
	}


	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		float scale = paperScale(), lx = left(), ty = top();
		int mx = (int) localX(mouseX), my = (int) localY(mouseY);

		g.pose().pushPose();
		g.pose().translate(lx, ty, 0);
		g.pose().scale(scale, scale, 1f);
		g.blit(PAGE, 0, 0, 0, 0, W, H, W, H);
		FitText.drawCentered(g, font, Component.translatable("trader.tears-of-ink.title"), W / 2, TITLE_Y, TEXT_W, TITLE);

		// Обрезаем прокручиваемое содержимое строго по бумаге, а не по окну.
		g.enableScissor((int) Math.ceil(lx + (M - 3) * scale),
				(int) Math.ceil(ty + BODY_Y * scale),
				(int) Math.floor(lx + (M + TEXT_W + 3) * scale),
				(int) Math.floor(ty + BODY_BOTTOM * scale));
		options.clear();
		contentExtent = BODY_Y;
		g.pose().pushPose();
		g.pose().translate(0, -scroll, 0);
		renderContent(g, mx, my + scroll);
		g.pose().popPose();
		g.disableScissor();

		maxScroll = Math.max(0, contentExtent - BODY_BOTTOM + 3);
		if (scroll > maxScroll) scroll = maxScroll;
		if (maxScroll > 0) {
			int track = BODY_BOTTOM - BODY_Y;
			int thumb = Math.max(12, track * track / (track + maxScroll));
			int y = BODY_Y + (track - thumb) * scroll / maxScroll;
			g.fill(M + TEXT_W + 4, BODY_Y, M + TEXT_W + 6, BODY_BOTTOM, 0x553A2226);
			g.fill(M + TEXT_W + 4, y, M + TEXT_W + 6, y + thumb, 0xBB7A4549);
		}
		g.pose().popPose();
	}

	/** Все действия остаются прежними; изменена только композиция текста и кнопок. */
	private void renderContent(GuiGraphics g, int mouseX, int mouseY) {
		int l = 0, y = BODY_Y, textW = TEXT_W;
		if (board != null) {
			renderBoard(g, l, y, textW, mouseX, mouseY);
			return;
		}
		if (!selling) {
			y = paragraph(g, Component.translatable("trader.tears-of-ink.page"), l + M, y, textW, TEXT) + 10;
			y = option(g, Component.translatable("trader.tears-of-ink.option.sell"), l + M, y, () -> { selling = true; resetScroll(); click(); }, true, mouseX, mouseY);
			y = option(g, Component.translatable("trader.tears-of-ink.option.contracts", data.contracts()), l + M, y,
					() -> send(SoulTrader.ACTION_CONTRACTS), data.contracts() > 0, mouseX, mouseY);
			y = option(g, Component.translatable("trader.tears-of-ink.option.board"), l + M, y,
					() -> { resetScroll(); send(SoulTrader.ACTION_BOARD); click(); }, true, mouseX, mouseY);
			// особый заказ: обезумевший охотник
			int order = data.order();
			if (order == 1) {
				y = option(g, Component.translatable("trader.tears-of-ink.option.order_abandon"), l + M, y,
						() -> send(SoulTrader.ACTION_ORDER_ABANDON), true, mouseX, mouseY);
			} else {
				y = option(g, order == 0 ? Component.translatable("trader.tears-of-ink.option.order")
								: Component.translatable("trader.tears-of-ink.option.order_wait", order - 2), l + M, y,
						() -> send(SoulTrader.ACTION_ORDER), order == 0, mouseX, mouseY);
			}
			int essence = essence();
			if (essence > 0) {
				y = option(g, Component.translatable("trader.tears-of-ink.option.essence", essence,
								com.tarkrakov.tearsofink.hunt.MadOrders.ESSENCE_PER_MOTE), l + M, y,
						() -> send(SoulTrader.ACTION_ESSENCE), essence >= com.tarkrakov.tearsofink.hunt.MadOrders.ESSENCE_PER_MOTE, mouseX, mouseY);
			}
			option(g, Component.translatable("trader.tears-of-ink.option.leave"), l + M, y, () -> { send(SoulTrader.ACTION_LEAVE); onClose(); }, true, mouseX, mouseY);
			return;
		}
		// --- страница продажи: что теряешь
		int now = data.soulMax(), after = now - InkData.SOUL_STEP;
		if (data.exchanged() > 0) { // Душа выше 100 от обмена — продажа откатывает обмен с потерей
			int inkBack = Math.min(InkData.INK_CAP, data.inkMax() + com.tarkrakov.tearsofink.vow.Vows.UNEXCHANGE_INK);
			y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.unexchange"), l + M, y, textW, TEXT) + 6;
			y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.soul", now, after), l + M, y, textW, WARN);
			y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.ink", data.inkMax(), inkBack), l + M, y, textW, TEXT);
			y = rankWarning(g, l, y, textW, now, after);
			y = inkRankPreview(g, l, y, textW, data.inkMax(), inkBack);
			y += 4;
			y = option(g, Component.translatable("trader.tears-of-ink.sell.confirm"), l + M, y, () -> send(SoulTrader.ACTION_SELL), true, mouseX, mouseY);
			option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { selling = false; resetScroll(); click(); }, true, mouseX, mouseY);
			return;
		}
		if (data.inkMax() >= InkData.INK_CAP) {
			y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.ink_full"), l + M, y, textW, WARN) + 10;
			option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { selling = false; resetScroll(); click(); }, true, mouseX, mouseY);
			return;
		}
		if (data.sold() >= (InkData.SOUL_START - InkData.SOUL_MIN) / InkData.SOUL_STEP) {
			y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.none"), l + M, y, textW, TEXT) + 10;
			option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { selling = false; resetScroll(); click(); }, true, mouseX, mouseY);
			return;
		}
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.text"), l + M, y, textW, TEXT) + 6;
		int stepsNow = Math.max(0, (InkData.SOUL_START - now) / InkData.SOUL_STEP), stepsAfter = Math.max(0, (InkData.SOUL_START - after) / InkData.SOUL_STEP);
		int pNow = Math.round(stepsNow * SoulPenalty.PER_STEP * 100), pAfter = Math.round(stepsAfter * SoulPenalty.PER_STEP * 100);
		int inkAfter = Math.min(InkData.INK_CAP, data.inkMax() + InkData.INK_PER_SOUL_STEP);
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.soul", now, after), l + M, y, textW, WARN);
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.ink", data.inkMax(), inkAfter), l + M, y, textW, TEXT);
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.penalty", pNow, pAfter), l + M, y, textW, WARN);
		y = rankWarning(g, l, y, textW, now, after);
		y = inkRankPreview(g, l, y, textW, data.inkMax(), inkAfter);
		y += 10;
		y = option(g, Component.translatable("trader.tears-of-ink.sell.confirm"), l + M, y, () -> send(SoulTrader.ACTION_SELL), true, mouseX, mouseY);
		option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { selling = false; resetScroll(); click(); }, true, mouseX, mouseY);
	}

	/** При продаже и получении Чернил показывать новую стадию и конкретное преимущество. */
	private int inkRankPreview(GuiGraphics g, int l, int y, int width, int before, int after) {
		if (data.path() != com.tarkrakov.tearsofink.vow.Vow.INK) return y;
		var first = com.tarkrakov.tearsofink.vow.InkRank.fromMaximum(before);
		var next = com.tarkrakov.tearsofink.vow.InkRank.fromMaximum(after);
		if (first == next) return y;
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.ink_rank_gain",
				Component.translatable("ink_rank.tears-of-ink." + first.id),
				Component.translatable("ink_rank.tears-of-ink." + next.id)), l + M, y, width, TITLE);
		return paragraph(g, Component.translatable("ink_rank.tears-of-ink.perk." + next.id), l + M, y, width, TITLE);
	}

	/** Предупредить о потере ранга ДО сделки, включая отключаемую боевую способность. */
	private int rankWarning(GuiGraphics g, int l, int y, int width, int before, int after) {
		var first = com.tarkrakov.tearsofink.vow.SoulRank.fromMaximum(before);
		var next = com.tarkrakov.tearsofink.vow.SoulRank.fromMaximum(after);
		if (first == next) return y;
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.rank_loss",
				Component.translatable("soul_rank.tears-of-ink." + first.id),
				Component.translatable("soul_rank.tears-of-ink." + next.id)), l + M, y, width, WARN);
		if (data.path() == com.tarkrakov.tearsofink.vow.Vow.SOUL) {
			y = paragraph(g, Component.translatable("soul_rank.tears-of-ink.perk." + first.id), l + M, y, width, WARN);
		}
		return y;
	}

	/** Доска контрактов: активный контракт, вкладки сложности, выбранный контракт, обновление. */
	private void renderBoard(GuiGraphics g, int l, int y, int textW, int mouseX, int mouseY) {
		HuntContracts.BoardData b = board;
		int passed = (int) ((System.currentTimeMillis() - boardAt) / 1000);
		if (b.hasActive()) {
			y = paragraph(g, b.active(), l + M, y, textW, WARN);
			y = paragraph(g, Component.translatable("trader.tears-of-ink.board.left", clock(b.activeSeconds() - passed)), l + M, y, textW, TEXT) + 2;
			y = option(g, Component.translatable("trader.tears-of-ink.board.abandon"), l + M, y,
					() -> send(SoulTrader.ACTION_ABANDON), true, mouseX, mouseY) + 4;
		}
		// вкладки
		int x = l + M;
		for (HuntContracts.Entry e : b.entries()) {
			Component name = Component.translatable("trader.tears-of-ink.board.rank." + e.rank());
			int w = font.width(name);
			if (x > l + M && x + w > l + M + textW) { x = l + M; y += 13; }
			boolean sel = e.rank() == tab;
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y - 1 && mouseY < y + 10;
			int color = sel ? TITLE : e.taken() ? FADED : hover ? HOVER : OPTION;
			g.drawString(font, sel ? name.copy().withStyle(ChatFormatting.UNDERLINE) : name, x, y, color, false);
			int rank = e.rank();
			options.add(new Option(name, x, y, w, 10, () -> { tab = rank; resetScroll(); click(); }, true));
			x += w + 10;
		}
		y += 16;
		HuntContracts.Entry cur = null;
		for (HuntContracts.Entry e : b.entries()) if (e.rank() == tab) cur = e;
		if (cur != null) {
			y = paragraph(g, Component.translatable("trader.tears-of-ink.board.target", cur.target()), l + M, y, textW, TEXT) + 2;
			y = paragraph(g, Component.translatable("trader.tears-of-ink.board.hunter", cur.hunter()), l + M, y, textW, TEXT);
			y = paragraph(g, Component.translatable("trader.tears-of-ink.board.arms", cur.arms()), l + M, y, textW, TEXT) + 2;
			y = paragraph(g, Component.translatable("trader.tears-of-ink.board.reward." + cur.rank()), l + M, y, textW, TITLE) + 6;
			int rank = cur.rank();
			Component take = Component.translatable(cur.taken() ? "trader.tears-of-ink.board.taken" : "trader.tears-of-ink.board.take");
			y = option(g, take, l + M, y, () -> send(SoulTrader.ACTION_TAKE + rank), !cur.taken() && !b.hasActive(), mouseX, mouseY) + 2;
		}
		y += 8;
		y = paragraph(g, Component.translatable("trader.tears-of-ink.board.refresh", clock(b.refreshSeconds() - passed)), l + M, y, textW, FADED);
		option(g, Component.translatable("trader.tears-of-ink.board.back"), l + M, y, () -> { board = null; resetScroll(); click(); }, true, mouseX, mouseY);
		if (b.refreshSeconds() - passed < 0 && passed % 2 == 0 && (System.currentTimeMillis() - boardAt) > 1500) {
			boardAt = System.currentTimeMillis(); // доска обновилась — попросить свежую
			send(SoulTrader.ACTION_BOARD);
		}
	}

	private int paragraph(GuiGraphics g, Component text, int x, int y, int w, int color) {
		for (var line : font.split(text, w)) {
			g.drawString(font, line, x, y, color, false);
			y += 10;
		}
		contentExtent = Math.max(contentExtent, y + 2);
		return y;
	}

	/** Эссенций надежды в инвентаре своего игрока. */
	private static int essence() {
		var p = net.minecraft.client.Minecraft.getInstance().player;
		if (p == null) return 0;
		int n = 0;
		for (var s : p.getInventory().items) if (s.is(com.tarkrakov.tearsofink.item.ModItems.HOPE_ESSENCE)) n += s.getCount();
		return n;
	}


	private int option(GuiGraphics g, Component text, int x, int y, Runnable click, boolean enabled, int mx, int my) {
		Component line = Component.literal("› ").append(text);
		var lines = font.split(line, TEXT_W);
		int h = Math.max(LINE, lines.size() * 10 + 2);
		boolean hover = enabled && mx >= x && mx < x + TEXT_W && my >= y && my < y + h;
		int color = !enabled ? FADED : hover ? HOVER : OPTION;
		int yy = y;
		for (var part : lines) {
			g.drawString(font, part, x, yy, color, false);
			yy += 10;
		}
		options.add(new Option(text, x, y, TEXT_W, h, click, enabled));
		contentExtent = Math.max(contentExtent, y + h);
		return y + h;
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1f));
	}


	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0) {
			double x = localX(mx), screenY = localY(my), y = screenY + scroll;
			if (screenY >= BODY_Y && screenY <= BODY_BOTTOM) {
				for (Option o : List.copyOf(options)) {
					if (o.enabled() && x >= o.x() && x < o.x() + o.w() && y >= o.y() && y < o.y() + o.h()) {
						o.click().run();
						return true;
					}
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		double x = localX(mx), y = localY(my);
		if (x >= M && x < M + TEXT_W + 8 && y >= BODY_Y && y <= BODY_BOTTOM && maxScroll > 0) {
			scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY) * 22));
			return true;
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		int delta = switch (key) {
			case 264 -> 20; // down
			case 265 -> -20; // up
			case 267 -> BODY_BOTTOM - BODY_Y; // PageDown
			case 266 -> BODY_Y - BODY_BOTTOM; // PageUp
			default -> 0;
		};
		if (delta != 0 && maxScroll > 0) {
			scroll = Math.max(0, Math.min(maxScroll, scroll + delta));
			return true;
		}
		return super.keyPressed(key, scan, modifiers);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(g);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
