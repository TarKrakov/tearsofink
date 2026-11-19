package com.tarkrakov.tearsofink.prologue;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.item.OrdinaryArmorItem;
import com.tarkrakov.tearsofink.trader.SoulTrader;
import com.tarkrakov.tearsofink.travel.FountainTravel;
import com.tarkrakov.tearsofink.vow.Vows;
import com.tarkrakov.tearsofink.weapon.SoulShieldItem;
import com.tarkrakov.tearsofink.weapon.forge.AscensionFont;
import com.tarkrakov.tearsofink.weapon.forge.Gear;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Personal, monotone milestones: guidance only, never a gameplay gate or reward. */
public final class FateProgress {
    public enum Goal {
        ARRIVAL("arrival", 0), FIRST_FOUNTAIN("first_fountain", 1), GEAR("gear", 2),
        SECOND_FOUNTAIN("second_fountain", 3), TRADER("trader", 4),
        CONTRACT("contract", 5), REDEEM("redeem", 6), FONT("font", 7),
        CONTRAST("contrast", 8), RUBRICS("rubrics", 9);

        public final String id;
        private final int bit;
        Goal(String id, int index) { this.id = id; this.bit = 1 << index; }
        public boolean done(int mask) { return (mask & bit) != 0; }
    }

    private static final AttachmentType<Integer> DATA = AttachmentRegistry.<Integer>builder()
            .persistent(Codec.INT).copyOnDeath().initializer(() -> 0)
            .buildAndRegister(TearsOfInk.id("fate_progress"));

    public record Sync(int mask) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(TearsOfInk.id("fate_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Sync::mask, Sync::new);
        @Override public Type<Sync> type() { return TYPE; }
    }

    private FateProgress() {}
    public static void init() { PayloadTypeRegistry.playS2C().register(Sync.TYPE, Sync.CODEC); }

    private static void save(ServerPlayer p, int mask, boolean force) {
        int old = p.getAttachedOrCreate(DATA);
        if (mask != old) p.setAttached(DATA, mask);
        if ((force || mask != old) && ServerPlayNetworking.canSend(p, Sync.TYPE))
            ServerPlayNetworking.send(p, new Sync(mask));
    }

    public static void mark(ServerPlayer p, Goal... goals) {
        if (!ModDimensions.isLands(p.level()) || p.isSpectator() || !p.isAlive()) return;
        int mask = p.getAttachedOrCreate(DATA);
        for (Goal goal : goals) mask |= goal.bit;
        save(p, mask, false);
    }

    /** Backfill only demonstrable history from existing saves, plus bounded inventory observation. */
    public static void refresh(ServerPlayer p, boolean force) {
        int mask = p.getAttachedOrCreate(DATA);
        boolean inside = ModDimensions.isLands(p.level());
        if (!p.isSpectator() && p.isAlive()) {
            var history = p.getAttachedOrCreate(DiaryEntries.DATA);
            if (inside || history.contains(DiaryEntries.FIRST_STEPS)) mask |= Goal.ARRIVAL.bit;
            if (Goal.ARRIVAL.done(mask)) {
                int opened = FountainTravel.openedCount(p);
                if (opened >= 1) mask |= Goal.FIRST_FOUNTAIN.bit;
                if (opened >= SoulTrader.REQUIRED_FOUNTAINS) mask |= Goal.SECOND_FOUNTAIN.bit;
                if (SoulTrader.introduced(p)) mask |= Goal.TRADER.bit;
                if (p.getAttachedOrCreate(AscensionFont.HARD_CONTRACTS) > 0
                        || Vows.get(p).awakenings().stream().anyMatch(id -> id.matches("(heart:)?contract_[012]")))
                    mask |= Goal.CONTRACT.bit;
                if (history.contains("draft_weaponcraft") || history.contains("draft_armor")) mask |= Goal.GEAR.bit;
                if (inside && !(Goal.GEAR.done(mask) && Goal.CONTRAST.done(mask) && Goal.RUBRICS.done(mask))) {
                    var inv = p.getInventory();
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        var stack = inv.getItem(i);
                        if (stack.isEmpty()) continue;
                        if ((Gear.ours(stack) && !stack.is(ModItems.DIM_SCIMITAR))
                                || stack.getItem() instanceof OrdinaryArmorItem
                                || (stack.getItem() instanceof SoulShieldItem shield && shield.power() >= 1f))
                            mask |= Goal.GEAR.bit;
                        if (Gear.ours(stack)) {
                            if (Gear.tier(stack) >= 1) mask |= Goal.CONTRAST.bit;
                            if (Gear.tier(stack) >= 2) mask |= Goal.RUBRICS.bit;
                        }
                    }
                }
            }
        }
        save(p, mask, force);
    }

    /** At most two useful next steps. Finishing any later goal first is allowed and remembered. */
    public static List<Goal> next(int mask) {
        if (!Goal.ARRIVAL.done(mask)) return List.of(Goal.ARRIVAL);
        List<Goal> out = new ArrayList<>(2);
        if (!Goal.FIRST_FOUNTAIN.done(mask)) out.add(Goal.FIRST_FOUNTAIN);
        else if (!Goal.SECOND_FOUNTAIN.done(mask)) out.add(Goal.SECOND_FOUNTAIN);
        else if (!Goal.TRADER.done(mask)) out.add(Goal.TRADER);
        else if (!Goal.CONTRACT.done(mask)) out.add(Goal.CONTRACT);
        else if (!Goal.REDEEM.done(mask)) out.add(Goal.REDEEM);
        if (!Goal.GEAR.done(mask)) out.add(Goal.GEAR);
        if (Goal.TRADER.done(mask) && Goal.CONTRACT.done(mask) && out.size() < 2) {
            if (!Goal.FONT.done(mask)) out.add(Goal.FONT);
            else if (!Goal.CONTRAST.done(mask)) out.add(Goal.CONTRAST);
            else if (!Goal.RUBRICS.done(mask)) out.add(Goal.RUBRICS);
        }
        return List.copyOf(out);
    }
}
