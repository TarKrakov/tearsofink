package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Remove ONLY generation fallout and unsupported native debris, never arbitrary loot. */
public final class BattlefieldGroundCleanup {
    private BattlefieldGroundCleanup() {}

    private static boolean nativeLoose(BlockState s) {
        return s.is(ModBlocks.ASHEN_SOIL) || s.is(ModBlocks.ASHEN_PATH)
                || s.is(ModBlocks.ASH) || s.is(ModBlocks.ASPHODELITE_ROCK);
    }
    private static boolean nativePlant(BlockState s) {
        return s.is(ModBlocks.PALE_GRASS) || s.is(ModBlocks.TALL_PALE_GRASS)
                || s.is(ModBlocks.TEARBLOOM) || s.is(ModBlocks.PALE_SAPLING)
                || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(Blocks.DEAD_BUSH) || s.is(ModBlocks.BLOODROOT)
                || s.is(ModBlocks.INKROOT);
    }
    private static boolean nativeDrop(ItemEntity item) {
        var s = item.getItem();
        return s.is(ModBlocks.PALE_GRASS.asItem()) || s.is(ModBlocks.TALL_PALE_GRASS.asItem())
                || s.is(ModBlocks.TEARBLOOM.asItem()) || s.is(ModBlocks.PALE_SAPLING.asItem())
                || s.is(ModBlocks.ASHEN_SOIL.asItem()) || s.is(ModBlocks.ASH.asItem())
                || s.is(ModBlocks.ASHEN_PATH.asItem()) || s.is(ModBlocks.ASPHODELITE_ROCK.asItem())
                || s.is(Blocks.SHORT_GRASS.asItem()) || s.is(Blocks.TALL_GRASS.asItem())
                || s.is(Blocks.DEAD_BUSH.asItem())
                || s.is(ModBlocks.BLOODROOT.asItem()) || s.is(ModBlocks.INKROOT.asItem());
    }

    /** One scan per loaded battlefield chunk, budgeted by the installer. */
    public static void cleanChunk(ServerLevel level, BattlefieldWorldPlan.Field field, int cx, int cz) {
        var chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) return;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = cx * 16 + dx, z = cz * 16 + dz;
            if (field.openness(x, z) < .34 && field.approach(x, z) < .40) continue;
            pruneSurface(level, chunk, x, z);
        }
        clearDroppedPlants(level, new AABB(cx * 16, level.getMinBuildHeight(), cz * 16,
                cx * 16 + 16, level.getMaxBuildHeight(), cz * 16 + 16));
    }

    /** Cleans the highest exposed strata, not underground cave floors. */
    private static void pruneSurface(ServerLevel level,
                                     net.minecraft.world.level.chunk.LevelChunk chunk, int x, int z) {
        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        for (int y = top; y >= Math.max(level.getMinBuildHeight() + 2, top - 8); y--) {
            BlockPos block = new BlockPos(x, y, z);
            var state = chunk.getBlockState(block);
            if (state.isAir()) continue;
            var below = chunk.getBlockState(block.below());
            if (nativePlant(state) && below.getCollisionShape(level, block.below()).isEmpty()) {
                level.setBlock(block, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            } else if (nativeLoose(state) && below.isAir() && !hasSideSupport(level, block)) {
                level.setBlock(block, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
    }

    private static boolean hasSideSupport(ServerLevel level, BlockPos pos) {
        for (var face : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var p = pos.relative(face);
            var c = level.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4);
            if (c == null) return true;
            if (!c.getBlockState(p).getCollisionShape(level, p).isEmpty()) return true;
        }
        return false;
    }

    /** Just-created plants and natural dirt drops; not tools, armor or resources. */
    public static void clearDroppedPlants(ServerLevel level, AABB area) {
        for (var item : level.getEntitiesOfClass(ItemEntity.class, area,
                i -> i.tickCount < 2400 && nativeDrop(i))) item.discard();
    }

    public static void clearAfterScene(ServerLevel level, BlockPos origin, int radius) {
        // The original chunk may have been cleaned BEFORE a later scene's
        // earthwork. Rescan only its local natural surface once after the scene.
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx*dx + dz*dz > radius*radius) continue;
            int x = origin.getX()+dx, z = origin.getZ()+dz;
            var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk != null) pruneSurface(level, chunk, x, z);
        }
        clearDroppedPlants(level, new AABB(origin.getX() - radius, origin.getY() - 28,
                origin.getZ() - radius, origin.getX() + radius + 1,
                origin.getY() + 45, origin.getZ() + radius + 1));
    }
}
