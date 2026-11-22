package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Linked, scarred natural tracks across a shattered battlefield. Some fields
 * straddle a deep river gorge or a high plateau: no author scene should become
 * an unreachable island. This builder supports stone slopes and narrow
 * causeways across NATURAL chasms, in bounded 12-step chunks.
 *
 * Route parts are never force-loaded, only executed on confirmed battlefield
 * chunks, and all columns are preflighted before any world edit. No existing
 * roads, temples, scene blocks, other non-natural structures or player builds
 * are touched.
 */
public final class BattlefieldAccessRoutes {
    private BattlefieldAccessRoutes() {}
    private static final int STEP = 12;
    private record Point(int x, int z) {}
    private record Column(int x, int z, int naturalY, int targetY, int topY, boolean gentle) {}

    // Road entrance -> front -> center -> right, left, upper ledge and hill.
    // Positions are expressed in field-relative forward/right coordinates.
    private static final int[][] LINKS = {
            {-74, 0, -37, 0},
            {-37, 0,   0, 0},
            {  0, 0,  43, 0},
            {  0, 0, -33, 38},
            {  0, 0, -16,-44},
            {  0, 0,  39,-35},
            { 43, 0,  44, 44},
            {-33,38, -52,-23}
    };

    /** A deterministic quiet corridor for real foot traffic, not more sword beds. */
    public static boolean nearCorridor(BattlefieldWorldPlan.Field field, int x, int z, int margin) {
        for (int[] c : LINKS) {
            Point a = point(field, c[0], c[1]);
            Point b = point(field, c[2], c[3]);
            double dx = b.x - a.x, dz = b.z - a.z;
            double d = dx*dx + dz*dz;
            double t = d < 1 ? 0 : Math.max(0, Math.min(1,
                    ((x - a.x)*dx + (z - a.z)*dz) / d));
            double px = a.x + t*dx, pz = a.z + t*dz;
            if ((x-px)*(x-px) + (z-pz)*(z-pz) <= margin * margin) return true;
        }
        return false;
    }

    public static int routes() { return LINKS.length; }

    public static int parts(int index) {
        int[] v = LINKS[index];
        return (int)Math.ceil(Math.hypot(v[2]-v[0],v[3]-v[1]) / STEP);
    }

    private static Point point(BattlefieldWorldPlan.Field field, double forward, double right) {
        int x = field.x() + (int)Math.round(field.inwardX() * forward - field.inwardZ() * right);
        int z = field.z() + (int)Math.round(field.inwardZ() * forward + field.inwardX() * right);
        return new Point(x,z);
    }

