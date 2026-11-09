package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;
import terrablender.api.TerraBlenderApi;

/** Точка входа TerraBlender: встраиваем наши биомы в обычный мир. */
public class ModTerraBlender implements TerraBlenderApi {
	/** Частота появления Чернильных пустошей. Ванильный регион имеет вес 10. */
	public static final int INK_WASTES_WEIGHT = 3;

	@Override
	public void onTerraBlenderInitialized() {
		Regions.register(new InkWastesRegion(TearsOfInk.id("overworld"), INK_WASTES_WEIGHT));
		SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, TearsOfInk.MOD_ID,
				ModSurfaceRules.makeRules());
	}
}
