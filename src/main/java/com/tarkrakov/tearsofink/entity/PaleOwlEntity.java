package com.tarkrakov.tearsofink.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.tarkrakov.tearsofink.entity.PaleOwlTuning.*;

/** Peaceful, server-driven bird with bounded local steering and collision-checked destinations. */
public final class PaleOwlEntity extends PathfinderMob {
    public enum Phase { IDLE, SWAY, TAKEOFF, FLY, DIVE, LAND }
    private static final Phase[] PHASES = Phase.values();
    // Phase and its start time travel together; previous phase allows a brief visual handoff.
    private static final EntityDataAccessor<Long> PHASE = SynchedEntityData.defineId(PaleOwlEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> PREVIOUS = SynchedEntityData.defineId(PaleOwlEntity.class, EntityDataSerializers.LONG);

    private Vec3 waypoint, perch;
    private CaterpillarEntity prey;
    private int swayIn, restIn, flightIn, huntIn, searchIn, retryIn, fleeIn;
    private boolean initialized;

    public PaleOwlEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FLYING_SPEED, FLIGHT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, HUNT_DAMAGE).add(Attributes.FOLLOW_RANGE, HUNT_RADIUS);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 0L);
        builder.define(PREVIOUS, 0L);
    }
    public long phaseStamp() { return entityData.get(PHASE); }
    public long previousPhaseStamp() { return entityData.get(PREVIOUS); }
    public static long startTick(long stamp) { return stamp >>> 3; }
    public static Phase phaseOf(long stamp) {
        int id = (int)(stamp & 7);
        return id < PHASES.length ? PHASES[id] : Phase.IDLE;
    }
    public Phase phase() { return phaseOf(phaseStamp()); }
    private int phaseAge() {
        return (int)Math.min(Integer.MAX_VALUE, Math.max(0L, level().getGameTime() - startTick(phaseStamp())));
    }
    private void phase(Phase next) {
        if (phase() == next && phaseStamp() != 0L) return;
        entityData.set(PREVIOUS, phaseStamp());
        entityData.set(PHASE, (level().getGameTime() << 3) | next.ordinal());
    }

    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && isAlive() && !isNoAi()) {
            fleeIn = 100;
            if (initialized) {
                if (phase() == Phase.IDLE || phase() == Phase.SWAY || onGround()) beginTakeoff();
                else beginFlight();
            }
        }
        return accepted;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("OwlHuntCooldown", huntIn);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        huntIn = Math.max(0, tag.getInt("OwlHuntCooldown"));
        initialized = false;
        // A loaded owl chooses a safe new destination; no stale entity or chunk references.
        setNoGravity(false);
    }

    private int duration(int min, int spread) { return min + random.nextInt(spread + 1); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        if (isNoAi()) { setNoGravity(false); return; }
        if (!initialized) {
            initialized = true;
            huntIn = Math.max(huntIn, duration(HUNT_MIN, HUNT_SPREAD));
            if (onGround()) {
                beginRest();
                if (fleeIn > 0) beginTakeoff();
            } else beginFlight();
        }
        if (huntIn > 0) --huntIn;
        if (searchIn > 0) --searchIn;
        if (retryIn > 0) --retryIn;
        if (fleeIn > 0) --fleeIn;
        switch (phase()) {
            case IDLE, SWAY -> tickRest();
            case TAKEOFF -> tickTakeoff();
            case FLY -> tickFlight();
            case DIVE -> tickDive();
            case LAND -> tickLand();
        }
    }

    private void restMotion() {
        // Keep a small downward velocity so vanilla physics continues establishing onGround.
        setNoGravity(false);
        setDeltaMovement(0, -0.08, 0);
    }
    private void beginRest() {
        waypoint = null; perch = null; prey = null;
        restMotion();
        swayIn = duration(SWAY_MIN, SWAY_SPREAD);
        restIn = duration(REST_MIN, REST_SPREAD);
        phase(Phase.IDLE);
    }
    private void tickRest() {
        if (!onGround() || isInWater()) { beginFlight(); return; }
        restMotion();
        if (phase() == Phase.SWAY) {
            if (phaseAge() >= SWAY_TICKS) {
                phase(Phase.IDLE);
                swayIn = duration(SWAY_MIN, SWAY_SPREAD);
            }
            return;
        }
        if (--restIn <= 0 || fleeIn > 0) { beginTakeoff(); return; }
        if (--swayIn <= 0) phase(Phase.SWAY);
    }

    private void beginTakeoff() {
        if (phase() == Phase.TAKEOFF) return;
        waypoint = null; perch = null; prey = null;
        phase(Phase.TAKEOFF);
    }
    private void tickTakeoff() {
        if (phaseAge() < 5 && onGround()) { restMotion(); return; }
        setNoGravity(true);
        if (waypoint == null) {
            waypoint = chooseWaypoint(true);
            if (waypoint == null) {
                if (onGround()) { fleeIn = 0; beginRest(); }
                else beginFlight();
                return;
            }
        }
        if (!steer(waypoint, FLIGHT_SPEED * 0.8) || phaseAge() >= TAKEOFF_TICKS) beginFlight();
    }

    private void beginFlight() {
        prey = null; perch = null; waypoint = null;
        flightIn = Math.max(fleeIn, duration(FLIGHT_MIN, FLIGHT_SPREAD));
        retryIn = 0;
        setNoGravity(true);
        phase(Phase.FLY);
    }
    private void tickFlight() {
        setNoGravity(true);
        if (flightIn > 0) --flightIn;
        if (fleeIn == 0 && huntIn == 0 && searchIn == 0) {
            searchIn = SEARCH_INTERVAL;
            CaterpillarEntity found = findPrey();
            if (found != null) {
                prey = found; waypoint = null; perch = null;
                huntIn = duration(HUNT_MIN, HUNT_SPREAD);
                phase(Phase.DIVE);
                return;
            }
        }
        if (flightIn <= 0 && retryIn == 0) {
            retryIn = 20;
            Vec3 site = choosePerch();
            if (site != null) {
                perch = site; waypoint = null;
                phase(Phase.DIVE);
                return;
            }
        }
        if (waypoint != null && (position().distanceToSqr(waypoint) < 0.35 || horizontalCollision || verticalCollision))
            waypoint = null;
        if (waypoint == null && (retryIn == 0 || tickCount % 10 == 0)) {
            waypoint = chooseWaypoint(false);
            if (waypoint == null) retryIn = Math.max(retryIn, 10);
        }
        if (waypoint != null) {
            if (!steer(waypoint, FLIGHT_SPEED)) { waypoint = null; retryIn = Math.max(retryIn, 10); }
        } else {
            setDeltaMovement(Vec3.ZERO);
            if (flightIn <= 0 && phaseAge() > 240) {
                perch = null;
                phase(Phase.LAND);
                setNoGravity(false);
            }
        }
    }

    private CaterpillarEntity findPrey() {
        CaterpillarEntity nearest = null;
        double best = HUNT_RADIUS * HUNT_RADIUS;
        for (CaterpillarEntity candidate : level().getEntitiesOfClass(CaterpillarEntity.class,
                getBoundingBox().inflate(HUNT_RADIUS), c -> c.isAlive() && c.variant() == CaterpillarEntity.Variant.PALE)) {
            double distance = distanceToSqr(candidate);
            if (distance >= best || candidate.getY() > getY() - 0.45 || !hasLineOfSight(candidate)) continue;
            if (!clearPath(position(), candidate.position().add(0, 0.35, 0))) continue;
            best = distance; nearest = candidate;
        }
        return nearest;
    }

    private void tickDive() {
        setNoGravity(true);
        if (prey != null) {
            if (!prey.isAlive() || prey.variant() != CaterpillarEntity.Variant.PALE ||
                    distanceToSqr(prey) > HUNT_RADIUS * HUNT_RADIUS || !hasLineOfSight(prey) ||
                    phaseAge() >= DIVE_LIMIT) { beginFlight(); return; }
            if (distanceToSqr(prey) <= STRIKE_RANGE * STRIKE_RANGE) {
                prey.hurt(damageSources().mobAttack(this), HUNT_DAMAGE);
                beginFlight(); // exactly one hit per approach
                return;
            }
            if (!steer(prey.position().add(0, 0.25, 0), DIVE_SPEED)) beginFlight();
            return;
        }
        if (perch == null || !validPerch(perch) || phaseAge() >= DIVE_LIMIT) { beginFlight(); return; }
        Vec3 approach = perch.add(0, 0.85, 0);
        if (position().distanceToSqr(approach) < 0.12) {
            phase(Phase.LAND);
            return;
        }
        if (!steer(approach, FLIGHT_SPEED)) beginFlight();
    }
    private void tickLand() {
        if (perch != null && !validPerch(perch)) { beginFlight(); return; }
        if (onGround()) {
            restMotion();
            if (phaseAge() >= LAND_TICKS) beginRest();
            return;
        }
        if (perch == null) {
            setNoGravity(false);
            if (phaseAge() > 100) beginFlight();
            return;
        }
        setNoGravity(true);
        if (position().distanceToSqr(perch) < 0.025) {
            restMotion(); // contact is established by vanilla collision, not teleportation
        } else if (!steer(perch, LAND_SPEED)) { beginFlight(); return; }
        if (phaseAge() > 65 && !onGround()) beginFlight();
    }

    private Vec3 chooseWaypoint(boolean rising) {
        Vec3 origin = position();
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double length = rising ? 2.0 + random.nextDouble() * 3.0 : 3.0 + random.nextDouble() * 5.0;
            double dy = rising ? 1.5 + random.nextDouble() * 2.0 : random.nextDouble() * 4.0 - 1.5;
            Vec3 candidate = origin.add(Math.cos(angle) * length, dy, Math.sin(angle) * length);
            if (clearPath(origin, candidate)) return candidate;
        }
        Vec3 above = origin.add(0, 2, 0);
        return clearPath(origin, above) ? above : null;
    }
    private Vec3 choosePerch() {
        Vec3 origin = position();
        Vec3 fallback = null;
        for (int i = 0; i < 16; i++) {
            BlockPos column = i == 0 ? blockPosition() : blockPosition().offset(random.nextInt(13) - 6, 0, random.nextInt(13) - 6);
            if (!level().hasChunkAt(column)) continue;
            int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
            if (y < origin.y - 16 || y > origin.y + 5) continue;
            Vec3 site = Vec3.atBottomCenterOf(new BlockPos(column.getX(), y, column.getZ()));
            if (!validPerch(site) || !clearPath(origin, site.add(0, 0.85, 0))) continue;
            var below = level().getBlockState(BlockPos.containing(site).below());
            if (below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS)) return site;
            if (fallback == null) fallback = site;
        }
        return fallback;
    }
    private boolean validPerch(Vec3 site) {
        BlockPos foot = BlockPos.containing(site);
        BlockPos below = foot.below();
        if (!level().hasChunkAt(foot) || !level().hasChunkAt(below)) return false;
        var support = level().getBlockState(below);
        if (!support.isFaceSturdy(level(), below, Direction.UP) && !support.is(BlockTags.LEAVES)) return false;
        return level().getFluidState(foot).isEmpty() && freeAt(site) && freeAt(site.add(0, 0.85, 0));
    }
    private boolean clearPath(Vec3 from, Vec3 to) {
        int steps = Math.max(1, (int)Math.ceil(from.distanceTo(to) / 0.5));
        for (int i = 1; i <= steps; i++) if (!freeAt(from.lerp(to, (double)i / steps))) return false;
        return true;
    }
    private boolean freeAt(Vec3 point) {
        AABB box = getBoundingBox().move(point.subtract(position()));
        if (box.minY < level().getMinBuildHeight() || box.maxY >= level().getMaxBuildHeight()) return false;
        // Margin for collision-shape checks at chunk borders; do not load chunks for the bird.
        if (!level().hasChunkAt(BlockPos.containing(box.minX - 1, point.y, box.minZ - 1)) ||
                !level().hasChunkAt(BlockPos.containing(box.maxX + 1, point.y, box.minZ - 1)) ||
                !level().hasChunkAt(BlockPos.containing(box.minX - 1, point.y, box.maxZ + 1)) ||
                !level().hasChunkAt(BlockPos.containing(box.maxX + 1, point.y, box.maxZ + 1))) return false;
        return level().getWorldBorder().isWithinBounds(box) && level().noBlockCollision(this, box);
    }
    private boolean steer(Vec3 target, double speed) {
        Vec3 diff = target.subtract(position());
        double length = diff.length();
        if (length < 0.04) { setDeltaMovement(Vec3.ZERO); return true; }
        Vec3 desired = diff.scale(Math.min(speed, length) / length);
        Vec3 movement = getDeltaMovement().lerp(desired, 0.45);
        if (movement.length() > speed) movement = movement.normalize().scale(speed);
        if (!freeAt(position().add(movement))) movement = desired;
        if (!freeAt(position().add(movement))) { setDeltaMovement(Vec3.ZERO); return false; }
        setDeltaMovement(movement);
        if (movement.horizontalDistanceSqr() > 0.0004) {
            float yaw = (float)Math.toDegrees(Math.atan2(-movement.x, movement.z));
            setYRot(Mth.approachDegrees(getYRot(), yaw, 12f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
        return true;
    }
}
