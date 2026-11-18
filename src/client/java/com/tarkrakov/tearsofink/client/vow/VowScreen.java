package com.tarkrakov.tearsofink.client.vow;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.prologue.FitText;
import com.tarkrakov.tearsofink.vow.Vow;
import com.tarkrakov.tearsofink.vow.Vows;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
 * Выбор обета (у Г-на Проводника, после приветствия; прошедшим пролог — при входе в Земли).
 * Главная страница — два пути; описание выбранного пути; отдельная страница окончательного
 * предупреждения и финального подтверждения. Закрыть без выбора нельзя.
 * Временный фон выбора — новый авторский лист gui/diary.png (256×256).
 * Собственная vow.png может заменить его позднее; место для закладок не используется.
 */
public class VowScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/diary.png");
	private static final int W = 256, H = 256, X = 64, TEXT_W = 130, LINE = 12;
	private static final int TITLE_Y = 23, BODY_Y = 44, BOTTOM_OPTIONS_Y = 187;
	private static final int TEXT = 0x2A1E2C, TITLE = 0x5A1E2C, OPTION = 0x3A2A30, HOVER = 0x8A2A40, INK = 0x3A1E5A, SOUL = 0x1E6A72;

	/** Какой путь сейчас рассматривает (NONE — главная страница). */
	private Vow looking = Vow.NONE;
	private boolean confirming;
	private final List<Option> options = new ArrayList<>();

	private record Option(int x, int y, int w, int h, Runnable click) {}

	public VowScreen() {
		super(Component.translatable("vow.tears-of-ink.choose.title"));
	}

	public static void open() {
		Minecraft mc = Minecraft.getInstance();
		if (Vow.CLIENT != Vow.NONE || mc.screen instanceof VowScreen) return;
		mc.setScreen(new VowScreen());
	}

	/** Книжный лист целиком, без обрезки пера и переплёта; на маленьком экране уменьшаем весь интерфейс. */
	private float bookScale() {
		return Math.min(1f, Math.min((width - 8f) / W, (height - 8f) / H));
	}

	private float left() { return (width - W * bookScale()) * 0.5f; }
	private float top() { return (height - H * bookScale()) * 0.5f; }
	private double localX(double x) { return (x - left()) / bookScale(); }
	private double localY(double y) { return (y - top()) / bookScale(); }

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		float scale = bookScale();
		int mx = (int) localX(mouseX), my = (int) localY(mouseY);
		g.pose().pushPose();
		g.pose().translate(left(), top(), 0);
		g.pose().scale(scale, scale, 1f);
		g.blit(PAGE, 0, 0, 0, 0, W, H, W, H);
		options.clear();
		FitText.drawCentered(g, font, Component.translatable("vow.tears-of-ink.choose.title"), X + TEXT_W / 2, TITLE_Y, TEXT_W, TITLE);

		int y = BODY_Y;
		if (looking == Vow.NONE) {
			y = paragraph(g, Component.translatable("vow.tears-of-ink.choose.text"), X, y, TEXT_W, TEXT) + 10;
			y = option(g, Component.translatable("vow.tears-of-ink.ink.name"), X, y, INK, () -> look(Vow.INK), mx, my);
			y = paragraph(g, Component.translatable("vow.tears-of-ink.ink.short"), X + 7, y, TEXT_W - 7, TEXT) + 6;
			y = option(g, Component.translatable("vow.tears-of-ink.soul.name"), X, y, SOUL, () -> look(Vow.SOUL), mx, my);
			paragraph(g, Component.translatable("vow.tears-of-ink.soul.short"), X + 7, y, TEXT_W - 7, TEXT);
			g.pose().popPose();
			return;
		}

		int color = looking == Vow.SOUL ? SOUL : INK;
		FitText.drawCentered(g, font, Component.translatable("vow.tears-of-ink." + looking.id + ".name"), X + TEXT_W / 2, y, TEXT_W, color);
		y += 17;
		if (confirming) {
			y = paragraph(g, Component.translatable("vow.tears-of-ink.choose.irreversible"), X, y, TEXT_W, TITLE) + 10;
			y = paragraph(g, Component.translatable("vow.tears-of-ink.choose.final_warning"), X, y, TEXT_W, TEXT) + 10;
			y = Math.max(y, BOTTOM_OPTIONS_Y);
			Vow chosen = looking;
			y = option(g, Component.translatable("vow.tears-of-ink.choose.accept"), X, y, color, () -> accept(chosen), mx, my);
			option(g, Component.translatable("vow.tears-of-ink.choose.back"), X, y, OPTION, () -> { confirming = false; click(); }, mx, my);
		} else {
			y = paragraph(g, Component.translatable("vow.tears-of-ink." + looking.id + ".text"), X, y, TEXT_W, TEXT) + 10;
			y = Math.max(y, BOTTOM_OPTIONS_Y);
			y = option(g, Component.translatable("vow.tears-of-ink.choose.continue"), X, y, color,
					() -> { confirming = true; click(); }, mx, my);
			option(g, Component.translatable("vow.tears-of-ink.choose.back"), X, y, OPTION, () -> look(Vow.NONE), mx, my);
		}
		g.pose().popPose();
	}

	private void look(Vow v) {
		looking = v;
		confirming = false;
		click();
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1f));
	}

	private void accept(Vow v) {
		ClientPlayNetworking.send(new Vows.Choose(v.ordinal()));
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.7f));
		minecraft.setScreen(null);
	}

	private int paragraph(GuiGraphics g, Component text, int x, int y, int w, int color) {
		for (var line : font.split(text, w)) {
			g.drawString(font, line, x, y, color, false);
			y += 10;
		}
		return y;
	}

	private int option(GuiGraphics g, Component text, int x, int y, int color, Runnable click, int mx, int my) {
		Component line = Component.literal("› ").append(text);
		int w = Math.min(font.width(line), TEXT_W);
		boolean hover = mx >= x && mx < x + w && my >= y - 1 && my < y + 10;
		g.drawString(font, line, x, y, hover ? HOVER : color, false);
		options.add(new Option(x, y, w, 10, click));
		return y + LINE;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0) {
			for (Option o : List.copyOf(options)) {
				if (localX(mx) >= o.x() && localX(mx) < o.x() + o.w()
						&& localY(my) >= o.y() - 1 && localY(my) < o.y() + o.h()) {
					o.click().run();
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	/** Без выбора не закрыть. */
	@Override
	public boolean shouldCloseOnEsc() {
		return false;
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
