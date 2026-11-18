package com.tarkrakov.tearsofink.entity.patrol;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Солдат бледного патруля (копейщик, щитовик, арбалетчик). Общее: отряд (кто свои), лидер,
 * отрезок дороги для патруля, общая цель (ударил одного — идут все), синхронное «действие» для анимаций.
 */
public abstract class PaleSoldier extends Monster implements com.tarkrakov.tearsofink.entity.faction.FactionMember {
	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(PaleSoldier.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> AGGRESSIVE = SynchedEntityData.defineId(PaleSoldier.class, EntityDataSerializers.BOOLEAN);

	/** Действия для анимаций (у каждого свои значения). */
	public static final int ACT_NONE = 0;

	@Nullable UUID squad;
	@Nullable UUID leader;
	public int patrolRoad = -1;
	/** Сила солдата (по поясу глубины): множитель здоровья и урона. */
	private float power = 1f;
	public double patrolFrom, patrolTo;
	private int alertCooldown;

	protected PaleSoldier(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, ACT_NONE);
		builder.define(AGGRESSIVE, false);
	}

	public int action() {
		return this.entityData.get(ACTION);
	}

	protected void setAction(int action) {
		this.entityData.set(ACTION, action);
	}

	public boolean isAggressiveSynced() {
		return this.entityData.get(AGGRESSIVE);
	}

	// ===== Отряд =====
	public void joinSquad(UUID squad, @Nullable UUID leader) {
		this.squad = squad;
		this.leader = leader;
	}

	public boolean isLeader() {
		return squad == null || leader == null || leader.equals(this.getUUID());
	}

	@Override
	public com.tarkrakov.tearsofink.entity.faction.Faction faction() {
		return com.tarkrakov.tearsofink.entity.faction.Faction.INK; // бледные патрули служат Шпилю
	}

	/** Свой ли это — по таблице фракций. */
	public boolean isAlly(Entity other) {
		return com.tarkrakov.tearsofink.entity.faction.Factions.allied(this, other);
	}

	// ===== Бой с несколькими врагами =====
	/** Враг ли это для солдата: текущая цель, враг по таблице фракций или тот, кто нападает на него/своих. */
	public boolean isEnemy(LivingEntity e) {
		if (e == this || !e.isAlive() || isAlly(e)) return false;
		if (e == this.getTarget()) return true;
		if (e instanceof net.minecraft.world.entity.player.Player p) return !p.isCreative() && !p.isSpectator()
				&& com.tarkrakov.tearsofink.entity.faction.Factions.wantsToAttack(this, e);
		if (e instanceof net.minecraft.world.entity.Mob m && m.getTarget() != null && isAlly(m.getTarget()) || e instanceof net.minecraft.world.entity.Mob m2 && m2.getTarget() == this) return true;
		return com.tarkrakov.tearsofink.entity.faction.Factions.wantsToAttack(this, e);
	}

	/** Урон по цели: по мобам ×VS_MOBS_DAMAGE, по игрокам как есть. */
	public float damageVs(LivingEntity e, double base) {
		return (float) ((e instanceof net.minecraft.world.entity.player.Player ? base : base * SquadTuning.VS_MOBS_DAMAGE) * power);
	}

	/** Телохранитель стрелков: щитовик и копейщик. */
	protected boolean guardsArchers() {
		return false;
	}

	private int guardScan;

