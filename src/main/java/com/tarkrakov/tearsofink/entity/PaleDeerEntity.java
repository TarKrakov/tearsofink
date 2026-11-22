package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.vow.Aura;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import static com.tarkrakov.tearsofink.entity.PaleDeerTuning.*;

/** Peaceful ground fauna. Herd decisions and alerts are server-side; clips are synced as phase/start tick. */
public final class PaleDeerEntity extends PathfinderMob {
    public enum Phase { IDLE, WALK, EATING, RUN, WATCH_START, WATCH, WATCH_ROAR, WATCH_END, ROAR }
    private static final Phase[] PHASES = Phase.values();
    private static final EntityDataAccessor<Long> PHASE = SynchedEntityData.defineId(PaleDeerEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> PREVIOUS = SynchedEntityData.defineId(PaleDeerEntity.class, EntityDataSerializers.LONG);
    private UUID herdId, suspect;
    private long seenAlarm;
    private int suspicion, suspicionNeeded, pathIn, personalIn, pendingRoar = -1;
    private int restoredAge;
    private Phase restoredPhase;
    private BlockPos personalTarget, routeGoal;
    private int lastGuard = -1, lastGuardHeading = Integer.MIN_VALUE, guardAttempts;
    private final Map<UUID, Vec3> samples = new HashMap<>();
    private final PaleDeerAudio deerAudio=new PaleDeerAudio();

    public PaleDeerEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED).add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }
    @Override protected void registerGoals() { goalSelector.addGoal(0, new FloatGoal(this)); }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    // Own hoof contacts replace generic block step sounds, not other entity sounds.
    @Override protected void playStepSound(BlockPos pos,BlockState state) {}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 0L); builder.define(PREVIOUS, 0L);
    }
    public long phaseStamp() { return entityData.get(PHASE); }
    public long previousPhaseStamp() { return entityData.get(PREVIOUS); }
    public static long startTick(long stamp) { return stamp >>> 4; }
    public static Phase phaseOf(long stamp) {
        int n = (int)(stamp & 15);
        return n < PHASES.length ? PHASES[n] : Phase.IDLE;
    }
    public Phase phase() { return phaseOf(phaseStamp()); }
    public int phaseAge() { return (int)Math.min(12000L, Math.max(0L, level().getGameTime()-startTick(phaseStamp()))); }
    private void phase(Phase next) {
        if (phaseStamp() != 0 && phase() == next) return;
        entityData.set(PREVIOUS, phaseStamp());
        entityData.set(PHASE, (level().getGameTime()<<4)|next.ordinal());
    }
    public UUID herdId() { return herdId; }
    void configureHerd(UUID id) { herdId = id; }
    public float individualTempo() { return .92f + Math.floorMod(getUUID().hashCode(), 161)/1000f; }
    public int runOffsetMs() { return Math.floorMod(getUUID().hashCode() * 31, 1000); }
    boolean activeNearPlayer(ServerLevel level) {
        for (ServerPlayer p : level.players()) if (p.isAlive() && !p.isSpectator() && distanceToSqr(p) <= ACTIVE_RADIUS*ACTIVE_RADIUS) return true;
        return false;
    }
    private PaleDeerHerd.Group group(ServerLevel level) {
        PaleDeerHerd data = PaleDeerHerd.of(level);
        PaleDeerHerd.Group g = data.get(herdId);
        if (g == null) {
            // Eggs/summons within 20 blocks form a test herd, separate from natural territory quotas.
            for (PaleDeerEntity d : level.getEntitiesOfClass(PaleDeerEntity.class, getBoundingBox().inflate(20),
                    other -> other != this && other.isAlive() && other.herdId != null)) {
                PaleDeerHerd.Group candidate = data.get(d.herdId);
                if (candidate != null && !candidate.natural && candidate.members.size() < HERD_MAX) {
                    g = candidate; break;
                }
            }
            if (g == null) g = data.create(level, blockPosition(), false);
            herdId = g.id;
        }
        data.join(g, getUUID());
        return g;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (herdId != null) tag.putUUID("DeerHerd", herdId);
        tag.putLong("DeerSeenAlarm", seenAlarm);
        tag.putString("DeerPhase", phase().name());
        tag.putInt("DeerPhaseAge", phaseAge());
        tag.putInt("DeerPendingRoar", pendingRoar);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        herdId=tag.hasUUID("DeerHerd")?tag.getUUID("DeerHerd"):null;
        seenAlarm=tag.getLong("DeerSeenAlarm");
        try { restoredPhase=Phase.valueOf(tag.getString("DeerPhase")); } catch (IllegalArgumentException e) { restoredPhase=Phase.IDLE; }
        restoredAge=Math.max(0,Math.min(12000,tag.getInt("DeerPhaseAge")));
        pendingRoar=tag.contains("DeerPendingRoar")?Math.max(-1,Math.min(ROAR_DELAY_MAX,tag.getInt("DeerPendingRoar"))):-1;
    }
    private void alert(ServerLevel level, DamageSource source) {
        if (isNoAi()) return;
        var attacker=source.getEntity();
        if (attacker == null) attacker=source.getDirectEntity();
        if (attacker != null && attacker != this)
            PaleDeerHerd.of(level).alarm(level,group(level),this,attacker.blockPosition());
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        boolean ok=super.hurt(source,amount);
        if (ok && isAlive() && level() instanceof ServerLevel server) alert(server,source);
        return ok;
    }
    @Override public void die(DamageSource source) {
        if (level() instanceof ServerLevel server && herdId != null) {
            PaleDeerHerd data=PaleDeerHerd.of(server);
            PaleDeerHerd.Group g=data.get(herdId);
            if (g!=null) {
                // A killing projectile still warns the surviving members.
                if (!isNoAi() && source.getEntity()!=null)
                    data.alarm(server,g,this,source.getEntity().blockPosition());
                data.died(server,g,getUUID());
            }
        }
        super.die(source);
    }
    private boolean pathArea(ServerLevel level) {
        return LoadedFaunaTerrain.loaded(level,getBoundingBox(),PATH_MARGIN);
    }
    @Override public void tick() {
        if (!(level() instanceof ServerLevel server)) { super.tick(); return; }
        if (!LoadedFaunaTerrain.canTick(server,this)) { getNavigation().stop(); setDeltaMovement(Vec3.ZERO); return; }
        boolean active=activeNearPlayer(server);
        if (!active || (!getNavigation().isDone() && !pathArea(server))) stopRoute();
        double oldX=getX(), oldZ=getZ();
        super.tick();
        if (!active || !isAlive() || isNoAi()) return;
        if (restoredPhase!=null) {
            entityData.set(PHASE,(Math.max(0,server.getGameTime()-restoredAge)<<4)|restoredPhase.ordinal());
            restoredPhase=null;
        }
        if (pathIn>0) --pathIn;
        if (personalIn>0) --personalIn;
        PaleDeerHerd data=PaleDeerHerd.of(server);
        PaleDeerHerd.Group g=group(server);
        data.tick(server,g,this);
        tickBehavior(server,data,g);
        double dx=getX()-oldX,dz=getZ()-oldZ;
        deerAudio.tick(this,g.members.size(),suspect!=null&&suspicion>0,dx*dx+dz*dz);
    }
    private void tickBehavior(ServerLevel server,PaleDeerHerd data,PaleDeerHerd.Group g) {
        int guard=g.guards.indexOf(getUUID());
        if (g.mode==PaleDeerHerd.Mode.PANIC) {
            panic(server,g);
            return;
        }
        pendingRoar=-1;
        seenAlarm=g.alarm;
        if (phase()==Phase.WATCH_ROAR || phase()==Phase.ROAR || phase()==Phase.RUN) phase(Phase.IDLE);
        if (guard!=lastGuard || (guard>=0 && lastGuardHeading!=g.guardHeading)) {
            personalTarget=null; personalIn=0; guardAttempts=0; stopRoute();
            if (phase()==Phase.WATCH || phase()==Phase.WATCH_START) phase(Phase.WATCH_END);
            lastGuard=guard; lastGuardHeading=g.guardHeading;
        }
        if (Math.floorMod(server.getGameTime()+getUUID().hashCode(),SENSE_INTERVAL)==0) sense(server,data,g,guard>=0);
        if (g.mode==PaleDeerHerd.Mode.PANIC) { panic(server,g); return; }
        if (suspect!=null && suspicion>0) {
            stopRoute();
            if (server.getEntity(suspect) instanceof ServerPlayer p) face(p.position());
            if (phase()==Phase.WATCH_START && phaseAge()>=WATCH_START_TICKS) phase(Phase.WATCH);
            else if (phase()!=Phase.WATCH && phase()!=Phase.WATCH_START && phase()!=Phase.WATCH_END) phase(Phase.IDLE);
            return;
        }
        if (phase()==Phase.WATCH_END) {
            stopRoute();
            if (phaseAge()<WATCH_END_TICKS) return;
            phase(Phase.IDLE);
        }
        if (guard>=0) watch(server,g,guard);
        else {
            if (phase()==Phase.WATCH || phase()==Phase.WATCH_START) { stopRoute(); phase(Phase.WATCH_END); return; }
            calm(server,g);
        }
    }
    private void face(Vec3 target) {
        double x=target.x-getX(), z=target.z-getZ();
        if (x*x+z*z<.01) return;
        float yaw=(float)(Math.toDegrees(Math.atan2(z,x))-90);
        setYRot(Mth.approachDegrees(getYRot(),yaw,12));
        yBodyRot=getYRot();
        setYHeadRot(getYRot());
    }
    private void sense(ServerLevel level,PaleDeerHerd data,PaleDeerHerd.Group g,boolean guard) {
        double nearest=Double.MAX_VALUE;
        ServerPlayer candidate=null;
        Map<UUID,Vec3> next=new HashMap<>();
        for (ServerPlayer p:level.players()) {
            double d=distanceToSqr(p);
            if (!p.isAlive() || p.isSpectator() || p.isCreative() || d>20*20) continue;
            Vec3 pos=p.position(), old=samples.get(p.getUUID());
            next.put(p.getUUID(),pos);
            boolean moving=old==null || pos.subtract(old).horizontalDistanceSqr()>.04*.04;
            int gait=!moving?0:p.isShiftKeyDown()?1:p.isSprinting()?3:2;
            int light=Aura.light(p);
            double sight=(guard?GUARD_SIGHT:DEER_SIGHT)[gait]*(light<0?1:Aura.LIGHT_SEEN[Math.min(3,light)]);
            double hear=(guard?GUARD_HEARING:DEER_HEARING)[gait];
            boolean visual=d<=sight*sight && LoadedFaunaTerrain.loaded(level,new AABB(getEyePosition(),p.getEyePosition()),1) && hasLineOfSight(p);
            boolean audible=hear>0 && d<=hear*hear;
            if ((visual||audible) && d<nearest) { nearest=d; candidate=p; }
        }
        samples.clear(); samples.putAll(next);
        if (candidate==null) {
            suspicion=Math.max(0,suspicion-SENSE_INTERVAL*2);
            if (suspicion==0) suspect=null;
            return;
        }
        if (!candidate.getUUID().equals(suspect)) {
            suspect=candidate.getUUID(); suspicion=0;
            suspicionNeeded=SUSPICION_MIN+random.nextInt(SUSPICION_SPREAD+1);
        }
        suspicion+=SENSE_INTERVAL;
        if (suspicion>=suspicionNeeded || nearest<2*2) {
            data.alarm(level,g,this,candidate.blockPosition());
            suspicion=0; suspect=null;
        }
    }
    private void panic(ServerLevel level,PaleDeerHerd.Group g) {
        if (seenAlarm!=g.alarm) {
            seenAlarm=g.alarm;
            pendingRoar=getUUID().equals(g.caller)?0:random.nextInt(ROAR_DELAY_MAX+1);
            suspect=null; suspicion=0; personalTarget=null; personalIn=0;
            stopRoute();
        }
        if (pendingRoar>=0) {
            stopRoute();
            if (pendingRoar-->0) return;
            if (phase()==Phase.WATCH || phase()==Phase.WATCH_START) phase(Phase.WATCH_ROAR);
            else if (phase()==Phase.WATCH_END && phaseAge()<WATCH_END_TICKS) { pendingRoar=0; return; }
            else phase(Phase.ROAR);
        }
        switch (phase()) {
            case WATCH_ROAR -> { if (phaseAge()<WATCH_ROAR_TICKS) return; phase(Phase.WATCH_END); return; }
            case WATCH_END -> { if (phaseAge()<WATCH_END_TICKS) return; }
            case ROAR -> { if (phaseAge()<ROAR_TICKS) return; }
        }
        BlockPos goal=spread(g.target,SPREAD);
        // The escape goal is shared; each member finds its own local path through the trees.
        if (moveToward(level,goal,true)) phase(Phase.RUN);
        else phase(Phase.IDLE);
    }
    private BlockPos spread(BlockPos center,int radius) {
        int hash=getUUID().hashCode();
        double angle=Math.floorMod(hash,360)*Math.PI/180;
        double r=4+Math.floorMod(hash>>>9,Math.max(1,radius-3));
        return center.offset((int)Math.round(Math.cos(angle)*r),0,(int)Math.round(Math.sin(angle)*r));
    }
    private void watch(ServerLevel level,PaleDeerHerd.Group g,int guard) {
        BlockPos edge=g.guardPoint(guard);
        if (personalTarget==null && guardAttempts<3 && pathIn==0 && LoadedFaunaTerrain.search(level)) {
            // Prefer a usable elevated edge within 4 blocks of the desired slot.
            BlockPos best=null;
            for (int i=0;i<7;i++) {
                BlockPos point=i==0?edge:edge.offset(random.nextInt(9)-4,0,random.nextInt(9)-4);
                BlockPos here=PaleDeerEncounters.site(level,point.getX(),point.getZ(),false);
                if (here!=null && (best==null || here.getY()>best.getY())) best=here;
            }
            personalTarget=best;
            if (best==null) { pathIn=PATH_RETRY; guardAttempts++; }
        }
        if (phase()==Phase.WATCH_START) {
            stopRoute();
            if (phaseAge()>=WATCH_START_TICKS) phase(Phase.WATCH);
            return;
        }
        if (phase()==Phase.WATCH) { stopRoute(); return; }
        if (personalTarget!=null && horizontalSq(personalTarget)>3*3 && guardAttempts<3) {
            boolean canTry=pathIn==0;
            if (moveToward(level,personalTarget,false)) { phase(Phase.WALK); return; }
            if (canTry) { guardAttempts++; personalTarget=null; }
            phase(Phase.IDLE); return;
        }
        if (personalTarget==null && guardAttempts<3) { phase(Phase.IDLE); return; }
        if (onGround() && !isInWater()) {
            stopRoute();
            Vec3 outward=position().subtract(Vec3.atCenterOf(g.target));
            if (outward.horizontalDistanceSqr()>.1) face(position().add(outward));
            phase(Phase.WATCH_START);
        }
    }
    private void calm(ServerLevel level,PaleDeerHerd.Group g) {
        BlockPos destination=spread(g.target,SPREAD);
        if (g.mode==PaleDeerHerd.Mode.TRAVEL || g.mode==PaleDeerHerd.Mode.RECOVER || horizontalSq(g.target)>20*20) {
            personalTarget=null; personalIn=0;
            if (moveToward(level,destination,false)) phase(Phase.WALK); else { stopRoute(); phase(Phase.IDLE); }
            return;
        }
        if (phase()==Phase.EATING && phaseAge()<EAT_TICKS) { stopRoute(); return; }
        if (phase()==Phase.EATING) {
            personalIn=40+random.nextInt(100); personalTarget=null; phase(Phase.IDLE);
        }
        if (personalTarget!=null && horizontalSq(personalTarget)>2*2 && personalIn>0) {
            if (moveToward(level,personalTarget,false)) { phase(Phase.WALK); return; }
            if (pathIn>0) personalTarget=null;
        }
        personalTarget=null;
        if (g.mode==PaleDeerHerd.Mode.GRAZE && personalIn==0 && forageNear(level,blockPosition())) {
            stopRoute(); phase(Phase.EATING); return;
        }
        if (personalIn==0 && LoadedFaunaTerrain.search(level)) {
            for (int i=0;i<6;i++) {
                BlockPos q=destination.offset(random.nextInt(13)-6,0,random.nextInt(13)-6);
                BlockPos at=PaleDeerEncounters.site(level,q.getX(),q.getZ(),false);
                if (at!=null && (g.mode!=PaleDeerHerd.Mode.GRAZE || forageNear(level,at))) {
                    personalTarget=at; personalIn=120+random.nextInt(120); break;
                }
            }
            if (personalTarget==null) personalIn=40+random.nextInt(100);
        }
        if (personalTarget!=null && moveToward(level,personalTarget,false)) phase(Phase.WALK);
        else { stopRoute(); phase(Phase.IDLE); }
    }
    static boolean forageNear(ServerLevel level,BlockPos p) {
        if (!LoadedFaunaTerrain.loaded(level,new AABB(p),2)) return false;
        for (BlockPos at:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1))) {
            var state=level.getBlockState(at);
            if (state.is(ModBlocks.PALE_GRASS)||state.is(ModBlocks.TALL_PALE_GRASS)||state.is(ModBlocks.TEARBLOOM)||state.is(BlockTags.FLOWERS)) return true;
        }
        return false;
    }
    private double horizontalSq(BlockPos target) {
        double dx=target.getX()+.5-getX(), dz=target.getZ()+.5-getZ();
        return dx*dx+dz*dz;
    }
    private void stopRoute() { getNavigation().stop(); routeGoal=null; }
    private boolean moveToward(ServerLevel level,BlockPos target,boolean running) {
        if (horizontalSq(target)<2.5*2.5 && Math.abs(target.getY()-getY())<5) { stopRoute(); return false; }
        if (!getNavigation().isDone() && target.equals(routeGoal)) return true;
        if (!getNavigation().isDone()) stopRoute();
        if (pathIn>0 || !pathArea(level) || !LoadedFaunaTerrain.search(level)) return false;
        pathIn=PATH_RETRY+random.nextInt(12);
        Vec3 dir=Vec3.atCenterOf(target).subtract(position());
        dir=new Vec3(dir.x,0,dir.z);
        double distance=Math.min(18,dir.horizontalDistance());
        if (distance<.5) return false;
        dir=dir.normalize();
        double[] angles={0,35,-35,70,-70,105,-105};
        int start=random.nextInt(6);
        for (int i=0;i<4;i++) {
            if (i>0 && !LoadedFaunaTerrain.search(level)) break;
            int index=i==0?0:1+((i-1+start)%6);
            double a=Math.toRadians(angles[index]), cos=Math.cos(a), sin=Math.sin(a);
            double step=i>=2?Math.min(9,distance):distance;
            int x=Mth.floor(getX()+(dir.x*cos-dir.z*sin)*step);
            int z=Mth.floor(getZ()+(dir.z*cos+dir.x*sin)*step);
            BlockPos at=PaleDeerEncounters.site(level,x,z,false);
            if (at==null || Math.abs(at.getY()-getY())>8) continue;
            Path path=getNavigation().createPath(at,0);
            if (path==null || !path.canReach()) continue;
            boolean safe=true;
            for (int n=0;n<path.getNodeCount();n++) {
                var node=path.getNode(n);
                BlockPos p=new BlockPos(node.x,node.y,node.z);
                if (!LoadedFaunaTerrain.loaded(level,new AABB(p),2)
                        || !level.getBiome(p).is(com.tarkrakov.tearsofink.world.ModBiomes.PALE_FOREST)
                        || com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary.inside(
                                level, p.getX() + .5d, p.getZ() + .5d)) { safe=false; break; }
            }
            if (safe && getNavigation().moveTo(path,(running?RUN:WALK)*individualTempo())) {
                routeGoal=target; return true;
            }
        }
        return false;
    }
}
