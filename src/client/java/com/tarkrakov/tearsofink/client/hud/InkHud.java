package com.tarkrakov.tearsofink.client.hud;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkSyncPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Полоски Души (слева от сердец) и Чернил (справа от еды) + короткая надпись при входе/выходе.
 * Видны только внутри Чернильных Земель. Ванильный интерфейс не трогаем — только дорисовываем.
 */
public final class InkHud {
	// ===== Настройки — крутить здесь =====
	/** Размер рамки столбика (в пикселях интерфейса) и толщина рамки. */
	public static final int FRAME_W = 9, FRAME_H = 42, BORDER = 2;
	/** Размер заполнения внутри рамки. */
	public static final int FILL_W = FRAME_W - BORDER * 2, FILL_H = FRAME_H - BORDER * 2;
	/** Отступ столбиков от краёв хотбара. */
	public static final int SIDE_GAP = 13;
	/** Нижний край столбиков — на столько выше низа экрана (над слотом второй руки). */
	public static final int BOTTOM_OFFSET = 25;
	/** Как быстро полоски проявляются/растворяются (доля в секунду). */
	public static final float FADE_PER_SECOND = 2.5f;
	/** Сколько висит надпись при входе/выходе (тиков) и сколько из них она гаснет. */
	public static final int MESSAGE_TICKS = 40, MESSAGE_FADE_TICKS = 12;

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

	private static Component message = null;
	private static int messageTicks = 0;

	private InkHud() {}

	public static void receive(InkSyncPayload p) {
		if (known && p.inside() != inside) {
			message = Component.translatable(p.inside() ? "message.tears-of-ink.enter" : "message.tears-of-ink.leave");
			messageTicks = MESSAGE_TICKS;
		}
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
		message = null;
		messageTicks = 0;
	}

	public static void clientTick() {
		if (messageTicks > 0) messageTicks--;
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		long now = System.nanoTime();
		float dt = lastNanos == 0L ? 0f : Math.min((now - lastNanos) / 1_000_000_000f, 0.25f);
		lastNanos = now;

		alpha = inside ? Math.min(1f, alpha + dt * FADE_PER_SECOND) : Math.max(0f, alpha - dt * FADE_PER_SECOND);
		soulShown += (soul - soulShown) * Math.min(1f, dt * 10f);
		inkShown += (ink - inkShown) * Math.min(1f, dt * 10f);

		if (mc.options.hideGui || mc.player == null) return;
		if (mc.getDebugOverlay().showDebugScreen()) return;
		boolean survival = mc.gameMode != null && mc.gameMode.canHurtPlayer();

		int w = g.guiWidth();
		int h = g.guiHeight();

		RenderSystem.enableBlend();
		if (alpha > 0f && survival) {
			int y = h - BOTTOM_OFFSET - FRAME_H;
			int leftX = w / 2 - 91 - SIDE_GAP - FRAME_W;
			int rightX = w / 2 + 91 + SIDE_GAP;
			g.setColor(1f, 1f, 1f, alpha);

			// Душа: заполнение — текущая душа из 100; проданная часть — трещины сверху
			drawFrame(g, leftX, y);
			int lost = Math.round(FILL_H * (InkData.SOUL_START - soulMax) / (float) InkData.SOUL_START);
			int soulH = Math.round(FILL_H * Mth.clamp(soulShown / InkData.SOUL_START, 0f, 1f));
			drawFromBottom(g, SOUL_FILL, leftX, y, soulH);
			if (lost > 0) drawFromTop(g, SOUL_LOST, leftX, y, lost);

			// Чернила: заполнение — текущие из своего запаса
			drawFrame(g, rightX, y);
			int inkH = Math.round(FILL_H * Mth.clamp(inkShown / inkMax, 0f, 1f));
			drawFromBottom(g, INK_FILL, rightX, y, inkH);

			g.setColor(1f, 1f, 1f, 1f);
		}

		// Короткая надпись над хотбаром
		if (message != null && messageTicks > 0) {
			float a = Math.min(1f, messageTicks / (float) MESSAGE_FADE_TICKS);
			int color = ((int) (a * 255) << 24) | (inside ? 0xB79BD6 : 0xBBBBBB);
			if (a > 0.05f) {
				int textW = mc.font.width(message);
				g.drawString(mc.font, message, (w - textW) / 2, h - 72, color, true);
			}
		}
		RenderSystem.disableBlend();
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
