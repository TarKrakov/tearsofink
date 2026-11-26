package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.block.BattlefieldBannerBlock;
import com.tarkrakov.tearsofink.block.BattlefieldSceneBlock;
import com.tarkrakov.tearsofink.block.BattlefieldRelicPatchBlock;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldRelicRecipes;
import net.minecraft.world.level.block.Blocks;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

import static com.tarkrakov.tearsofink.pantheon.PantheonArenaScenes.*;
import static com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Scenario.*;

/** Version one of the temple's compact, independent battlefield. Coordinates are schematic-local. */
final class OrionBattlefield {
    private static final Map<BlockPos, BlockState> DECOR = Map.ofEntries(
            Map.entry(at(37, 77), scene(REGULAR_SQUAD_LINE_ANCHOR, 2)),
            Map.entry(at(39, 83), scene(BROKEN_WALL, 4)),
            Map.entry(at(47, 101), scene(SPEAR_COLLAPSE, 1)),
            Map.entry(at(75, 92), scene(PALADIN_HOLDOUT_ANCHOR, 5)),
            Map.entry(at(81, 99), scene(PALADIN_MERCY_HOLDOUT, 0)),
            Map.entry(at(74, 68), scene(FINAL_TRACE, 2)),
            Map.entry(at(34, 80), banner(BattlefieldBannerBlock.Faction.REGULAR, Direction.EAST, 0)),
            Map.entry(at(43, 94), banner(BattlefieldBannerBlock.Faction.REGULAR, Direction.SOUTH, 4)),
            Map.entry(at(80, 94), banner(BattlefieldBannerBlock.Faction.SOUL, Direction.WEST, 0)));
    // Additional low, inert weapon beds and rare upright faction markers. Keep the centre
    // and the arrival axis open; the former inner colonnade is now part of the battlefield.
    private static final Map<BlockPos, BlockState> EXTRA = Map.ofEntries(
            Map.entry(at(37, 64), patch(0, 2, Direction.NORTH)),
            Map.entry(at(78, 64), patch(6, 1, Direction.SOUTH)),
            Map.entry(at(33, 88), patch(7, 3, Direction.EAST)),
            Map.entry(at(82, 83), patch(0, 1, Direction.WEST)),
            Map.entry(at(37, 111), patch(1, 3, Direction.SOUTH)),
            Map.entry(at(79, 109), patch(7, 2, Direction.NORTH)),
            Map.entry(at(59, 116), patch(0, 0, Direction.WEST)),
            Map.entry(at(46, 92), scene(BLADE_FAN, 3)),
            Map.entry(at(69, 103), scene(INK_HUNTER_SWEEP, 4)),
            Map.entry(at(69, 61), banner(BattlefieldBannerBlock.Faction.HUNTER, Direction.WEST, 0)),
            Map.entry(at(47, 115), banner(BattlefieldBannerBlock.Faction.REGULAR, Direction.NORTH, 0)),
            Map.entry(at(86, 102), banner(BattlefieldBannerBlock.Faction.SOUL, Direction.WEST, 0)));
    private static final int[][] BLOOM = {
            {33, 66, 4}, {42, 71, 3}, {48, 82, 4}, {38, 92, 5}, {45, 105, 5},
            {58, 109, 5}, {65, 96, 4}, {73, 103, 6}, {82, 84, 5}, {69, 77, 3}, {79, 72, 4}
    };

    private OrionBattlefield() {}

    private static BlockPos at(int x, int z) { return new BlockPos(x, FLOOR + 1, z); }

    private static BlockState scene(BattlefieldPosedScenes.Scenario scenario, int variant) {
        // Layout 2 holds the flat authored weapon poses. Flags are three explicit anchors.
        return ModBlocks.BATTLEFIELD_SCENE.defaultBlockState()
                .setValue(BattlefieldSceneBlock.SCENE, scenario.ordinal())
                .setValue(BattlefieldSceneBlock.VARIANT, variant)
                .setValue(BattlefieldSceneBlock.LAYOUT, 2);
    }

    private static BlockState banner(BattlefieldBannerBlock.Faction faction, Direction direction, int lean) {
        return ModBlocks.BATTLEFIELD_BANNER.defaultBlockState()
                .setValue(BattlefieldBannerBlock.FACTION, faction)
                .setValue(BattlefieldBannerBlock.LEAN, lean)
                .setValue(HorizontalDirectionalBlock.FACING, direction);
    }

    static int[] contacts(BlockState state) {
        var scenario = BattlefieldPosedScenes.Scenario.values()[state.getValue(BattlefieldSceneBlock.SCENE)];
        return new int[BattlefieldPosedScenes.plan(scenario, state.getValue(BattlefieldSceneBlock.VARIANT), 2).size()];
    }

    private static BlockState patch(int style, int variant, Direction direction) {
        return ModBlocks.BATTLEFIELD_RELIC_PATCH.defaultBlockState()
                .setValue(BattlefieldRelicPatchBlock.STYLE, style)
                .setValue(BattlefieldRelicPatchBlock.VARIANT, variant)
                .setValue(HorizontalDirectionalBlock.FACING, direction);
    }

