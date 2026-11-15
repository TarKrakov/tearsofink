package com.tarkrakov.tearsofink.weapon.combat;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillInput;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
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
 * Ревущая Секира (Шпиль): R — рывок вперёд и рассекающий взмах; парой на ПКМ — казнь
 * (держать — топоры расходятся, отпустил — подпрыжка и двойной удар; убил — утробный рёв, враги вокруг бегут в ужасе).
 * Движения — weapon_anims/axe.json, связки — weapon_combos/axe.json.
 */
public final class AxeMoves {
	// ===== Настройки — крутить здесь =====
	/** R: рывок (блоков за тик × тиков ≈ 4 блока), потом взмах: дуга, дальность, урон ×силу удара оружия. */
	public static final double RUSH_SPEED = 0.8;
	public static final int RUSH_TICKS = 5, RUSH_SWEEP_AT = 7;
	public static final double RUSH_ARC = 150, RUSH_REACH = 3.8;
	public static final float RUSH_DAMAGE = 1.6f, RUSH_KNOCKBACK = 0.7f;
	/** Казнь: дольше держишь — сильнее (до MAX_HOLD_TICKS); урон каждого из двух ударов ×(от … до) силы оружия. */
	public static final int MAX_HOLD_TICKS = 40;
	public static final float EXECUTE_MIN = 1.6f, EXECUTE_MAX = 2.6f;
	public static final double EXECUTE_ARC = 100, EXECUTE_REACH = 3.6;
	/** Удары казни после отпускания (тики): правый, левый. */
	public static final int EXECUTE_HIT_RIGHT = 5, EXECUTE_HIT_LEFT = 8, EXECUTE_TICKS = 18;
	/** Рёв после убийства казнью: радиус и сколько тиков враги бегут. */
	public static final double FEAR_RADIUS = 10;
	public static final int FEAR_TICKS = 100;

	public static final RushSkill RUSH = new RushSkill();
	public static final ExecutionSkill EXECUTION = new ExecutionSkill();

	private record Rush(double dx, double dz, int tick, float power) {}

	private record Hit(long at, float mult, boolean last) {}

	private static final Map<UUID, Rush> RUSHING = new HashMap<>();
	private static final Map<UUID, List<Hit>> HITS = new HashMap<>();
	/** Кто в ужасе: моб → (от кого бежит, до какого тика). */
	private static final Map<Mob, Object[]> FEAR = new HashMap<>();
	/** Убила ли казнь кого-нибудь (на одну казнь — один рёв). */
	private static final Set<UUID> ROARED = new HashSet<>();

