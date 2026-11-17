package com.tarkrakov.tearsofink.weapon.combat;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillInput;
import com.tarkrakov.tearsofink.skill.Skills;
import com.tarkrakov.tearsofink.weapon.WeaponAbilities;
import com.tarkrakov.tearsofink.weapon.forge.Awakening;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-owned phases. No energy cost; continuations bypass the entry cooldown, never its validation. */
public final class PaleGreatswordMoves {
    private static final Archetype ARCH = Archetype.PALE_LINEAR_GREATSWORD;
    private static final TagKey<Block> SOFT = TagKey.create(Registries.BLOCK, TearsOfInk.id("pale_dash_soft"));
    private enum Phase { IDLE, SINGLE_WINDOW, SINGLE_DASH, BACKSTEP, GUARD, PARRY, LEAP, SLAM, CHARGE, DUAL_DASH, RECOVER }
    private static final class State {
        Phase phase = Phase.IDLE;
        long until, started, parryUntil, singleReady, dualReady, chargeReady, backstepReady, guardReady;
        PaleGreatswordBalance.Tuning tuning;
        boolean left, paired, parried, killed, airborne, slammed;
        int broken;
        double travelled, maxDistance;
        float damage, power;
        Vec3 direction, lastPosition;
        ServerLevel level;
        ServerPlayer owner;
        ItemStack main, off;
        final Set<UUID> hit = new HashSet<>();
    }
    private static final Map<UUID, State> STATES = new HashMap<>();
    // This guard lets the original hit be replaced with one smaller hit. Damage events
    // fire again for that nested call, just as they do in ShieldStance.
    private static boolean passingGuardDamage;
    private record DamageWeapon(ServerPlayer player, ItemStack stack) {}
    private static final ThreadLocal<DamageWeapon> DAMAGE_WEAPON = new ThreadLocal<>();

    /** Selected weapon for the existing tier/awakening pipeline during a synchronous hurt call. */
    public static ItemStack damageWeapon(LivingEntity attacker) {
        DamageWeapon context = DAMAGE_WEAPON.get();
        return context != null && context.player == attacker ? context.stack : attacker.getMainHandItem();
    }

    private static boolean hurt(ServerPlayer p, State s, LivingEntity target, float damage) {
        DamageWeapon previous = DAMAGE_WEAPON.get();
        DAMAGE_WEAPON.set(new DamageWeapon(p, s.left ? s.off : s.main));
        try {
            // Each deliberate solo phase is one distinct hit, even immediately after LMB.
            // The phase's target sets/tick gate still prevent repeated per-tick damage.
            int previousInvulnerability = target.invulnerableTime;
            boolean separateHit = s.phase == Phase.SINGLE_WINDOW || s.phase == Phase.SINGLE_DASH || s.phase == Phase.BACKSTEP;
            if (separateHit) target.invulnerableTime = 0;
            boolean hit = target.hurt(p.damageSources().playerAttack(p), damage);
            if (!hit && separateHit) target.invulnerableTime = previousInvulnerability;
            return hit;
        } finally {
            if (previous == null) DAMAGE_WEAPON.remove();
            else DAMAGE_WEAPON.set(previous);
        }
    }

    // Skills performs dimension/vow/resource/wear checks. R cooldown starts at chain end; RMB at entry.
    public static final Skill SINGLE = entry("pale_sweep", false, false);
    private static final Skill PARRY = entry("pale_pair_parry", true, false);
    private static final Skill BACKSTEP = new Skill(TearsOfInk.id("pale_backstep"), 0, 0, 0) {
        @Override public boolean activate(ServerPlayer p, SkillInput input) {
            return begin(p, false, false, false, true);
        }
    };
    private static final Skill CHARGE = entry("pale_pair_charge", true, true);

    private PaleGreatswordMoves() {}

    private static Skill entry(String id, boolean pair, boolean charge) {
        return new Skill(TearsOfInk.id(id), 0, 0, 0) {
            @Override public boolean activate(ServerPlayer p, SkillInput input) {
                return begin(p, pair, charge, input.sneaking(), false);
            }
        };
    }

