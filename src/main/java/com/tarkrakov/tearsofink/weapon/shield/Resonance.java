package com.tarkrakov.tearsofink.weapon.shield;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.effect.ModEffects;
import com.tarkrakov.tearsofink.ink.SoulPenalty;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillInput;
import com.tarkrakov.tearsofink.weapon.AbilityWeapon;
import com.tarkrakov.tearsofink.weapon.WeaponAbilities;
import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import com.tarkrakov.tearsofink.weapon.combat.Combat;
import com.tarkrakov.tearsofink.weapon.combat.Interruptible;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Хлопок и Резонанс (этап 3 двух щитов): R с парой щитов Души — хлопок щит о щит, затем Резонанс по сочетанию щитов.
 * Одна общая перезарядка и цена Душой; заряд стойки Резонанс забирает себе и становится сильнее.
 * Числа — {@link ResonanceTuning}. Одиночные абилки щитов при паре не работают (WeaponAbilities.use).
 */
public final class Resonance {
	/** Щиты по их одиночной абилке. */
	enum Shield { RAM, LIGHT, MERCY, REPEL }

	/** Десять сочетаний: имя — ключ перевода resonance.tears-of-ink.&lt;имя&gt;. */
	enum Kind {
		AVALANCHE("avalanche"),   // Таран + Таран — Лавина
		SUN("sun"),               // Светлый + Светлый — Солнце
		GRACE("grace"),           // Милосердие + Милосердие — Благодать
		SHOCKWAVE("shockwave"),   // Отпор + Отпор — Ударная волна
		ADVANCE("advance"),       // Таран + Отпор — Наступление
		PHALANX("phalanx"),       // Таран + Светлый — Фаланга
		INTERCESSOR("intercessor"), // Таран + Милосердие — Заступник
		HALO("halo"),             // Светлый + Милосердие — Ореол
		FLASH("flash"),           // Светлый + Отпор — Вспышка
		VEIL("veil");             // Милосердие + Отпор — Покров

		final String id;

		Kind(String id) {
			this.id = id;
		}
	}

	public static final ResonanceSkill SKILL = new ResonanceSkill();

	private record Pending(Kind kind, int ticks, float power) {}

	private record Dash(Kind kind, double dx, double dz, int ticksLeft, float power, Set<UUID> touched) {}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static final Map<UUID, Dash> DASH = new HashMap<>();
	/** Фаланга: до какого тика спереди не пробить. */
	private static final Map<UUID, Long> PHALANX_UNTIL = new HashMap<>();
	/** Ореол: сколько тиков ауре осталось и её сила. */
	private static final Map<UUID, float[]> HALO = new HashMap<>();

