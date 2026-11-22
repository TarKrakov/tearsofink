package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.entity.CaterpillarEntity;
import com.tarkrakov.tearsofink.entity.PaleDeerEntity;
import com.tarkrakov.tearsofink.entity.PaleOwlEntity;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;

/** D-143: a quiet battlefield, with only the original Pale Striders as fauna.
 * No mobs killed, drops generated, chunks force-loaded or saved entities erased.
 */
public final class BattlefieldFaunaBoundary {
    private static final int CHECK_INTERVAL = 40;
    private static final int MOVE_BUDGET = 8;
    private BattlefieldFaunaBoundary() {}

    /** The immutable field edge, shared by every natural wildlife spawn path. */
    public static boolean inside(ServerLevel level, double x, double z) {
        if (!ModDimensions.isLands(level)) return false;
        var plan = BattlefieldWorldPlan.of(InkLandsLayout.of(level));
        if (plan == null) return false;
        var field = plan.near((int) Math.floor(x), (int) Math.floor(z), 12);
        return field != null && field.openness((int) Math.floor(x), (int) Math.floor(z)) > .045d;
    }

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(BattlefieldFaunaBoundary::tick);
    }

    private static boolean wild(Mob mob) {
        return mob instanceof PaleOwlEntity || mob instanceof PaleDeerEntity
                || mob instanceof CaterpillarEntity
                || mob.getType().getCategory() == MobCategory.CREATURE
                || mob.getType().getCategory() == MobCategory.AMBIENT;
    }

    private static void tick(ServerLevel level) {
        if (!ModDimensions.isLands(level) || level.players().isEmpty()
                || level.getGameTime() % CHECK_INTERVAL != 13) return;
        var plan = BattlefieldWorldPlan.of(InkLandsLayout.of(level));
        if (plan == null) return;
        int moved = 0;
        for (var field : plan.fields()) {
            boolean playerNear = level.players().stream().anyMatch(p -> p.isAlive()
                    && !p.isSpectator() && field.distance(p.getBlockX(), p.getBlockZ()) < 170d);
            if (!playerNear) continue;
            AABB area = new AABB(field.x() - 90d, level.getMinBuildHeight(),
                    field.z() - 90d, field.x() + 90d, level.getMaxBuildHeight(),
                    field.z() + 90d);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area, BattlefieldFaunaBoundary::wild)) {
                if (moved >= MOVE_BUDGET) return;
                // Never disturb explicitly kept or player-owned animals. Mobs
                // already in saved worlds remain the same entities after moving.
                if (!mob.isAlive() || mob.hasCustomName() || mob.isPersistenceRequired()
                        || mob.isNoAi() || mob.isLeashed() || mob.isPassenger()
                        || mob instanceof TamableAnimal tame && tame.isTame()
                        || field.openness(mob.getBlockX(), mob.getBlockZ()) <= .045d) continue;
                Vec3 destination = safeOutside(level, field, mob);
                if (destination == null) continue; // no loaded destination yet
                mob.getNavigation().stop();
                mob.setDeltaMovement(Vec3.ZERO);
                mob.teleportTo(destination.x, destination.y, destination.z);
                moved++;
            }
        }
    }

    private static Vec3 safeOutside(ServerLevel level, BattlefieldWorldPlan.Field field, Mob mob) {
        double base = Math.atan2(mob.getZ() - field.z(), mob.getX() - field.x());
        for (int i = 0; i < 16; i++) {
            double angle = base + ((i & 1) == 0 ? 1 : -1) * (i / 2) * Math.PI / 12d;
            int radius = 96 + (i % 3) * 7;
            int x = field.x() + (int) Math.round(Math.cos(angle) * radius);
            int z = field.z() + (int) Math.round(Math.sin(angle) * radius);
            if (field.openness(x, z) > 0d) continue;
            var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) continue;
            int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
            BlockPos foot = new BlockPos(x, surface, z);
            if (!level.getBiome(foot).is(ModBiomes.PALE_FOREST)) continue;
            var ground = chunk.getBlockState(foot.below());
            if (!ground.isFaceSturdy(level, foot.below(), Direction.UP)
                    || !ground.getFluidState().isEmpty()) continue;
            double y = surface + (mob instanceof PaleOwlEntity ? 2d : 0d);
            Vec3 target = new Vec3(x + .5d, y, z + .5d);
            AABB box = mob.getBoundingBox().move(target.subtract(mob.position()));
            if (!level.getWorldBorder().isWithinBounds(box) || !level.noCollision(mob, box))
                continue;
            return target;
        }
        return null;
    }
}
