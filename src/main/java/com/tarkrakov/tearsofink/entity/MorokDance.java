package com.tarkrakov.tearsofink.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Ephemeral server-side group: no new entities, tickets, global map, or saved targets. */
final class MorokDance {
    private static final int RECRUIT_RADIUS=18, GATHER_LIMIT=160;
    private static final double GATHER_SPEED=.16, ORBIT_SPEED=.14;
    private final Vec3 center;
    private final List<UUID> members;
    private final double angle;
    private final int direction;
    private final long gatherDeadline;
    private final int duration;
    private long start=-1, checked=Long.MIN_VALUE;
    private boolean ended;

    private MorokDance(Vec3 center,List<MorokEntity> group,double angle,int direction,long now,int duration){
        this.center=center;this.members=group.stream().map(MorokEntity::getUUID).toList();
        this.angle=angle;this.direction=direction;this.gatherDeadline=now+GATHER_LIMIT;this.duration=duration;
    }
    static void tryStart(ServerLevel level,MorokEntity initiator){
        if(!initiator.canJoinDance()||!LoadedFaunaTerrain.search(level))return;
        List<MorokEntity> nearby=new ArrayList<>(level.getEntitiesOfClass(MorokEntity.class,initiator.getBoundingBox().inflate(RECRUIT_RADIUS,5,RECRUIT_RADIUS),
                m->m!=initiator&&m.canJoinDance()&&m.distanceToSqr(initiator)<=RECRUIT_RADIUS*RECRUIT_RADIUS));
        if(nearby.size()<2)return;
        nearby.sort(Comparator.comparingDouble(m->m.distanceToSqr(initiator)));
        List<MorokEntity> group=new ArrayList<>();group.add(initiator);
        int size=nearby.size()>=3&&level.random.nextBoolean()?4:3;
        group.addAll(nearby.subList(0,size-1));
        Vec3 mean=Vec3.ZERO;
        for(MorokEntity m:group)mean=mean.add(m.position());
        mean=mean.scale(1.0/size);
        Vec3 center=null;
        for(int i=0;i<12;i++){
            Vec3 c=mean.add(i==0?0:(level.random.nextDouble()-.5)*6,
                    i==0?0:(level.random.nextDouble()-.25)*3,
                    i==0?0:(level.random.nextDouble()-.5)*6);
            boolean tooFar=false;
            for(MorokEntity m:group)if(m.position().distanceToSqr(c)>17*17){tooFar=true;break;}
            if(tooFar||Sanctuary.near(level,c.x,c.z,6)||!clearRing(level,c))continue;
            var active=level.getEntitiesOfClass(MorokEntity.class,new AABB(c,c).inflate(12),m->m.dance!=null&&!m.dance.ended);
            if(active.stream().anyMatch(m->m.dance.center.distanceToSqr(c)<8*8))continue;
            center=c;break;
        }
        if(center==null)return;
        final Vec3 anchor=center;
        group.sort(Comparator.comparingDouble(m->Math.atan2(m.getZ()-anchor.z,m.getX()-anchor.x)));
        double angle=Math.atan2(group.get(0).getZ()-center.z,group.get(0).getX()-center.x);
        MorokDance dance=new MorokDance(center,group,angle,level.random.nextBoolean()?1:-1,level.getGameTime(),400+level.random.nextInt(401));
        for(MorokEntity m:group)m.joinDance(dance);
    }
    private static boolean clearRing(ServerLevel level,Vec3 c){
        if(!LoadedFaunaTerrain.loaded(level,new AABB(c,c).inflate(4,2,4),2))return false;
        for(int i=0;i<32;i++){
            double a=i*Math.PI/16;
            Vec3 p=c.add(Math.cos(a)*MorokDanceOrbit.RADIUS,0,Math.sin(a)*MorokDanceOrbit.RADIUS);
            AABB box=new AABB(p.x-.47,p.y-.35,p.z-.47,p.x+.47,p.y+1,p.z+.47);
            if(!LoadedFaunaTerrain.free(level,null,box))return false;
            for(BlockPos at:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
                if(!level.getFluidState(at).isEmpty())return false;
        }
        return true;
    }
    private Vec3 point(int slot,long elapsed){
        var offset=MorokDanceOrbit.at(slot,members.size(),angle,direction,elapsed);
        return center.add(offset.x(),offset.y(),offset.z());
    }
    void stop(){ended=true;}
    private boolean validate(ServerLevel level,long now){
        if(ended)return false;
        if(checked==now)return true;
        checked=now;
        if((start>=0&&now>=start+duration)||(start<0&&now>=gatherDeadline)){stop();return false;}
        boolean ready=true;
        for(int i=0;i<members.size();i++){
            if(!(level.getEntity(members.get(i)) instanceof MorokEntity m)||!m.isAlive()||m.isNoAi()
                    ||m.dance!=this||m.phase()!=MorokEntity.IDLE||!LoadedFaunaTerrain.canTick(level,m)){
                stop();return false;
            }
            double d=m.position().distanceToSqr(point(i,start<0?0:now-start));
            if(start>=0&&d>4*4){stop();return false;}
            if(d>.65*.65)ready=false;
        }
        if(start<0&&ready)start=now;
        return true;
    }
    boolean move(ServerLevel level,MorokEntity mob){
        long now=level.getGameTime();
        if(!validate(level,now))return false;
        int slot=members.indexOf(mob.getUUID());
        if(slot<0)return false;
        Vec3 target=point(slot,start<0?0:now-start+4);
        if(!mob.steerDance(level,target,start<0?GATHER_SPEED:ORBIT_SPEED)){
            stop();return false;
        }
        return true;
    }
}
