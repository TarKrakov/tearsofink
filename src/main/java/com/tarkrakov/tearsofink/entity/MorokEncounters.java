package com.tarkrakov.tearsofink.entity;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.*;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.*;
import static com.tarkrakov.tearsofink.entity.MorokTuning.*;

/** Real loaded ruin starts, battlefield groups and rare forest singles. */
public final class MorokEncounters {
    private MorokEncounters(){}
    public static void init(){ServerTickEvents.END_WORLD_TICK.register(MorokEncounters::tick);}
    private static void tick(ServerLevel l){
        if(!ModDimensions.isLands(l)||l.getDifficulty()==Difficulty.PEACEFUL||!l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        for(ServerPlayer p:l.players()){
            if(!p.isAlive()||p.isSpectator()||p.isCreative()||Math.floorMod(l.getGameTime()+p.getUUID().hashCode(),INTERVAL)!=0||!LoadedFaunaTerrain.search(l))continue;
            var layout=InkLandsLayout.of(l);if(layout!=null&&!Ruins.nearStart(layout,p.getX(),p.getZ()))attempt(l,layout,p);
        }
    }
    private static long mixSite(long n){
        n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;
        n=(n^(n>>>27))*0x94d049bb133111ebL;
        return n^(n>>>31);
    }
    private static int count(ServerLevel l,Vec3 p,double r){return l.getEntitiesOfClass(MorokEntity.class,new AABB(p,p).inflate(r,24,r),MorokEntity::isAlive).size();}
    private static void attempt(ServerLevel l,InkLandsLayout layout,ServerPlayer p){
        boolean field=BattlefieldFaunaBoundary.inside(l,p.getX(),p.getZ());int existing=count(l,p.position(),LOCAL_RADIUS);
        if(field){
            int left=Math.min(6,FIELD_CAP-existing);
            for(int i=0;i<36&&left>0;i++){double a=l.random.nextDouble()*Math.PI*2,r=22+l.random.nextDouble()*32;int x=(int)Math.floor(p.getX()+Math.cos(a)*r),z=(int)Math.floor(p.getZ()+Math.sin(a)*r);
                if(BattlefieldFaunaBoundary.inside(l,x,z)&&spawn(l,layout,x,z,FIELD_CAP,true,null)){
                    left--;
                    int cluster=Math.min(left,2+l.random.nextInt(2));
                    for(int j=0;j<20&&cluster>0;j++){
                        double angle=l.random.nextDouble()*Math.PI*2,dist=4+l.random.nextDouble()*5;
                        int nx=x+(int)Math.round(Math.cos(angle)*dist),nz=z+(int)Math.round(Math.sin(angle)*dist);
                        if(BattlefieldFaunaBoundary.inside(l,nx,nz)&&spawn(l,layout,nx,nz,FIELD_CAP,true,null)){left--;cluster--;}
                    }
                }}
            return;
        }
        if(existing>=RUIN_CAP)return;
        var ruin=l.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("ruin_scattered"));
        if(ruin!=null){
            int cx=p.getBlockX()>>4,cz=p.getBlockZ()>>4;
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
                var chunk=l.getChunkSource().getChunkNow(cx+dx,cz+dz);if(chunk==null)continue;
                var start=chunk.getStartForStructure(ruin);if(start==null||!start.isValid())continue;
                BoundingBox box=start.getBoundingBox();int x0=(box.minX()+box.maxX())/2,z0=(box.minZ()+box.maxZ())/2;
                double d=Math.hypot(x0-p.getX(),z0-p.getZ());
                if(d<22||d>50||count(l,new Vec3(x0,p.getY(),z0),18)>0)continue;
                if((mixSite(layout.seed() ^ BlockPos.asLong(x0,0,z0)) & 1L) != 0L)continue;
                int left=Math.min(1+l.random.nextInt(2),RUIN_CAP-existing);
                for(int i=0;i<12&&left>0;i++){int x=x0+l.random.nextInt(9)-4,z=z0+l.random.nextInt(9)-4;if(spawn(l,layout,x,z,RUIN_CAP,false,box))left--;}
                return;
            }
        }
        if(existing>0||l.random.nextInt(4)!=0)return;
        for(int i=0;i<18;i++){double a=l.random.nextDouble()*Math.PI*2,r=28+l.random.nextDouble()*20;int x=(int)Math.floor(p.getX()+Math.cos(a)*r),z=(int)Math.floor(p.getZ()+Math.sin(a)*r);
            if(!BattlefieldFaunaBoundary.inside(l,x,z)&&!layout.nearRoad(x,z,5)&&spawn(l,layout,x,z,1,false,null))return;}
    }
    private static boolean spawn(ServerLevel l,InkLandsLayout layout,int x,int z,int cap,boolean field,BoundingBox ruin){
        if(layout.depthInside(x,z)<16||Ruins.nearStart(layout,x,z)||Ruins.nearTemple(layout,x,z)||Sanctuary.near(layout,x,z,8))return false;
        var chunk=l.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)return false;
        int surface=LoadedFaunaTerrain.surface(chunk,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        // Above the local surface, including ruins; never fall back to ground-level spawns.
        int first=FLIGHT_MIN_HEIGHT+l.random.nextInt(FLIGHT_MAX_HEIGHT-FLIGHT_MIN_HEIGHT+1);
        for(int i=0;i<=FLIGHT_MAX_HEIGHT-FLIGHT_MIN_HEIGHT;i++){
            int y=surface+FLIGHT_MIN_HEIGHT+(first-FLIGHT_MIN_HEIGHT+i)%(FLIGHT_MAX_HEIGHT-FLIGHT_MIN_HEIGHT+1);
            BlockPos pos=new BlockPos(x,y,z);Vec3 at=new Vec3(x+.5,y+.15,z+.5);AABB body=new AABB(at.x-.35,at.y-.15,at.z-.35,at.x+.35,at.y+.85,at.z+.35);
            if(!LoadedFaunaTerrain.free(l,null,body)||!l.noCollision(body)||!l.getFluidState(pos).isEmpty()||!l.getFluidState(pos.above()).isEmpty())continue;
            if(!(l.getBiome(pos).is(ModBiomes.PALE_FOREST)||l.getBiome(pos).is(ModBiomes.INKWOOD)))continue;
            if(!field&&BattlefieldFaunaBoundary.inside(l,x,z))continue;
            if(count(l,at,LOCAL_RADIUS)>=cap||count(l,at,3)>0)return false;
            boolean close=false;for(ServerPlayer other:l.players()){
                if(other.isSpectator()||!other.isAlive())continue;
                double d=other.position().subtract(at).horizontalDistanceSqr();
                if(d<400||(d<LOCAL_RADIUS*LOCAL_RADIUS&&count(l,other.position(),LOCAL_RADIUS)>=cap)){close=true;break;}}
            if(close)return false;
            MorokEntity mob=ModMobs.MOROK.create(l);if(mob==null)return false;
            mob.moveTo(at.x,at.y,at.z,l.random.nextFloat()*360,0);mob.encounter(at);return l.addFreshEntity(mob);
        }
        return false;
    }
}
