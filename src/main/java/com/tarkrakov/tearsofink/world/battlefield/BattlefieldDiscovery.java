package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Client receives fog-locality only for already generated, stamped battlefields. */
public final class BattlefieldDiscovery {
    private BattlefieldDiscovery() {}

    public record FieldSync(int x, int z, long seed, boolean active) implements CustomPacketPayload {
        public static final Type<FieldSync> TYPE = new Type<>(TearsOfInk.id("battlefield_fog"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FieldSync> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, FieldSync::x,
                        ByteBufCodecs.VAR_INT, FieldSync::z,
                        ByteBufCodecs.VAR_LONG, FieldSync::seed,
                        ByteBufCodecs.BOOL, FieldSync::active,
                        FieldSync::new);
        @Override public Type<FieldSync> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(FieldSync.TYPE, FieldSync.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 40 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!ServerPlayNetworking.canSend(player, FieldSync.TYPE)) continue;
                FieldSync state = new FieldSync(0, 0, 0, false);
                if (ModDimensions.isLands(player.level())) {
                    var level = player.serverLevel();
                    InkLandsLayout layout = InkLandsLayout.of(level);
                    BattlefieldWorldPlan plan = BattlefieldWorldPlan.of(layout);
                    if (plan != null) for (var field : plan.fields()) {
                        if (field.distance(player.getBlockX(), player.getBlockZ()) < 210
                                && BattlefieldInstallations.fieldReady(level, field)) {
                            state = new FieldSync(field.x(), field.z(), field.seed(), true);
                            break;
                        }
                    }
                }
                ServerPlayNetworking.send(player, state);
            }
        });
    }
}
