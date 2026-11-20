package com.tarkrakov.tearsofink.weapon;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Волна чернил: летит прямо, проходит сквозь врагов (каждого бьёт один раз), гаснет о стену
 * или через ~12 блоков. Видна как поток чернильных брызг.
 */
public class InkWaveEntity extends Entity {
	// ===== Настройки — крутить здесь =====
	public static final double SPEED = 0.9;
	public static final double MAX_DISTANCE = 12;
	public static final float DAMAGE = 6f;

	/** Светлая волна (щит Отпора) — другие частицы, свой урон и отбрасывание. */
	private static final EntityDataAccessor<Boolean> LIGHT = SynchedEntityData.defineId(InkWaveEntity.class, EntityDataSerializers.BOOLEAN);

	private float damage = DAMAGE;
	private double knockback = 0.4;
	private UUID ownerId;
	private UUID onlyTarget;
	private double travelled = 0;
	private final Set<UUID> hit = new HashSet<>();

	public InkWaveEntity(EntityType<? extends InkWaveEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public InkWaveEntity(Level level, ServerPlayer owner, Vec3 dir) {
		this(level, owner, dir, false, DAMAGE, 0.4);
	}

	/** Волна охотника: задевает только его врага (только этого, остальных — нет). */
	public InkWaveEntity onlyTarget(@org.jetbrains.annotations.Nullable Entity target) {
		this.onlyTarget = target == null ? null : target.getUUID();
		return this;
	}

	public InkWaveEntity(Level level, net.minecraft.world.entity.LivingEntity owner, Vec3 dir, boolean light, float damage, double knockback) {
		this(ModEntities.INK_WAVE, level);
		this.entityData.set(LIGHT, light);
		this.damage = damage;
		this.knockback = knockback;
		this.ownerId = owner.getUUID();
		this.setPos(owner.getX() + dir.x * 0.8, owner.getY() + 0.6, owner.getZ() + dir.z * 0.8);
		this.setDeltaMovement(dir.scale(SPEED));
		this.setYRot((float) (Math.atan2(-dir.x, dir.z) * 180 / Math.PI));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(LIGHT, false);
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 v = this.getDeltaMovement();
		Vec3 next = this.position().add(v);

		if (this.level().isClientSide) {
			// поток брызг: широкая дуга поперёк движения
			Vec3 side = new Vec3(-v.z, 0, v.x).normalize();
			for (int i = -2; i <= 2; i++) {
				Vec3 p = this.position().add(side.scale(i * 0.35));
				this.level().addParticle(this.entityData.get(LIGHT) ? ParticleTypes.END_ROD : ParticleTypes.SQUID_INK,
						p.x, p.y + this.random.nextDouble() * 0.6, p.z, 0, 0.01, 0);
			}
			this.setPos(next);
			return;
		}

		ServerLevel level = (ServerLevel) this.level();
		BlockPos at = BlockPos.containing(next.x, next.y + 0.3, next.z);
		if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty() || travelled >= MAX_DISTANCE) {
			level.sendParticles(this.entityData.get(LIGHT) ? ParticleTypes.END_ROD : ParticleTypes.SQUID_INK,
					getX(), getY() + 0.3, getZ(), 15, 0.4, 0.3, 0.4, 0.05);
			this.discard();
			return;
		}
		Entity owner = ownerId == null ? null : level.getEntity(ownerId);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.3),
				t -> t != owner && t.isAlive() && (onlyTarget == null || onlyTarget.equals(t.getUUID()))
						&& (!(owner instanceof ServerPlayer player && this.entityData.get(LIGHT))
								|| com.tarkrakov.tearsofink.weapon.shield.ShieldStance.offensiveTarget(player, t)))) {
			if (!hit.add(target.getUUID())) continue;
			if (owner instanceof ServerPlayer player) {
				com.tarkrakov.tearsofink.weapon.forge.Gear.hurtModWave(target, player.damageSources().playerAttack(player), damage);
			} else if (owner instanceof LivingEntity mob) {
				target.hurt(level.damageSources().mobAttack(mob), damage);
			} else {
				target.hurt(level.damageSources().magic(), damage);
			}
			target.knockback(knockback, -v.x, -v.z);
		}
		this.setPos(next);
		travelled += v.length();
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.discard(); // волна не переживает сохранение мира
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
