package com.tarkrakov.tearsofink.entity.orion;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tarkrakov.tearsofink.entity.orion.OrionCombat.*;

public final class OrionEntity extends PathfinderMob {
    private static final EntityDataAccessor<CompoundTag> PERFORMANCE =
            SynchedEntityData.defineId(OrionEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(OrionEntity.class, EntityDataSerializers.INT);
    private static final List<Clip> DODGES = List.of(Clip.DODGE_LEFT, Clip.DODGE_RIGHT, Clip.DODGE_BACK);

    private final ServerBossEvent boss = new ServerBossEvent(getType().getDescription(),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private final EnumMap<Technique, Long> readyAt = new EnumMap<>(Technique.class);
    private final Map<UUID, Threat> threat = new HashMap<>();
    private final List<HitWindow> hit = List.of(new HitWindow(), new HitWindow(), new HitWindow());
    private final DeadlyOpening opening = new DeadlyOpening();
    private Clip clip = Clip.IDLE;
    private long started, recoverUntil, longReadyAt, lastContact, lastChoice, lastDamage = -100;
    private float rate = 1;
    private float previousTime;
    private final OrionAnimationClock clientClock = new OrionAnimationClock();
    private Performance clientPerformance;
    private Combo combo;
    private int step;
    private Technique technique;
    private ServerPlayer opponent;
    private Vec3 travel = Vec3.ZERO;
    private Wave wave;
    private List<Vec3> detour = List.of();
    private int detourIndex;
    private UUID detourTarget;
    private Vec3 plannedTarget;
    private long repathAt, escapeUntil;
    private boolean blockedMovement;
    private double techniqueTravel;
    private long linkUntil, repositionUntil;
    private boolean fighting, phaseTwo, pendingEmpower, inDamage, pendingLethal, finaleComplete;
    private String choice = "waiting";
    private static final String LAST_STAND = "entity.tears-of-ink.orion.last_stand";

    private record Threat(float points, long time) {}
    private record Wave(Vec3 origin, Vec3 forward, Vec3 lateral, long launch, boolean radiant, HitWindow hit) {}

    public record Performance(Clip clip, long start, float rate, Clip previous, long previousStart,
                              boolean phaseTwo, float fromTick, float toTick, float previousSeconds) {
        public float elapsedTicks(long tick, float partial) {
            if (fromTick >= 0) return Mth.lerp(Mth.clamp(partial, 0, 1), fromTick, toTick);
            return Math.max(0, tick - start + partial);
        }
        public float seconds(long tick, float partial) { return elapsedTicks(tick, partial) * rate / 20; }
    }

    public OrionEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, TOUGHNESS).add(Attributes.KNOCKBACK_RESISTANCE, .88)
                .add(Attributes.FOLLOW_RANGE, 64).add(Attributes.MOVEMENT_SPEED, RUN_SPEED);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PERFORMANCE, new CompoundTag());
        builder.define(ACTION_TICKS, 0);
    }

    public Performance performance() {
        return clientPerformance != null ? clientPerformance : trackedPerformance();
    }

    private Performance trackedPerformance() {
        CompoundTag state = entityData.get(PERFORMANCE);
        if (state.isEmpty()) return new Performance(Clip.IDLE, 0, 1, Clip.IDLE, 0, false, -1, -1, 0);
        Clip synced = Clip.values()[state.getInt("clip")];
        long start = state.getLong("start");
        long previousStart = state.getLong("previousStart");
        float previousSeconds = Math.max(0, (start - previousStart) * state.getFloat("previousRate") / 20);
        int elapsed = synced.usesCursor() ? entityData.get(ACTION_TICKS) : -1;
        return new Performance(synced, start, state.getFloat("rate"), Clip.values()[state.getInt("previous")],
                previousStart, state.getBoolean("phaseTwo"), elapsed, elapsed, previousSeconds);
    }

    private void tickClientAnimation() {
        Performance tracked = trackedPerformance();
        Performance former = clientPerformance;
        boolean changed = former == null || former.clip() != tracked.clip() || former.start() != tracked.start();
        float previousSeconds = tracked.previousSeconds();
        if (!changed) previousSeconds = former.previousSeconds();
        else if (former != null && tracked.previous() == former.clip() && tracked.previousStart() == former.start())
            previousSeconds = former.seconds(level().getGameTime() - 1, 1);

        if (changed) clientClock.reset(tracked.toTick());
        else if (tracked.clip().usesCursor()) clientClock.advance(tracked.toTick());
        clientPerformance = new Performance(tracked.clip(), tracked.start(), tracked.rate(), tracked.previous(),
                tracked.previousStart(), tracked.phaseTwo(), clientClock.previous(), clientClock.current(), previousSeconds);
    }

    private void play(Clip next) {
        CompoundTag state = new CompoundTag();
        state.putInt("previous", clip.ordinal());
        state.putLong("previousStart", started);
        state.putFloat("previousRate", rate);
        clip = next;
        started = level().getGameTime();
        rate = clip.rate(phaseTwo);
        previousTime = 0;
        state.putInt("clip", clip.ordinal());
        state.putLong("start", started);
        state.putFloat("rate", rate);
        state.putBoolean("phaseTwo", phaseTwo);
        entityData.set(PERFORMANCE, state);
        entityData.set(ACTION_TICKS, 0);
    }

    public void beginFight(int players) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(partyHealth(players));
        setHealth(getMaxHealth());
        fighting = true;
        started = lastContact = level().getGameTime();
        recoverUntil = started + 10;
        choice = "first approach";
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!acceptsDamage(fighting, pendingLethal, pendingEmpower, clip)
                || opening.lethalDeferred() || !PantheonCombat.permitsInteraction(this, source.getEntity())) return false;
        amount = incoming(amount, clip, previousTime);
        float before = getHealth();
        inDamage = true;
        boolean landed;
        try {
            landed = super.hurt(source, amount);
        } finally {
            inDamage = false;
        }
        if (landed && source.getEntity() instanceof ServerPlayer player && PantheonCombat.fighter(player)) {
            long now = level().getGameTime();
            float effective = Math.max(0, before - getHealth());
            threat.put(player.getUUID(), new Threat(Math.min(120, threat(player, now) + effective), now));
            lastDamage = now;
        }
        return landed;
    }

    @Override public void setHealth(float health) {
        if (inDamage) {
            switch (healthEvent(health, getMaxHealth(), phaseTwo)) {
                case EMPOWER -> {
                    health = getMaxHealth() * PHASE_THRESHOLD;
                    pendingEmpower = true;
                }
                case DEFEAT -> {
                    health = 1;
                    if (opening.required(phaseTwo)) {
                        opening.deferLethal();
                        boss.setName(Component.translatable(LAST_STAND));
                        for (ServerPlayer player : PantheonCombat.opponents(this)) {
                            player.sendSystemMessage(Component.translatable(LAST_STAND + ".hint"));
                        }
                    } else pendingLethal = true;
                }
                case NONE -> {}
            }
        }
        super.setHealth(health);
    }

    public boolean lethalPending() { return pendingLethal; }
    public boolean finaleComplete() { return finaleComplete; }

    public void beginFinale() {
        stopChain("defeated");
        fighting = false;
        wave = null;
        setInvulnerable(true);
        setDeltaMovement(Vec3.ZERO);
        play(Clip.DEATH);
        boss.setName(getType().getDescription());
        boss.setProgress(0);
    }

    @Override public void remove(RemovalReason reason) {
        boss.removeAllPlayers();
        super.remove(reason);
    }

    public String combatStatus() {
        return "Orion " + (phaseTwo ? 2 : 1) + " / " + clip + "@" + previousTime
                + " / " + (combo == null ? "-" : combo + " " + (step + 1)) + " / "
                + (opponent == null ? "-" : opponent.getGameProfile().getName()) + " / " + choice
                + " / first deadly " + opening.performed() + "/6"
                + (opening.lethalDeferred() ? " / lethal deferred" : "");
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            tickClientAnimation();
            return;
        }
        final long now = server.getGameTime();
        final int elapsed = (int)(now - started);
        final float time = elapsed * rate / 20;
        // The server owns contact timing. The client uses these samples to pace its visual playhead.
        if (clip.usesCursor()) entityData.set(ACTION_TICKS, elapsed);
        if (clip == Clip.DEATH) {
            tickDeath(server, time);
            return;
        }
        if (!fighting || pendingLethal) return;
        List<ServerPlayer> players = PantheonCombat.opponents(this);
        updateBoss(players);
        if (players.isEmpty()) return;
        if (wave != null) tickWave(server, players, now);
        opening.resolve(wave != null);
        if (opening.releaseLethal()) {
            pendingLethal = true;
            return;
        }

        if (clip == Clip.EMPOWER) {
            if (crossed(previousTime, time, .50f)) light(server, position().add(0, 1.5, 0), 1.8, false);
            previousTime = time;
            if (time >= clip.length()) {
                phaseTwo = true;
                readyAt.replaceAll((ignored, value) -> now + Math.max(0, (long)((value - now) * PHASE_COOLDOWN)));
                recoverUntil = now + OPENING_RESPONSE;
                play(Clip.IDLE);
            }
            return;
        }
        if (technique != null) {
            tickTechnique(server, players, now, time);
            return;
        }
        if (pendingEmpower) {
            empower();
            return;
        }
        if (!opening.awaitingWave()) think(players, now);
    }

    private void updateBoss(List<ServerPlayer> players) {
        for (ServerPlayer player : List.copyOf(boss.getPlayers()))
            if (!players.contains(player)) boss.removePlayer(player);
        for (ServerPlayer player : players) boss.addPlayer(player);
        boss.setProgress(getHealth() / getMaxHealth());
    }

    private void empower() {
        stopChain("Soul threshold");
        pendingEmpower = false;
        wave = null;
        setDeltaMovement(Vec3.ZERO);
        play(Clip.EMPOWER);
        level().playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1, .84f);
    }

    private void think(List<ServerPlayer> players, long now) {
        if (!players.contains(opponent) || now - lastChoice >= (phaseTwo ? 8 : 12)) chooseTarget(players, now);
        if (opponent == null) return;
        double distance = distanceTo(opponent);
        boolean visible = hasLineOfSight(opponent);
        boolean openingRequired = opening.required(phaseTwo);
        if (openingRequired && now < repositionUntil) {
            reposition(now);
            return;
        }
        if (now < escapeUntil) {
            maneuver(distance, now);
            return;
        }
        boolean approachBlocked = needsDetour(distance, visible);
        double range = phaseTwo ? ENGAGE_RANGE_PHASE_TWO : ENGAGE_RANGE;
        boolean ready = now >= recoverUntil && distance <= range && visible && !approachBlocked;
        if (openingRequired && ready) {
            if (combo != Combo.DEADLY) {
                combo = Combo.DEADLY;
                step = opening.performed();
                linkUntil = now + LINK_WAIT;
            }
            if (tryLinked(now, "first deadly")) return;
        } else if (!openingRequired && combo != null) {
            if (visible && !approachBlocked && distance <= range && tryLinked(now, "chain continuation")) return;
            if (combo != null && now >= linkUntil) finishChain(now, "link lost");
        } else if (ready) {
            boolean mobile = opponent.isSprinting() || now - lastContact > (phaseTwo ? 70 : 100);
            Combo candidate = selectCombo(now, distance, mobile);
            boolean recentHit = now - lastDamage < 14 && distance < 4.5;
            if (recentHit && now >= readyAt.getOrDefault(Technique.DODGE, 0L) && random.nextFloat() < .14
                    && startTechnique(Technique.DODGE, now, "evade recent pressure")) return;
            if (candidate != null) {
                combo = candidate;
                step = 0;
                linkUntil = now + LINK_WAIT;
                if (tryLinked(now, mobile ? "mobile target" : distance > 5 ? "range" : "close")) return;
            } else if (distance <= SLASH_START_RANGE && now >= readyAt.getOrDefault(Technique.SLASH, 0L)
                    && startTechnique(Technique.SLASH, now, "short opening")) return;
        }
        maneuver(distance, now);
    }

    /** Align between clips within a bounded turn budget. Failed mandatory links resume at the same step. */
    private boolean tryLinked(long now, String reason) {
        Technique move = combo.steps.get(step);
        if (startTechnique(move, now, reason)) return true;
        if (now >= linkUntil) {
            if (opening.required(phaseTwo)) {
                repositionBeforeDeadly(now);
            } else finishChain(now, "link blocked");
            return false;
        }
        face(opponent.position(), ALIGN_TURN);
        return true;
    }

    private void chooseTarget(List<ServerPlayer> players, long now) {
        lastChoice = now;
        ServerPlayer best = null;
        double highest = -Double.MAX_VALUE;
        for (ServerPlayer player : players) {
            boolean visible = hasLineOfSight(player);
            double score = 25 - Math.min(38, distanceTo(player)) + threat(player, now) * .32
                    + (visible ? 10 : -12) - Math.abs(getY() - player.getY()) * 3
                    + (opponent == player && now - lastContact < 90 ? 3.5 : 0);
            if (score > highest) {
                highest = score;
                best = player;
            }
        }
        opponent = best;
    }

    private float threat(ServerPlayer player, long now) {
        Threat entry = threat.get(player.getUUID());
        return entry == null ? 0 : Math.max(0, entry.points - (now - entry.time) * .38f);
    }

    @Nullable private Combo selectCombo(long now, double distance, boolean mobile) {
        List<Combo> choices = new ArrayList<>();
        for (Combo option : Combo.values()) {
            if (!option.available(phaseTwo, now >= longReadyAt, move -> now >= readyAt.getOrDefault(move, 0L))) continue;
            if (distance > SLASH_START_RANGE && option.steps.getFirst() == Technique.SLASH) continue;
            int weight = mobile ? option.mobile : distance > 5 ? option.distant : option.close;
            for (int i = 0; i < weight; i++) choices.add(option);
        }
        return choices.isEmpty() ? null : choices.get(random.nextInt(choices.size()));
    }

    private boolean startTechnique(Technique move, long now, String reason) {
        if (opponent == null || !hasLineOfSight(opponent)) return false;
        float difference = Mth.wrapDegrees(yawTo(opponent.position()) - getYRot());
        if (Math.abs(difference) > ALIGN_TOLERANCE) return false;
        face(opponent.position(), ALIGN_TURN);
        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        float motion = phaseTwo ? PHASE_MOVE / PHASE_RATE : 1;
        travel = Vec3.ZERO;
        Clip next = switch (move) {
            case SLASH -> Clip.SLASH_COMBO;
            case CLEAVE -> Clip.CLEAVE;
            case SPIN -> {
                travel = forward.scale(dashDistance(distanceTo(opponent), phaseTwo));
                double probe = Math.min(1.5, Math.max(.36, distanceTo(opponent) - .5));
                if (!canTraverse(position(), position().add(forward.scale(probe)))) {
                    requestEscape(now);
                    yield null;
                }
                yield Clip.SPIN_DASH;
            }
            case DODGE -> chooseDodge(forward, motion);
        };
        if (next == null) return false;
        if (combo != null && combo.phase == 2 && step == 0) longReadyAt = now + LONG_COOLDOWN;
        technique = move;
        techniqueTravel = 0;
        blockedMovement = false;
        clearDetour();
        readyAt.put(move, now + Math.round(move.cooldown * (phaseTwo ? PHASE_COOLDOWN : 1)));
        for (HitWindow window : hit) window.reset();
        setDeltaMovement(Vec3.ZERO);
        choice = reason;
        play(next);
        TearsOfInk.LOGGER.debug("Orion {}: {}", getUUID(), combatStatus());
        return true;
    }

    @Nullable private Clip chooseDodge(Vec3 forward, float motion) {
        int offset = random.nextInt(DODGES.size());
        for (int i = 0; i < DODGES.size(); i++) {
            Clip option = DODGES.get((i + offset) % DODGES.size());
            Vec3 direction = switch (option) {
                case DODGE_LEFT -> forward.yRot(Mth.HALF_PI);
                case DODGE_RIGHT -> forward.yRot(-Mth.HALF_PI);
                default -> forward.reverse();
            };
            Vec3 full = direction.scale(DODGE_DISTANCE * motion);
            if (canStep(full.scale(.33)) && canStep(full.scale(.66)) && canStep(full)) {
                travel = full;
                return option;
            }
        }
        return null;
    }

    private void tickTechnique(ServerLevel server, List<ServerPlayer> players, long now, float time) {
        float to = Math.min(time, clip.length());
        Vec3 old = position();
        if (technique == Technique.SPIN || technique == Technique.DODGE) {
            double interval = technique == Technique.SPIN
                    ? spinAdvance(to, phaseTwo) - spinAdvance(previousTime, phaseTwo)
                    : movementProgress(to, .05f, .39f) - movementProgress(previousTime, .05f, .39f);
            if (interval > 0 && travel.horizontalDistanceSqr() > .0001 && !step(travel.scale(interval * movementFactor()))) {
                if (techniqueTravel < DASH_CONFIRM || !hasLineOfSight(opponent)) blockedMovement = true;
                travel = Vec3.ZERO;
            }
        } else if (players.contains(opponent)) {
            // The blade's path stays committed during contact. Only the intervening windups track the target.
            if (technique == Technique.SLASH) advanceSlash(to);
            else if (to < CLEAVE_BRACE_START) face(opponent.position(), 9);
        }
        techniqueTravel += position().subtract(old).horizontalDistance();
        if (technique == Technique.SLASH) {
            for (int i = 0; i < SLASH.size(); i++) sweep(server, players, SLASH.get(i), i, to, old);
        } else if (technique == Technique.SPIN) {
            sweep(server, players, SPIN, 0, to, old);
        } else if (technique == Technique.CLEAVE) {
            sweep(server, players, CLEAVE, 0, to, old);
            if (crossed(previousTime, to, CLEAVE_IMPACT)) {
                Vec3 tip = OrionBlade.sample(clip, CLEAVE_IMPACT, position(), getYRot()).tip();
                Vec3 ground = new Vec3(tip.x, getY() + .08, tip.z);
                shockwave(server, players, ground, phaseTwo ? LIGHT_RADIUS : CLEAVE_RADIUS,
                        CLEAVE_IMPACT_DAMAGE.at(phaseTwo), hit.getFirst(), phaseTwo);
                launchWave(server, ground, now);
            }
        }
        previousTime = to;
        if (pendingEmpower && (time >= clip.length() || wave == null && switch (technique) {
            case SLASH -> to >= EMPOWER_AFTER_SLASH;
            case SPIN -> to >= EMPOWER_AFTER_SPIN;
            default -> false;
        })) {
            empower();
            return;
        }
        if (time < clip.length()) return;
        if (blockedMovement) requestEscape(now);
        if (combo == Combo.DEADLY && opening.required(phaseTwo)) {
            opening.finish(technique, technique != Technique.SPIN || techniqueTravel >= DASH_CONFIRM);
            if (opening.awaitingWave()) {
                finishChain(now, "first deadly executed");
                recoverUntil += OPENING_RECOVERY_BONUS;
                return;
            }
            if (step == opening.performed()) {
                repositionBeforeDeadly(now);
                return;
            }
        }
        if (combo != null && ++step < combo.steps.size()) {
            technique = null;
            travel = Vec3.ZERO;
            linkUntil = Math.max(now, escapeUntil) + LINK_WAIT;
            chooseTarget(players, now);
            if (opponent != null && now >= escapeUntil) tryLinked(now, "chain continuation");
            return;
        }
        finishChain(now, "recovery");
    }

    private void advanceSlash(float to) {
        float interval = slashAdvance(to) - slashAdvance(previousTime);
        if (interval <= 0 || !hasLineOfSight(opponent)) return;
        if (SLASH.stream().noneMatch(window -> window.touches(previousTime, to))) face(opponent.position(), SLASH_TURN);
        if (Math.abs(Mth.wrapDegrees(yawTo(opponent.position()) - getYRot())) > 35) return;
        double distance = opponent.position().subtract(position()).horizontalDistance();
        double advance = Math.min(distance - SLASH_STOP_RANGE,
                interval * (phaseTwo ? SLASH_ADVANCE_PHASE_TWO : SLASH_ADVANCE) * movementFactor());
        if (advance > 0) step(Vec3.directionFromRotation(0, getYRot()).scale(advance));
    }

    private void finishChain(long now, String reason) {
        int recovery = combo == null ? SOLO_RECOVERY : combo.recovery;
        stopChain(reason);
        recoverUntil = now + Math.round(recovery * (phaseTwo ? RECOVERY_PHASE_TWO : 1));
        play(Clip.IDLE);
    }

    private void sweep(ServerLevel server, List<ServerPlayer> players, Window window, int index, float to, Vec3 old) {
        if (!window.touches(previousTime, to)) return;
        if (crossed(previousTime, to, window.from()))
            server.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1, .78f);
        float first = Math.max(previousTime, window.from());
        float last = Math.min(to, window.to());
        int subdivisions = Math.max(1, Mth.ceil((last - first) / SWEEP_STEP));
        HitWindow affected = hit.get(index);
        for (int i = 0; i <= subdivisions; i++) {
            float sample = Mth.lerp((float) i / subdivisions, first, last);
            Vec3 origin = old.lerp(position(), (sample - previousTime) / Math.max(.0001f, to - previousTime));
            OrionBlade.Segment blade = OrionBlade.sample(clip, sample, origin, getYRot());
            var obstruction = server.clip(new ClipContext(blade.base(), blade.tip(), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (obstruction.getType() != HitResult.Type.MISS) {
                blade = new OrionBlade.Segment(blade.base(), obstruction.getLocation());
            }
            double margin = switch (clip) {
                case SLASH_COMBO -> SLASH_MARGIN;
                case SPIN_DASH -> SPIN_MARGIN;
                default -> BLADE_TOLERANCE;
            };
            for (ServerPlayer player : players) {
                if (affected.contains(player.getUUID())) continue;
                var bounds = player.getBoundingBox();
                Vec3 contact = blade.contact(bounds, margin);
                if (contact == null) continue;
                Vec3 body = new Vec3(player.getX(), Math.clamp(contact.y, bounds.minY + .05, bounds.maxY - .05), player.getZ());
                if (clearLine(server, origin.add(0, getEyeHeight(), 0), contact) && clearLine(server, contact, body))
                    strike(player, bladeDamage(window), affected, attackFrom(origin));
            }
        }
    }

    private float bladeDamage(Window window) {
        return technique == Technique.CLEAVE && combo == Combo.DEADLY
                ? DEADLY_CLEAVE_DAMAGE : window.damage().at(phaseTwo);
    }

    private void shockwave(ServerLevel server, List<ServerPlayer> players, Vec3 at, double radius, float damage,
                           HitWindow affected, boolean radiant) {
        light(server, at, radius, radiant);
        for (ServerPlayer player : players) {
            Vec3 delta = player.position().subtract(at);
            if (Math.abs(delta.y) > 1.25 || delta.horizontalDistanceSqr() > radius * radius
                    || !clearLine(server, at.add(0, .25, 0), player.position().add(0, .6, 0))) continue;
            strike(player, damage, affected);
        }
    }

    private void launchWave(ServerLevel server, Vec3 ground, long now) {
        Vec3 origin = ground.add(0, .22, 0);
        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        if (!clearLine(server, position().add(0, .35, 0), origin)) return;
        wave = new Wave(origin, forward, forward.yRot(Mth.HALF_PI), now + WAVE_DELAY, phaseTwo, new HitWindow());
        server.sendParticles(phaseTwo ? ParticleTypes.END_ROD : ParticleTypes.CRIT,
                origin.x, origin.y, origin.z, 14, .55, .1, .55, .035);
    }

    private void tickWave(ServerLevel server, List<ServerPlayer> players, long now) {
        long tick = now - wave.launch();
        if (tick < 0) return;
        if (tick >= WAVE_TICKS) {
            wave = null;
            return;
        }
        double speed = WAVE_SPEED * (wave.radiant() ? PHASE_MOVE : 1);
        double distance = tick * speed;
        double width = WAVE_HALF_WIDTH + distance * .09;
        Vec3 origin = wave.origin();
        Vec3 leadingEdge = origin.add(wave.forward().scale(distance));
        if (!clearLine(server, origin, leadingEdge)) {
            wave = null;
            return;
        }
        if (tick == 0)
            server.playSound(null, origin.x, origin.y, origin.z, SoundEvents.BREEZE_WIND_CHARGE_BURST,
                    SoundSource.HOSTILE, .9f, wave.radiant() ? 1.3f : .9f);
        for (int i = -3; i <= 3; i++) {
            Vec3 point = leadingEdge.add(wave.lateral().scale(i * width / 3));
            if (!clearLine(server, origin, point)) continue;
            server.sendParticles(wave.radiant() ? ParticleTypes.END_ROD : ParticleTypes.CLOUD,
                    point.x, point.y, point.z, 2, .10, .09, .10, .025);
        }
        Vec3 sourcePosition = origin.subtract(wave.forward().scale(.55));
        DamageSource source = attackFrom(sourcePosition);
        for (ServerPlayer player : players) {
            Vec3 offset = player.position().subtract(origin);
            double forward = offset.dot(wave.forward());
            if (Math.abs(offset.y) > 1.12 || Math.abs(offset.dot(wave.lateral())) > width
                    || forward < distance - .67 || forward > distance + speed * .55) continue;
            if (!clearLine(server, origin, player.position().add(0, .38, 0))) continue;
            strike(player, WAVE_DAMAGE.at(wave.radiant()), wave.hit(), source);
        }
    }

    private boolean clearLine(ServerLevel level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    private DamageSource attackFrom(Vec3 sourcePosition) {
        return new DamageSource(damageSources().mobAttack(this).typeHolder(), this) {
            @Override public Vec3 getSourcePosition() { return sourcePosition; }
        };
    }

    private void strike(ServerPlayer player, float damage, HitWindow affected) {
        strike(player, damage, affected, damageSources().mobAttack(this));
    }

    private void strike(ServerPlayer player, float damage, HitWindow affected, DamageSource source) {
        if (!PantheonCombat.fighter(player) || !affected.register(player.getUUID())) return;
        if (player.hurt(source, damage))
            lastContact = level().getGameTime();
    }

    private void stopChain(String reason) {
        combo = null;
        technique = null;
        travel = Vec3.ZERO;
        step = 0;
        choice = reason;
    }

    private void repositionBeforeDeadly(long now) {
        stopChain("first deadly: reposition");
        repositionUntil = now + REPOSITION_TICKS;
        recoverUntil = repositionUntil + BLOCKED_RECOVERY;
    }

    private void clearDetour() {
        detour = List.of();
        detourIndex = 0;
        detourTarget = null;
        plannedTarget = null;
        repathAt = 0;
    }

    private void requestEscape(long now) {
        clearDetour();
        escapeUntil = Math.max(escapeUntil, now + 10);
        linkUntil = Math.max(linkUntil, escapeUntil + LINK_WAIT);
    }

    private void reposition(long now) {
        if (!hasLineOfSight(opponent)) {
            maneuver(distanceTo(opponent), now);
            return;
        }
        Vec3 fromTarget = position().subtract(opponent.position()).multiply(1, 0, 1);
        if (fromTarget.horizontalDistanceSqr() < .01) fromTarget = Vec3.directionFromRotation(0, getYRot()).reverse();
        double speed = WALK_SPEED * PHASE_MOVE * movementFactor();
        Vec3 direction = localHeading(fromTarget.normalize(), speed);
        if (direction != null && step(direction.scale(speed))) {
            face(position().add(direction), ALIGN_TURN);
            if (clip != Clip.WALK) play(Clip.WALK);
        } else {
            requestEscape(now);
            maneuver(distanceTo(opponent), now);
        }
    }

    private boolean needsDetour(double distance, boolean visible) {
        if (!visible) return true;
        if (distance <= 2.65) return false;
        Vec3 direction = opponent.position().subtract(position()).multiply(1, 0, 1).normalize();
        double probe = Math.max(.25, distance - 2.35);
        if (detourIndex >= detour.size()) probe = Math.min(probe, 2.5);
        return !canTraverse(position(), position().add(direction.scale(probe)));
    }

    private void maneuver(double distance, long now) {
        Vec3 direction = opponent.position().subtract(position()).multiply(1, 0, 1).normalize();
        boolean responding = now < recoverUntil;
        boolean visible = hasLineOfSight(opponent);
        boolean escaping = now < escapeUntil;
        boolean routing = escaping || needsDetour(distance, visible);
        if (!routing && (responding && distance < (phaseTwo ? 3.25 : 3.6) || !responding && distance < 2.65)) {
            clearDetour();
            face(opponent.position(), 19);
            if (clip != Clip.IDLE) play(Clip.IDLE);
            return;
        }
        if (!routing && responding && distance < 5.4)
            direction = direction.yRot((tickCount / 80 % 2 == 0 ? 1 : -1) * .9f);
        boolean run = distance > (phaseTwo ? 4.9 : 6);
        double speed = (run ? RUN_SPEED : WALK_SPEED) * (phaseTwo ? PHASE_MOVE : 1) * movementFactor();
        if (routing) {
            Vec3 route = followingPath(now, escaping);
            if (route != null) direction = route;
            else direction = localHeading(direction, speed);
        } else {
            clearDetour();
            if (!canTraverse(position(), position().add(direction.scale(speed)))) {
                direction = localHeading(direction, speed);
                if (direction == null) requestEscape(now);
            }
        }
        Vec3 before = position();
        boolean moved = false;
        if (direction != null) {
            double stride = speed;
            if (routing && detourIndex < detour.size())
                stride = Math.min(stride, detour.get(detourIndex).subtract(position()).horizontalDistance());
            if (stride > .001) moved = step(direction.scale(stride));
            if (!moved) {
                detour = List.of();
                detourIndex = 0;
                repathAt = Math.min(repathAt, now + 3);
            }
        }
        Vec3 actual = position().subtract(before);
        boolean advancing = actual.horizontalDistanceSqr() > .0001;
        Clip next = advancing ? run ? Clip.RUN : Clip.WALK : Clip.IDLE;
        if (clip != next) play(next);
        face(advancing && (routing || run) ? position().add(actual) : opponent.position(), run ? 16 : 22);
    }

    @Nullable private Vec3 followingPath(long now, boolean escaping) {
        Vec3 target = opponent.position();
        boolean changed = !opponent.getUUID().equals(detourTarget);
        boolean movedTarget = plannedTarget == null || target.distanceToSqr(plannedTarget) > 2.25;
        if (changed || now >= repathAt && (movedTarget || detourIndex >= detour.size() || now >= repathAt + 20)) {
            detourTarget = opponent.getUUID();
            plannedTarget = target;
            repathAt = now + 8;
            Vec3 origin = position();
            ServerLevel server = (ServerLevel) level();
            detour = OrionRoute.find(origin, target, at -> {
                Vec3 offset = at.subtract(target);
                return offset.horizontalDistanceSqr() <= 2.35 * 2.35
                        && (!escaping || at.distanceToSqr(origin) >= 1.2 * 1.2)
                        && clearLine(server, at.add(0, getEyeHeight(), 0), opponent.getEyePosition());
            }, this::canTraverse);
            detourIndex = 0;
        }
        while (detourIndex < detour.size()) {
            Vec3 offset = detour.get(detourIndex).subtract(position()).multiply(1, 0, 1);
            if (offset.horizontalDistanceSqr() > .08 * .08) {
                if (!canTraverse(position(), detour.get(detourIndex))) {
                    detour = List.of();
                    detourIndex = 0;
                    repathAt = Math.min(repathAt, now + 3);
                    return null;
                }
                return offset.normalize();
            }
            detourIndex++;
        }
        return null;
    }

    /** Try lateral and retreat directions as well as forward, without taking several partial steps in one tick. */
    @Nullable private Vec3 localHeading(Vec3 preferred, double speed) {
        if (preferred.horizontalDistanceSqr() < .0001) preferred = Vec3.directionFromRotation(0, getYRot());
        Vec3 best = null;
        double score = -Double.MAX_VALUE;
        for (float angle : new float[]{0, .6f, -.6f, 1.16f, -1.16f, 1.57f, -1.57f, 2.15f, -2.15f, 2.7f, -2.7f, Mth.PI}) {
            Vec3 candidate = preferred.yRot(angle);
            if (!canTraverse(position(), position().add(candidate.scale(speed)))) continue;
            double value = Math.cos(angle);
            if (canTraverse(position(), position().add(candidate.scale(Math.max(speed, 1.1))))) value += 1.5;
            if (value > score) { best = candidate; score = value; }
        }
        return best;
    }

    private float yawTo(Vec3 target) {
        Vec3 difference = target.subtract(position());
        return difference.horizontalDistanceSqr() < .0001 ? getYRot()
                : (float)(Mth.atan2(-difference.x, difference.z) * Mth.RAD_TO_DEG);
    }

    private void face(Vec3 target, float max) {
        setYRot(Mth.approachDegrees(getYRot(), yawTo(target), max));
        setYHeadRot(getYRot());
        yBodyRot = getYRot();
    }

    private double movementFactor() {
        // Shields and Ink slow effects modify the normal attribute, including on custom movement.
        return Math.clamp(getAttributeValue(Attributes.MOVEMENT_SPEED) / RUN_SPEED, .35, 1.3);
    }

    private boolean canStep(Vec3 delta) { return canStandAt(position().add(delta)); }

    private boolean canTraverse(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int samples = Math.max(1, (int)Math.ceil(delta.horizontalDistance() / .18));
        for (int i = 1; i <= samples; i++)
            if (!canStandAt(from.add(delta.scale((double)i / samples)))) return false;
        return true;
    }

    private boolean canStandAt(Vec3 next) {
        if (!PantheonCombat.canMoveOrion(this, next)
                || level().getBlockCollisions(this, getBoundingBox().move(next.subtract(position()))).iterator().hasNext())
            return false;
        BlockPos support = BlockPos.containing(next.x, next.y - .12, next.z);
        return level().getBlockState(support).isFaceSturdy(level(), support, Direction.UP);
    }

    private boolean step(Vec3 delta) {
        double distance = delta.horizontalDistance();
        Vec3 before = position();
        moveInSlices(delta, slice -> {
            if (!canStep(slice)) return false;
            move(MoverType.SELF, slice);
            return true;
        });
        return position().distanceToSqr(before) >= distance * distance * .75;
    }

    private void light(ServerLevel level, Vec3 at, double radius, boolean radiant) {
        var particle = radiant ? ParticleTypes.END_ROD : ParticleTypes.CLOUD;
        for (int i = 0; i < 20; i++) {
            double angle = i * (Math.PI * 2 / 20);
            level.sendParticles(particle, at.x + Math.cos(angle) * radius, at.y + .12,
                    at.z + Math.sin(angle) * radius, 1, .08, .06, .08, .018);
        }
        level.playSound(null, at.x, at.y, at.z, radiant ? SoundEvents.AMETHYST_BLOCK_RESONATE
                : SoundEvents.MACE_SMASH_GROUND, SoundSource.HOSTILE, 1.1f, radiant ? 1.2f : .8f);
    }

    private void tickDeath(ServerLevel level, float time) {
        setDeltaMovement(Vec3.ZERO);
        if (finaleComplete || time < clip.length()) return;
        finaleComplete = true;
        Vec3 star = position().add(0, 1.55, 0);
        level.sendParticles(ParticleTypes.FLASH, star.x, star.y, star.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, star.x, star.y, star.z, 90, .30, .34, .30, .19);
        level.sendParticles(ParticleTypes.GLOW, star.x, star.y, star.z, 38, .48, .38, .48, .22);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4f, 1.15f);
    }
}
