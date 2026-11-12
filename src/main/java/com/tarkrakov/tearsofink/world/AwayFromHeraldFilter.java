package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Фильтр размещения: не ставить (дерево и т.п.) ближе {@code margin} блоков к обочине магистрали.
 * Так кроны и ветки не нависают над дорогой. В JSON: {"type": "tears-of-ink:away_from_herald", "margin": 6}.
 */
public class AwayFromHeraldFilter extends PlacementFilter {
	public static final MapCodec<AwayFromHeraldFilter> CODEC = Codec.INT.fieldOf("radius")
			.xmap(AwayFromHeraldFilter::new, f -> f.radius);
	public static final PlacementModifierType<AwayFromHeraldFilter> TYPE = Registry.register(
			BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, TearsOfInk.id("away_from_herald"), () -> CODEC);

	private final int radius;

	public AwayFromHeraldFilter(int radius) {
		this.radius = radius;
	}

	@Override
	protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
		if (!(context.generator().getBiomeSource() instanceof InkLandsAccess access)) return true;
		InkLandsLayout layout = access.tearsofink$layout(context.getLevel().getLevel().getChunkSource().randomState().sampler());
		return layout == null || !layout.nearHerald(pos.getX(), pos.getZ(), radius);
	}

	@Override
	public PlacementModifierType<?> type() {
		return TYPE;
	}

	/** Вызывается при запуске, чтобы тип зарегистрировался. */
	public static void init() {
	}
}
