package com.tarkrakov.tearsofink.client.hud;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkSyncPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Полоски Души и Чернил — рядом, справа от еды. Надписи о входе/выходе — только в чате (шлёт сервер).
 * Видны только внутри Чернильных Земель. Ванильный интерфейс не трогаем — только дорисовываем.
 */
public final class InkHud {
	// ===== Настройки — крутить здесь =====
	/** Размер рамки столбика (в пикселях интерфейса) и толщина рамки. */
	public static final int FRAME_W = 9, FRAME_H = 42, BORDER = 2;
	/** Размер заполнения внутри рамки. */
	public static final int FILL_W = FRAME_W - BORDER * 2, FILL_H = FRAME_H - BORDER * 2;
	/** Отступ первого столбика от правого края хотбара и промежуток между столбиками. */
	public static final int SIDE_GAP = 5, BAR_GAP = 10;
	/** Нижний край столбиков — на столько выше низа экрана (вровень с низом хотбара). */
	public static final int BOTTOM_OFFSET = 1;
	/**
	 * Если справа от хотбара что-то уже стоит (слот второй руки у левшей или
	 * индикатор атаки в режиме «у хотбара») — столбики отодвигаются на столько.
	 */
	public static final int MAKE_ROOM_SHIFT = 29;
	/** Цвета чисел над столбиками (RGB) и отступ чисел от верха рамки. */
	public static final int SOUL_TEXT_COLOR = 0xE6F0EE, INK_TEXT_COLOR = 0xB79BD6, TEXT_GAP = 2;
	/** Как быстро полоски проявляются/растворяются (доля в секунду). */
	public static final float FADE_PER_SECOND = 2.5f;
	/** Вспышка столбика Чернил при трате и пульсация, когда чернил не хватило (в секундах). */
	public static final float FLASH_SECONDS = 0.25f, PULSE_SECONDS = 0.6f;

	private static final ResourceLocation FRAME = TearsOfInk.id("textures/gui/ink_bar_frame.png");
	private static final ResourceLocation SOUL_FILL = TearsOfInk.id("textures/gui/soul_fill.png");
	private static final ResourceLocation SOUL_LOST = TearsOfInk.id("textures/gui/soul_lost.png");
	private static final ResourceLocation INK_FILL = TearsOfInk.id("textures/gui/ink_fill.png");

	private static boolean known = false;
	private static boolean inside = false;
	private static int soulMax = InkData.SOUL_START, inkMax = InkData.INK_START;
	private static float soul = InkData.SOUL_START, ink = InkData.INK_START;
	/** Плавно догоняющие значения — чтобы полоски не прыгали рывками. */
	private static float soulShown = soul, inkShown = ink;
	private static float alpha = 0f;
	private static long lastNanos = 0L;

	private static float flash = 0f, pulse = 0f;

	private InkHud() {}

	public static void receive(InkSyncPayload p) {
		if (known && p.ink() < ink - 0.5f) flash = FLASH_SECONDS;
		if (!known) {
			soulShown = p.soul();
			inkShown = p.ink();
		}
		known = true;
		inside = p.inside();
		soulMax = p.soulMax();
		soul = p.soul();
		inkMax = Math.max(1, p.inkMax());
		ink = p.ink();
	}

	public static void reset() {
		known = false;
		inside = false;
		alpha = 0f;
		flash = pulse = 0f;
	}

