package com.tarkrakov.tearsofink.weapon.combat;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillInput;
import com.tarkrakov.tearsofink.skill.Skills;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Разящий Кинжал (Шпиль): R — рывок, нож отведён; задел врага — укол, перезарядка рывка вдвое короче.
 * Парой ПКМ — рывок с двумя клинками; задел — вонзает оба и вырывает в стороны.
 * Движения — weapon_anims/dagger.json, связки — weapon_combos/dagger.json.
 */
public final class DaggerMoves {
	// ===== Настройки — крутить здесь =====
	/** Рывок: блоков за тик × тиков (≈ 5 блоков). */
	public static final double DASH_SPEED = 1.0;
	public static final int DASH_TICKS = 5;
	/** Укол рывка (×сила удара оружия) и сколько остаётся от перезарядки при попадании. */
	public static final float STAB = 2.2f, CD_KEEP_ON_HIT = 0.5f;
	/** Парный рывок: вонзание и вырывание (×сила оружия), через сколько тиков вырывает. */
	public static final float PIERCE = 1.6f, RIP = 1.4f;
	public static final int RIP_AFTER = 7;

	public static final RushSkill RUSH = new RushSkill("dagger_rush", 8, 80, false);
	public static final RushSkill DUAL_RUSH = new RushSkill("dagger_dual_rush", 0, 120, true);

	private record Dash(double dx, double dz, int tick, boolean dual, float power) {}

	private static final Map<UUID, Dash> DASHING = new HashMap<>();
	/** Вырвать клинки: игрок → (цель, тик). */
	private static final Map<UUID, Object[]> RIPS = new HashMap<>();

	private DaggerMoves() {}
	public static boolean cancelWindup(ServerPlayer p) {
		RIPS.remove(p.getUUID());
		return DASHING.remove(p.getUUID()) != null;
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(DaggerMoves::tick);
	}

	public static class RushSkill extends Skill {
		final boolean dual;

		RushSkill(String id, int ink, int cd, boolean dual) {
			super(TearsOfInk.id(id), ink, 0, cd);
			this.dual = dual;
		}

		@Override
		public boolean activate(ServerPlayer p, SkillInput input) {
			if (DASHING.containsKey(p.getUUID())) return false;
			Vec3 l = p.getLookAngle();
			Vec3 f = new Vec3(l.x, 0, l.z);
			f = f.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : f.normalize();
			DASHING.put(p.getUUID(), new Dash(f.x, f.z, 0, dual, com.tarkrakov.tearsofink.weapon.WeaponAbilities.power(p)));
			if (dual) Combat.play(p, Archetype.DAGGER, "dual_dash", "dual_dash", DASH_TICKS + 4);
			// одним ножом движение «dash» включает сама абилка R (ability_anim в weapon_combos/dagger.json)
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.6f);
			return true;
		}
	}

	private static void tick(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Dash>> it = DASHING.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			Dash d = e.getValue();
			if (p == null || !p.isAlive()) {
				it.remove();
				continue;
			}
			LivingEntity hit = target(p, d);
			if (hit != null) {
				it.remove();
				stop(p);
				strike(p, d, hit);
				continue;
			}
			int t = d.tick() + 1;
			if (t > DASH_TICKS) {
				it.remove();
				stop(p);
				continue;
			}
			p.setDeltaMovement(d.dx() * DASH_SPEED, Math.min(p.getDeltaMovement().y, 0.1), d.dz() * DASH_SPEED);
			p.hurtMarked = true;
			p.serverLevel().sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 0.4, p.getZ(), 1, 0.15, 0.1, 0.15, 0.01);
			e.setValue(new Dash(d.dx(), d.dz(), t, d.dual(), d.power()));
		}
		for (Iterator<Map.Entry<UUID, Object[]>> it = RIPS.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			LivingEntity t = (LivingEntity) e.getValue()[0];
			if (p == null || !p.isAlive() || p.serverLevel().getGameTime() < (long) e.getValue()[1]) {
				if (p == null || !p.isAlive()) it.remove();
				continue;
			}
			it.remove();
			if (t.isAlive() && t.distanceToSqr(p) < 16) {
				float base = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
				t.invulnerableTime = 0;
				t.hurt(p.damageSources().playerAttack(p), base * RIP * (float) e.getValue()[2]);
				ServerLevel level = p.serverLevel();
				level.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 14, 0.4, 0.3, 0.4, 0.2);
				level.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.8f);
			}
		}
	}

	/** Первый враг прямо по ходу рывка. */
	private static LivingEntity target(ServerPlayer p, Dash d) {
		AABB box = p.getBoundingBox().inflate(0.5, 0.2, 0.5).move(d.dx() * 0.9, 0, d.dz() * 0.9);
		LivingEntity best = null;
		double bd = Double.MAX_VALUE;
		for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
				t -> t != p && t.isAlive() && !t.isAlliedTo(p) && !(t instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID())))) {
			double dist = t.distanceToSqr(p);
			if (dist < bd) {
				bd = dist;
				best = t;
			}
		}
		return best;
	}

	private static void stop(ServerPlayer p) {
		Vec3 v = p.getDeltaMovement();
		p.setDeltaMovement(v.x * 0.2, v.y, v.z * 0.2);
		p.hurtMarked = true;
	}

	/** Попал: укол (одним) или вонзание и вырывание (парой); перезарядка рывка вдвое короче. */
	private static void strike(ServerPlayer p, Dash d, LivingEntity t) {
		float base = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
		ServerLevel level = p.serverLevel();
		t.hurt(p.damageSources().playerAttack(p), base * (d.dual() ? PIERCE : STAB) * d.power());
		level.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
		level.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 1.4f);
		if (d.dual()) {
			Combat.play(p, Archetype.DAGGER, "dual_pierce_rip", "dual_pierce_rip", 16);
			RIPS.put(p.getUUID(), new Object[]{t, level.getGameTime() + RIP_AFTER, d.power()});
			Skills.cutCooldown(p, DUAL_RUSH, CD_KEEP_ON_HIT);
		} else {
			Combat.play(p, Archetype.DAGGER, "dash_stab", null, 10);
			Skills.cutCooldown(p, RUSH, CD_KEEP_ON_HIT);
		}
	}
}
