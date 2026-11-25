package com.tarkrakov.tearsofink.skill;

import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.vow.Vow;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Вознесение духа (белая книга, Путь Души; зеркало Чернильного прыжка): Shift+пробел с земли — дух поднимает тебя высоко.
 * Клиент шлёт сразу после отрыва от земли. Падение после — без урона.
 */
public class SpiritAscensionSkill extends Skill {
	// ===== Настройки — крутить здесь =====
	/** Скорость вверх (обычный прыжок ≈ 0.42; 0.9 — примерно на 4 блока). */
	public static final double UP = 0.9;
	/** Сколько тиков после вознесения падение не ранит. */
	public static final int SAFE_FALL_TICKS = 80;

	private final Map<UUID, Long> safeUntil = new HashMap<>();

	public SpiritAscensionSkill(ResourceLocation id, int inkCost, int soulCost, int cooldownTicks) {
		super(id, inkCost, soulCost, cooldownTicks);
	}

	@Override
	public Vow path() {
		return Vow.SOUL;
	}

	void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!source.is(DamageTypeTags.IS_FALL)) return true;
			Long until = safeUntil.get(entity.getUUID());
			return until == null || until < entity.level().getGameTime();
		});
	}

	@Override
	public boolean activate(ServerPlayer player, SkillInput input) {
		if (player.onGround() || player.isFallFlying() || player.isInWater()) return false; // только на взлёте (клиент шлёт сразу после отрыва)
		com.tarkrakov.tearsofink.weapon.combat.CombatTransitions.begin(player, false);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new SkillPayloads.Boost((float) UP));
		player.resetFallDistance();
		ServerLevel level = player.serverLevel();
		safeUntil.put(player.getUUID(), level.getGameTime() + SAFE_FALL_TICKS);
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.5f);
		level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.6f, 1.2f);
		level.sendParticles(ModParticles.TEAR_GLINT, player.getX(), player.getY() + 0.2, player.getZ(), 20, 0.4, 0.1, 0.4, 0.02);
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(), player.getZ(), 8, 0.3, 0.05, 0.3, 0.02);
		return true;
	}
}
