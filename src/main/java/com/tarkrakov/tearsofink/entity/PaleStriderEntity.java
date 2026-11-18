package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.sound.StriderSong;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** D-056: peaceful atmospheric walker, synchronized singing, no combat or loot. */
public final class PaleStriderEntity extends PathfinderMob {
    public enum Phase { IDLE, WALK, SING_START, SING_LOOP, SING_END }
    private static final Phase[] PHASES = Phase.values();
    private static final EntityDataAccessor<Long> PHASE = SynchedEntityData.defineId(PaleStriderEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> PREVIOUS = SynchedEntityData.defineId(PaleStriderEntity.class, EntityDataSerializers.LONG);

    // Slow real movement to match the author's 10-second walk cycle. Tune after viewing.
    public static final double WALK_SPEED = 0.045;
    private static final int START_TICKS = 40, LOOP_TICKS = 80, END_TICKS = 40, LOOPS = 3;
    private int songIn = -1, stillTicks, movingTicks;
    private boolean encounterSpawn;
    private UUID songEpisode;
    private long songStart;
    private enum Mode { TEMPLE, ROAMING }
    private Mode mode = Mode.ROAMING;
    private UUID group;
    private BlockPos choirAnchor, destination;
    private Vec3 waypoint;
    private float heading;
    private int routeIn, wanderPause, groupIn, waitForGroup;
    private UUID leader;
    private boolean followerBehind;

    public PaleStriderEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
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
    private void phase(Phase next) {
        if (phase() == next && phaseStamp() != 0L) return;
        entityData.set(PREVIOUS, phaseStamp());
        entityData.set(PHASE, (level().getGameTime() << 3) | next.ordinal());
    }
    private boolean singing() { return phase().ordinal() >= Phase.SING_START.ordinal(); }

    public void configureEncounter(boolean temple, UUID group, float heading) {
        encounterSpawn = true;
        mode = temple ? Mode.TEMPLE : Mode.ROAMING;
        choirAnchor = temple ? blockPosition() : null;
        this.group = group;
        this.heading = heading;
        songIn = temple ? 100 + random.nextInt(120) : 360 + random.nextInt(240);
    }

    @Override public boolean removeWhenFarAway(double distance) { return encounterSpawn; }
    @Override public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity other) {}
    @Override public boolean canBeLeashed() { return false; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("StriderEncounter", encounterSpawn);
        tag.putBoolean("StriderTempleChoir", mode == Mode.TEMPLE);
        tag.putString("StriderMode", mode.name());
        tag.putFloat("StriderHeading", heading);
        if (group != null) tag.putUUID("StriderGroup", group);
        if (destination != null) tag.putLong("StriderDestination", destination.asLong());
        if (choirAnchor != null) tag.putLong("StriderChoirAnchor", choirAnchor.asLong());
        tag.putInt("StriderSongIn", Math.max(0, songIn));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        encounterSpawn = tag.getBoolean("StriderEncounter");
        mode = "TEMPLE".equals(tag.getString("StriderMode")) || tag.getBoolean("StriderTempleChoir")
                ? Mode.TEMPLE : Mode.ROAMING;
        group = tag.hasUUID("StriderGroup") ? tag.getUUID("StriderGroup") : null;
        heading = tag.contains("StriderHeading") && Float.isFinite(tag.getFloat("StriderHeading"))
                ? tag.getFloat("StriderHeading") : getYRot();
        if (tag.contains("StriderDestination")) destination = BlockPos.of(tag.getLong("StriderDestination"));
        choirAnchor = mode == Mode.TEMPLE && tag.contains("StriderChoirAnchor") ? BlockPos.of(tag.getLong("StriderChoirAnchor")) : null;
        songIn = tag.contains("StriderSongIn") ? Math.max(60 + random.nextInt(120), tag.getInt("StriderSongIn")) : -1;
    }

    @Override public void tick() {
        Vec3 before = position();
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (!isAlive() || isNoAi()) {
            stopSong(server);
            if (singing()) phase(Phase.IDLE);
            return;
        }
        if (phaseStamp() == 0L) phase(Phase.IDLE);
        if (songIn < 0) songIn = 140 + random.nextInt(180);

        if (singing()) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (!onGround() || isInWater()) {
                stopSong(server);
                phase(Phase.IDLE);
                songIn = 200;
                return;
            }
            long elapsed = server.getGameTime() - songStart;
            if (elapsed >= START_TICKS + LOOP_TICKS * LOOPS + END_TICKS) {
                songEpisode = null; // Natural end: let the audio tail finish in real time.
                phase(Phase.IDLE);
                songIn = 500 + random.nextInt(501);
            } else if (elapsed >= START_TICKS + LOOP_TICKS * LOOPS) phase(Phase.SING_END);
            else if (elapsed >= START_TICKS) phase(Phase.SING_LOOP);
            return;
        }

