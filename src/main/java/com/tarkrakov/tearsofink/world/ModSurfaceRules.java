package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

/** Из каких блоков состоит поверхность наших биомов. */
public final class ModSurfaceRules {
	private static final SurfaceRules.RuleSource ASHEN_SOIL = SurfaceRules.state(ModBlocks.ASHEN_SOIL.defaultBlockState());
	private static final SurfaceRules.RuleSource ASH = SurfaceRules.state(ModBlocks.ASH.defaultBlockState());
	/** Под «потолками» (над пещерами) — туф, чтобы пепел не осыпался в пещеры. */
	private static final SurfaceRules.RuleSource TUFF = SurfaceRules.state(Blocks.TUFF.defaultBlockState());

	private ModSurfaceRules() {}

	public static SurfaceRules.RuleSource makeRules() {
		SurfaceRules.ConditionSource aboveWater = SurfaceRules.waterBlockCheck(-1, 0);

		SurfaceRules.RuleSource inkWastes = SurfaceRules.sequence(
				// Самый верхний блок: пепельная земля (под водой — просто пепел)
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(aboveWater, ASHEN_SOIL),
						ASH)),
				// Несколько блоков под ним: пепел
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, TUFF),
						ASH))
		);

		return SurfaceRules.ifTrue(
				SurfaceRules.abovePreliminarySurface(),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.INK_WASTES), inkWastes));
	}
}
