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
 * Окно Торговки душами. Пока лист — как у дневника (gui/diary.png, 192×232); свой фон — gui/soul_trader.png, когда нарисует автор.
 * Главная страница: продать душу / отдать контракты / доска контрактов / уйти. Страница продажи — что теряешь, и подтверждение.
 * Доска контрактов — три вкладки (лёгкий, средний, сложный), активный контракт сверху.
 */
public class SoulTraderScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/diary.png");
	private static final int W = 192, H = 232, M = 18, LINE = 12;
	private static final int TEXT = 0x2A1E2C, TITLE = 0x1E4E55, OPTION = 0x3A2A30, HOVER = 0x1E7A82, FADED = 0x8A7C7A, WARN = 0x6A1420;

	private SoulTrader.Open data;
	private boolean selling, exchanging, changing;
	/** Доска контрактов (null — главная страница). */
	private HuntContracts.BoardData board;
	private long boardAt;
	private int tab;
	private final List<Option> options = new ArrayList<>();

	private record Option(Component text, int x, int y, int w, Runnable click, boolean enabled) {}

	private SoulTraderScreen(SoulTrader.Open data) {
		super(Component.translatable("trader.tears-of-ink.title"));
		this.data = data;
	}

	/** Сервер прислал данные: открыть окно или обновить уже открытое. */
	public static void open(SoulTrader.Open data) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof SoulTraderScreen s && s.data.entity() == data.entity()) {
			s.data = data;
			s.selling = s.exchanging = s.changing = false;
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

	private int l() { return (width - W) / 2; }
	private int t() { return (height - H) / 2; }

	private void send(int action) {
		ClientPlayNetworking.send(new SoulTrader.Action(data.entity(), action));
	}

	private static String stage(int soulMax) {
		if (soulMax >= 90) return null;
		if (soulMax >= 75) return "marked";
		if (soulMax >= 60) return "tainted";
		return "hollow";
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int l = l(), t = t();
		g.blit(PAGE, l, t, 0, 0, W, H, 256, 256);
		FitText.drawCentered(g, font, Component.translatable("trader.tears-of-ink.title"), l + W / 2, t + M, W - M * 2, TITLE);
		options.clear();
		int y = t + M + 16;
		int textW = W - M * 2;
		if (board != null) {
			renderBoard(g, l, y, textW, mouseX, mouseY);
			return;
		}
		if (exchanging) {
			renderExchange(g, l, y, textW, mouseX, mouseY);
			return;
		}
		if (changing) {
			renderVow(g, l, y, textW, mouseX, mouseY);
			return;
		}
		if (!selling) {
			y = paragraph(g, Component.translatable("trader.tears-of-ink.page"), l + M, y, textW, TEXT) + 10;
			y = option(g, Component.translatable("trader.tears-of-ink.option.sell"), l + M, y, () -> { selling = true; click(); }, true, mouseX, mouseY);
			y = option(g, Component.translatable("trader.tears-of-ink.option.exchange"), l + M, y, () -> { exchanging = true; click(); }, true, mouseX, mouseY);
			y = option(g, Component.translatable("trader.tears-of-ink.option.vow"), l + M, y, () -> { changing = true; click(); },
					data.path() != com.tarkrakov.tearsofink.vow.Vow.NONE, mouseX, mouseY);
			y = option(g, Component.translatable("trader.tears-of-ink.option.contracts", data.contracts()), l + M, y,
					() -> send(SoulTrader.ACTION_CONTRACTS), data.contracts() > 0, mouseX, mouseY);
			y = option(g, Component.translatable("trader.tears-of-ink.option.board"), l + M, y,
					() -> { send(SoulTrader.ACTION_BOARD); click(); }, true, mouseX, mouseY);
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
			y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.ink", data.inkMax(), inkBack), l + M, y, textW, TEXT) + 10;
			y = option(g, Component.translatable("trader.tears-of-ink.sell.confirm"), l + M, y, () -> send(SoulTrader.ACTION_SELL), true, mouseX, mouseY);
			option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { selling = false; click(); }, true, mouseX, mouseY);
			return;
		}
		if (data.sold() >= (InkData.SOUL_START - InkData.SOUL_MIN) / InkData.SOUL_STEP) {
			y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.none"), l + M, y, textW, TEXT) + 10;
			option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { selling = false; click(); }, true, mouseX, mouseY);
			return;
		}
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.text"), l + M, y, textW, TEXT) + 6;
		int stepsNow = Math.max(0, (InkData.SOUL_START - now) / InkData.SOUL_STEP), stepsAfter = Math.max(0, (InkData.SOUL_START - after) / InkData.SOUL_STEP);
		int pNow = Math.round(stepsNow * SoulPenalty.PER_STEP * 100), pAfter = Math.round(stepsAfter * SoulPenalty.PER_STEP * 100);
		int inkAfter = Math.min(InkData.INK_CAP, data.inkMax() + InkData.INK_PER_SOUL_STEP);
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.soul", now, after), l + M, y, textW, WARN);
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.ink", data.inkMax(), inkAfter), l + M, y, textW, TEXT);
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.penalty", pNow, pAfter), l + M, y, textW, WARN);
		String sNow = stage(now), sAfter = stage(after);
		if (sAfter != null && !sAfter.equals(sNow)) {
			y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.stage",
					Component.translatable("trader.tears-of-ink.stage." + sAfter)).withStyle(ChatFormatting.ITALIC), l + M, y, textW, WARN);
		}
		y += 10;
		y = option(g, Component.translatable("trader.tears-of-ink.sell.confirm"), l + M, y, () -> send(SoulTrader.ACTION_SELL), true, mouseX, mouseY);
		option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { selling = false; click(); }, true, mouseX, mouseY);
	}

	/** Обмен Чернил на Душу (обеты): что отдаёшь, что получаешь; или почему нельзя. */
	private void renderExchange(GuiGraphics g, int l, int y, int textW, int mouseX, int mouseY) {
		y = paragraph(g, Component.translatable("trader.tears-of-ink.exchange.text"), l + M, y, textW, TEXT) + 6;
		int block = data.exchangeBlock();
		if (block != 0) {
			String why = block == 1 ? "sold" : block == 2 ? "limit" : "ink";
			y = paragraph(g, Component.translatable("trader.tears-of-ink.exchange.no_" + why), l + M, y, textW, WARN) + 10;
		} else {
			int soulAfter = data.soulMax() + com.tarkrakov.tearsofink.vow.Vows.EXCHANGE_SOUL;
			int inkAfter = data.inkMax() - com.tarkrakov.tearsofink.vow.Vows.EXCHANGE_INK;
			y = paragraph(g, Component.translatable("trader.tears-of-ink.exchange.soul", data.soulMax(), soulAfter), l + M, y, textW, TITLE);
			y = paragraph(g, Component.translatable("trader.tears-of-ink.exchange.ink", data.inkMax(), inkAfter), l + M, y, textW, WARN);
			y = paragraph(g, Component.translatable("trader.tears-of-ink.exchange.left",
					com.tarkrakov.tearsofink.vow.Vows.EXCHANGE_LIMIT - data.exchanged()), l + M, y, textW, FADED) + 10;
			y = option(g, Component.translatable("trader.tears-of-ink.exchange.confirm"), l + M, y, () -> send(SoulTrader.ACTION_EXCHANGE), true, mouseX, mouseY);
		}
		option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { exchanging = false; click(); }, true, mouseX, mouseY);
	}

	/** Смена обета: на какой путь, цена (первая бесплатно), что уснёт. */
	private void renderVow(GuiGraphics g, int l, int y, int textW, int mouseX, int mouseY) {
		com.tarkrakov.tearsofink.vow.Vow to = data.path().other();
		y = paragraph(g, Component.translatable("trader.tears-of-ink.vow.text",
				Component.translatable("vow.tears-of-ink." + data.path().id + ".name"),
				Component.translatable("vow.tears-of-ink." + to.id + ".name")), l + M, y, textW, TEXT) + 6;
		y = paragraph(g, data.changeCost() == 0
				? Component.translatable("trader.tears-of-ink.vow.free")
				: Component.translatable("trader.tears-of-ink.vow.cost", data.changeCost()), l + M, y, textW, data.changeCost() == 0 ? TITLE : WARN);
		y = paragraph(g, Component.translatable("trader.tears-of-ink.vow.sleep"), l + M, y, textW, FADED) + 10;
		y = option(g, Component.translatable("trader.tears-of-ink.vow.confirm"), l + M, y, () -> send(SoulTrader.ACTION_VOW), true, mouseX, mouseY);
		option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { changing = false; click(); }, true, mouseX, mouseY);
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
			boolean sel = e.rank() == tab;
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y - 1 && mouseY < y + 10;
			int color = sel ? TITLE : e.taken() ? FADED : hover ? HOVER : OPTION;
			g.drawString(font, sel ? name.copy().withStyle(ChatFormatting.UNDERLINE) : name, x, y, color, false);
			int rank = e.rank();
			options.add(new Option(name, x, y, w, () -> { tab = rank; click(); }, true));
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
		int bottom = t() + H - M - 22;
		y = Math.max(y, bottom);
		y = paragraph(g, Component.translatable("trader.tears-of-ink.board.refresh", clock(b.refreshSeconds() - passed)), l + M, y, textW, FADED);
		option(g, Component.translatable("trader.tears-of-ink.board.back"), l + M, y, () -> { board = null; click(); }, true, mouseX, mouseY);
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
		int w = Math.min(font.width(line), W - M * 2);
		boolean hover = enabled && mx >= x && mx < x + w && my >= y - 1 && my < y + 10;
		int color = !enabled ? FADED : hover ? HOVER : OPTION;
		var lines = font.split(line, W - M * 2);
		int yy = y;
		for (var l : lines) {
			g.drawString(font, l, x, yy, color, false);
			yy += 10;
		}
		options.add(new Option(text, x, y, W - M * 2, click, enabled));
		return y + Math.max(LINE, lines.size() * 10 + 2);
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1f));
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0) {
			for (Option o : List.copyOf(options)) {
				if (o.enabled() && mx >= o.x() && mx < o.x() + o.w() && my >= o.y() - 1 && my < o.y() + 10) {
					o.click().run();
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, button);
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