    public static boolean isWeapon(ItemStack stack) { return stack.is(ModItems.PALE_LINEAR_GREATSWORD); }
    public static boolean dual(Player p) { return isWeapon(p.getMainHandItem()) && isWeapon(p.getOffhandItem()); }
    private static State state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), id -> new State()); }
    private static long now(ServerPlayer p) { return p.server.getTickCount(); }

    public static boolean locked(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        return s != null && s.phase != Phase.IDLE && s.phase != Phase.SINGLE_WINDOW
                && !(s.phase == Phase.PARRY && now(p) > s.parryUntil);
    }

    private static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isPassenger() && !p.isSleeping()
                && com.tarkrakov.tearsofink.ink.InkPowers.get(p).inside()
                && !com.tarkrakov.tearsofink.weapon.blood.Parry.active(p);
    }

    public static void init() {
        PaleGreatswordBalance.init();
        ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> STATES.remove(h.getPlayer().getUUID()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p) {
                State s = STATES.get(p.getUUID());
                if (s != null) s.phase = Phase.IDLE;
                // Removal belongs to the tick iterator. A dash may kill another player
                // whose entry is in this same map; mutating it from hurt would invalidate it.
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer p)) return true;
            State s = STATES.get(p.getUUID());
            net.minecraft.world.entity.Entity attacker = source.getEntity() != null
                    ? source.getEntity() : source.getDirectEntity();
            // Shift+R is a damage-reducing block, NOT a full parry: it never
            // triggers the counter or cancels the hit's knockback. Only incoming
            // attacks in front of the guarding blades are reduced.
            if (!passingGuardDamage && s != null && s.phase == Phase.GUARD && now(p) < s.until
                    && available(p) && valid(p, s) && amount > 0
                    && !source.is(DamageTypeTags.BYPASSES_SHIELD)
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    && guardFacing(p, source)) {
                var guard = s.tuning.shiftGuard();
                float reduction = s.paired ? guard.dualReduction() : guard.singleReduction();
                passingGuardDamage = true;
                try {
                    p.hurt(source, amount * (1f - reduction));
                } finally {
                    passingGuardDamage = false;
                }
                p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.65f, s.paired ? 0.95f : 1.1f);
                return false;
            }
            // Existing full parry remains exclusive to ordinary R with a pair.
            // Environmental fall/void damage must not trigger it.
            if (s == null || s.phase != Phase.PARRY || s.parried || now(p) > s.parryUntil
                    || !valid(p, s) || amount <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    || attacker == null || attacker == p || attacker.isAlliedTo(p)) return true;
            s.parried = true;
            play(p, s, "dual_repel", "dual_repel_off", 10);
            area(p, s, 360, s.tuning.dualR().repelReach(),
                    s.tuning.dualR().repelDamageMultiplier(), s.tuning.dualR().repelKnockback());
            // Same recognisable clang as the Dussak parry, without touching its sounds.
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.45f, 1.9f);
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.6f);
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                    p.getX(), p.getY() + 1, p.getZ(), 8, 0.8, 0.3, 0.8, 0.02);
            return false;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var it = STATES.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                State s = e.getValue();
                if (p == null || !p.isAlive() || (s.owner != null && s.owner != p)) { it.remove(); continue; }
                if (s.phase == Phase.IDLE) continue;
                if (!available(p) || !valid(p, s)) { cancel(p, s); continue; }
                long time = now(p);
                switch (s.phase) {
                    case SINGLE_WINDOW -> { if (time > s.until) finish(p, s, false); }
                    case PARRY -> { if (time > s.until) finish(p, s, true); }
                    case CHARGE -> {
                        if (!p.isUsingItem() && time - s.started > 3) cancel(p, s);
                        else if (time - s.started >= 50) p.releaseUsingItem();
                    }
                    case SINGLE_DASH, DUAL_DASH -> dashTick(p, s);
                    case BACKSTEP -> {
                        var b = s.tuning.backstep();
                        if (!s.slammed && time >= s.started + b.hitTicks()) {
                            s.slammed = true;
                            area(p, s, b.sweep());
                            if (p.isAlive()) {
                                Vec3 back = flat(p).scale(-b.backwardSpeed());
                                p.setDeltaMovement(back.x, p.onGround() ? b.upwardSpeed() : p.getDeltaMovement().y, back.z);
                                p.hurtMarked = true; // normal synchronized velocity; vanilla collision handles walls
                                p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                                        net.minecraft.sounds.SoundSource.PLAYERS, .8f, 1.1f);
                            }
                        }
                        if (time >= s.until) s.phase = Phase.IDLE;
                    }
                    case LEAP -> {
                        // Prevent fall damage from this leap only; no immunity is kept after landing/cancel.
                        p.fallDistance = 0;
                        if (!p.onGround()) s.airborne = true;
                        if (time - s.started >= 4 && s.airborne && p.onGround()) {
                            play(p, s, "dual_slam", "dual_slam_off", 14);
                            s.phase = Phase.SLAM; s.started = time; s.until = time + 14; s.slammed = false;
                        } else if (time > s.until) finish(p, s, true); // void/flying/water: no remote explosion
                    }
                    case SLAM -> {
                        if (!s.slammed && time >= s.started + 4) {
                            s.slammed = true;
                            if (p.onGround()) {
                                area(p, s, 360, s.tuning.dualR().slamReach(),
                                        s.tuning.dualR().slamDamageMultiplier(), s.tuning.dualR().slamKnockback());
                                p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                                        net.minecraft.sounds.SoundSource.PLAYERS, 0.75f, 0.85f);
                                p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                                        p.getX(), p.getY() + 0.1, p.getZ(), 26, 1.4, 0.15, 1.4, 0.08);
                                p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                                        p.getX(), p.getY() + 0.2, p.getZ(), 6, 1.2, 0.1, 1.2, 0);
                            }
                        }
                        if (time >= s.until) s.phase = Phase.IDLE;
                    }
                    case RECOVER, GUARD -> { if (time >= s.until) s.phase = Phase.IDLE; }
                    default -> {}
                }
            }
        });
    }

    /** Shift+R: a short frontal block, independent of the offensive R chain. */
    public static void guard(ServerPlayer p) {
        if (!available(p)) return;
        boolean main = isWeapon(p.getMainHandItem());
        boolean off = isWeapon(p.getOffhandItem());
        if (!main && !off) return;
        // A different two-handed weapon in the main hand occupies the offhand.
        if (!main && com.tarkrakov.tearsofink.weapon.InkGreatswordItem.twoHanded(p.getMainHandItem())) return;
        State s = state(p);
        if (s.phase != Phase.IDLE && !valid(p, s)) cancel(p, s);
        if (s.phase != Phase.IDLE || now(p) < s.guardReady) return;
        var tune = PaleGreatswordBalance.get();
        var block = tune.shiftGuard();
        boolean pair = main && off;
        boolean left = !main;
        int duration = pair ? block.dualDurationTicks() : block.singleDurationTicks();
        int cooldown = pair ? block.dualCooldownTicks() : block.singleCooldownTicks();

        s.tuning = tune;
        s.owner = p; s.level = p.serverLevel();
        s.main = p.getMainHandItem(); s.off = p.getOffhandItem();
        s.left = left; s.paired = pair; s.started = now(p);
        s.phase = Phase.GUARD; s.until = now(p) + duration;
        s.guardReady = cooldown(p, s, cooldown);
        play(p, s, pair ? "dual_guard" : "guard", pair ? "dual_guard_off" : null, duration);
        // Free ordinary technique; the existing weapon-wear economy still applies.
        ItemStack sword = left ? s.off : s.main;
        com.tarkrakov.tearsofink.weapon.forge.Gear.wear(p, sword,
                com.tarkrakov.tearsofink.weapon.forge.Gear.WEAR_ABILITY);
        Awakening.abilityUsed(p);
        p.serverLevel().playSound(null, p.blockPosition(),
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.65f, pair ? 0.9f : 1.1f);
    }

    private static boolean guardFacing(ServerPlayer p, net.minecraft.world.damagesource.DamageSource source) {
        Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos == null) return false; // no protection against fire, falling or the void
        Vec3 to = sourcePos.subtract(p.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1.0e-4) return true;
        State s = STATES.get(p.getUUID());
        if (s == null) return false;
        double arc = s.paired ? s.tuning.shiftGuard().dualArc() : s.tuning.shiftGuard().singleArc();
        return flat(p).dot(to.normalize()) >= Math.cos(Math.toRadians(arc / 2));
    }

    public static void useR(ServerPlayer p, boolean left) {
        if (!available(p) || (dual(p) && left)) return;
        State s = state(p);
        if (s.phase != Phase.IDLE && !valid(p, s)) cancel(p, s);
        if (s.phase == Phase.SINGLE_WINDOW && s.left == left && now(p) <= s.until) {
            s.phase = Phase.SINGLE_DASH;
            startDash(p, s, s.tuning.singleR().dashDistance(), s.tuning.singleR().dashDamageMultiplier());
            play(p, s, "rush", null, s.tuning.singleR().dashTicks());
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_1.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.85f);
            return;
        }
        if (s.phase == Phase.PARRY && dual(p) && now(p) <= s.until) {
            s.phase = Phase.LEAP; s.started = now(p); s.until = now(p) + 50;
            s.airborne = false;
            // The second R is committed: start cooldown now, irrespective of landing/interruptions.
            s.dualReady = cooldown(p, s, s.tuning.dualR().cooldownTicks());
            Vec3 d = flat(p);
            p.setDeltaMovement(d.x * 0.3, 1.05, d.z * 0.3);
            p.hurtMarked = true;
            play(p, s, "dual_leap", "dual_leap_off", 50);
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_1.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.1f);
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    p.getX(), p.getY() + 0.15, p.getZ(), 12, 0.5, 0.1, 0.5, 0.08);
            return;
        }
        Skills.activate(p, dual(p) ? PARRY : SINGLE, new SkillInput(0, 0, left));
    }

    public static boolean charge(ServerPlayer p) {
        return available(p) && dual(p) && Skills.activate(p, CHARGE, new SkillInput(0, 0, false));
    }

    public static boolean backstep(ServerPlayer p) {
        return available(p) && !dual(p) && isWeapon(p.getMainHandItem())
                && Skills.activate(p, BACKSTEP, new SkillInput(0, 0, false));
    }

    private static boolean begin(ServerPlayer p, boolean pair, boolean charge, boolean left, boolean backstep) {
        if (!available(p) || (pair ? !dual(p) : !isWeapon(left ? p.getOffhandItem() : p.getMainHandItem()))) return false;
        State s = state(p);
        if (backstep && now(p) >= s.backstepReady && s.phase == Phase.SINGLE_WINDOW) finish(p, s, false);
        if (s.phase != Phase.IDLE) return false;
        if (now(p) < (backstep ? s.backstepReady : charge ? s.chargeReady : pair ? s.dualReady : s.singleReady)) return false;
        s.tuning = PaleGreatswordBalance.get();
        s.owner = p; s.level = p.serverLevel(); s.main = p.getMainHandItem(); s.off = p.getOffhandItem();
        s.left = left; s.paired = pair; s.parried = false; s.started = now(p);
        s.power = pair ? 1 : WeaponAbilities.power(p);
        // Read the selected sword's attributes, not a different weapon in the main hand.
        final double[] base = {p.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)};
        ItemStack weapon = left ? s.off : s.main;
        weapon.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) base[0] += modifier.amount();
        });
        s.damage = (float) (left ? base[0] : p.getAttributeValue(Attributes.ATTACK_DAMAGE)) * s.power;
        if (backstep) {
            var b = s.tuning.backstep();
            s.phase = Phase.BACKSTEP; s.until = now(p) + b.durationTicks(); s.slammed = false;
            s.backstepReady = now(p) + Math.round(b.cooldownTicks() * Awakening.cooldownMult(s.main));
            play(p, s, "backstep", null, b.durationTicks());
        } else if (charge) {
            s.phase = Phase.CHARGE;
            play(p, s, "dual_charge", "dual_charge_off", 52);
            // Hunter axe charging uses this existing iron-windup sound.
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.65f);
        } else if (pair) {
            var parry = s.tuning.dualR();
            s.phase = Phase.PARRY;
            s.parryUntil = now(p) + parry.parryTicks();
            s.until = now(p) + parry.followupWindowTicks();
            // Preserve the authored 26-tick animation; only the effective parry guard is shorter.
            play(p, s, "dual_parry", "dual_parry_off", 26);
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.55f, 1.25f);
        } else {
            s.phase = Phase.SINGLE_WINDOW; s.until = now(p) + s.tuning.singleR().windowTicks();
            play(p, s, "ability", null, s.tuning.singleR().sweepTicks());
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.1f, 0.65f);
            area(p, s, s.tuning.singleR().sweep());
        }
        return true;
    }

    public static void release(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.phase != Phase.CHARGE || !available(p) || !valid(p, s)) return;
        float charge = Mth.clamp((now(p) - s.started) / 50f, 0, 1);
        s.phase = Phase.DUAL_DASH;
        var charged = s.tuning.dualCharge();
        s.chargeReady = cooldown(p, s, charged.cooldownTicks());
        startDash(p, s, charged.dashDistance(),
                Mth.lerp(charge, charged.minDamageMultiplier(), charged.maxDamageMultiplier()));
        play(p, s, "dual_rush", "dual_rush_off", 18);
        p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_1.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.85f);
    }

    private static void startDash(ServerPlayer p, State s, double distance, float mult) {
        // Dash is horizontal like AxeMoves.RUSH. Pitch must not push the player's hitbox into the floor.
        s.direction = flat(p); s.travelled = 0; s.maxDistance = distance;
        s.broken = 0; s.killed = false; s.hit.clear(); s.started = now(p);
        s.lastPosition = p.position();
        s.damage *= mult;
    }

    private static boolean valid(ServerPlayer p, State s) {
        return p.serverLevel() == s.level && p.getMainHandItem() == s.main && p.getOffhandItem() == s.off
                && (!s.paired || dual(p));
    }

    private static void play(ServerPlayer p, State s, String main, String off, int ticks) {
        Combat.play(p, ARCH, s.left ? null : main, s.left ? main + "_off" : off, ticks);
    }

    private static void cancel(ServerPlayer p, State s) {
        if (s.phase == Phase.SINGLE_WINDOW || s.phase == Phase.SINGLE_DASH) finish(p, s, false);
        else if (s.phase == Phase.PARRY || s.phase == Phase.LEAP || s.phase == Phase.SLAM) finish(p, s, true);
        else if (s.phase == Phase.CHARGE) s.chargeReady = cooldown(p, s, s.tuning.dualCharge().cooldownTicks());
        Combat.play(p, ARCH, "recover", s.paired || s.left ? "recover" : null, 4);
        s.phase = Phase.IDLE;
        if (p.isUsingItem() && isWeapon(p.getUseItem())) p.stopUsingItem();
        p.setDeltaMovement(0, p.getDeltaMovement().y, 0); p.hurtMarked = true;
    }

    private static long cooldown(ServerPlayer p, State s, int ticks) {
        int duration = Math.round(ticks * Awakening.cooldownMult(s.left ? s.off : s.main));
        p.getCooldowns().addCooldown(ModItems.PALE_LINEAR_GREATSWORD, duration);
        return now(p) + duration;
    }

    private static void finish(ServerPlayer p, State s, boolean pair) {
        if (pair) {
            if (s.dualReady <= now(p)) s.dualReady = cooldown(p, s, s.tuning.dualR().cooldownTicks());
        } else s.singleReady = cooldown(p, s, s.tuning.singleR().cooldownTicks());
        s.phase = Phase.IDLE;
    }

    /**
     * Use the same synchronized player velocity as the hunter axes rather than moving the
     * server player by setPos+teleport every tick (which the real client can immediately undo).
     * Server checks the next swept volume; actual travel is measured next tick so distance
     * and the existing soft-block/collision safeguards remain authoritative.
     */
    private static void dashTick(ServerPlayer p, State s) {
        Vec3 current = p.position();
        double moved = current.subtract(s.lastPosition).dot(s.direction);
        if (moved < -0.5 || moved > 2.0 || now(p) - s.started > 22) {
            endDash(p, s);
            return;
        }
        s.travelled += Math.max(0, moved);
        s.lastPosition = current;
        double remaining = s.maxDistance - s.travelled;
        if (remaining <= 0.08) { endDash(p, s); return; }

        // Prevent passing the wall or breaking protected terrain before applying impulse.
        double speed = Math.min(0.72, remaining);
        AABB next = p.getBoundingBox().move(s.direction.scale(speed));
        if (!clearPath(p, s, next)) { endDash(p, s); return; }
        // Test future occupied volume, never damage the same target twice per dash.
        for (LivingEntity target : p.serverLevel().getEntitiesOfClass(LivingEntity.class, next.inflate(0.3))) {
            if (!enemy(p, target) || s.hit.contains(target.getUUID()) || !p.hasLineOfSight(target)) continue;
            s.hit.add(target.getUUID());
            if (hurt(p, s, target, s.damage) && target.isDeadOrDying()
                    && s.phase == Phase.DUAL_DASH && !s.killed) {
                s.killed = true;
                s.chargeReady = now(p) + Math.max(0, s.chargeReady - now(p)) / 2;
                p.getCooldowns().addCooldown(ModItems.PALE_LINEAR_GREATSWORD, (int) (s.chargeReady - now(p)));
            }
            if (!p.isAlive()) return;
        }
        p.setDeltaMovement(s.direction.x * speed, Math.min(0.1, p.getDeltaMovement().y), s.direction.z * speed);
        p.hurtMarked = true; // vanilla velocity packet: same path used by AxeMoves
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                p.getX(), p.getY() + 0.4, p.getZ(), 2, 0.12, 0.12, 0.12, 0.01);
    }

    private static boolean clearPath(ServerPlayer p, State s, AABB box) {
        ServerLevel level = p.serverLevel();
        if (!level.getWorldBorder().isWithinBounds(box) || box.minY < level.getMinBuildHeight()
                || box.maxY > level.getMaxBuildHeight()
                || !level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) return false;
        // Use the same full bounds as noCollision: an epsilon here can skip a soft block
        // while vanilla still sees a tiny overlap (the player's width is a float).
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var block = level.getBlockState(pos);
            var shape = block.getCollisionShape(level, pos);
            if (shape.isEmpty() || shape.toAabbs().stream().noneMatch(b -> b.move(pos).intersects(box))) continue;
            if (s.phase != Phase.DUAL_DASH || s.broken >= 3 || !block.is(SOFT) || block.hasBlockEntity()
                    || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                    || !level.mayInteract(p, pos) || !p.mayUseItemAt(pos, Direction.UP, s.main)
                    || !p.gameMode.destroyBlock(pos)) return false;
            if (++s.broken >= 3) return false;
        }
        return level.noCollision(p, box);
    }

    private static void endDash(ServerPlayer p, State s) {
        boolean pair = s.phase == Phase.DUAL_DASH;
        Vec3 velocity = p.getDeltaMovement();
        p.setDeltaMovement(0, velocity.y, 0); p.hurtMarked = true;
        if (!pair) s.singleReady = cooldown(p, s, s.tuning.singleR().cooldownTicks());
        s.phase = Phase.RECOVER; s.until = now(p) + 5;
        play(p, s, pair ? "dual_recover" : "recover", pair ? "dual_recover_off" : null, 5);
    }

    private static boolean enemy(ServerPlayer p, LivingEntity t) {
        return t != p && t.isAlive() && !t.isSpectator() && !t.isAlliedTo(p)
                && !(t instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID()))
                && (!(t instanceof Player other) || p.canHarmPlayer(other));
    }

    private static Vec3 flat(ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 1e-5 ? new Vec3(0, 0, 1) : look.normalize();
    }

    private static void area(ServerPlayer p, State s, PaleGreatswordBalance.Sweep sweep) {
        area(p, s, sweep.arc(), sweep.reach(), sweep.damageMultiplier(), sweep.knockback());
    }

    private static void area(ServerPlayer p, State s, double arc, double reach, double mult, double knock) {
        Vec3 forward = flat(p);
        for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(reach))) {
            if (!p.isAlive()) return;
            if (!enemy(p, t) || t.getBoundingBox().distanceToSqr(p.getEyePosition()) > reach * reach || !p.hasLineOfSight(t)) continue;
            Vec3 to = t.position().subtract(p.position()).multiply(1, 0, 1);
            if (arc < 360 && to.lengthSqr() > 0.01 && to.normalize().dot(forward) < Math.cos(Math.toRadians(arc / 2))) continue;
            if (mult == 0 || hurt(p, s, t, (float) (s.damage * mult))) {
                Vec3 dir = arc >= 360 && to.lengthSqr() > 0.01 ? to.normalize() : forward;
                t.knockback(knock, -dir.x, -dir.z);
                t.hurtMarked = true;
            }
        }
    }
}
