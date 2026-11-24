package com.tarkrakov.tearsofink.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.fog.InkFog;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * В Чернильных пустошах подменяем текстуры солнца и луны на свои.
 * Файлы: assets/tears-of-ink/textures/environment/ink_sun.png и ink_moon_phases.png
 * Также убираем облака: в густом тумане они выглядят как тёмные заплатки на небе.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Unique
	private static final ResourceLocation TEARSOFINK$SUN = TearsOfInk.id("textures/environment/ink_sun.png");
	@Unique
	private static final ResourceLocation TEARSOFINK$MOON = TearsOfInk.id("textures/environment/ink_moon_phases.png");
	/** Насколько яркими остаются солнце и луна в тумане пустошей (1 = как в ванилле). */
	@Unique
	private static final float TEARSOFINK$SKY_BODY_BRIGHTNESS = 0.65f;

	@ModifyArg(
			method = "renderSky",
			at = @At(value = "INVOKE",
					target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"),
			index = 1)
	private ResourceLocation tearsofink$swapSunAndMoon(ResourceLocation texture) {
		if (!"minecraft".equals(texture.getNamespace())) return texture;
		float s = InkFog.strength();
		boolean sun = "textures/environment/sun.png".equals(texture.getPath());
		boolean moon = "textures/environment/moon_phases.png".equals(texture.getPath());
		if (!sun && !moon) return texture;

		var level = Minecraft.getInstance().level;
		if (level != null && PantheonTemple.isTemple(level)) {
			float alpha = RenderSystem.getShaderColor()[3];
			if (sun) RenderSystem.setShaderColor(1f, 0.66f, 0.29f, alpha);
			else RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
			return texture;
		}

		if (sun && s > 0f) {
			// Затухание при переходе: обычное солнце тает в дымке (s 0 → 0.5),
			// затем из дымки проступает наше (s 0.5 → 1). Луна рисуется следом с тем же цветом.
			float fade = s < 0.5f ? 1f - 2f * s : (2f * s - 1f) * TEARSOFINK$SKY_BODY_BRIGHTNESS;
			float[] c = RenderSystem.getShaderColor();
			RenderSystem.setShaderColor(c[0], c[1], c[2], c[3] * fade);
		}
		if (s < 0.5f) return texture;
		return sun ? TEARSOFINK$SUN : TEARSOFINK$MOON;
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void tearsofink$noCloudsInInkLands(CallbackInfo ci) {
		if (InkFog.strength() >= 0.5f) ci.cancel();
	}
}
