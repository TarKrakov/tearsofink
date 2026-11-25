package com.tarkrakov.tearsofink.entity.orion;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tarkrakov.tearsofink.entity.orion.OrionCombat.*;

public final class OrionEntity extends PathfinderMob {
    private static final EntityDataAccessor<CompoundTag> PERFORMANCE =
            SynchedEntityData.defineId(OrionEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final List<Clip> DODGES = List.of(Clip.DODGE_LEFT, Clip.DODGE_RIGHT, Clip.DODGE_BACK);

    private final ServerBossEvent boss = new ServerBossEvent(getType().getDescription(),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private final EnumMap<Technique, Long> readyAt = new EnumMap<>(Technique.class);
    private final Map<UUID, Threat> threat = new HashMap<>();
    private final List<Set<UUID>> hit = List.of(new HashSet<>(), new HashSet<>(), new HashSet<>());
    private Clip clip = Clip.IDLE;
    private long started, recoverUntil, longReadyAt, lastContact, lastChoice, lastDamage = -100;
    private float rate = 1;
    private float previousTime;
    private Combo combo;
    private int step;
    private Technique technique;
    private ServerPlayer opponent;
    private Vec3 travel = Vec3.ZERO;
    private Wave wave;
    private Path detour;
    private UUID detourTarget;
    private long repathAt;
    private boolean fighting, phaseTwo, pendingEmpower, inDamage, pendingLethal, finaleComplete;
    private String choice = "waiting";

    private record Threat(float points, long time) {}
    private record Wave(Vec3 origin, Vec3 forward, Vec3 lateral, long launch, boolean radiant, Set<UUID> hit) {}

    public record Performance(Clip clip, long start, float rate, Clip previous, long previousStart,
                              float previousRate, boolean phaseTwo) {
        public float seconds(long tick, float partial) { return Math.max(0, (tick - start + partial) * rate / 20); }
        public float previousSeconds(long tick) { return Math.max(0, (tick - previousStart) * previousRate / 20); }
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
    }

    public Performance performance() {
        CompoundTag state = entityData.get(PERFORMANCE);
        if (state.isEmpty()) return new Performance(Clip.IDLE, 0, 1, Clip.IDLE, 0, 1, false);
        return new Performance(Clip.values()[state.getInt("clip")], state.getLong("start"), state.getFloat("rate"),
                Clip.values()[state.getInt("previous")], state.getLong("previousStart"),
                state.getFloat("previousRate"), state.getBoolean("phaseTwo"));
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
    }

    public void beginFight(int players) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH * (1 + (Math.min(8, players) - 1) * COOP_HEALTH));
        setHealth(getMaxHealth());
        fighting = true;
        started = lastContact = level().getGameTime();
        recoverUntil = started + 10;
        choice = "first approach";
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!acceptsDamage(fighting, pendingLethal, pendingEmpower, clip)
                || !PantheonCombat.permitsInteraction(this, source.getEntity())) return false;
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
                    pendingLethal = true;
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
        boss.setProgress(0);
    }

    @Override public void remove(RemovalReason reason) {
        boss.removeAllPlayers();
        super.remove(reason);
    }

    public String combatStatus() {
        return "Orion " + (phaseTwo ? 2 : 1) + " / " + clip + "@" + previousTime
                + " / " + (combo == null ? "-" : combo + " " + (step + 1)) + " / "
                + (opponent == null ? "-" : opponent.getGameProfile().getName()) + " / " + choice;
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        final long now = server.getGameTime();
        final float time = (now - started) * rate / 20;
        if (clip == Clip.DEATH) {
            tickDeath(server, time);
            return;
        }
        if (!fighting || pendingLethal) return;
        List<ServerPlayer> players = PantheonCombat.opponents(this);
        updateBoss(players);
        if (players.isEmpty()) return;
        if (wave != null) tickWave(server, players, now);

        if (clip == Clip.EMPOWER) {
            if (crossed(previousTime, time, .50f)) light(server, position().add(0, 1.5, 0), 1.8, false);
            previousTime = time;
            if (time >= clip.length()) {
                phaseTwo = true;
                readyAt.replaceAll((ignored, value) -> now + Math.max(0, (long)((value - now) * PHASE_COOLDOWN)));
                recoverUntil = now + 9;
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
        think(players, now);
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
        if (!players.contains(opponent) || now - lastChoice >= 12) chooseTarget(players, now);
        if (opponent == null) return;
        double distance = distanceTo(opponent);
        if (now >= recoverUntil && distance <= 13 && hasLineOfSight(opponent)) {
            boolean mobile = opponent.isSprinting() || now - lastContact > 110;
            Combo candidate = selectCombo(now, distance, mobile);
            boolean recentHit = now - lastDamage < 14 && distance < 4.5;
            if (recentHit && now >= readyAt.getOrDefault(Technique.DODGE, 0L) && random.nextFloat() < .14
                    && startTechnique(Technique.DODGE, now, "evade recent pressure")) return;
            if (candidate != null) {
                combo = candidate;
                step = 0;
                if (startTechnique(combo.steps.getFirst(), now, mobile ? "mobile target" : distance > 5 ? "range" : "close"))
                    return;
                stopChain("no free opening");
                recoverUntil = now + 7;
            } else if (distance <= 3.6 && now >= readyAt.getOrDefault(Technique.SLASH, 0L)
                    && startTechnique(Technique.SLASH, now, "short opening")) return;
        }
        maneuver(distance, now);
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
            if (distance > 6 && option.steps.getFirst() == Technique.SLASH) continue;
            int weight = mobile ? option.mobile : distance > 5 ? option.distant : option.close;
            for (int i = 0; i < weight; i++) choices.add(option);
        }
        return choices.isEmpty() ? null : choices.get(random.nextInt(choices.size()));
    }

    private boolean startTechnique(Technique move, long now, String reason) {
        if (opponent == null) return false;
        face(opponent.position(), 180);
        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        float motion = phaseTwo ? PHASE_MOVE / PHASE_RATE : 1;
        travel = Vec3.ZERO;
        Clip next = switch (move) {
            case SLASH -> Clip.SLASH_COMBO;
            case CLEAVE -> Clip.CLEAVE;
            case SPIN -> {
                double cover = Math.clamp(distanceTo(opponent) - .55, 1.9, DASH_DISTANCE) * motion;
                travel = forward.scale(cover);
                yield canStep(forward.scale(.42)) ? Clip.SPIN_DASH : null;
            }
            case DODGE -> chooseDodge(forward, motion);
        };
        if (next == null) return false;
        if (combo != null && combo.phase == 2 && step == 0) longReadyAt = now + LONG_COOLDOWN;
        technique = move;
        readyAt.put(move, now + Math.round(move.cooldown * (phaseTwo ? PHASE_COOLDOWN : 1)));
        for (Set<UUID> window : hit) window.clear();
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
            float start = technique == Technique.SPIN ? .12f : .05f;
            float end = technique == Technique.SPIN ? .82f : .39f;
            double interval = movementProgress(to, start, end) - movementProgress(previousTime, start, end);
            if (interval > 0 && !step(travel.scale(interval))) travel = Vec3.ZERO;
        } else {
            // Follow the target only early in a telegraph. Commit to the facing before contact.
            if (players.contains(opponent) && (technique == Technique.SLASH && to < .58f
                    || technique == Technique.CLEAVE && to < CLEAVE_BRACE_START))
                face(opponent.position(), 9);
        }
        if (technique == Technique.SLASH) {
            for (int i = 0; i < SLASH.size(); i++) sweep(server, players, SLASH.get(i), i, to, old);
        } else if (technique == Technique.SPIN) {
            sweep(server, players, SPIN, 0, to, old);
            if (phaseTwo && crossed(previousTime, to, SPIN_LIGHT))
                shockwave(server, players, position(), LIGHT_RADIUS, 8, hit.getFirst(), true);
        } else if (technique == Technique.CLEAVE) {
            sweep(server, players, CLEAVE, 0, to, old);
            if (crossed(previousTime, to, CLEAVE_IMPACT)) {
                Vec3 tip = OrionBlade.sample(clip, CLEAVE_IMPACT, position(), getYRot()).tip();
                Vec3 ground = new Vec3(tip.x, getY() + .08, tip.z);
                shockwave(server, players, ground, phaseTwo ? LIGHT_RADIUS : CLEAVE_RADIUS,
                        phaseTwo ? 13 : 11, hit.getFirst(), phaseTwo);
                launchWave(server, ground, now);
            }
        }
        previousTime = to;
        if (time < clip.length()) return;
        if (pendingEmpower) {
            empower();
            return;
        }
        if (combo != null && ++step < combo.steps.size()) {
            chooseTarget(players, now);
            if (opponent != null && startTechnique(combo.steps.get(step), now, "chain continuation")) return;
        }
        int recovery = combo == null ? 31 : combo.recovery;
        stopChain(players.contains(opponent) ? "recovery" : "target lost");
        recoverUntil = now + Math.round(recovery * (phaseTwo ? .80f : 1));
        play(Clip.IDLE);
    }

    private void sweep(ServerLevel server, List<ServerPlayer> players, Window window, int index, float to, Vec3 old) {
        if (!window.touches(previousTime, to)) return;
        if (crossed(previousTime, to, window.from()))
            server.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1, .78f);
        float first = Math.max(previousTime, window.from());
        float last = Math.min(to, window.to());
        int subdivisions = Math.max(1, Mth.ceil((last - first) / .018f));
        Set<UUID> affected = hit.get(index);
        for (int i = 0; i <= subdivisions; i++) {
            float sample = Mth.lerp((float) i / subdivisions, first, last);
            Vec3 origin = old.lerp(position(), (sample - previousTime) / Math.max(.0001f, to - previousTime));
            OrionBlade.Segment blade = OrionBlade.sample(clip, sample, origin, getYRot());
            for (ServerPlayer player : players) {
                if (!affected.contains(player.getUUID()) && blade.touches(player.getBoundingBox()) && hasLineOfSight(player))
                    strike(player, window.damage(), affected);
            }
        }
    }

    private void shockwave(ServerLevel server, List<ServerPlayer> players, Vec3 at, double radius, float damage,
                           Set<UUID> affected, boolean radiant) {
        light(server, at, radius, radiant);
        for (ServerPlayer player : players) {
            Vec3 delta = player.position().subtract(at);
            if (Math.abs(delta.y) > 1.25 || delta.horizontalDistanceSqr() > radius * radius || !hasLineOfSight(player)) continue;
            strike(player, damage, affected);
        }
    }

    private void launchWave(ServerLevel server, Vec3 ground, long now) {
        Vec3 origin = ground.add(0, .22, 0);
        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        if (!clearLine(server, position().add(0, .35, 0), origin)) return;
        wave = new Wave(origin, forward, forward.yRot(Mth.HALF_PI), now + WAVE_DELAY, phaseTwo, new HashSet<>());
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
        DamageSource source = new DamageSource(damageSources().mobAttack(this).typeHolder(), this) {
            @Override public Vec3 getSourcePosition() { return sourcePosition; }
        };
        for (ServerPlayer player : players) {
            Vec3 offset = player.position().subtract(origin);
            double forward = offset.dot(wave.forward());
            if (Math.abs(offset.y) > 1.12 || Math.abs(offset.dot(wave.lateral())) > width
                    || forward < distance - .67 || forward > distance + speed * .55) continue;
            if (!clearLine(server, origin, player.position().add(0, .38, 0))) continue;
            strike(player, WAVE_DAMAGE, wave.hit(), source);
        }
    }

    private boolean clearLine(ServerLevel level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    private void strike(ServerPlayer player, float damage, Set<UUID> affected) {
        strike(player, damage, affected, damageSources().mobAttack(this));
    }

    private void strike(ServerPlayer player, float damage, Set<UUID> affected, DamageSource source) {
        if (!PantheonCombat.fighter(player) || !affected.add(player.getUUID())) return;
        if (player.hurt(source, damage * (phaseTwo ? PHASE_DAMAGE : 1)))
            lastContact = level().getGameTime();
    }

    private void stopChain(String reason) {
        combo = null;
        technique = null;
        travel = Vec3.ZERO;
        step = 0;
        choice = reason;
    }

    private void maneuver(double distance, long now) {
        Vec3 direction = opponent.position().subtract(position()).multiply(1, 0, 1).normalize();
        boolean responding = now < recoverUntil;
        if (responding && distance < 3.9 || !responding && distance < 2.8) {
            face(opponent.position(), 19);
            if (clip != Clip.IDLE) play(Clip.IDLE);
            return;
        }
        boolean visible = hasLineOfSight(opponent);
        if (distance < 5.3 || !visible)
            direction = direction.yRot((tickCount / 80 % 2 == 0 ? 1 : -1) * (responding ? 1.18f : .60f));
        boolean run = !responding && distance > 6;
        double speed = (run ? RUN_SPEED : WALK_SPEED) * (phaseTwo ? PHASE_MOVE : 1);
        if (!visible && !responding) {
            Vec3 route = followingPath(now);
            if (route != null) direction = route;
        }
        Vec3 advance = direction.scale(speed);
        boolean moved = step(advance) || step(advance.yRot(1.16f)) || step(advance.yRot(-1.16f));
        Clip next = moved ? run ? Clip.RUN : Clip.WALK : Clip.IDLE;
        if (clip != next) play(next);
        face(moved && run ? position().add(direction) : opponent.position(), run ? 16 : 22);
    }

    @Nullable private Vec3 followingPath(long now) {
        if (!opponent.getUUID().equals(detourTarget) || now >= repathAt) {
            detourTarget = opponent.getUUID();
            repathAt = now + 18;
            detour = getNavigation().createPath(opponent, 0);
        }
        if (detour == null) return null;
        while (!detour.isDone()) {
            Vec3 offset = detour.getNextEntityPos(this).subtract(position()).multiply(1, 0, 1);
            if (offset.horizontalDistanceSqr() > .62 * .62) return offset.normalize();
            detour.advance();
        }
        return null;
    }

    private void face(Vec3 target, float max) {
        Vec3 difference = target.subtract(position());
        if (difference.horizontalDistanceSqr() < .0001) return;
        float yaw = (float)(Mth.atan2(-difference.x, difference.z) * Mth.RAD_TO_DEG);
        setYRot(Mth.approachDegrees(getYRot(), yaw, max));
        setYHeadRot(getYRot());
        yBodyRot = getYRot();
    }

    private boolean canStep(Vec3 delta) {
        Vec3 next = position().add(delta);
        if (!PantheonCombat.canMoveOrion(this, next) || level().getBlockCollisions(this, getBoundingBox().move(delta)).iterator().hasNext())
            return false;
        BlockPos support = BlockPos.containing(next.x, next.y - .12, next.z);
        return level().getBlockState(support).isFaceSturdy(level(), support, Direction.UP);
    }

    private boolean step(Vec3 delta) {
        double distance = delta.horizontalDistance();
        int slices = Math.max(1, Mth.ceil(distance / .22));
        Vec3 slice = delta.scale(1.0 / slices);
        Vec3 before = position();
        for (int i = 0; i < slices; i++) {
            if (!canStep(slice)) break;
            move(MoverType.SELF, slice);
        }
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
