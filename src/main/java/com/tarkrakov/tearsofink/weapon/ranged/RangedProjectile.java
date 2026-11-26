package com.tarkrakov.tearsofink.weapon.ranged;

import com.tarkrakov.tearsofink.weapon.ModEntities;
import com.tarkrakov.tearsofink.weapon.blood.Bleeding;
import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned transient projectile, no pickup. Members of one accepted volley share a hit ledger. */
public final class RangedProjectile extends ThrowableItemProjectile {
    private float damage, bleed;
    private boolean slow;
    private int lifetime=100;
    private Map<UUID,Long> pairHits;
    private static final DustParticleOptions PELLET_TRACE = new DustParticleOptions(new Vector3f(1f,.76f,.30f),.75f);
    public static final DustParticleOptions KNIFE_TRACE = new DustParticleOptions(new Vector3f(.55f,.025f,.045f),.65f);
    private Vec3 traceImpact;
    public RangedProjectile(EntityType<? extends RangedProjectile> type, Level level) { super(type,level); }
    public RangedProjectile(ServerLevel level, ServerPlayer owner,float damage,float bleed,boolean slow,Map<UUID,Long> pairHits) {
        super(ModEntities.RANGED_SHOT,owner,level);
        this.damage=damage; this.bleed=bleed; this.slow=slow; this.pairHits=pairHits;
    }
    public static Map<UUID,Long> pair() { return new HashMap<>(); }
    @Override protected Item getDefaultItem(){ return Items.ARROW; }
    public void limitLifetime(int ticks){lifetime=Math.clamp(ticks,1,100);}
    @Override public void tick(){
        // The sprite alone is hard to see at 3.2 blocks/tick (especially on the impact tick).
        // Sample the real server-side path, including a hit before the first client render.
        boolean pellet=level() instanceof ServerLevel && getItem().is(ModItems.PALE_BULLET);
        boolean knife=level() instanceof ServerLevel && getItem().is(ModItems.BLOOD_THROWING_DAGGER);
        Vec3 start=pellet||knife?position():null;
        traceImpact=null;
        super.tick();
        if(pellet||knife){
            Vec3 end=traceImpact!=null?traceImpact:position();
            Vec3 delta=end.subtract(start);
            double length=delta.length();
            if(length>.05){
                int samples=Math.clamp((int)Math.ceil(length/.85),1,5);
                for(int i=0;i<samples;i++){
                    Vec3 point=start.add(delta.scale((i+.5)/samples));
                    ((ServerLevel)level()).sendParticles(knife?KNIFE_TRACE:PELLET_TRACE,point.x,point.y,point.z,1,0,0,0,0);
                }
            }
        }
        if(!level().isClientSide && tickCount>=lifetime) discard();
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if(!(level() instanceof ServerLevel level) || !(getOwner() instanceof ServerPlayer p)
            || !(hit.getEntity() instanceof LivingEntity target) || target==p || target.isAlliedTo(p)
            || !PantheonCombat.permitsInteraction(target,this)) return;
        Long first=pairHits==null ? null : pairHits.get(target.getUUID());
        // Only members of the same accepted volley, within a short interval. Unrelated attacks retain i-frames.
        int old=target.invulnerableTime;
        boolean companion=first!=null && level.getGameTime()-first<=4;
        if(companion) target.invulnerableTime=0;
        boolean accepted;
        try { accepted=target.hurt(damageSources().thrown(this,p),damage); }
        finally { if(companion) target.invulnerableTime=Math.max(old,target.invulnerableTime); }
        if(!accepted) return;
        if(pairHits!=null) pairHits.put(target.getUUID(),level.getGameTime());
        if(slow) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0));
        if(bleed>0 && p.getRandom().nextFloat()<bleed) Bleeding.apply(target,p,1);
    }
    @Override protected void onHit(HitResult result){ traceImpact=result.getLocation(); super.onHit(result); if(!level().isClientSide) discard(); }
}
