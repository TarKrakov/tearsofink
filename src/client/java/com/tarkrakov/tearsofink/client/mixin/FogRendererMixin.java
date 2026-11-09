package com.tarkrakov.tearsofink.client.mixin;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tarkrakov.tearsofink.client.fog.InkFog;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * После того как игра настроила свой туман, сужаем его, если игрок в Чернильных пустошах.
 * Меняем только дальность тумана — цвет берётся из fog_color биома.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@Inject(method = "setupFog", at = @At("TAIL"))
	private static void tearsofink$inkFog(Camera camera, FogRenderer.FogMode fogMode, float renderDistance,
										  boolean thickFog, float partialTick, CallbackInfo ci) {
		if (fogMode == FogRenderer.FogMode.FOG_TERRAIN) {
			InkFog.tick(camera);
		}
		if (!InkFog.shouldApply(camera)) return;

		float s = InkFog.strength();
		float vanillaStart = RenderSystem.getShaderFogStart();
		float vanillaEnd = RenderSystem.getShaderFogEnd();
		float end = InkFog.fogEnd(vanillaEnd, renderDistance);
		float start = Mth.lerp(s, vanillaStart, Math.min(InkFog.FOG_START, end * 0.5f));

		RenderSystem.setShaderFogStart(Math.min(start, end));
		RenderSystem.setShaderFogEnd(end);
		if (s > 0.5f) {
			RenderSystem.setShaderFogShape(FogShape.SPHERE);
		}
	}
}