	/** Чернил (или чего-то ещё) не хватило — столбик пульсирует. */
	public static void pulse() {
		pulse = PULSE_SECONDS;
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		long now = System.nanoTime();
		float dt = lastNanos == 0L ? 0f : Math.min((now - lastNanos) / 1_000_000_000f, 0.25f);
		lastNanos = now;

		alpha = inside ? Math.min(1f, alpha + dt * FADE_PER_SECOND) : Math.max(0f, alpha - dt * FADE_PER_SECOND);
		soulShown += (soul - soulShown) * Math.min(1f, dt * 10f);
		inkShown += (ink - inkShown) * Math.min(1f, dt * 10f);
		flash = Math.max(0f, flash - dt);
		pulse = Math.max(0f, pulse - dt);

		if (mc.options.hideGui || mc.player == null) return;
		if (mc.getDebugOverlay().showDebugScreen()) return;
		boolean survival = mc.gameMode != null && mc.gameMode.canHurtPlayer();

		int w = g.guiWidth();
		int h = g.guiHeight();

		RenderSystem.enableBlend();
		if (alpha > 0f && survival) {
			int y = h - BOTTOM_OFFSET - FRAME_H;
			int soulX = w / 2 + 91 + SIDE_GAP + (rightSideBusy(mc) ? MAKE_ROOM_SHIFT : 0);
			int inkX = soulX + FRAME_W + BAR_GAP;
			g.setColor(1f, 1f, 1f, alpha);

			// Душа: заполнение — текущая душа из 100; проданная часть — трещины сверху
			drawFrame(g, soulX, y);
			int lost = Math.round(FILL_H * (InkData.SOUL_START - soulMax) / (float) InkData.SOUL_START);
			int soulH = Math.round(FILL_H * Mth.clamp(soulShown / InkData.SOUL_START, 0f, 1f));
			drawFromBottom(g, SOUL_FILL, soulX, y, soulH);
			if (lost > 0) drawFromTop(g, SOUL_LOST, soulX, y, lost);

			// Чернила: заполнение — текущие из своего запаса
			drawFrame(g, inkX, y);
			int inkH = Math.round(FILL_H * Mth.clamp(inkShown / inkMax, 0f, 1f));
			drawFromBottom(g, INK_FILL, inkX, y, inkH);

			// вспышка при трате — светлая подсветка; пульсация при нехватке — красноватое мигание рамки
			if (flash > 0f) {
				int a = (int) (alpha * 110 * flash / FLASH_SECONDS);
				g.fill(inkX + BORDER, y + BORDER, inkX + BORDER + FILL_W, y + BORDER + FILL_H, (a << 24) | 0xD8CCF0);
			}
			if (pulse > 0f) {
				float wave = (float) Math.abs(Math.sin(pulse / PULSE_SECONDS * Math.PI * 3));
				int a = (int) (alpha * 150 * wave);
				g.fill(inkX, y, inkX + FRAME_W, y + FRAME_H, (a << 24) | 0x8A1A2A);
			}

			g.setColor(1f, 1f, 1f, 1f);

			// числа над столбиками
			if (alpha > 0.05f) {
				int a = (int) (alpha * 255) << 24;
				drawNumber(g, mc, Math.round(soul), soulX, y, a | SOUL_TEXT_COLOR);
				drawNumber(g, mc, Math.round(ink), inkX, y, a | INK_TEXT_COLOR);
			}
		}
		RenderSystem.disableBlend();
	}

	/** Занято ли место справа от хотбара ванильными элементами. */
	private static boolean rightSideBusy(Minecraft mc) {
		boolean offhandOnRight = mc.options.mainHand().get() == HumanoidArm.LEFT && !mc.player.getOffhandItem().isEmpty();
		boolean attackAtHotbar = mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR;
		return offhandOnRight || attackAtHotbar;
	}

	/** Число по центру над рамкой. */
	private static void drawNumber(GuiGraphics g, Minecraft mc, int value, int frameX, int frameY, int color) {
		String text = Integer.toString(value);
		int textX = frameX + (FRAME_W - mc.font.width(text) + 1) / 2;
		g.drawString(mc.font, text, textX, frameY - mc.font.lineHeight - TEXT_GAP + 1, color, true);
	}

	private static void drawFrame(GuiGraphics g, int x, int y) {
		g.blit(FRAME, x, y, 0, 0, FRAME_W, FRAME_H, FRAME_W, FRAME_H);
	}

	/** Заполнение снизу вверх на {@code height} пикселей. */
	private static void drawFromBottom(GuiGraphics g, ResourceLocation tex, int frameX, int frameY, int height) {
		if (height <= 0) return;
		int v = FILL_H - height;
		g.blit(tex, frameX + BORDER, frameY + BORDER + v, 0, v, FILL_W, height, FILL_W, FILL_H);
	}

	/** Заполнение сверху вниз на {@code height} пикселей. */
	private static void drawFromTop(GuiGraphics g, ResourceLocation tex, int frameX, int frameY, int height) {
		g.blit(tex, frameX + BORDER, frameY + BORDER, 0, 0, FILL_W, height, FILL_W, FILL_H);
	}
}
