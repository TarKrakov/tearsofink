package com.tarkrakov.tearsofink.skill;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.vow.Vows;
import com.tarkrakov.tearsofink.weapon.*;
import com.tarkrakov.tearsofink.weapon.blood.*;
import com.tarkrakov.tearsofink.weapon.combat.*;
import com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem;
import com.tarkrakov.tearsofink.weapon.shield.*;
import com.tarkrakov.tearsofink.world.ModDimensions;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Read-only, server-authoritative HUD. Never grants or starts an action. */
public final class CombatCooldowns {
    private CombatCooldowns() {}
    public static final int READY = 0, BLOCKED = 1, ACTIVE = 2, NO_RESOURCE = 3;
    public record Entry(String id, int remaining, int total, int state) {
        public static final Entry EMPTY = new Entry("",0,1,READY);
        public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::id, ByteBufCodecs.VAR_INT, Entry::remaining,
                ByteBufCodecs.VAR_INT, Entry::total, ByteBufCodecs.VAR_INT, Entry::state, Entry::new);
        public static Entry of(String id,int left,int length,int state) {
            return new Entry(id,Math.max(0,left),Math.max(1,Math.max(left,length)),state);
        }
    }
    public record Snapshot(Entry move, Entry r, Entry rmb, Entry shiftR) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(TearsOfInk.id("combat_cooldowns"));
        public static final StreamCodec<ByteBuf, Snapshot> CODEC = StreamCodec.composite(
                Entry.CODEC, Snapshot::move, Entry.CODEC, Snapshot::r,
                Entry.CODEC, Snapshot::rmb, Entry.CODEC, Snapshot::shiftR, Snapshot::new);
        public static final Snapshot EMPTY = new Snapshot(Entry.EMPTY,Entry.EMPTY,Entry.EMPTY,Entry.EMPTY);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static final Map<UUID,Snapshot> LAST = new HashMap<>();
    public static void init() {
        PayloadTypeRegistry.playS2C().register(Snapshot.TYPE,Snapshot.CODEC);
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->LAST.remove(h.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->LAST.clear());
        ServerTickEvents.END_SERVER_TICK.register(s->{
            for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                if (!ServerPlayNetworking.canSend(p,Snapshot.TYPE)) continue;
                Snapshot view = snapshot(p);
                if (!view.equals(LAST.put(p.getUUID(),view))) ServerPlayNetworking.send(p,view);
            }
        });
    }

    /** Matches the paired RMB dispatcher; ordinary items opt out before calling this. */
    public static Skill pairedSkill(Archetype a) {
        return switch(a) {
            case EXECUTIONER -> WeaponAbilities.GUILLOTINE;
            case DAGGER -> DaggerMoves.DUAL_RUSH;
            case AXE -> AxeMoves.EXECUTION;
            case DUSSAK -> BloodAbilities.DOUBLE_PARRY;
            case TALWAR -> BloodAbilities.TALWAR_CROSS;
            default -> WeaponAbilities.FLOURISH;
        };
    }
    private static boolean locked(ServerPlayer p) {
        return PaleGreatswordMoves.locked(p) || PaleLongswordMoves.locked(p) || Parry.active(p);
    }
    private static Entry generic(ServerPlayer p,Skill skill,boolean shield) {
        if (skill == null || !Vows.allows(p,skill)) return Entry.EMPTY;
        String id = skill.id().getPath();
        if (skill == WeaponAbilities.MERCY && WeaponAbilities.MERCY.isActive(p) && !PaleLongswordMoves.locked(p))
            return Entry.of(id,0,1,ACTIVE);
        int left = Skills.remaining(p,skill), total = Skills.cooldownLength(p,skill);
        if (shield && WeaponAbilities.shieldCooldownRemaining(p) > left) {
            left = WeaponAbilities.shieldCooldownRemaining(p);
            total = ShieldTuning.ABILITY_SHARED_TICKS;
        }
        if (skill == WeaponAbilities.MERCY && WeaponAbilities.MERCY.lockoutRemaining(p) > left) {
            left = WeaponAbilities.MERCY.lockoutRemaining(p);
            total = WeaponAbilities.MercyAbility.LOCKOUT_TICKS;
        }
        int state = locked(p) ? BLOCKED : !Skills.resourceReady(p,skill) ? NO_RESOURCE : READY;
        return Entry.of(id,left,total,state);
    }
    private static Entry special(ServerPlayer p,String id,int[] timer,boolean otherLock) {
        int state = timer[2];
        if (otherLock || Parry.active(p)) state = BLOCKED;
        return Entry.of(id,timer[0],timer[1],state);
    }
    private static Entry r(ServerPlayer p,boolean left) {
        if (ShieldStance.dual(p)) return generic(p,WeaponAbilities.boundAbility(p,left),true);
        ItemStack main = p.getMainHandItem(), stack = left ? p.getOffhandItem() : main;
        if (left && PaleGreatswordMoves.dual(p))
            return special(p,"pale_guard",PaleGreatswordMoves.hudTimer(p,1,false),PaleLongswordMoves.locked(p));
        if (left && InkGreatswordItem.twoHanded(main)) return Entry.EMPTY;
        if (PaleLongswordMoves.isWeapon(stack)) {
            if (left && PaleLongswordMoves.dual(p)) return Entry.EMPTY;
            return special(p,"pale_longsword_strike",PaleLongswordMoves.hudTimer(p,false),PaleGreatswordMoves.locked(p));
        }
        if (PaleGreatswordMoves.isWeapon(stack))
            return special(p,PaleGreatswordMoves.dual(p) ? "pale_pair_parry" : "pale_sweep",
                    PaleGreatswordMoves.hudTimer(p,0,left),PaleLongswordMoves.locked(p));
        return generic(p,WeaponAbilities.boundAbility(p,left),stack.getItem() instanceof SoulShieldItem);
    }
    private static Entry rmb(ServerPlayer p) {
        ItemStack main = p.getMainHandItem(), off = p.getOffhandItem();
        if (ShieldStance.dual(p)) return Entry.EMPTY;
        if (PaleLongswordMoves.dual(p))
            return special(p,"pale_guard",PaleLongswordMoves.hudTimer(p,true),PaleGreatswordMoves.locked(p));
        if (PaleGreatswordMoves.isWeapon(main))
            return special(p,PaleGreatswordMoves.dual(p) ? "pale_pair_charge" : "pale_guard",
                    PaleGreatswordMoves.hudTimer(p,PaleGreatswordMoves.dual(p)?2:1,false),PaleLongswordMoves.locked(p));
        if (!(main.getItem() instanceof InkGreatswordItem) || main.getItem() instanceof SoulShieldItem) return Entry.EMPTY;
        Archetype a = Archetype.of(main);
        if (a == null) return Entry.EMPTY;
        // Aiming at a block is only a temporary input diversion, not loss of the hilt technique.
        if (!InkGreatswordItem.twoHanded(main) && off.isEmpty()) {
            float pct = p.getCooldowns().getCooldownPercent(main.getItem(),0f);
            return Entry.of("hilt",Math.round(pct*1000),1000,
                    PaleLongswordMoves.locked(p)||Parry.active(p)||Combat.hitPending(p) ? BLOCKED : READY);
        }
        if (a != Archetype.of(off) || main.getItem() instanceof OrdinaryWeaponItem) return Entry.EMPTY;
        Skill skill = pairedSkill(a);
        Entry e = generic(p,skill,false);
        if (p.isUsingItem() && p.getUseItem() == main &&
                (skill == WeaponAbilities.GUILLOTINE || skill == AxeMoves.EXECUTION))
            return Entry.of(e.id(),e.remaining(),e.total(),ACTIVE);
        return e;
    }
    private static Snapshot snapshot(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || p.isCreative() || !ModDimensions.hasLandsRules(p.level()) || !InkPowers.get(p).inside()) return Snapshot.EMPTY;
        Skill move = Skills.mobility(p);
        Entry m = Skills.knows(p,move) ? generic(p,move,false) : Entry.EMPTY;
        if (!m.id().isEmpty() && (p.isFallFlying() || p.isPassenger() || p.isSleeping()))
            m = Entry.of(m.id(),m.remaining(),m.total(),BLOCKED);
        return new Snapshot(m,r(p,false),rmb(p),r(p,true));
    }
}
