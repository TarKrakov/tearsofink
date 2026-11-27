package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.world.*;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary;
import com.tarkrakov.tearsofink.world.herald.BloodSettlements;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import java.util.*;
import static com.tarkrakov.tearsofink.entity.TrollTerritoryRules.*;

/** One or two persistent residents per discovered clearing. Never loads chunks. */
final class TrollEncounters {
    private TrollEncounters(){}
    static void init(){
        ServerTickEvents.END_WORLD_TICK.register(TrollEncounters::tick);
        ServerEntityEvents.ENTITY_LOAD.register((e,l)->{if(ModDimensions.isLands(l)&&e instanceof TrollEntity t)TrollTerritories.of(l).loaded(t);});
        ServerEntityEvents.ENTITY_UNLOAD.register((e,l)->{
            if(ModDimensions.isLands(l)&&e instanceof TrollEntity t&&t.territoryId()!=null&&e.getRemovalReason()==Entity.RemovalReason.DISCARDED)
                TrollTerritories.of(l).died(t.territoryId(),t.getUUID(),l.getGameTime());
        });
    }
    private static void tick(ServerLevel l){
        if(!ModDimensions.isLands(l)||l.getDifficulty()==Difficulty.PEACEFUL||!l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        TrollTerritories data=TrollTerritories.of(l);
        for(ServerPlayer p:l.players()){
            if(!p.isAlive()||p.isSpectator()||Math.floorMod(l.getGameTime()+p.getUUID().hashCode(),300)!=0||!LoadedFaunaTerrain.search(l))continue;
            InkLandsLayout layout=InkLandsLayout.of(l);if(layout==null)continue;
            // Only replenish confirmed vacant slots; UUIDs of unloaded residents
            // remain occupied indefinitely. Reuse exactly the saved bed.
            boolean replenished=false;
            for(TrollTerritories.Zone z:data.zones()){
                if(!within(p.getX(),p.getZ(),z.center.getX()+.5,z.center.getZ()+.5,SPAWN_MAX+BED_MAX))continue;
                for(TrollTerritories.Slot slot:z.slots){
                    if(slot.vacant(l.getGameTime())&&nearEnough(p,slot.bed)&&farFromPlayers(l,slot.bed)
                        &&validBed(l,layout,slot.bed)&&spawn(l,data,z,slot))replenished=true;
                }
            }
            if(replenished||l.random.nextInt(3)!=0)continue;
            int desired=size(l.random.nextInt(100));
            for(int i=0;i<20;i++){
                double a=l.random.nextDouble()*Math.PI*2,r=36+l.random.nextDouble()*24;
                int x=(int)Math.floor(p.getX()+Math.cos(a)*r),z=(int)Math.floor(p.getZ()+Math.sin(a)*r);
                if(layout.depthInside(x,z)<CHASE||layout.nearRoad(x,z,DEFEND+12)||Ruins.nearStart(layout,x,z)
                    ||Sanctuary.near(layout,x,z,CHASE+8)||BattlefieldFaunaBoundary.inside(l,x,z))continue;
                BlockPos center=site(l,x,z);
                if(center==null||!data.available(center)||!validBed(l,layout,center)||!farFromPlayers(l,center))continue;
                // Keep the same roll throughout the search. Never silently
                // downgrade a selected family to an intentional singleton.
                List<BlockPos> beds=new ArrayList<>();beds.add(center);
                if(desired==2){
                    for(int j=0;j<24;j++){
                        double angle=l.random.nextDouble()*Math.PI*2,dist=BED_MIN+l.random.nextDouble()*(BED_MAX-BED_MIN);
                        BlockPos other=site(l,x+(int)Math.round(Math.cos(angle)*dist),z+(int)Math.round(Math.sin(angle)*dist));
                        if(other==null||Math.abs(other.getY()-center.getY())>3
                            ||distanceSq(x,z,other.getX(),other.getZ())<BED_MIN*BED_MIN
                            ||!within(x,z,other.getX(),other.getZ(),BED_MAX)
                            ||!farFromPlayers(l,other)||!validBed(l,layout,other))continue;
                        beds.add(other);break;
                    }
                    if(beds.size()!=2)continue;
                }
                TrollTerritories.Zone zone=data.create(center,beds);
                for(TrollTerritories.Slot slot:zone.slots)spawn(l,data,zone,slot);
                break;
            }
        }
    }
    private static boolean nearEnough(ServerPlayer p,BlockPos bed){
        return within(p.getX(),p.getZ(),bed.getX()+.5,bed.getZ()+.5,SPAWN_MAX);
    }
    private static boolean farFromPlayers(ServerLevel l,BlockPos bed){
        for(ServerPlayer p:l.players())if(p.isAlive()&&!p.isSpectator()&&distanceSq(p.getX(),p.getZ(),bed.getX()+.5,bed.getZ()+.5)<SPAWN_MIN*SPAWN_MIN)return false;
        return true;
    }
    /** Ground and complete mob volume must already be loaded and unoccupied. */
    static BlockPos site(ServerLevel l,int x,int z){
        var chunk=l.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)return null;
        int y=LoadedFaunaTerrain.surface(chunk,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        BlockPos p=new BlockPos(x,y,z);return clear(l,p)?p:null;
    }
    private static boolean clear(ServerLevel l,BlockPos p){
        var chunk=LoadedFaunaTerrain.chunk(l,p);if(chunk==null)return false;
        Vec3 at=Vec3.atBottomCenterOf(p);AABB body=new AABB(at.x-1.3,at.y,at.z-1.3,at.x+1.3,at.y+4,at.z+1.3);
        if(!LoadedFaunaTerrain.free(l,null,body)||!l.noCollision(body)||!chunk.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP))return false;
        for(BlockPos q:BlockPos.betweenClosed(BlockPos.containing(body.minX,body.minY,body.minZ),BlockPos.containing(body.maxX,body.maxY,body.maxZ)))
            if(!l.getFluidState(q).isEmpty())return false;
        return true;
    }
    private static boolean validBed(ServerLevel l,InkLandsLayout layout,BlockPos p){
        if(!clear(l,p)||layout.depthInside(p.getX(),p.getZ())<20||layout.nearRoad(p.getX(),p.getZ(),12)
            ||Ruins.nearStart(layout,p.getX(),p.getZ())||Sanctuary.near(layout,p.getX(),p.getZ(),40)
            ||BattlefieldFaunaBoundary.inside(l,p.getX(),p.getZ())||BloodSettlements.contains(l,p))return false;
        return l.getBiome(p).is(ModBiomes.PALE_FOREST)||l.getBiome(p).is(ModBiomes.INKWOOD);
    }
    private static boolean spawn(ServerLevel l,TrollTerritories data,TrollTerritories.Zone z,TrollTerritories.Slot s){
        if(s.member!=null||!clear(l,s.bed))return false;
        TrollEntity mob=ModMobs.TROLL.create(l);if(mob==null)return false;
        Vec3 at=Vec3.atBottomCenterOf(s.bed);mob.moveTo(at.x,at.y,at.z,l.random.nextFloat()*360,0);
        mob.bindTerritory(z.id,z.center,s.bed);data.reserve(s,mob.getUUID());
        if(l.addFreshEntity(mob))return true;
        data.failed(s,l.getGameTime());return false;
    }
}
