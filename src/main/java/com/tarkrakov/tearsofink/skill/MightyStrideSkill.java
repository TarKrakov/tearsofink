package com.tarkrakov.tearsofink.skill;

import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.vow.Vow;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Могучая поступь (белая книга, Путь Души; зеркало Рывка, та же клавиша): тяжёлый шаг вперёд — короче и медленнее рывка,
 * без неуязвимости, но щиты остаются поднятыми, а врагов перед тобой отталкивает. Ведёт в бой, а не из боя.
 */
public class MightyStrideSkill extends Skill {
	// ===== Настройки — крутить здесь =====
	/** Скорость шага (блоков за тик) и сколько тиков: 0.75 × 6 ≈ 4,5 блока — как Рывок. */
	public static final double SPEED = 0.75;
	public static final int DURATION_TICKS = 6;
	/** Отбрасывание врагов, задетых шагом. */
	public static final double KNOCKBACK = 1.1;
	/** Сколько скорости остаётся после шага. */
	public static final double END_SLOWDOWN = 0.2;

	private record Step(double dx, double dz, int ticksLeft, Set<UUID> hit) {}

	private final Map<UUID, Step> active = new HashMap<>();
	public boolean cancelForAction(ServerPlayer p) { return active.remove(p.getUUID()) != null; }

	public MightyStrideSkill(ResourceLocation id, int inkCost, int soulCost, int cooldownTicks) {
		super(id, inkCost, soulCost, cooldownTicks);
	}

	@Override
	public Vow path() {
		return Vow.SOUL;
	}

	void init() {
		ServerTickEvents.END_SERVER_TICK.register(this::tick);
	}

	@Override
	public boolean activate(ServerPlayer player, SkillInput input) {
		if (active.containsKey(player.getUUID()) || player.isFallFlying()) return false;
		float strafe = Mth.clamp(input.strafe(), -1f, 1f);
		float forward = Mth.clamp(input.forward(), -1f, 1f);
		if (Math.abs(strafe) < 0.01f && Math.abs(forward) < 0.01f) forward = 1f;
		double len = Math.sqrt(strafe * strafe + forward * forward);
		strafe /= (float) len;
		forward /= (float) len;
		float yaw = player.getYRot() * Mth.DEG_TO_RAD;
		double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
		double dx = strafe * cos - forward * sin;
		double dz = forward * cos + strafe * sin;
		active.put(player.getUUID(), new Step(dx, dz, DURATION_TICKS, new HashSet<>()));
		push(player, dx, dz);
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS, 1.2f, 0.6f);
		level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.6f, 0.5f);
		level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 10, 0.4, 0.05, 0.4, 0.02);
		return true;
	}

	private void push(ServerPlayer player, double dx, double dz) {
		Vec3 v = player.getDeltaMovement();
		player.setDeltaMovement(dx * SPEED, Math.min(v.y, 0.05), dz * SPEED);
		player.hurtMarked = true;
	}

	private static boolean enemy(ServerPlayer p, LivingEntity t) {
		if (t instanceof Player) return false;
		if (t instanceof Mob m && m.getTarget() == p) return true;
		return t instanceof Enemy;
	}

	private void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Step>> it = active.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Step> e = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
			Step s = e.getValue();
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			ServerLevel level = player.serverLevel();
			AABB box = player.getBoundingBox().inflate(0.7, 0.2, 0.7).move(s.dx() * 0.6, 0, s.dz() * 0.6);
			for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, t -> t != player && t.isAlive() && enemy(player, t))) {
				if (!s.hit().add(t.getUUID())) continue;
				t.knockback(KNOCKBACK, -s.dx(), -s.dz());
				level.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.6f);
			}
			int left = s.ticksLeft() - 1;
			if (left > 0) {
				push(player, s.dx(), s.dz());
				level.sendParticles(ModParticles.TEAR_GLINT, player.getX(), player.getY() + 0.8, player.getZ(), 2, 0.3, 0.4, 0.3, 0);
				e.setValue(new Step(s.dx(), s.dz(), left, s.hit()));
			} else {
				Vec3 v = player.getDeltaMovement();
				player.setDeltaMovement(v.x * END_SLOWDOWN, v.y, v.z * END_SLOWDOWN);
				player.hurtMarked = true;
				// удар ноги о землю
				level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 14, 0.6, 0.05, 0.6, 0.03);
				level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS, 1.4f, 0.45f);
				it.remove();
			}
		}
	}
}
