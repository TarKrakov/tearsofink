package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import terrablender.api.Regions;
import terrablender.api.TerraBlenderApi;

/** Точка входа TerraBlender: регистрируем наши биомы (чтобы они были в списке биомов мира). */
public class ModTerraBlender implements TerraBlenderApi {
	@Override
	public void onTerraBlenderInitialized() {
		Regions.register(new InkLandsRegion(TearsOfInk.id("ink_lands"), 1));
		// Правила поверхности подключаются в NoiseGeneratorSettingsMixin — для всех регионов мира.
	}
}
