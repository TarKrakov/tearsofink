package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative, transient score state for the actual surviving participants only. */
public final class OrionMusic {
    public enum Stage { OFF, FIRST, FADING, AWAKENING, SECOND }
    private static final UUID NO_FIGHT = new UUID(0, 0);
    private static final Sync STOP = new Sync(NO_FIGHT, Stage.OFF);
    private static final Map<UUID, Sync> SENT = new HashMap<>();
    private OrionMusic() {}

    public record Sync(UUID fight, Stage stage) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(TearsOfInk.id("orion_music"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.of(
                (buf, msg) -> {
                    buf.writeUUID(msg.fight());
                    buf.writeVarInt(msg.stage().ordinal());
                },
                buf -> {
                    UUID fight = buf.readUUID();
                    int value = buf.readVarInt();
                    if (value < 0 || value >= Stage.values().length) {
                        throw new IllegalArgumentException("Invalid Orion music stage");
                    }
                    return new Sync(fight, Stage.values()[value]);
                });
        @Override public Type<Sync> type() { return TYPE; }
    }

    static void init() {
        PayloadTypeRegistry.playS2C().register(Sync.TYPE, Sync.CODEC);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SENT.clear());
    }

    static void sync(MinecraftServer server, UUID fight, Stage stage, List<ServerPlayer> fighters) {
        Set<UUID> participating = new HashSet<>();
        if (fight != null && stage != Stage.OFF) {
            for (ServerPlayer player : fighters) participating.add(player.getUUID());
        }
        boolean heartbeat = server.getTickCount() % 20 == 0;
        Set<UUID> connected = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            connected.add(id);
            if (!ServerPlayNetworking.canSend(player, Sync.TYPE)) {
                SENT.remove(id);
                continue;
            }
            Sync previous = SENT.get(id);
            if (participating.contains(id)) {
                Sync next = new Sync(fight, stage);
                if (!next.equals(previous) || heartbeat) {
                    ServerPlayNetworking.send(player, next);
                    SENT.put(id, next);
                }
            } else if (previous != null) {
                ServerPlayNetworking.send(player, STOP);
                SENT.remove(id);
            }
        }
        SENT.keySet().retainAll(connected);
    }
}
