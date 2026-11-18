package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Nearby ambient encounters, independent of the caterpillar/animal spawn cap. */
public final class PaleStriderEncounters {
    private PaleStriderEncounters() {}
    private static final int INTERVAL = 300, LOCAL_RADIUS = 96, TEMPLE_RADIUS = 68;
    private static final int SOLO_CAP = 2, CHOIR_CAP = 3;
    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(PaleStriderEncounters::tick);
    }

    private static void tick(ServerLevel level) {
        if (!ModDimensions.isLands(level) || level.getGameTime() % INTERVAL != 0 ||
                !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        InkLandsLayout layout = InkLandsLayout.of(level);
        if (layout == null) return;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || !level.getBiome(player.blockPosition()).is(ModBiomes.PALE_FOREST)) continue;
            int[] temple = null;
            double best = TEMPLE_RADIUS * TEMPLE_RADIUS;
            for (int[] p : Ruins.templePoints(layout)) {
                double dx = p[0] - player.getX(), dz = p[1] - player.getZ();
                double d = dx * dx + dz * dz;
                if (d < best) { best = d; temple = p; }
            }
            boolean choir = temple != null;
            // Count all nearby striders, including those spawned for a different player.
            int count = level.getEntitiesOfClass(PaleStriderEntity.class,
                    player.getBoundingBox().inflate(LOCAL_RADIUS, 64, LOCAL_RADIUS), PaleStriderEntity::isAlive).size();
            int cap = choir ? CHOIR_CAP : SOLO_CAP;
            if (count >= cap) continue;
            if (choir) spawnChoir(level, layout, player, temple, cap - count);
            else spawnSolo(level, layout, player);
        }
    }
    private static void spawnSolo(ServerLevel level, InkLandsLayout layout, ServerPlayer player) {
        for (int i = 0; i < 24; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 35 + level.random.nextDouble() * 30;
            int x = (int)Math.floor(player.getX() + Math.cos(angle) * radius);
            int z = (int)Math.floor(player.getZ() + Math.sin(angle) * radius);
            if (spawnAt(level, layout, x, z, false)) return;
        }
    }
    private static void spawnChoir(ServerLevel level, InkLandsLayout layout, ServerPlayer player, int[] temple, int missing) {
        for (int i = 0; i < 24; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 32 + level.random.nextDouble() * 12;
            int x = (int)Math.floor(temple[0] + Math.cos(angle) * radius);
            int z = (int)Math.floor(temple[1] + Math.sin(angle) * radius);
            if (Math.hypot(x - player.getX(), z - player.getZ()) > 80 || !spawnAt(level, layout, x, z, true)) continue;
            int placed = 1;
            for (int j = 0; j < 20 && placed < missing; j++) {
                int dx = level.random.nextInt(17) - 8, dz = level.random.nextInt(17) - 8;
                if (dx * dx + dz * dz < 16) continue;
                if (Math.hypot(x + dx - player.getX(), z + dz - player.getZ()) <= 80 &&
                        spawnAt(level, layout, x + dx, z + dz, true)) placed++;
            }
            return;
        }
        // In dense forest one free glade is better than forcing entities through blocks.
        spawnSolo(level, layout, player);
    }
    private static boolean spawnAt(ServerLevel level, InkLandsLayout layout, int x, int z, boolean temple) {
        BlockPos column = new BlockPos(x, 0, z);
        if (!level.hasChunkAt(column) || layout.depthInside(x, z) < 0 ||
                Sanctuary.near(layout, x, z, 4) || layout.nearRoad(x, z, 3)) return false;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBiome(pos).is(ModBiomes.PALE_FOREST) ||
                !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        // Avoid appearing in a player's immediate view and avoid loading neighbouring chunks.
        for (ServerPlayer p : level.players()) {
            double dx = p.getX() - (x + 0.5), dz = p.getZ() - (z + 0.5);
            if (!p.isSpectator() && dx * dx + dz * dz < 28 * 28) return false;
        }
        for (int dx = -2; dx <= 2; dx += 4) for (int dz = -2; dz <= 2; dz += 4)
            if (!level.hasChunkAt(pos.offset(dx, 0, dz))) return false;
        AABB volume = new AABB(x - 0.25, y, z - 0.25, x + 1.25, y + 7.75, z + 1.25);
        if (volume.maxY >= level.getMaxBuildHeight() || !level.noCollision(volume)) return false;
        for (int dy = 0; dy < 8; dy++) if (!level.getFluidState(pos.above(dy)).isEmpty()) return false;
        if (!level.getEntitiesOfClass(PaleStriderEntity.class, volume.inflate(3), PaleStriderEntity::isAlive).isEmpty()) return false;
        PaleStriderEntity mob = ModMobs.PALE_STRIDER.create(level);
        if (mob == null) return false;
        mob.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360f, 0);
        mob.configureEncounter(temple);
        return level.addFreshEntity(mob);
    }
}