    private static boolean earth(BlockState s) {
        return s.is(ModBlocks.ASHEN_SOIL) || s.is(ModBlocks.ASHEN_PATH)
                || s.is(ModBlocks.ASPHODELITE_ROCK) || s.is(ModBlocks.ASH)
                || s.is(ModBlocks.PALE_ORE) || s.is(ModBlocks.ASPHODELITE_ORE)
                || s.is(ModBlocks.DEEPSLATE_ASPHODELITE_ORE)
                || s.is(ModBlocks.ASPHODELITE_COAL_ORE)
                || s.is(ModBlocks.ASPHODELITE_IRON_ORE)
                || s.is(ModBlocks.ASPHODELITE_COPPER_ORE)
                || s.is(ModBlocks.ASPHODELITE_GOLD_ORE)
                || s.is(ModBlocks.ASPHODELITE_REDSTONE_ORE)
                || s.is(ModBlocks.ASPHODELITE_LAPIS_ORE)
                || s.is(ModBlocks.ASPHODELITE_DIAMOND_ORE)
                || s.is(ModBlocks.ASPHODELITE_EMERALD_ORE)
                || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(BlockTags.DIRT) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY);
    }
    private static boolean natural(BlockState s) {
        if (s.hasBlockEntity()) return false;
        if (!s.getFluidState().isEmpty() && !s.is(Blocks.WATER)) return false;
        return earth(s) || s.is(Blocks.WATER) || s.isAir()
                || s.is(ModBlocks.BATTLEFIELD_FIELD_MARKER)
                || s.is(ModBlocks.PALE_LOG) || s.is(ModBlocks.PALE_WOOD)
                || s.is(ModBlocks.PALE_LEAVES) || s.is(ModBlocks.PALE_GRASS)
                || s.is(ModBlocks.TALL_PALE_GRASS) || s.is(ModBlocks.TEARBLOOM)
                || s.is(ModBlocks.PALE_SAPLING) || s.is(Blocks.SHORT_GRASS)
                || s.is(Blocks.TALL_GRASS) || s.is(Blocks.DEAD_BUSH)
                || s.is(BlockTags.FLOWERS) || s.canBeReplaced();
    }

    /** Natural bedrock floor, not a water surface or treetop. */
    private static int floor(ServerLevel level, int x, int z) {
        var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return Integer.MIN_VALUE;
        int y = Math.min(level.getMaxBuildHeight() - 1,
                chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15));
        var pos = new BlockPos.MutableBlockPos();
        for (int n = 0; n <= 80 && y - n > level.getMinBuildHeight()+1; n++) {
            var s = chunk.getBlockState(pos.set(x, y - n, z));
            if (!natural(s)) return Integer.MIN_VALUE;
            if (earth(s)) return y - n;
        }
        return Integer.MIN_VALUE;
    }

    /** Every link is split into serial, retryable short sections. */
    public static boolean prepare(ServerLevel level, BattlefieldWorldPlan.Field field,
                                  int link, int section) {
        if (link < 0 || link >= LINKS.length || section < 0 || section >= parts(link)) return false;
        var layout = InkLandsLayout.of(level);
        if (layout == null || !BattlefieldInstallations.fieldReady(level, field)) return false;

        int[] coords = LINKS[link];
        Point from = point(field, coords[0], coords[1]);
        Point to = point(field, coords[2], coords[3]);
        int y0 = floor(level, from.x(), from.z()), y1 = floor(level, to.x(), to.z());
        if (y0 == Integer.MIN_VALUE || y1 == Integer.MIN_VALUE) return false;

        double length = Math.hypot(to.x() - from.x(), to.z() - from.z());
        int steps = (int)Math.ceil(length);
        int start = section * STEP, stop = Math.min(steps, (section + 1) * STEP);
        List<Column> edit = new ArrayList<>();
        Map<Long, Boolean> provenChunks = new HashMap<>();
        var pos = new BlockPos.MutableBlockPos();
        for (int i = start; i <= stop; i++) {
            double t = i / (double)steps;
            // A lightly wandering, one-block-at-a-time path rather than a
            // perfectly straight road. The centerline and endpoints remain
            // connected across section boundaries.
            double bend = 1.8 * Math.sin(t * Math.PI) * Math.sin(t * 12 + (field.seed() & 31));
            double ax = from.x() + (to.x() - from.x()) * t;
            double az = from.z() + (to.z() - from.z()) * t;
            double inv = 1d / Math.max(1d,length);
            int cx = (int)Math.round(ax - (to.z()-from.z())*inv*bend);
            int cz = (int)Math.round(az + (to.x()-from.x())*inv*bend);
            // The elevation follows a continuous natural-earth ramp. Deep
            // river crossings are filled; high ledges are carved, not skipped.
            double eased = t * t * (3d - 2d * t);
            int target = (int)Math.round(y0 + (y1 - y0) * eased);
            for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) {
                if (ox*ox + oz*oz > 5) continue;
                int x = cx + ox, z = cz + oz;
                BlockPos anchor = new BlockPos(x, target, z);
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null
                        || !level.getWorldBorder().isWithinBounds(anchor)) return false;
                // Earthworks never reshape ground beneath a visiting player.
                // Retry once they have walked further into the battlefield.
                for (var player : level.players()) {
                    if (Math.abs(player.getX() - x) < 12
                            && Math.abs(player.getZ() - z) < 12) return false;
                }
                if (layout.roadNear(x, z, 8) != null) return false;
                var temples = layout.templeNetwork();
                if (temples != null && temples.overlapsPath(x, z, 7)) return false;
                // Never cut through flags, giant swords or posed relic scenes.
                boolean closeToArt = false;
                for (var slot : field.placements()) {
                    int dx = x - slot.x(), dz = z - slot.z();
                    int clearance = slot.kind() == BattlefieldWorldPlan.Kind.MONUMENT ? 12
                            : slot.kind() == BattlefieldWorldPlan.Kind.RELIC ? 5 : 6;
                    if (dx*dx + dz*dz < clearance*clearance) {
                        closeToArt = true; break;
                    }
                }
                if (closeToArt) continue;
                long chunkKey = net.minecraft.world.level.ChunkPos.asLong(x >> 4, z >> 4);
                if (!provenChunks.computeIfAbsent(chunkKey,
                        key -> BattlefieldInstallations.stamped(level, field,
                                (x & ~15) + 8, (z & ~15) + 8))) return false;
                int naturalY = floor(level, x, z);
                if (naturalY == Integer.MIN_VALUE
                        || Math.abs(naturalY - target) > 66
                        || target < level.getMinBuildHeight()+2
                        || target + 10 >= level.getMaxBuildHeight()) return false;
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)-1;
                int topY = Math.max(target+8, surfaceY);
                // An existing supported bank already within one block of the
                // guided grade needs only a trampled breadcrumb; no flattening.
                boolean gentle = Math.abs(target-naturalY) <= 1
                        && level.getBlockState(pos.set(x, naturalY+1, z)).isAir();
                int scanLow = Math.min(naturalY, target);
                for (int y = scanLow; y <= topY; y++) {
                    if (!natural(level.getBlockState(pos.set(x, y, z)))) return false;
                }
                edit.add(new Column(x,z,naturalY,target,topY,gentle));
            }
        }
        // All safety checks succeeded. Writes are bounded to the section.
        for (Column column : edit) {
            int x = column.x, z = column.z, y = column.targetY;
            if (!column.gentle) {
                for (int high = column.topY; high > y; high--) {
                    var p = pos.set(x, high, z);
                    if (!level.getBlockState(p).isAir())
                        level.setBlock(p, Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                }
                for (int low = column.naturalY; low < y; low++) {
                    var p = pos.set(x, low, z);
                    level.setBlock(p, ModBlocks.ASPHODELITE_ROCK.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
            // Eroded ground, never a bright new highway; normal earth remains
            // at about 1/4 of the points.
            int h = field.hash(x, z, link * 33);
            if ((h & 3) != 0) {
                level.setBlock(pos.set(x, column.gentle ? column.naturalY : y, z),
                        (h & 15) == 0 ? ModBlocks.ASPHODELITE_ROCK.defaultBlockState()
                                : ModBlocks.ASHEN_PATH.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return true;
    }
}