	private AxeMoves() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(AxeMoves::tick);
	}

	// ---------------- R: рывок и взмах ----------------

	public static class RushSkill extends Skill {
		RushSkill() {
			super(TearsOfInk.id("axe_rush"), 12, 0, 100);
		}

		@Override
		public boolean activate(ServerPlayer p, SkillInput input) {
			if (RUSHING.containsKey(p.getUUID())) return false;
			Vec3 d = flat(p);
			RUSHING.put(p.getUUID(), new Rush(d.x, d.z, 0, com.tarkrakov.tearsofink.weapon.WeaponAbilities.power(p)));
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.7f, 0.8f);
			return true;
		}
	}

	private static void rushTick(ServerPlayer p, Rush r, Iterator<?> it) {
		int t = r.tick() + 1;
		if (t <= RUSH_TICKS) {
			p.setDeltaMovement(r.dx() * RUSH_SPEED, Math.min(p.getDeltaMovement().y, 0.1), r.dz() * RUSH_SPEED);
			p.hurtMarked = true;
			p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
		} else if (t == RUSH_TICKS + 1) {
			Vec3 v = p.getDeltaMovement();
			p.setDeltaMovement(v.x * 0.25, v.y, v.z * 0.25);
			p.hurtMarked = true;
		}
		if (t >= RUSH_SWEEP_AT) {
			float base = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
			int n = arc(p, RUSH_ARC, RUSH_REACH, base * RUSH_DAMAGE * r.power(), RUSH_KNOCKBACK);
			ServerLevel level = p.serverLevel();
			Vec3 f = flat(p);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + f.x * 1.8, p.getEyeY() - 0.5, p.getZ() + f.z * 1.8, 3, 0.8, 0.1, 0.8, 0);
			level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1f, 0.6f);
			if (n > 0) level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 0.7f);
			it.remove();
			return;
		}
		RUSHING.put(p.getUUID(), new Rush(r.dx(), r.dz(), t, r.power()));
	}

	// ---------------- ПКМ парой: казнь ----------------

	public static class ExecutionSkill extends Skill {
		ExecutionSkill() {
			super(TearsOfInk.id("axe_execution"), 0, 0, 160);
		}

		@Override
		public boolean activate(ServerPlayer p, SkillInput input) {
			Combat.dualAnim(p, Archetype.AXE, "execute_spread", "execute_spread", MAX_HOLD_TICKS + 10);
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
			return true;
		}
	}

	/** Отпустил ПКМ: подпрыжка и двойной удар (сила — от того, сколько держал). */
	public static void strike(ServerPlayer p, int held) {
		float k = Mth.clamp(held / (float) MAX_HOLD_TICKS, 0, 1);
		float mult = Mth.lerp(k, EXECUTE_MIN, EXECUTE_MAX);
		long now = p.serverLevel().getGameTime();
		HITS.put(p.getUUID(), new ArrayList<>(List.of(new Hit(now + EXECUTE_HIT_RIGHT, mult, false), new Hit(now + EXECUTE_HIT_LEFT, mult, true))));
		ROARED.remove(p.getUUID());
		Vec3 f = flat(p);
		p.setDeltaMovement(p.getDeltaMovement().add(f.x * 0.25, 0.38, f.z * 0.25)); // лёгкая подпрыжка
		p.hurtMarked = true;
		Combat.play(p, Archetype.AXE, "execute_strike", "execute_strike_off", EXECUTE_TICKS);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.5f);
	}

	private static void executeHit(ServerPlayer p, Hit h) {
		float base = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
		ServerLevel level = p.serverLevel();
		List<LivingEntity> killed = new ArrayList<>();
		arcHit(p, EXECUTE_ARC, EXECUTE_REACH, base * h.mult(), 0.5f, killed);
		Vec3 f = flat(p);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + f.x * 1.6, p.getEyeY() - 0.6, p.getZ() + f.z * 1.6, 2, 0.4, 0.2, 0.4, 0);
		level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.1f, 0.55f);
		if (!killed.isEmpty() && ROARED.add(p.getUUID())) roar(p);
	}

	/** Утробный рёв: все враги вокруг бегут в ужасе. */
	private static void roar(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.6f, 0.65f);
		level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.5f, 1.4f);
		level.sendParticles(ParticleTypes.SONIC_BOOM, p.getX(), p.getY() + 1.2, p.getZ(), 1, 0, 0, 0, 0);
		long until = level.getGameTime() + FEAR_TICKS;
		for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(FEAR_RADIUS),
				m -> m.isAlive() && !(m instanceof OwnableEntity o && o.getOwnerUUID() != null) && m.distanceToSqr(p) <= FEAR_RADIUS * FEAR_RADIUS)) {
			if (m.getMaxHealth() >= 150) continue; // боссов не пугает
			FEAR.put(m, new Object[]{p.position(), until});
			m.setTarget(null);
			level.sendParticles(ParticleTypes.SMOKE, m.getX(), m.getY() + m.getBbHeight() + 0.2, m.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
		}
	}

	// ---------------- общее ----------------

	private static void tick(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Rush>> it = RUSHING.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null || !p.isAlive()) {
				it.remove();
				continue;
			}
			rushTick(p, e.getValue(), it);
		}
		for (Iterator<Map.Entry<UUID, List<Hit>>> it = HITS.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null || !p.isAlive() || Archetype.of(p.getMainHandItem()) != Archetype.AXE) {
				it.remove();
				continue;
			}
			long now = p.serverLevel().getGameTime();
			e.getValue().removeIf(h -> {
				if (now < h.at()) return false;
				executeHit(p, h);
				return true;
			});
			if (e.getValue().isEmpty()) it.remove();
		}
		if (!FEAR.isEmpty()) {
			for (Iterator<Map.Entry<Mob, Object[]>> it = FEAR.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				Mob m = e.getKey();
				Vec3 from = (Vec3) e.getValue()[0];
				if (!m.isAlive() || m.level().getGameTime() > (long) e.getValue()[1]) {
					it.remove();
					continue;
				}
				m.setTarget(null);
				if (m instanceof PathfinderMob pm && (pm.getNavigation().isDone() || m.tickCount % 20 == 0)) {
					Vec3 away = m.position().subtract(from).multiply(1, 0, 1);
					away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
					Vec3 to = m.position().add(away.scale(10));
					pm.getNavigation().moveTo(to.x, to.y, to.z, 1.4);
				}
			}
		}
	}

	private static int arc(ServerPlayer p, double arcDeg, double reach, float dmg, float knock) {
		return arcHit(p, arcDeg, reach, dmg, knock, null);
	}

	/** Урон дугой перед игроком; killed — кого добили. */
	private static int arcHit(ServerPlayer p, double arcDeg, double reach, float dmg, float knock, List<LivingEntity> killed) {
		Vec3 f = flat(p);
		double half = Math.toRadians(arcDeg / 2);
		int n = 0;
		for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(reach + 1),
				t -> t != p && t.isAlive() && !t.isAlliedTo(p) && !(t instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID())))) {
			Vec3 to = t.position().subtract(p.position()).multiply(1, 0, 1);
			if (Math.sqrt(t.getBoundingBox().distanceToSqr(p.getEyePosition())) > reach) continue;
			if (to.lengthSqr() > 0.04 && Math.acos(Mth.clamp(to.normalize().dot(f), -1, 1)) > half) continue;
			if (!p.hasLineOfSight(t)) continue;
			if (t.hurt(p.damageSources().playerAttack(p), dmg)) {
				n++;
				t.invulnerableTime = 0; // второй удар казни тоже доходит
				if (knock > 0) t.knockback(knock, -f.x, -f.z);
				if (killed != null && (t.isDeadOrDying() || !t.isAlive()) && !(t instanceof Player)) killed.add(t);
				if (killed != null && t instanceof Player && t.isDeadOrDying()) killed.add(t);
			}
		}
		return n;
	}

	private static Vec3 flat(ServerPlayer p) {
		Vec3 l = p.getLookAngle();
		Vec3 f = new Vec3(l.x, 0, l.z);
		return f.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : f.normalize();
	}
}
