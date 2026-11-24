package com.tarkrakov.tearsofink.pantheon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

final class PantheonTravel {
    private record ReturnSpot(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch) {
        static final Codec<ReturnSpot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(ReturnSpot::dimension),
                Vec3.CODEC.fieldOf("position").forGetter(ReturnSpot::pos),
                Codec.FLOAT.fieldOf("yaw").forGetter(ReturnSpot::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(ReturnSpot::pitch)
        ).apply(instance, ReturnSpot::new));
    }

    private static final AttachmentType<ReturnSpot> RETURN = AttachmentRegistry.<ReturnSpot>builder()
            .persistent(ReturnSpot.CODEC).copyOnDeath().buildAndRegister(TearsOfInk.id("pantheon_return"));

    private PantheonTravel() {}

    static void init() {}

    static boolean enter(ServerPlayer player) {
        if (PantheonEditor.active() && !PantheonEditor.isOwner(player)) return false;
        ServerLevel destination = player.server.getLevel(PantheonTemple.DIMENSION);
        Vec3 arrival = Vec3.atBottomCenterOf(PantheonTemple.entrance());
        if (destination == null || !PantheonTemple.ready(destination) || !safe(player, destination, arrival)) return false;
        if (!PantheonTemple.isTemple(player.level())) {
            player.setAttached(RETURN, new ReturnSpot(player.serverLevel().dimension(),
                    player.position(), player.getYRot(), player.getXRot()));
        }
        move(player, destination, arrival, 180, 0);
        return true;
    }

    static boolean leave(ServerPlayer player) {
        ReturnSpot previous = player.getAttached(RETURN);
        ServerLevel level = previous == null ? null : player.server.getLevel(previous.dimension());
        if (level != null && !PantheonTemple.isTemple(level)) {
            Vec3 target = nearest(player, level, previous.pos());
            if (target == null && ModDimensions.isLands(level)) {
                BlockPos entrance = PantheonRuins.entrance(level);
                if (entrance != null) target = nearest(player, level, Vec3.atBottomCenterOf(entrance));
            }
            if (target != null) {
                move(player, level, target, previous.yaw(), previous.pitch());
                player.setAttached(RETURN, null);
                return true;
            }
        }
        if (!returnToSpawn(player)) return false;
        player.setAttached(RETURN, null);
        return true;
    }

    static boolean returnToSpawn(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        Vec3 position = nearest(player, overworld, Vec3.atBottomCenterOf(spawn));
        if (position == null) {
            int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
            position = nearest(player, overworld, Vec3.atBottomCenterOf(spawn.atY(y)));
        }
        if (position == null) {
            for (BlockPos column : BlockPos.spiralAround(spawn, 6, Direction.EAST, Direction.SOUTH)) {
                int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                Vec3 surface = Vec3.atBottomCenterOf(column.atY(y));
                if (safe(player, overworld, surface)) {
                    position = surface;
                    break;
                }
            }
        }
        if (position == null) return false;
        move(player, overworld, position, overworld.getSharedSpawnAngle(), 0);
        return true;
    }

    static DimensionTransition respawn(ServerPlayer player, DimensionTransition.PostDimensionTransition post) {
        ServerLevel temple = player.serverLevel();
        Vec3 at = Vec3.atBottomCenterOf(PantheonTemple.entrance());
        if (!PantheonTemple.ready(temple) || !safe(player, temple, at)) return null;
        return new DimensionTransition(temple, at, Vec3.ZERO, 180, 0, post);
    }

    private static void move(ServerPlayer player, ServerLevel level, Vec3 pos, float yaw, float pitch) {
        player.teleportTo(level, pos.x, pos.y, pos.z, Float.isFinite(yaw) ? yaw : 0,
                Float.isFinite(pitch) ? pitch : 0);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
    }

    private static Vec3 nearest(ServerPlayer player, ServerLevel level, Vec3 saved) {
        if (!Double.isFinite(saved.x) || !Double.isFinite(saved.y) || !Double.isFinite(saved.z)) return null;
        BlockPos center = BlockPos.containing(saved);
        if (!level.getWorldBorder().isWithinBounds(center)) return null;
        if (safe(player, level, saved)) return saved;
        // Stay in the original column. A horizontal search could land across a quest gate or wall.
        for (int step = 0; step <= 4; step++) {
            Vec3 above = Vec3.atBottomCenterOf(center.above(step));
            if (safe(player, level, above)) return above;
            if (step == 0) continue;
            Vec3 below = Vec3.atBottomCenterOf(center.below(step));
            if (safe(player, level, below)) return below;
        }
        return null;
    }

    private static boolean safe(ServerPlayer player, ServerLevel level, Vec3 pos) {
        BlockPos feet = BlockPos.containing(pos);
        if (feet.getY() <= level.getMinBuildHeight() || feet.getY() + 2 >= level.getMaxBuildHeight()) return false;
        var bounds = player.getDimensions(Pose.STANDING).makeBoundingBox(pos);
        if (!level.getWorldBorder().isWithinBounds(bounds)) return false;
        var floor = feet.below();
        var under = level.getBlockState(floor);
        if (!under.isFaceSturdy(level, floor, Direction.UP) || hazardous(under)) return false;
        BlockPos max = BlockPos.containing(bounds.maxX - 0.0001, bounds.maxY - 0.0001, bounds.maxZ - 0.0001);
        BlockPos min = BlockPos.containing(bounds.minX + 0.0001, bounds.minY + 0.0001, bounds.minZ + 0.0001);
        for (BlockPos block : BlockPos.betweenClosed(min, max)) {
            if (hazardous(level.getBlockState(block))) return false;
        }
        // Other players may arrive together. Block collisions alone determine whether the landing is safe.
        return !level.getBlockCollisions(player, bounds).iterator().hasNext();
    }

    private static boolean hazardous(BlockState state) {
        return !state.getFluidState().isEmpty() || state.is(BlockTags.FIRE) || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY) || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE) || state.is(BlockTags.CAMPFIRES);
    }
}
