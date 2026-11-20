package com.tarkrakov.tearsofink.prologue;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.world.item.ItemStack;
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
        ARRIVAL("arrival", 0), FIRST_FOUNTAIN("first_fountain", 1), ANVIL("anvil", 10), GEAR("gear", 2),
        SECOND_FOUNTAIN("second_fountain", 3), TRADER("trader", 4),
        CONTRACT("contract", 5), REDEEM("redeem", 6), FONT("font", 7),
        CONTRAST("contrast", 8), RUBRICS("rubrics", 9),
        INGOT("ingot", 11), WEAPON("weapon", 12), ARMOR("armor", 13), KEY("key", 14),
        TRAVEL("travel", 15), ALTAR("altar", 16), AWAKENING("awakening", 17);

        public final String id;
        private final int bit;
        Goal(String id, int index) { this.id = id; this.bit = 1 << index; }
        public boolean done(int mask) { return (mask & bit) != 0; }
    }

    /** Three directions share one persistent history. GEAR is a legacy bit, never reused. */
    public enum Track {
        STORY("story", Goal.ARRIVAL, Goal.FIRST_FOUNTAIN, Goal.SECOND_FOUNTAIN, Goal.TRADER),
        CALL("call", Goal.KEY, Goal.TRAVEL),
        CRAFT("craft", Goal.INGOT, Goal.ANVIL, Goal.WEAPON, Goal.ARMOR, Goal.CONTRACT, Goal.REDEEM,
                Goal.ALTAR, Goal.AWAKENING, Goal.FONT, Goal.CONTRAST, Goal.RUBRICS);

        public final String id;
        public final List<Goal> goals;
        Track(String id, Goal... goals) { this.id = id; this.goals = List.of(goals); }
    }

    public static boolean optional(Goal goal) {
        return goal == Goal.ALTAR || goal == Goal.AWAKENING;
    }

    /** Superseded tutorials are not awarded as completed. Older players need not repeat them. */
    public static boolean skipped(Goal goal, int mask) {
        return !goal.done(mask) && ((goal == Goal.KEY && Goal.SECOND_FOUNTAIN.done(mask))
                || (goal == Goal.INGOT && Goal.ANVIL.done(mask)));
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
                if (StoryKeyItem.wasUsed(p)) mask |= Goal.KEY.bit;
                if (p.getAttachedOrCreate(AscensionFont.HARD_CONTRACTS) > 0
                        || Vows.get(p).awakenings().stream().anyMatch(id -> id.matches("(heart:)?contract_[012]")))
                    mask |= Goal.CONTRACT.bit;
                if (inside) {
                    var inv = p.getInventory();
                    for (int i = 0; i < inv.getContainerSize(); i++)
                        mask |= itemBits(inv.getItem(i));
                }
            }
        }
        save(p, mask, force);
    }

    private static int itemBits(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int bits = 0;
        if (stack.is(ModItems.PALE_INGOT)) bits |= Goal.INGOT.bit;
        if (stack.is(ModBlocks.INK_ANVIL.asItem())) bits |= Goal.ANVIL.bit;
        if (stack.is(ModBlocks.CURSED_ALTAR.asItem())) bits |= Goal.ALTAR.bit;
        if (stack.getItem() instanceof OrdinaryArmorItem) bits |= Goal.ARMOR.bit | Goal.GEAR.bit;
        if ((Gear.ours(stack) && !stack.is(ModItems.DIM_SCIMITAR))
                || (stack.getItem() instanceof SoulShieldItem shield && shield.power() >= 1f))
            bits |= Goal.WEAPON.bit | Goal.GEAR.bit;
        if (Gear.ours(stack)) {
            if (Gear.tier(stack) >= 1) bits |= Goal.CONTRAST.bit;
            if (Gear.tier(stack) >= 2) bits |= Goal.RUBRICS.bit;
        }
        return bits;
    }

    /** Forge result access catches even a piece that is immediately equipped, stored or given away. */
    public static void obtained(ServerPlayer player, ItemStack stack) {
        if (!ModDimensions.isLands(player.level()) || player.isSpectator() || !player.isAlive()) return;
        save(player, player.getAttachedOrCreate(DATA) | itemBits(stack), false);
    }

    private static Goal first(int mask, Goal... goals) {
        for (Goal goal : goals) if (!goal.done(mask) && !skipped(goal, mask)) return goal;
        return null;
    }

    /** At most one currently suggested action per lane. No action gates any game mechanic. */
    public static Goal active(Track track, int mask) {
        if (mask < 0) return null;
        if (track == Track.STORY)
            return first(mask, Goal.ARRIVAL, Goal.FIRST_FOUNTAIN, Goal.SECOND_FOUNTAIN, Goal.TRADER);
        if (!Goal.FIRST_FOUNTAIN.done(mask)) return null;
        if (track == Track.CALL) {
            if (!Goal.SECOND_FOUNTAIN.done(mask)) return first(mask, Goal.KEY);
            return Goal.TRADER.done(mask) ? first(mask, Goal.TRAVEL) : null;
        }
        Goal basic = first(mask, Goal.INGOT, Goal.ANVIL, Goal.WEAPON, Goal.ARMOR);
        if (basic != null) return basic;
        if (!Goal.TRADER.done(mask)) return null;
        Goal hunt = first(mask, Goal.CONTRACT, Goal.REDEEM);
        if (hunt != null) return hunt;
        // Awakening is independent of ascension: never make a rare altar reagent a mandatory detour.
        if (Goal.ALTAR.done(mask) && !Goal.AWAKENING.done(mask)) return Goal.AWAKENING;
        Goal tier = first(mask, Goal.FONT, Goal.CONTRAST, Goal.RUBRICS);
        return tier != null ? tier : first(mask, Goal.ALTAR, Goal.AWAKENING);
    }

    public static List<Goal> next(int mask) {
        List<Goal> out = new ArrayList<>(3);
        for (Track track : Track.values()) {
            Goal goal = active(track, mask);
            if (goal != null) out.add(goal);
        }
        return List.copyOf(out);
    }
}
