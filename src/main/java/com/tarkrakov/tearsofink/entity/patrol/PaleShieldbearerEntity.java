package com.tarkrakov.tearsofink.entity.patrol;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Бледный щитовик — танк отряда. Крупный, медленный, крепкий. Поднимает щит (спереди почти неуязвим,
 * топор выбивает щит), бьёт щитом вблизи, делает рывок по прямой (сносит мягкие блоки, промахнулся — тупит).
 */
public class PaleShieldbearerEntity extends PaleSoldier {
	public static final int ACT_BLOCK = 1, ACT_BASH = 2, ACT_CHARGE_WINDUP = 3, ACT_CHARGE = 4, ACT_STUN = 5;

	int shieldUp, shieldRest, shieldDisabled;
	int bashTicks = -1, bashCooldown;
	int chargeTicks = -1, chargeCooldown;
	int stun;
	private Vec3 chargeDir = Vec3.ZERO;

	public PaleShieldbearerEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, SquadTuning.TANK_HEALTH)
				.add(Attributes.ATTACK_DAMAGE, SquadTuning.BASH_DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, SquadTuning.TANK_SPEED)
				.add(Attributes.ARMOR, SquadTuning.TANK_ARMOR)
				.add(Attributes.KNOCKBACK_RESISTANCE, SquadTuning.TANK_KNOCKBACK_RES)
				.add(Attributes.FOLLOW_RANGE, 18);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new CombatGoal());
		this.goalSelector.addGoal(4, new FollowLeaderGoal(this, SquadTuning.FORMATION_STEP));
		this.goalSelector.addGoal(5, new RoadPatrolGoal(this));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public boolean shieldRaised() {
		return shieldUp > 0 && shieldDisabled <= 0;
	}

	boolean busy() {
		return bashTicks >= 0 || chargeTicks >= 0 || stun > 0;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide) return;
		if (shieldDisabled > 0) shieldDisabled--;
		if (shieldRest > 0) shieldRest--;
		if (bashCooldown > 0) bashCooldown--;
		if (chargeCooldown > 0) chargeCooldown--;
		if (shieldUp > 0 && --shieldUp == 0) shieldRest = SquadTuning.SHIELD_REST;

		if (stun > 0) stun--;
		if (bashTicks >= 0) tickBash();
		if (chargeTicks >= 0) tickCharge();

		int act = stun > 0 ? ACT_STUN
				: chargeTicks >= 0 ? (chargeTicks < SquadTuning.CHARGE_WINDUP ? ACT_CHARGE_WINDUP : ACT_CHARGE)
				: bashTicks >= 0 ? ACT_BASH
				: shieldRaised() ? ACT_BLOCK : ACT_NONE;
		setAction(act);
	}

	// ----- удар щитом -----
	void startBash() {
		bashTicks = 0;
		bashCooldown = SquadTuning.BASH_COOLDOWN;
		this.getNavigation().stop();
	}

	private void tickBash() {
		bashTicks++;
		if (bashTicks == SquadTuning.BASH_HIT_TICK) {
			LivingEntity t = this.getTarget();
			if (t != null && t.isAlive() && this.distanceTo(t) <= SquadTuning.BASH_RANGE + 0.6 && inFront(t.position(), 0.3)) {
				t.hurt(this.damageSources().mobAttack(this), (float) SquadTuning.BASH_DAMAGE);
				t.knockback(SquadTuning.BASH_KNOCKBACK, this.getX() - t.getX(), this.getZ() - t.getZ());
				this.playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.6f);
			}
		}
		if (bashTicks >= SquadTuning.BASH_TICKS) bashTicks = -1;
	}

	// ----- рывок -----
	void startCharge(LivingEntity target) {
		chargeTicks = 0;
		chargeCooldown = SquadTuning.CHARGE_COOLDOWN;
		Vec3 d = target.position().subtract(this.position()).multiply(1, 0, 1);
		chargeDir = d.lengthSqr() < 1.0E-4 ? this.getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
		this.getNavigation().stop();
		this.playSound(SoundEvents.RAVAGER_ROAR, 0.6f, 1.4f); // заглушка: рык перед рывком
	}

	private void faceCharge() {
		float yaw = (float) (Math.atan2(chargeDir.z, chargeDir.x) * (180 / Math.PI)) - 90f;
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
	}

	private void tickCharge() {
		chargeTicks++;
		faceCharge();
		if (chargeTicks < SquadTuning.CHARGE_WINDUP) return; // замах: стоит, щит поднят
		this.setDeltaMovement(chargeDir.x * SquadTuning.CHARGE_SPEED, this.getDeltaMovement().y, chargeDir.z * SquadTuning.CHARGE_SPEED);
		this.hasImpulse = true;

		LivingEntity t = this.getTarget();
		if (t != null && t.isAlive() && this.getBoundingBox().inflate(0.4).intersects(t.getBoundingBox())) {
			t.hurt(this.damageSources().mobAttack(this), (float) SquadTuning.CHARGE_DAMAGE);
			t.knockback(SquadTuning.CHARGE_KNOCKBACK, -chargeDir.x, -chargeDir.z);
			this.playSound(SoundEvents.SHIELD_BLOCK, 1.2f, 0.5f);
			endCharge(false);
			return;
		}
		if (this.horizontalCollision) {
			breakSoftBlocks();
			this.playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 0.7f);
			endCharge(true);
			return;
		}
		if (chargeTicks >= SquadTuning.CHARGE_WINDUP + SquadTuning.CHARGE_RUN) endCharge(true);
	}

	private void endCharge(boolean missed) {
		chargeTicks = -1;
		this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1, 0.2));
		if (missed) stun = SquadTuning.CHARGE_STUN; // промахнулся — тупит
	}

	/** Сносит мягкие блоки перед собой (если в мире разрешено разрушение мобами). */
	private void breakSoftBlocks() {
		if (!this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
		Vec3 front = this.position().add(chargeDir.scale(this.getBbWidth() / 2 + 0.6));
		Vec3 side = new Vec3(-chargeDir.z, 0, chargeDir.x);
		for (int s = -1; s <= 1; s++) {
			for (int h = 0; h <= 2; h++) {
				BlockPos pos = BlockPos.containing(front.add(side.scale(s * 0.6)).add(0, h + 0.1, 0));
				BlockState state = this.level().getBlockState(pos);
				if (isSoft(state, pos)) this.level().destroyBlock(pos, true, this);
			}
		}
	}

	private boolean isSoft(BlockState state, BlockPos pos) {
		if (state.isAir() || !state.getFluidState().isEmpty()) return false;
		float hardness = state.getDestroySpeed(this.level(), pos);
		if (hardness < 0 || hardness > SquadTuning.CHARGE_BREAK_HARDNESS) return false;
		if (state.hasBlockEntity()) return false;                       // фонари, фонтаны, сундуки и т.п.
		if (state.is(BlockTags.LOGS)) return false;                      // стволы (и Багряного Вестника)
		if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_BRICKS)) return false; // камень
		return true;
	}

	// ----- щит -----
	private boolean inFront(Vec3 point, double cos) {
		Vec3 look = this.getLookAngle().multiply(1, 0, 1);
		Vec3 to = point.subtract(this.position()).multiply(1, 0, 1);
		if (look.lengthSqr() < 1.0E-4 || to.lengthSqr() < 1.0E-4) return true;
		return look.normalize().dot(to.normalize()) > cos;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!this.level().isClientSide && shieldRaised() && source.getSourcePosition() != null
				&& inFront(source.getSourcePosition(), 0.3)) {
			if (source.getDirectEntity() instanceof Projectile) {
				this.playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.8f);
				return false; // стрелы спереди отскакивают
			}
			if (source.getEntity() instanceof LivingEntity attacker && attacker.getMainHandItem().getItem() instanceof AxeItem) {
				shieldDisabled = SquadTuning.SHIELD_AXE_DISABLE; // топор выбил щит
				shieldUp = 0;
				shieldRest = SquadTuning.SHIELD_REST;
				this.playSound(SoundEvents.SHIELD_BREAK, 1.0f, 0.8f);
			} else {
				this.playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 1.0f);
				amount *= SquadTuning.SHIELD_PASS;
			}
		}
		return super.hurt(source, amount);
	}

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
		return 0.55f;
	}

	/** Бой щитовика: лезет к игроку первым, щит, удар щитом, рывок. */
	class CombatGoal extends Goal {
		private int repath;

		CombatGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = getTarget();
			return t != null && t.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity t = getTarget();
			if (t == null) return;
			if (busy()) {
				getNavigation().stop();
				return;
			}
			getLookControl().setLookAt(t, 30f, 30f);
			double dist = distanceTo(t);

			if (dist >= SquadTuning.CHARGE_MIN && dist <= SquadTuning.CHARGE_MAX && chargeCooldown <= 0 && onGround()
					&& hasLineOfSight(t) && getRandom().nextInt(10) == 0) {
				startCharge(t);
				return;
			}
			if (shieldUp <= 0 && shieldRest <= 0 && shieldDisabled <= 0 && dist < SquadTuning.SHIELD_RANGE) {
				shieldUp = SquadTuning.SHIELD_MAX;
			}
			if (shieldRaised() && dist <= SquadTuning.BASH_RANGE && bashCooldown <= 0) {
				startBash();
				return;
			}
			if (dist > 1.8) {
				if (--repath <= 0) {
					repath = 8;
					getNavigation().moveTo(t, shieldRaised() ? 0.55 : 1.0);
				}
			} else {
				getNavigation().stop();
			}
		}
	}
}