	private Resonance() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Resonance::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Resonance::allowDamage);
	}

	// ---------------- запуск ----------------

	public static class ResonanceSkill extends Skill {
		ResonanceSkill() {
			super(TearsOfInk.id("shield_resonance"), 0, ResonanceTuning.SOUL_COST, ResonanceTuning.COOLDOWN_TICKS);
		}

		@Override
		public boolean activate(ServerPlayer p, SkillInput input) {
			if (PENDING.containsKey(p.getUUID()) || DASH.containsKey(p.getUUID())) return false;
			Kind kind = kind(p.getMainHandItem(), p.getOffhandItem());
			if (kind == null) return false;
			if (p.isUsingItem()) p.stopUsingItem(); // из стойки — без выброса: заряд уйдёт в Резонанс
			int charge = ShieldStance.takeCharge(p);
			float power = 1 + ResonanceTuning.POWER_PER_CHARGE * charge;
			PENDING.put(p.getUUID(), new Pending(kind, ResonanceTuning.CLAP_TICKS, power));
			Combat.dualAnim(p, Archetype.BASTION, "clap", "clap", ResonanceTuning.CLAP_TICKS + 8);
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.8f, 0.7f);
			return true;
		}
	}

	private static Shield shield(ItemStack stack) {
		if (!(stack.getItem() instanceof AbilityWeapon w)) return null;
		Skill a = w.ability();
		if (a == WeaponAbilities.RAM) return Shield.RAM;
		if (a == WeaponAbilities.RADIANCE) return Shield.LIGHT;
		if (a == WeaponAbilities.MERCY) return Shield.MERCY;
		if (a == WeaponAbilities.REPEL) return Shield.REPEL;
		return null;
	}

	static Kind kind(ItemStack a, ItemStack b) {
		Shield x = shield(a), y = shield(b);
		if (x == null || y == null) return null;
		if (x.ordinal() > y.ordinal()) {
			Shield t = x;
			x = y;
			y = t;
		}
		return switch (x) {
			case RAM -> switch (y) {
				case RAM -> Kind.AVALANCHE;
				case LIGHT -> Kind.PHALANX;
				case MERCY -> Kind.INTERCESSOR;
				case REPEL -> Kind.ADVANCE;
			};
			case LIGHT -> switch (y) {
				case LIGHT -> Kind.SUN;
				case MERCY -> Kind.HALO;
				default -> Kind.FLASH;
			};
			case MERCY -> y == Shield.MERCY ? Kind.GRACE : Kind.VEIL;
			case REPEL -> Kind.SHOCKWAVE;
		};
	}

	// ---------------- ход ----------------

	private static void tick(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Pending> e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			Pending pd = e.getValue();
			if (p == null || !p.isAlive() || !ShieldStance.dual(p)) {
				it.remove();
				continue;
			}
			if (pd.ticks() > 1) {
				e.setValue(new Pending(pd.kind(), pd.ticks() - 1, pd.power()));
				continue;
			}
			it.remove();
			clap(p, pd.kind(), pd.power());
		}
		for (Iterator<Map.Entry<UUID, Dash>> it = DASH.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Dash> e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null || !p.isAlive()) {
				it.remove();
				continue;
			}
			Dash d = dashTick(p, e.getValue());
			if (d == null) it.remove();
			else e.setValue(d);
		}
		for (Iterator<Map.Entry<UUID, float[]>> it = HALO.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, float[]> e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			float[] h = e.getValue();
			if (p == null || !p.isAlive() || !ShieldStance.dual(p) || --h[0] <= 0) {
				it.remove();
				continue;
			}
			haloTick(p, (int) h[0], h[1]);
		}
		long now = server.overworld().getGameTime();
		PHALANX_UNTIL.values().removeIf(t -> t < now);
	}

	/** Хлопок: звон, вспышка — и Резонанс. */
	private static void clap(ServerPlayer p, Kind kind, float power) {
		ServerLevel level = p.serverLevel();
		Vec3 look = flatLook(p);
		Vec3 at = p.position().add(look.scale(0.7)).add(0, 1.2, 0);
		level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 24, 0.1, 0.1, 0.1, 0.18);
		level.sendParticles(ModParticles.TEAR_GLINT, at.x, at.y, at.z, 12, 0.4, 0.4, 0.4, 0.02);
		level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.45f, 1.9f);
		level.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0f, 1.3f);
		level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.9f);
		p.displayClientMessage(Component.translatable("resonance.tears-of-ink." + kind.id).withStyle(ChatFormatting.GOLD), true);
		switch (kind) {
			case AVALANCHE -> startDash(p, kind, ResonanceTuning.AVALANCHE_TICKS, power);
			case ADVANCE, PHALANX, INTERCESSOR -> {
				if (kind == Kind.PHALANX) PHALANX_UNTIL.put(p.getUUID(), level.getGameTime() + ResonanceTuning.DASH_TICKS + ResonanceTuning.PHALANX_AFTER_TICKS);
				startDash(p, kind, ResonanceTuning.DASH_TICKS, power);
			}
			case SUN -> sun(p, power);
			case GRACE -> grace(p, power);
			case SHOCKWAVE -> ring(p, ResonanceTuning.SHOCKWAVE_RADIUS, ResonanceTuning.SHOCKWAVE_DAMAGE * power,
					ResonanceTuning.SHOCKWAVE_KNOCKBACK * power, 0.25);
			case HALO -> {
				HALO.put(p.getUUID(), new float[]{ResonanceTuning.HALO_TICKS, power});
				level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
			}
			case FLASH -> flash(p, power);
			case VEIL -> veil(p, power);
		}
	}

	// ---------------- рывки: Лавина, Наступление, Фаланга, Заступник ----------------

	private static void startDash(ServerPlayer p, Kind kind, int ticks, float power) {
		Vec3 dir = flatLook(p);
		DASH.put(p.getUUID(), new Dash(kind, dir.x, dir.z, ticks, power, new HashSet<>()));
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 0.5f);
	}

	/** Один тик рывка. null — рывок кончился. */
	private static Dash dashTick(ServerPlayer p, Dash d) {
		ServerLevel level = p.serverLevel();
		double speed = d.kind() == Kind.AVALANCHE ? ResonanceTuning.AVALANCHE_SPEED : ResonanceTuning.DASH_SPEED;
		AABB box = p.getBoundingBox().inflate(0.9, 0.3, 0.9).move(d.dx() * 0.7, 0, d.dz() * 0.7);
		boolean bumped = false;
		for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, t -> t != p && t.isAlive() && !(t instanceof ArmorStand))) {
			if (!d.touched().add(t.getUUID())) continue;
			if (enemy(p, t)) {
				switch (d.kind()) {
					case AVALANCHE -> {
						t.hurt(p.damageSources().playerAttack(p), ResonanceTuning.AVALANCHE_DAMAGE * d.power());
						t.knockback(ResonanceTuning.AVALANCHE_KNOCKBACK, -d.dx(), -d.dz());
						stagger(p, t, ResonanceTuning.STAGGER_TICKS);
					}
					case PHALANX -> {
						t.hurt(p.damageSources().playerAttack(p), ResonanceTuning.PHALANX_DAMAGE * d.power());
						t.knockback(ResonanceTuning.PHALANX_KNOCKBACK, -d.dx(), -d.dz());
						blind(t, ResonanceTuning.PHALANX_BLIND_TICKS);
					}
					case ADVANCE -> bumped = true;
					case INTERCESSOR -> t.knockback(0.6, -d.dx(), -d.dz()); // врагов просто отодвигает
					default -> {}
				}
				level.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.7f);
			} else if (d.kind() == Kind.INTERCESSOR) {
				heal(t, ResonanceTuning.INTERCESSOR_HEAL * d.power());
				t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ResonanceTuning.INTERCESSOR_REGEN_TICKS, 0));
				level.sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + t.getBbHeight() + 0.3, t.getZ(), 3, 0.3, 0.1, 0.3, 0);
				level.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
			}
		}
		if (bumped || d.ticksLeft() <= 0) {
			Vec3 v = p.getDeltaMovement();
			p.setDeltaMovement(v.x * 0.3, v.y, v.z * 0.3);
			p.hurtMarked = true;
			if (d.kind() == Kind.ADVANCE) { // хлопок о землю — волна кольцом
				ring(p, ResonanceTuning.ADVANCE_RADIUS, ResonanceTuning.ADVANCE_DAMAGE * d.power(), ResonanceTuning.ADVANCE_KNOCKBACK * d.power(), 0.45);
				level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.4f);
			}
			if (d.kind() == Kind.INTERCESSOR) heal(p, ResonanceTuning.INTERCESSOR_SELF_HEAL * d.power());
			return null;
		}
		p.setDeltaMovement(d.dx() * speed, Math.min(p.getDeltaMovement().y, 0.1), d.dz() * speed);
		p.hurtMarked = true;
		var trail = switch (d.kind()) {
			case INTERCESSOR -> ParticleTypes.HAPPY_VILLAGER;
			case PHALANX -> ParticleTypes.END_ROD;
			default -> ParticleTypes.CLOUD;
		};
		level.sendParticles(trail, p.getX(), p.getY() + 0.6, p.getZ(), 3, 0.3, 0.3, 0.3, 0.01);
		level.sendParticles(ModParticles.TEAR_GLINT, p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.3, 0.4, 0.3, 0);
		return new Dash(d.kind(), d.dx(), d.dz(), d.ticksLeft() - 1, d.power(), d.touched());
	}

	/** Фаланга: спереди не пробить, пока идёт рывок и чуть после. */
	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer p)) return true;
		Long until = PHALANX_UNTIL.get(p.getUUID());
		if (until == null || until < p.serverLevel().getGameTime()) return true;
		Vec3 from = source.getSourcePosition();
		if (from == null) return true;
		Vec3 to = from.subtract(p.position()).multiply(1, 0, 1);
		if (to.lengthSqr() < 1.0e-4) return true;
		if (flatLook(p).dot(to.normalize()) < Math.cos(Math.toRadians(ResonanceTuning.PHALANX_COVER_DEGREES))) return true;
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.3f);
		return false;
	}

	// ---------------- вокруг себя: Солнце, Благодать, Ударная волна, Ореол, Вспышка, Покров ----------------

	/** Солнце: свои — сияние (−40% урона), враги вокруг — ослеплены и теряют цель. */
	private static void sun(ServerPlayer p, float power) {
		ServerLevel level = p.serverLevel();
		double r = ResonanceTuning.SUN_RADIUS;
		int ticks = (int) (ResonanceTuning.SUN_TICKS * power);
		for (LivingEntity t : around(p, r)) {
			if (enemy(p, t)) {
				blind(t, ResonanceTuning.SUN_BLIND_TICKS);
				if (t instanceof Mob m) m.setTarget(null);
			} else if (t instanceof Player || t == p) {
				t.addEffect(new MobEffectInstance(ModEffects.RADIANCE, ticks, 0, false, false, true));
				t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1, false, false, false));
			}
		}
		p.addEffect(new MobEffectInstance(ModEffects.RADIANCE, ticks, 0, false, false, true));
		p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1, false, false, false));
		burst(level, p, r, ParticleTypes.END_ROD, 60);
		level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.6f);
	}

	/** Благодать: сильное лечение своих вокруг, дурные эффекты сняты. */
	private static void grace(ServerPlayer p, float power) {
		ServerLevel level = p.serverLevel();
		List<LivingEntity> all = new ArrayList<>(around(p, ResonanceTuning.GRACE_RADIUS));
		all.add(p);
		for (LivingEntity t : all) {
			if (t != p && enemy(p, t)) continue;
			heal(t, ResonanceTuning.GRACE_HEAL * power);
			for (MobEffectInstance fx : new ArrayList<>(t.getActiveEffects())) {
				if (fx.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) t.removeEffect(fx.getEffect());
			}
			level.sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + t.getBbHeight() + 0.3, t.getZ(), 4, 0.3, 0.2, 0.3, 0);
		}
		burst(level, p, ResonanceTuning.GRACE_RADIUS, ModParticles.TEAR_GLINT, 50);
		level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.8f);
	}

	/** Волна кольцом вокруг (Ударная волна, конец Наступления): урон, отброс от тебя, сбить замах. */
	private static void ring(ServerPlayer p, double r, float damage, double knock, double lift) {
		ServerLevel level = p.serverLevel();
		for (LivingEntity t : around(p, r)) {
			if (!enemy(p, t)) continue;
			if (t.hurt(p.damageSources().playerAttack(p), damage)) t.invulnerableTime = 0;
			Vec3 d = away(p, t);
			t.knockback(knock, -d.x, -d.z);
			t.setDeltaMovement(t.getDeltaMovement().add(0, lift, 0));
			t.hurtMarked = true;
			stagger(p, t, ResonanceTuning.STAGGER_TICKS);
		}
		for (int i = 0; i < 48; i++) {
			double a = i * Math.PI * 2 / 48;
			for (double k = 0.4; k <= 1.0; k += 0.3) {
				level.sendParticles(ParticleTypes.END_ROD, p.getX() + Math.cos(a) * r * k, p.getY() + 0.2, p.getZ() + Math.sin(a) * r * k,
						1, 0, 0.05, 0, 0.02);
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.3, p.getZ(), 1, 0, 0, 0, 0);
		level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2f, 0.5f);
	}

	/** Ореол: раз в секунду свои рядом лечатся и получают сопротивление. */
	private static void haloTick(ServerPlayer p, int left, float power) {
		ServerLevel level = p.serverLevel();
		double r = ResonanceTuning.HALO_RADIUS;
		if (left % 4 == 0) {
			for (int i = 0; i < 8; i++) {
				double a = left * 0.12 + i * Math.PI / 4;
				level.sendParticles(ModParticles.TEAR_GLINT, p.getX() + Math.cos(a) * r, p.getY() + 0.3, p.getZ() + Math.sin(a) * r, 1, 0, 0.1, 0, 0);
			}
			level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 2.3, p.getZ(), 1, 0.3, 0, 0.3, 0);
		}
		if (left % 20 != 0) return;
		List<LivingEntity> all = new ArrayList<>(around(p, r));
		all.add(p);
		for (LivingEntity t : all) {
			if (t != p && enemy(p, t)) continue;
			heal(t, ResonanceTuning.HALO_HEAL_PER_SECOND * power);
			t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, false));
		}
	}

	/** Вспышка: конус перед собой — отброс, ослепление, замедление. */
	private static void flash(ServerPlayer p, float power) {
		ServerLevel level = p.serverLevel();
		Vec3 look = flatLook(p);
		double r = ResonanceTuning.FLASH_RANGE;
		for (LivingEntity t : around(p, r)) {
			if (!enemy(p, t)) continue;
			Vec3 d = t.position().subtract(p.position()).multiply(1, 0, 1);
			if (d.lengthSqr() > 1.0e-4 && Math.toDegrees(Math.acos(Mth.clamp(look.dot(d.normalize()), -1, 1))) > ResonanceTuning.FLASH_HALF_ANGLE) continue;
			if (t.hurt(p.damageSources().playerAttack(p), ResonanceTuning.FLASH_DAMAGE * power)) t.invulnerableTime = 0;
			t.knockback(ResonanceTuning.FLASH_KNOCKBACK * power, -look.x, -look.z);
			blind(t, ResonanceTuning.FLASH_BLIND_TICKS);
			t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ResonanceTuning.FLASH_BLIND_TICKS, 1));
		}
		for (int i = 0; i < 40; i++) {
			double a = (p.getRandom().nextDouble() * 2 - 1) * Math.toRadians(ResonanceTuning.FLASH_HALF_ANGLE);
			Vec3 dir = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), 0, look.x * Math.sin(a) + look.z * Math.cos(a));
			double dist = 0.8 + p.getRandom().nextDouble() * (r - 0.8);
			level.sendParticles(ParticleTypes.END_ROD, p.getX() + dir.x * dist, p.getY() + 0.5 + p.getRandom().nextDouble() * 1.2,
					p.getZ() + dir.z * dist, 1, 0, 0, 0, 0.03);
		}
		Vec3 at = p.position().add(look.scale(2)).add(0, 1.2, 0);
		level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 2, 0.5, 0.3, 0.5, 0);
		level.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1f, 1.2f);
	}

	/** Покров: импульс — свои лечатся, враги отлетают. */
	private static void veil(ServerPlayer p, float power) {
		ServerLevel level = p.serverLevel();
		double r = ResonanceTuning.VEIL_RADIUS;
		heal(p, ResonanceTuning.VEIL_HEAL * power);
		for (LivingEntity t : around(p, r)) {
			if (enemy(p, t)) {
				if (t.hurt(p.damageSources().playerAttack(p), ResonanceTuning.VEIL_DAMAGE * power)) t.invulnerableTime = 0;
				Vec3 d = away(p, t);
				t.knockback(ResonanceTuning.VEIL_KNOCKBACK * power, -d.x, -d.z);
			} else {
				heal(t, ResonanceTuning.VEIL_HEAL * power);
				level.sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + t.getBbHeight() + 0.3, t.getZ(), 2, 0.3, 0.1, 0.3, 0);
			}
		}
		burst(level, p, r, ModParticles.TEAR_GLINT, 40);
		burst(level, p, r, ParticleTypes.END_ROD, 30);
		level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 1.3f);
		level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.8f);
	}

	// ---------------- общее ----------------

	/**
	 * Враг ли: монстры (в том числе солдаты Шпиля) и всякий, кто целится в тебя. Остальные — свои:
	 * игроки, охотник, которому помогаешь, звери, жители.
	 */
	static boolean enemy(ServerPlayer p, LivingEntity t) {
		if (t instanceof Player) return false;
		if (t instanceof Mob m && m.getTarget() == p) return true;
		return t instanceof Enemy;
	}

	private static List<LivingEntity> around(ServerPlayer p, double r) {
		return p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r, 2, r),
				t -> t != p && t.isAlive() && !(t instanceof ArmorStand) && t.distanceToSqr(p) <= (r + 0.5) * (r + 0.5));
	}

	private static void heal(LivingEntity t, float amount) {
		t.heal(t instanceof ServerPlayer sp ? amount * SoulPenalty.multiplier(sp) : amount);
	}

	private static void blind(LivingEntity t, int ticks) {
		t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
		t.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
		if (t instanceof Mob m) m.setTarget(null);
	}

	private static void stagger(ServerPlayer p, LivingEntity t, int ticks) {
		boolean interrupted = t instanceof Interruptible i ? i.interrupt(p, ticks) : t instanceof ServerPlayer o && Combat.interrupt(o, ticks);
		t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, interrupted ? 3 : 1, false, false));
	}

	private static Vec3 away(ServerPlayer p, LivingEntity t) {
		Vec3 d = t.position().subtract(p.position()).multiply(1, 0, 1);
		return d.lengthSqr() < 1.0e-4 ? flatLook(p) : d.normalize();
	}

	private static void burst(ServerLevel level, ServerPlayer p, double r, net.minecraft.core.particles.ParticleOptions particle, int n) {
		for (int i = 0; i < n; i++) {
			double a = p.getRandom().nextDouble() * Math.PI * 2, d = p.getRandom().nextDouble() * r;
			level.sendParticles(particle, p.getX() + Math.cos(a) * d, p.getY() + 0.3 + p.getRandom().nextDouble() * 1.6,
					p.getZ() + Math.sin(a) * d, 1, 0, 0.05, 0, 0.02);
		}
	}

	private static Vec3 flatLook(ServerPlayer p) {
		Vec3 look = p.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}
}
