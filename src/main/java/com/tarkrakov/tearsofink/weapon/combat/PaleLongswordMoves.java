package com.tarkrakov.tearsofink.weapon.combat;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillInput;
import com.tarkrakov.tearsofink.weapon.InkGreatswordItem;
import com.tarkrakov.tearsofink.weapon.forge.Awakening;
import com.tarkrakov.tearsofink.weapon.forge.Gear;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative resource-free longsword techniques. */
public final class PaleLongswordMoves {
    private static final Archetype ARCH = Archetype.PALE_LONGSWORD;
    private enum Phase { IDLE, STRIKE, GUARD }
    private static final class State {
        Phase phase = Phase.IDLE;
        long singleReady, dualReady, guardReady, hitAt, until;
        int singleLength, dualLength, guardLength;
        ServerPlayer owner;
        ServerLevel level;
        ItemStack main, off;
        boolean left, paired, hit;
        float damage;
        PaleLongswordTuning.Strike strike;
    }
    private record Context(ServerPlayer player, ItemStack stack) {}
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final ThreadLocal<Context> DAMAGE = new ThreadLocal<>();
    private static boolean passingGuard;
    private PaleLongswordMoves() {}

    public static final Skill SINGLE = new Skill(TearsOfInk.id("pale_longsword_strike"), 0, 0,
            PaleLongswordTuning.SINGLE.cooldown()) {
        @Override public boolean activate(ServerPlayer p, SkillInput input) {
            return useR(p, input.sneaking());
        }
    };
    public static boolean isWeapon(ItemStack s) { return s.is(ModItems.PALE_LONGSWORD); }
    public static boolean dual(Player p) { return isWeapon(p.getMainHandItem()) && isWeapon(p.getOffhandItem()); }
    private static long now(ServerPlayer p) { return p.server.getTickCount(); }
    private static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isPassenger() && !p.isSleeping()
                && ModDimensions.hasLandsRules(p.level());
    }
    private static boolean valid(ServerPlayer p, State s) {
        return p == s.owner && p.serverLevel() == s.level && p.getMainHandItem() == s.main
                && p.getOffhandItem() == s.off && (!s.paired || dual(p));
    }
    private static State state(ServerPlayer p) {
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        if (s.owner != null && s.owner != p) { s = new State(); STATES.put(p.getUUID(), s); }
        return s;
    }
    private static void capture(ServerPlayer p, State s) {
        s.owner = p; s.level = p.serverLevel(); s.main = p.getMainHandItem(); s.off = p.getOffhandItem();
    }
    public static void cancelForAction(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.phase == Phase.IDLE) return;
        if (s.phase == Phase.GUARD) releaseGuard(p);
        else s.phase = Phase.IDLE;
        if (p.isUsingItem() && isWeapon(p.getUseItem())) p.stopUsingItem();
    }
    public static boolean locked(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        return s != null && s.phase != Phase.IDLE && valid(p,s) && available(p);
    }
    public static ItemStack damageWeapon(LivingEntity attacker, ItemStack fallback) {
        Context c = DAMAGE.get();
        return c != null && c.player() == attacker ? c.stack() : fallback;
    }
    public static boolean hurtAs(ServerPlayer p, ItemStack stack, LivingEntity target, DamageSource source, float damage) {
        Context old = DAMAGE.get();
        DAMAGE.set(new Context(p,stack));
        try { return target.hurt(source,damage); }
        finally { if (old == null) DAMAGE.remove(); else DAMAGE.set(old); }
    }
    public static double attackDamage(ServerPlayer p, ItemStack stack) {
        if (stack == p.getMainHandItem()) return p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double[] base = {p.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)};
        stack.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) base[0] += modifier.amount();
        });
        return base[0];
    }
    private static void wear(ServerPlayer p, ItemStack stack) {
        Gear.wear(p,stack,Gear.WEAR_ABILITY);
        Awakening.abilityUsed(p);
    }
    private static void playPair(ServerPlayer p, String name, int ticks, float tempo) {
        Combat.play(p,ARCH,name,name+"_off",ticks,tempo);
    }
    public static int[] hudTimer(ServerPlayer p, boolean guard) {
        State s = STATES.get(p.getUUID());
        if (s == null) return new int[]{0,1,0};
        boolean pair = dual(p);
        long ready = guard ? s.guardReady : pair ? s.dualReady : s.singleReady;
        int length = guard ? s.guardLength : pair ? s.dualLength : s.singleLength;
        int state = valid(p,s) && guard && s.phase == Phase.GUARD ? 2 : 0;
        return new int[]{(int)Math.max(0,ready-now(p)),Math.max(1,length),state};
    }

    public static boolean useR(ServerPlayer p, boolean left) {
        if (!available(p)) return false;
        if (left && dual(p)) return false;
        ItemStack sword = left ? p.getOffhandItem() : p.getMainHandItem();
        if (!isWeapon(sword)) return false;
        State s = state(p);
        boolean pair = dual(p);
        long time = now(p);
        if (time < (pair ? s.dualReady : s.singleReady)) return false;
        CombatTransitions.begin(p, false);
        var tune = pair ? PaleLongswordTuning.DUAL : PaleLongswordTuning.SINGLE;
        capture(p,s); s.left = left; s.paired = pair; s.strike = tune;
        s.damage = (float) attackDamage(p,sword); s.hit = false;
        s.phase = Phase.STRIKE; s.hitAt = time+tune.hit(); s.until = time+tune.duration();
        long ready = time+Math.round(tune.cooldown()*Awakening.cooldownMult(sword));
        if (pair) { s.dualReady = ready; s.dualLength = (int)(ready-time); }
        else { s.singleReady = ready; s.singleLength = (int)(ready-time); }
        String name = pair ? "bb_author_dual_r_ability" : "bb_author_r_ability";
        if (pair) playPair(p,name,tune.duration(),tune.tempo());
        else Combat.play(p,ARCH,left?null:name,left?name:null,tune.duration(),tune.tempo());
        wear(p,sword);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.8f,.75f);
        return true;
    }
    public static boolean beginGuard(ServerPlayer p) {
        if (!available(p) || !dual(p)) return false;
        State s = state(p);
        if (s.phase == Phase.GUARD && valid(p,s)) return true;
        CombatTransitions.begin(p, false);
        long time = now(p);
        capture(p,s); s.left = false; s.paired = true; s.phase = Phase.GUARD;
        s.until = time+PaleLongswordTuning.BLOCK_START;
        s.guardLength = 0;
        s.guardReady = time+s.guardLength;
        playPair(p,"bb_author_dual_block_start",PaleLongswordTuning.BLOCK_START,PaleLongswordTuning.BLOCK_TRANSITION_TEMPO);
        wear(p,s.main);
        return true;
    }
    public static void releaseGuard(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.phase != Phase.GUARD) return;
        s.phase = Phase.IDLE;
        if (available(p) && valid(p,s)) playPair(p,"bb_author_dual_block_end",PaleLongswordTuning.BLOCK_END,PaleLongswordTuning.BLOCK_TRANSITION_TEMPO);
    }
    private static boolean holding(ServerPlayer p, State s) {
        return p.isUsingItem() && p.getUseItem() == s.main;
    }
    private static Vec3 flat(ServerPlayer p) {
        Vec3 v = p.getLookAngle().multiply(1,0,1);
        return v.lengthSqr() < 1e-5 ? new Vec3(0,0,1) : v.normalize();
    }
    private static boolean facing(ServerPlayer p, DamageSource source) {
        Vec3 pos = source.getSourcePosition();
        if (pos == null) return false;
        Vec3 to = pos.subtract(p.position()).multiply(1,0,1);
        return to.lengthSqr()<1e-4 || flat(p).dot(to.normalize())>=Math.cos(Math.toRadians(PaleLongswordTuning.BLOCK_ARC/2));
    }
    private static void strike(ServerPlayer p, State s) {
        var tune = s.strike;
        Vec3 dir = flat(p);
        for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(tune.reach()))) {
            if (!p.isAlive()) break;
            if (t==p || !t.isAlive() || t.isSpectator() || t.isAlliedTo(p)
                    || (t instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID()))
                    || (t instanceof Player other && !p.canHarmPlayer(other))) continue;
            if (t.getBoundingBox().distanceToSqr(p.getEyePosition())>tune.reach()*tune.reach() || !p.hasLineOfSight(t)) continue;
            Vec3 to = t.position().subtract(p.position()).multiply(1,0,1);
            if (to.lengthSqr()>.01 && dir.dot(to.normalize())<Math.cos(Math.toRadians(tune.arc()/2))) continue;
            if (hurtAs(p,s.left?s.off:s.main,t,p.damageSources().playerAttack(p),s.damage*tune.damage())) {
                t.knockback(tune.knockback(),-dir.x,-dir.z); t.hurtMarked = true;
                p.setLastHurtMob(t);
            }
        }
        Vec3 eye = p.getEyePosition();
        p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK,eye.x+dir.x*1.6,eye.y-.4,eye.z+dir.z*1.6,1,0,0,0,0);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.PLAYER_ATTACK_STRONG,SoundSource.PLAYERS,1f,.75f);
    }
    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register((h,server) -> STATES.remove(h.getPlayer().getUUID()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source) -> {
            if (entity instanceof ServerPlayer p) {
                State s = STATES.get(p.getUUID());
                if (s != null) s.phase = Phase.IDLE;
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount) -> {
            if (passingGuard || !(entity instanceof ServerPlayer p) || amount<=0) return true;
            State s = STATES.get(p.getUUID());
            if (s==null || s.phase!=Phase.GUARD || !available(p) || !valid(p,s) || !holding(p,s)
                    || source.is(DamageTypeTags.BYPASSES_SHIELD) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    || !facing(p,source)) return true;
            passingGuard = true;
            try { p.hurt(source,amount*(1f-PaleLongswordTuning.BLOCK_REDUCTION)); }
            finally { passingGuard = false; }
            p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.SHIELD_BLOCK,SoundSource.PLAYERS,.7f,1.05f);
            return false;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var it = STATES.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                State s = e.getValue();
                if (p==null || p!=s.owner || !p.isAlive()) { it.remove(); continue; }
                if (s.phase==Phase.IDLE) continue;
                if (!available(p) || !valid(p,s)) {
                    boolean guard = s.phase==Phase.GUARD;
                    s.phase=Phase.IDLE;
                    if (guard && p.isUsingItem() && isWeapon(p.getUseItem())) p.stopUsingItem();
                    continue;
                }
                long time = now(p);
                if (s.phase==Phase.STRIKE) {
                    if (!s.hit && time>=s.hitAt) { s.hit=true; strike(p,s); }
                    if (time>=s.until) s.phase=Phase.IDLE;
                } else if (!holding(p,s)) releaseGuard(p);
                else if (time>=s.until) {
                    playPair(p,"bb_author_dual_block_loop",PaleLongswordTuning.BLOCK_LOOP,1f);
                    s.until=time+PaleLongswordTuning.BLOCK_LOOP;
                }
            }
        });
    }
}
