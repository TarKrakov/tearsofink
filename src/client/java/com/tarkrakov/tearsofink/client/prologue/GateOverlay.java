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
	private static int total, ticks = -1;

	private GateOverlay() {}

	public static void start(int duration) {
		total = Math.max(1, duration);
		ticks = 0;
	}

	public static void tick() {
		if (ticks >= 0 && ++ticks > total + FADE_OUT) ticks = -1;
	}

	public static void render(GuiGraphics g, float partialTick) {
		if (ticks < 0) return;
		float t = ticks + partialTick;
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
