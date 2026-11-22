package com.tarkrakov.tearsofink.world.battlefield;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.InkLandsAccess;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * The continuous landscape is reserved BEFORE its trees spawn, rather than
 * destroying them after chunk completion. The old roadside tree filter is kept.
 */
public final class BattlefieldTreeFilter extends PlacementFilter {
    public static final MapCodec<BattlefieldTreeFilter> CODEC = Codec.INT.fieldOf("margin")
            .xmap(BattlefieldTreeFilter::new, f -> f.margin);
    public static final PlacementModifierType<BattlefieldTreeFilter> TYPE = Registry.register(
            BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, TearsOfInk.id("away_from_battlefield"), () -> CODEC);
    private final int margin;
    public BattlefieldTreeFilter(int margin) { this.margin = margin; }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!(context.generator().getBiomeSource() instanceof InkLandsAccess access)) return true;
        InkLandsLayout layout = access.tearsofink$layout(
                context.getLevel().getLevel().getChunkSource().randomState().sampler());
        BattlefieldWorldPlan plan = BattlefieldWorldPlan.of(layout);
        if (plan == null) return true;
        for (var f : plan.fields()) {
            if (f.distance(pos.getX(), pos.getZ()) > BattlefieldWorldPlan.FIELD_RADIUS + 25 + margin
                    && f.approach(pos.getX(), pos.getZ()) < .15) continue;
            double pressure = Math.max(f.openness(pos.getX(), pos.getZ()),
                    f.approach(pos.getX(), pos.getZ()) * .76);
            // No trees in the heart. Patchy natural transition, no identical circle.
            if (pressure >= .98) return false;
            if (((f.hash(pos.getX(), pos.getZ(), 0xBEEF) >>> 1) & 0xffff) / 65535.0 < pressure * .96)
                return false;
        }
        return true;
    }

    @Override public PlacementModifierType<?> type() { return TYPE; }
    public static void init() {}
}
