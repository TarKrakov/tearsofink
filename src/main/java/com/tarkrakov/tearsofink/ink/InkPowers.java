package com.tarkrakov.tearsofink.ink;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.advancement.ModAdvancements;
import com.tarkrakov.tearsofink.world.InkLandsAccess;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import com.tarkrakov.tearsofink.effect.ModEffects;

/**
 * Силы чернил: Душа и Чернила игрока, вход/выход из Чернильных Земель, восстановление и синхронизация.
 * Всё работает только внутри области; снаружи значения замораживаются.
 */
public final class InkPowers {
	// ===== Настройки — крутить здесь =====
	/** Вход засчитывается, когда зашёл вглубь области на столько блоков. */
	public static final int ENTER_DEPTH = 12;
	/** Выход засчитывается, когда отошёл наружу на столько блоков. */
	public static final int LEAVE_DEPTH = 12;
	/** Как часто проверять, где игрок (в тиках; 20 тиков = 1 секунда). */
	private static final int ZONE_CHECK_INTERVAL = 10;
	/** Как часто отправлять значения на клиент, пока они меняются (в тиках). */
	private static final int SYNC_INTERVAL = 4;

	public static final AttachmentType<InkData> DATA = AttachmentRegistry.<InkData>builder()
			.persistent(InkData.CODEC)
			.copyOnDeath()
			.initializer(InkData::fresh)
			.buildAndRegister(TearsOfInk.id("ink_data"));

	private InkPowers() {}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(InkSyncPayload.TYPE, InkSyncPayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(InkPowers::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			// на случай если игрок вышел внутри, а зашёл уже в другом месте — сразу проверим
			updateZone(player, false);
			sync(player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			set(newPlayer, get(newPlayer).refilled());
			updateZone(newPlayer, true);
			sync(newPlayer);
		});
	}

	public static InkData get(ServerPlayer player) {
		return player.getAttachedOrCreate(DATA);
	}

	public static void set(ServerPlayer player, InkData data) {
		InkData old = player.getAttached(DATA);
		InkData fresh = data.clamped();
		player.setAttached(DATA, fresh);
		if (old != null) {
			if (fresh.inside() && !old.inside()) ModAdvancements.grant(player, ModAdvancements.INK_LANDS);
			if (fresh.soulMax() < old.soulMax()) ModAdvancements.grant(player, ModAdvancements.CURSE_ESSENCE);
			if (fresh.soulMax() > old.soulMax()) ModAdvancements.grant(player, ModAdvancements.ATONEMENT);
		}
	}

	/** Отправить значения игроку (для полосок). */
	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, InkSyncPayload.of(get(player)));
	}

	/** Изменить значения и сразу отправить игроку. */
	public static void update(ServerPlayer player, InkData data) {
		set(player, data);
		sync(player);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.tickCount % ZONE_CHECK_INTERVAL == 0) {
				updateZone(player, true);
				updateSoulLossIcon(player);
			}
			InkData data = get(player);
			if (!data.inside() || !player.isAlive()) continue;

			// восстановление — только внутри области
			float soul = Math.min(data.soulMax(), data.soul() + InkData.SOUL_REGEN_PER_SECOND / 20f);
			float ink = Math.min(data.inkMax(), data.ink() + InkData.INK_REGEN_PER_SECOND / 20f);
			if (soul != data.soul() || ink != data.ink()) {
				set(player, new InkData(data.soulMax(), soul, data.inkBonus(), ink, true));
				if (player.tickCount % SYNC_INTERVAL == 0 || (soul == data.soulMax() && ink == data.inkMax())) {
					sync(player);
				}
			}
		}
	}

	/**
	 * Значок «Утрата души»: виден, пока игрок внутри области и продал хоть часть души.
	 * Уровень = сколько шагов по 5 продано. Ставится заново, даже если его сняли молоком.
	 */
	private static void updateSoulLossIcon(ServerPlayer player) {
		InkData data = get(player);
		int level = data.inside() ? data.soldSteps() : 0;
		MobEffectInstance current = player.getEffect(ModEffects.SOUL_LOSS);
		if (level <= 0) {
			if (current != null) player.removeEffect(ModEffects.SOUL_LOSS);
			return;
		}
		if (current == null || current.getAmplifier() != level - 1) {
			if (current != null) player.removeEffect(ModEffects.SOUL_LOSS);
			player.addEffect(new MobEffectInstance(ModEffects.SOUL_LOSS, MobEffectInstance.INFINITE_DURATION, level - 1, true, false, true));
		}
	}

	/** Насколько глубоко игрок в Чернильных Землях; {@code null} — в этом измерении их нет. */
	private static Double depth(ServerPlayer player) {
		// Земли — своё измерение: внутри = в этом измерении (край там — туманная петля)
		return com.tarkrakov.tearsofink.world.ModDimensions.isLands(player.level()) ? 1000.0 : null;
	}

	/**
	 * Проверяет, вошёл или вышел ли игрок.
	 * @param announce показать надпись (при входе в игру не показываем)
	 */
	private static void updateZone(ServerPlayer player, boolean announce) {
		InkData data = get(player);
		Double depth = depth(player);
		boolean inside = data.inside();
		if (depth == null) {
			inside = false;
		} else if (!inside && depth >= ENTER_DEPTH) {
			inside = true;
		} else if (inside && depth <= -LEAVE_DEPTH) {
			inside = false;
		}
		if (inside == data.inside()) return;

		update(player, data.withInside(inside));
		if (announce) {
			Component message = inside
					? Component.translatable("message.tears-of-ink.enter").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)
					: Component.translatable("message.tears-of-ink.leave").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
			// только в чат
			player.sendSystemMessage(message);
		}
	}
}
