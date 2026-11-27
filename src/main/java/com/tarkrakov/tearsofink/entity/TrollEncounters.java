package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.world.*;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary;
import com.tarkrakov.tearsofink.world.herald.BloodSettlements;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;

/** Rare persistent territory, with an unmodified natural clearing as the initial bedding site. */
final class TrollEncounters {
    private TrollEncounters(){}
    static void init(){ServerTickEvents.END_WORLD_TICK.register(TrollEncounters::tick);}
    private static void tick(ServerLevel l){
        if(!ModDimensions.isLands(l)||l.getDifficulty()==Difficulty.PEACEFUL||!l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        for(ServerPlayer p:l.players()){
            if(!p.isAlive()||p.isSpectator()||Math.floorMod(l.getGameTime()+p.getUUID().hashCode(),300)!=0||!LoadedFaunaTerrain.search(l))continue;
            if(!l.getEntitiesOfClass(TrollEntity.class,p.getBoundingBox().inflate(100,64,100),TrollEntity::isAlive).isEmpty()||l.random.nextInt(3)!=0)continue;
            InkLandsLayout layout=InkLandsLayout.of(l);if(layout==null)continue;
            for(int i=0;i<20;i++){
                double a=l.random.nextDouble()*Math.PI*2,r=36+l.random.nextDouble()*24;
                int x=(int)Math.floor(p.getX()+Math.cos(a)*r),z=(int)Math.floor(p.getZ()+Math.sin(a)*r);
                if(layout.depthInside(x,z)<20||layout.nearRoad(x,z,12)||Ruins.nearStart(layout,x,z)||Sanctuary.near(layout,x,z,40)||BattlefieldFaunaBoundary.inside(l,x,z))continue;
                var chunk=l.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)continue;
                int y=LoadedFaunaTerrain.surface(chunk,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                BlockPos pos=new BlockPos(x,y,z);Vec3 at=Vec3.atBottomCenterOf(pos);
                AABB body=new AABB(at.x-1.3,y,at.z-1.3,at.x+1.3,y+4,at.z+1.3);
                if(!LoadedFaunaTerrain.free(l,null,body)||!l.noCollision(body)||!l.getFluidState(pos).isEmpty())continue;
                if(!(l.getBiome(pos).is(ModBiomes.PALE_FOREST)||l.getBiome(pos).is(ModBiomes.INKWOOD))||BloodSettlements.contains(l,pos))continue;
                if(!chunk.getBlockState(pos.below()).isFaceSturdy(l,pos.below(),Direction.UP))continue;
                if(l.players().stream().anyMatch(other->other.isAlive()&&!other.isSpectator()&&other.position().distanceToSqr(at)<32*32))continue;
                if(!l.getEntitiesOfClass(TrollEntity.class,body.inflate(100,64,100),TrollEntity::isAlive).isEmpty())continue;
                TrollEntity mob=ModMobs.TROLL.create(l);if(mob==null)break;
                mob.moveTo(at.x,at.y,at.z,l.random.nextFloat()*360,0);mob.setHome(at);if(l.addFreshEntity(mob))break;
            }
        }
    }
}