	/** Кто-то из мобов кидается на нашего арбалетчика — перехватить (если сейчас не дерёмся с игроком). */
	private void guardArchers() {
		if (++guardScan < 20) return;
		guardScan = 0;
		if (this.getTarget() instanceof net.minecraft.world.entity.player.Player p && p.isAlive()) return;
		LivingEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (PaleSoldier mate : squadmates(24)) {
			if (!(mate instanceof PaleCrossbowmanEntity)) continue;
			for (net.minecraft.world.entity.Mob m : this.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
					mate.getBoundingBox().inflate(SquadTuning.BODYGUARD_RADIUS), m -> m.getTarget() == mate && !isAlly(m))) {
				double d = this.distanceToSqr(m);
				if (d < bestD) { bestD = d; best = m; }
			}
		}
		if (best != null && best != this.getTarget()) this.setTarget(best);
	}

	/** Цели: кто ударил → игроки → враги по таблице фракций (ванильные монстры, другие фракции). */
	protected void addFactionTargets() {
		this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
				net.minecraft.world.entity.player.Player.class, true));
		this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
				LivingEntity.class, 10, true, false,
				e -> !(e instanceof net.minecraft.world.entity.player.Player)
						&& com.tarkrakov.tearsofink.entity.faction.Factions.wantsToAttack(this, e)));
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		return isAlly(other) || super.isAlliedTo(other);
	}

	@Nullable
	public PaleSoldier leaderEntity() {
		if (leader == null || !(this.level() instanceof ServerLevel level)) return null;
		return level.getEntity(leader) instanceof PaleSoldier p && p.isAlive() ? p : null;
	}

	/** Свои в радиусе. */
	public List<PaleSoldier> squadmates(double radius) {
		return this.level().getEntitiesOfClass(PaleSoldier.class, this.getBoundingBox().inflate(radius),
				p -> p != this && p.isAlive() && squad != null && squad.equals(p.squad));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide) return;
		if (guardsArchers()) guardArchers();
		LivingEntity target = this.getTarget();
		this.entityData.set(AGGRESSIVE, target != null && target.isAlive());
		// общая цель: заметил/ударили — поднимаем весь отряд
		if (alertCooldown > 0) alertCooldown--;
		if (target != null && target.isAlive() && alertCooldown <= 0) {
			alertCooldown = 40;
			for (PaleSoldier mate : squadmates(32)) {
				if (mate.getTarget() == null) mate.setTarget(target);
			}
		}
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (target != null && isAlly(target)) return; // своих не бьём
		super.setTarget(target);
	}

	public float power() {
		return power;
	}

	/** Задать силу (при появлении): здоровье и урон ×power. */
	public void setPower(float value) {
		power = value;
		var hp = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
		if (hp != null) {
			hp.setBaseValue(hp.getBaseValue() * value);
			setHealth(getMaxHealth());
		}
		var dmg = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
		if (dmg != null) dmg.setBaseValue(dmg.getBaseValue() * value);
	}

	@Override
	public void die(net.minecraft.world.damagesource.DamageSource source) {
		super.die(source);
		// последний из отряда пал — ближним игрокам передышка от патрулей
		if (!level().isClientSide && squad != null && squadmates(64).isEmpty() && level() instanceof ServerLevel sl) {
			for (var p : sl.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, getBoundingBox().inflate(SquadTuning.RESPITE_RADIUS))) {
				SquadPatrols.grantRespite(p);
			}
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putFloat("Power", power);
		if (squad != null) tag.putUUID("Squad", squad);
		if (leader != null) tag.putUUID("SquadLeader", leader);
		if (patrolRoad >= 0) {
			tag.putInt("PatrolRoad", patrolRoad);
			tag.putDouble("PatrolFrom", patrolFrom);
			tag.putDouble("PatrolTo", patrolTo);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Power")) power = tag.getFloat("Power"); // здоровье/урон уже сохранены в атрибутах
		if (tag.hasUUID("Squad")) squad = tag.getUUID("Squad");
		if (tag.hasUUID("SquadLeader")) leader = tag.getUUID("SquadLeader");
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

	// Shared quiet voice level and positional equipment footsteps.
	protected abstract SoundEvent soldierStepSound();

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		if (!level().isClientSide) playSound(soldierStepSound(), 0.32f, 1.0f);
	}

	@Override
	protected float getSoundVolume() { return 0.40f; }

	@Override
	public int getAmbientSoundInterval() { return 240; }

	@Override
	public boolean removeWhenFarAway(double distance) {
		return distance > 128 * 128; // уходят, только когда игрок очень далеко
	}
}
