package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Nearby ambient encounters, independent of the caterpillar/animal spawn cap. */
public final class PaleStriderEncounters {
    private static final int INTERVAL = 300, LOCAL_RADIUS = 96, TEMPLE_RADIUS = 68;
    private PaleStriderEncounters() {}

    public static void init() {
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof PaleStriderEntity strider) strider.stopSong(level);
        });
        ServerTickEvents.END_WORLD_TICK.register(PaleStriderEncounters::tick);
    }

    private static void tick(ServerLevel level) {
        if (!ModDimensions.isLands(level) || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        InkLandsLayout layout = null;
        for (ServerPlayer player : level.players()) {
            if (Math.floorMod(level.getGameTime() + player.getUUID().hashCode(), INTERVAL) != 0
                    || !player.isAlive() || player.isSpectator()
                    || !paleAt(level, player.blockPosition()) || !LoadedFaunaTerrain.search(level)) continue;
            if (layout == null) layout = InkLandsLayout.of(level);
            if (layout == null) return;
            attempt(level, layout, player);
        }
    }

    static boolean paleAt(ServerLevel level, BlockPos pos) {
        return LoadedFaunaTerrain.loaded(level, new AABB(pos), 4) && level.getBiome(pos).is(ModBiomes.PALE_FOREST);
    }

    private static int count(ServerLevel level, Vec3 center) {
        return level.getEntitiesOfClass(PaleStriderEntity.class, new AABB(center, center).inflate(LOCAL_RADIUS, 64, LOCAL_RADIUS),
                mob -> mob.isAlive() &&
                        mob.position().subtract(center).horizontalDistanceSqr() <= LOCAL_RADIUS * LOCAL_RADIUS).size();
    }

    private static void attempt(ServerLevel level, InkLandsLayout layout, ServerPlayer player) {
        int[] temple = null;
        double best = TEMPLE_RADIUS * TEMPLE_RADIUS;
        for (int[] p : Ruins.templePoints(layout)) {
            double dx = p[0] - player.getX(), dz = p[1] - player.getZ();
            double distance = dx * dx + dz * dz;
            if (distance < best) { best = distance; temple = p; }
        }
        boolean templeChoir = temple != null;
        int nearby = count(level, player.position());
        if (nearby >= (templeChoir ? 3 : 2)) return;
        int roll = level.random.nextInt(10);
        int size = templeChoir ? 3 : roll < 6 ? 1 : roll < 9 ? 2 : 3;
        if (nearby != 0 && !templeChoir) size = 1;
        int cap = templeChoir || size == 3 ? 3 : 2;

        Vec3 start = null;
        for (int i = 0; i < 24; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = (templeChoir ? 32 : 35) + level.random.nextDouble() * (templeChoir ? 12 : 30);
            int x = (int)Math.floor((templeChoir ? temple[0] : player.getX()) + Math.cos(angle) * radius);
            int z = (int)Math.floor((templeChoir ? temple[1] : player.getZ()) + Math.sin(angle) * radius);
            Vec3 candidate = site(level, layout, player, x, z);
            if (candidate == null) continue;
            if (start == null) start = candidate;
            Vec3 eye = player.getEyePosition();
            Vec3 body = candidate.add(0, 4, 0);
            if (LoadedFaunaTerrain.loaded(level, new AABB(eye, body), 2)
                    && level.clip(new ClipContext(eye, body, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() == HitResult.Type.BLOCK) {
                start = candidate;
                break;
            }
        }
        if (start == null) return;
        if (size == 3 && !templeChoir && count(level, start) != 0) {
            size = 2;
            cap = 2;
        }
        UUID group = size > 1 ? UUID.randomUUID() : null;
        float heading = level.random.nextFloat() * 360;
        if (!spawn(level, player, start, templeChoir, group, heading, cap)) return;
        for (int i = 0, placed = 1; i < 20 && placed < size; i++) {
            int dx = level.random.nextInt(17) - 8, dz = level.random.nextInt(17) - 8;
            int distance = dx * dx + dz * dz;
            if (distance < 9 || distance > 64) continue;
            Vec3 next = site(level, layout, player, Mth.floor(start.x) + dx, Mth.floor(start.z) + dz);
            if (next != null && Math.abs(next.y - start.y) <= 3 &&
                    spawn(level, player, next, templeChoir, group, heading, cap)) placed++;
        }
    }


    private static Vec3 site(ServerLevel level, InkLandsLayout layout, ServerPlayer player, int x, int z) {
        double distance = Math.hypot(x + .5 - player.getX(), z + .5 - player.getZ());
        if (distance < 35 || distance > 65 || layout.depthInside(x, z) < 0 ||
                Sanctuary.near(layout, x, z, 4) || layout.nearRoad(x, z, 3)) return null;
        for (ServerPlayer other : level.players())
            if (!other.isSpectator() && Math.hypot(other.getX() - x - .5, other.getZ() - z - .5) < 28) return null;
        var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return null;
        int y = LoadedFaunaTerrain.surface(chunk, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos foot = new BlockPos(x, y, z);
        Vec3 point = Vec3.atBottomCenterOf(foot);
        AABB volume = new AABB(x - .25, y, z - .25, x + 1.25, y + 7.75, z + 1.25);
        if (!paleAt(level, foot) || !LoadedFaunaTerrain.free(level, null, volume) || !level.noCollision(volume)
                || !chunk.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP)) return null;
        for (BlockPos at : BlockPos.betweenClosed(BlockPos.containing(volume.minX, volume.minY, volume.minZ),
                BlockPos.containing(volume.maxX, volume.maxY, volume.maxZ)))
            if (!level.getFluidState(at).isEmpty()) return null;
        return point;
    }

    private static boolean spawn(ServerLevel level, ServerPlayer player, Vec3 at, boolean temple, UUID group, float heading, int cap) {
        if (count(level, at) >= cap || count(level, player.position()) >= cap) return false;
        for (var other : level.players()) {
            if (other == player || !other.isAlive() || other.isSpectator()
                    || other.position().subtract(at).horizontalDistanceSqr() > LOCAL_RADIUS * LOCAL_RADIUS) continue;
            if (count(level, other.position()) >= cap) return false;
        }
        if (!level.getEntitiesOfClass(PaleStriderEntity.class, new AABB(at, at).inflate(2.8, 5, 2.8), PaleStriderEntity::isAlive)
                .isEmpty()) return false;
        PaleStriderEntity mob = ModMobs.PALE_STRIDER.create(level);
        if (mob == null) return false;
        mob.moveTo(at.x, at.y, at.z, heading, 0);
        mob.configureEncounter(temple, group, heading);
        return level.addFreshEntity(mob);
    }
}
