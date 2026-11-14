package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.weapon.InkWaveEntity;
import com.tarkrakov.tearsofink.weapon.WeaponAbilities;
import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import com.tarkrakov.tearsofink.weapon.combat.Combat;
import com.tarkrakov.tearsofink.weapon.combat.Combos;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Охотник — человек со скелетом игрока. Бьёт теми же ударами архетипа, что и игрок (связки, парные связки,
 * приёмы на R, парные приёмы на ПКМ — Гильотина и Чернильный Росчерк), плюс чернильный рывок. Приёмы — без Чернил,
 * только перезарядка (ранг её ускоряет). Урон его приёмов проходит только по его врагу.
 * Стиль боя — по типу: Кровавый давит и чередует удары с приёмами, Чернильный дерётся без оглядки,
 * охотник Шпиля бьёт и отходит, играет от приёмов.
 * Живёт только пока идёт Охота: в мир не сохраняется.
 */
public class HunterEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	/** Какие навески gear_* видны (биты по местам навесок). */
	private static final EntityDataAccessor<Integer> GEAR = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);

	@Nullable
	private LivingEntity foe;
	private boolean engaged;
	private float baseDamage = 5;
	private long lastHurt = -100;
	private double lastFoeDist = -1;

	// удары связки
	private int step;
	private long busyUntil, lastSwing;
	private int swingsSinceSkill, swingsWanted = 2;

	// перезарядки (время, когда снова можно)
	private long skillReady, offSkillReady, techReady, dashReady;

	// рывок
	private int dashTicks;
	private double dashX, dashZ;

	// отход (Шпиль): до какого времени держится на дистанции
	private long keepAwayUntil;

	/** Отложенные действия: момент удара, удары Гильотины и т. п. */
	private record Timed(long at, Runnable action) {}

	private final List<Timed> timed = new ArrayList<>();

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

	/** Собрать охотника: тип × ранг × оружие (одно или два). */
	public void setup(HunterType type, int rank, ItemStack main, ItemStack off) {
		entityData.set(TYPE, type.ordinal());
		entityData.set(RANK, rank);
		entityData.set(GEAR, rank == 0 ? 0 : rank == 1 ? random.nextInt(1 << 16) : -1);
		float hp = HuntTuning.HUNTER_HP[rank] * (1 + (random.nextFloat() * 2 - 1) * HuntTuning.HUNTER_HP_SPREAD);
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
		setHealth(hp);
		baseDamage = HuntTuning.HUNTER_DAMAGE[rank] * (1 + (random.nextFloat() * 2 - 1) * HuntTuning.HUNTER_DAMAGE_SPREAD);
		setItemSlot(EquipmentSlot.MAINHAND, main);
		setItemSlot(EquipmentSlot.OFFHAND, off);
		setDropChance(EquipmentSlot.MAINHAND, 0);
		setDropChance(EquipmentSlot.OFFHAND, 0);
		setCustomName(Component.translatable("hunt.tears-of-ink.hunter_name",
				Component.translatable("hunt.tears-of-ink.type." + type.id),
				Component.translatable("hunt.tears-of-ink.rank." + rank)));
		setCustomNameVisible(true);
	}

	public float power() {
		return getMaxHealth() * baseDamage * (getOffhandItem().isEmpty() ? 1f : 1.2f);
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
		boolean hit = super.hurt(source, amount);
		if (hit) lastHurt = level().getGameTime();
		return hit;
	}

	// ===== Что в руках =====

	@Nullable
	private Archetype mainArch() {
		return Archetype.of(getMainHandItem());
	}

	@Nullable
	private Archetype offArch() {
		return Archetype.of(getOffhandItem());
	}

	/** Пара одинаковых клинков — парные связки и приём на ПКМ. */
	private boolean dual() {
		Archetype m = mainArch();
		return m != null && m == offArch();
	}

	private float cd(int ticks) {
		float m = HuntTuning.RANK_COOLDOWN[rank()];
		if (hunterType() == HunterType.SPIRE) m *= HuntTuning.SPIRE_COOLDOWN;
		return ticks * m;
	}

	// ===== Ход =====

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!(level() instanceof ServerLevel level)) return;
		long now = level.getGameTime();
		runTimed(now);
		tickDash(level);

		LivingEntity f = foe;
		if (f == null || !f.isAlive() || f.level() != level) {
			getNavigation().stop();
			return;
		}
		getLookControl().setLookAt(f, 30, 30);
		double dist = distanceTo(f);
		boolean fleeing = lastFoeDist >= 0 && dist > lastFoeDist + 0.05;
		lastFoeDist = dist;

		if (!engaged) { // ждёт игроков в стороне
			if (dist > HuntTuning.WAIT_MAX) getNavigation().moveTo(f, 0.9);
			else if (dist < HuntTuning.WAIT_MIN) moveAway(f, 4, 1.0);
			else getNavigation().stop();
			return;
		}
		if (dashTicks > 0 || now < busyUntil) return;

		switch (hunterType()) {
			case BLOOD -> blood(level, f, dist, fleeing, now);
			case INK -> ink(level, f, dist, now);
			default -> spire(level, f, dist, now);
		}
	}

	/** Кровавый: давит; несколько ударов — приём; рывок (не чаще раза в 5 с) — отскочить от удара или догнать. */
	private void blood(ServerLevel level, LivingEntity f, double dist, boolean fleeing, long now) {
		if (now >= dashReady) {
			if (now - lastHurt < 8 && dist < 3) { // ударили вблизи — отскок
				dash(level, f.position(), -1);
				return;
			}
			if (fleeing && dist > HuntTuning.BLOOD_CHASE) { // убегает — догнать
				dash(level, f.position(), 1);
				return;
			}
		}
		if (swingsSinceSkill >= swingsWanted && trySkill(level, f, dist, now)) return;
		melee(level, f, dist, now, 14);
	}

	/** Чернильный: не отступает, бьёт без пауз, приёмы — сразу по откату, рывок — только вперёд. */
	private void ink(ServerLevel level, LivingEntity f, double dist, long now) {
		if (now >= dashReady && dist > HuntTuning.INK_DASH_FROM) {
			dash(level, f.position(), 1);
			return;
		}
		if (trySkill(level, f, dist, now)) return;
		melee(level, f, dist, now, 2);
	}

	/** Шпиль: 1–2 удара, отход и приём издалека; рывок — порвать дистанцию и сразу приём. Иногда подбивает врага. */
	private void spire(ServerLevel level, LivingEntity f, double dist, long now) {
		boolean ranged = mainArch() == Archetype.SCIMITAR; // Волна бьёт издалека
		if (now < keepAwayUntil) {
			if (dist < HuntTuning.SPIRE_KEEP_MIN) {
				if (now >= dashReady && dist < 3) { // наседают — рывок назад и сразу приём
					dash(level, f.position(), -1);
					timed.add(new Timed(now + 7, () -> {
						LivingEntity ff = foe;
						if (ff != null && ff.isAlive()) trySkill(level, ff, distanceTo(ff), level.getGameTime());
					}));
					return;
				}
				moveAway(f, 4, 1.15);
				return;
			}
			getNavigation().stop();
			// на дистанции: Волна по врагу, а с двуручником — ждать, пока подойдёт, и встретить Рассечением
			if (ranged && trySkill(level, f, dist, now)) return;
			if (!ranged && dist < 3.5 && trySkill(level, f, dist, now)) return;
			if (dist > HuntTuning.SPIRE_KEEP_MAX + 2) keepAwayUntil = 0; // убежал — снова в ближний
			return;
		}
		if (swingsSinceSkill >= swingsWanted) { // наработал ударов — отход
			swingsSinceSkill = 0;
			swingsWanted = 1 + random.nextInt(2);
			keepAwayUntil = now + 60 + random.nextInt(40);
			return;
		}
		if (dist > 4 && trySkill(level, f, dist, now)) return;
		melee(level, f, dist, now, 10);
	}

	private void moveAway(LivingEntity f, double by, double speed) {
		Vec3 away = position().subtract(f.position()).multiply(1, 0, 1).normalize();
		Vec3 to = position().add(away.scale(by));
		getNavigation().moveTo(to.x, to.y, to.z, speed);
	}

	// ===== Ближний бой =====

	/** Удар связки (или парной связки). pause — передышка после последнего удара. */
	private void melee(ServerLevel level, LivingEntity f, double dist, long now, int pause) {
		Archetype a = mainArch();
		Combos.Combo combo = a == null ? null : Combos.get(a);
		List<Combos.Step> steps = combo == null ? List.of() : dual() && !combo.dualSteps().isEmpty() ? combo.dualSteps() : combo.steps();
		float reach = steps.isEmpty() ? 3 : steps.get(Math.min(step, steps.size() - 1)).reach();
		if (dist > reach - 0.4 + f.getBbWidth() / 2) {
			getNavigation().moveTo(f, 1.15);
			return;
		}
		getNavigation().stop();
		if (steps.isEmpty()) return;
		if (now - lastSwing > combo.scaled(combo.resetTicks()) + 20) step = 0;
		Combos.Step s = steps.get(step % steps.size());
		int duration = combo.scaled(s.duration());
		lastSwing = now;
		boolean last = step % steps.size() == steps.size() - 1;
		busyUntil = now + duration + (last ? pause : 2);
		step = last ? 0 : step + 1;
		swingsSinceSkill++;
		timed.add(new Timed(now + combo.scaled(s.hit()), () -> strike(s, 1f)));
		anim(a, s.anim(), duration, combo.tempo());
		if (s.offAnim() != null) anim(a, "off:" + s.offAnim(), duration, combo.tempo());
	}

	private void strike(Combos.Step s, float mult) {
		LivingEntity f = foe;
		if (f == null || !f.isAlive()) return;
		double dist = distanceTo(f) - f.getBbWidth() / 2;
		if (dist > s.reach() + 0.5) return;
		Vec3 to = flatTo(f);
		Vec3 look = getLookAngle().multiply(1, 0, 1).normalize();
		double angle = Math.toDegrees(Math.acos(Mth.clamp(look.dot(to), -1, 1)));
		if (angle > s.arc() / 2 + 25) return;
		if (f.hurt(damageSources().mobAttack(this), baseDamage * s.damage() * mult)) {
			if (s.knockback() > 0) f.knockback(s.knockback(), -to.x, -to.z);
			// охотник Шпиля иногда подбивает врага — короткое окно, чтобы отойти
			if (hunterType() == HunterType.SPIRE && random.nextFloat() < HuntTuning.SPIRE_LAUNCH_CHANCE) {
				f.push(0, HuntTuning.SPIRE_LAUNCH, 0);
				f.hurtMarked = true;
			}
		}
	}

	// ===== Приёмы =====

	/** Приём по готовности: парный на ПКМ, приём правого клинка, двуручник в левой (на 60%). */
	private boolean trySkill(ServerLevel level, LivingEntity f, double dist, long now) {
		Archetype m = mainArch(), o = offArch();
		if (m == null) return false;
		if (dual() && now >= techReady) {
			if (m == Archetype.EXECUTIONER && dist < 4) return guillotine(level, now);
			if (m == Archetype.SCIMITAR && dist < 16) return flourish(level, f, now);
		}
		if (now >= skillReady) {
			if (m == Archetype.SCIMITAR && dist < 16) return wave(level, f, now, false);
			if (m == Archetype.EXECUTIONER && dist < 4) return cleave(level, now, false, 1f);
		}
		// клинок в правой, двуручник в левой — приём левой рукой, слабее
		if (!dual() && o == Archetype.EXECUTIONER && m == Archetype.SCIMITAR && now >= offSkillReady && dist < 4) {
			return cleave(level, now, true, WeaponAbilities.OFFHAND_TWO_HANDED_POWER);
		}
		return false;
	}

	private void skillDone(long now, int busy) {
		busyUntil = now + busy;
		swingsSinceSkill = 0;
		swingsWanted = 2 + random.nextInt(2);
		getNavigation().stop();
	}

	private boolean wave(ServerLevel level, LivingEntity f, long now, boolean offhand) {
		Combos.Combo combo = Combos.get(Archetype.SCIMITAR);
		Vec3 dir = flatTo(f);
		InkWaveEntity w = new InkWaveEntity(level, this, dir, false, HuntTuning.WAVE_DAMAGE * skillScale(), 0.4).onlyTarget(f);
		level.addFreshEntity(w);
		anim(Archetype.SCIMITAR, (offhand ? "off:" : "") + combo.abilityAnim(), combo.scaled(combo.abilityTicks()), combo.tempo());
		level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.6f);
		level.playSound(null, blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.HOSTILE, 0.8f, 0.6f);
		skillReady = now + (long) cd(WeaponAbilities.WAVE.cooldownTicks());
		skillDone(now, combo.scaled(combo.abilityTicks()));
		return true;
	}

	private boolean flourish(ServerLevel level, LivingEntity f, long now) {
		Combos.Combo combo = Combos.get(Archetype.SCIMITAR);
		Vec3 dir = flatTo(f);
		Vec3 side = new Vec3(-dir.z, 0, dir.x);
		for (int i = 0; i < 2; i++) {
			InkWaveEntity w = new InkWaveEntity(level, this, dir, false, HuntTuning.WAVE_DAMAGE * skillScale(), 0.4).onlyTarget(f);
			double up = i == 0 ? 0.55 : -0.25, off = i == 0 ? 0.25 : -0.25;
			w.setPos(w.getX() + side.x * off, w.getY() + up, w.getZ() + side.z * off);
			level.addFreshEntity(w);
		}
		anim(Archetype.SCIMITAR, "flourish_high", 14, combo.tempo());
		anim(Archetype.SCIMITAR, "off:flourish_low", 14, combo.tempo());
		level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.7f);
		level.playSound(null, blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.HOSTILE, 1f, 0.5f);
		techReady = now + (long) cd(WeaponAbilities.FLOURISH.cooldownTicks());
		skillDone(now, 16);
		return true;
	}

	/** Рассечение: замах, удар сверху по площадке перед собой (только по врагу). */
	private boolean cleave(ServerLevel level, long now, boolean offhand, float mult) {
		Combos.Combo combo = Combos.get(Archetype.EXECUTIONER);
		int windup = WeaponAbilities.CleaveAbility.WINDUP_TICKS;
		anim(Archetype.EXECUTIONER, (offhand ? "off:" : "") + combo.abilityAnim(), combo.scaled(combo.abilityTicks()), combo.tempo());
		level.playSound(null, blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.HOSTILE, 0.6f, 0.6f);
		for (int i = 1; i < windup; i += 2) {
			timed.add(new Timed(now + i, () -> level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 2.2, getZ(), 3, 0.2, 0.1, 0.2, 0)));
		}
		timed.add(new Timed(now + windup, () -> {
			Vec3 dir = getLookAngle().multiply(1, 0, 1).normalize();
			Vec3 c = position().add(dir.scale(WeaponAbilities.CleaveAbility.REACH));
			AABB area = new AABB(c.x - 1.5, c.y - 0.5, c.z - 1.5, c.x + 1.5, c.y + 2.0, c.z + 1.5);
			LivingEntity f = foe;
			if (f != null && f.isAlive() && f.getBoundingBox().intersects(area)) {
				f.hurt(damageSources().mobAttack(this), HuntTuning.CLEAVE_DAMAGE * skillScale() * mult);
				f.knockback(WeaponAbilities.CleaveAbility.KNOCKBACK, -dir.x, -dir.z);
			}
			level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 0.3, c.z, 60, 1.2, 0.2, 1.2, 0.08);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y + 0.8, c.z, 3, 0.8, 0.1, 0.8, 0);
			level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2f, 0.5f);
			level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.4f, 1.6f);
		}));
		if (offhand) offSkillReady = now + (long) cd(WeaponAbilities.CLEAVE.cooldownTicks());
		else skillReady = now + (long) cd(WeaponAbilities.CLEAVE.cooldownTicks());
		skillDone(now, Math.max(windup + 4, combo.scaled(combo.abilityTicks())));
		return true;
	}

	/** Гильотина: клинки рубят по очереди, потом казнь сверху обоими. */
	private boolean guillotine(ServerLevel level, long now) {
		Combos.Combo combo = Combos.get(Archetype.EXECUTIONER);
		int every = Combat.chopInterval();
		int chops = HuntTuning.GUILLOTINE_CHOPS_MIN + random.nextInt(HuntTuning.GUILLOTINE_CHOPS_MAX - HuntTuning.GUILLOTINE_CHOPS_MIN + 1);
		for (int n = 0; n < chops; n++) {
			boolean left = n % 2 == 1;
			long at = now + (long) n * every;
			timed.add(new Timed(at, () -> anim(Archetype.EXECUTIONER, (left ? "off:" : "") + Combat.GUILLOTINE_CHOP.anim(), every, combo.tempo())));
			timed.add(new Timed(at + combo.scaled(Combat.GUILLOTINE_CHOP.hit()), () -> strike(Combat.GUILLOTINE_CHOP, 1f)));
		}
		long end = now + (long) chops * every;
		int endDur = combo.scaled(Combat.GUILLOTINE_END.duration());
		timed.add(new Timed(end, () -> {
			anim(Archetype.EXECUTIONER, Combat.GUILLOTINE_END.anim(), endDur, combo.tempo());
			anim(Archetype.EXECUTIONER, "off:" + Combat.GUILLOTINE_END.anim(), endDur, combo.tempo());
		}));
		timed.add(new Timed(end + combo.scaled(Combat.GUILLOTINE_END.hit()), () -> strike(Combat.GUILLOTINE_END, 1f)));
		techReady = now + (long) cd(WeaponAbilities.GUILLOTINE.cooldownTicks());
		skillDone(now, (int) (end - now) + endDur);
		return true;
	}

	private float skillScale() {
		return HuntTuning.SKILL_DAMAGE[rank()];
	}

	// ===== Чернильный рывок =====

	/** Рывок к точке (dir = 1) или от неё (dir = -1); чуть вбок — чтобы не по прямой. */
	private void dash(ServerLevel level, Vec3 point, int dir) {
		Vec3 v = point.subtract(position()).multiply(1, 0, 1).normalize().scale(dir);
		if (dir < 0) { // отскок — с уходом вбок
			Vec3 side = new Vec3(-v.z, 0, v.x).scale(random.nextBoolean() ? 0.6 : -0.6);
			v = v.add(side).normalize();
		}
		dashX = v.x;
		dashZ = v.z;
		dashTicks = HuntTuning.DASH_TICKS;
		dashReady = level.getGameTime() + HuntTuning.DASH_COOLDOWN[rank()];
		getNavigation().stop();
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SQUID_SQUIRT, SoundSource.HOSTILE, 0.7f, 1.3f);
		for (int i = 0; i < 32; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			level.sendParticles(ModParticles.INK_SPARK, getX(), getY() + 0.9, getZ(), 0,
					Math.cos(a) * 0.15, 0.05 + random.nextDouble() * 0.1, Math.sin(a) * 0.15, 1.0);
		}
	}

	private void tickDash(ServerLevel level) {
		if (dashTicks <= 0) return;
		dashTicks--;
		if (dashTicks > 0) {
			setDeltaMovement(dashX * HuntTuning.DASH_SPEED, Math.min(getDeltaMovement().y, 0.1), dashZ * HuntTuning.DASH_SPEED);
			level.sendParticles(ModParticles.INK_SPARK, getX(), getY() + 0.7, getZ(), 8, 0.25, 0.4, 0.25, 0.02);
		} else {
			Vec3 v = getDeltaMovement();
			setDeltaMovement(v.x * 0.3, v.y, v.z * 0.3);
		}
		hurtMarked = true;
	}

	// ===== Служебное =====

	private void runTimed(long now) {
		if (timed.isEmpty()) return;
		List<Timed> due = new ArrayList<>();
		for (Iterator<Timed> it = timed.iterator(); it.hasNext(); ) {
			Timed t = it.next();
			if (t.at() <= now) {
				due.add(t);
				it.remove();
			}
		}
		for (Timed t : due) t.action().run();
	}

	private Vec3 flatTo(LivingEntity f) {
		Vec3 d = f.position().subtract(position()).multiply(1, 0, 1);
		return d.lengthSqr() < 1.0e-4 ? getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
	}

	private void anim(@Nullable Archetype a, String anim, int ticks, float tempo) {
		if (a == null) return;
		swing(anim.startsWith("off:") ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, false);
		Combat.Anim msg = new Combat.Anim(getId(), a.id, anim, ticks, -1f, tempo);
		for (ServerPlayer p : PlayerLookup.tracking(this)) ServerPlayNetworking.send(p, msg);
	}
}
