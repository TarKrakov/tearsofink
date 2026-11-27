package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.entity.patrol.ForestCamps;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary;
import com.tarkrakov.tearsofink.world.herald.BloodSettlements;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tarkrakov.tearsofink.entity.TrollTerritoryRules.*;

/** A fellable tree is a rooted regional trunk with attached, naturally decaying leaves. */
final class TrollTrees {
    private static final int STUMP_HEIGHT = 2, MAX_HEIGHT = 23, MAX_BRANCH = 12, MAX_LOGS = 140;

    private TrollTrees() {}

    static boolean regionalLog(BlockState state) {
        return state.is(ModBlocks.PALE_LOG) || state.is(ModBlocks.INK.log());
    }

    static BlockPos rootAt(ServerLevel level, int x, int z) {
        var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return null;
        int top = LoadedFaunaTerrain.surface(chunk, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top - 1; y >= Math.max(level.getMinBuildHeight() + 1, top - MAX_HEIGHT); y--) {
            pos.set(x, y, z);
            var state = chunk.getBlockState(pos);
            Block log = state.getBlock();
            if (!regionalLog(state)) continue;
            var ground = chunk.getBlockState(pos.below());
            if (!(ground.is(ModBlocks.ASHEN_SOIL) || ground.is(ModBlocks.TAR_SOIL))) continue;
            if (state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y
                    && chunk.getBlockState(pos.above()).is(log) && chunk.getBlockState(pos.above(2)).is(log)) {
                return pos.immutable();
            }
        }
        return null;
    }

    static void thinAround(ServerLevel level, BlockPos center, BlockPos entrance) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -22; dx <= 22; dx++) for (int dz = -22; dz <= 22; dz++) {
            int distance = dx * dx + dz * dz;
            if (distance > 22 * 22) continue;
            BlockPos column = center.offset(dx, 0, dz);
            int chance = distance <= 10 * 10 ? 85 : distance <= 16 * 16 ? 55 : 22;
            if (variation(level.getSeed(), column.asLong(), 100) >= chance) continue;
            BlockPos root = rootAt(level, column.getX(), column.getZ());
            if (root != null && Math.abs(root.getY() - center.getY()) <= 12
                    && distanceSq(root.getX(), root.getZ(), entrance.getX(), entrance.getZ()) >= 9) {
                candidates.add(root);
            }
        }
        candidates.sort(Comparator.comparingDouble(p -> distanceSq(p.getX(), p.getZ(), center.getX(), center.getZ())));
        int cut = 0;
        for (BlockPos root : candidates) {
            if (fell(level, center, root, false) && ++cut >= 12) break;
        }
    }

    static boolean fell(ServerLevel level, BlockPos center, BlockPos root, boolean audible) {
        List<BlockPos> logs = edibleCrown(level, center, root);
        if (logs.isEmpty()) return false;
        if (audible) level.levelEvent(2001, root.above(STUMP_HEIGHT), Block.getId(level.getBlockState(root)));
        // Remove only the wood. Leaves recalculate their support, keeping crowns
        // belonging to standing trees and dropping nothing from the removed logs.
        remove(level, logs);
        return true;
    }

    static void remove(ServerLevel level, Collection<BlockPos> logs) {
        logs.stream().sorted(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed())
                .forEach(pos -> level.removeBlock(pos, false));
    }

    static List<BlockPos> forConstruction(ServerLevel level, BlockPos center, BlockPos obstruction) {
        Block log = level.getBlockState(obstruction).getBlock();
        ArrayDeque<BlockPos> queued = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queued.add(obstruction);
        seen.add(obstruction);
        while (!queued.isEmpty() && seen.size() <= MAX_LOGS) {
            BlockPos pos = queued.removeFirst();
            BlockPos root = rootAt(level, pos.getX(), pos.getZ());
            if (pos.equals(root)) {
                if (!within(center.getX(), center.getZ(), root.getX(), root.getZ(), ROAM)) return List.of();
                List<BlockPos> trunk = crown(level, center, root);
                if (trunk.isEmpty()) return trunk;
                if (obstruction.equals(root) || obstruction.equals(root.above()) || trunk.contains(obstruction)) {
                    trunk.add(root);
                    trunk.add(root.above());
                    return trunk;
                }
                return List.of();
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                var chunk = LoadedFaunaTerrain.chunk(level, neighbor);
                if (chunk != null && chunk.getBlockState(neighbor).is(log) && seen.add(neighbor)) {
                    if (direction == Direction.DOWN) queued.addFirst(neighbor);
                    else queued.addLast(neighbor);
                }
            }
        }
        return List.of();
    }

    static boolean edible(ServerLevel level, BlockPos center, BlockPos root) {
        return !edibleCrown(level, center, root).isEmpty();
    }

    private static List<BlockPos> edibleCrown(ServerLevel level, BlockPos center, BlockPos root) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !within(center.getX(), center.getZ(), root.getX(), root.getZ(), ROAM)
                || !root.equals(rootAt(level, root.getX(), root.getZ()))) return List.of();
        return crown(level, center, root);
    }

    private static List<BlockPos> crown(ServerLevel level, BlockPos center, BlockPos root) {
        InkLandsLayout layout = InkLandsLayout.of(level);
        if (layout == null || ForestCamps.near(level, root, 30)) return List.of();
        Block log = level.getBlockState(root).getBlock();
        Block leaves = log == ModBlocks.PALE_LOG ? ModBlocks.PALE_LEAVES : ModBlocks.INK.leaves();
        BlockPos cut = root.above(STUMP_HEIGHT);
        ArrayDeque<BlockPos> queued = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> crown = new ArrayList<>();
        boolean foliage = false;
        queued.add(cut);
        seen.add(cut);

        while (!queued.isEmpty()) {
            BlockPos pos = queued.removeFirst();
            if (crown.size() >= MAX_LOGS || pos.getY() > root.getY() + MAX_HEIGHT
                    || !within(root.getX(), root.getZ(), pos.getX(), pos.getZ(), MAX_BRANCH)
                    || !within(center.getX(), center.getZ(), pos.getX(), pos.getZ(), DEFEND)
                    || !TrollCave.clearOfStructures(level, pos)
                    || layout.nearRoad(pos.getX(), pos.getZ(), 8) || Ruins.nearStart(layout, pos.getX(), pos.getZ())
                    || Sanctuary.near(layout, pos.getX(), pos.getZ(), 0)
                    || BattlefieldFaunaBoundary.inside(level, pos.getX(), pos.getZ())
                    || BloodSettlements.contains(level, pos)) return List.of();
            crown.add(pos);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                var chunk = LoadedFaunaTerrain.chunk(level, neighbor);
                if (chunk == null) return List.of();
                var state = chunk.getBlockState(neighbor);
                if (state.is(log)) {
                    if (neighbor.getY() < cut.getY()) {
                        // A second ground-reaching stem is a different tree.
                        if (!neighbor.equals(cut.below())) return List.of();
                    } else if (seen.add(neighbor)) {
                        queued.addLast(neighbor);
                    }
                } else if (state.is(leaves)) {
                    if (state.getValue(LeavesBlock.PERSISTENT)) return List.of();
                    foliage = true;
                } else if (state.is(BlockTags.LOGS) || state.hasBlockEntity()
                        || state.is(BlockTags.PLANKS) || state.is(BlockTags.FENCES) || state.is(BlockTags.WOODEN_SLABS)
                        || state.is(BlockTags.WOODEN_STAIRS)) {
                    return List.of();
                }
            }
        }
        return foliage && crown.size() >= 5 ? crown : List.of();
    }
}
