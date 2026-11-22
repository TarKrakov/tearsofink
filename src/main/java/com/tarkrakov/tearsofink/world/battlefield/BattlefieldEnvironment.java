package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
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
        return s.is(ModBlocks.ASHEN_SOIL) || s.is(ModBlocks.ASH) || s.is(ModBlocks.ASPHODELITE_ROCK)
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

    private static boolean stampColumn(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                                       int x, int z, int begin) {
        for (int y = begin; y >= Math.max(level.getMinBuildHeight() + 2, begin - 90); y--) {
            BlockState under = level.getBlockState(pos.set(x, y, z));
            if ((under.is(ModBlocks.ASPHODELITE_ROCK)
                    || under.is(BlockTags.BASE_STONE_OVERWORLD))
                    && !under.hasBlockEntity() && under.getFluidState().isEmpty()) {
                level.setBlock(pos, ModBlocks.BATTLEFIELD_FIELD_MARKER.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                return true;
            }
        }
        return false;
    }

    /** Return true if a natural column was altered. Everything stays inside the given chunk. */
    public static boolean decorate(WorldGenLevel level, InkLandsLayout layout,
                                   BattlefieldWorldPlan.Field field,
                                   BlockPos.MutableBlockPos pos, int x, int z) {
        double fieldValue = field.openness(x, z);
        double approach = field.approach(x, z);
        // Every eligible new chunk gains a buried proof block, not just
        // the exact slot column (where deep ash could hide that proof).
        boolean atChunkProof = (x & 15) == 8 && (z & 15) == 8;
        boolean planned = atChunkProof && (fieldValue > .28 || approach > .35);
        if (field.x() == x && field.z() == z) planned = true;
        for (var slot : field.placements()) {
            int sx = x - slot.x(), sz = z - slot.z();
            // The sculptor may need land in a neighboring chunk beyond the
            // irregular field edge. Stamp ALL involved virgin chunks in
            // worldgen before any server-side landscape changes.
            if (atChunkProof && sx * sx + sz * sz < 25 * 25) planned = true;
            if (sx == 0 && sz == 0) planned = true;
            if (planned) break;
        }
        if (fieldValue < .10 && approach < .15 && !planned) return false;
        // Never decorate the road, its shoulders, temple approach or a building.
        if (layout.roadNear(x, z, 9) != null) return false;
        var network = layout.templeNetwork();
        if (network != null && network.overlapsPath(x, z, 3)) return false;
        int ground = floor(level, pos, x, z);
        // The battlefield also crosses natural river channels. Proof must be
        // recorded below the WATER at new chunk centers so those causeways can
        // be prepared later, even where no dry soil exists.
        if (ground == Integer.MIN_VALUE) return planned && stampColumn(level, pos, x, z,
                level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 4);
        if (ground > level.getMaxBuildHeight() - 42) return false;
        BlockState soil = level.getBlockState(pos.set(x, ground, z));
        int hash = field.hash(x, z, 0xED06);
        double wear = Math.max(fieldValue, approach * .58);
        boolean changed = false;
        // Battlefield land tells its OWN history: uneven blast craters,
        // trenches and broken rims, created during virgin chunk generation.
        int scar = field.scar(x, z);
        if (scar > 0 && (soil.is(ModBlocks.ASHEN_SOIL) || soil.is(ModBlocks.ASPHODELITE_ROCK)
                || soil.is(ModBlocks.ASH) || soil.is(ModBlocks.ASHEN_PATH))) {
            int bottom = Math.max(level.getMinBuildHeight() + 2, ground - scar);
            boolean nativeColumn = true;
            for (int y = bottom; y <= ground; y++) {
                if (!base(level.getBlockState(pos.set(x, y, z)))) {
                    nativeColumn = false; break;
                }
            }
            if (nativeColumn) {
                for (int y = ground; y > bottom; y--) {
                    level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                }
                level.setBlock(pos.set(x, bottom, z), ModBlocks.ASPHODELITE_ROCK.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                ground = bottom;
                soil = level.getBlockState(pos.set(x, ground, z));
                changed = true;
            }
        } else if (scar < 0 && level.getBlockState(pos.set(x, ground + 1, z)).isAir()) {
            level.setBlock(pos.set(x, ground + 1, z), ModBlocks.ASPHODELITE_ROCK.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            ground++;
            soil = level.getBlockState(pos.set(x, ground, z));
            changed = true;
        }
        // Material changes are small irregular scars on the existing surface,
        // not a new flat layer. Preserve the exact local natural height.
        if (soil.is(ModBlocks.ASHEN_SOIL) || soil.is(Blocks.GRASS_BLOCK)) {
            double sample = (hash >>> 1 & 0xFFFF) / 65535.0;
            if (sample < wear * .33) {
                BlockState replacement = ((hash >>> 18) & 7) == 0
                        ? ModBlocks.ASPHODELITE_ROCK.defaultBlockState()
                        : ModBlocks.ASHEN_PATH.defaultBlockState();
                level.setBlock(pos.set(x, ground, z), replacement, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
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
                level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                changed = true;
            } else if (open && tree(s)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                changed = true;
            }
        }
        // Stamp ONLY freshly generated sites, well below the loose falling ash.
        // Never excavate a void under an ash layer or write over buildings.
        if (planned && stampColumn(level, pos, x, z, ground - 4)) changed = true;
        return changed;
    }
}
