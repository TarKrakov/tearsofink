package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.sound.ModSounds;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** D-056: peaceful atmospheric walker, synchronized singing, no combat or loot. */
public final class PaleStriderEntity extends PathfinderMob {
    public enum Phase { IDLE, WALK, SING_START, SING_LOOP, SING_END }
    private static final Phase[] PHASES = Phase.values();
    private static final EntityDataAccessor<Long> PHASE = SynchedEntityData.defineId(PaleStriderEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> PREVIOUS = SynchedEntityData.defineId(PaleStriderEntity.class, EntityDataSerializers.LONG);

    // Slow real movement to match the author's 10-second walk cycle. Tune after viewing.
    public static final double WALK_SPEED = 0.045;
    private static final int START_TICKS = 40, LOOP_TICKS = 80, END_TICKS = 40, LOOPS = 3;
    private int songIn = -1, stillTicks, movingTicks, lastSoundCycle = -1;
    private boolean choir, encounterSpawn, templeChoir;
    private BlockPos choirAnchor;

    public PaleStriderEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
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
    private int phaseAge() { return (int)Math.min(Integer.MAX_VALUE, Math.max(0L, level().getGameTime() - startTick(phaseStamp()))); }
    private void phase(Phase next) {
        if (phase() == next && phaseStamp() != 0L) return;
        entityData.set(PREVIOUS, phaseStamp());
        entityData.set(PHASE, (level().getGameTime() << 3) | next.ordinal());
    }
    private boolean singing() { return phase().ordinal() >= Phase.SING_START.ordinal(); }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8, 0.01f) {
            @Override public boolean canUse() { return !singing() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !singing() && super.canContinueToUse(); }
            @Override protected Vec3 getPosition() {
                Vec3 next = super.getPosition();
                if (next == null || choirAnchor == null) return next;
                double dx = next.x - (choirAnchor.getX() + 0.5), dz = next.z - (choirAnchor.getZ() + 0.5);
                if (dx * dx + dz * dz > 12 * 12) return null;
                if (level() instanceof ServerLevel server && Sanctuary.near(server, next.x, next.z, 3)) return null;
                return next;
            }
        });
    }

    public void configureEncounter(boolean temple) {
        encounterSpawn = true;
        templeChoir = temple;
        choirAnchor = temple ? blockPosition() : null;
        songIn = temple ? 100 + random.nextInt(120) : 140 + random.nextInt(180);
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
        tag.putBoolean("StriderTempleChoir", templeChoir);
        if (choirAnchor != null) tag.putLong("StriderChoirAnchor", choirAnchor.asLong());
        tag.putInt("StriderSongIn", Math.max(0, songIn));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        encounterSpawn = tag.getBoolean("StriderEncounter");
        templeChoir = tag.getBoolean("StriderTempleChoir");
        choirAnchor = templeChoir && tag.contains("StriderChoirAnchor") ? BlockPos.of(tag.getLong("StriderChoirAnchor")) : null;
        songIn = tag.contains("StriderSongIn") ? Math.max(0, tag.getInt("StriderSongIn")) : -1;
    }

    @Override public void tick() {
        Vec3 before = position();
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive() || isNoAi()) return;
        if (phaseStamp() == 0L) phase(Phase.IDLE);
        if (songIn < 0) songIn = 140 + random.nextInt(180);

        if (singing()) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (!onGround() || isInWater()) {
                phase(Phase.IDLE);
                songIn = 200;
                return;
            }
            int elapsed = phaseAge();
            switch (phase()) {
                case SING_START -> {
                    if (elapsed >= START_TICKS) {
                        phase(Phase.SING_LOOP);
                        lastSoundCycle = -1;
                        playPhrase(server, 0);
                    }
                }
                case SING_LOOP -> {
                    if (elapsed >= LOOP_TICKS * LOOPS) phase(Phase.SING_END);
                    else playPhrase(server, elapsed / LOOP_TICKS);
                }
                case SING_END -> {
                    if (elapsed >= END_TICKS) {
                        phase(Phase.IDLE);
                        songIn = 500 + random.nextInt(501);
                    }
                }
                default -> {}
            }
            return;
        }

        // Never animate footsteps during standing, singing, or a failed path.
        boolean moving = position().subtract(before).horizontalDistanceSqr() > 0.000025;
        if (moving) { movingTicks++; stillTicks = 0; }
        else { stillTicks++; movingTicks = 0; }
        if (movingTicks >= 3 && phase() != Phase.WALK) phase(Phase.WALK);
        if (stillTicks >= 5 && phase() != Phase.IDLE) phase(Phase.IDLE);

        if (songIn > 0) --songIn;
        if (songIn <= 0 && onGround() && !isInWater()) {
            // Gather nearby available singers only outside protected temple interiors.
            boolean nearTemple = Sanctuary.near(server, getX(), getZ(), 48);
            if (nearTemple) {
                List<PaleStriderEntity> neighbours = server.getEntitiesOfClass(PaleStriderEntity.class,
                        getBoundingBox().inflate(24, 8, 24),
                        e -> e != this && e.isAlive() && !e.isNoAi() && !e.singing() && e.onGround() && !e.isInWater()
                                && !Sanctuary.inside(server, e.getX(), e.getZ()));
                int count = Math.min(2, neighbours.size());
                startSong(count > 0);
                for (int i = 0; i < count; i++) neighbours.get(i).startSong(true);
            } else startSong(false);
        }
    }
    private void startSong(boolean together) {
        choir = together;
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        phase(Phase.SING_START);
        lastSoundCycle = -1;
    }
    private void playPhrase(ServerLevel server, int cycle) {
        if (lastSoundCycle == cycle) return;
        lastSoundCycle = cycle;
        // Fixed-range event and client attenuation are independent of source loudness.
        // Short, quiet 4s phrases; no loud volume multiplier to extend range.
        float pitch = choir ? 0.98f + (getUUID().hashCode() & 3) * 0.013f : 1.0f;
        server.playSound(null, getX(), getY() + 3.0, getZ(), ModSounds.STRIDER_SONG,
                SoundSource.NEUTRAL, choir ? 0.28f : 0.65f, pitch);
    }
}
