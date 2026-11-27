package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.sound.ModSounds;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static com.tarkrakov.tearsofink.entity.TrollTerritoryRules.*;

public final class TrollEntity extends PathfinderMob implements Enemy {
    public static final float SCALE = .75f, WIDTH = 2.2f, HEIGHT = 3.6f;
    public static final int IDLE = 0, WALK = 1, RIGHT = 2, LEFT = 3, BELLY = 4, SLAM = 5,
            SCRATCH = 6, SIT = 7, SLEEP = 8, STAND = 9, DEATH = 10;
    private static final int[] DURATION = {60, 80, 40, 40, 40, 58, 60, 15, 240, 15, 41};
    private static final double HEALTH = 100.0;
    private static final int PURSUIT_INTERVAL = 6;
    private static final double ENGAGE_RANGE = 4.1, ATTACK_HEIGHT = 2.7;
    private static final EntityDataAccessor<Long> STATE = SynchedEntityData.defineId(TrollEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> PREVIOUS = SynchedEntityData.defineId(TrollEntity.class, EntityDataSerializers.LONG);

    private static final EntityDataAccessor<Long> SLAM_LANDED = SynchedEntityData.defineId(TrollEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> POWER_SLAM = SynchedEntityData.defineId(TrollEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FAST_WALK = SynchedEntityData.defineId(TrollEntity.class, EntityDataSerializers.BOOLEAN);
    private Vec3 home, bed;
    private Vec3 leapOrigin, leapGoal;
    private int leapTicks, noPlayerHit, leapCooldown;
    private boolean leaping, leapPreviousGravity;
    private BlockPos entrance, forageTree, punchTree;
    private UUID territoryId, unreachablePrey;
    private LivingEntity prey;
    private Vec3 pursuitPosition;
    private float attackYaw;
    private boolean returning, sleepPending, struck, leftPunch, staleCopy;
    private int sleepTravel, returnFailures, pursuitStall, unseenTicks, forageIn, forageTravel, retryPreyIn;
    private int scanIn, moveIn, actionIn, ambientIn, laughIn, attackIn, sleepFor, snoreIn;

    public TrollEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        actionIn = 95 + random.nextInt(85);
        ambientIn = 35 + random.nextInt(75);
        forageIn = 300 + random.nextInt(540);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.MOVEMENT_SPEED, .19)
                .add(Attributes.ATTACK_DAMAGE, 10).add(Attributes.FOLLOW_RANGE, 20)
                .add(Attributes.KNOCKBACK_RESISTANCE, .85).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level) {
            @Override
            protected void followThePath() {
                Vec3 feet = getTempMobPos();
                Vec3 step = path.getNextEntityPos(mob);
                // WalkNodeEvaluator plans a multi-block footprint. Advancing from
                // its corner skips turns before the Troll's body has cleared them.
                if (Math.abs(feet.x - step.x) < .65 && Math.abs(feet.z - step.z) < .65
                        && Math.abs(feet.y - step.y) < 1) {
                    path.advance();
                }
                doStuckDetection(feet);
            }
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, 0L);
        builder.define(PREVIOUS, 0L);
        builder.define(SLAM_LANDED, 0L);
        builder.define(FAST_WALK, false);
        builder.define(POWER_SLAM, false);
    }

    public int clip() { return (int) (entityData.get(STATE) & 15); }
    public long clipStart() { return entityData.get(STATE) >>> 4; }
    public int previousClip() { return (int) (entityData.get(PREVIOUS) & 15); }
    public int previousTicks() { return (int) (entityData.get(PREVIOUS) >>> 4); }
    public long slamLanded(){return entityData.get(SLAM_LANDED);}
    public boolean fastWalk(){return entityData.get(FAST_WALK);}
    private boolean powerSlam(){return entityData.get(POWER_SLAM);}
    private int leapFlightTicks(){return powerSlam()?LEAP_FLIGHT:18;}
    public float animationTicks(int c,float ticks,float partial) {
        if(c!=SLAM)return c==WALK&&fastWalk()?ticks*2:ticks;
        long landed=slamLanded();
        if(landed>0)return 28+Math.max(0,level().getGameTime()-landed+partial);
        if(ticks<=LEAP_WINDUP)return ticks;
        return Math.min(27.8f,10+18*(ticks-LEAP_WINDUP)/leapFlightTicks());
    }

