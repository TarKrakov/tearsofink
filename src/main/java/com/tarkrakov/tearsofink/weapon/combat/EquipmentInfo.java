package com.tarkrakov.tearsofink.weapon.combat;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Read-only information for equipment tooltips. Never used for combat decisions. */
public final class EquipmentInfo {
    private EquipmentInfo() {}

    public record ComboView(String id, boolean paired, int steps,
                            float minMultiplier, float maxMultiplier,
                            float minReach, float maxReach, int minTicks, int maxTicks) {}
    public record PaleView(int singleR, int dualR, int charge, int backstep,
                           float singleGuard, float dualGuard) {}

    public record Sync(List<ComboView> combos, PaleView pale) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(TearsOfInk.id("equipment_info"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.of(
                (buf, msg) -> {
                    buf.writeVarInt(msg.combos.size());
                    for (ComboView c : msg.combos) {
                        buf.writeUtf(c.id(), 64);
                        buf.writeBoolean(c.paired());
                        buf.writeVarInt(c.steps());
                        buf.writeFloat(c.minMultiplier());
                        buf.writeFloat(c.maxMultiplier());
                        buf.writeFloat(c.minReach());
                        buf.writeFloat(c.maxReach());
                        buf.writeVarInt(c.minTicks());
                        buf.writeVarInt(c.maxTicks());
                    }
                    PaleView p = msg.pale();
                    buf.writeVarInt(p.singleR()); buf.writeVarInt(p.dualR());
                    buf.writeVarInt(p.charge()); buf.writeVarInt(p.backstep());
                    buf.writeFloat(p.singleGuard()); buf.writeFloat(p.dualGuard());
                },
                buf -> {
                    int n = buf.readVarInt();
                    if (n < 0 || n > Archetype.values().length * 2) throw new IllegalArgumentException("Invalid equipment info count");
                    List<ComboView> combos = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) combos.add(new ComboView(
                            buf.readUtf(64), buf.readBoolean(), buf.readVarInt(),
                            buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                            buf.readVarInt(), buf.readVarInt()));
                    return new Sync(List.copyOf(combos), new PaleView(buf.readVarInt(), buf.readVarInt(),
                            buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat()));
                });

        @Override public Type<Sync> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(Sync.TYPE, Sync.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer(), snapshot()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (!success) return;
            Sync data = snapshot();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) send(p, data);
        });
    }

    private static void send(ServerPlayer player, Sync data) {
        if (ServerPlayNetworking.canSend(player, Sync.TYPE)) ServerPlayNetworking.send(player, data);
    }

    private static Sync snapshot() {
        List<ComboView> views = new ArrayList<>();
        for (Archetype a : Archetype.values()) {
            Combos.Combo c = Combos.get(a);
            append(views, a, c, false, c.steps());
            append(views, a, c, true, c.dualSteps());
        }
        var t = PaleGreatswordBalance.get();
        return new Sync(List.copyOf(views), new PaleView(t.singleR().cooldownTicks(), t.dualR().cooldownTicks(),
                t.dualCharge().cooldownTicks(), t.backstep().cooldownTicks(),
                t.shiftGuard().singleReduction(), t.shiftGuard().dualReduction()));
    }

    private static void append(List<ComboView> views, Archetype a, Combos.Combo c, boolean paired, List<Combos.Step> steps) {
        if (steps.isEmpty()) return;
        float minD = Float.POSITIVE_INFINITY, maxD = Float.NEGATIVE_INFINITY;
        float minR = Float.POSITIVE_INFINITY, maxR = Float.NEGATIVE_INFINITY;
        int minT = Integer.MAX_VALUE, maxT = 0;
        for (Combos.Step s : steps) {
            minD = Math.min(minD, s.damage()); maxD = Math.max(maxD, s.damage());
            minR = Math.min(minR, s.reach()); maxR = Math.max(maxR, s.reach());
            int ticks = c.scaled(s.duration(), paired);
            minT = Math.min(minT, ticks); maxT = Math.max(maxT, ticks);
        }
        views.add(new ComboView(a.id, paired, steps.size(), minD, maxD, minR, maxR, minT, maxT));
    }
}
