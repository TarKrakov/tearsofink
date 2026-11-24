package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.fog.InkFog;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Гасим ванильное зарево заката/рассвета в Чернильных пустошах.
 * Это зарево окрашивало туман в красный, когда смотришь на солнце,
 * отсюда и был «пересвет» в одну сторону и темнота в другую.
 */
@Mixin(DimensionSpecialEffects.class)
public abstract class DimensionSpecialEffectsMixin {
	@Inject(method = "getSunriseColor", at = @At("RETURN"), cancellable = true)
	private void tearsofink$noSunriseGlow(float timeOfDay, float partialTicks, CallbackInfoReturnable<float[]> cir) {
		float[] color = cir.getReturnValue();
		var level = Minecraft.getInstance().level;
		if (level != null && PantheonTemple.isTemple(level)) {
			if (color != null) cir.setReturnValue(new float[]{1f, 0.45f, 0.16f, color[3]});
			return;
		}
		float s = InkFog.strength();
		if (color == null || s <= 0f) return;
		if (s >= 1f) {
			cir.setReturnValue(null);
			return;
		}
		// Ванилла возвращает общий массив — копируем, чтобы не испортить его для других.
		float[] faded = color.clone();
		faded[3] *= (1f - s);
		cir.setReturnValue(faded);
	}
}
