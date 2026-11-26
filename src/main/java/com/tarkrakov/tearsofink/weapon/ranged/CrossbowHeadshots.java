package com.tarkrakov.tearsofink.weapon.ranged;

import com.tarkrakov.tearsofink.entity.ModMobs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Conservative server hit regions inside the real collision box.
 * No automatic headshot for an unknown/non-humanoid body plan.
 * Authored mobs have individual rest-pose bands; these are not animated bone hitboxes.
 */
final class CrossbowHeadshots {
    private CrossbowHeadshots() {}

    static Vec3 contact(RangedProjectile projectile, LivingEntity target, Vec3 from, Vec3 to) {
        if(from==null||to==null||from.distanceToSqr(to)<1e-8) return null;
        AABB body=target.getBoundingBox();
        double height=body.getYsize(), width=body.getXsize();
        if(height<=0||width<=0) return null;
        var type=target.getType();
        double low, high, radius;
        if(type==ModMobs.PALE_SPEARMAN){
            // Small head on the projecting neck: rest model Y=1.25–1.625.
            low=body.minY+height*.735; high=body.minY+height*.96; radius=width*.40;
        }else if(type==ModMobs.PALE_CROSSBOWMAN){
            // Leaning head, lower than the top of the standing collision box.
            low=body.minY+height*.72; high=body.minY+height*.96; radius=width*.40;
        }else if(type==ModMobs.PALE_SHIELDBEARER){
            low=body.minY+height*.79; high=body.minY+height*.98; radius=width*.35;
        }else if(type==ModMobs.ORION){
            // 6×7×7 px head at 46–53 px above the feet, render scale .82.
            // Exclude the broad shoulders and mane from the weak point.
            low=body.minY+height*.83; high=body.minY+height*.97; radius=width*.15;
        }else if(type==EntityType.CREEPER){
            low=body.minY+height*.70; high=body.maxY; radius=width*.46;
        }else if(target instanceof Player||target instanceof Zombie||target instanceof AbstractSkeleton
            ||target instanceof AbstractIllager||target instanceof AbstractPiglin
            ||type==EntityType.VILLAGER||type==EntityType.WANDERING_TRADER||type==EntityType.WITCH
            ||type==EntityType.ENDERMAN||type==EntityType.IRON_GOLEM){
            // Eye-height follows vanilla changes in age and standing/crouching pose.
            if(target.getPose()==Pose.SLEEPING||target.getPose()==Pose.SWIMMING
                ||target.getPose()==Pose.FALL_FLYING||target.getPose()==Pose.SPIN_ATTACK)return null;
            double eye=target.getEyeY();
            low=Math.max(body.minY,eye-Math.min(height*.30,width*.50));
            high=Math.min(body.maxY,eye+Math.min(height*.20,width*.36));
            radius=width*.48;
        }else return null;
        if(high<=low||radius<=0)return null;

        // ProjectileUtil's entity collision has an inflated box, and some vanilla
        // hit results carry the entity origin rather than an exact contact.
        // Reconstruct the segment of this tick and require real body entry.
        Vec3 entry=body.contains(from)?from:body.clip(from,to).orElse(null);
        if(entry==null||entry.y<low-.025||entry.y>high+.025)return null;
        AABB head=new AABB(target.getX()-radius,low,target.getZ()-radius,
                           target.getX()+radius,high,target.getZ()+radius);
        Vec3 point=head.contains(entry)?entry:head.clip(entry,to).orElse(null);
        if(point==null)return null;
        // A wall behind the inflated collision margin cannot grant a critical hit.
        if(projectile.level().clip(new ClipContext(from,point,ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,projectile)).getType()!=HitResult.Type.MISS)return null;
        return point;
    }
}
