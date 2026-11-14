package com.tarkrakov.tearsofink.weapon.combat;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Связки ударов клинков по архетипам. Клиент просит «удар» (ЛКМ), сервер решает, какой шаг связки,
 * запускает движение у всех и наносит урон по дуге в момент удара (а не в момент клика).
 * Шкала атаки не восстановилась (min_strength) — связка сбрасывается к первому удару.
 */
public final class Combat {
	/** Клиент → сервер: игрок махнул клинком в правой руке. */
	public record Swing() implements CustomPacketPayload {
		public static final Type<Swing> TYPE = new Type<>(TearsOfInk.id("combat_swing"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Swing> CODEC = StreamCodec.unit(new Swing());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Сервер → клиенты: сущность начала движение anim архетипа. ticks — сколько клинок занят,
	 * speed — во сколько раз быстрее после этого заполняется шкала атаки (меньше 0 — приём, шкала не трогается).
	 */
	public record Anim(int entity, String archetype, String anim, int ticks, float speed, float tempo) implements CustomPacketPayload {
		public static final Type<Anim> TYPE = new Type<>(TearsOfInk.id("combat_anim"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Anim> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Anim::entity, ByteBufCodecs.STRING_UTF8, Anim::archetype,
				ByteBufCodecs.STRING_UTF8, Anim::anim, ByteBufCodecs.VAR_INT, Anim::ticks, ByteBufCodecs.FLOAT, Anim::speed, ByteBufCodecs.FLOAT, Anim::tempo, Anim::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class State {
		int next;            // какой шаг связки следующий
		long busyUntil;      // до какого тика идёт текущий удар
		long lastEnd;        // когда закончился прошлый удар
		long hitAt = -1;     // когда нанести урон текущего удара
		Archetype hitArchetype;
		Combos.Step hitStep;
		float hitScale = 1f; // слабый удар (шкала не восстановилась)
		final java.util.List<Pending> extra = new java.util.ArrayList<>(); // удары парных приёмов
		float speed = 1f, carry; // ускоренное заполнение шкалы
		boolean queued;      // нажал чуть раньше — удар запомнен
		long queuedAt;
		boolean dual;        // идёт парная связка (клинки в обеих руках)
		int window = 30;     // окно «чуть позже» после последнего удара
	}

	private record Pending(long at, Archetype archetype, Combos.Step step) {}

	private static final Map<UUID, State> STATES = new HashMap<>();

	// ===== Парные приёмы (два одинаковых клинка, ПКМ) — числа крутить здесь =====
	/** Гильотина: удар каждой рукой по очереди, пока держишь ПКМ (тики: длина, момент урона). */
	public static final Combos.Step GUILLOTINE_CHOP = new Combos.Step("guillotine_chop", 10, 4, 0.8f, 3.3f, 100f, 0.3f, 1f);
	/** Гильотина: завершающий удар сверху обоими клинками, когда отпустил ПКМ. */
	public static final Combos.Step GUILLOTINE_END = new Combos.Step("guillotine_end", 18, 9, 2.0f, 3.6f, 80f, 1.0f, 1f);
	public static final int GUILLOTINE_MAX_CHOPS = 8;

	private Combat() {}

	public static void init() {
		Combos.init();
		PayloadTypeRegistry.playC2S().register(Swing.TYPE, Swing.CODEC);
		PayloadTypeRegistry.playS2C().register(Anim.TYPE, Anim.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Swing.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> swing(ctx.player())));
		ServerTickEvents.END_SERVER_TICK.register(Combat::tick);
	}

	private static State state(ServerPlayer p) {
		return STATES.computeIfAbsent(p.getUUID(), u -> new State());
	}

	/** Сейчас идёт удар или приём (клинок занят). */
	public static boolean busy(ServerPlayer p) {
		State s = STATES.get(p.getUUID());
		return s != null && p.serverLevel().getGameTime() < s.busyUntil;
	}

	private static void swing(ServerPlayer p) {
		if (!p.isAlive() || p.isSpectator()) return;
		ItemStack weapon = p.getMainHandItem();
		Archetype a = Archetype.of(weapon);
		if (a == null) return;
		State s = state(p);
		long now = p.serverLevel().getGameTime();
		Combos.Combo combo = Combos.get(a);
		float strength = p.getAttackStrengthScale(0.5f);
		// сколько ещё ждать до готовности (с учётом ускоренной/замедленной шкалы)
		float delay = p.getCurrentItemAttackStrengthDelay();
		long waitBar = strength >= combo.minStrength() ? 0 : (long) Math.ceil((combo.minStrength() - strength) * delay / Math.max(0.1f, s.speed));
		long wait = Math.max(Math.max(0, s.busyUntil - now), waitBar);
		if (wait > 0) {
			if (wait <= combo.early()) {
				// чуть раньше — не беда: удар запомнен и выйдет ровно вовремя
				s.queued = true;
				s.queuedAt = now;
				return;
			}
			if (now < s.busyUntil) return; // ещё машет — клик мимо, но связку не рвём
		}
		start(p, s, a, combo, strength, now);
	}

	private static void start(ServerPlayer p, State s, Archetype a, Combos.Combo combo, float strength, long now) {
		s.queued = false;
		boolean dual = !combo.dualSteps().isEmpty() && Archetype.of(p.getOffhandItem()) == a;
		java.util.List<Combos.Step> steps = dual ? combo.dualSteps() : combo.steps();
		if (s.dual != dual) {
			s.dual = dual;
			s.next = 0;
		}
		int index;
		float scale = 1f;
		if (strength < combo.minStrength()) {
			// сильно поторопился — связка сорвана: слабый первый удар, дальше снова с начала
			index = 0;
			scale = strength * strength;
			s.next = 0;
		} else {
			if (now - s.lastEnd > s.window || s.next >= steps.size()) s.next = 0;
			index = s.next;
			s.next = index + 1 >= steps.size() ? 0 : index + 1;
		}
		Combos.Step step = steps.get(index);
		s.window = step.late() >= 0 ? step.late() : combo.resetTicks();
		int duration = combo.scaled(step.duration());
		s.busyUntil = now + duration;
		s.lastEnd = now + duration;
		s.hitAt = now + combo.scaled(step.hit());
		s.hitArchetype = a;
		s.hitStep = step;
		s.hitScale = scale;
		s.speed = step.speed() * combo.tempo();
		s.carry = 0;
		p.resetAttackStrengthTicker();
		broadcast(p, a, step.anim(), duration, s.speed, combo.tempo()); // пустое anim — правая рука в этом ударе не двигается (шкала всё равно)
		if (step.offAnim() != null) broadcast(p, a, "off:" + step.offAnim(), duration, -2f, combo.tempo());
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f,
				a == Archetype.EXECUTIONER ? 0.6f : 0.9f);
	}

	/** Приём (абилка) клинка: движение у всех и занятость клинка. */
	public static void ability(ServerPlayer p, ItemStack weapon) {
		Archetype a = Archetype.of(weapon);
		if (a == null) return;
		Combos.Combo combo = Combos.get(a);
		State s = state(p);
		long now = p.serverLevel().getGameTime();
		s.busyUntil = now + combo.abilityTicks();
		s.lastEnd = now + combo.abilityTicks();
		s.next = 0;
		s.hitAt = -1;
		s.queued = false;
		boolean left = weapon == p.getOffhandItem() && weapon != p.getMainHandItem();
		broadcast(p, a, (left ? "off:" : "") + combo.abilityAnim(), combo.abilityTicks(), -1f, 1f); // абилка левой руки — движение левой
	}

	/** Гильотина: очередной удар одной рукой (offhand — левой, т. е. второй рукой). */
	public static void guillotineChop(ServerPlayer p, boolean offhand) {
		State s = state(p);
		long now = p.serverLevel().getGameTime();
		Combos.Combo combo = Combos.get(Archetype.EXECUTIONER);
		s.busyUntil = now + chopInterval();
		s.lastEnd = s.busyUntil;
		s.next = 0;
		s.queued = false;
		s.extra.add(new Pending(now + combo.scaled(GUILLOTINE_CHOP.hit()), Archetype.EXECUTIONER, GUILLOTINE_CHOP));
		broadcast(p, Archetype.EXECUTIONER, (offhand ? "off:" : "") + GUILLOTINE_CHOP.anim(), chopInterval(), -1f, combo.tempo());
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 0.55f + p.getRandom().nextFloat() * 0.1f);
	}

	/** Промежуток между ударами Гильотины (с учётом общей скорости Палачей). */
	public static int chopInterval() {
		return Combos.get(Archetype.EXECUTIONER).scaled(GUILLOTINE_CHOP.duration());
	}

	/** Гильотина: отпустил ПКМ — удар сверху обоими клинками. */
	public static void guillotineEnd(ServerPlayer p) {
		State s = state(p);
		long now = p.serverLevel().getGameTime();
		Combos.Combo combo = Combos.get(Archetype.EXECUTIONER);
		int duration = combo.scaled(GUILLOTINE_END.duration());
		s.busyUntil = now + duration;
		s.lastEnd = s.busyUntil;
		s.extra.add(new Pending(now + combo.scaled(GUILLOTINE_END.hit()), Archetype.EXECUTIONER, GUILLOTINE_END));
		broadcast(p, Archetype.EXECUTIONER, GUILLOTINE_END.anim(), duration, -1f, combo.tempo());
		broadcast(p, Archetype.EXECUTIONER, "off:" + GUILLOTINE_END.anim(), duration, -1f, combo.tempo());
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.5f);
	}

	/** Движение парного приёма у всех, без урона (урон даёт сам приём). */
	public static void dualAnim(ServerPlayer p, Archetype a, String mainAnim, String offAnim, int ticks) {
		State s = state(p);
		long now = p.serverLevel().getGameTime();
		s.busyUntil = now + ticks;
		s.lastEnd = s.busyUntil;
		s.next = 0;
		s.queued = false;
		broadcast(p, a, mainAnim, ticks, -1f, 1f);
		broadcast(p, a, "off:" + offAnim, ticks, -1f, 1f);
	}

	/**
	 * Ускоренное заполнение шкалы атаки: каждый тик добавляет (speed − 1) лишних тиков. Возвращает новый остаток.
	 * Работает одинаково на сервере и у игрока (чтобы шкала под прицелом не врала).
	 */
	public static float fastFill(net.minecraft.world.entity.player.Player p, float speed, float carry) {
		if (speed == 1f || p.getAttackStrengthScale(0) >= 1f) return 0;
		carry += speed - 1f; // меньше 1 — шкала идёт медленнее (тики отнимаются)
		int add = (int) carry;
		if (add != 0) {
			var acc = (com.tarkrakov.tearsofink.mixin.LivingEntityAttackAccessor) p;
			acc.tearsofink$setAttackStrengthTicker(Math.max(0, acc.tearsofink$getAttackStrengthTicker() + add));
		}
		return carry - add;
	}

	public static void broadcast(ServerPlayer p, Archetype a, String anim, int ticks, float speed, float tempo) {
		Anim msg = new Anim(p.getId(), a.id, anim, ticks, speed, tempo);
		ServerPlayNetworking.send(p, msg);
		for (ServerPlayer other : PlayerLookup.tracking(p)) {
			if (other != p) ServerPlayNetworking.send(other, msg);
		}
	}

	private static void tick(MinecraftServer server) {
		if (STATES.isEmpty()) return;
		STATES.entrySet().removeIf(e -> server.getPlayerList().getPlayer(e.getKey()) == null);
		for (var e : STATES.entrySet()) {
			State s = e.getValue();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null) continue;
			long now = p.serverLevel().getGameTime();
			if (s.speed != 1f) {
				s.carry = fastFill(p, s.speed, s.carry);
				if (p.getAttackStrengthScale(0) >= 1f) s.speed = 1f;
			}
			if (!s.extra.isEmpty()) {
				var it = s.extra.iterator();
				while (it.hasNext()) {
					Pending h = it.next();
					if (now < h.at()) continue;
					it.remove();
					if (p.isAlive() && Archetype.of(p.getMainHandItem()) == h.archetype()) strike(p, h.step(), 1f);
				}
			}
			if (s.hitAt >= 0 && now >= s.hitAt) {
				s.hitAt = -1;
				if (p.isAlive() && Archetype.of(p.getMainHandItem()) == s.hitArchetype) strike(p, s.hitStep, s.hitScale); // сменил клинок — удар пропал
			}
			if (s.queued) {
				Archetype a = Archetype.of(p.getMainHandItem());
				if (a == null || !p.isAlive() || now - s.queuedAt > 20) {
					s.queued = false;
				} else if (now >= s.busyUntil && p.getAttackStrengthScale(0.5f) >= Combos.get(a).minStrength()) {
					start(p, s, a, Combos.get(a), p.getAttackStrengthScale(0.5f), now);
				}
			}
		}
	}

