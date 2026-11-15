package com.tarkrakov.tearsofink.weapon.blood;

import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import com.tarkrakov.tearsofink.weapon.combat.Combat;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Стойка парирования Дуссака: пока стоит, любой удар вблизи (не стрелы) обнуляется, а клинок сам отвечает
 * росчерком с кровотечением. Парой (ПКМ) — дольше и сильнее, отвечают оба клинка.
 */
public final class Parry {
	private static final class State {
		long until, nextRiposte, resumeAt = -1;
		boolean dual;
	}

	private static final Map<UUID, State> STANCE = new HashMap<>();

	private Parry() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Parry::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (STANCE.isEmpty() || !(entity instanceof ServerPlayer p)) return true;
			State s = STANCE.get(p.getUUID());
			if (s == null || !melee(source, p)) return true;
			long now = p.server.getTickCount();
			if (now >= s.nextRiposte) riposte(p, (LivingEntity) source.getEntity(), s, now);
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.4f);
			return false; // удар пойман — урона нет
		});
	}

	/** Удар вблизи: бьёт само существо (не стрела, не взрыв). */
	private static boolean melee(DamageSource source, ServerPlayer p) {
		return source.getEntity() instanceof LivingEntity by && by != p && source.getDirectEntity() == by
				&& by.distanceTo(p) <= BloodTuning.RIPOSTE_REACH + 1;
	}

	public static boolean active(ServerPlayer p) {
		return STANCE.containsKey(p.getUUID());
	}

	/** Встать в стойку. dual — парой (движение обеих рук задаёт сам вызов). */
	public static void start(ServerPlayer p, boolean dual) {
		State s = new State();
		long now = p.server.getTickCount();
		s.dual = dual;
		s.until = now + (dual ? BloodTuning.DOUBLE_PARRY_TICKS : BloodTuning.PARRY_TICKS);
		STANCE.put(p.getUUID(), s);
		if (dual) Combat.play(p, Archetype.DUSSAK, "parry", "parry", BloodTuning.DOUBLE_PARRY_TICKS);
		// ДЗЫНЬ
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.45f, 1.9f);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
	}

	private static void riposte(ServerPlayer p, LivingEntity by, State s, long now) {
		s.nextRiposte = now + BloodTuning.RIPOSTE_GAP;
		s.resumeAt = now + BloodTuning.RIPOSTE_GAP;
		double base = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float dmg = (float) (base * (s.dual ? BloodTuning.DOUBLE_RIPOSTE_MULT : BloodTuning.RIPOSTE_MULT));
		if (by.isAlive() && by.distanceTo(p) <= BloodTuning.RIPOSTE_REACH && by.hurt(p.damageSources().playerAttack(p), dmg)) {
			Bleeding.apply(by, p, s.dual ? BloodTuning.DOUBLE_RIPOSTE_BLEED : BloodTuning.RIPOSTE_BLEED);
			Vec3 d = by.position().subtract(p.position()).multiply(1, 0, 1).normalize();
			by.knockback(0.4, -d.x, -d.z);
		}
		if (s.dual) Combat.play(p, Archetype.DUSSAK, "dual_split", "dual_split", BloodTuning.RIPOSTE_GAP);
		else Combat.play(p, Archetype.DUSSAK, "riposte", null, BloodTuning.RIPOSTE_GAP);
		p.serverLevel().sendParticles(ParticleTypes.CRIT, by.getX(), by.getY() + by.getBbHeight() * 0.6, by.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35f, 2.0f);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1.1f);
	}

	private static void tick(MinecraftServer server) {
		if (STANCE.isEmpty()) return;
		long now = server.getTickCount();
		for (Iterator<Map.Entry<UUID, State>> it = STANCE.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			State s = e.getValue();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			boolean holding = p != null && p.isAlive() && Archetype.of(p.getMainHandItem()) == Archetype.DUSSAK
					&& (!s.dual || Archetype.of(p.getOffhandItem()) == Archetype.DUSSAK);
			if (!holding || now >= s.until) {
				it.remove();
				if (p != null && p.isAlive()) Combat.play(p, Archetype.DUSSAK, "parry_release", s.dual ? "parry_release" : null, 4);
				continue;
			}
			if (s.resumeAt >= 0 && now >= s.resumeAt) { // после ответа — снова в стойку
				s.resumeAt = -1;
				int left = (int) (s.until - now);
				Combat.play(p, Archetype.DUSSAK, "parry_resume", s.dual ? "parry_resume" : null, left);
			}
		}
	}
}
