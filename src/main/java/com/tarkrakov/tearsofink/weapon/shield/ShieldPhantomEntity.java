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
import net.minecraft.util.Mth;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A short shield sweep. The network entity stays at the launch point: no velocity,
 * position interpolation or restarting client simulation. Both sides evaluate the
 * same bounded path; only the server resolves contact and damage.
 */
public final class ShieldPhantomEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> SHIELD = SynchedEntityData.defineId(ShieldPhantomEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> RANGE = SynchedEntityData.defineId(ShieldPhantomEntity.class, EntityDataSerializers.FLOAT);
    private UUID owner;
    private float damage;
    private int age;
    private final Set<UUID> hit = new HashSet<>();

    public ShieldPhantomEntity(EntityType<? extends ShieldPhantomEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public ShieldPhantomEntity(ServerLevel level, ServerPlayer player, ItemStack shield, float damage) {
        this(ModEntities.SHIELD_PHANTOM, level);
        owner = player.getUUID();
        this.damage = damage * ShieldTuning.PHANTOM_DAMAGE_MULT;
        entityData.set(SHIELD, new ItemStack(shield.getItem()));
        Vec3 start = player.getEyePosition().add(0, -0.35, 0);
        setPos(start.x, start.y, start.z);
        setYRot(player.getYRot());
        setXRot(player.getXRot());
        setDeltaMovement(Vec3.ZERO);
        // Truncate even the visual path at the first wall. Check again on each
        // server tick in case a block is placed during the short flight.
        Vec3 end = start.add(getLookAngle().scale(ShieldTuning.PHANTOM_RANGE));
        var wall = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        float range = (float)(wall.getType() == HitResult.Type.MISS ? ShieldTuning.PHANTOM_RANGE : start.distanceTo(wall.getLocation()));
        entityData.set(RANGE, range);
    }

    public ItemStack shield() { return entityData.get(SHIELD); }
    public int flightAge() { return age; }
    public int flightTicks() {
        return Math.max(1, (int)Math.ceil(entityData.get(RANGE) / ShieldTuning.PHANTOM_SPEED));
    }
    public double flightDistance(float ticks) {
        return Math.min(entityData.get(RANGE), Math.max(0, ticks) * ShieldTuning.PHANTOM_SPEED);
    }
    public Vec3 flightOffset(float partialTick) {
        return getLookAngle().scale(flightDistance(age + partialTick));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SHIELD, ItemStack.EMPTY);
        b.define(RANGE, (float)ShieldTuning.PHANTOM_RANGE);
    }

    @Override public void tick() {
        super.tick();
        // An independent hard lifetime also bounds stationary, obstructed and
        // zero-range sweeps. No client-side setPos or repeated flight.
        if (age >= flightTicks() || age >= ShieldTuning.PHANTOM_MAX_TICKS) {
            discard();
            return;
        }
        int previous = age++;
        if (level().isClientSide) return;

        ServerLevel level = (ServerLevel)level();
        if (owner == null || !(level.getEntity(owner) instanceof ServerPlayer player) || !player.isAlive()) {
            discard();
            return;
        }
        Vec3 origin = position(), direction = getLookAngle();
        Vec3 from = origin.add(direction.scale(flightDistance(previous)));
        Vec3 to = origin.add(direction.scale(flightDistance(age)));
        var wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blocked = wall.getType() != HitResult.Type.MISS;
        if (blocked) to = wall.getLocation();

        // Sweep the whole enlarged shield volume, including the first and last
        // positions. The projectile survives a hit. Each target can take damage
        // only once per cast; a rejected hurt can be retried on a later tick.
        double radius = ShieldTuning.PHANTOM_SIZE / 2.0 + ShieldTuning.PHANTOM_HIT_MARGIN;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(from, to).inflate(radius), t -> ShieldStance.offensiveTarget(player, t) && !hit.contains(t.getUUID()))) {
            AABB targetBox = target.getBoundingBox();
            AABB expanded = targetBox.inflate(radius);
            var contact = expanded.contains(from) ? java.util.Optional.of(from) : expanded.clip(from, to);
            if (contact.isEmpty()) {
                if (!expanded.contains(to)) continue;
                contact = java.util.Optional.of(to);
            }
            Vec3 at = contact.get();
            // Visibility is measured from the launch point, not from wherever
            // the player has moved since casting the phantom.
            Vec3 closest = new Vec3(Mth.clamp(at.x, targetBox.minX, targetBox.maxX),
                    Mth.clamp(at.y, targetBox.minY, targetBox.maxY),
                    Mth.clamp(at.z, targetBox.minZ, targetBox.maxZ));
            if (level.clip(new ClipContext(origin, closest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) continue;
            if (Gear.hurtModWave(target, player.damageSources().playerAttack(player), damage)) {
                hit.add(target.getUUID());
                level.sendParticles(ParticleTypes.END_ROD, closest.x, closest.y, closest.z, 7, 0.15, 0.2, 0.15, 0.035);
            }
        }
        if (blocked) discard();
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean isPickable() { return false; }
}
