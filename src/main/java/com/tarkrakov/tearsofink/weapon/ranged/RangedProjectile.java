package com.tarkrakov.tearsofink.weapon.ranged;

import com.tarkrakov.tearsofink.weapon.ModEntities;
import com.tarkrakov.tearsofink.weapon.blood.Bleeding;
import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.particle.ModParticles;
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
    private RangedShotPerks perks=RangedShotPerks.NONE;
    void setPerks(RangedShotPerks value){perks=value;}
    private boolean slow;
    private int lifetime=100;
    private Map<UUID,Long> pairHits;
    private static final DustParticleOptions PELLET_TRACE = new DustParticleOptions(new Vector3f(1f,.76f,.30f),.75f);
    public static final DustParticleOptions KNIFE_TRACE = new DustParticleOptions(new Vector3f(.55f,.025f,.045f),.65f);
    private Vec3 traceImpact, collisionStart, collisionEnd;
    private boolean crossbowBolt(){return getItem().is(Items.ARROW)||getItem().is(ModItems.INK_CROSSBOW_BOLT_EMPOWERED_VISUAL);}
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
        boolean ink=level() instanceof ServerLevel && getItem().is(ModItems.INK_CROSSBOW_BOLT_EMPOWERED_VISUAL);
        Vec3 start=pellet||knife||ink?position():null;
        traceImpact=null;
        boolean bolt=level() instanceof ServerLevel&&crossbowBolt();
        collisionStart=bolt?position():null;
        collisionEnd=bolt?collisionStart.add(getDeltaMovement()):null;
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
        if(ink){
            // Follow the actual server path, including impacts before a client can render.
            Vec3 end=traceImpact!=null?traceImpact:position();
            Vec3 delta=end.subtract(start);
            int samples=Math.clamp((int)Math.ceil(delta.length()/.65),1,5);
            ServerLevel server=(ServerLevel)level();
            for(int i=0;i<samples;i++){
                Vec3 at=start.add(delta.scale((i+.5)/samples));
                server.sendParticles(ModParticles.INK_SPARK,at.x,at.y,at.z,2,.035,.035,.035,.015);
            }
            if(delta.lengthSqr()>.0025)
                server.sendParticles(ModParticles.INK_SMOKE,end.x,end.y,end.z,1,0,0,0,0);
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
        Vec3 head=crossbowBolt()?CrossbowHeadshots.contact(this,target,collisionStart,collisionEnd):null;
        float finalDamage=head!=null?damage*CrossbowTuning.HEADSHOT_MULTIPLIER:damage;
        if(com.tarkrakov.tearsofink.world.ModDimensions.hasLandsRules(level))finalDamage=perks.damage(p,target,finalDamage);
        float before=target.getHealth();
        boolean accepted;
        try { accepted=target.hurt(damageSources().thrown(this,p),finalDamage); }
        finally { if(companion) target.invulnerableTime=Math.max(old,target.invulnerableTime); }
        if(!accepted) return;
        if(com.tarkrakov.tearsofink.world.ModDimensions.hasLandsRules(level))perks.afterHit(p,Math.max(0,before-target.getHealth()));
        if(head!=null){
            level.sendParticles(ParticleTypes.CRIT,head.x,head.y,head.z,10,.12,.12,.12,.12);
            p.playNotifySound(SoundEvents.PLAYER_ATTACK_CRIT,SoundSource.PLAYERS,.9f,1.4f);
        }
        if(pairHits!=null) pairHits.put(target.getUUID(),level.getGameTime());
        if(slow) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0));
        if(bleed>0 && p.getRandom().nextFloat()<bleed) Bleeding.apply(target,p,1);
    }
    @Override protected void onHit(HitResult result){
        traceImpact=result.getLocation();
        if(level() instanceof ServerLevel server && getItem().is(ModItems.INK_CROSSBOW_BOLT_EMPOWERED_VISUAL)){
            server.sendParticles(ModParticles.INK_SPARK,traceImpact.x,traceImpact.y,traceImpact.z,12,.16,.16,.16,.08);
            server.sendParticles(ModParticles.INK_SMOKE,traceImpact.x,traceImpact.y,traceImpact.z,3,.09,.09,.09,.025);
        }
        super.onHit(result);
        if(!level().isClientSide) discard();
    }
}
