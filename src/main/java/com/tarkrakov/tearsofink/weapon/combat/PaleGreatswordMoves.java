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
    private enum Phase { IDLE, SINGLE_WINDOW, SINGLE_DASH, BACKSTEP, GUARD, LEAP_START, LEAP, SLAM, CHARGE, DUAL_DASH, RECOVER }
    private static final class State {
        Phase phase = Phase.IDLE;
        long until, started, nextLeapLoop, singleReady, dualReady, chargeReady, backstepReady, guardReady;
        int singleLength, dualLength, chargeLength, guardLength;
        PaleGreatswordBalance.Tuning tuning;
        boolean left, paired, killed, airborne, slammed;
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

    // Skills performs dimension/vow/resource/wear checks. Solo R cooldown starts at chain end; dual R at entry.
    public static final Skill SINGLE = entry("pale_sweep", false, false);
    private static final Skill DUAL_R = entry("pale_pair_parry", true, false); // stable legacy skill ID
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
        return s != null && s.phase != Phase.IDLE && s.phase != Phase.SINGLE_WINDOW;
    }

    public static boolean ownsSkill(Skill skill) { return skill == SINGLE || skill == DUAL_R || skill == CHARGE || skill == BACKSTEP; }
    public static void cancelForAction(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s != null && s.phase != Phase.IDLE) cancel(p,s);
    }
    private static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isPassenger() && !p.isSleeping()
                && com.tarkrakov.tearsofink.ink.InkPowers.get(p).inside();
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
            if (s != null && s.phase == Phase.GUARD
                    && available(p) && valid(p, s) && amount > 0 && PaleParry.active(now(p), s.until)
                    && !source.is(DamageTypeTags.BYPASSES_SHIELD)
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    && guardFacing(p, source)) {
                lowerGuard(p, s);
                PaleParry.success(p);
                return false;
            }
            return true;
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
                    case SINGLE_WINDOW -> {
                        // Серверный урон совпадает с пересечением клинком цели, не с нажатием R.
                        if (!s.slammed && time >= s.started + 8) {
                            s.slammed = true;
                            area(p, s, s.tuning.singleR().sweep());
                        }
                        if (time > s.until) {
                            // Авторский swing самостоятельно отпускает held-позу; повторный
                            // Combat.play здесь мог стереть уже начатый игроком удар ЛКМ.
                            finish(p, s, false);
                        }
                    }
                    case LEAP_START -> {
                        if (time >= s.until) {
                            if (!p.onGround() || p.isInWater() || p.isFallFlying()) { cancel(p, s); continue; }
                            launchLeap(p, s, time);
                        }
                    }
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
                            play(p, s, "bb_author_dual_r_end", "bb_author_dual_r_end_off", 15);
                            s.phase = Phase.SLAM; s.started = time; s.until = time + 15; s.slammed = false;
                        } else if (time >= s.until || p.isInWater() || p.isFallFlying()) {
                            cancel(p, s); // no remote explosion or lingering pose
                        } else if (time >= s.nextLeapLoop) {
                            play(p, s, "bb_author_dual_r_loop", "bb_author_dual_r_loop_off", 20);
                            s.nextLeapLoop = time + 20;
                        }
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
                    case GUARD -> { if (time >= s.until) lowerGuard(p, s); }
                    case RECOVER -> { if (time >= s.until) s.phase = Phase.IDLE; }
                    default -> {}
                }
            }
        });
    }

    /** Solo RMB opens a fixed parry window; dual RMB remains the charge attack. */
    public static boolean holdGuard(ServerPlayer p) {
        return isWeapon(p.getMainHandItem()) && !dual(p) && beginParry(p);
    }

    /** Dual Shift+R opens a parry, not a toggle. */
    public static void guard(ServerPlayer p) { beginParry(p); }

    private static boolean beginParry(ServerPlayer p) {
        if (!available(p)) return false;
        boolean main = isWeapon(p.getMainHandItem());
        boolean off = isWeapon(p.getOffhandItem());
        if (!main && !off) return false;
        State s = state(p);
        if (s.phase != Phase.IDLE && !valid(p, s)) cancel(p, s);
        if (!PaleParry.tryStart(p)) return false;
        CombatTransitions.begin(p, false);
        var tune = PaleGreatswordBalance.get();
        boolean pair = main && off;
        boolean left = !main;

        s.tuning = tune;
        s.owner = p; s.level = p.serverLevel();
        s.main = p.getMainHandItem(); s.off = p.getOffhandItem();
        s.left = left; s.paired = pair; s.started = now(p);
        s.phase = Phase.GUARD; s.until = now(p) + PaleParry.WINDOW_TICKS;
        play(p, s, pair ? "bb_author_dual_guard_start" : left ? "guard" : "bb_author_guard_start",
                pair ? "bb_author_dual_guard_start_off" : null, PaleParry.WINDOW_TICKS);
        // Free ordinary technique; the existing weapon-wear economy still applies.
        ItemStack sword = left ? s.off : s.main;
        com.tarkrakov.tearsofink.weapon.forge.Gear.wear(p, sword,
                com.tarkrakov.tearsofink.weapon.forge.Gear.WEAR_ABILITY);
        Awakening.abilityUsed(p);
        p.serverLevel().playSound(null, p.blockPosition(),
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.65f, pair ? 0.9f : 1.1f);
        return true;
    }

    public static void cancelParryForSwing(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s != null && s.phase == Phase.GUARD) lowerGuard(p,s);
    }

    private static void lowerGuard(ServerPlayer p, State s) {
        s.phase = Phase.IDLE; // protection ends immediately; return pose can blend into another action
        if (available(p) && valid(p,s)) play(p, s, s.paired ? "bb_author_dual_guard_end" : s.left ? "guard_release" : "bb_author_guard_end",
                s.paired ? "bb_author_dual_guard_end_off" : null, s.paired || s.left ? 10 : 4);
        if (!s.paired && !s.left && p.isUsingItem() && p.getUseItem() == s.main) p.stopUsingItem();
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
            // Второй R не может отменить ещё не произошедшее попадание первой фазы.
            if (now(p) < s.started + 8) return;
            s.phase = Phase.SINGLE_DASH;
            startDash(p, s, s.tuning.singleR().dashDistance(), s.tuning.singleR().dashDamageMultiplier());
            play(p, s, s.left ? "rush" : "bb_author_r_dash", null, s.tuning.singleR().dashTicks());
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_1.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.85f);
            return;
        }
        Skills.activate(p, dual(p) ? DUAL_R : SINGLE, new SkillInput(0, 0, left));
    }

    private static void launchLeap(ServerPlayer p, State s, long time) {
        var leap = s.tuning.dualR();
        s.phase = Phase.LEAP; s.started = time; s.until = time + 50;
        s.airborne = false; s.nextLeapLoop = time + 20;
        Vec3 d = flat(p).scale(leap.forwardSpeed());
        p.setDeltaMovement(d.x, leap.upwardSpeed(), d.z);
        p.hurtMarked = true;
        play(p, s, "bb_author_dual_r_loop", "bb_author_dual_r_loop_off", 20);
        p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_1.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.1f);
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                p.getX(), p.getY() + 0.15, p.getZ(), 12, 0.5, 0.1, 0.5, 0.08);
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
        if (pair && !charge && (!p.onGround() || p.isInWater() || p.isFallFlying())) return false;
        if (now(p) < (backstep ? s.backstepReady : charge ? s.chargeReady : pair ? s.dualReady : s.singleReady)) return false;
        if (charge && s.phase == Phase.CHARGE) return false;
        if (!pair && !backstep && (s.phase == Phase.SINGLE_WINDOW || s.phase == Phase.SINGLE_DASH)) return false;
        CombatTransitions.begin(p, false);
        if (now(p) < (backstep ? s.backstepReady : charge ? s.chargeReady : pair ? s.dualReady : s.singleReady)) return false;
        s.tuning = PaleGreatswordBalance.get();
        s.owner = p; s.level = p.serverLevel(); s.main = p.getMainHandItem(); s.off = p.getOffhandItem();
        s.left = left; s.paired = pair; s.started = now(p);
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
            s.phase = Phase.LEAP_START; s.until = now(p) + 25;
            s.dualReady = hudCooldown(p, s, s.tuning.dualR().cooldownTicks(), 1);
            play(p, s, "bb_author_dual_r_start", "bb_author_dual_r_start_off", 25);
        } else {
            s.phase = Phase.SINGLE_WINDOW; s.until = now(p) + s.tuning.singleR().windowTicks();
            s.slammed = false;
            play(p, s, s.left ? "ability" : "bb_author_r_swing", null, s.tuning.singleR().sweepTicks());
            p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.1f, 0.65f);
        }
        return true;
    }

    public static void release(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.phase != Phase.CHARGE || !available(p) || !valid(p, s)) return;
        float charge = Mth.clamp((now(p) - s.started) / 50f, 0, 1);
        s.phase = Phase.DUAL_DASH;
        var charged = s.tuning.dualCharge();
        s.chargeReady = hudCooldown(p, s, charged.cooldownTicks(), 2);
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
        if (s.phase == Phase.GUARD) { lowerGuard(p, s); return; }
        if (s.phase == Phase.SINGLE_WINDOW || s.phase == Phase.SINGLE_DASH) finish(p, s, false);
        else if (s.phase == Phase.LEAP_START || s.phase == Phase.LEAP || s.phase == Phase.SLAM) finish(p, s, true);
        else if (s.phase == Phase.CHARGE) s.chargeReady = hudCooldown(p, s, s.tuning.dualCharge().cooldownTicks(), 2);
        Combat.play(p, ARCH, !s.paired && !s.left ? "bb_author_r_recover" : "recover", s.paired || s.left ? "recover" : null, 4);
        s.phase = Phase.IDLE;
        if (p.isUsingItem() && isWeapon(p.getUseItem())) p.stopUsingItem();
        p.setDeltaMovement(0, p.getDeltaMovement().y, 0); p.hurtMarked = true;
    }

    // Read-only HUD view of the actual independent technique timers.
    public static int[] hudTimer(ServerPlayer p, int action, boolean left) {
        State s = STATES.get(p.getUUID());
        if (action == 1) return PaleParry.hud(p, s != null && valid(p,s) && available(p) && s.phase == Phase.GUARD && PaleParry.active(now(p), s.until));
        if (s == null) return new int[]{0, 1, action == 0 && dual(p) && (!p.onGround() || p.isInWater() || p.isFallFlying()) ? 1 : 0};
        boolean pair = dual(p);
        long ready = action == 1 ? s.guardReady : action == 2 ? s.chargeReady : pair ? s.dualReady : s.singleReady;
        int length = action == 1 ? s.guardLength : action == 2 ? s.chargeLength : pair ? s.dualLength : s.singleLength;
        int state = 0;
        if (valid(p, s) && s.phase != Phase.IDLE) {
            if (action == 0 && s.phase == Phase.SINGLE_WINDOW && s.left == left && now(p) >= s.started + 8 && now(p) <= s.until) state = 2;
            else if ((action == 1 && s.phase == Phase.GUARD) || (action == 2 && s.phase == Phase.CHARGE)) state = 2;
            else if ((action == 0 && (s.phase == Phase.SINGLE_WINDOW || s.phase == Phase.SINGLE_DASH || s.phase == Phase.LEAP_START || s.phase == Phase.LEAP || s.phase == Phase.SLAM)) || (action == 2 && s.phase == Phase.DUAL_DASH)) state = 1;
        }
        if (action == 0 && pair && (!p.onGround() || p.isInWater() || p.isFallFlying()) && state == 0) state = 1;
        return new int[]{(int)Math.max(0, ready - now(p)), Math.max(1, length), state};
    }

    private static long hudCooldown(ServerPlayer p, State s, int ticks, int action) {
        long ready = cooldown(p, s, ticks);
        int length = (int)(ready - now(p));
        if (action == 0) s.singleLength = length;
        else if (action == 1) s.dualLength = length;
        else s.chargeLength = length;
        return ready;
    }

    private static long cooldown(ServerPlayer p, State s, int ticks) {
        int duration = Math.round(ticks * Awakening.cooldownMult(s.left ? s.off : s.main));
        return now(p) + duration;
    }

    private static void finish(ServerPlayer p, State s, boolean pair) {
        if (pair) {
            if (s.dualReady <= now(p)) s.dualReady = hudCooldown(p, s, s.tuning.dualR().cooldownTicks(), 1);
        } else s.singleReady = hudCooldown(p, s, s.tuning.singleR().cooldownTicks(), 0);
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
        if (!pair) s.singleReady = hudCooldown(p, s, s.tuning.singleR().cooldownTicks(), 0);
        s.phase = Phase.RECOVER; s.until = now(p) + 5;
        play(p, s, pair ? "dual_recover" : s.left ? "recover" : "bb_author_r_recover",
                pair ? "dual_recover_off" : null, 10);
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
