package com.tarkrakov.tearsofink.weapon.blood;

import com.tarkrakov.tearsofink.weapon.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Кровавая дуга Тальвара: короткий широкий полумесяц (R) или крест под 45° (ПКМ парой).
 * Летит недалеко, режет всех на пути (кроме хозяина), вешает кровотечение. Рисуется частицами.
 */
public class BloodArcEntity extends Entity {
	private static final EntityDataAccessor<Boolean> CROSS = SynchedEntityData.defineId(BloodArcEntity.class, EntityDataSerializers.BOOLEAN);
	private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.75f, 0.05f, 0.1f), 1.1f);
	private static final DustParticleOptions DARK = new DustParticleOptions(new Vector3f(0.35f, 0.0f, 0.05f), 1.4f);

	private UUID ownerId;
	private float damage;
	private int bleed;
	private double travelled;
	private final Set<UUID> hit = new HashSet<>();
	/** Задевает только эту цель (дуги охотников бьют только их врага). */
	@org.jetbrains.annotations.Nullable
	private UUID only;

	public BloodArcEntity onlyTarget(LivingEntity target) {
		only = target.getUUID();
		return this;
	}

	public BloodArcEntity(EntityType<? extends BloodArcEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public BloodArcEntity(Level level, LivingEntity owner, Vec3 dir, boolean cross, float damage, int bleed) {
		this(ModEntities.BLOOD_ARC, level);
		this.entityData.set(CROSS, cross);
		this.damage = damage;
		this.bleed = bleed;
		this.ownerId = owner.getUUID();
		this.setPos(owner.getX() + dir.x * 0.8, owner.getY() + 0.9, owner.getZ() + dir.z * 0.8);
		this.setDeltaMovement(dir.scale(BloodTuning.ARC_SPEED));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(CROSS, false);
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 v = getDeltaMovement();
		Vec3 next = position().add(v);
		if (level().isClientSide) {
			drawShape(v);
			setPos(next);
			return;
		}
		ServerLevel level = (ServerLevel) level();
		BlockPos at = BlockPos.containing(next.x, next.y, next.z);
		if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty() || travelled >= BloodTuning.ARC_RANGE) {
			discard();
			return;
		}
		Entity owner = ownerId == null ? null : level.getEntity(ownerId);
		Vec3 side = new Vec3(-v.z, 0, v.x).normalize().scale(BloodTuning.ARC_HALF_WIDTH);
		AABB box = new AABB(position().subtract(side).add(0, -0.8, 0), next.add(side).add(0, 1.0, 0)).inflate(0.3);
		for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, t -> t != owner && t.isAlive() && (only == null || only.equals(t.getUUID())))) {
			if (!hit.add(t.getUUID())) continue;
			boolean hurt = owner instanceof ServerPlayer p ? t.hurt(p.damageSources().playerAttack(p), damage)
					: owner instanceof LivingEntity mob ? t.hurt(level.damageSources().mobAttack(mob), damage)
					: t.hurt(level.damageSources().magic(), damage);
			if (hurt) Bleeding.apply(t, owner, bleed);
		}
		setPos(next);
		travelled += v.length();
	}

	/** Полумесяц поперёк полёта (или крест из двух диагоналей). */
	private void drawShape(Vec3 v) {
		Vec3 side = new Vec3(-v.z, 0, v.x).normalize();
		Vec3 fwd = v.normalize();
		boolean cross = entityData.get(CROSS);
		for (int i = -6; i <= 6; i++) {
			double s = i / 6.0;
			if (cross) {
				for (int d : new int[]{1, -1}) {
					Vec3 p = position().add(side.scale(s * BloodTuning.ARC_HALF_WIDTH)).add(0, s * d * 1.1, 0);
					level().addParticle(i % 3 == 0 ? DARK : RED, p.x, p.y, p.z, 0, 0, 0);
				}
			} else {
				double bend = (1 - s * s) * 0.6; // середина впереди — полумесяц
				Vec3 p = position().add(side.scale(s * BloodTuning.ARC_HALF_WIDTH)).add(fwd.scale(bend));
				level().addParticle(i % 3 == 0 ? DARK : RED, p.x, p.y + random.nextDouble() * 0.15, p.z, 0, 0, 0);
			}
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		discard(); // дуга не переживает сохранение
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
