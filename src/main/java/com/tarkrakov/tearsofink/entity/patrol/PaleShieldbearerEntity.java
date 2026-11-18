package com.tarkrakov.tearsofink.entity.patrol;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import com.tarkrakov.tearsofink.sound.ModSounds;
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
public class PaleShieldbearerEntity extends PaleSoldier implements com.tarkrakov.tearsofink.weapon.combat.Interruptible {
	public static final int ACT_BLOCK = 1, ACT_BASH = 2, ACT_CHARGE_WINDUP = 3, ACT_CHARGE = 4, ACT_STUN = 5;

	// анимации (клиент)
	public final net.minecraft.world.entity.AnimationState idleState = new net.minecraft.world.entity.AnimationState();
	public final net.minecraft.world.entity.AnimationState blockState = new net.minecraft.world.entity.AnimationState();
	public final net.minecraft.world.entity.AnimationState bashState = new net.minecraft.world.entity.AnimationState();
	public final net.minecraft.world.entity.AnimationState chargeStartState = new net.minecraft.world.entity.AnimationState();
	public final net.minecraft.world.entity.AnimationState chargeState = new net.minecraft.world.entity.AnimationState();
	public final net.minecraft.world.entity.AnimationState stunState = new net.minecraft.world.entity.AnimationState();
	private int lastAction = -1;

