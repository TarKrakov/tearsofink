package com.tarkrakov.tearsofink.client.prologue;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Переход сквозь ворота Преддверия: экран затягивает бирюзовой пеленой (текстура ворот на весь экран,
 * как у портала Незера), после переноса — плавно рассеивается.
 */
public final class GateOverlay {
	/** Сколько тиков рассеивается после переноса. */
	private static final int FADE_OUT = 25;
	/** Дольше этого пелену не держим, даже если мир не прогрузился (тики). */
	private static final int MAX_HOLD = 300;
	private static int total, ticks = -1, held;
	private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> from;

	private GateOverlay() {}

	public static void start(int duration) {
		total = Math.max(1, duration);
		ticks = 0;
		held = 0;
		var level = Minecraft.getInstance().level;
		from = level == null ? null : level.dimension();
	}

	public static void tick() {
		if (ticks < 0) return;
		// пелена держится, пока не перенесло и пока вокруг не прогрузилась земля — чтобы не стоять в пустоте
		if (ticks == total && !arrived() && ++held < MAX_HOLD) return;
		if (++ticks > total + FADE_OUT) ticks = -1;
	}

	private static boolean arrived() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.level.dimension() == from) return false;
		int cx = mc.player.getBlockX() >> 4, cz = mc.player.getBlockZ() >> 4;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (!mc.level.getChunkSource().hasChunk(cx + dx, cz + dz)) return false;
			}
		}
		return mc.levelRenderer.hasRenderedAllSections();
	}

	public static void render(GuiGraphics g, float partialTick) {
		if (ticks < 0) return;
		float t = ticks == total ? total : ticks + partialTick;
		float alpha = t < total ? (t / total) : 1f - (t - total) / FADE_OUT;
		alpha = Math.max(0f, Math.min(1f, alpha));
		if (alpha <= 0f) return;
		alpha = alpha * alpha * 0.95f;
		TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
				.getParticleIcon(ModBlocks.VESTIBULE_GATE.defaultBlockState());
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		g.setColor(1f, 1f, 1f, alpha);
		g.blit(0, 0, -90, g.guiWidth(), g.guiHeight(), sprite);
		g.setColor(1f, 1f, 1f, 1f);
		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}
}
