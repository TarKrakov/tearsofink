package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.block.BattlefieldBannerBlock;
import com.tarkrakov.tearsofink.block.BattlefieldSceneBlock;
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

    static BlockState block(BlockPos pos, BlockState neutral, boolean[][] free, boolean flowering) {
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