        // Never animate footsteps during standing, singing, or a failed path.
        boolean moving = position().subtract(before).horizontalDistanceSqr() > 0.000025;
        if (moving) { movingTicks++; stillTicks = 0; }
        else { stillTicks++; movingTicks = 0; }
        if (movingTicks >= 3 && phase() != Phase.WALK) phase(Phase.WALK);
        if (stillTicks >= 5 && phase() != Phase.IDLE) phase(Phase.IDLE);

        if (songIn > 0) --songIn;
        if (songIn <= 0 && canSing(server)) {
            List<PaleStriderEntity> neighbours = server.getEntitiesOfClass(PaleStriderEntity.class,
                    getBoundingBox().inflate(mode == Mode.TEMPLE ? 24 : 12, 8, mode == Mode.TEMPLE ? 24 : 12),
                    e -> e != this && e.canSing(server) && (mode == Mode.TEMPLE && e.mode == Mode.TEMPLE
                            || group != null && group.equals(e.group)));
            neighbours.sort(Comparator.comparing(PaleStriderEntity::getUUID));
            int count = Math.min(2, neighbours.size());
            startSong(server, 0, count > 0);
            for (int i = 0; i < count; i++) neighbours.get(i).startSong(server, i + 1, true);
            return;
        }
        walk(server);
    }

    private boolean canSing(ServerLevel server) {
        return isAlive() && !isNoAi() && !singing() && onGround() && !isInWater()
                && (ModDimensions.isLands(server) || !encounterSpawn)
                && !Sanctuary.inside(server, getX(), getZ());
    }

    private void walk(ServerLevel level) {
        if (!ModDimensions.isLands(level) || !onGround() || isInWater()) return;
        if (wanderPause > 0) { --wanderPause; return; }
        if (group != null && --groupIn <= 0) updateGroup(level);
        PaleStriderEntity guide = leader != null && level.getEntity(leader) instanceof PaleStriderEntity mob
                && mob.isAlive() && group.equals(mob.group) ? mob : null;
        if (guide == null && followerBehind && waitForGroup++ < 60) return;
        if (!followerBehind) waitForGroup = 0;

        if (waypoint != null && (horizontalCollision || position().distanceToSqr(waypoint) < .22)) {
            waypoint = null;
            if (horizontalCollision) routeIn = 8 + random.nextInt(12);
        }
        if (guide != null && distanceToSqr(guide) < 4 * 4) {
            waypoint = null;
            return;
        }
        if (waypoint == null && --routeIn <= 0 && LoadedFaunaTerrain.search(level)) {
            routeIn = 10 + random.nextInt(12);
            planStep(level, guide);
        }
        if (waypoint == null) return;
        Vec3 direction = waypoint.subtract(position()).multiply(1, 0, 1).normalize();
        Vec3 step = direction.scale(WALK_SPEED);
        Vec3 next = position().add(direction.scale(.28));
        if (floorNear(level, next.x, next.z, Mth.floor(getY() + .05)) == null) {
            waypoint = null;
            return;
        }
        setDeltaMovement(step.x, getDeltaMovement().y, step.z);
        setYRot(Mth.approachDegrees(getYRot(), (float)Math.toDegrees(Math.atan2(-step.x, step.z)), 8));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void updateGroup(ServerLevel level) {
        groupIn = 40 + random.nextInt(20);
        var members = level.getEntitiesOfClass(PaleStriderEntity.class, getBoundingBox().inflate(40, 12, 40),
                mob -> mob != this && mob.isAlive() && group.equals(mob.group));
        var first = members.stream().min(Comparator.comparing(PaleStriderEntity::getUUID)).orElse(this);
        leader = first.getUUID().compareTo(getUUID()) < 0 ? first.getUUID() : null;
        followerBehind = leader == null && members.stream().anyMatch(mob -> distanceToSqr(mob) > 12 * 12);
    }

    private void planStep(ServerLevel level, PaleStriderEntity guide) {
        if (guide != null) {
            heading = guide.heading;
            Vec3 side = Vec3.directionFromRotation(0, heading + (getUUID().hashCode() % 2 == 0 ? 90 : -90));
            Vec3 site = guide.position().add(side.scale(3 + Math.floorMod(getUUID().hashCode(), 3)));
            waypoint = localStep(level, site);
            return;
        }
        if (destination != null && (distanceToSqr(Vec3.atBottomCenterOf(destination)) < 2.5 * 2.5)) {
            destination = null;
            wanderPause = 30 + random.nextInt(90);
            return;
        }
        if (destination != null && (!validDistrict(level, destination)
                || !LoadedFaunaTerrain.free(level, this, boxAt(Vec3.atBottomCenterOf(destination))))) destination = null;
        if (destination == null) destination = chooseDestination(level);
        Vec3 target = destination != null ? Vec3.atBottomCenterOf(destination)
                : position().add(Vec3.directionFromRotation(0, heading).scale(8));
        waypoint = localStep(level, target);
        if (waypoint == null) {
            destination = null;
            heading += 65 + random.nextInt(135);
            routeIn = 25 + random.nextInt(35);
        } else {
            Vec3 delta = waypoint.subtract(position());
            heading = (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
        }
    }

    private BlockPos chooseDestination(ServerLevel level) {
        for (int i = 0; i < 8; i++) {
            double length = mode == Mode.TEMPLE ? 4 + random.nextDouble() * 7 : 24 + random.nextDouble() * 40;
            Vec3 from = mode == Mode.TEMPLE && choirAnchor != null ? Vec3.atBottomCenterOf(choirAnchor) : position();
            Vec3 sample = from.add(Vec3.directionFromRotation(0, heading + random.nextFloat() * 130 - 65).scale(length));
            BlockPos column = BlockPos.containing(sample);
            var chunk = LoadedFaunaTerrain.chunk(level, column);
            if (chunk == null) continue;
            int y = LoadedFaunaTerrain.surface(chunk, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            BlockPos at = column.atY(y);
            if (Math.abs(at.getY() - getY()) <= 10 && validDistrict(level, at)
                    && floorAt(level, sample.x, sample.z, y)) return BlockPos.containing(sample.x, y, sample.z);
        }
        return null;
    }

    private Vec3 localStep(ServerLevel level, Vec3 goal) {
        Vec3 offset = goal.subtract(position());
        float yaw = (float)Math.toDegrees(Math.atan2(-offset.x, offset.z));
        int side = random.nextBoolean() ? 1 : -1;
        for (int turn : new int[]{0, 30 * side, -30 * side, 60 * side, -60 * side, 100 * side, -100 * side}) {
            Vec3 delta = Vec3.directionFromRotation(0, yaw + turn);
            Vec3 last = position();
            int y = Mth.floor(getY() + .05);
            for (int i = 1; i <= 10; i++) {
                Vec3 sample = position().add(delta.scale(i * .5));
                Vec3 floor = floorNear(level, sample.x, sample.z, y);
                if (floor == null || !validDistrict(level, BlockPos.containing(floor))
                        || !LoadedFaunaTerrain.free(level, this, boxAt(last.add(0, Math.max(0, floor.y - last.y), 0)))) break;
                last = floor;
                y = Mth.floor(floor.y);
            }
            if (last.subtract(position()).horizontalDistanceSqr() >= 2.25) return last;
        }
        return null;
    }

    private boolean validDistrict(ServerLevel level, BlockPos at) {
        return (choirAnchor == null || choirAnchor.distSqr(at) <= 14 * 14)
                && !Sanctuary.near(level, at.getX() + .5, at.getZ() + .5, 3)
                && PaleStriderEncounters.paleAt(level, at);
    }

    private Vec3 floorNear(ServerLevel level, double x, double z, int y) {
        for (int dy : new int[]{0, 1, -1})
            if (floorAt(level, x, z, y + dy)) return new Vec3(x, y + dy, z);
        return null;
    }

    private AABB boxAt(Vec3 at) { return getBoundingBox().move(at.subtract(position())); }

    private boolean floorAt(ServerLevel level, double x, double z, int y) {
        Vec3 at = new Vec3(x, y, z);
        BlockPos foot = BlockPos.containing(at);
        if (!LoadedFaunaTerrain.free(level, this, boxAt(at))) return false;
        var below = foot.below();
        return level.getFluidState(foot).isEmpty()
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private void startSong(ServerLevel server, int part, boolean together) {
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        songEpisode = UUID.randomUUID();
        songStart = server.getGameTime();
        phase(Phase.SING_START);
        StriderSong play = new StriderSong(songEpisode, getX(), getY() + 3, getZ(), part, together ? 0.28f : 0.65f);
        for (var player : server.players()) {
            if (player.distanceToSqr(play.x(), play.y(), play.z()) <= StriderSong.DELIVERY_RANGE * StriderSong.DELIVERY_RANGE
                    && ServerPlayNetworking.canSend(player, StriderSong.TYPE)) ServerPlayNetworking.send(player, play);
        }
    }

    void stopSong(ServerLevel server) {
        if (songEpisode == null) return;
        StriderSong stop = new StriderSong(songEpisode, 0, 0, 0, -1, 0);
        for (var player : server.players())
            if (ServerPlayNetworking.canSend(player, StriderSong.TYPE)) ServerPlayNetworking.send(player, stop);
        songEpisode = null;
    }
}
