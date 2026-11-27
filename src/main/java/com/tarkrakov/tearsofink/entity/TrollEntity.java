package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.sound.ModSounds;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class TrollEntity extends PathfinderMob implements Enemy {
    public static final float SCALE=.75f, WIDTH=2.2f, HEIGHT=3.6f;
    public static final int IDLE=0,WALK=1,RIGHT=2,LEFT=3,BELLY=4,SLAM=5,SCRATCH=6,SIT=7,SLEEP=8,STAND=9,DEATH=10;
    public static final int[] DURATION={60,80,40,40,40,58,60,15,240,15,41};
    private static final EntityDataAccessor<Long> STATE=SynchedEntityData.defineId(TrollEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> PREVIOUS=SynchedEntityData.defineId(TrollEntity.class,EntityDataSerializers.LONG);
    private Vec3 home;
    private LivingEntity prey;
    private int scanIn,moveIn,actionIn=100,ambientIn=80,laughIn,attackIn,sleepFor;
    private boolean struck,alternate;
    public TrollEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder createAttributes(){
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH,64).add(Attributes.MOVEMENT_SPEED,.19)
            .add(Attributes.ATTACK_DAMAGE,10).add(Attributes.FOLLOW_RANGE,20).add(Attributes.KNOCKBACK_RESISTANCE,.85);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(STATE,0L);b.define(PREVIOUS,0L);}
    public int clip(){return (int)(entityData.get(STATE)&15);}
    public long clipStart(){return entityData.get(STATE)>>>4;}
    public int previousClip(){return (int)(entityData.get(PREVIOUS)&15);}
    public int previousTicks(){return (int)(entityData.get(PREVIOUS)>>>4);}
    private int elapsed(){return (int)Math.min(100000,Math.max(0,level().getGameTime()-clipStart()));}
    private void play(int next){
        if(clip()==next&&entityData.get(STATE)!=0)return;
        entityData.set(PREVIOUS,((long)elapsed()<<4)|clip());
        entityData.set(STATE,(level().getGameTime()<<4)|next);struck=false;
    }
    void setHome(Vec3 p){home=p;setPersistenceRequired();}
    @Override public SoundSource getSoundSource(){return SoundSource.HOSTILE;}
    @Override public void addAdditionalSaveData(CompoundTag t){
        super.addAdditionalSaveData(t);if(home!=null){t.putDouble("TrollHomeX",home.x);t.putDouble("TrollHomeY",home.y);t.putDouble("TrollHomeZ",home.z);}
        t.putInt("TrollLaughIn",laughIn);
    }
    @Override public void readAdditionalSaveData(CompoundTag t){
        super.readAdditionalSaveData(t);if(t.contains("TrollHomeX"))home=new Vec3(t.getDouble("TrollHomeX"),t.getDouble("TrollHomeY"),t.getDouble("TrollHomeZ"));
        laughIn=Math.max(0,t.getInt("TrollLaughIn"));play(IDLE);
    }
    private boolean eligible(LivingEntity e){
        return e!=null&&e!=this&&!(e instanceof TrollEntity)&&e.isAlive()&&!e.isSpectator()
            &&(!(e instanceof Player p)||(!p.isCreative()&&(!ModDimensions.isLands(level())||!Sanctuary.contains(p))))
            &&(home==null||home.distanceToSqr(e.position())<=32*32)
            &&(!ModDimensions.isLands(level())||!Sanctuary.near((ServerLevel)level(),e.getX(),e.getZ(),0));
    }
    @Override public boolean hurt(DamageSource source,float amount){
        boolean ok=super.hurt(source,amount);
        if(ok&&!level().isClientSide&&isAlive()&&source.getEntity() instanceof LivingEntity e&&eligible(e))prey=e;
        return ok;
    }
    @Override public void die(DamageSource source){if(!level().isClientSide){getNavigation().stop();play(DEATH);}super.die(source);}
    @Override protected void tickDeath(){++deathTime;if(deathTime>=DURATION[DEATH]+1&&!level().isClientSide){level().broadcastEntityEvent(this,(byte)60);remove(RemovalReason.KILLED);}}
    private void face(LivingEntity e){
        Vec3 d=e.position().subtract(position());float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));
        setYRot(Mth.approachDegrees(getYRot(),yaw,25));yBodyRot=getYRot();setYHeadRot(getYRot());
    }
    private void stop(){getNavigation().stop();Vec3 v=getDeltaMovement();setDeltaMovement(0,v.y,0);}
    @Override public void tick(){
        if(level() instanceof ServerLevel l&&!LoadedFaunaTerrain.canTick(l,this)){setDeltaMovement(Vec3.ZERO);return;}
        super.tick();if(level().isClientSide||!isAlive()||isNoAi())return;
        ServerLevel l=(ServerLevel)level();if(l.getDifficulty()==Difficulty.PEACEFUL){discard();return;}
        if(home==null)home=position();
        if(entityData.get(STATE)==0)play(IDLE);
        if(laughIn>0)--laughIn;if(attackIn>0)--attackIn;
        if(!eligible(prey)||distanceToSqr(prey)>28*28)prey=null;
        if(--scanIn<=0){
            scanIn=10;if(prey==null){double best=16*16;for(LivingEntity e:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(16,5,16),this::eligible)){
                double d=distanceToSqr(e);if(d<best&&getSensing().hasLineOfSight(e)){prey=e;best=d;}
            }}
        }
        int c=clip(),t=elapsed();
        if(c>=RIGHT&&c<=SLAM){
            stop();int hit=c==SLAM?28:c==BELLY?23:22;
            if(!struck&&t>=hit){struck=true;impact(l,c);}
            if(t>=DURATION[c]){attackIn=18+random.nextInt(14);play(IDLE);}
            return;
        }
        if(c==SIT){stop();if(t>=DURATION[SIT]){if(prey!=null)play(STAND);else{sleepFor=240+random.nextInt(361);play(SLEEP);}}return;}
        if(c==SLEEP){stop();if(prey!=null||--sleepFor<=0){play(STAND);return;}if(t%100==0)playSound(ModSounds.TROLL_SNORE,.65f,.85f+random.nextFloat()*.12f);return;}
        if(c==STAND){stop();if(t>=DURATION[STAND]){actionIn=160;play(IDLE);}return;}
        if(c==SCRATCH){stop();if(prey!=null||t>=DURATION[SCRATCH]){actionIn=140;play(IDLE);}return;}
        if(prey!=null&&home.distanceToSqr(position())<=36*36){
            double dist=Math.sqrt(distanceToSqr(prey));
            if(dist<=4.1&&getSensing().hasLineOfSight(prey)){
                stop();face(prey);
                if(attackIn<=0){
                    int roll=random.nextInt(100);int next=roll<22?SLAM:roll<46?BELLY:alternate?LEFT:RIGHT;
                    alternate=!alternate;play(next);
                }else play(IDLE);
            }else{if(--moveIn<=0){moveIn=12;getNavigation().moveTo(prey,1.0);}play(getNavigation().isDone()?IDLE:WALK);}
            return;
        }
        prey=null;
        if(--ambientIn<=0){ambientIn=100+random.nextInt(180);playSound(random.nextBoolean()?ModSounds.TROLL_CHOMP:ModSounds.TROLL_SNORT,.55f,.85f+random.nextFloat()*.2f);}
        if(--actionIn<=0&&getNavigation().isDone()){
            actionIn=120+random.nextInt(240);
            if(home.distanceToSqr(position())<16&&random.nextInt(3)==0){play(SIT);return;}
            if(random.nextInt(3)==0){play(SCRATCH);return;}
            double x=home.x+(random.nextDouble()-.5)*18,z=home.z+(random.nextDouble()-.5)*18;
            if(LoadedFaunaTerrain.loaded(l,new AABB(position(),new Vec3(x,home.y,z)),3))getNavigation().moveTo(x,home.y,z,.65);
        }
        if(home.distanceToSqr(position())>18*18&&--moveIn<=0){moveIn=30;getNavigation().moveTo(home.x,home.y,home.z,.8);}
        play(getNavigation().isDone()?IDLE:WALK);
    }
    private void impact(ServerLevel l,int c){
        boolean area=c==SLAM;double range=area?5.0:c==BELLY?4.5:4.1;
        Vec3 forward=Vec3.directionFromRotation(0,getYRot());
        if(area){l.sendParticles(ParticleTypes.POOF,getX(),getY()+.15,getZ(),35,2,.12,2,.04);playSound(ModSounds.TROLL_SLAM,1f,.85f);}
        for(LivingEntity e:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(range,2,range),this::eligible)){
            Vec3 delta=e.position().subtract(position());double d=delta.horizontalDistance();
            if(d>range||Math.abs(delta.y)>2.7||!getSensing().hasLineOfSight(e))continue;
            if(!area&&(d>.01&&forward.dot(new Vec3(delta.x,0,delta.z).normalize())<.55))continue;
            if(e.hurt(damageSources().mobAttack(this),c==SLAM?13:c==BELLY?10:9)){
                e.knockback(c==BELLY?1.1:c==SLAM?.85:.4,getX()-e.getX(),getZ()-e.getZ());
                if(e instanceof Player&&laughIn<=0){laughIn=160;playSound(ModSounds.TROLL_LAUGH,.8f,.9f+random.nextFloat()*.15f);}
            }
        }
    }
}
