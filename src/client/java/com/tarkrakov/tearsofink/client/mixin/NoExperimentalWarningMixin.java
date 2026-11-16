package com.tarkrakov.tearsofink.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Убирает окно «Миры, использующие экспериментальные настройки, не поддерживаются». Его вызывает наше измерение
 * (своя генерация в данных мода) — для игры это безвредно. Только окно: сам мир и сохранение не меняются.
 * Цели сверены probe 16 ноя (WorldOpenFlows.openWorldCheckWorldStemCompatibility / confirmWorldCreation).
 */
@Mixin(WorldOpenFlows.class)
public abstract class NoExperimentalWarningMixin {
	/** Открытие мира: «настройки генерации экспериментальные?» — считаем стабильными. */
	@ModifyExpressionValue(method = "openWorldCheckWorldStemCompatibility", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/storage/WorldData;worldGenSettingsLifecycle()Lcom/mojang/serialization/Lifecycle;"))
	private Lifecycle tearsofink$stableOnOpen(Lifecycle original) {
		return Lifecycle.stable();
	}

	/** Создание мира: то же предупреждение. */
	@ModifyVariable(method = "confirmWorldCreation", at = @At("HEAD"), argsOnly = true)
	private static Lifecycle tearsofink$stableOnCreate(Lifecycle lifecycle) {
		return Lifecycle.stable();
	}
}
