package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import com.tarkrakov.tearsofink.weapon.combat.Combat;
import com.tarkrakov.tearsofink.weapon.combat.Combos;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Охотник — человек со скелетом игрока. Бьёт теми же ударами архетипа, что и игрок (связки, движения, третье лицо).
 * Тип (Шпиль / Кровь / Чернила) — текстура и линия оружия, ранг — навески gear_* и сила.
 * Живёт только пока идёт Охота: в мир не сохраняется.
 */
public class HunterEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	/** Какие навески gear_* видны (биты по порядку навесок в модели). */
	private static final EntityDataAccessor<Integer> GEAR = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);

	/** Кого бьёт сейчас (цель Охоты, а у Чернильного после победы — игроки). */
	@Nullable
	private LivingEntity foe;
	private boolean engaged;
	private float baseDamage = 5;
	// удары связки
	private int step;
	private long busyUntil, hitAt = -1, lastSwing;
	@Nullable
	private Combos.Step pending;

	public HunterEntity(EntityType<? extends HunterEntity> type, Level level) {
		super(type, level);
		setLeftHanded(false);
		setPersistenceRequired();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40)
				.add(Attributes.MOVEMENT_SPEED, HuntTuning.HUNTER_SPEED)
				.add(Attributes.FOLLOW_RANGE, 48)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
				.add(Attributes.ATTACK_DAMAGE, 5);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TYPE, 0);
		builder.define(RANK, 0);
		builder.define(GEAR, 0);
	}

	public HunterType hunterType() {
		return HunterType.byIndex(entityData.get(TYPE));
	}

	public int rank() {
		return entityData.get(RANK);
	}

	public int gear() {
		return entityData.get(GEAR);
	}

	/** Собрать охотника: тип × ранг × оружие. */
	public void setup(HunterType type, int rank, ItemStack weapon) {
		entityData.set(TYPE, type.ordinal());
		entityData.set(RANK, rank);
		entityData.set(GEAR, rank == 0 ? 0 : rank == 1 ? random.nextInt(1 << 8) : -1);
		float hp = HuntTuning.HUNTER_HP[rank] * (1 + (random.nextFloat() * 2 - 1) * HuntTuning.HUNTER_HP_SPREAD);
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
		setHealth(hp);
		baseDamage = HuntTuning.HUNTER_DAMAGE[rank] * (1 + (random.nextFloat() * 2 - 1) * HuntTuning.HUNTER_DAMAGE_SPREAD);
		setItemSlot(EquipmentSlot.MAINHAND, weapon);
		setDropChance(EquipmentSlot.MAINHAND, 0);
		setCustomName(Component.translatable("hunt.tears-of-ink.hunter_name",
				Component.translatable("hunt.tears-of-ink.type." + type.id),
				Component.translatable("hunt.tears-of-ink.rank." + rank)));
		setCustomNameVisible(true);
	}

	public float power() {
		return getMaxHealth() * baseDamage;
	}

	/** Кого бить. engaged — пора драться (а не ждать в стороне). */
	public void setFoe(@Nullable LivingEntity foe, boolean engaged) {
		this.foe = foe;
		this.engaged = engaged;
	}

	@Nullable
	public LivingEntity foe() {
		return foe;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// охотники Шпиля и Крови не связываются с игроками (их цель — добыча Охоты); Чернильный — уязвим
		if (source.getEntity() instanceof Player && hunterType() != HunterType.INK) return false;
		return super.hurt(source, amount);
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!(level() instanceof ServerLevel level)) return;
		long now = level.getGameTime();
		if (hitAt >= 0 && now >= hitAt) {
			strike();
			hitAt = -1;
		}
		LivingEntity f = foe;
		if (f == null || !f.isAlive() || f.level() != level) {
			getNavigation().stop();
			return;
		}
		getLookControl().setLookAt(f, 30, 30);
		double dist = distanceTo(f);
		if (!engaged) { // ждёт игроков в стороне
			if (dist > HuntTuning.WAIT_MAX) getNavigation().moveTo(f, 0.9);
			else if (dist < HuntTuning.WAIT_MIN) {
				Vec3 away = position().subtract(f.position()).normalize().scale(4).add(position());
				getNavigation().moveTo(away.x, away.y, away.z, 1.0);
			} else getNavigation().stop();
			return;
		}
		Archetype a = Archetype.of(getMainHandItem());
		Combos.Combo combo = a == null ? null : Combos.get(a);
		float reach = combo == null ? 3 : combo.steps().get(Math.min(step, combo.steps().size() - 1)).reach();
		if (dist > reach - 0.4 + f.getBbWidth() / 2) {
			if (now >= busyUntil) getNavigation().moveTo(f, 1.15);
			return;
		}
		getNavigation().stop();
		if (now < busyUntil || combo == null || combo.steps().isEmpty()) return;
		if (now - lastSwing > combo.scaled(combo.resetTicks()) + 20) step = 0;
		Combos.Step s = combo.steps().get(step % combo.steps().size());
		int duration = combo.scaled(s.duration());
		hitAt = now + combo.scaled(s.hit());
		pending = s;
		lastSwing = now;
		boolean last = step % combo.steps().size() == combo.steps().size() - 1;
		busyUntil = now + duration + (last ? 14 : 2);
		step = last ? 0 : step + 1;
		swing(InteractionHand.MAIN_HAND, false);
		Combat.Anim msg = new Combat.Anim(getId(), a.id, s.anim(), duration, -1f, combo.tempo());
		for (ServerPlayer p : PlayerLookup.tracking(this)) ServerPlayNetworking.send(p, msg);
	}

	private void strike() {
		Combos.Step s = pending;
		LivingEntity f = foe;
		if (s == null || f == null || !f.isAlive()) return;
		double dist = distanceTo(f) - f.getBbWidth() / 2;
		if (dist > s.reach() + 0.5) return;
		Vec3 look = getLookAngle().multiply(1, 0, 1).normalize();
		Vec3 to = f.position().subtract(position()).multiply(1, 0, 1).normalize();
		double angle = Math.toDegrees(Math.acos(Mth.clamp(look.dot(to), -1, 1)));
		if (angle > s.arc() / 2 + 25) return;
		if (f.hurt(damageSources().mobAttack(this), baseDamage * s.damage()) && s.knockback() > 0) {
			f.knockback(s.knockback(), -to.x, -to.z);
		}
	}
}
