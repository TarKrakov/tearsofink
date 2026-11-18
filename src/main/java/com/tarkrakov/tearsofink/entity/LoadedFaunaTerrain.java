package com.tarkrakov.tearsofink.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.WeakHashMap;

/** Queries for ambient fauna must never request a chunk or promote its status. */
final class LoadedFaunaTerrain {
    private static final int SEARCH_BUDGET = 12;
    private static final WeakHashMap<ServerLevel, Budget> BUDGETS = new WeakHashMap<>();

    private LoadedFaunaTerrain() {}

    static boolean search(ServerLevel level) {
        Budget budget = BUDGETS.computeIfAbsent(level, ignored -> new Budget());
        if (budget.tick != level.getGameTime()) {
            budget.tick = level.getGameTime();
            budget.remaining = SEARCH_BUDGET;
        }
        if (budget.remaining == 0) return false;
        --budget.remaining;
        return true;
    }

    private static final class Budget {
        long tick = Long.MIN_VALUE;
        int remaining;
    }

    static LevelChunk chunk(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
    }

    static int surface(LevelChunk chunk, Heightmap.Types type, int x, int z) {
        return chunk.getHeight(type, x & 15, z & 15) + 1;
    }

    static boolean loaded(ServerLevel level, AABB box, double margin) {
        for (int x = Mth.floor(box.minX - margin) >> 4; x <= Mth.floor(box.maxX + margin) >> 4; x++) {
            for (int z = Mth.floor(box.minZ - margin) >> 4; z <= Mth.floor(box.maxZ + margin) >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            }
        }
        return true;
    }

    static boolean free(ServerLevel level, Entity mob, AABB box) {
        return box.minY >= level.getMinBuildHeight() && box.maxY < level.getMaxBuildHeight()
                && loaded(level, box, 2) && level.getWorldBorder().isWithinBounds(box)
                && level.noBlockCollision(mob, box);
    }
}
