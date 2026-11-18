package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.tarkrakov.tearsofink.entity.PaleOwlTuning.*;

/** Peaceful, server-driven bird with bounded local steering and collision-checked destinations. */
public final class PaleOwlEntity extends PathfinderMob {
    public enum Phase { IDLE, SWAY, TAKEOFF, FLY, DIVE, LAND }
    private static final int[] HUNT_HOP_LENGTHS = {2, 4};
    private static final int[] WATER_TURNS = {0, 90, 180, 270};
    private static final Phase[] PHASES = Phase.values();
    // Phase and its start time travel together; previous phase allows a brief visual handoff.
    private static final EntityDataAccessor<Long> PHASE = SynchedEntityData.defineId(PaleOwlEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> PREVIOUS = SynchedEntityData.defineId(PaleOwlEntity.class, EntityDataSerializers.LONG);

    private final ArrayDeque<Vec3> route = new ArrayDeque<>();
    private Vec3 waypoint, perch, restingAt;
    private BlockPos familyArea;
    private boolean treeRest, treeDestination;
    private record Family(BlockPos area) implements SpawnGroupData {}
    private CaterpillarEntity prey;
    private int swayIn, restIn, flightIn, huntIn, searchIn, retryIn, fleeIn, approachIn;
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

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType type, SpawnGroupData data) {
        SpawnGroupData group = super.finalizeSpawn(level, difficulty, type, data);
        if (group instanceof Family family) familyArea = family.area();
        else familyArea = blockPosition();
        return new Family(familyArea);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("OwlHuntCooldown", huntIn);
        if (familyArea != null) tag.putLong("OwlFamilyArea", familyArea.asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        huntIn = Math.max(0, tag.getInt("OwlHuntCooldown"));
        if (tag.contains("OwlFamilyArea")) familyArea = BlockPos.of(tag.getLong("OwlFamilyArea"));
        initialized = false;
        // A loaded owl chooses a safe new destination; no stale entity or chunk references.
        setNoGravity(false);
    }

    private int duration(int min, int spread) { return min + random.nextInt(spread + 1); }

    @Override public void tick() {
        if (level() instanceof ServerLevel level && !LoadedFaunaTerrain.canTick(level, this)) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        if (isNoAi()) { setNoGravity(false); return; }
        if (!initialized) {
            initialized = true;
            if (familyArea == null || familyArea.distSqr(blockPosition()) > 32 * 32) familyArea = blockPosition();
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
        if (isInWater()) {
            escapeWater();
            return;
        }
        switch (phase()) {
            case IDLE, SWAY -> tickRest();
            case TAKEOFF -> tickTakeoff();
            case FLY -> tickFlight();
            case DIVE -> tickDive();
            case LAND -> tickLand();
        }
    }

    private void escapeWater() {
        beginTakeoff();
        ServerLevel level = (ServerLevel)level();
        Vec3 up = new Vec3(0, FLIGHT_SPEED, 0);
        for (int i = -1; i < WATER_TURNS.length; i++) {
            Vec3 move = i == -1 ? up : up.add(Vec3.directionFromRotation(0, getYRot() + WATER_TURNS[i]).scale(.12));
            if (LoadedFaunaTerrain.free(level, this, getBoundingBox().move(move))) {
                setNoGravity(true);
                setDeltaMovement(move);
                return;
            }
        }
        setNoGravity(false);
    }

    private void restMotion() {
        // Keep a small downward velocity so vanilla physics continues establishing onGround.
        setNoGravity(false);
        setDeltaMovement(0, -0.08, 0);
    }
    private void beginRest() {
        waypoint = null;
        perch = null;
        prey = null;
        route.clear();
        restingAt = position();
        treeRest = treeAt(restingAt);
        restMotion();
        swayIn = duration(SWAY_MIN, SWAY_SPREAD);
        restIn = treeRest ? duration(REST_MIN, REST_SPREAD) : duration(40, 60);
        phase(Phase.IDLE);
    }

    private void tickRest() {
        if (isInWater() || !onGround() || (treeRest && (!treeAt(restingAt) || !validPerch(restingAt)))) {
            beginTakeoff();
            return;
        }
        restMotion();
        if (treeRest && tryHunt()) return;
        if (--restIn <= 0 || fleeIn > 0) { beginTakeoff(); return; }
        if (phase() == Phase.SWAY) {
            if (phaseAge() >= SWAY_TICKS) {
                phase(Phase.IDLE);
                swayIn = duration(SWAY_MIN, SWAY_SPREAD);
            }
        } else if (--swayIn <= 0) phase(Phase.SWAY);
    }

    private void beginTakeoff() {
        if (phase() == Phase.TAKEOFF) return;
        waypoint = null;
        perch = null;
        prey = null;
        route.clear();
        phase(Phase.TAKEOFF);
    }

    private void tickTakeoff() {
        if (phaseAge() < 5 && onGround()) { restMotion(); return; }
        setNoGravity(true);
        if (prey != null) {
            if (waypoint == null || !prey.isAlive() || phaseAge() >= 45) {
                returnToTrees();
            } else if (position().distanceToSqr(waypoint) < .04) {
                waypoint = null;
                phase(Phase.DIVE);
            } else if (!steer(waypoint, FLIGHT_SPEED)) {
                returnToTrees();
            }
            return;
        }
        if (route.isEmpty() && waypoint == null) {
            if (!planPerch(true)) waypoint = chooseWaypoint(true);
            if (route.isEmpty() && waypoint == null) {
                fleeIn = 0;
                if (onGround()) beginRest();
                else {
                    beginFlight();
                    flightIn = 0;
                }
                return;
            }
        }
        if (phaseAge() >= TAKEOFF_TICKS) { phase(Phase.FLY); return; }
        if (perch != null) followPerch();
        else if (!steer(waypoint, FLIGHT_SPEED * .8)) beginFlight();
    }

    private void beginFlight() {
        prey = null;
        perch = null;
        waypoint = null;
        route.clear();
        flightIn = duration(FLIGHT_MIN, FLIGHT_SPREAD);
        retryIn = 0;
        setNoGravity(true);
        phase(Phase.FLY);
    }

    private void tickFlight() {
        setNoGravity(true);
        if (flightIn > 0) --flightIn;
        if (perch != null) { followPerch(); return; }
        if (fleeIn == 0 && tryHunt()) return;
        if (retryIn == 0) {
            retryIn = duration(16, 12);
            if (planPerch(true) || (flightIn == 0 && planPerch(false))) {
                followPerch();
                return;
            }
            if (waypoint == null) waypoint = chooseWaypoint(false);
        }
        if (waypoint != null && (position().distanceToSqr(waypoint) < .12 || !steer(waypoint, FLIGHT_SPEED))) {
            waypoint = null;
        }
        if (waypoint == null) {
            setDeltaMovement(Vec3.ZERO);
            if (phaseAge() > 240 && flightIn <= 0) {
                phase(Phase.LAND);
                setNoGravity(false);
            }
        }
    }

    private boolean tryHunt() {
        if (huntIn != 0 || searchIn != 0 || fleeIn != 0 ||
                !LoadedFaunaTerrain.search((ServerLevel)level())) return false;
        searchIn = SEARCH_INTERVAL;
        CaterpillarEntity found = null;
        Vec3 launch = null;
        boolean perched = onGround();
        double best = Double.MAX_VALUE;
        for (var candidate : level().getEntitiesOfClass(CaterpillarEntity.class, getBoundingBox().inflate(HUNT_RADIUS, 20, HUNT_RADIUS),
                c -> c.isAlive() && c.variant() == CaterpillarEntity.Variant.PALE)) {
            Vec3 delta = candidate.position().subtract(position());
            if (delta.y > -.45 || delta.horizontalDistanceSqr() > HUNT_RADIUS * HUNT_RADIUS
                    || delta.lengthSqr() >= best) continue;
            Vec3 strike = candidate.position().add(0, .25, 0);
            Vec3 from = perched ? launchTowards(strike) : position();
            if (from == null || (!perched && !clearPath(from, strike))) continue;
            found = candidate;
            launch = perched ? from : null;
            best = delta.lengthSqr();
        }
        if (found == null) return false;
        prey = found;
        waypoint = launch;
        perch = null;
        route.clear();
        huntIn = duration(HUNT_MIN, HUNT_SPREAD);
        setNoGravity(true);
        phase(launch == null ? Phase.DIVE : Phase.TAKEOFF);
        return true;
    }

    private Vec3 launchTowards(Vec3 strike) {
        Vec3 direction = strike.subtract(position()).multiply(1, 0, 1).normalize();
        for (int length : HUNT_HOP_LENGTHS) {
            Vec3 hop = position().add(direction.scale(length)).add(0, 1.5, 0);
            if (clearPath(position(), hop) && clearPath(hop, strike)) return hop;
        }
        return null;
    }

    private void returnToTrees() {
        beginFlight();
        flightIn = 0;
    }

    private void tickDive() {
        if (prey == null) { followPerch(); return; }
        Vec3 target = prey.position().add(0, .25, 0);
        if (!prey.isAlive() || prey.variant() != CaterpillarEntity.Variant.PALE ||
                phaseAge() >= DIVE_LIMIT || !clearPath(position(), target)) {
            returnToTrees();
            return;
        }
        if (getBoundingBox().inflate(.16).intersects(prey.getBoundingBox())) {
            prey.hurt(damageSources().mobAttack(this), HUNT_DAMAGE);
            returnToTrees();
        } else if (!steer(target, DIVE_SPEED)) returnToTrees();
    }

    private void followPerch() {
        if (--approachIn <= 0 || !validTargetPerch()) { returnToTrees(); return; }
        while (!route.isEmpty() && position().distanceToSqr(route.peek()) < .035) route.remove();
        if (route.isEmpty()) { phase(Phase.LAND); return; }
        Vec3 target = route.peek();
        if (phase() != Phase.TAKEOFF) {
            phase(target.y < getY() - 1.1 ? Phase.DIVE : Phase.FLY);
        }
        if (!steer(target, FLIGHT_SPEED)) returnToTrees();
    }

    private void tickLand() {
        if (isInWater()) { beginTakeoff(); return; }
        if (perch != null && !validTargetPerch()) { returnToTrees(); return; }
        if (onGround()) {
            restMotion();
            if (phaseAge() >= LAND_TICKS) beginRest();
            return;
        }
        if (perch == null) {
            if (LoadedFaunaTerrain.loaded((ServerLevel)level(), getBoundingBox(), 2)) setNoGravity(false);
            else setDeltaMovement(Vec3.ZERO);
            if (phaseAge() > 100) returnToTrees();
            return;
        }
        if (position().distanceToSqr(perch) < .025) restMotion();
        else if (!steer(perch, LAND_SPEED)) returnToTrees();
        if (phaseAge() > 65) returnToTrees();
    }

    private Vec3 chooseWaypoint(boolean rising) {
        Vec3 origin = position();
        Vec3 above = origin.add(0, rising ? 3 : 2, 0);
        if (rising && clearPath(origin, above)) return above;
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double length = 2 + random.nextDouble() * 4;
            double dy = rising ? 1.5 + random.nextDouble() * 3 : random.nextDouble() * 3 - 1;
            Vec3 candidate = origin.add(Math.cos(angle) * length, dy, Math.sin(angle) * length);
            if (clearPath(origin, candidate)) return candidate;
        }
        return clearPath(origin, above) ? above : null;
    }

    private boolean planPerch(boolean trees) {
        ServerLevel level = (ServerLevel)level();
        if (!LoadedFaunaTerrain.search(level)) return false;
        List<Vec3> candidates = new ArrayList<>();
        BlockPos origin = blockPosition();
        for (int i = 0; i < (trees ? 24 : 12); i++) {
            int radius = i < 10 ? 5 : 16;
            BlockPos center = i >= 19 && familyArea.distSqr(origin) < 22 * 22 ? familyArea : origin;
            BlockPos column = i == 0 ? origin : center.offset(random.nextInt(2 * radius + 1) - radius, 0,
                    random.nextInt(2 * radius + 1) - radius);
            var chunk = LoadedFaunaTerrain.chunk(level, column);
            if (chunk == null) continue;
            int x = column.getX(), z = column.getZ();
            int top = LoadedFaunaTerrain.surface(chunk, Heightmap.Types.MOTION_BLOCKING, x, z);
            int min = Math.max(level.getMinBuildHeight() + 1, Math.max(top - 15, origin.getY() - 20));
            for (int y = Math.min(top, origin.getY() + 24); y >= min; --y) {
                BlockPos foot = column.atY(y);
                var support = chunk.getBlockState(foot.below());
                if (trees != isTree(support) || (!trees && y != top)) continue;
                Vec3 site = Vec3.atBottomCenterOf(foot);
                if (trees && treeRest && restingAt != null && site.distanceToSqr(restingAt) < 3) continue;
                if (validPerch(site)) { candidates.add(site); break; }
            }
        }
        candidates.sort(Comparator.comparingDouble(site -> {
            double distance = site.distanceToSqr(position());
            return distance + .18 * site.distanceToSqr(Vec3.atBottomCenterOf(familyArea))
                    + (distance < 16 && treeRest ? 30 : 0);
        }));
        for (int i = 0; i < Math.min(6, candidates.size()); i++) {
            Vec3 site = candidates.get(i);
            if (!level.getEntitiesOfClass(PaleOwlEntity.class, new AABB(site, site).inflate(24, 24, 24),
                    owl -> owl != this && (owl.position().distanceToSqr(site) < 4
                            || owl.perch != null && owl.perch.distanceToSqr(site) < 2)).isEmpty()) continue;
            List<Vec3> path = approach(site);
            if (path.isEmpty()) continue;
            perch = site;
            treeDestination = trees;
            approachIn = 220;
            waypoint = null;
            route.clear();
            route.addAll(path);
            return true;
        }
        return false;
    }

    private List<Vec3> approach(Vec3 site) {
        Vec3 from = position();
        Vec3 arrival = site.add(0, .85, 0);
        if ((phase() != Phase.TAKEOFF || arrival.y >= from.y + 1) && clearPath(from, arrival)) {
            return List.of(arrival);
        }
        double above = Math.max(from.y + 2, arrival.y + 1);
        for (int lift : new int[]{0, 3, 6}) {
            Vec3 rise = new Vec3(from.x, above + lift, from.z);
            Vec3 across = new Vec3(arrival.x, rise.y, arrival.z);
            if (clearPath(from, rise) && clearPath(rise, across) && clearPath(across, arrival)) {
                return List.of(rise, across, arrival);
            }
        }
        return List.of();
    }

    private boolean treeAt(Vec3 site) {
        BlockPos below = BlockPos.containing(site.add(0, .02, 0)).below();
        var chunk = LoadedFaunaTerrain.chunk((ServerLevel)level(), below);
        return chunk != null && isTree(chunk.getBlockState(below));
    }

    private static boolean isTree(BlockState state) {
        return state.is(ModBlocks.PALE_LEAVES) || state.is(ModBlocks.PALE_LOG) || state.is(ModBlocks.PALE_WOOD);
    }

    private boolean validTargetPerch() {
        return perch != null && (!treeDestination || treeAt(perch)) && validPerch(perch);
    }

    private boolean validPerch(Vec3 site) {
        if (!freeAt(site) || !freeAt(site.add(0, .85, 0))) return false;
        BlockPos foot = BlockPos.containing(site);
        BlockPos below = foot.below();
        var support = level().getBlockState(below);
        return level().getFluidState(foot).isEmpty() && support.getFluidState().isEmpty()
                && (support.is(ModBlocks.PALE_LEAVES) || support.isFaceSturdy(level(), below, Direction.UP));
    }

    private boolean clearPath(Vec3 from, Vec3 to) {
        int steps = Math.max(1, (int)Math.ceil(from.distanceTo(to) / .5));
        for (int i = 1; i <= steps; i++) if (!freeAt(from.lerp(to, (double)i / steps))) return false;
        return true;
    }

    private boolean freeAt(Vec3 point) {
        AABB box = getBoundingBox().move(point.subtract(position()));
        if (!LoadedFaunaTerrain.free((ServerLevel)level(), this, box)) return false;
        return level().getFluidState(BlockPos.containing(point)).isEmpty();
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
