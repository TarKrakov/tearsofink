package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tarkrakov.tearsofink.world.ModSurfaceRules;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Ставим наши правила поверхности первыми — для любых регионов мира (TerraBlender/BOP/ваниль).
 * Правила срабатывают только в биомах Чернильных Земель, поэтому остальной мир не трогают.
 */
@Mixin(NoiseGeneratorSettings.class)
public abstract class NoiseGeneratorSettingsMixin {
	@Unique
	private SurfaceRules.RuleSource tearsofink$base;
	@Unique
	private SurfaceRules.RuleSource tearsofink$combined;

	@ModifyReturnValue(method = "surfaceRule", at = @At("RETURN"))
	private SurfaceRules.RuleSource tearsofink$prependInkLands(SurfaceRules.RuleSource original) {
		if (original != tearsofink$base || tearsofink$combined == null) {
			tearsofink$base = original;
			tearsofink$combined = SurfaceRules.sequence(ModSurfaceRules.makeRules(), original);
		}
		return tearsofink$combined;
	}
}
