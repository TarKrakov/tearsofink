package com.tarkrakov.tearsofink.client.mixin;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tarkrakov.tearsofink.client.fog.InkFog;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import com.tarkrakov.tearsofink.pantheon.PantheonArenaScenes;
import com.tarkrakov.tearsofink.world.ModBiomes;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * После того как игра настроила свой туман, сужаем его, если игрок в Чернильных пустошах.
 * Меняем только дальность тумана — цвет берётся из fog_color биома.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@Shadow private static float fogRed;
	@Shadow private static float fogGreen;
	@Shadow private static float fogBlue;

	/** Match the distant horizon to the warm twilight; preserve status-effect fog. */
	@Inject(method = "setupColor", at = @At("TAIL"))
	private static void tearsofink$pantheonHorizon(Camera camera, float partialTick, ClientLevel level,
			int viewDistance, float skyDarkness, CallbackInfo ci) {
		if (!PantheonTemple.isTemple(level)) return;
		if (camera.getEntity() instanceof LivingEntity living &&
				(living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;
		FogType medium = camera.getFluidInCamera();
		if (medium == FogType.WATER && PantheonArenaScenes.usesLakeColor(camera.getBlockPosition())) {
			var lake = level.registryAccess().registryOrThrow(Registries.BIOME).get(ModBiomes.TAR_LAKE);
			if (lake != null) {
				int color = lake.getWaterFogColor();
				fogRed = (color >> 16 & 255) / 255f;
				fogGreen = (color >> 8 & 255) / 255f;
				fogBlue = (color & 255) / 255f;
				RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0f);
			}
			return;
		}
		if (medium != FogType.NONE) return;
		float south = Math.max(0f, (float) Math.cos(Math.toRadians(camera.getYRot())));
		float glow = south * south;
		fogRed = Mth.lerp(glow, 0.38f, 0.68f);
		fogGreen = Mth.lerp(glow, 0.22f, 0.34f);
		fogBlue = Mth.lerp(glow, 0.27f, 0.19f);
		RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0f);
	}

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
