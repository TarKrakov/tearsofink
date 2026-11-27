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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import static com.tarkrakov.tearsofink.entity.MorokTuning.*;

/** Real loaded ruin starts, battlefield groups and rare forest singles. */
public final class MorokEncounters {
    private MorokEncounters(){}
    private static final Map<ServerLevel,Map<Long,Long>> RUIN_REST=new WeakHashMap<>();
    private record RuinSite(int x,int z,long key,double distance,BoundingBox box){}
    public static void init(){ServerTickEvents.END_WORLD_TICK.register(MorokEncounters::tick);}
    private static void tick(ServerLevel l){
        if(!ModDimensions.isLands(l)||l.getDifficulty()==Difficulty.PEACEFUL||!l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        long now=l.getGameTime();
        Map<Long,Long> rest=RUIN_REST.computeIfAbsent(l,k->new HashMap<>());
        if(now%600==0)rest.values().removeIf(until->until<=now);
        for(ServerPlayer p:l.players()){
            if(!p.isAlive()||p.isSpectator()||Math.floorMod(l.getGameTime()+p.getUUID().hashCode(),INTERVAL)!=0||!LoadedFaunaTerrain.search(l))continue;
            var layout=InkLandsLayout.of(l);if(layout!=null)attempt(l,layout,p,rest);
        }
    }
    private static long mixSite(long n){
        n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;
        n=(n^(n>>>27))*0x94d049bb133111ebL;
        return n^(n>>>31);
    }
    private static int count(ServerLevel l,Vec3 p,double r){return l.getEntitiesOfClass(MorokEntity.class,new AABB(p,p).inflate(r,64,r),m->m.isAlive()&&m.position().subtract(p).horizontalDistanceSqr()<=r*r).size();}
    private static void attempt(ServerLevel l,InkLandsLayout layout,ServerPlayer p,Map<Long,Long> rest){
        boolean field=BattlefieldFaunaBoundary.inside(l,p.getX(),p.getZ());int existing=count(l,p.position(),LOCAL_RADIUS);
        if(field){
            int left=Math.min(6,FIELD_CAP-existing);
            for(int i=0;i<36&&left>0;i++){double a=l.random.nextDouble()*Math.PI*2,r=18+l.random.nextDouble()*30;int x=(int)Math.floor(p.getX()+Math.cos(a)*r),z=(int)Math.floor(p.getZ()+Math.sin(a)*r);
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
        if(existing>=OUTSIDE_CAP)return;
        var ruin=l.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("ruin_scattered"));
        if(ruin!=null){
            int cx=p.getBlockX()>>4,cz=p.getBlockZ()>>4;
            var sites=new ArrayList<RuinSite>();
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++){
                var chunk=l.getChunkSource().getChunkNow(cx+dx,cz+dz);if(chunk==null)continue;
                var start=chunk.getStartForStructure(ruin);if(start==null||!start.isValid())continue;
                BoundingBox box=start.getBoundingBox();
                int x=(box.minX()+box.maxX())/2,z=(box.minZ()+box.maxZ())/2;
                double d=Math.hypot(x-p.getX(),z-p.getZ());
                long key=BlockPos.asLong(x,0,z);
                if(d>64||rest.getOrDefault(key,Long.MIN_VALUE)>l.getGameTime())continue;
                if(Math.floorMod(mixSite(layout.seed()^key),3)==0)continue;
                if(count(l,new Vec3(x,p.getY(),z),RUIN_RADIUS)>0)continue;
                sites.add(new RuinSite(x,z,key,d,box));
            }
            sites.sort(Comparator.comparingDouble(RuinSite::distance));
            for(int n=0;n<Math.min(3,sites.size());n++){
                RuinSite site=sites.get(n);
                int left=Math.min(1+l.random.nextInt(RUIN_CAP),OUTSIDE_CAP-existing),made=0;
                for(int i=0;i<24&&left>0;i++){
                    int x,z;
                    if(i<12){
                        x=site.box.minX()+l.random.nextInt(site.box.maxX()-site.box.minX()+1);
                        z=site.box.minZ()+l.random.nextInt(site.box.maxZ()-site.box.minZ()+1);
                    }else{
                        x=site.x+l.random.nextInt(21)-10;z=site.z+l.random.nextInt(21)-10;
                    }
                    if(spawn(l,layout,x,z,OUTSIDE_CAP,false,site.box)){left--;made++;}
                }
                if(made>0){rest.put(site.key,l.getGameTime()+RUIN_COOLDOWN);return;}
                // A blocked ruin must not prevent trying other sites or the forest.
            }
        }
        if(count(l,p.position(),FOREST_RADIUS)>0||l.random.nextBoolean())return;
        for(int i=0;i<32;i++){
            double a=l.random.nextDouble()*Math.PI*2,r=18+l.random.nextDouble()*16;
            int x=(int)Math.floor(p.getX()+Math.cos(a)*r),z=(int)Math.floor(p.getZ()+Math.sin(a)*r);
            if(!BattlefieldFaunaBoundary.inside(l,x,z)&&!layout.nearRoad(x,z,5)&&spawn(l,layout,x,z,OUTSIDE_CAP,false,null))return;
        }
    }
    private static boolean spawn(ServerLevel l,InkLandsLayout layout,int x,int z,int cap,boolean field,BoundingBox ruin){
        if(layout.depthInside(x,z)<16||Ruins.nearStart(layout,x,z)||Sanctuary.near(layout,x,z,8))return false;
        var chunk=l.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)return false;
        int surface=LoadedFaunaTerrain.surface(chunk,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        // Above the local surface, including ruins; never fall back to ground-level spawns.
        int first=FLIGHT_MIN_HEIGHT+l.random.nextInt(FLIGHT_MAX_HEIGHT-FLIGHT_MIN_HEIGHT+1);
        for(int i=0;i<8;i++){
            int y=surface+(i<3?FLIGHT_MIN_HEIGHT+(first-FLIGHT_MIN_HEIGHT+i)%3:FLIGHT_MAX_HEIGHT+i-2);
            BlockPos pos=new BlockPos(x,y,z);Vec3 at=new Vec3(x+.5,y+.15,z+.5);AABB body=new AABB(at.x-.35,at.y-.15,at.z-.35,at.x+.35,at.y+.85,at.z+.35);
            if(!LoadedFaunaTerrain.free(l,null,body)||!l.noCollision(body)||!l.getFluidState(pos).isEmpty()||!l.getFluidState(pos.above()).isEmpty())continue;
            if(!(l.getBiome(pos).is(ModBiomes.PALE_FOREST)||l.getBiome(pos).is(ModBiomes.INKWOOD)))continue;
            if(field!=BattlefieldFaunaBoundary.inside(l,x,z))continue;
            if(com.tarkrakov.tearsofink.world.herald.BloodSettlements.contains(l,pos))continue;
            if(count(l,at,LOCAL_RADIUS)>=cap||count(l,at,3)>0)return false;
            boolean close=false;for(ServerPlayer other:l.players()){
                if(other.isSpectator()||!other.isAlive())continue;
                double d=other.position().subtract(at).horizontalDistanceSqr();
                if(d<SPAWN_MIN_DISTANCE*SPAWN_MIN_DISTANCE||(d<LOCAL_RADIUS*LOCAL_RADIUS&&count(l,other.position(),LOCAL_RADIUS)>=cap)){close=true;break;}}
            if(close)return false;
            MorokEntity mob=ModMobs.MOROK.create(l);if(mob==null)return false;
            mob.moveTo(at.x,at.y,at.z,l.random.nextFloat()*360,0);mob.encounter(at);return l.addFreshEntity(mob);
        }
        return false;
    }
}
