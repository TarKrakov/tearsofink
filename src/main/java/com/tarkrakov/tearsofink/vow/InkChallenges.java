package com.tarkrakov.tearsofink.vow;

import com.tarkrakov.tearsofink.advancement.ModAdvancements;
import com.tarkrakov.tearsofink.entity.faction.Faction;
import com.tarkrakov.tearsofink.entity.faction.FactionMember;
import com.tarkrakov.tearsofink.ink.InkPowers;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Необязательные достижения Чернил, не источник энергии. Пять врагов Шпиля/Чернильных охотников
 * за 10 секунд и PvP; оба обрабатываются на сервере, без принуждения к PvP для прогрессии.
 */
public final class InkChallenges {
	public static final int DARKNESS_KILLS = 5, DARKNESS_TICKS = 200;

	private record Streak(int kills, long started) {}
	private static final Map<UUID, Streak> STREAKS = new HashMap<>();

	private InkChallenges() {}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register(InkChallenges::onDeath);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STREAKS.remove(handler.getPlayer().getUUID()));
	}

	private static void onDeath(LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer player) || victim == player
				|| Vows.vow(player) != Vow.INK || !InkPowers.get(player).inside()
				|| player.getAbilities().instabuild) return;
		if (victim instanceof ServerPlayer) {
			ModAdvancements.grant(player, "ink_forbidden_fruit");
			return;
		}
		if (!(victim instanceof FactionMember member)
				|| (member.faction() != Faction.INK && member.faction() != Faction.FERAL)
				|| victim.isAlliedTo(player)) return;
		long now = player.serverLevel().getGameTime();
		Streak old = STREAKS.get(player.getUUID());
		int kills = old == null || now - old.started() >= DARKNESS_TICKS ? 1 : old.kills() + 1;
		long start = old == null || now - old.started() >= DARKNESS_TICKS ? now : old.started();
		if (kills >= DARKNESS_KILLS) {
			ModAdvancements.grant(player, "ink_crusher_of_darkness");
			STREAKS.remove(player.getUUID());
		} else STREAKS.put(player.getUUID(), new Streak(kills, start));
	}
}
