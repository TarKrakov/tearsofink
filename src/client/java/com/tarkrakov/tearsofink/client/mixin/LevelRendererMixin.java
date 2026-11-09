package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.fog.InkFog;
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

	@ModifyArg(
			method = "renderSky",
			at = @At(value = "INVOKE",
					target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"),
			index = 1)
	private ResourceLocation tearsofink$swapSunAndMoon(ResourceLocation texture) {
		if (InkFog.strength() < 0.5f || !"minecraft".equals(texture.getNamespace())) return texture;
		return switch (texture.getPath()) {
			case "textures/environment/sun.png" -> TEARSOFINK$SUN;
			case "textures/environment/moon_phases.png" -> TEARSOFINK$MOON;
			default -> texture;
		};
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void tearsofink$noCloudsInInkWastes(CallbackInfo ci) {
		if (InkFog.strength() >= 0.5f) ci.cancel();
	}
}
