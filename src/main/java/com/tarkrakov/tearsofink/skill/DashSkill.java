package com.tarkrakov.tearsofink.skill;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.tarkrakov.tearsofink.particle.ModParticles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Рывок: короткий бросок в сторону движения (стоя — вперёд) с кадрами неуязвимости в начале.
 * Скорость задаётся несколько тиков подряд — так дальность одинаковая и на земле, и в воздухе.
 */
public class DashSkill extends Skill {
	// ===== Настройки — крутить здесь =====
	/** Скорость рывка (блоков за тик) и сколько тиков он длится: 0.9 × 5 ≈ 4.5 блока. */
	public static final double SPEED = 0.9;
	public static final int DURATION_TICKS = 5;
	/** Неуязвимость в начале рывка (тиков; 20 = 1 секунда). */
	public static final int INVULNERABLE_TICKS = 6;
	/** Сколько скорости остаётся после рывка (чтобы не улетать по инерции). */
	public static final double END_SLOWDOWN = 0.3;

	private record Dash(double dx, double dz, int ticksLeft, int invulnerableLeft) {}

	private final Map<UUID, Dash> active = new HashMap<>();

	public DashSkill(ResourceLocation id, int inkCost, int soulCost, int cooldownTicks) {
		super(id, inkCost, soulCost, cooldownTicks);
	}

	void init() {
		ServerTickEvents.END_SERVER_TICK.register(this::tick);
		// кадры неуязвимости: урон в начале рывка не проходит (кроме «пустоты» и /kill)
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			Dash dash = active.get(entity.getUUID());
			return dash == null || dash.invulnerableLeft() <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
		});
	}

	public boolean isInvulnerable(ServerPlayer player) {
		Dash dash = active.get(player.getUUID());
		return dash != null && dash.invulnerableLeft() > 0;
	}

	@Override
	public boolean activate(ServerPlayer player, SkillInput input) {
		if (active.containsKey(player.getUUID()) || player.isFallFlying()) return false;

		// направление как у обычной ходьбы: из нажатых клавиш и поворота головы
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

		active.put(player.getUUID(), new Dash(dx, dz, DURATION_TICKS, INVULNERABLE_TICKS));
		push(player, dx, dz);

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 0.7f, 1.3f);
		level.sendParticles(ModParticles.INK_SMOKE, player.getX(), player.getY() + 0.8, player.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
		return true;
	}

	private void push(ServerPlayer player, double dx, double dz) {
		Vec3 v = player.getDeltaMovement();
		player.setDeltaMovement(dx * SPEED, Math.min(v.y, 0.1), dz * SPEED);
		player.hurtMarked = true; // отправить новую скорость на клиент
	}

	private void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Dash>> it = active.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Dash> e = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
			Dash d = e.getValue();
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			int left = d.ticksLeft() - 1;
			int inv = d.invulnerableLeft() - 1;
			if (left > 0) {
				push(player, d.dx(), d.dz());
				player.serverLevel().sendParticles(ModParticles.INK_SMOKE, player.getX(), player.getY() + 0.6, player.getZ(), 2, 0.2, 0.3, 0.2, 0.0);
			} else if (left == 0) {
				Vec3 v = player.getDeltaMovement();
				player.setDeltaMovement(v.x * END_SLOWDOWN, v.y, v.z * END_SLOWDOWN);
				player.hurtMarked = true;
			}
			if (left <= 0 && inv <= 0) {
				it.remove();
			} else {
				e.setValue(new Dash(d.dx(), d.dz(), left, inv));
			}
		}
	}
}
