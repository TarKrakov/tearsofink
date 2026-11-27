package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.entity.patrol.ForestCamps;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary;
import com.tarkrakov.tearsofink.world.herald.BloodSettlements;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-only bounded destruction. Never requests chunks, breaks inventory or protected sites. */
final class TrollBlockSmash {
    private TrollBlockSmash(){}
    private static boolean allowed(ServerLevel l,TrollEntity mob,BlockPos p){
        if(!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||LoadedFaunaTerrain.chunk(l,p)==null
                ||!l.getWorldBorder().isWithinBounds(p)||!TrollCave.clearOfStructures(l,p))return false;
        if(ModDimensions.isLands(l)){
            InkLandsLayout layout=InkLandsLayout.of(l);
            if(layout==null||layout.nearRoad(p.getX(),p.getZ(),8)||Ruins.nearStart(layout,p.getX(),p.getZ())
                    ||Sanctuary.near(layout,p.getX(),p.getZ(),8)||BattlefieldFaunaBoundary.inside(l,p.getX(),p.getZ())
                    ||BloodSettlements.contains(l,p)||ForestCamps.near(l,p,30)||layout.nearHerald(p.getX(),p.getZ(),40))return false;
        }
        var state=l.getBlockState(p);
        if(state.isAir()||state.hasBlockEntity()||!state.getFluidState().isEmpty()||state.is(Blocks.BEDROCK))return false;
        float hardness=state.getDestroySpeed(l,p);
        return hardness>=0&&hardness<=6;
    }
    private static boolean clear(ServerLevel l,TrollEntity mob,AABB box,int max,boolean test,boolean solidOnly){
        if(!LoadedFaunaTerrain.loaded(l,box,1))return false;
        int broken=0;
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(box.minX+.001,box.minY+.001,box.minZ+.001),
                BlockPos.containing(box.maxX-.001,box.maxY-.001,box.maxZ-.001))){
            if(!allowed(l,mob,p))continue;
            if(solidOnly&&l.getBlockState(p).getCollisionShape(l,p).isEmpty())continue;
            if(test)return true;
            if(l.destroyBlock(p,false,mob)&&++broken>=max)break;
        }
        return broken>0;
    }
    private static AABB bellyBox(TrollEntity mob,float yaw){
        Vec3 f=Vec3.directionFromRotation(0,yaw);
        return mob.getBoundingBox().deflate(.15,.15,.15).expandTowards(f.x*2.3,0,f.z*2.3);
    }
    static boolean hasObstacle(ServerLevel l,TrollEntity mob,float yaw){
        return clear(l,mob,bellyBox(mob,yaw),1,true,true);
    }
    static void belly(ServerLevel l,TrollEntity mob,float yaw){
        clear(l,mob,bellyBox(mob,yaw),48,false,true);
    }
    static void flight(ServerLevel l,TrollEntity mob,Vec3 motion){
        // Preserve supporting ground on descent; clear only the rising body
        // and horizontal corridor at current body height.
        if(motion.y<0&&motion.horizontalDistanceSqr()<.0001)return;
        AABB swept=mob.getBoundingBox().deflate(.05,.05,.05)
                .expandTowards(motion.x,Math.max(0,motion.y),motion.z);
        clear(l,mob,swept,24,false,true);
    }
    static void landing(ServerLevel l,TrollEntity mob){
        Vec3 at=mob.position();
        AABB box=new AABB(at.x-3,at.y-.9,at.z-3,at.x+3,at.y+2.6,at.z+3);
        if(!LoadedFaunaTerrain.loaded(l,box,1))return;
        int broken=0;
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
            double dx=p.getX()+.5-at.x,dz=p.getZ()+.5-at.z,dist=dx*dx+dz*dz;
            if(dist>9||p.getY()<at.y&&dist<2.25||!allowed(l,mob,p))continue;
            if(l.destroyBlock(p,false,mob)&&++broken>=64)break;
        }
    }
}
