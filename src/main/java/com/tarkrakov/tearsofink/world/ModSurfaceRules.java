package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.SurfaceRules;

/** Из каких блоков состоит поверхность наших биомов. */
public final class ModSurfaceRules {
	private static final SurfaceRules.RuleSource ASHEN_SOIL = SurfaceRules.state(ModBlocks.ASHEN_SOIL.defaultBlockState());
	private static final SurfaceRules.RuleSource ASH = SurfaceRules.state(ModBlocks.ASH.defaultBlockState());
	/** Под «потолками» (над пещерами) — туф, чтобы пепел не осыпался в пещеры. */
	private static final SurfaceRules.RuleSource ASPHODELITE_ROCK = SurfaceRules.state(ModBlocks.ASPHODELITE_ROCK.defaultBlockState());
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

		// Вся толща камня под пустошами выше y≈0 — асфоделитовая порода
		// (ниже, как и в ванилле, начинается глубинный сланец — с плавным переходом 0..8).
		SurfaceRules.RuleSource rock = SurfaceRules.ifTrue(
				SurfaceRules.not(SurfaceRules.verticalGradient(TearsOfInk.MOD_ID + ":asphodelite_rock",
						VerticalAnchor.absolute(0), VerticalAnchor.absolute(8))),
				ASPHODELITE_ROCK);

		return SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.INK_WASTES), SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), inkWastes),
				rock));
	}
}
