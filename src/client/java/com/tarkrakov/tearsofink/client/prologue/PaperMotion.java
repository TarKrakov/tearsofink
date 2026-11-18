package com.tarkrakov.tearsofink.client.prologue;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Клиентские анимации авторских слоёв в координатах исходного PNG. */
final class PaperMotion {
	private PaperMotion() {}

	/** Непрерывное покачивание на реальном времени + затухающая инерция при открытии. */
	static float sway(double seconds, double period, double amplitude, double phase, double impulse) {
		double t = Math.max(0.0, seconds);
		double omega = 2.0 * Math.PI / period;
		double idle = amplitude * Math.sin(omega * t + phase) * (1.0 - Math.exp(-t * 2.5));
		double settling = impulse * Math.exp(-t * 1.9) * Math.sin(omega * 1.6 * t);
		return (float) (idle + settling);
	}

	/** Pivot задаётся в координатах полного листа, а не относительно обрезанного слоя. */
	static void layer(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h,
			float pivotX, float pivotY, float degrees) {
		g.pose().pushPose();
		g.pose().translate(pivotX, pivotY, 0);
		g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
		g.pose().translate(-pivotX, -pivotY, 0);
		g.blit(texture, x, y, 0, 0, w, h, w, h);
		g.pose().popPose();
	}
}
