package com.tarkrakov.tearsofink.pantheon;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class PantheonGatekeeper {
    private static final AttachmentType<Boolean> MET = AttachmentRegistry.<Boolean>builder()
            .persistent(Codec.BOOL).copyOnDeath().buildAndRegister(TearsOfInk.id("pantheon_key_earned"));
    private static final int KEY_COOLDOWN = 20;

    private PantheonGatekeeper() {}

    static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) restoreKey(player);
            ServerLevel lands = ModDimensions.lands(server);
            if (lands == null) return;
            BlockPos at = PantheonRuins.keeper(lands);
            if (at == null || !lands.isPositionEntityTicking(at) || !lands.areEntitiesLoaded(new ChunkPos(at).toLong())) return;
            UUID uuid = keeperId(lands, at);
            if (!(lands.getEntity(uuid) instanceof Villager keeper)) {
                Villager replacement = EntityType.VILLAGER.create(lands);
                if (replacement == null) return;
                replacement.setUUID(uuid);
                replacement.setNoAi(true);
                replacement.setInvulnerable(true);
                replacement.setPersistenceRequired();
                replacement.setCustomName(Component.translatable("entity.tears-of-ink.pantheon_gatekeeper"));
                replacement.setCustomNameVisible(true);
                replacement.moveTo(Vec3.atBottomCenterOf(at), 180, 0);
                lands.addFreshEntity(replacement);
            } else if (keeper.position().distanceToSqr(Vec3.atBottomCenterOf(at)) > 0.36) {
                keeper.moveTo(Vec3.atBottomCenterOf(at), keeper.getYRot(), 0);
            }
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer server) || !isKeeper(server.serverLevel(), entity)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND || server.isSpectator()
                    || server.distanceToSqr(entity) > 25) return InteractionResult.FAIL;
            boolean first = !earned(server);
            if (first) server.setAttached(MET, true);
            restoreKey(server);
            server.sendSystemMessage(Component.translatable(first
                    ? "message.tears-of-ink.pantheon.greeting" : "message.tears-of-ink.pantheon.reminder"));
            if (first) PantheonTemple.ensureInstallation(server.server);
            return InteractionResult.SUCCESS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                world instanceof ServerLevel server && isKeeper(server, entity)
                        ? InteractionResult.FAIL : InteractionResult.PASS);
    }

    private static boolean isKeeper(ServerLevel level, Entity entity) {
        if (!(entity instanceof Villager) || !ModDimensions.isLands(level)) return false;
        BlockPos at = PantheonRuins.keeper(level);
        return at != null && keeperId(level, at).equals(entity.getUUID());
    }

    private static UUID keeperId(ServerLevel level, BlockPos at) {
        return UUID.nameUUIDFromBytes(("tearsofink:pantheon_gatekeeper:" + level.getSeed() + ":" + at.asLong())
                .getBytes(StandardCharsets.UTF_8));
    }

    private static boolean earned(ServerPlayer player) {
        return Boolean.TRUE.equals(player.getAttached(MET));
    }

    private static void restoreKey(ServerPlayer player) {
        if (earned(player)) PersonalSlots.ensurePantheonKey(player);
    }

    public static void useKey(ServerPlayer player) {
        if (!earned(player) || !player.isAlive() || player.isSpectator() || player.hasDisconnected()
                || player.isChangingDimension() || player.getCooldowns().isOnCooldown(ModItems.PANTHEON_KEY)
                || !PersonalSlots.get(player, PersonalSlots.PANTHEON_KEY).is(ModItems.PANTHEON_KEY)) return;

        player.getCooldowns().addCooldown(ModItems.PANTHEON_KEY, KEY_COOLDOWN);
        boolean inTemple = PantheonTemple.isTemple(player.level());
        boolean moved = inTemple ? PantheonTravel.leave(player) : PantheonTravel.enter(player);
        if (!moved) {
            if (!inTemple) PantheonTemple.ensureInstallation(player.server);
            player.displayClientMessage(Component.translatable(inTemple
                    ? "message.tears-of-ink.pantheon.no_safe_return" : "message.tears-of-ink.pantheon.preparing"), true);
        }
    }
}
