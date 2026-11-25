package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.effect.ModEffects;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillInput;
import com.tarkrakov.tearsofink.skill.SkillPayloads;
import com.tarkrakov.tearsofink.skill.Skills;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Абилки оружия: Таран и Сияние (щиты Души), Волна (Скимитар) и Рассечение (Клинок Палача) — оружие Чернил.
 * Цифры — в начале каждого класса.
 */
public final class WeaponAbilities {
	public static final RamAbility RAM = new RamAbility();
	public static final RadianceAbility RADIANCE = new RadianceAbility();
	public static final WaveAbility WAVE = new WaveAbility();
	/** Тусклый скимитар (стартовый Пути Чернил): Волна слабее и дороже. */
	public static final DimWaveAbility DIM_WAVE = new DimWaveAbility();
	public static final CleaveAbility CLEAVE = new CleaveAbility();
	/** Двуручный клинок в ЛЕВОЙ руке (когда в правой одноручный): урон его абилки ×это. */
	public static final float OFFHAND_TWO_HANDED_POWER = 1f;
	/** Сила текущей абилки для игрока (1 — полная). Ставится перед запуском, абилки читают при срабатывании. */
	private static final Map<UUID, Float> POWER = new HashMap<>();

	public static float power(ServerPlayer player) {
		return POWER.getOrDefault(player.getUUID(), 1f);
	}
	/** Парные приёмы (два одинаковых клинка, ПКМ). */
	public static final GuillotineAbility GUILLOTINE = new GuillotineAbility();
	public static final FlourishAbility FLOURISH = new FlourishAbility();
	public static final MercyAbility MERCY = new MercyAbility();
	public static final RepelAbility REPEL = new RepelAbility();
	public static final BlotAbility BLOT = new BlotAbility();
	public static final SalvationAbility SALVATION = new SalvationAbility();

