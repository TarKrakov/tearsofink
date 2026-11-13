package com.tarkrakov.tearsofink.client.trader;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.prologue.FitText;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.SoulPenalty;
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
 * Главная страница: продать душу / отдать контракты / уйти. Страница продажи — что теряешь, и подтверждение.
 */
public class SoulTraderScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/diary.png");
	private static final int W = 192, H = 232, M = 18, LINE = 12;
	private static final int TEXT = 0x2A1E2C, TITLE = 0x1E4E55, OPTION = 0x3A2A30, HOVER = 0x1E7A82, FADED = 0x8A7C7A, WARN = 0x6A1420;

	private SoulTrader.Open data;
	private boolean selling;
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
			s.selling = false;
			return;
		}
		mc.setScreen(new SoulTraderScreen(data));
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
		if (!selling) {
			y = paragraph(g, Component.translatable("trader.tears-of-ink.page"), l + M, y, textW, TEXT) + 10;
			y = option(g, Component.translatable("trader.tears-of-ink.option.sell"), l + M, y, () -> { selling = true; click(); }, true, mouseX, mouseY);
			y = option(g, Component.translatable("trader.tears-of-ink.option.contracts", data.contracts()), l + M, y,
					() -> send(SoulTrader.ACTION_CONTRACTS), data.contracts() > 0, mouseX, mouseY);
			option(g, Component.translatable("trader.tears-of-ink.option.leave"), l + M, y, () -> { send(SoulTrader.ACTION_LEAVE); onClose(); }, true, mouseX, mouseY);
			return;
		}
		// --- страница продажи: что теряешь
		int now = data.soulMax(), after = now - InkData.SOUL_STEP;
		if (after < InkData.SOUL_MIN) {
			y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.none"), l + M, y, textW, TEXT) + 10;
			option(g, Component.translatable("trader.tears-of-ink.sell.cancel"), l + M, y, () -> { selling = false; click(); }, true, mouseX, mouseY);
			return;
		}
		y = paragraph(g, Component.translatable("trader.tears-of-ink.sell.text"), l + M, y, textW, TEXT) + 6;
		int stepsNow = (InkData.SOUL_START - now) / InkData.SOUL_STEP, stepsAfter = stepsNow + 1;
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

	private int paragraph(GuiGraphics g, Component text, int x, int y, int w, int color) {
		for (var line : font.split(text, w)) {
			g.drawString(font, line, x, y, color, false);
			y += 10;
		}
		return y;
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
