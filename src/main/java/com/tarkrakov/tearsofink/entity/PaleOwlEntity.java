package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.sound.ModSounds;
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
    private static final int[] HUNT_HOP_ANGLES = {0, 45, -45, 90, -90};
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
    private int swayIn, restIn, flightIn, searchIn, retryIn, fleeIn, approachIn;
    private int huntIn = -1, huntAge, huntReturnIn;
    private boolean initialized;
    private int hootIn = -1, wingSoundIn;

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
            abortHunt();
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
        tag.putInt("OwlHuntCooldown", Math.max(-1, huntIn));
        if (familyArea != null) tag.putLong("OwlFamilyArea", familyArea.asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        huntIn = tag.contains("OwlHuntCooldown")
                ? Math.max(-1, Math.min(HUNT_MIN + HUNT_SPREAD, tag.getInt("OwlHuntCooldown"))) : -1;
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
            // New owls become hungry soon; reloading a saved owl must not restart its cooldown.
            if (huntIn < 0) huntIn = duration(HUNT_FIRST_MIN, HUNT_FIRST_SPREAD);
            if (onGround()) {
                beginRest();
                if (fleeIn > 0) beginTakeoff();
            } else beginFlight();
        }
        if (huntIn > 0) --huntIn;
        if (huntReturnIn > 0) --huntReturnIn;
        if (prey != null && ++huntAge >= HUNT_LIMIT) returnToTrees();
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
        tickOwlSounds();
    }

    /** Quiet positional audio, timed from the same phase stamp as the model. */
    private void tickOwlSounds() {
        if (hootIn < 0) hootIn = duration(HOOT_FIRST_MIN, HOOT_FIRST_SPREAD);
        if (hootIn > 0) --hootIn;
        if (wingSoundIn > 0) --wingSoundIn;
        if (isSilent()) return;
        Phase current = phase();
        if (current == Phase.IDLE || current == Phase.SWAY) {
            if (hootIn == 0 && onGround() && fleeIn == 0) {
                level().playSound(null, getX(), getY() + getBbHeight() * 0.5, getZ(),
                        ModSounds.PALE_OWL_HOOT, getSoundSource(), HOOT_VOLUME, 1.0f);
                hootIn = duration(HOOT_MIN, HOOT_SPREAD);
            }
            return;
        }
        if (wingSoundIn > 0) return;
        int age = phaseAge();
        float volume = switch (current) {
            case TAKEOFF -> age == 6 ? TAKEOFF_WING_VOLUME : 0f;
            case FLY -> age % 30 == 20 ? FLY_WING_VOLUME : 0f;
            case LAND -> age == 8 ? LAND_WING_VOLUME : 0f;
            default -> 0f;
        };
        if (volume > 0f) {
            level().playSound(null, getX(), getY() + getBbHeight() * 0.5, getZ(),
                    ModSounds.PALE_OWL_WING, getSoundSource(), volume, 1.0f);
            wingSoundIn = 8;
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
        abortHunt();
        waypoint = null;
        perch = null;
        prey = null;
        route.clear();
        restingAt = position();
        treeRest = treeAt(restingAt);
        if (treeRest) huntReturnIn = 0;
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
        abortHunt();
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
            if (waypoint == null || !prey.isAlive() || phaseAge() >= HUNT_LAUNCH_LIMIT) {
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
        abortHunt();
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
        if (fleeIn == 0 && tryHunt()) return;
        if (perch != null) { followPerch(); return; }
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
        if (huntIn != 0 || searchIn != 0 || fleeIn != 0 || huntReturnIn != 0) return false;
        ServerLevel level = (ServerLevel)level();
        if (!LoadedFaunaTerrain.search(level)) return false;
        searchIn = SEARCH_INTERVAL;
        Vec3 origin = position();
        boolean perched = onGround();
        var candidates = new ArrayList<>(level.getEntitiesOfClass(CaterpillarEntity.class,
                getBoundingBox().inflate(HUNT_RADIUS, HUNT_VERTICAL_RANGE, HUNT_RADIUS), c -> {
                    Vec3 delta = c.position().subtract(origin);
                    return c.isAlive() && c.variant() == CaterpillarEntity.Variant.PALE
                            && delta.y < -.45 && delta.y >= -HUNT_VERTICAL_RANGE
                            && delta.horizontalDistanceSqr() <= HUNT_RADIUS * HUNT_RADIUS;
                }));
        candidates.sort(Comparator.comparingDouble(c -> c.position().distanceToSqr(origin)));
        // Bound expensive swept-volume probes even in a dense caterpillar colony.
        for (int i = 0; i < Math.min(HUNT_CANDIDATES, candidates.size()); i++) {
            CaterpillarEntity candidate = candidates.get(i);
            if (!canStrike(level, candidate)) continue;
            Vec3 strike = candidate.position().add(0, .25, 0);
            List<Vec3> path = huntPath(strike, perched);
            if (path == null) continue;
            prey = candidate;
            huntAge = 0;
            perch = null;
            route.clear();
            route.addAll(path);
            waypoint = perched ? route.removeFirst() : null;
            setNoGravity(true);
            phase(perched ? Phase.TAKEOFF : Phase.DIVE);
            return true;
        }
        return false;
    }

    /**
     * A seated owl must clear its own branch first. Try a few sideways exits instead
     * of requiring the one line through the crown to the prey to be unobstructed.
     * All segments use the existing loaded-chunk and full-body collision checks.
     * Null = unreachable; empty = direct approach from flight.
     */
    private List<Vec3> huntPath(Vec3 strike, boolean perched) {
        Vec3 origin = position();
        if (!perched) {
            if (clearPath(origin, strike)) return List.of();
            Vec3 above = strike.add(0, 3, 0);
            if (above.y < origin.y && clearPath(origin, above) && clearPath(above, strike))
                return List.of(above);
            return null;
        }
        Vec3 direction = strike.subtract(origin).multiply(1, 0, 1).normalize();
        if (direction.horizontalDistanceSqr() < .01) direction = Vec3.directionFromRotation(0, getYRot());
        for (int angle : HUNT_HOP_ANGLES) {
            double radians = Math.toRadians(angle);
            double dx = direction.x * Math.cos(radians) - direction.z * Math.sin(radians);
            double dz = direction.x * Math.sin(radians) + direction.z * Math.cos(radians);
            Vec3 hop = origin.add(dx * 3, angle == 0 ? 1.5 : 2.5, dz * 3);
            if (!clearPath(origin, hop)) continue;
            if (clearPath(hop, strike)) return List.of(hop);
            Vec3 above = strike.add(0, 3, 0);
            if (above.y < hop.y && clearPath(hop, above) && clearPath(above, strike))
                return List.of(hop, above);
        }
        // Some crowns have a narrow vertical exit but no diagonal exit.
        Vec3 rise = origin.add(0, 3, 0);
        if (clearPath(origin, rise) && clearPath(rise, strike)) return List.of(rise);
        return null;
    }

    /** Preserve the last two local prey from lethal owl strikes, not from players.
     *  Check again at contact: another owl may have killed prey between launch and impact.
     */
    private static boolean canStrike(ServerLevel level, CaterpillarEntity candidate) {
        if (candidate.getHealth() > HUNT_DAMAGE) return true;
        Vec3 at = candidate.position();
        return level.getEntitiesOfClass(CaterpillarEntity.class,
                candidate.getBoundingBox().inflate(PREY_RADIUS, PREY_VERTICAL, PREY_RADIUS),
                c -> c != candidate && c.isAlive() && c.variant() == CaterpillarEntity.Variant.PALE
                        && Math.abs(c.getY() - at.y) <= PREY_VERTICAL
                        && c.position().subtract(at).horizontalDistanceSqr() <= PREY_RADIUS * PREY_RADIUS)
                .size() >= PREY_RESERVE;
    }

    @Override public void die(DamageSource source) {
        if (level() instanceof ServerLevel server && source.getEntity() instanceof net.minecraft.server.level.ServerPlayer
                && com.tarkrakov.tearsofink.world.ModDimensions.isLands(server)) {
            PaleOwlEncounters.recordPlayerKill(server, blockPosition());
        }
        super.die(source);
    }

    private void abortHunt() {
        if (prey == null) return;
        prey = null;
        waypoint = null;
        route.clear();
        huntIn = duration(HUNT_RETRY_MIN, HUNT_RETRY_SPREAD);
        huntReturnIn = HUNT_RETURN_LIMIT;
    }

    private void returnToTrees() {
        beginFlight();
        flightIn = 0;
    }

    private void tickDive() {
        if (prey == null) { followPerch(); return; }
        if (!prey.isAlive() || prey.variant() != CaterpillarEntity.Variant.PALE) {
            returnToTrees();
            return;
        }
        while (!route.isEmpty() && position().distanceToSqr(route.peek()) < .04) route.remove();
        if (!route.isEmpty()) {
            if (!steer(route.peek(), DIVE_SPEED)) returnToTrees();
            return;
        }

        Vec3 target = prey.position().add(0, .25, 0);
        if (getBoundingBox().inflate(.16).intersects(prey.getBoundingBox())) {
            // Never bite through a newly placed block or across an unloaded boundary.
            if (!clearPath(position(), target) || !canStrike((ServerLevel)level(), prey)) { returnToTrees(); return; }
            boolean hit = prey.hurt(damageSources().mobAttack(this), HUNT_DAMAGE);
            prey = null;
            huntIn = hit ? duration(HUNT_MIN, HUNT_SPREAD) : duration(HUNT_RETRY_MIN, HUNT_RETRY_SPREAD);
            huntReturnIn = HUNT_RETURN_LIMIT;
            returnToTrees();
        } else if ((huntAge % 5 == 0 && !clearPath(position(), target)) || !steer(target, DIVE_SPEED)) {
            returnToTrees();
        }
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
        if (!freeAt(site)) return false;
        AABB wingSpace = getBoundingBox().move(site.add(0, .85, 0).subtract(position()))
                .inflate(PERCH_WING_CLEARANCE, 0, PERCH_WING_CLEARANCE);
        if (!LoadedFaunaTerrain.free((ServerLevel)level(), this, wingSpace)) return false;
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
