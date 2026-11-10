package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;
import terrablender.api.TerraBlenderApi;

/** Точка входа TerraBlender: регистрируем наши биомы и правила поверхности. */
public class ModTerraBlender implements TerraBlenderApi {
	@Override
	public void onTerraBlenderInitialized() {
		Regions.register(new InkLandsRegion(TearsOfInk.id("ink_lands"), 1));
		SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, TearsOfInk.MOD_ID,
				ModSurfaceRules.makeRules());
	}
}