	int shieldUp, shieldRest, shieldDisabled;
	int bashTicks = -1, bashCooldown;
	int chargeTicks = -1, chargeCooldown;
	/** Когда последний раз попал по врагу (тики) — давно не попадал → рывок. */
	private int lastLanded;
	/** Сколько блоков снёс за этот рывок. */
	private int chargeBroken;
	private int lastChargeImpactSound = -1000, lastBlockSound = -1000, lastChargeStepSound = -1000;
	private int stunSoundAt = -1;
	int stun;
	private Vec3 chargeDir = Vec3.ZERO;
	private final java.util.Set<Integer> chargeHit = new java.util.HashSet<>();

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
		this.goalSelector.addGoal(3, new com.tarkrakov.tearsofink.entity.patrol.DouseLanternGoal(this));
		this.goalSelector.addGoal(4, new FollowLeaderGoal(this, SquadTuning.FORMATION_STEP));
		this.goalSelector.addGoal(5, new RoadPatrolGoal(this));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		addFactionTargets();
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) return;
		int act = action();
		if (act != lastAction) {
			if (act == ACT_BASH) bashState.start(this.tickCount);
			if (act == ACT_CHARGE_WINDUP) chargeStartState.start(this.tickCount);
			if (act != ACT_BASH) bashState.stop();
			if (act != ACT_CHARGE_WINDUP) chargeStartState.stop();
			lastAction = act;
		}
		boolean moving = this.walkAnimation.isMoving();
		chargeState.animateWhen(act == ACT_CHARGE, this.tickCount);
		stunState.animateWhen(act == ACT_STUN, this.tickCount);
		blockState.animateWhen(act == ACT_BLOCK && !moving, this.tickCount);
		idleState.animateWhen(act == ACT_NONE && !moving, this.tickCount);
	}

	@Override
	protected boolean guardsArchers() {
		return true;
	}

	public boolean shieldRaised() {
		return shieldUp > 0 && shieldDisabled <= 0;
	}

	/** Удар эфесом: сбивает рывок (замах и разбег) и удар щитом до попадания — тупит, как после промаха. */
	@Override
	public boolean interrupt(LivingEntity by, int stagger) {
		boolean winding = chargeTicks >= 0 || (bashTicks >= 0 && bashTicks < SquadTuning.BASH_HIT_TICK);
		if (!winding) return false;
		if (chargeTicks >= 0) endCharge(false);
		bashTicks = -1;
		stun = Math.max(stun, stagger);
		this.getNavigation().stop();
		return true;
	}

	@Override
	public boolean windingUp() {
		return (chargeTicks >= 0 && chargeTicks < SquadTuning.CHARGE_WINDUP + SquadTuning.CHARGE_RUN) || (bashTicks >= 0 && bashTicks < SquadTuning.BASH_HIT_TICK);
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
		if (act == ACT_STUN && action() != ACT_STUN) stunSoundAt = tickCount + 6;
		if (act != ACT_STUN) stunSoundAt = -1;
		if (act == ACT_STUN && stunSoundAt >= 0 && tickCount >= stunSoundAt) {
			stunSoundAt = -1;
			playSound(ModSounds.PALE_SHIELD_STUN, 0.40f, 1.0f);
		}
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
		if (bashTicks == SquadTuning.BASH_HIT_TICK - 6) playSound(ModSounds.PALE_SHIELD_BASH_SWING, 0.43f, 1.0f);
		if (bashTicks == SquadTuning.BASH_HIT_TICK) {
			// щит бьёт всех врагов перед собой (дугой)
			boolean any = false;
			for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SquadTuning.BASH_RANGE + 1), this::isEnemy)) {
				if (this.distanceTo(e) > SquadTuning.BASH_RANGE + 0.6 + e.getBbWidth() / 2 || !inFront(e.position(), 0.3)) continue;
				e.hurt(this.damageSources().mobAttack(this), damageVs(e, SquadTuning.BASH_DAMAGE));
				e.knockback(SquadTuning.BASH_KNOCKBACK, this.getX() - e.getX(), this.getZ() - e.getZ());
				any = true;
			}
			if (any) {
				this.playSound(ModSounds.PALE_SHIELD_BASH_HIT, 0.53f, 1.0f);
				lastLanded = this.tickCount;
			}
		}
		if (bashTicks >= SquadTuning.BASH_TICKS) bashTicks = -1;
	}

	// ----- рывок -----
	/** Идёт рывок (замах или бег): сколько тиков и куда. Охотники по нему уворачиваются. */
	public int chargeTicks() {
		return chargeTicks;
	}

	public Vec3 chargeDirection() {
		return chargeDir;
	}

	void startCharge(LivingEntity target) {
		chargeTicks = 0;
		chargeBroken = 0;
		chargeHit.clear();
		chargeCooldown = SquadTuning.CHARGE_COOLDOWN;
		lastLanded = this.tickCount;
		Vec3 d = target.position().subtract(this.position()).multiply(1, 0, 1);
		chargeDir = d.lengthSqr() < 1.0E-4 ? this.getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
		this.getNavigation().stop();
		this.playSound(ModSounds.PALE_SHIELD_CHARGE_PREPARE, 0.58f, 1.0f);
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
		LivingEntity t = this.getTarget();
		if (chargeTicks < SquadTuning.CHARGE_WINDUP) { // замах: стоит, щит поднят, целится
			if (t != null && t.isAlive()) {
				Vec3 d = t.position().subtract(this.position()).multiply(1, 0, 1);
				if (d.lengthSqr() > 1.0E-4) chargeDir = d.normalize();
			}
			return;
		}
		if (chargeTicks == SquadTuning.CHARGE_WINDUP) playSound(ModSounds.PALE_SHIELD_CHARGE_START, 0.47f, 1.0f);
		if (t != null && t.isAlive()) steerToward(t);
		this.setDeltaMovement(chargeDir.x * SquadTuning.CHARGE_SPEED, this.getDeltaMovement().y, chargeDir.z * SquadTuning.CHARGE_SPEED);
		this.hasImpulse = true;

		// таранит всех врагов на пути; попал в цель — рывок окончен
		boolean hitTarget = false;
		for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.4), this::isEnemy)) {
			if (!chargeHit.add(e.getId())) continue;
			if (com.tarkrakov.tearsofink.weapon.shield.ShieldStance.inStance(e)) { // Скала: упёрся в два щита — отскок и оглушение
				e.hurt(this.damageSources().mobAttack(this), damageVs(e, SquadTuning.CHARGE_DAMAGE)); // стойка примет удар (Душа, заряд)
				chargeImpactSound();
				endCharge(true);
				this.setDeltaMovement(-chargeDir.x * 0.6, 0.2, -chargeDir.z * 0.6);
				this.hasImpulse = true;
				return;
			}
			e.hurt(this.damageSources().mobAttack(this), damageVs(e, SquadTuning.CHARGE_DAMAGE));
			e.knockback(SquadTuning.CHARGE_KNOCKBACK, -chargeDir.x, -chargeDir.z);
			lastLanded = this.tickCount;
			chargeImpactSound();
			if (e == t) hitTarget = true;
		}
		if (hitTarget) {
			endCharge(false);
			return;
		}
		if (this.horizontalCollision) {
			int broken = chargeBroken < SquadTuning.CHARGE_BREAK_MAX ? breakSoftBlocks(SquadTuning.CHARGE_BREAK_MAX - chargeBroken) : 0;
			if (broken > 0) { // проломил — бежит дальше
				playSound(ModSounds.PALE_SHIELD_CHARGE_BREAK_SOFT, 0.47f, 1.0f);
				chargeBroken += broken;
				return;
			}
			playSound(ModSounds.PALE_SHIELD_CHARGE_WALL, 0.55f, 1.0f);
			endCharge(true); // твёрдое (или лимит) — оглушён
			return;
		}
		if (chargeTicks >= SquadTuning.CHARGE_WINDUP + SquadTuning.CHARGE_RUN) endCharge(true);
	}

	/** Чуть доворачивает бег за игроком (не больше CHARGE_TURN_DEG за тик). */
	private void steerToward(LivingEntity t) {
		Vec3 d = t.position().subtract(this.position()).multiply(1, 0, 1);
		if (d.lengthSqr() < 1.0E-4) return;
		double cur = Math.atan2(chargeDir.z, chargeDir.x), want = Math.atan2(d.z, d.x);
		double diff = Math.atan2(Math.sin(want - cur), Math.cos(want - cur));
		double max = Math.toRadians(SquadTuning.CHARGE_TURN_DEG);
		if (Math.abs(diff) > Math.PI / 2) return; // игрок уже сбоку/позади — не крутится волчком
		double a = cur + Math.max(-max, Math.min(max, diff));
		chargeDir = new Vec3(Math.cos(a), 0, Math.sin(a));
	}

	/** Можно ли пробежать по прямой до цели: нет твёрдых стен (мягкие он снесёт), нет обрывов, цель примерно на одной высоте. */
	boolean clearChargePath(LivingEntity t) {
		if (Math.abs(t.getY() - this.getY()) > 1.5) return false;
		Vec3 from = this.position(), to = t.position();
		Vec3 d = to.subtract(from).multiply(1, 0, 1);
		double len = d.length();
		if (len < 1.0E-3) return true;
		Vec3 dir = d.scale(1 / len), side = new Vec3(-dir.z, 0, dir.x);
		boolean griefing = this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
		for (double s = 0.5; s < len; s += 0.5) {
			for (int w = -1; w <= 1; w++) {
				Vec3 p = from.add(dir.scale(s)).add(side.scale(w * 0.4));
				BlockPos base = BlockPos.containing(p.x, from.y + 0.1, p.z);
				for (int h = 0; h <= 2; h++) {
					BlockPos pos = base.above(h);
					BlockState st = this.level().getBlockState(pos);
					if (st.getCollisionShape(this.level(), pos).isEmpty()) continue;
					if (h == 0 && st.getCollisionShape(this.level(), pos).max(net.minecraft.core.Direction.Axis.Y) <= 0.5) continue; // плита/тропа — перешагнёт
					if (!griefing || !isSoft(st, pos)) return false;
				}
				if (w == 0) { // пол: не дальше одного блока вниз
					BlockPos below = base.below();
					if (this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty()
							&& this.level().getBlockState(below.below()).getCollisionShape(this.level(), below.below()).isEmpty()) return false;
				}
			}
		}
		return true;
	}

	private void endCharge(boolean missed) {
		chargeTicks = -1;
		this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1, 0.2));
		if (missed) stun = SquadTuning.CHARGE_STUN; // промахнулся — тупит
	}

	/** Сносит мягкие блоки перед собой (если в мире разрешено разрушение мобами). */
	private int breakSoftBlocks(int limit) {
		if (!this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return 0;
		int broken = 0;
		Vec3 front = this.position().add(chargeDir.scale(this.getBbWidth() / 2 + 0.6));
		Vec3 side = new Vec3(-chargeDir.z, 0, chargeDir.x);
		for (int s = -1; s <= 1; s++) {
			for (int h = 0; h <= 2; h++) {
				BlockPos pos = BlockPos.containing(front.add(side.scale(s * 0.6)).add(0, h + 0.1, 0));
				BlockState state = this.level().getBlockState(pos);
				if (broken < limit && isSoft(state, pos) && this.level().destroyBlock(pos, true, this)) broken++;
			}
		}
		return broken;
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
				blockSound(ModSounds.PALE_SHIELD_DEFLECT, 0.43f);
				return false; // стрелы спереди отскакивают
			}
			if (source.getEntity() instanceof LivingEntity attacker && attacker.getMainHandItem().getItem() instanceof AxeItem) {
				shieldDisabled = SquadTuning.SHIELD_AXE_DISABLE; // топор выбил щит
				shieldUp = 0;
				shieldRest = SquadTuning.SHIELD_REST;
				this.playSound(ModSounds.PALE_SHIELD_DISABLED, 0.53f, 1.0f);
			} else {
				blockSound(ModSounds.PALE_SHIELD_BLOCK, 0.48f);
				amount *= SquadTuning.SHIELD_PASS;
			}
		}
		return super.hurt(source, amount);
	}

	@Override
	protected SoundEvent soldierStepSound() { return ModSounds.PALE_SHIELD_STEP; }

	private void chargeImpactSound() {
		if (tickCount - lastChargeImpactSound < 5) return;
		lastChargeImpactSound = tickCount;
		playSound(ModSounds.PALE_SHIELD_CHARGE_HIT, 0.60f, 1.0f);
	}

	private void blockSound(SoundEvent sound, float volume) {
		if (tickCount - lastBlockSound < 4) return;
		lastBlockSound = tickCount;
		playSound(sound, volume, 1.0f);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		if (chargeTicks >= SquadTuning.CHARGE_WINDUP) {
			if (!level().isClientSide && tickCount - lastChargeStepSound >= 7) {
				lastChargeStepSound = tickCount;
				playSound(ModSounds.PALE_SHIELD_CHARGE_STEP, 0.43f, 1.0f);
			}
		} else super.playStepSound(pos, state);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.PALE_SHIELD_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.PALE_SHIELD_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.PALE_SHIELD_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 1.0f;
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
		public void start() {
			lastLanded = tickCount;
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity t = getTarget();
			if (t == null) return;
			if (chargeTicks < 0) getLookControl().setLookAt(t, 30f, 30f); // во время удара/тупняка всё равно доворачивается к игроку
			if (busy()) {
				getNavigation().stop();
				return;
			}
			double dist = distanceTo(t);

			// давно не попадал по врагу (бегает вокруг, кайтит) — рывок, даже если не откатился
			boolean noHits = tickCount - lastLanded >= SquadTuning.CHARGE_IF_NO_HIT && dist >= SquadTuning.CHARGE_NO_HIT_MIN;
			if (dist <= SquadTuning.CHARGE_MAX && onGround() && hasLineOfSight(t) && clearChargePath(t)
					&& (noHits || dist >= SquadTuning.CHARGE_MIN && chargeCooldown <= 0 && getRandom().nextInt(10) == 0)) {
				startCharge(t);
				return;
			}
			if (shieldUp <= 0 && shieldRest <= 0 && shieldDisabled <= 0 && dist < SquadTuning.SHIELD_RANGE) {
				shieldUp = SquadTuning.SHIELD_MAX;
				playSound(ModSounds.PALE_SHIELD_RAISE, 0.38f, 1.0f);
			}
			if (shieldRaised() && dist <= SquadTuning.BASH_RANGE && bashCooldown <= 0) {
				startBash();
				return;
			}
			if (dist > 1.8) {
				if (--repath <= 0) {
					repath = 5;
					getNavigation().moveTo(t, shieldRaised() ? SquadTuning.SHIELD_WALK : 1.0);
				}
			} else {
				getNavigation().stop();
			}
		}
	}
}
