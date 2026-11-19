package com.tarkrakov.tearsofink.entity.patrol;

import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import java.util.EnumSet;

/** Long rests near campfires; bounded individual walking, never a road patrol. */
final class CampPatrolGoal extends Goal {
    private final PaleSoldier mob;
    private int pause;
    private Path route;
    CampPatrolGoal(PaleSoldier mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
    @Override public boolean canUse(){
        if(!mob.isCampGuard()||mob.getTarget()!=null||!(mob.level() instanceof ServerLevel level))return false;
        if(pause>0&&mob.campAllows(mob.blockPosition())){pause--;return false;}
        if(!ModDimensions.chunksReady(level,mob.blockPosition(),48))return false;
        boolean returning=!mob.campAllows(mob.blockPosition());
        // Return from a shove or a fight; no teleport. Elevated guards never choose a downward route.
        if(returning){
            route=mob.getNavigation().createPath(mob.campHome(),0);
            if(route!=null&&route.canReach())return true;
            pause=40;return false;
        }
        BlockPos center=mob.isCampElevated()?mob.campHome():mob.campRest();
        int radius=mob.isCampElevated()?1:mob.getRandom().nextInt(4)==0?10:5;
        for(int i=0;i<6;i++){
            BlockPos p=center.offset(mob.getRandom().nextInt(radius*2+1)-radius,0,mob.getRandom().nextInt(radius*2+1)-radius);
            if(!mob.isCampElevated()&&i%3==1)p=p.above();
            if(!mob.isCampElevated()&&i%3==2)p=p.below();
            if(!mob.campAllows(p)||!ForestCamps.canStand(level,mob,p)||p.equals(mob.blockPosition()))continue;
            Path path=mob.getNavigation().createPath(p,0);
            if(path!=null&&path.canReach()&&mob.campPathAllowed(path)){route=path;return true;}
        }
        pause=60+mob.getRandom().nextInt(61);return false;
    }
    @Override public void start(){if(route!=null)mob.getNavigation().moveTo(route,.75);}
    @Override public boolean canContinueToUse(){return mob.isCampGuard()&&mob.getTarget()==null&&!mob.getNavigation().isDone();}
    @Override public void stop(){mob.getNavigation().stop();route=null;pause=80+mob.getRandom().nextInt(161);}
}
