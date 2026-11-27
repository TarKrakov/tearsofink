package com.tarkrakov.tearsofink.entity;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.sound.ModSounds;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import static com.tarkrakov.tearsofink.entity.MorokTuning.*;

/** Server-owned wandering, pursuit and interruptible, irreversible fuse. */
public final class MorokEntity extends PathfinderMob implements Enemy {
    public static final int IDLE=0,CHARGE=1,DETONATE=2;
    private static final EntityDataAccessor<Long> PHASE=SynchedEntityData.defineId(MorokEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> LEFT=SynchedEntityData.defineId(MorokEntity.class,EntityDataSerializers.INT);
    private Vec3 home, destination;
    private ServerPlayer prey;
    private int wanderIn,whisperIn=40,lostSight,scanIn;
    private boolean encounter,exploded,deathFx;
    MorokDance dance;
    private int danceIn=80;
    public MorokEntity(EntityType<? extends PathfinderMob> type,Level level) { super(type,level); setNoGravity(true); }
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH,HEALTH).add(Attributes.MOVEMENT_SPEED,.22)
                .add(Attributes.FLYING_SPEED,CHARGE_SPEED).add(Attributes.FOLLOW_RANGE,DETECT);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b); b.define(PHASE,0L); b.define(LEFT,0);
    }
    public int phase(){return (int)(entityData.get(PHASE)&3L);}
    public long phaseStart(){return entityData.get(PHASE)>>>2;}
    public int fuseLeft(){return entityData.get(LEFT);}
    private void phase(int next){
        if(phase()!=next||entityData.get(PHASE)==0L) entityData.set(PHASE,(level().getGameTime()<<2)|next);
    }
    boolean canJoinDance(){return isAlive()&&!isNoAi()&&phase()==IDLE&&prey==null&&dance==null&&danceIn<=0;}
    void joinDance(MorokDance group){dance=group;destination=null;}
    private void leaveDance(){if(dance!=null){dance.stop();dance=null;danceIn=300+random.nextInt(301);destination=null;}}
    boolean steerDance(ServerLevel level,Vec3 at,double speed){return steer(level,at,speed);}
    void encounter(Vec3 at){home=at;encounter=true;}
    @Override public net.minecraft.sounds.SoundSource getSoundSource(){return net.minecraft.sounds.SoundSource.HOSTILE;}
    @Override public boolean removeWhenFarAway(double d){return encounter&&phase()!=DETONATE;}
    @Override public boolean causeFallDamage(float d,float m,DamageSource src){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putBoolean("MorokEncounter",encounter);tag.putInt("MorokFuse",fuseLeft());
        if(home!=null){tag.putDouble("MorokHomeX",home.x);tag.putDouble("MorokHomeY",home.y);tag.putDouble("MorokHomeZ",home.z);}
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);encounter=tag.getBoolean("MorokEncounter");
        if(tag.contains("MorokHomeX"))home=new Vec3(tag.getDouble("MorokHomeX"),tag.getDouble("MorokHomeY"),tag.getDouble("MorokHomeZ"));
        int fuse=Mth.clamp(tag.getInt("MorokFuse"),0,FUSE);entityData.set(LEFT,fuse);phase(fuse>0?DETONATE:IDLE);setNoGravity(true);
    }
    @Override public void tick(){
        if(level() instanceof ServerLevel s&&!LoadedFaunaTerrain.canTick(s,this)){setDeltaMovement(Vec3.ZERO);return;}
        super.tick();
        if(!isAlive())return;
        if(level().isClientSide){visual();return;}
        if(isNoAi()){leaveDance();setDeltaMovement(Vec3.ZERO);return;}
        ServerLevel s=(ServerLevel)level();
        if(s.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){discard();return;}
        setNoGravity(true);if(home==null)home=position();
        if(phase()==DETONATE){
            setDeltaMovement(getDeltaMovement().scale(.55));
            int left=fuseLeft()-1;entityData.set(LEFT,Math.max(0,left));if(left<=0)detonate(s);return;
        }
        if(prey!=null&&(!eligible(prey)||distanceToSqr(prey)>FORGET*FORGET||home.distanceToSqr(prey.position())>48*48||(!visible(s,prey)&&++lostSight>70))){
            prey=null;lostSight=0;destination=null;phase(IDLE);
        }
        if(prey==null&&--scanIn<=0){
            scanIn=5;double best=DETECT*DETECT;
            for(ServerPlayer p:s.players()){double d=distanceToSqr(p);if(d<=best&&eligible(p)&&visible(s,p)){best=d;prey=p;}}
            if(prey!=null){leaveDance();phase(CHARGE);lostSight=0;playSound(ModSounds.MOROK_CRY,.8f,.9f+random.nextFloat()*.18f);}
        }
        if(prey!=null){
            Vec3 goal=prey.position().add(0,Math.min(1.2,prey.getBbHeight()*.7),0);
            if(visible(s,prey)){
                lostSight=0;
                if(center().distanceToSqr(goal)<=TRIGGER*TRIGGER){
                    setDeltaMovement(Vec3.ZERO);phase(DETONATE);entityData.set(LEFT,FUSE);playSound(ModSounds.MOROK_FUSE,.7f,1f);return;
                }
            }
            steer(s,goal.subtract(0,.32,0),CHARGE_SPEED);return;
        }
        phase(IDLE);
        if(--whisperIn<=0){whisperIn=95+random.nextInt(145);playSound(ModSounds.MOROK_WHISPER,.32f,.88f+random.nextFloat()*.24f);}
        if(dance!=null){
            if(dance.move(s,this))return;
            leaveDance();
        }
        if(danceIn>0)--danceIn;
        if(danceIn<=0&&Math.floorMod(s.getGameTime()+getUUID().hashCode(),100)==0&&random.nextInt(3)==0){
            MorokDance.tryStart(s,this);
            if(dance!=null&&dance.move(s,this))return;
        }
        if(destination==null||--wanderIn<=0||position().distanceToSqr(destination)<.12){
            wanderIn=50+random.nextInt(80);destination=null;
            for(int i=0;i<8;i++){Vec3 p=home.add((random.nextDouble()-.5)*12,(random.nextDouble()-.5)*2,(random.nextDouble()-.5)*12);if(free(s,p)){destination=p;break;}}
        }
        if(destination==null)setDeltaMovement(Vec3.ZERO);else if(!steer(s,destination,IDLE_SPEED))destination=null;
    }
    private Vec3 center(){return position().add(0,.32,0);}
    private boolean eligible(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&p.level()==level()&&(!ModDimensions.isLands(level())||!Sanctuary.contains(p));}
    private boolean visible(ServerLevel l,ServerPlayer p){
        Vec3 a=center(),b=p.position().add(0,p.getBbHeight()*.65,0);
        return LoadedFaunaTerrain.loaded(l,new AABB(a,b),1)&&l.clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;
    }
    private boolean free(ServerLevel l,Vec3 at){
        if(ModDimensions.isLands(l)&&Sanctuary.near(l,at.x,at.z,2))return false;
        return LoadedFaunaTerrain.free(l,this,getBoundingBox().move(at.subtract(position())))&&l.getFluidState(BlockPos.containing(at)).isEmpty();
    }
    private boolean steer(ServerLevel l,Vec3 goal,double speed){
        Vec3 diff=goal.subtract(position());if(diff.lengthSqr()<.0025){setDeltaMovement(Vec3.ZERO);return true;}
        Vec3 desired=diff.scale(Math.min(speed,diff.length())/diff.length()),next=null;
        for(Vec3 v:new Vec3[]{getDeltaMovement().lerp(desired,.45),desired,desired.add(0,speed,0),desired.yRot(.75f),desired.yRot(-.75f),new Vec3(0,speed,0),desired.yRot(1.55f),desired.yRot(-1.55f),new Vec3(0,-speed,0)}){
            if(v.lengthSqr()<.00001)continue;if(v.length()>speed)v=v.normalize().scale(speed);
            if(free(l,position().add(v))){next=v;break;}
        }
        if(next==null){setDeltaMovement(Vec3.ZERO);return false;}
        setDeltaMovement(next);
        if(next.horizontalDistanceSqr()>.0001){float yaw=(float)Math.toDegrees(Math.atan2(-next.x,next.z));setYRot(Mth.approachDegrees(getYRot(),yaw,16));yBodyRot=getYRot();yHeadRot=getYRot();}
        return true;
    }
    private void detonate(ServerLevel l){
        if(!isAlive()||exploded)return;exploded=true;Vec3 c=center();
        // No world explosion: damage only, no blocks, fire, drops or faction exclusions.
        DamageSource source=damageSources().explosion(this,this);
        for(LivingEntity e:l.getEntitiesOfClass(LivingEntity.class,new AABB(c,c).inflate(BLAST),e->e!=this&&e.isAlive())){
            AABB b=e.getBoundingBox();
            double dx=c.x-Mth.clamp(c.x,b.minX,b.maxX),dy=c.y-Mth.clamp(c.y,b.minY,b.maxY),dz=c.z-Mth.clamp(c.z,b.minZ,b.maxZ);
            if(dx*dx+dy*dy+dz*dz<=BLAST*BLAST)e.hurt(source,DAMAGE);
        }
        l.sendParticles(ParticleTypes.FLASH,c.x,c.y,c.z,1,0,0,0,0);
        l.sendParticles(ModParticles.MOROK_FLAME,c.x,c.y,c.z,38,.25,.25,.25,.17);
        l.sendParticles(ModParticles.MOROK_SHARD,c.x,c.y,c.z,58,.25,.25,.25,.25);
        playSound(ModSounds.MOROK_BURST,1f,1f);discard();
    }
    @Override public void die(DamageSource source){
        leaveDance();
        if(level() instanceof ServerLevel s&&!exploded&&!deathFx){deathFx=true;Vec3 c=center();s.sendParticles(ModParticles.MOROK_SHARD,c.x,c.y,c.z,20,.22,.22,.22,.095);}
        super.die(source);
    }
    private void visual(){
        Vec3 c=center();
        for(int i=0;i<(phase()==DETONATE?5:3);i++){
            double x=(random.nextDouble()-.5)*.65,y=(random.nextDouble()-.5)*.65,z=(random.nextDouble()-.5)*.65;
            level().addParticle(ModParticles.MOROK_FLAME,c.x+x,c.y+y,c.z+z,x*.055,.035+Math.abs(y)*.04,z*.055);
        }
        if(phase()!=IDLE||tickCount%2==0){Vec3 v=getDeltaMovement().scale(-.16);
            level().addParticle(ModParticles.MOROK_SHARD,c.x+(random.nextDouble()-.5)*.5,c.y+(random.nextDouble()-.5)*.5,c.z+(random.nextDouble()-.5)*.5,v.x+(random.nextDouble()-.5)*.06,v.y+.018,v.z+(random.nextDouble()-.5)*.06);}
    }
}
