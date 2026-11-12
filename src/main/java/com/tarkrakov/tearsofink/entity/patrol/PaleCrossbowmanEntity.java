package com.tarkrakov.tearsofink.entity.patrol;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Бледный арбалетчик — держит дистанцию и постоянно напрягает. Цикл: прицел → выстрел → перезарядка.
 * Если игрок подбежал — один раз за цикл отпрыгивает (можно прервать даже перезарядку),
 * дальше обязан дозарядиться и выстрелить — тут его и ловить. Бегущий игрок его догонит.
 */
public class PaleCrossbowmanEntity extends PaleSoldier {
	public static final int ACT_AIM = 1, ACT_SHOOT = 2, ACT_RELOAD = 3, ACT_DODGE = 4;
	private static final int PHASE_AIM = 0, PHASE_SHOOT = 1, PHASE_RELOAD = 2;

	int phase = PHASE_AIM, phaseTicks;
	boolean dodgedThisCycle;
	int dodgeTicks = -1, dodgeCooldown;
	private Vec3 dodgeAway = Vec3.ZERO;

	public PaleCrossbowmanEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 7;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, SquadTuning.ARCHER_HEALTH)
				.add(Attributes.MOVEMENT_SPEED, SquadTuning.ARCHER_SPEED)
				.add(Attributes.FOLLOW_RANGE, SquadTuning.SHOOT_RANGE);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new CombatGoal());
		this.goalSelector.addGoal(4, new FollowLeaderGoal(this, SquadTuning.FORMATION_STEP * 2));
		this.goalSelector.addGoal(5, new RoadPatrolGoal(this));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		addFactionTargets();
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide) return;
		if (dodgeCooldown > 0) dodgeCooldown--;
		if (dodgeTicks >= 0) {
			dodgeTicks++;
			if (dodgeTicks == SquadTuning.ARCHER_DODGE_PUSH_TICK) {
				this.setDeltaMovement(dodgeAway.x * SquadTuning.ARCHER_DODGE_BACK,
						Math.max(SquadTuning.ARCHER_DODGE_UP, this.getDeltaMovement().y), dodgeAway.z * SquadTuning.ARCHER_DODGE_BACK);
				this.hasImpulse = true;
			}
			if (dodgeTicks >= SquadTuning.ARCHER_DODGE_TICKS) dodgeTicks = -1;
		}
		LivingEntity t = this.getTarget();
		int act = ACT_NONE;
		if (dodgeTicks >= 0) act = ACT_DODGE;
		else if (t != null && t.isAlive()) act = phase == PHASE_AIM ? ACT_AIM : phase == PHASE_SHOOT ? ACT_SHOOT : ACT_RELOAD;
		setAction(act);
	}

	void startDodge(LivingEntity from) {
		dodgeTicks = 0;
		dodgeCooldown = SquadTuning.ARCHER_DODGE_COOLDOWN;
		dodgedThisCycle = true;
		Vec3 away = this.position().subtract(from.position()).multiply(1, 0, 1);
		dodgeAway = away.lengthSqr() < 1.0E-4 ? this.getLookAngle().reverse() : away.normalize();
		this.getNavigation().stop();
	}

	private void shoot(LivingEntity target) {
		PaleBoltEntity bolt = new PaleBoltEntity(this.level(), this);
		double dx = target.getX() - this.getX();
		double dy = target.getY(0.5) - bolt.getY();
		double dz = target.getZ() - this.getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		bolt.shoot(dx, dy + flat * 0.12, dz, (float) SquadTuning.BOLT_SPEED,
				SquadTuning.BOLT_SPREAD * (4 - this.level().getDifficulty().getId()) / 2f + 1f);
		this.level().addFreshEntity(bolt);
		this.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.8f);
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
		return 0.8f;
	}

	/** Бой арбалетчика: дистанция, цикл выстрела, одна отпрыжка за цикл. */
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
		public void start() {
			phase = PHASE_AIM;
			phaseTicks = 0;
			dodgedThisCycle = false;
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity t = getTarget();
			if (t == null) return;
			getLookControl().setLookAt(t, 30f, 30f);
			if (dodgeTicks >= 0) return;
			double dist = distanceTo(t);
			boolean sees = hasLineOfSight(t);

			// игрок вплотную — одна отпрыжка за цикл (в т.ч. посреди перезарядки)
			if (dist < SquadTuning.ARCHER_DODGE_CLOSE && !dodgedThisCycle && dodgeCooldown <= 0 && onGround()) {
				startDodge(t);
				return;
			}

			// цикл выстрела (по мобам — быстрее)
			phaseTicks++;
			float rate = t instanceof net.minecraft.world.entity.player.Player ? 1f : SquadTuning.VS_MOBS_FIRE_RATE;
			switch (phase) {
				case PHASE_AIM -> {
					if (!sees || dist > SquadTuning.SHOOT_RANGE) {
						phaseTicks = 0; // потерял — целится заново
					} else if (phaseTicks >= SquadTuning.AIM_TICKS * rate) {
						shoot(t);
						phase = PHASE_SHOOT;
						phaseTicks = 0;
					}
				}
				case PHASE_SHOOT -> {
					if (phaseTicks >= SquadTuning.SHOOT_TICKS) {
						phase = PHASE_RELOAD;
						phaseTicks = 0;
					}
				}
				default -> {
					if (phaseTicks >= SquadTuning.RELOAD_TICKS * rate) {
						phase = PHASE_AIM;
						phaseTicks = 0;
						dodgedThisCycle = false; // новый цикл — снова может отпрыгнуть
					}
				}
			}

			// движение: держать дистанцию; при прицеле и выстреле стоит
			boolean standing = phase != PHASE_RELOAD && sees && dist >= SquadTuning.KEEP_MIN && dist <= SquadTuning.KEEP_MAX;
			if (standing) {
				getNavigation().stop();
				return;
			}
			if (--repath > 0) return;
			repath = 10;
			if (dist < SquadTuning.KEEP_MIN) {
				Vec3 away = position().subtract(t.position()).multiply(1, 0, 1);
				if (away.lengthSqr() < 1.0E-4) away = getLookAngle().reverse();
				Vec3 to = position().add(away.normalize().scale(6));
				getNavigation().moveTo(to.x, to.y, to.z, 1.1);
			} else if (dist > SquadTuning.KEEP_MAX || !sees) {
				getNavigation().moveTo(t, 1.0);
			} else {
				getNavigation().stop();
			}
		}
	}
}
