package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Chunk-local, late decoration on newly generated terrain ONLY. No neighboring
 * chunk access, no road edits, and no changes to authored buildings or old chunks.
 */
public final class BattlefieldEnvironment {
    private BattlefieldEnvironment() {}
    private static boolean base(BlockState s) {
        return s.is(ModBlocks.ASHEN_SOIL) || s.is(ModBlocks.ASPHODELITE_ROCK)
                || s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD);
    }
    private static boolean tree(BlockState s) {
        return s.is(ModBlocks.PALE_LOG) || s.is(ModBlocks.PALE_WOOD)
                || s.is(ModBlocks.PALE_LEAVES);
    }
    private static boolean plant(BlockState s) {
        return s.is(ModBlocks.PALE_GRASS) || s.is(ModBlocks.TALL_PALE_GRASS)
                || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(Blocks.DEAD_BUSH);
    }
    /** Only natural vegetation above a solid, native forest column is eligible. */
    private static int floor(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        int limit = Math.max(level.getMinBuildHeight(), surface - 52);
        for (int y = surface; y >= limit; y--) {
            BlockState s = level.getBlockState(pos.set(x, y, z));
            if (base(s) && s.getFluidState().isEmpty()) return y;
            if (!s.isAir() && !tree(s) && !plant(s)) return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    /** Return true if a natural column was altered. Everything stays inside the given chunk. */
    public static boolean decorate(WorldGenLevel level, InkLandsLayout layout,
                                   BattlefieldWorldPlan.Field field,
                                   BlockPos.MutableBlockPos pos, int x, int z) {
        double fieldValue = field.openness(x, z);
        double approach = field.approach(x, z);
        boolean planned = field.x() == x && field.z() == z;
        if (!planned) for (var slot : field.placements()) {
            if (slot.x() == x && slot.z() == z) { planned = true; break; }
        }
        if (fieldValue < .10 && approach < .15 && !planned) return false;
        // Never decorate the road, its shoulders, temple approach or a building.
        if (layout.roadNear(x, z, 9) != null) return false;
        var network = layout.templeNetwork();
        if (network != null && network.overlapsPath(x, z, 3)) return false;
        int ground = floor(level, pos, x, z);
        if (ground == Integer.MIN_VALUE || ground > level.getMaxBuildHeight() - 42) return false;
        BlockState soil = level.getBlockState(pos.set(x, ground, z));
        int hash = field.hash(x, z, 0xED06);
        double wear = Math.max(fieldValue, approach * .58);
        boolean changed = false;
        // Material changes are small irregular scars on the existing surface,
        // not a new flat layer. Preserve the exact local natural height.
        if (soil.is(ModBlocks.ASHEN_SOIL) || soil.is(Blocks.GRASS_BLOCK)) {
            double sample = (hash >>> 1 & 0xFFFF) / 65535.0;
            if (sample < wear * .33) {
                BlockState replacement = ((hash >>> 18) & 7) == 0
                        ? ModBlocks.ASPHODELITE_ROCK.defaultBlockState()
                        : ModBlocks.ASHEN_PATH.defaultBlockState();
                level.setBlock(pos.set(x, ground, z), replacement, 2);
                changed = true;
            }
        }
        // In the inner heart remove only natural pale trunks/canopy that happen
        // to intrude from trees whose roots were outside the no-tree mask.
        boolean open = fieldValue > .83;
        int end = Math.min(level.getMaxBuildHeight() - 1, ground + (open ? 42 : 3));
        for (int y = ground + 1; y <= end; y++) {
            BlockState s = level.getBlockState(pos.set(x, y, z));
            if (s.isAir()) continue;
            if (plant(s) && (open || (field.hash(x, z, y) >>> 1 & 255) < 255 * wear)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                changed = true;
            } else if (open && tree(s)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                changed = true;
            }
        }
        // An inert marker proves NEW worldgen occurred here. It is concealed
        // two blocks underground; old chunks can never acquire it retroactively.
        if (planned) {
            BlockPos mark = pos.set(x, ground - 2, z).immutable();
            BlockState under = level.getBlockState(mark);
            if (base(under) && !under.hasBlockEntity() && under.getFluidState().isEmpty()) {
                level.setBlock(mark, ModBlocks.BATTLEFIELD_FIELD_MARKER.defaultBlockState(), 2);
                changed = true;
            }
        }
        return changed;
    }
}
