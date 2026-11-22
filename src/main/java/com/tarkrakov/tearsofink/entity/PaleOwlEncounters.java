package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.tarkrakov.tearsofink.entity.PaleOwlTuning.*;

/**
 * D-073: fill gaps in the resident owl population, including already explored forest.
 * Caterpillars need not disappear to free a vanilla CREATURE slot.
 * This is a bounded, local supplement, not a second unrestricted natural spawn cycle.
 */
final class PaleOwlEncounters {
    private PaleOwlEncounters() {}

    static void init() {
        ServerTickEvents.END_WORLD_TICK.register(PaleOwlEncounters::tick);
    }

    static void recordPlayerKill(ServerLevel level, BlockPos pos) {
        PaleOwlRecovery.of(level).record(level, pos);
    }

    private static void tick(ServerLevel level) {
        if (!ModDimensions.isLands(level) || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        if (level.getGameTime() % ENCOUNTER_INTERVAL == 0) PaleOwlRecovery.of(level).prune(level.getGameTime());
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()
                    || Math.floorMod(level.getGameTime() + player.getUUID().hashCode(), ENCOUNTER_INTERVAL) != 0) continue;
            // Share the D-058 search budget; read only FULL, already loaded chunks.
            if (!LoadedFaunaTerrain.search(level)) continue;
            if (!paleAt(level, player.blockPosition())
                    || count(level, player.position(), ENCOUNTER_NEAR_RADIUS) >= ENCOUNTER_TARGET
                    || count(level, player.position(), ENCOUNTER_CAP_RADIUS) >= ENCOUNTER_CAP) continue;
            attempt(level, player);
        }
    }

    private static boolean paleAt(ServerLevel level, BlockPos pos) {
        return LoadedFaunaTerrain.loaded(level, new AABB(pos), 2)
                && level.getBiome(pos).is(ModBiomes.PALE_FOREST)
                && !com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary.inside(
                        level, pos.getX() + .5d, pos.getZ() + .5d);
    }

    private static int count(ServerLevel level, Vec3 point, int radius) {
        return level.getEntitiesOfClass(PaleOwlEntity.class, new AABB(point, point).inflate(radius, 64, radius),
                owl -> owl.isAlive() && owl.position().subtract(point).horizontalDistanceSqr() <= radius * radius).size();
    }

    private static void attempt(ServerLevel level, ServerPlayer player) {
        for (int i = 0; i < ENCOUNTER_COLUMNS; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = ENCOUNTER_MIN_DISTANCE + level.random.nextDouble()
                    * (ENCOUNTER_MAX_DISTANCE - ENCOUNTER_MIN_DISTANCE);
            int x = Mth.floor(player.getX() + radius * Math.cos(angle));
            int z = Mth.floor(player.getZ() + radius * Math.sin(angle));
            Vec3 at = site(level, player, x, z);
            if (at == null || !canAdd(level, player, at)) continue;
            PaleOwlEntity owl = ModMobs.PALE_OWL.create(level);
            if (owl == null) return;
            owl.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
            owl.finalizeSpawn(level, level.getCurrentDifficultyAt(owl.blockPosition()), MobSpawnType.NATURAL, null);
            if (level.addFreshEntity(owl)) return; // At most one addition per scheduled player check.
        }
    }

    private static boolean canAdd(ServerLevel level, ServerPlayer player, Vec3 at) {
        if (count(level, player.position(), ENCOUNTER_NEAR_RADIUS) >= ENCOUNTER_TARGET
                || count(level, player.position(), ENCOUNTER_CAP_RADIUS) >= ENCOUNTER_CAP
                || count(level, at, ENCOUNTER_CAP_RADIUS) >= ENCOUNTER_CAP
                || Sanctuary.inside(level, at.x, at.z)
                || PaleOwlRecovery.of(level).blocked(at, level.getGameTime())) return false;
        for (ServerPlayer other : level.players()) {
            if (!other.isAlive() || other.isSpectator()) continue;
            double distance = other.position().subtract(at).horizontalDistanceSqr();
            if (distance < ENCOUNTER_MIN_DISTANCE * ENCOUNTER_MIN_DISTANCE
                    || (other != player && distance <= ENCOUNTER_CAP_RADIUS * ENCOUNTER_CAP_RADIUS
                    && count(level, other.position(), ENCOUNTER_CAP_RADIUS) >= ENCOUNTER_CAP)) return false;
        }
        return true;
    }

    private static Vec3 site(ServerLevel level, ServerPlayer player, int x, int z) {
        BlockPos column = new BlockPos(x, player.getBlockY(), z);
        var chunk = LoadedFaunaTerrain.chunk(level, column);
        if (chunk == null) return null;
        int top = LoadedFaunaTerrain.surface(chunk, Heightmap.Types.MOTION_BLOCKING, x, z);
        int low = Math.max(level.getMinBuildHeight() + 1,
                Math.max(top - 12, player.getBlockY() - 3));
        int high = Math.min(level.getMaxBuildHeight() - 3, Math.min(top, player.getBlockY() + 16));
        // Prefer reachable lower branches over the highest tips: owls remain arboreal.
        for (int y = low; y <= high; y++) {
            BlockPos foot = column.atY(y);
            var support = chunk.getBlockState(foot.below());
            if (!support.is(ModBlocks.PALE_LEAVES) && !support.is(ModBlocks.PALE_LOG)
                    && !support.is(ModBlocks.PALE_WOOD)) continue;
            Vec3 point = Vec3.atBottomCenterOf(foot);
            AABB body = new AABB(point.x - .375, y, point.z - .375, point.x + .375, y + .85, point.z + .375);
            AABB wings = body.move(0, .85, 0).inflate(PERCH_WING_CLEARANCE, 0, PERCH_WING_CLEARANCE);
            if (!paleAt(level, foot) || !LoadedFaunaTerrain.free(level, null, body)
                    || !LoadedFaunaTerrain.free(level, null, wings) || !level.noCollision(body)
                    || !level.getFluidState(foot).isEmpty() || !support.getFluidState().isEmpty()
                    || (!support.is(ModBlocks.PALE_LEAVES)
                    && !support.isFaceSturdy(level, foot.below(), Direction.UP))) continue;
            if (!level.getEntitiesOfClass(PaleOwlEntity.class, body.inflate(3), PaleOwlEntity::isAlive).isEmpty()) continue;
            return point;
        }
        return null;
    }
}
