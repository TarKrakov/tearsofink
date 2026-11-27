package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.entity.patrol.ForestCamps;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.LivingEntity;

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

    // One bounded pass per cave, once all nearby chunks are already loaded.
    static boolean thinAround(ServerLevel level, BlockPos center, BlockPos entrance) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !LoadedFaunaTerrain.loaded(level, new AABB(center).inflate(CLEARING_RADIUS,0,CLEARING_RADIUS),2)) return false;
        InkLandsLayout layout=InkLandsLayout.of(level);
        if(layout==null)return false;
        List<BlockPos> candidates = new ArrayList<>();
        List<BlockPos> stumps = new ArrayList<>();
        for (int dx = -CLEARING_RADIUS; dx <= CLEARING_RADIUS; dx++) for (int dz = -CLEARING_RADIUS; dz <= CLEARING_RADIUS; dz++) {
            int distance = dx * dx + dz * dz;
            if (distance > CLEARING_RADIUS * CLEARING_RADIUS) continue;
            BlockPos column = center.offset(dx, 0, dz);
            BlockPos root = rootAt(level, column.getX(), column.getZ());
            if (root != null && Math.abs(root.getY() - center.getY()) <= 12
                    && distanceSq(root.getX(), root.getZ(), entrance.getX(), entrance.getZ()) >= 9
                    && variation(level.getSeed(), column.asLong(), 100)<clearingChance(distance)) {
                candidates.add(root);
            } else if (root == null) {
                BlockPos stump = existingStump(level,column.getX(),column.getZ());
                if(stump!=null&&Math.abs(stump.getY()-center.getY())<=12)stumps.add(stump);
            }
        }
        candidates.sort(Comparator.comparingDouble(p -> distanceSq(p.getX(), p.getZ(), center.getX(), center.getZ())));
        int cut = 0;
        for (BlockPos root : candidates) {
            if(!freshColumn(level,root))continue;
            List<BlockPos> logs=edibleCrown(level,center,root);
            if(logs.isEmpty()||logs.stream().anyMatch(pos->!freshColumn(level,pos)))continue;
            remove(level,logs);
            stumps.add(root);
            if (++cut >= CLEARING_MAX_CUT) break;
        }
        // Where natural trees are sparse or connected crowns cannot safely
        // be cut, place a few regional two-block stumps on bare forest soil.
        // Keep the cave footprint and its entire approach free of new stumps.
        List<BlockPos> columns=new ArrayList<>();
        for(int dx=-CLEARING_RADIUS;dx<=CLEARING_RADIUS;dx++)for(int dz=-CLEARING_RADIUS;dz<=CLEARING_RADIUS;dz++){
            int d=dx*dx+dz*dz;
            if(d>=14*14&&d<=CLEARING_RADIUS*CLEARING_RADIUS)columns.add(center.offset(dx,0,dz));
        }
        columns.sort(Comparator.comparingInt(p->variation(level.getSeed(),p.asLong(),Integer.MAX_VALUE)));
        int added=0;
        for(BlockPos column:columns){
            if(stumps.size()>=CLEARING_TARGET_STUMPS||added>=CLEARING_MAX_ADDED)break;
            if(stumps.stream().anyMatch(p->distanceSq(p.getX(),p.getZ(),column.getX(),column.getZ())<4*4))continue;
            BlockPos feet=TrollCave.ground(level,column.getX(),column.getZ());
            if(feet==null||Math.abs(feet.getY()-center.getY())>8||!freshColumn(level,feet)
                    ||!safeStump(level,layout,feet))continue;
            boolean treeNearby=false;
            for(int dx=-2;dx<=2&&!treeNearby;dx++)for(int dz=-2;dz<=2;dz++){
                if(rootAt(level,feet.getX()+dx,feet.getZ()+dz)!=null){treeNearby=true;break;}
            }
            if(treeNearby)continue;
            Block log=level.getBiome(feet).is(ModBiomes.INKWOOD)?ModBlocks.INK.log():ModBlocks.PALE_LOG;
            BlockState state=log.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Y);
            level.setBlock(feet,state,Block.UPDATE_ALL);
            level.setBlock(feet.above(),state,Block.UPDATE_ALL);
            stumps.add(feet);added++;
        }
        return true;
    }

    private static boolean freshColumn(ServerLevel level,BlockPos pos){
        var chunk=LoadedFaunaTerrain.chunk(level,pos);
        return chunk!=null&&chunk.getInhabitedTime()<20*60*20;
    }

    private static BlockPos existingStump(ServerLevel level,int x,int z){
        var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);
        if(chunk==null)return null;
        int top=LoadedFaunaTerrain.surface(chunk,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        for(int y=top-1;y>=Math.max(level.getMinBuildHeight()+1,top-6);y--){
            BlockPos p=new BlockPos(x,y,z);
            BlockState s=chunk.getBlockState(p),ground=chunk.getBlockState(p.below());
            if(regionalLog(s)&&s.getValue(RotatedPillarBlock.AXIS)==Direction.Axis.Y
                    &&(ground.is(ModBlocks.ASHEN_SOIL)||ground.is(ModBlocks.TAR_SOIL))
                    &&chunk.getBlockState(p.above()).is(s.getBlock())
                    &&!regionalLog(chunk.getBlockState(p.above(2))))return p;
        }
        return null;
    }

    private static boolean safeStump(ServerLevel level,InkLandsLayout layout,BlockPos p){
        int x=p.getX(),z=p.getZ();
        if(!TrollCave.clearOfStructures(level,p)||layout.nearRoad(x,z,8)
                ||Ruins.nearStart(layout,x,z)||Sanctuary.near(layout,x,z,8)
                ||BattlefieldFaunaBoundary.inside(level,x,z)||BloodSettlements.contains(level,p)
                ||ForestCamps.near(level,p,30)||layout.nearHerald(x,z,40)
                ||!(level.getBiome(p).is(ModBiomes.PALE_FOREST)||level.getBiome(p).is(ModBiomes.INKWOOD)))return false;
        BlockState ground=level.getBlockState(p.below());
        if(!(ground.is(ModBlocks.ASHEN_SOIL)||ground.is(ModBlocks.TAR_SOIL)))return false;
        for(int y=0;y<6;y++)if(!level.getBlockState(p.above(y)).isAir())return false;
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(p).expandTowards(0,2,0)).isEmpty();
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
