package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import static com.tarkrakov.tearsofink.entity.TrollTerritoryRules.overlapsProtectedColumn;

/** A rock shelter built once, when an unclaimed territory is first settled. */
final class TrollCave {
    static final int RADIUS = 14;
    private static final ResourceKey<LootTable> PALE_LOOT = loot("troll_cave_pale");
    private static final ResourceKey<LootTable> INK_LOOT = loot("troll_cave_inkwood");
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private TrollCave() {}

    private static ResourceKey<LootTable> loot(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, TearsOfInk.id("chests/" + path));
    }

    record Plan(BlockPos entrance, BlockPos center, List<BlockPos> beds, BlockPos chest, Direction outward,
                Map<BlockPos, BlockState> blocks, Set<BlockPos> treeClearing) {
        void build(ServerLevel level, boolean inkwood) {
            TrollTrees.remove(level, treeClearing);
            blocks.forEach((pos, state) -> level.setBlock(pos, state, FLAGS));
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, outward), FLAGS);
            if (level.getBlockEntity(chest) instanceof ChestBlockEntity storage) {
                storage.setLootTable(inkwood ? INK_LOOT : PALE_LOOT, level.random.nextLong());
            }
        }
    }

    static BlockPos ground(ServerLevel level, int x, int z) {
        var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return null;
        int top = LoadedFaunaTerrain.surface(chunk, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int y = top - 1; y >= Math.max(level.getMinBuildHeight(), top - 24); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = chunk.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return null;
            if (terrain(state)) return pos.above();
            if (!plants(state) && !state.isAir() && !TrollTrees.regionalLog(state)) return null;
        }
        return null;
    }

    static Plan plan(ServerLevel level, BlockPos mouth, Direction inward, int members, boolean inkwood) {
        return plan(level,mouth,inward,members,inkwood,reason->{});
    }

    private static Plan reject(Consumer<String> diagnostic,String reason){
        diagnostic.accept(reason);
        return null;
    }

    static Plan plan(ServerLevel level, BlockPos mouth, Direction inward, int members, boolean inkwood, Consumer<String> diagnostic) {
        AABB bounds = AABB.encapsulatingFullBlocks(at(mouth, inward, -6, -7), at(mouth, inward, 6, 12))
                .inflate(1, 0, 1).expandTowards(0, 8, 0).expandTowards(0, -10, 0);
        if (bounds.minY < level.getMinBuildHeight() || bounds.maxY >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(bounds) || !LoadedFaunaTerrain.loaded(level, bounds, 0)) {
            return reject(diagnostic,"unloaded_or_bounds");
        }

        BlockPos exit = at(mouth, inward, 0, -7);
        BlockPos outside = ground(level, exit.getX(), exit.getZ());
        if (outside == null || Math.abs(outside.getY() - mouth.getY()) > 5) return reject(diagnostic,"approach_slope");
        int rise = outside.getY() - mouth.getY();
        BlockPos center = at(mouth, inward, 0, 6);
        Set<BlockPos> treeClearing = new HashSet<>();
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (int depth = -6; depth <= 12; depth++) {
            int opening = switch (depth) {
                case -6, -5, -4, -3, -2, -1, 0 -> 2;
                case 1, 2, 3, 9, 10 -> 3;
                case 4, 5, 6, 7, 8 -> 4;
                default -> -1;
            };
            int edge = depth < 0 ? opening : switch (depth) {
                case 0, 12 -> 4;
                case 1, 2, 11 -> 5;
                default -> 6;
            };
            for (int side = -edge; side <= edge; side++) {
                int grade = depth < 0 ? Mth.floor(-depth * rise / 6f) : 0;
                BlockPos floor = at(mouth, inward, side, depth).above(grade);
                boolean open = Math.abs(side) <= opening;
                int skin = TrollTerritoryRules.variation(level.getSeed(), floor.asLong(), 13);
                // Tall steps leave clearance for the whole Troll hitbox while climbing.
                int ceiling = depth >= 1 && depth <= 8 ? 6 : 5;
                int top = depth < 0 ? 5 : open ? ceiling + 1 + skin % 2 : Math.abs(side) == edge ? 3 + skin % 3 : ceiling + 1;
                if (!clearOfStructures(level, floor)) return reject(diagnostic,"structure");
                if (LoadedFaunaTerrain.chunk(level, floor).getInhabitedTime() >= 20 * 60 * 20) return reject(diagnostic,"inhabited_chunk");
                if (!foundation(level, center, floor, blocks, treeClearing, rock(skin, inkwood))) return reject(diagnostic,"foundation");
                for (int y = 0; y < top; y++) {
                    BlockPos pos = floor.above(y);
                    BlockState old = source(level, pos, treeClearing);
                    boolean passage = open && (depth < 0 || y < ceiling);
                    if (!old.getFluidState().isEmpty() || old.hasBlockEntity()) return reject(diagnostic,"fluid_or_inventory");
                    if (passage) {
                        if (old.isAir()) continue;
                        if (!plants(old) && !terrain(old) && !clearTree(level, center, pos, old, treeClearing)) {
                            return reject(diagnostic,"passage_obstruction");
                        }
                        blocks.put(pos, Blocks.AIR.defaultBlockState());
                    } else if (old.isAir() || plants(old) || clearTree(level, center, pos, old, treeClearing)) {
                        blocks.put(pos, rock((skin + y * 5) % 13, inkwood));
                    } else if (!terrain(old)) {
                        return reject(diagnostic,"wall_obstruction");
                    }
                }
            }
        }
        // Recheck the far end of the approach against the untouched forest floor.
        for (int side = -1; side <= 1; side++) {
            BlockPos column = outside.relative(inward.getClockWise(), side);
            if (!naturalStep(level, column, treeClearing)) return reject(diagnostic,"approach_blocked");
        }
        // A creature elsewhere in the broad search AABB (e.g. a flying owl)
        // should not veto the whole cave. Never change blocks intersecting
        // a living entity or the blocks immediately supporting it.
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            AABB occupied = entity.getBoundingBox().expandTowards(0,-1,0);
            if (blocks.keySet().stream().anyMatch(pos->occupied.intersects(new AABB(pos)))
                    || treeClearing.stream().anyMatch(pos->occupied.intersects(new AABB(pos))))
                return reject(diagnostic,"entity_in_footprint");
        }

        List<BlockPos> beds = members == 1 ? List.of(center)
                : List.of(at(mouth, inward, -3, 6), at(mouth, inward, 3, 6));
        return new Plan(mouth, center, beds, at(mouth, inward, 0, 10), inward.getOpposite(), blocks, treeClearing);
    }

    private static BlockPos at(BlockPos mouth, Direction inward, int side, int depth) {
        int below = depth < 2 ? 0 : depth < 5 ? 1 : depth < 8 ? 2 : 3;
        return mouth.relative(inward, depth).relative(inward.getClockWise(), side).below(below);
    }

    static boolean clearOfStructures(ServerLevel level, BlockPos pos) {
        var chunk = LoadedFaunaTerrain.chunk(level, pos);
        if (chunk == null) return false;
        // A reference means a structure intersects SOME part of this chunk,
        // not that every column is occupied. Protect its actual bounding box
        // plus a margin, even underground; never force-load an unknown start.
        for (var entry : chunk.getAllReferences().entrySet()) {
            for (long reference : entry.getValue()) {
                var startChunk = level.getChunkSource().getChunkNow(ChunkPos.getX(reference), ChunkPos.getZ(reference));
                if (startChunk == null) return false;
                var start = startChunk.getStartForStructure(entry.getKey());
                if (start == null || !start.isValid()) continue;
                var box = start.getBoundingBox();
                if (overlapsProtectedColumn(pos.getX(),pos.getZ(),box.minX(),box.minZ(),box.maxX(),box.maxZ()))
                    return false;
            }
        }
        return true;
    }

    private static boolean naturalStep(ServerLevel level, BlockPos pos, Set<BlockPos> treeClearing) {
        for (int step = 1; step >= -1; step--) {
            BlockPos candidate = pos.above(step);
            if (terrain(level.getBlockState(candidate.below()))
                    && level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) {
                for (int y = 0; y < 4; y++) {
                    BlockPos air = candidate.above(y);
                    BlockState state = source(level, air, treeClearing);
                    if (!state.getFluidState().isEmpty() || (!plants(state) && !state.getCollisionShape(level, air).isEmpty())) return false;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean foundation(ServerLevel level, BlockPos center, BlockPos floor, Map<BlockPos, BlockState> blocks,
                                      Set<BlockPos> treeClearing, BlockState rock) {
        for (int depth = 1; depth <= 6; depth++) {
            BlockPos pos = floor.below(depth);
            BlockState old = source(level, pos, treeClearing);
            if (!old.getFluidState().isEmpty() || old.hasBlockEntity()) return false;
            if (terrain(old)) return old.isFaceSturdy(level, pos, Direction.UP);
            if (depth == 6 || (!old.isAir() && !plants(old) && !clearTree(level, center, pos, old, treeClearing))) return false;
            blocks.put(pos, rock);
        }
        return false;
    }

    private static BlockState source(ServerLevel level, BlockPos pos, Set<BlockPos> treeClearing) {
        return treeClearing.contains(pos) ? Blocks.AIR.defaultBlockState() : level.getBlockState(pos);
    }

    private static boolean clearTree(ServerLevel level, BlockPos center, BlockPos pos, BlockState state,
                                     Set<BlockPos> treeClearing) {
        if (!TrollTrees.regionalLog(state)) return false;
        var logs = TrollTrees.forConstruction(level, center, pos);
        treeClearing.addAll(logs);
        return !logs.isEmpty();
    }

    private static boolean plants(BlockState state) {
        if (state.is(BlockTags.LEAVES)) return !state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT);
        return state.canBeReplaced() || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN) || state.is(ModBlocks.DUCKWEED) || state.is(ModBlocks.INKROOT)
                || state.is(ModBlocks.TARWEED) || state.is(ModBlocks.WATERBLOOM) || state.is(ModBlocks.BOGLEAF)
                || state.is(ModBlocks.INK_PUDDLE) || state.is(ModBlocks.DRYING_INK_PUDDLE);
    }

    private static boolean terrain(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)
                || state.is(Blocks.TUFF) || state.is(Blocks.GRAVEL)
                || state.is(ModBlocks.ASPHODELITE_ROCK) || state.is(ModBlocks.ASH)
                || state.is(ModBlocks.ASHEN_SOIL) || state.is(ModBlocks.TAR_SOIL);
    }

    private static BlockState rock(int variation, boolean inkwood) {
        return switch (variation) {
            case 0, 3 -> Blocks.TUFF.defaultBlockState();
            case 2, 7 -> (inkwood ? Blocks.DEEPSLATE : Blocks.ANDESITE).defaultBlockState();
            case 5 -> (inkwood ? Blocks.COBBLED_DEEPSLATE : Blocks.COBBLESTONE).defaultBlockState();
            default -> ModBlocks.ASPHODELITE_ROCK.defaultBlockState();
        };
    }
}