    private int elapsed() {
        return (int) Math.clamp(level().getGameTime() - clipStart(), 0L, 100_000L);
    }

    private void play(int next) {
        if (clip() == next && entityData.get(STATE) != 0) return;
        entityData.set(PREVIOUS, ((long) elapsed() << 4) | clip());
        entityData.set(STATE, (level().getGameTime() << 4) | next);
        struck = false;
    }

    Vec3 homePoint() { return home; }
    Vec3 bedPoint() { return bed == null ? home : bed; }
    UUID territoryId() { return territoryId; }
    BlockPos entrance() { return entrance; }
    void retireStaleCopy() { staleCopy = true; }

    void bindTerritory(UUID id, BlockPos center, BlockPos sleepingSite, BlockPos mouth) {
        territoryId = id;
        home = Vec3.atBottomCenterOf(center);
        bed = Vec3.atBottomCenterOf(sleepingSite);
        entrance = mouth;
        setPersistenceRequired();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putDouble("TrollHomeX", home.x);
            tag.putDouble("TrollHomeY", home.y);
            tag.putDouble("TrollHomeZ", home.z);
        }
        if (territoryId != null) tag.putUUID("TrollTerritory", territoryId);
        if (bed != null) {
            tag.putDouble("TrollBedX", bed.x);
            tag.putDouble("TrollBedY", bed.y);
            tag.putDouble("TrollBedZ", bed.z);
        }
        if (entrance != null) tag.putLong("TrollEntrance", entrance.asLong());
        tag.putInt("TrollLaughIn", laughIn);
        tag.putInt("TrollAttackIn", attackIn);
        if(leaping)tag.putBoolean("NoGravity",leapPreviousGravity);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Migrate only the previous standard base; preserve modifiers, custom
        // base values and the fraction of health remaining in saved worlds.
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getBaseValue() == 64.0) {
            float fraction = getHealth() / getMaxHealth();
            health.setBaseValue(HEALTH);
            setHealth(fraction * getMaxHealth());
        }
        home = tag.contains("TrollHomeX") ? new Vec3(tag.getDouble("TrollHomeX"), tag.getDouble("TrollHomeY"), tag.getDouble("TrollHomeZ")) : null;
        bed = tag.contains("TrollBedX") ? new Vec3(tag.getDouble("TrollBedX"), tag.getDouble("TrollBedY"), tag.getDouble("TrollBedZ")) : home;
        territoryId = tag.hasUUID("TrollTerritory") ? tag.getUUID("TrollTerritory") : null;
        entrance = tag.contains("TrollEntrance", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("TrollEntrance")) : null;
        laughIn = Math.clamp(tag.getInt("TrollLaughIn"), 0, 160);
        attackIn = Math.clamp(tag.getInt("TrollAttackIn"), 0, 60);
        play(isDeadOrDying() ? DEATH : IDLE);
    }

    @Override public boolean canUsePortal(boolean allowPassengers) {
        return territoryId == null && super.canUsePortal(allowPassengers);
    }

    @Override public SoundSource getSoundSource() { return SoundSource.HOSTILE; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.TROLL_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSounds.TROLL_DEATH; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (clip() == WALK) playSound(ModSounds.TROLL_STEP, .75f, .9f + random.nextFloat() * .2f);
    }

    private double homeDistance(Vec3 pos) { return distanceSq(home.x, home.z, pos.x, pos.z); }
    private double bedDistance() { return position().distanceToSqr(bedPoint()); }

    private boolean walkTo(ServerLevel level, Vec3 pos, double speed) {
        if (!LoadedFaunaTerrain.loaded(level, new AABB(position(), pos), 3)) return false;
        var path = pathTo(BlockPos.containing(pos));
        return path != null && path.canReach() && getNavigation().moveTo(path, speed);
    }

    private Path pathTo(BlockPos feet) {
        // The vanilla walking node anchors the north-west corner, while our
        // beds and approach positions are specified at the Troll's center.
        int offset = Mth.floor(getBbWidth() + 1) / 2;
        return getNavigation().createPath(feet.offset(-offset, 0, -offset), 0);
    }

    private boolean eligible(ServerLevel level, LivingEntity entity) {
        return entity != null && entity.level() == level && entity != this && !(entity instanceof TrollEntity) && !(entity instanceof ArmorStand)
                && entity.isAlive() && !entity.isSpectator() && !entity.isInvulnerable() && canAttack(entity)
                && !isAlliedTo(entity) && (!(entity instanceof Player player) || !player.isCreative())
                && (home == null || homeDistance(entity.position()) <= CHASE * CHASE)
                && !Sanctuary.near(level, entity.getX(), entity.getZ(), 0)
                && !BattlefieldFaunaBoundary.inside(level, entity.getX(), entity.getZ());
    }

    private void acquire(LivingEntity entity) {
        if (prey == entity) return;
        if(prey instanceof Player && !(entity instanceof Player))return;
        boolean player=entity instanceof Player;
        prey = entity;
        if(player){noPlayerHit=0;leapCooldown=0;returning=false;}
        forageTree = null;
        sleepPending = false;
        moveIn = 0;
        pursuitStall = 0;
        unseenTicks = 0;
        pursuitPosition = position();
        if(player&&punchTree!=null){punchTree=null;play(IDLE);}
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof TrollEntity) return false;
        boolean applied = super.hurt(source, amount);
        if (applied && level() instanceof ServerLevel level && isAlive() && !returning
                && source.getEntity() instanceof LivingEntity attacker && eligible(level, attacker)
                && (home == null || homeDistance(attacker.position()) <= DEFEND * DEFEND)
                && distanceToSqr(attacker) <= 28 * 28) {
            acquire(attacker);
            if (territoryId != null) {
                for (TrollEntity relative : level.getEntitiesOfClass(TrollEntity.class, getBoundingBox().inflate(14),
                        other -> other != this && other.isAlive() && !other.isNoAi()
                                && territoryId.equals(other.territoryId) && other.prey == null && !other.returning)) {
                    // The nearby resident wakes even if the cave walls conceal the attacker.
                    if (relative.eligible(level, attacker) && relative.distanceToSqr(attacker) <= DETECT * DETECT) {
                        relative.acquire(attacker);
                    }
                }
            }
        }
        return applied;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level) {
            cancelLeap();
            stop();
            play(DEATH);
            if (territoryId != null && ModDimensions.isLands(level)) {
                TrollTerritories.of(level).died(territoryId, getUUID(), level.getGameTime());
            }
        }
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        if (++deathTime >= DURATION[DEATH] + 1 && !level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    private void look(float yaw) {
        setYRot(yaw);
        yBodyRot = yaw;
        setYHeadRot(yaw);
    }

    private float yawTo(LivingEntity target) {
        return (float) Math.toDegrees(Math.atan2(getX() - target.getX(), target.getZ() - getZ()));
    }

    private void stop() {
        getNavigation().stop();
        setXxa(0);
        setZza(0);
    }

    @Override
    public void tick() {
        if (staleCopy) {
            discard();
            return;
        }
        if (level() instanceof ServerLevel level && !LoadedFaunaTerrain.canTick(level, this)) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if(leaping&&!level().isClientSide)setDeltaMovement(Vec3.ZERO);
        super.tick();
        if (!(level() instanceof ServerLevel level) || !isAlive()) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        if (isNoAi()) {cancelLeap();return;}
        if (home == null) home = position();
        if (bed == null) bed = home;
        if (entityData.get(STATE) == 0) play(IDLE);
        if (laughIn > 0) --laughIn;
        if (attackIn > 0) --attackIn;
        if (forageIn > 0) --forageIn;
        if (retryPreyIn > 0) --retryPreyIn;
        if(leapCooldown>0)--leapCooldown;
        if(prey instanceof Player)noPlayerHit=Math.min(10000,noPlayerHit+1);
        entityData.set(FAST_WALK,prey instanceof Player);

        if (homeDistance(position()) >= CHASE * CHASE && !returning) returnHome();
        if (prey != null) {
            unseenTicks = getSensing().hasLineOfSight(prey) ? 0 : unseenTicks + 1;
            if (!eligible(level, prey) || distanceToSqr(prey) > (prey instanceof Player?CHASE*CHASE:28*28) || unseenTicks >= (prey instanceof Player?240:100)) losePrey();
        }
        if (--scanIn <= 0) {
            scanIn = 4;
            if (!returning && !(prey instanceof Player)) scan(level);
        }

        if (tickAnimation(level)) return;
        if (prey != null) {
            fight(level);
        } else if (returning || homeDistance(position()) > ROAM * ROAM) {
            if (!returning) returnHome();
            goHome(level);
        } else {
            idle(level);
        }
    }

    private void scan(ServerLevel level) {
        LivingEntity closest = prey;
        double best = prey == null ? Double.POSITIVE_INFINITY : homeDistance(prey.position());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(DETECT, 5, DETECT), target -> eligible(level, target))) {
            if (!(entity instanceof Player) && retryPreyIn > 0 && entity.getUUID().equals(unreachablePrey)) continue;
            double distance = distanceToSqr(entity);
            if (distance > DETECT * DETECT || homeDistance(entity.position()) > DEFEND * DEFEND) continue;
            boolean preferred = entity instanceof Player && !(closest instanceof Player);
            boolean samePriority = closest == null || (entity instanceof Player) == (closest instanceof Player);
            double priority = entity instanceof Player ? distance : homeDistance(entity.position());
            if ((preferred || samePriority && priority < best) && getSensing().hasLineOfSight(entity)) {
                closest = entity;
                best = priority;
            }
        }
        if (closest != null) acquire(closest);
    }

    private void losePrey() {
        prey = null;
        scanIn = 4;
        stop();
        if (homeDistance(position()) > ROAM * ROAM) returnHome();
    }

    private void returnHome() {
        returning = true;
        sleepPending = false;
        forageTree = null;
        prey = null;
        returnFailures = 0;
        moveIn = 0;
        stop();
    }

    private boolean tickAnimation(ServerLevel level) {
        int clip = clip(), elapsed = elapsed();
        switch (clip) {
            case SLAM -> tickSlam(level,elapsed);
            case RIGHT, LEFT, BELLY -> {
                stop();
                look(attackYaw);
                int impact = clip == BELLY ? 23 : 22;
                if(!struck&&prey instanceof Player&&elapsed<impact-5)
                    attackYaw=Mth.approachDegrees(attackYaw,yawTo(prey),20);
                look(attackYaw);
                if (!struck && elapsed >= impact) {
                    struck = true;
                    if (!returning && onGround()) {
                        if (punchTree == null) {
                            if(clip==BELLY)TrollBlockSmash.belly(level,this,attackYaw);
                            impact(level, clip);
                        } else if (canReachTree(level, punchTree)
                                && TrollTrees.fell(level, BlockPos.containing(home), punchTree, true)) {
                            feed();
                        }
                    }
                }
                if (elapsed >= DURATION[clip]) {
                    attackIn = prey instanceof Player?6:18+random.nextInt(14);
                    punchTree = null;
                    play(IDLE);
                }
            }
            case SIT -> {
                stop();
                if (elapsed >= DURATION[SIT]) {
                    if (prey != null || returning || bedDistance() > 4) {
                        play(STAND);
                    } else {
                        sleepFor = 240 + random.nextInt(361);
                        snoreIn = 12;
                        play(SLEEP);
                    }
                }
            }
            case SLEEP -> {
                stop();
                if (prey != null || returning || bedDistance() > 4 || --sleepFor <= 0) {
                    play(STAND);
                } else if (--snoreIn <= 0) {
                    snoreIn = 75 + random.nextInt(31);
                    playSound(ModSounds.TROLL_SNORE, .76f, .85f + random.nextFloat() * .12f);
                }
            }
            case STAND -> {
                stop();
                if (elapsed >= DURATION[STAND]) {
                    actionIn = 160;
                    play(IDLE);
                }
            }
            case SCRATCH -> {
                stop();
                if (prey != null || returning || elapsed >= DURATION[SCRATCH]) {
                    actionIn = 140;
                    play(IDLE);
                }
            }
            default -> { return false; }
        }
        return true;
    }

    private void fight(ServerLevel level) {
        Vec3 offset = prey.position().subtract(position());
        boolean player=prey instanceof Player;
        double horizontal=offset.horizontalDistanceSqr();
        boolean visible=getSensing().hasLineOfSight(prey);
        if(player&&--moveIn<=0){
            moveIn=PURSUIT_INTERVAL;
            pursuitStall=horizontal>4&&position().distanceToSqr(pursuitPosition)<.16?pursuitStall+PURSUIT_INTERVAL:0;
            pursuitPosition=position();
        }
        float aim=yawTo(prey);
        boolean ready=attackIn<=0&&onGround();
        boolean blocked=player&&(horizontal<=ENGAGE_RANGE*ENGAGE_RANGE&&!visible||pursuitStall>=24);
        // Do not require eye-level visibility to smash an obstruction in front.
        if(ready&&blocked&&TrollBlockSmash.hasObstacle(level,this,aim)){
            stop();look(aim);pursuitStall=0;beginAttack(BELLY);return;
        }
        if(ready&&player&&leapCooldown<=0&&needsLeap(offset.lengthSqr(),noPlayerHit)
                &&LoadedFaunaTerrain.loaded(level,new AABB(position(),position().add(offset.normalize().scale(Math.min(LEAP_RANGE,offset.length())))).inflate(2),2)){
            stop();look(aim);beginAttack(SLAM);return;
        }
        if(horizontal<=ENGAGE_RANGE*ENGAGE_RANGE&&Math.abs(offset.y)<=ATTACK_HEIGHT&&visible){
            look(Mth.approachDegrees(getYRot(),aim,35));
            if(ready&&Math.abs(Mth.wrapDegrees(aim-getYRot()))<=12){
                stop();pursuitStall=0;int roll=random.nextInt(100);
                int next=player?(roll<30?BELLY:leftPunch?LEFT:RIGHT)
                        :roll<22?SLAM:roll<46?BELLY:leftPunch?LEFT:RIGHT;
                beginAttack(next);return;
            }
            if(!player||horizontal<=2.6*2.6){stop();play(IDLE);return;}
        }
        if(player){
            if(moveIn==PURSUIT_INTERVAL){
                if(LoadedFaunaTerrain.loaded(level,new AABB(position(),prey.position()),3)){
                    int off=Mth.floor(getBbWidth()+1)/2;
                    Path route=getNavigation().createPath(prey.blockPosition().offset(-off,0,-off),1);
                    if(route!=null)getNavigation().moveTo(route,PLAYER_PURSUIT_SPEED);
                    else getNavigation().moveTo(prey,PLAYER_PURSUIT_SPEED);
                }else stop();
            }
        }else if(--moveIn<=0){
            moveIn=12;
            pursuitStall=position().distanceToSqr(pursuitPosition)<.36?pursuitStall+12:0;
            pursuitPosition=position();
            if(pursuitStall>=60){
                unreachablePrey=prey.getUUID();retryPreyIn=160;losePrey();
            }else if(LoadedFaunaTerrain.loaded(level,new AABB(position(),prey.position()),3))getNavigation().moveTo(prey,1);
            else stop();
        }
        play(getNavigation().isDone()?IDLE:WALK);
    }

    private void beginAttack(int attack) {
        if (attack == LEFT || attack == RIGHT) leftPunch = !leftPunch;
        attackYaw = getYRot();
        if(attack==SLAM){entityData.set(SLAM_LANDED,0L);entityData.set(POWER_SLAM,prey instanceof Player);leapTicks=0;leapOrigin=null;}
        play(attack);
        playSound(ModSounds.TROLL_WINDUP, .85f, attack == SLAM ? .85f : 1f + random.nextFloat() * .12f);
    }

    private void tickSlam(ServerLevel level,int elapsed){
        stop();
        if(slamLanded()>0){
            look(attackYaw);
            if(level.getGameTime()-slamLanded()>=LEAP_RECOVERY){
                attackIn=prey instanceof Player?6:20;play(IDLE);
            }
            return;
        }
        if(!leaping){
            if(elapsed<LEAP_WINDUP){
                if(prey!=null)attackYaw=Mth.approachDegrees(attackYaw,yawTo(prey),25);
                look(attackYaw);return;
            }
            if(returning||prey==null||!onGround()){play(IDLE);return;}
            Vec3 delta=prey.position().subtract(position());
            if(!powerSlam())delta=delta.scale(.4);
            Vec3 horizontal=new Vec3(delta.x,0,delta.z);
            double reach=powerSlam()?LEAP_RANGE:2.5;
            if(horizontal.length()>reach)horizontal=horizontal.normalize().scale(reach);
            Vec3 candidate=position().add(horizontal.x,powerSlam()?Math.clamp(delta.y,-8,LEAP_RISE):0,horizontal.z);
            if(homeDistance(candidate)>CHASE*CHASE||!LoadedFaunaTerrain.loaded(level,new AABB(position(),candidate).inflate(3),2)){
                play(IDLE);leapCooldown=20;return;
            }
            leapOrigin=position();leapGoal=candidate;leapTicks=0;
            leapPreviousGravity=isNoGravity();setNoGravity(true);leaping=true;
            setDeltaMovement(Vec3.ZERO);
        }
        look(attackYaw);
        int step=++leapTicks;
        int flight=leapFlightTicks();
        double t=Math.min(1,step/(double)flight);
        double horizontal=leapHorizontal(t);
        Vec3 path=leapGoal.subtract(leapOrigin);
        Vec3 desired=leapOrigin.add(path.x*horizontal,leapHeight(t,path.y)*(powerSlam()?1:.16),path.z*horizontal);
        Vec3 motion=desired.subtract(position());
        // Bounded physical movement, not teleportation. Obstructions remain
        // solid unless the griefing rules allow clearing the actual swept body.
        if(step>flight)motion=new Vec3(0,-Math.min(1.2,.2+(step-flight)*.12),0);
        if(motion.length()>1.8)motion=motion.normalize().scale(1.8);
        if(!LoadedFaunaTerrain.loaded(level,getBoundingBox().expandTowards(motion),2)){
            cancelLeap();play(IDLE);leapCooldown=20;return;
        }
        TrollBlockSmash.flight(level,this,motion);
        move(MoverType.SELF,motion);
        setDeltaMovement(Vec3.ZERO);
        fallDistance=0;
        if((step>2&&motion.y<=0&&onGround())||step>=flight+24){
            boolean landed=onGround();
            cancelLeap();entityData.set(SLAM_LANDED,level.getGameTime());
            leapCooldown=20;
            if(landed&&!struck){
                struck=true;
                TrollBlockSmash.landing(level,this);
                if(!returning)impact(level,SLAM);
            }
        }
    }

    private void cancelLeap(){
        if(leaping){setNoGravity(leapPreviousGravity);leaping=false;setDeltaMovement(Vec3.ZERO);fallDistance=0;}
        leapOrigin=null;leapGoal=null;
    }

    private void goHome(ServerLevel level) {
        if (bedDistance() <= 4 || (returnFailures >= 3 && homeDistance(position()) <= ROAM * ROAM)) {
            returning = false;
            actionIn = 75;
            stop();
            play(IDLE);
            return;
        }
        if (--moveIn <= 0 && getNavigation().isDone()) {
            moveIn = 25;
            if (!walkTo(level, bedPoint(), .92)) {
                ++returnFailures;
                if (entrance != null && position().distanceToSqr(Vec3.atBottomCenterOf(entrance)) > 4) {
                    walkTo(level, Vec3.atBottomCenterOf(entrance), .92);
                }
            }
        }
        play(getNavigation().isDone() ? IDLE : WALK);
    }

    private void idle(ServerLevel level) {
        if (--ambientIn <= 0) {
            ambientIn = 100 + random.nextInt(180);
            boolean chomp = random.nextBoolean();
            playSound(chomp ? ModSounds.TROLL_CHOMP : ModSounds.TROLL_SNORT,
                    chomp ? .72f : .92f, .85f + random.nextFloat() * .2f);
        }
        if (forageTree != null && tickForaging(level)) return;
        if (!sleepPending && forageIn == 0 && getNavigation().isDone() && territoryId != null
                && ModDimensions.isLands(level) && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && LoadedFaunaTerrain.search(level)) {
            if (findFoodTree(level)) {
                forageIn = 1_050 + random.nextInt(800);
                play(WALK);
                return;
            }
            forageIn = 300 + random.nextInt(300);
        }
        if (sleepPending) {
            if (bedDistance() <= 4) {
                sleepPending = false;
                stop();
                play(SIT);
                return;
            }
            if (--sleepTravel <= 0) {
                sleepPending = false;
                stop();
                actionIn = 100;
            } else if (--moveIn <= 0) {
                moveIn = 30;
                walkTo(level, bedPoint(), .78);
            }
        } else if (--actionIn <= 0 && getNavigation().isDone()) {
            actionIn = 120 + random.nextInt(240);
            int roll = random.nextInt(9);
            if (roll < 3) {
                if (bedDistance() <= 4) {
                    play(SIT);
                    return;
                }
                sleepPending = true;
                sleepTravel = 720;
                moveIn = 30;
                walkTo(level, bedPoint(), .78);
            } else if (roll < 5) {
                play(SCRATCH);
                return;
            } else if (LoadedFaunaTerrain.search(level)) {
                roam(level);
            }
        }
        play(getNavigation().isDone() ? IDLE : WALK);
    }

    private boolean findFoodTree(ServerLevel level) {
        BlockPos center = BlockPos.containing(home);
        for (int attempt = 0; attempt < 96; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, radius = 4 + random.nextDouble() * 20;
            BlockPos root = TrollTrees.rootAt(level, Mth.floor(home.x + Math.cos(angle) * radius),
                    Mth.floor(home.z + Math.sin(angle) * radius));
            if (root == null || Math.abs(root.getY() - getY()) > 8 || !TrollTrees.edible(level, center, root)) continue;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos approach = root.relative(direction, 3);
                if (homeDistance(Vec3.atBottomCenterOf(approach)) > ROAM * ROAM) continue;
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos feet = approach.above(dy);
                    if (TrollEncounters.clear(level, feet) && walkTo(level, Vec3.atBottomCenterOf(feet), .75)) {
                        forageTree = root;
                        forageTravel = 400;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean tickForaging(ServerLevel level) {
        if (--forageTravel <= 0 || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            forageTree = null;
            stop();
            return false;
        }
        if (onGround() && canReachTree(level, forageTree)) {
            stop();
            Vec3 tree = Vec3.atCenterOf(forageTree);
            float yaw = (float) Math.toDegrees(Math.atan2(getX() - tree.x, tree.z - getZ()));
            look(Mth.approachDegrees(getYRot(), yaw, 25));
            if (attackIn <= 0 && Math.abs(Mth.wrapDegrees(yaw - getYRot())) <= 12) {
                punchTree = forageTree;
                forageTree = null;
                beginAttack(leftPunch ? LEFT : RIGHT);
            } else {
                play(IDLE);
            }
            return true;
        }
        if (getNavigation().isDone()) {
            forageTree = null;
            return false;
        }
        play(WALK);
        return true;
    }

    private boolean canReachTree(ServerLevel level, BlockPos root) {
        Vec3 tree = Vec3.atCenterOf(root);
        if (position().subtract(tree).horizontalDistanceSqr() > ENGAGE_RANGE * ENGAGE_RANGE
                || Math.abs(getY() - root.getY()) > 2 || !LoadedFaunaTerrain.loaded(level, new AABB(position(), tree), 1)) {
            return false;
        }
        BlockHitResult hit = level.clip(new ClipContext(getEyePosition(), Vec3.atCenterOf(root.above(2)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        BlockPos hitPos = hit.getBlockPos();
        return hit.getType() == HitResult.Type.BLOCK && hitPos.getX() == root.getX()
                && hitPos.getZ() == root.getZ() && Math.abs(hitPos.getY() - root.getY() - 2) <= 1;
    }

    private void feed() {
        heal(3f);
        ambientIn = Math.max(ambientIn, 75);
        playSound(ModSounds.TROLL_CHOMP, .82f, .85f + random.nextFloat() * .14f);
    }

    private void roam(ServerLevel level) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, radius = 6 + random.nextDouble() * (ROAM - 7);
            BlockPos site = TrollEncounters.site(level, Mth.floor(home.x + Math.cos(angle) * radius),
                    Mth.floor(home.z + Math.sin(angle) * radius));
            if (site == null || homeDistance(Vec3.atBottomCenterOf(site)) > ROAM * ROAM || Math.abs(site.getY() - home.y) > 8
                    || entrance != null && site.getY() >= home.y + 5 && site.closerThan(entrance, TrollCave.RADIUS)
                    || Sanctuary.near(level, site.getX(), site.getZ(), 2)
                    || BattlefieldFaunaBoundary.inside(level, site.getX(), site.getZ())) continue;
            if (walkTo(level, Vec3.atBottomCenterOf(site), .65)) return;
        }
    }

    private void impact(ServerLevel level, int attack) {
        boolean area = attack == SLAM;
        double range = area ? 5 : attack == BELLY ? 4.5 : ENGAGE_RANGE;
        Vec3 forward = Vec3.directionFromRotation(0, attackYaw);
        if (area) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + .15, getZ(), 35, 2, .12, 2, .04);
            playSound(ModSounds.TROLL_SLAM, 1f, .85f + random.nextFloat() * .15f);
        }
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) + (area ? 3 : attack == BELLY ? 0 : -1);
        var source = damageSources().mobAttack(this);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(range, ATTACK_HEIGHT, range), target -> eligible(level, target))) {
            Vec3 delta = victim.position().subtract(position());
            if (!withinStrike(delta.x, delta.y, delta.z, forward.x, forward.z, range, ATTACK_HEIGHT, area)
                    || !hasLineOfSight(victim)) continue;
            float before = victim.getHealth() + victim.getAbsorptionAmount();
            if (victim.hurt(source, Math.max(0, damage)) && victim.getHealth() + victim.getAbsorptionAmount() < before) {
                victim.knockback(attack == BELLY ? 1.1 : area ? .85 : .4, -delta.x, -delta.z);
                if(victim==prey&&victim instanceof Player)noPlayerHit=0;
                if (victim instanceof Player && laughIn <= 0) {
                    laughIn = 160;
                    playSound(ModSounds.TROLL_LAUGH, .8f, .9f + random.nextFloat() * .15f);
                } else if (!(victim instanceof Player) && victim.isDeadOrDying()) {
                    feed();
                }
            }
        }
    }
}