	private WeaponAbilities() {}

	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			POWER.clear();
			RAM.active.clear();
			RADIANCE.active.clear();
			CLEAVE.pending.clear();
			MERCY.active.clear();
			MERCY.lockedUntil.clear();
			SALVATION.glow.clear();
		});
		PayloadTypeRegistry.playC2S().register(SkillPayloads.UseWeapon.TYPE, SkillPayloads.UseWeapon.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SkillPayloads.UseWeapon.TYPE, (payload, context) ->
				context.player().server.execute(() -> use(context.player(), payload)));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			RAM.tick(server);
			RADIANCE.tick(server);
			CLEAVE.tick(server);
			MERCY.tick(server);
			SALVATION.tick(server);
		});
	}

	public static boolean cancelWindup(ServerPlayer p) {
		CLEAVE.pending.remove(p.getUUID());
		return RAM.active.remove(p.getUUID()) != null;
	}
	public static Skill boundAbility(ServerPlayer p, boolean left) {
		if (left && com.tarkrakov.tearsofink.weapon.ranged.RangedCombat.blocksOffhand(p)) return null;
		if (com.tarkrakov.tearsofink.weapon.shield.ShieldStance.dual(p))
			return left ? ((SoulShieldItem)p.getOffhandItem().getItem()).ability() : com.tarkrakov.tearsofink.weapon.shield.Resonance.SKILL;
		ItemStack stack = left ? p.getOffhandItem() : p.getMainHandItem();
		return stack.getItem() instanceof AbilityWeapon w ? w.ability() : null;
	}

	private static void use(ServerPlayer player, SkillPayloads.UseWeapon payload) {
		if (payload.offhand() && com.tarkrakov.tearsofink.weapon.ranged.RangedCombat.blocksOffhand(player)) return;
		if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) return;
		// два щита Души: R — Резонанс, Shift+R — способность левого щита
		if (com.tarkrakov.tearsofink.weapon.shield.ShieldStance.dual(player)) {
			Skill ability = boundAbility(player, payload.offhand());
			if (ability == MERCY && MERCY.isActive(player)) { MERCY.stop(player); return; }
			Skills.activate(player, ability, new SkillInput(payload.strafe(), payload.forward(), payload.offhand()));
			return;
		}
		// Shift+R is a separate defensive action for the Pale Linear Greatsword.
		// The existing packet encodes Shift as offhand; handle either sword before
		// normal offhand dispatch, including when both hands carry this sword.
		if (payload.offhand() && com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.dual(player)) {
			com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.guard(player);
			return;
		}
		ItemStack stack = payload.offhand() ? player.getOffhandItem() : player.getMainHandItem();
		if (com.tarkrakov.tearsofink.weapon.combat.PaleLongswordMoves.isWeapon(stack)) {
			com.tarkrakov.tearsofink.weapon.combat.PaleLongswordMoves.useR(player, payload.offhand());
			return;
		}
		if (com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.isWeapon(stack)) {
			POWER.put(player.getUUID(), 1f);
			com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.useR(player, payload.offhand());
			return;
		}
		if (!(stack.getItem() instanceof AbilityWeapon weapon) || weapon.ability() == null) return;
		// аура Милосердия гасится повторным нажатием — бесплатно и без проверок
		if (weapon.ability() == MERCY && MERCY.isActive(player)) {
			MERCY.stop(player);
			return;
		}
		SkillInput input = new SkillInput(payload.strafe(), payload.forward(), payload.offhand());
		POWER.put(player.getUUID(), 1f);
		if (stack.getItem() instanceof SoulShieldItem) {
			Skills.activate(player, weapon.ability(), input);
			return;
		}
		if (Skills.activate(player, weapon.ability(), input) && stack.getItem() instanceof InkGreatswordItem) {
			com.tarkrakov.tearsofink.weapon.combat.Combat.ability(player, stack); // движение приёма у всех
		}
	}

	private static Vec3 flatLook(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}

	// ======================================================================
	/** Щит-таран: рывок вперёд со щитом, задетых отбрасывает, немного ранит и замедляет. */
	public static class RamAbility extends Skill {
		public static final double SPEED = 0.85;
		public static final int DURATION_TICKS = 5;
		public static final float DAMAGE = 7f;
		public static final double KNOCKBACK = 0.55;
		public static final int SLOW_TICKS = 20;

		private record Ram(double dx, double dz, int ticksLeft, Set<UUID> hit, float power) {}

		private final Map<UUID, Ram> active = new HashMap<>();

		RamAbility() {
			super(TearsOfInk.id("shield_ram"), 0, 15, 80);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			if (active.containsKey(player.getUUID())) return false;
			Vec3 dir = flatLook(player);
			active.put(player.getUUID(), new Ram(dir.x, dir.z, DURATION_TICKS, new HashSet<>(), SoulShieldItem.selectedPower(player,RAM,input.sneaking())));
			push(player, dir.x, dir.z);
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 0.6f);
			return true;
		}

		private void push(ServerPlayer player, double dx, double dz) {
			player.setDeltaMovement(dx * SPEED, Math.min(player.getDeltaMovement().y, 0.1), dz * SPEED);
			player.hurtMarked = true;
		}

		void tick(MinecraftServer server) {
			Iterator<Map.Entry<UUID, Ram>> it = active.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Ram> e = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
				Ram r = e.getValue();
				if (player == null || !player.isAlive() || r.ticksLeft() <= 0) {
					if (player != null) {
						Vec3 v = player.getDeltaMovement();
						player.setDeltaMovement(v.x * 0.3, v.y, v.z * 0.3);
						player.hurtMarked = true;
					}
					it.remove();
					continue;
				}
				ServerLevel level = player.serverLevel();
				AABB box = player.getBoundingBox().inflate(0.8, 0.2, 0.8).move(r.dx() * 0.6, 0, r.dz() * 0.6);
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
						t -> com.tarkrakov.tearsofink.weapon.shield.ShieldStance.offensiveTarget(player, t) && player.hasLineOfSight(t))) {
					if (!r.hit().add(target.getUUID())) continue;
					float f = r.power(); // потёртый щит слабее
					target.hurt(player.damageSources().playerAttack(player), DAMAGE * f);
					target.knockback(KNOCKBACK * f, -r.dx(), -r.dz());
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 2));
					level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.8f);
				}
				push(player, r.dx(), r.dz());
				level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
				e.setValue(new Ram(r.dx(), r.dz(), r.ticksLeft() - 1, r.hit(), r.power()));
			}
		}
	}

	// ======================================================================
	/** Светлый щит: на время щит светлеет, входящий урон −40% (пока щит в левой руке). */
	public static class RadianceAbility extends Skill {
		public static final int DURATION_TICKS = 6 * 20;
		/** Сопротивление II = −40% урона. */
		public static final int RESISTANCE_LEVEL = 1;

		private record Radiance(ItemStack shield, int left) {}
		private final Map<UUID, Radiance> active = new HashMap<>();

		RadianceAbility() {
			super(TearsOfInk.id("radiance"), 0, 20, 240);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			ItemStack shield = player.getItemInHand(input.sneaking() ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND);
			active.put(player.getUUID(), new Radiance(shield,DURATION_TICKS));
			refreshRadiance(player,2);
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.5f);
			return true;
		}

		private static void refreshRadiance(ServerPlayer player, int duration) {
			player.addEffect(new MobEffectInstance(ModEffects.RADIANCE, duration, 0, false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, RESISTANCE_LEVEL, false, false, false));
		}

		void tick(MinecraftServer server) {
			Iterator<Map.Entry<UUID, Radiance>> it = active.entrySet().iterator();
			while (it.hasNext()) {
				var e = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
				Radiance state = e.getValue();
				int left = state.left() - 1;
				boolean held = player != null && (player.getMainHandItem() == state.shield()
						|| !com.tarkrakov.tearsofink.weapon.ranged.RangedCombat.blocksOffhand(player) && player.getOffhandItem() == state.shield());
				if (!held || !player.isAlive() || !com.tarkrakov.tearsofink.world.ModDimensions.hasLandsRules(player.level()) || left <= 0) {
					// Only our short pulse expires. Never remove another source of resistance.
					it.remove();
					continue;
				}
				refreshRadiance(player, Math.min(2,left));
				if (left % 3 == 0) {
					player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
							2, 0.5, 0.6, 0.5, 0.01);
				}
				e.setValue(new Radiance(state.shield(),left));
			}
		}
	}

	// ======================================================================
	/** Меч Волны: взмах — вперёд летит чёрная волна, проходит сквозь врагов. */
	public static class WaveAbility extends Skill {
		WaveAbility() {
			super(TearsOfInk.id("ink_wave"), 8, 0, 40);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			ServerLevel level = player.serverLevel();
			Vec3 dir = flatLook(player);
			InkWaveEntity wave = new InkWaveEntity(level, player, dir);
			level.addFreshEntity(wave);
			player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.6f);
			level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 0.8f, 0.6f);
			return true;
		}
	}

	/** Тусклая Волна: 10 Чернил вместо 8, урон ×0,8. */
	public static class DimWaveAbility extends Skill {
		public static final float DAMAGE = InkWaveEntity.DAMAGE * 0.8f;

		DimWaveAbility() {
			super(TearsOfInk.id("dim_wave"), 10, 0, 40);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			ServerLevel level = player.serverLevel();
			level.addFreshEntity(new InkWaveEntity(level, player, flatLook(player), false, DAMAGE, 0.35));
			player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.7f);
			level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 0.7f, 0.7f);
			return true;
		}
	}

	// ======================================================================
	/** Гильотина (два Клинка Палачей, держать ПКМ): сами удары ведёт InkGreatswordItem, здесь — допуск и перезарядка. */
	public static class GuillotineAbility extends Skill {
		GuillotineAbility() {
			super(TearsOfInk.id("guillotine"), 0, 0, 120);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			return true;
		}
	}

	/**
	 * Чернильный Росчерк (два Скимитара, ПКМ): встречный взмах — две чернильные волны (выше и ниже), урон ×2,
	 * цена — в полтора раза дороже Волны.
	 */
	public static class FlourishAbility extends Skill {
		FlourishAbility() {
			super(TearsOfInk.id("ink_flourish"), 12, 0, 50);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			ServerLevel level = player.serverLevel();
			Vec3 dir = flatLook(player);
			Vec3 side = new Vec3(-dir.z, 0, dir.x);
			for (int i = 0; i < 2; i++) {
				InkWaveEntity wave = new InkWaveEntity(level, player, dir);
				double up = i == 0 ? 0.55 : -0.25, off = i == 0 ? 0.25 : -0.25;
				wave.setPos(wave.getX() + side.x * off, wave.getY() + up, wave.getZ() + side.z * off);
				level.addFreshEntity(wave);
			}
			com.tarkrakov.tearsofink.weapon.combat.Combat.dualAnim(player, com.tarkrakov.tearsofink.weapon.combat.Archetype.SCIMITAR,
					"flourish_high", "flourish_low", 14);
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.7f);
			level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1f, 0.5f);
			return true;
		}
	}

	// ======================================================================
	/** Меч Рассечения: замах, затем удар сверху вниз — чернила бьют по площадке 3×3 перед игроком. */
	public static class CleaveAbility extends Skill {
		public static final int WINDUP_TICKS = 10;
		public static final float DAMAGE = 12f;
		public static final double KNOCKBACK = 1.0;
		/** Центр удара — на столько блоков перед игроком. */
		public static final double REACH = 2.75;

		private record PendingCleave(int ticksLeft, float power, boolean offhand) {}
		private final Map<UUID, PendingCleave> pending = new HashMap<>();

		CleaveAbility() {
			super(TearsOfInk.id("ink_cleave"), 15, 0, 100);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			if (pending.containsKey(player.getUUID())) return false;
			pending.put(player.getUUID(), new PendingCleave(WINDUP_TICKS, WeaponAbilities.power(player), input.sneaking()));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WINDUP_TICKS, 3, false, false, false));
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.6f, 0.6f);
			return true;
		}

		void tick(MinecraftServer server) {
			Iterator<Map.Entry<UUID, PendingCleave>> it = pending.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, PendingCleave> e = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
				if (player == null || !player.isAlive()) {
					it.remove();
					continue;
				}
				PendingCleave cleave = e.getValue();
				int left = cleave.ticksLeft() - 1;
				ServerLevel level = player.serverLevel();
				if (left > 0) {
					// замах: чернила собираются над головой
					level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 2.2, player.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
					e.setValue(new PendingCleave(left, cleave.power(), cleave.offhand()));
					continue;
				}
				it.remove();
				strike(player, level, cleave.power(), cleave.offhand());
			}
		}

		private void strike(ServerPlayer player, ServerLevel level, float mult, boolean leftHand) {
			Vec3 dir = flatLook(player);
			Vec3 center = player.position().add(dir.scale(REACH));
			AABB area = new AABB(center.x - 1.5, center.y - 0.5, center.z - 1.5, center.x + 1.5, center.y + 2.0, center.z + 1.5);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, t -> t != player && t.isAlive())) {
				target.hurt(player.damageSources().playerAttack(player), DAMAGE * mult);
				target.knockback(KNOCKBACK, -dir.x, -dir.z);
			}
			player.swing(leftHand ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND, true);
			level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 0.3, center.z, 60, 1.2, 0.2, 1.2, 0.08);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.8, center.z, 3, 0.8, 0.1, 0.8, 0.0);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 0.5f);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4f, 1.6f);
		}
	}

	// ======================================================================
	/**
	 * Щит Милосердия: аура лечения. Нажал — игрок замедлен, ты и игроки рядом лечатся каждую секунду,
	 * аура тянет Душу. Гаснет: повторное нажатие, кончилась Душа, щит убран, вышел из области, 0 Души.
	 */
	public static class MercyAbility extends Skill {
		public static final double RADIUS = 5;
		/** Лечение раз в секунду (2 = одно сердце). */
		public static final float HEAL_PER_SECOND = 1f;
		/** Сколько Души в секунду тянет аура (своё восстановление ~1/с продолжает идти). */
		public static final float SOUL_PER_SECOND = 4f;
		/** Через сколько тиков после конца ауры её можно включить снова. */
		public static final int LOCKOUT_TICKS = 6 * 20;

		private record Aura(ItemStack shield,int age,float power) {}
		private final Map<UUID, Aura> active = new HashMap<>();
		private final Map<UUID, Integer> lockedUntil = new HashMap<>();

		MercyAbility() {
			// 5 Души — за включение; перезарядка короткая, чтобы повторное нажатие гасило ауру
			super(TearsOfInk.id("mercy"), 0, 5, 10);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			UUID id = player.getUUID();
			if (active.containsKey(id)) {
				stop(player);
				return false; // выключение бесплатное
			}
			if (lockedUntil.getOrDefault(id, 0) > player.server.getTickCount()) return false;
			ItemStack shield = input.sneaking() ? player.getOffhandItem() : player.getMainHandItem();
			active.put(id,new Aura(shield,0,SoulShieldItem.selectedPower(player,MERCY,input.sneaking())));
			refreshAura(player);
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
			return true;
		}

		public boolean isActive(ServerPlayer player) {
			return active.containsKey(player.getUUID());
		}
		public void equipmentChanged(ServerPlayer player) {
			Aura aura = active.get(player.getUUID());
			if (aura != null && !holdsShield(player, aura)) stop(player);
		}

		private static boolean holdsShield(ServerPlayer player, Aura aura) {
			return player.getMainHandItem() == aura.shield() || !com.tarkrakov.tearsofink.weapon.ranged.RangedCombat.blocksOffhand(player) && player.getOffhandItem() == aura.shield();
		}

		public int lockoutRemaining(ServerPlayer p) {
			return Math.max(0, lockedUntil.getOrDefault(p.getUUID(), 0) - p.server.getTickCount());
		}

		void stop(ServerPlayer player) {
			active.remove(player.getUUID());
			lockedUntil.put(player.getUUID(), player.server.getTickCount() + LOCKOUT_TICKS);
			player.removeEffect(ModEffects.MERCY);
			// Our slowdown is a short refreshed pulse; do not strip external debuffs.
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 1.4f);
		}

		private static void refreshAura(ServerPlayer player) {
			player.addEffect(new MobEffectInstance(ModEffects.MERCY, 2, 0, false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 1, false, false, false));
		}

		void tick(MinecraftServer server) {
			lockedUntil.values().removeIf(until -> until <= server.getTickCount());
			for (UUID id : new java.util.ArrayList<>(active.keySet())) {
				ServerPlayer player = server.getPlayerList().getPlayer(id);
				if (player == null) {
					active.remove(id);
					continue;
				}
				Aura aura = active.get(id);
				var data = com.tarkrakov.tearsofink.ink.InkPowers.get(player);
				boolean creative = player.getAbilities().instabuild;
				float drain = SOUL_PER_SECOND / 20f;
				if (!player.isAlive() || !data.inside() || !holdsShield(player, aura) || (!creative && data.soul() < drain)) {
					stop(player);
					continue;
				}
				if (!creative) com.tarkrakov.tearsofink.ink.InkPowers.set(player, data.withSoul(data.soul() - drain));
				int t = aura.age() + 1;
				active.put(id,new Aura(aura.shield(),t,aura.power()));
				refreshAura(player);
				ServerLevel level = player.serverLevel();
				// кольцо искорок по краю ауры
				if (t % 4 == 0) {
					for (int i = 0; i < 6; i++) {
						double a = (t * 0.15) + i * Math.PI / 3;
						level.sendParticles(com.tarkrakov.tearsofink.particle.ModParticles.TEAR_GLINT,
								player.getX() + Math.cos(a) * RADIUS, player.getY() + 0.3, player.getZ() + Math.sin(a) * RADIUS, 1, 0, 0.1, 0, 0);
					}
				}
				if (t % 20 == 0) {
					for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(RADIUS),
							p -> p.isAlive() && (p == player || player.hasLineOfSight(p)) && (player.getTeam() == null || p.getTeam() == null || p.isAlliedTo(player)) && p.distanceToSqr(player) <= RADIUS * RADIUS)) {
						float before = ally.getHealth();
						if (!com.tarkrakov.tearsofink.weapon.shield.ShieldAuraHealing.heal(ally, HEAL_PER_SECOND * aura.power() * com.tarkrakov.tearsofink.ink.SoulPenalty.multiplier(ally), false)) continue;
						com.tarkrakov.tearsofink.vow.SoulFeats.onEmergencyHeal(player, ally, before);
						level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 1.9, ally.getZ(), 1, 0.2, 0.1, 0.2, 0);
					}
					com.tarkrakov.tearsofink.ink.InkPowers.sync(player);
				}
			}
		}
	}

	// ======================================================================
	/** Щит Отпора: светлая волна — слабее Волны меча, зато сильно расталкивает. Дешёвая. */
	public static class RepelAbility extends Skill {
		public static final float DAMAGE = 5f;
		public static final double RANGE = 7;
		public static final double KNOCKBACK = 0.77;

		RepelAbility() {
			super(TearsOfInk.id("repel"), 0, 8, 60);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			ServerLevel level = player.serverLevel();
			Vec3 dir = flatLook(player);
			level.addFreshEntity(new InkWaveEntity(level, player, dir, true, DAMAGE * SoulShieldItem.selectedPower(player, REPEL,input.sneaking()), KNOCKBACK).maxDistance(RANGE - 0.8));
			player.swing(input.sneaking() ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND, true);
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.5f);
			return true;
		}
	}

	// ======================================================================
	/** Длинный нож: Чернильная клякса — шарик чернил вперёд. Дёшево, часто. */
	public static class BlotAbility extends Skill {
		public static final float SPEED = 2.2f;

		BlotAbility() {
			super(TearsOfInk.id("ink_blot"), 3, 0, 16);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			ServerLevel level = player.serverLevel();
			InkBlotEntity blot = new InkBlotEntity(level, player);
			blot.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, SPEED, 1f);
			level.addFreshEntity(blot);
			player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
			level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 0.7f, 1.3f);
			return true;
		}
	}

	// ======================================================================
	/** Расколотая подвеска спасения: мгновенно лечит себя, несильно. */
	public static class SalvationAbility extends Skill {
		public static final float HEAL = 6f;
		/** Перезарядка = длительность «Ослабленной Души» (тики): 8 с. */
		public static final int COOLDOWN = 160;

		/** Длина эффекта: кольцо звёздочек поднимается от ног к голове (тики). */
		public static final int GLOW_TICKS = 20;
		private final Map<UUID, Integer> glow = new HashMap<>();

		SalvationAbility() {
			super(TearsOfInk.id("salvation"), 0, 10, COOLDOWN);
		}

		/** Звёздочки: вспышка + спиральное кольцо, поднимающееся вокруг игрока. */
		void tick(MinecraftServer server) {
			Iterator<Map.Entry<UUID, Integer>> it = glow.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Integer> e = it.next();
				ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
				int t = e.getValue();
				if (p == null || t >= GLOW_TICKS) { it.remove(); continue; }
				ServerLevel level = p.serverLevel();
				var star = com.tarkrakov.tearsofink.particle.ModParticles.SALVATION_STAR;
				if (t == 0) level.sendParticles(star, p.getX(), p.getY() + 1.0, p.getZ(), 24, 0.5, 0.7, 0.5, 0.05);
				double h = 0.1 + 2.0 * t / GLOW_TICKS;
				for (int k = 0; k < 3; k++) {
					double a = t * 0.9 + k * Math.PI * 2 / 3;
					level.sendParticles(star, p.getX() + Math.cos(a) * 0.8, p.getY() + h, p.getZ() + Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
				}
				e.setValue(t + 1);
			}
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			if (player.getHealth() >= player.getMaxHealth()) return false;
			if (player.hasEffect(com.tarkrakov.tearsofink.effect.ModEffects.WEAKENED_SOUL)) return false;
			player.heal(HEAL);
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.tarkrakov.tearsofink.effect.ModEffects.WEAKENED_SOUL,
					COOLDOWN, 0, false, false, true));
			ServerLevel level = player.serverLevel();
			level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
			level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.5f, 1.6f);
			glow.put(player.getUUID(), 0);
			player.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
			return true;
		}
	}
}
