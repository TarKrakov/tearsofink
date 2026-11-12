package com.tarkrakov.tearsofink.entity.spearman;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Бледный копейщик — патруль дорог Бледнолесья. Модель и анимации автора.
 * Спокоен: ходит по дороге между фонарями, копьё на плече. Заметил игрока: встаёт в стойку,
 * держит дистанцию копья, тычет (урон на 0,5 с анимации), отпрыгивает назад, если к нему лезут.
 */
public class PaleSpearmanEntity extends Monster {
	private static final EntityDataAccessor<Boolean> AGGRESSIVE = SynchedEntityData.defineId(PaleSpearmanEntity.class, EntityDataSerializers.BOOLEAN);
	private static final byte THRUST_EVENT = 4, DODGE_EVENT = 5;

	// анимации (клиент)
	public final AnimationState idleState = new AnimationState();
	public final AnimationState stanceState = new AnimationState();
	public final AnimationState thrustState = new AnimationState();
	public final AnimationState dodgeState = new AnimationState();
	private int thrustAnimLeft, dodgeAnimLeft;

	// бой (сервер)
	int attackCooldown;
	int thrustTicks = -1;
	int dodgeCooldown;
	int dodgeTicks = -1;
	private Vec3 dodgeAway = Vec3.ZERO;

	// патруль
	int patrolRoad = -1;
	double patrolFrom, patrolTo;

	public PaleSpearmanEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 6;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, SpearmanTuning.HEALTH)
				.add(Attributes.ATTACK_DAMAGE, SpearmanTuning.DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, SpearmanTuning.SPEED)
				.add(Attributes.FOLLOW_RANGE, SpearmanTuning.FOLLOW_RANGE)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(AGGRESSIVE, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SpearmanCombatGoal(this));
		this.goalSelector.addGoal(5, new RoadPatrolGoal(this));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public boolean isAggressiveSynced() {
		return this.entityData.get(AGGRESSIVE);
	}

	public boolean busy() {
		return thrustTicks >= 0 || dodgeTicks >= 0;
	}

	/** Начать тычок (сервер). */
	void startThrust() {
		thrustTicks = 0;
		attackCooldown = SpearmanTuning.ATTACK_COOLDOWN;
		this.level().broadcastEntityEvent(this, THRUST_EVENT);
	}

	/** Начать отпрыжку от цели (сервер). */
	void startDodge(LivingEntity from) {
		dodgeTicks = 0;
		dodgeCooldown = SpearmanTuning.DODGE_COOLDOWN;
		Vec3 away = this.position().subtract(from.position());
		away = new Vec3(away.x, 0, away.z);
		dodgeAway = away.lengthSqr() < 1.0E-4 ? this.getLookAngle().reverse() : away.normalize();
		this.getNavigation().stop();
		this.level().broadcastEntityEvent(this, DODGE_EVENT);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide) return;
		this.entityData.set(AGGRESSIVE, this.getTarget() != null && this.getTarget().isAlive());
		if (attackCooldown > 0) attackCooldown--;
		if (dodgeCooldown > 0) dodgeCooldown--;

		if (thrustTicks >= 0) {
			thrustTicks++;
			if (thrustTicks == SpearmanTuning.THRUST_HIT_TICK) spearHit();
			if (thrustTicks >= SpearmanTuning.THRUST_TICKS) thrustTicks = -1;
		}
		if (dodgeTicks >= 0) {
			dodgeTicks++;
			if (dodgeTicks == SpearmanTuning.DODGE_PUSH_TICK && this.onGround()) {
				this.setDeltaMovement(dodgeAway.x * SpearmanTuning.DODGE_BACK, SpearmanTuning.DODGE_UP,
						dodgeAway.z * SpearmanTuning.DODGE_BACK);
				this.hasImpulse = true;
			}
			if (dodgeTicks >= SpearmanTuning.DODGE_TICKS) dodgeTicks = -1;
		}
	}

	/** Момент выпада: бьёт цель, если она перед ним на длину копья. */
	private void spearHit() {
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive()) return;
		if (this.distanceTo(target) > SpearmanTuning.REACH + target.getBbWidth() / 2) return;
		Vec3 look = this.getLookAngle();
		Vec3 to = target.position().subtract(this.position());
		Vec3 flat = new Vec3(to.x, 0, to.z);
		if (flat.lengthSqr() > 1.0E-4 && new Vec3(look.x, 0, look.z).normalize().dot(flat.normalize()) < 0.5) return;
		if (!this.hasLineOfSight(target)) return;
		this.doHurtTarget(target);
		this.playSound(SoundEvents.TRIDENT_HIT, 0.8f, 0.9f);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == THRUST_EVENT) {
			thrustAnimLeft = SpearmanTuning.THRUST_TICKS;
			thrustState.start(this.tickCount);
		} else if (id == DODGE_EVENT) {
			dodgeAnimLeft = SpearmanTuning.DODGE_TICKS;
			dodgeState.start(this.tickCount);
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) return;
		if (thrustAnimLeft > 0 && --thrustAnimLeft == 0) thrustState.stop();
		if (dodgeAnimLeft > 0 && --dodgeAnimLeft == 0) dodgeState.stop();
		boolean special = thrustAnimLeft > 0 || dodgeAnimLeft > 0;
		boolean moving = this.walkAnimation.isMoving();
		idleState.animateWhen(!special && !moving && !isAggressiveSynced(), this.tickCount);
		stanceState.animateWhen(!special && !moving && isAggressiveSynced(), this.tickCount);
	}

	public boolean specialAnimating() {
		return thrustAnimLeft > 0 || dodgeAnimLeft > 0;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		boolean hurt = super.hurt(source, amount);
		if (hurt && !this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker
				&& dodgeCooldown <= 0 && thrustTicks < 0 && dodgeTicks < 0 && this.onGround()
				&& this.distanceTo(attacker) < SpearmanTuning.REACH) {
			startDodge(attacker);
		}
		return hurt;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (patrolRoad >= 0) {
			tag.putInt("PatrolRoad", patrolRoad);
			tag.putDouble("PatrolFrom", patrolFrom);
			tag.putDouble("PatrolTo", patrolTo);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("PatrolRoad")) {
			patrolRoad = tag.getInt("PatrolRoad");
			patrolFrom = tag.getDouble("PatrolFrom");
			patrolTo = tag.getDouble("PatrolTo");
		}
	}

	public void setPatrol(int road, double from, double to) {
		this.patrolRoad = road;
		this.patrolFrom = from;
		this.patrolTo = to;
	}

	// звуки-заглушки: глухие, «пустые» — скелет потише
	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.7f;
	}
}
