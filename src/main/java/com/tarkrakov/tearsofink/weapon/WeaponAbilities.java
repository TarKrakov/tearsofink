package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.effect.ModEffects;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillInput;
import com.tarkrakov.tearsofink.skill.SkillPayloads;
import com.tarkrakov.tearsofink.skill.Skills;
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
 * Абилки оружия: Таран и Сияние (щиты Души), Волна и Рассечение (двуручники Чернил).
 * Цифры — в начале каждого класса.
 */
public final class WeaponAbilities {
	public static final RamAbility RAM = new RamAbility();
	public static final RadianceAbility RADIANCE = new RadianceAbility();
	public static final WaveAbility WAVE = new WaveAbility();
	public static final CleaveAbility CLEAVE = new CleaveAbility();
	public static final MercyAbility MERCY = new MercyAbility();
	public static final RepelAbility REPEL = new RepelAbility();
	public static final BlotAbility BLOT = new BlotAbility();
	public static final SalvationAbility SALVATION = new SalvationAbility();

	private WeaponAbilities() {}

	public static void init() {
		PayloadTypeRegistry.playC2S().register(SkillPayloads.UseWeapon.TYPE, SkillPayloads.UseWeapon.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SkillPayloads.UseWeapon.TYPE, (payload, context) ->
				context.player().server.execute(() -> use(context.player(), payload)));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			RAM.tick(server);
			RADIANCE.tick(server);
			CLEAVE.tick(server);
			MERCY.tick(server);
		});
	}

	private static void use(ServerPlayer player, SkillPayloads.UseWeapon payload) {
		if (!player.isAlive() || player.isSpectator()) return;
		ItemStack stack = payload.offhand() ? player.getOffhandItem() : player.getMainHandItem();
		if (!(stack.getItem() instanceof AbilityWeapon weapon)) return;
		// двуручник занимает обе руки: щит в левой молчит
		if (payload.offhand() && player.getMainHandItem().getItem() instanceof InkGreatswordItem) return;
		// аура Милосердия гасится повторным нажатием — бесплатно и без проверок
		if (weapon.ability() == MERCY && MERCY.isActive(player)) {
			MERCY.stop(player);
			return;
		}
		SkillInput input = new SkillInput(payload.strafe(), payload.forward(), payload.offhand());
		if (Skills.activate(player, weapon.ability(), input) && stack.getItem() instanceof InkGreatswordItem) {
			// белая «шторка» перезарядки на иконке меча
			player.getCooldowns().addCooldown(stack.getItem(), weapon.ability().cooldownTicks());
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
		public static final float DAMAGE = 4f;
		public static final double KNOCKBACK = 1.6;
		public static final int SLOW_TICKS = 20;

		private record Ram(double dx, double dz, int ticksLeft, Set<UUID> hit) {}

		private final Map<UUID, Ram> active = new HashMap<>();

		RamAbility() {
			super(TearsOfInk.id("shield_ram"), 0, 15, 80);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			if (active.containsKey(player.getUUID())) return false;
			Vec3 dir = flatLook(player);
			active.put(player.getUUID(), new Ram(dir.x, dir.z, DURATION_TICKS, new HashSet<>()));
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
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, t -> t != player && t.isAlive())) {
					if (!r.hit().add(target.getUUID())) continue;
					target.hurt(player.damageSources().playerAttack(player), DAMAGE);
					target.knockback(KNOCKBACK, -r.dx(), -r.dz());
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 2));
					level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.8f);
				}
				push(player, r.dx(), r.dz());
				level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
				e.setValue(new Ram(r.dx(), r.dz(), r.ticksLeft() - 1, r.hit()));
			}
		}
	}

	// ======================================================================
	/** Светлый щит: на время щит светлеет, входящий урон −40% (пока щит в левой руке). */
	public static class RadianceAbility extends Skill {
		public static final int DURATION_TICKS = 6 * 20;
		/** Сопротивление II = −40% урона. */
		public static final int RESISTANCE_LEVEL = 1;

		private final Map<UUID, Integer> active = new HashMap<>();

		RadianceAbility() {
			super(TearsOfInk.id("radiance"), 0, 20, 240);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			active.put(player.getUUID(), DURATION_TICKS);
			player.addEffect(new MobEffectInstance(ModEffects.RADIANCE, DURATION_TICKS, 0, false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, RESISTANCE_LEVEL, false, false, false));
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.5f);
			return true;
		}

		void tick(MinecraftServer server) {
			Iterator<Map.Entry<UUID, Integer>> it = active.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Integer> e = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
				int left = e.getValue() - 1;
				boolean shieldInHand = player != null && player.getOffhandItem().getItem() instanceof SoulShieldItem;
				if (player == null || left <= 0 || !shieldInHand || !player.isAlive()) {
					if (player != null) {
						player.removeEffect(ModEffects.RADIANCE);
						player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
					}
					it.remove();
					continue;
				}
				if (left % 3 == 0) {
					player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
							2, 0.5, 0.6, 0.5, 0.01);
				}
				e.setValue(left);
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

	// ======================================================================
	/** Меч Рассечения: замах, затем удар сверху вниз — чернила бьют по площадке 3×3 перед игроком. */
	public static class CleaveAbility extends Skill {
		public static final int WINDUP_TICKS = 10;
		public static final float DAMAGE = 12f;
		public static final double KNOCKBACK = 1.0;
		/** Центр удара — на столько блоков перед игроком. */
		public static final double REACH = 2.75;

		private final Map<UUID, Integer> pending = new HashMap<>();

		CleaveAbility() {
			super(TearsOfInk.id("ink_cleave"), 15, 0, 100);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			if (pending.containsKey(player.getUUID())) return false;
			pending.put(player.getUUID(), WINDUP_TICKS);
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WINDUP_TICKS, 3, false, false, false));
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.6f, 0.6f);
			return true;
		}

		void tick(MinecraftServer server) {
			Iterator<Map.Entry<UUID, Integer>> it = pending.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Integer> e = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
				if (player == null || !player.isAlive()) {
					it.remove();
					continue;
				}
				int left = e.getValue() - 1;
				ServerLevel level = player.serverLevel();
				if (left > 0) {
					// замах: чернила собираются над головой
					level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 2.2, player.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
					e.setValue(left);
					continue;
				}
				it.remove();
				strike(player, level);
			}
		}

		private void strike(ServerPlayer player, ServerLevel level) {
			Vec3 dir = flatLook(player);
			Vec3 center = player.position().add(dir.scale(REACH));
			AABB area = new AABB(center.x - 1.5, center.y - 0.5, center.z - 1.5, center.x + 1.5, center.y + 2.0, center.z + 1.5);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, t -> t != player && t.isAlive())) {
				target.hurt(player.damageSources().playerAttack(player), DAMAGE);
				target.knockback(KNOCKBACK, -dir.x, -dir.z);
			}
			player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
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

		private final Map<UUID, Integer> active = new HashMap<>();
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
			active.put(id, 0);
			player.addEffect(new MobEffectInstance(ModEffects.MERCY, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MobEffectInstance.INFINITE_DURATION, 1, false, false, false));
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
			return true;
		}

		public boolean isActive(ServerPlayer player) {
			return active.containsKey(player.getUUID());
		}

		void stop(ServerPlayer player) {
			active.remove(player.getUUID());
			lockedUntil.put(player.getUUID(), player.server.getTickCount() + LOCKOUT_TICKS);
			player.removeEffect(ModEffects.MERCY);
			player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 1.4f);
		}

		private static boolean holdsMercyShield(ServerPlayer player) {
			return isMercy(player.getMainHandItem()) || isMercy(player.getOffhandItem());
		}

		private static boolean isMercy(ItemStack stack) {
			return stack.getItem() instanceof AbilityWeapon w && w.ability() == MERCY;
		}

		void tick(MinecraftServer server) {
			for (UUID id : new java.util.ArrayList<>(active.keySet())) {
				ServerPlayer player = server.getPlayerList().getPlayer(id);
				if (player == null) {
					active.remove(id);
					continue;
				}
				var data = com.tarkrakov.tearsofink.ink.InkPowers.get(player);
				boolean creative = player.getAbilities().instabuild;
				float drain = SOUL_PER_SECOND / 20f;
				if (!player.isAlive() || !data.inside() || !holdsMercyShield(player) || (!creative && data.soul() < drain)) {
					stop(player);
					continue;
				}
				if (!creative) com.tarkrakov.tearsofink.ink.InkPowers.set(player, data.withSoul(data.soul() - drain));
				int t = active.get(id) + 1;
				active.put(id, t);
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
							p -> p.isAlive() && p.distanceToSqr(player) <= RADIUS * RADIUS)) {
						ally.heal(HEAL_PER_SECOND * com.tarkrakov.tearsofink.ink.SoulPenalty.multiplier(ally));
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
		public static final float DAMAGE = 4f;
		public static final double KNOCKBACK = 0.77;

		RepelAbility() {
			super(TearsOfInk.id("repel"), 0, 8, 60);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			ServerLevel level = player.serverLevel();
			Vec3 dir = flatLook(player);
			level.addFreshEntity(new InkWaveEntity(level, player, dir, true, DAMAGE, KNOCKBACK));
			player.swing(input.sneaking() ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND, true);
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.5f);
			return true;
		}
	}

	// ======================================================================
	/** Длинный нож: Чернильная клякса — шарик чернил вперёд. Дёшево, часто. */
	public static class BlotAbility extends Skill {
		public static final float SPEED = 1.6f;

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

		SalvationAbility() {
			super(TearsOfInk.id("salvation"), 0, 10, COOLDOWN);
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			if (player.getHealth() >= player.getMaxHealth()) return false;
			if (player.hasEffect(com.tarkrakov.tearsofink.effect.ModEffects.WEAKENED_SOUL)) return false;
			player.heal(HEAL);
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.tarkrakov.tearsofink.effect.ModEffects.WEAKENED_SOUL,
					COOLDOWN, 0, false, false, true));
			ServerLevel level = player.serverLevel();
			level.sendParticles(com.tarkrakov.tearsofink.particle.ModParticles.TEAR_GLINT, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.4, 0.6, 0.4, 0.02);
			level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
			player.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
			return true;
		}
	}
}
