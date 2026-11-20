package com.tarkrakov.tearsofink.weapon.shield;

import com.tarkrakov.tearsofink.weapon.ModEntities;
import com.tarkrakov.tearsofink.weapon.forge.Gear;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Short, non-persistent luminous shield. Server sweeps the segment against walls and one target. */
public final class ShieldPhantomEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> SHIELD = SynchedEntityData.defineId(ShieldPhantomEntity.class, EntityDataSerializers.ITEM_STACK);
    private UUID owner;
    private float damage;
    private double remaining;
    public ShieldPhantomEntity(EntityType<? extends ShieldPhantomEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }
    public ShieldPhantomEntity(ServerLevel level, ServerPlayer player, ItemStack shield, float damage, double reach) {
        this(ModEntities.SHIELD_PHANTOM, level);
        owner = player.getUUID();
        this.damage = damage * ShieldTuning.PHANTOM_DAMAGE_MULT;
        remaining = reach + ShieldTuning.PHANTOM_EXTRA_REACH;
        entityData.set(SHIELD, new ItemStack(shield.getItem()));
        Vec3 start = player.getEyePosition().add(0,-0.20,0);
        setPos(start.x,start.y,start.z);
        setDeltaMovement(player.getLookAngle().normalize().scale(ShieldTuning.PHANTOM_SPEED));
        setYRot(player.getYRot());
        setXRot(player.getXRot());
    }
    public ItemStack shield() { return entityData.get(SHIELD); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(SHIELD, ItemStack.EMPTY); }
    @Override public void tick() {
        super.tick();
        Vec3 from = position(), velocity = getDeltaMovement();
        if (level().isClientSide) {
            setPos(from.add(velocity));
            return;
        }
        ServerLevel level = (ServerLevel)level();
        if (owner == null || !(level.getEntity(owner) instanceof ServerPlayer player) || !player.isAlive() || remaining <= 0 || velocity.lengthSqr() < 1e-6) { discard(); return; }
        Vec3 to = from.add(velocity.normalize().scale(Math.min(remaining, velocity.length())));
        var block = level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        boolean wall = block.getType() != HitResult.Type.MISS;
        if (wall) to = block.getLocation();
        LivingEntity nearest = null;
        double best = from.distanceToSqr(to);
        Vec3 impact = to;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(0.6),e -> ShieldStance.offensiveTarget(player,e))) {
            var box = t.getBoundingBox().inflate(ShieldTuning.PHANTOM_HIT_MARGIN);
            var hit = box.contains(from) ? java.util.Optional.of(from) : box.clip(from,to);
            if (hit.isEmpty() || !player.hasLineOfSight(t)) continue;
            double dist = from.distanceToSqr(hit.get());
            if (dist < best) { best = dist; nearest = t; impact = hit.get(); }
        }
        if (nearest != null) {
            Gear.hurtModWave(nearest, player.damageSources().playerAttack(player), damage);
            level.sendParticles(ParticleTypes.END_ROD,impact.x,impact.y,impact.z,7,0.15,0.2,0.15,0.035);
            discard(); return;
        }
        setPos(to);
        remaining -= from.distanceTo(to);
        if (wall || remaining <= 0) discard();
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean isPickable() { return false; }
}
