package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tarkrakov.tearsofink.world.ModSurfaceRules;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Ставим наши правила поверхности первыми — для любых регионов мира (TerraBlender/BOP/ваниль).
 * Оборачиваем метод целиком, чтобы сработать даже если другой мод подменяет правила.
 * Правила срабатывают только в биомах Чернильных Земель, остальной мир не трогают.
 */
@Mixin(NoiseGeneratorSettings.class)
public abstract class NoiseGeneratorSettingsMixin {
	@Unique
	private SurfaceRules.RuleSource tearsofink$base;
	@Unique
	private SurfaceRules.RuleSource tearsofink$combined;

	@WrapMethod(method = "surfaceRule")
	private SurfaceRules.RuleSource tearsofink$prependInkLands(Operation<SurfaceRules.RuleSource> original) {
		SurfaceRules.RuleSource base = original.call();
		if (base != tearsofink$base || tearsofink$combined == null) {
			tearsofink$base = base;
			tearsofink$combined = SurfaceRules.sequence(ModSurfaceRules.makeRules(), base);
		}
		return tearsofink$combined;
	}
}