	/** Урон по дуге перед игроком. */
	private static void strike(ServerPlayer p, Combos.Step step, float scale) {
		ServerLevel level = p.serverLevel();
		ItemStack weapon = p.getMainHandItem();
		Vec3 eye = p.getEyePosition();
		Vec3 look = p.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
		double half = Math.toRadians(step.arc() / 2);
		double base = p.getAttributeValue(Attributes.ATTACK_DAMAGE) * step.damage() * scale;
		boolean hitAny = false;
		for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(step.reach() + 1))) {
			if (t == p || !t.isAlive() || t.isSpectator() || t.isAlliedTo(p)) continue;
			if (t instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) continue;
			Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
			double dist = Math.sqrt(t.getBoundingBox().distanceToSqr(eye));
			if (dist > step.reach()) continue;
			if (Math.abs(to.y) > 2.5) continue;
			Vec3 toFlat = new Vec3(to.x, 0, to.z);
			if (toFlat.lengthSqr() > 0.04) {
				double angle = Math.acos(Mth.clamp(toFlat.normalize().dot(flat), -1, 1));
				if (angle > half) continue;
			}
			if (!p.hasLineOfSight(t)) continue;
			DamageSource src = p.damageSources().playerAttack(p);
			float dmg = EnchantmentHelper.modifyDamage(level, weapon, t, src, (float) base);
			if (t.hurt(src, dmg)) {
				hitAny = true;
				if (step.knockback() > 0) t.knockback(step.knockback(), -flat.x, -flat.z);
				EnchantmentHelper.doPostAttackEffects(level, t, src);
				p.setLastHurtMob(t);
			}
		}
		if (hitAny) {
			weapon.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, eye.x + flat.x * 1.6, eye.y - 0.4, eye.z + flat.z * 1.6, 1, 0, 0, 0, 0);
			level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 0.8f);
		}
	}
}
