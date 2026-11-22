package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The battlefield takes priority over natural slopes, gullies and pale trees.
 * A composed scene receives an earthen, feathered local platform before the
 * original 3D scene/flag placement validator runs. Ground OUTSIDE those islands
 * remains full of trenches, craters and rough land.
 *
 * SAFETY: never touches a pre-battlefield chunk, a road, temple approach, fluid,
 * non-natural/player construction or a block entity. All candidate columns are
 * preflighted before the first block is changed. No force-loaded chunks.
 */
public final class BattlefieldSitePreparation {
    private BattlefieldSitePreparation() {}
    private record Column(int x, int z, int floor, int desired, int clearTop) {}

    private static boolean nativeSoil(BlockState s) {
        return s.is(ModBlocks.ASHEN_SOIL) || s.is(ModBlocks.ASHEN_PATH)
                || s.is(ModBlocks.ASH) || s.is(ModBlocks.ASPHODELITE_ROCK)
                || s.is(ModBlocks.PALE_ORE) || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(BlockTags.DIRT) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.TUFF) || s.is(Blocks.CLAY);
    }

    private static boolean natural(BlockState s) {
        if (s.hasBlockEntity() || !s.getFluidState().isEmpty()) return false;
        return s.isAir() || nativeSoil(s) || s.is(ModBlocks.BATTLEFIELD_FIELD_MARKER) || s.is(ModBlocks.PALE_LOG)
                || s.is(ModBlocks.PALE_WOOD) || s.is(ModBlocks.PALE_LEAVES)
                || s.is(ModBlocks.PALE_GRASS) || s.is(ModBlocks.TALL_PALE_GRASS)
                || s.is(ModBlocks.PALE_SAPLING) || s.is(ModBlocks.TEARBLOOM)
                || s.is(ModBlocks.INK_PUDDLE) || s.is(ModBlocks.DRYING_INK_PUDDLE)
                || s.is(ModBlocks.BLOODROOT) || s.is(ModBlocks.INKROOT)
                || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(Blocks.DEAD_BUSH) || s.is(BlockTags.FLOWERS)
                || s.canBeReplaced();
    }

    private static int nativeFloor(ServerLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= Math.max(level.getMinBuildHeight() + 1, top - 82); y--) {
            var s = level.getBlockState(pos.set(x, y, z));
            if (!natural(s)) return Integer.MIN_VALUE;
            if (nativeSoil(s)) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static long chunkKey(int x, int z) {
        return ((long)(x >> 4) << 32) | ((z >> 4) & 0xffffffffL);
    }

    /**
     * Returns false without any modifications when the site has a protected
     * feature, player structure, liquid, missing/old chunk or exceeds build
     * limits. The next tick may retry after neighboring chunks finish loading.
     */
    public static boolean prepare(ServerLevel level, BattlefieldWorldPlan.Field field,
                                  BattlefieldWorldPlan.Placement slot) {
        int cx = slot.x(), cz = slot.z();
        int inner = slot.kind() == BattlefieldWorldPlan.Kind.MONUMENT ? 10
                : slot.kind() == BattlefieldWorldPlan.Kind.TRACE ? 6 : 9;
        int outer = inner + 4;
        var layout = InkLandsLayout.of(level);
        if (layout == null) return false;

        Map<Long, Boolean> verified = new HashMap<>();
        List<Integer> heights = new ArrayList<>();
        for (int dx = -8; dx <= 8; dx += 4) for (int dz = -8; dz <= 8; dz += 4) {
            if (dx*dx + dz*dz > 11*11) continue;
            int px = cx+dx, pz = cz+dz;
            if (!level.hasChunkAt(new BlockPos(px, 0, pz))) return false;
            int h = nativeFloor(level, px, pz);
            if (h != Integer.MIN_VALUE) heights.add(h);
        }
        // Favor the higher shoulder around a ravine rather than dropping a
        // whole battalion into a hole, while keeping true non-flat outskirts.
        if (heights.size() < 10) return false;
        Collections.sort(heights);
        int target = heights.get(heights.size() * 3 / 4);
        if (target < level.getMinBuildHeight() + 3
                || target + 30 > level.getMaxBuildHeight()) return false;

        List<Column> updates = new ArrayList<>();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int dx = -outer; dx <= outer; dx++) for (int dz = -outer; dz <= outer; dz++) {
            double distance = Math.hypot(dx, dz);
            if (distance > outer) continue;
            int px = cx+dx, pz = cz+dz;
            var pos = new BlockPos(px, target, pz);
            if (!level.getWorldBorder().isWithinBounds(pos)
                    || level.getChunkSource().getChunkNow(px >> 4, pz >> 4) == null
                    || layout.roadNear(px, pz, slot.kind() == BattlefieldWorldPlan.Kind.TRACE ? 5 : 11) != null)
                return false;
            var temples = layout.templeNetwork();
            if (temples != null && temples.overlapsPath(px, pz, 7)) return false;

            long chunk = chunkKey(px, pz);
            if (!verified.computeIfAbsent(chunk,
                    key -> BattlefieldInstallations.stamped(level, field, px, pz))) return false;

            // Do not disturb the core of another episode, even if their fringes
            // nearly touch. Terrain remains natural between the close clusters.
            boolean otherCore = false;
            for (var other : field.placements()) if (other.id() != slot.id()) {
                int ox = px - other.x(), oz = pz - other.z();
                if (ox*ox + oz*oz < 6*6) { otherCore = true; break; }
            }
            if (otherCore) continue;

            int surface = nativeFloor(level, px, pz);
            if (surface == Integer.MIN_VALUE) return false;
            double blend = distance <= inner ? 1 :
                    Math.max(0, 1 - (distance - inner) / (outer - inner));
            blend = blend * blend * (3 - 2 * blend);
            int desired = (int) Math.round(surface + (target - surface) * blend);
            if (Math.abs(desired - surface) > 66
                    || desired < level.getMinBuildHeight() + 2
                    || desired + 30 > level.getMaxBuildHeight()) return false;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, px, pz) - 1;
            int clearTop = Math.max(top, desired + (slot.kind() == BattlefieldWorldPlan.Kind.MONUMENT ? 25 : 14));
            // If natural volume cannot be moved, do not half-build a platform.
            for (int y = Math.min(surface, desired); y <= clearTop; y++) {
                if (!natural(level.getBlockState(probe.set(px, y, pz)))) return false;
            }
            updates.add(new Column(px, pz, surface, desired, clearTop));
        }

        // PRE-FLIGHT COMPLETE. Ground, scars and trees are now explicitly
        // allowed to change only in this slot's already generated islands.
        for (Column column : updates) {
            int x = column.x(), z = column.z(), h = column.desired();
            for (int y = column.clearTop(); y > h; y--) {
                var pos = probe.set(x, y, z);
                if (!level.getBlockState(pos).isAir())
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            int low = Math.min(column.floor(), h);
            for (int y = low; y < h; y++) {
                var pos = probe.set(x, y, z);
                if (column.floor() < h || !nativeSoil(level.getBlockState(pos)))
                    level.setBlock(pos, ModBlocks.ASPHODELITE_ROCK.defaultBlockState(), 2);
            }
            // The scar is still part of the old landscape: retain sparse
            // exposed rock instead of painting each zone with one flat tile.
            boolean rough = (field.hash(x, z, 0x71AA) & 7) == 0;
            level.setBlock(probe.set(x, h, z), rough
                    ? ModBlocks.ASPHODELITE_ROCK.defaultBlockState()
                    : ModBlocks.ASHEN_SOIL.defaultBlockState(), 2);
        }
        // Restore durable provenance after cutting away an old high marker
        // column; it is essential for fog and idempotent retries after reload.
        int underY = target - 3;
        if (underY > level.getMinBuildHeight()) {
            var below = probe.set(cx, underY, cz);
            if (nativeSoil(level.getBlockState(below))) {
                level.setBlock(below, ModBlocks.BATTLEFIELD_FIELD_MARKER.defaultBlockState(), 2);
            }
        }
        return true;
    }
}
