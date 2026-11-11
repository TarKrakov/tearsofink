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

	private WeaponAbilities() {}

	public static void init() {
		PayloadTypeRegistry.playC2S().register(SkillPayloads.UseWeapon.TYPE, SkillPayloads.UseWeapon.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SkillPayloads.UseWeapon.TYPE, (payload, context) ->
				context.player().server.execute(() -> use(context.player(), payload)));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			RAM.tick(server);
			RADIANCE.tick(server);
			CLEAVE.tick(server);
		});
	}

	private static void use(ServerPlayer player, SkillPayloads.UseWeapon payload) {
		if (!player.isAlive() || player.isSpectator()) return;
		ItemStack stack = payload.offhand() ? player.getOffhandItem() : player.getMainHandItem();
		if (!(stack.getItem() instanceof AbilityWeapon weapon)) return;
		// двуручник занимает обе руки: щит в левой молчит
		if (payload.offhand() && player.getMainHandItem().getItem() instanceof InkGreatswordItem) return;
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
			super(TearsOfInk.id("ink_wave"), 10, 0, 40);
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
		public static final double REACH = 2.2;

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
}
