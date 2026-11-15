package com.tarkrakov.tearsofink.skill;

import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.vow.Vow;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Чернильный прыжок (чёрная книга, Путь Чернил): второй прыжок в воздухе — чернила толкают от пустоты. Раз до приземления. */
public class InkJumpSkill extends Skill {
	// ===== Настройки — крутить здесь =====
	/** Скорость вверх второго прыжка (обычный прыжок ≈ 0.42). */
	public static final double UP = 0.5;

	/** Кто уже прыгнул в воздухе (сбрасывается на земле). */
	private final Set<UUID> used = new HashSet<>();

	public InkJumpSkill(ResourceLocation id, int inkCost, int soulCost, int cooldownTicks) {
		super(id, inkCost, soulCost, cooldownTicks);
	}

	@Override
	public Vow path() {
		return Vow.INK;
	}

	void init() {
		ServerTickEvents.END_SERVER_TICK.register(this::tick);
	}

	@Override
	public boolean activate(ServerPlayer player, SkillInput input) {
		if (player.onGround() || player.isFallFlying() || player.isInWater() || player.onClimbable() || used.contains(player.getUUID())) return false;
		used.add(player.getUUID());
		ServerPlayNetworking.send(player, new SkillPayloads.Boost((float) UP)); // скорость игрока знает клиент — толкает он
		player.resetFallDistance();
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 0.7f, 1.6f);
		for (int i = 0; i < 24; i++) {
			double a = player.getRandom().nextDouble() * Math.PI * 2;
			level.sendParticles(ModParticles.INK_SPARK, player.getX(), player.getY(), player.getZ(), 0,
					Math.cos(a) * 0.15, -0.1, Math.sin(a) * 0.15, 1.0);
		}
		return true;
	}

	private void tick(MinecraftServer server) {
		if (used.isEmpty()) return;
		used.removeIf(id -> {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			return p == null || p.onGround() || p.isInWater() || p.onClimbable();
		});
	}
}