    static int[] patchContacts(BlockState state, BlockPos world) {
        return new int[BattlefieldRelicRecipes.generate(world, state.getValue(BattlefieldRelicPatchBlock.STYLE),
                state.getValue(BattlefieldRelicPatchBlock.VARIANT)).size()];
    }

    // These are the measured inner rows of the original 115x144 Axiom scheme.
    // The rows at X=21/95 and Z=49/123 are the perimeter and never removed.
    private static final int[] INNER_X = {27,34,40,47,53,63,69,76,82,89};
    private static final int[] SIDE_Z = {69,80,92,103};
    static boolean innerColumnPart(BlockPos pos, BlockState state, PantheonTemple.Template original) {
        if (pos.getY() < 2 || pos.getY() > 11) return false;
        if (!state.is(ModBlocks.PANTHEON_COLUMN_SHAFT) && !state.is(ModBlocks.PANTHEON_COLUMN_CAPITAL)
                && !state.is(ModBlocks.INK_LANTERN)) return false;
        if (!state.equals(PantheonTemple.authored(original, pos))) return false;
        int x = pos.getX(), z = pos.getZ();
        for (int cx : INNER_X)
            if (Math.abs(x-cx) <= 2 && (Math.abs(z-60) <= 2 || Math.abs(z-112) <= 2)) return true;
        for (int cz : SIDE_Z)
            if (Math.abs(z-cz) <= 2 && (Math.abs(x-27) <= 2 || Math.abs(x-89) <= 2)) return true;
        return false;
    }

    private static boolean footprint(boolean[][] free, int x, int z, int radius) {
        if (x-radius < MIN_X || x+radius > MAX_X || z-radius < MIN_Z || z+radius > MAX_Z) return false;
        for (int i=-radius;i<=radius;i++) for(int j=-radius;j<=radius;j++)
            if (!free[x+i-MIN_X][z+j-MIN_Z]) return false;
        return true;
    }

    static BlockState block(BlockPos pos, BlockState neutral, boolean[][] free, boolean flowering) {
        int x=pos.getX(), z=pos.getZ(), dx=x-58, dz=z-86;
        int grain=Math.floorMod(x*x*13+z*z*17+x*z*31,101);
        // A single continuous field over the open interior, not an isolated central ellipse.
        if (pos.getY()==FLOOR) {
            if (Math.abs(dx)<=2 && dz < -8 || grain<12 && Math.abs(dx)>9)
                return ModBlocks.ASHEN_PATH.defaultBlockState();
            if (grain>96 && Math.abs(dx)>14) return ModBlocks.ASPHODELITE_ROCK.defaultBlockState();
            return ModBlocks.ASHEN_SOIL.defaultBlockState();
        }
        if (pos.getY()==FLOOR+1 && neutral.isAir()) {
            BlockState extra=EXTRA.get(pos);
            if (extra!=null) {
                int radius=extra.is(ModBlocks.BATTLEFIELD_RELIC_PATCH)
                        ? (extra.getValue(BattlefieldRelicPatchBlock.STYLE)>=5?8:10):4;
                if (footprint(free,x,z,radius)) return extra;
            }
            if (flowering) for (BlockPos anchor:EXTRA.keySet())
                if (anchor.distSqr(pos)<11 && !DECOR.containsKey(pos)) return neutral;
        }
        return legacyBlock(pos,neutral,free,flowering);
    }

    /** Original V1 layout used solely to recognize saved scenes during migration. */
    static BlockState legacyBlock(BlockPos pos, BlockState neutral, boolean[][] free, boolean flowering) {
        int x = pos.getX(), z = pos.getZ();
        int dx = x - 58, dz = z - 86;
        int grain = Math.floorMod(x * x * 13 + z * z * 17 + x * z * 31, 101);
        boolean field = dx * dx + dz * dz < 1010 + grain * 3;
        if (pos.getY() == FLOOR && field) {
            if (Math.abs(dx) <= 2 && dz < -8 || grain < 12 && Math.abs(dx) > 9)
                return ModBlocks.ASHEN_PATH.defaultBlockState();
            if (grain > 96 && Math.abs(dx) > 14)
                return ModBlocks.ASPHODELITE_ROCK.defaultBlockState();
            return ModBlocks.ASHEN_SOIL.defaultBlockState();
        }
        if (pos.getY() != FLOOR + 1 || !neutral.isAir()) return neutral;
        BlockState prop = DECOR.get(pos);
        if (prop != null) {
            // Each reused composition occupies a small flat footprint. Keep the entire
            // footprint clear of authored architecture, even for edited baselines.
            for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++)
                if (!free[x + i - MIN_X][z + j - MIN_Z]) return neutral;
            return prop;
        }
        if (flowering && field && grain < 45 && (Math.abs(dx) > 3 || dz > 8)) {
            for (BlockPos anchor : DECOR.keySet())
                if (anchor.distSqr(pos) < 11) return neutral;
            for (int[] group : BLOOM) {
                int bx = x - group[0], bz = z - group[1];
                if (bx * bx + bz * bz <= group[2] * group[2])
                    return ModBlocks.TEARBLOOM.defaultBlockState();
            }
        }
        return neutral;
    }
}
