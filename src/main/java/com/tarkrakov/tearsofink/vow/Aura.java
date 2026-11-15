package com.tarkrakov.tearsofink.vow;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.patrol.PaleSoldier;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.weapon.shield.ShieldStance;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Свет и тьма путей (решено 15 ноя): внутренний свет Пути Души по стадиям высокой Души и тьма Чернил по стадиям души ниже 100.
 * Сервер считает стадии, рассылает их всем, кто видит игрока (вид — на клиенте, AuraClient), и меняет заметность для мобов.
 * Работает только в Чернильных Землях.
 */
public final class Aura {
	// ===== Настройки — крутить здесь =====
	/** Стадии высокой Души (Путь Души): Стойкий / Светлый / Несгибаемый — по максимуму Души. */
	public static final int[] LIGHT_STAGES = {115, 130, 150};
	/** Стадии тьмы (любой путь): Отмеченный / Порченый / Пустой — максимум Души не выше этих. */
	public static final int[] DARK_STAGES = {85, 70, 55};
	/** Светящегося замечают дальше: множитель заметности по ступени света 0…3 (мягко, +10…30%). */
	public static final double[] LIGHT_SEEN = {1.1, 1.15, 1.2, 1.3};
	/** Тень: множитель заметности по стадии тьмы 1…3. */
	public static final double[] SHADOW = {0.85, 0.7, 0.55};
	/** Шпиль охотится на проклятых: солдаты Шпиля тень не видят насквозь, а Пустого замечают дальше. */
	public static final double SPIRE_HUNTS_HOLLOW = 1.25;
	/** Душа ниже этой доли максимума — свет тускнеет (вид и ясность на клиенте). */
	public static final float LOW_SOUL = 0.3f;

	/** Сервер → клиенты: вид игрока. light — ступень света (−1 — нет света; 0 — паломник 100–114; 1–3 — стадии), dark — стадия тьмы 0–3. */
	public record State(int entity, int light, int dark, boolean low) implements CustomPacketPayload {
		public static final Type<State> TYPE = new Type<>(TearsOfInk.id("aura"));
		public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, State::entity, ByteBufCodecs.VAR_INT, State::light, ByteBufCodecs.VAR_INT, State::dark,
				ByteBufCodecs.BOOL, State::low, State::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final Map<UUID, State> LAST = new HashMap<>();

	private Aura() {}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(State.TYPE, State.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Aura::tick);
		EntityTrackingEvents.START_TRACKING.register((entity, viewer) -> {
			if (entity instanceof ServerPlayer p) {
				State s = LAST.get(p.getUUID());
				if (s != null) ServerPlayNetworking.send(viewer, s);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> LAST.remove(h.getPlayer().getUUID()));
		ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> LAST.remove(h.getPlayer().getUUID()));
	}

	/** Ступень света: −1 — не светится; 0 — Путь Души 100–114; 1–3 — стадии высокой Души. */
	public static int light(ServerPlayer p) {
		if (!ModDimensions.isLands(p.level())) return -1;
		if (Vows.vow(p) != Vow.SOUL) return -1;
		int max = InkPowers.get(p).soulMax();
		if (max < InkData.SOUL_START) return -1; // продал душу — свет погас
		int st = 0;
		for (int t : LIGHT_STAGES) if (max >= t) st++;
		return st;
	}

	/** Стадия тьмы 0–3 (по максимуму Души: продажа и Чернильные сердца). */
	public static int dark(ServerPlayer p) {
		if (!ModDimensions.isLands(p.level())) return 0;
		int max = InkPowers.get(p).soulMax(), st = 0;
		for (int t : DARK_STAGES) if (max <= t) st++;
		return st;
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			InkData d = InkPowers.get(p);
			int light = light(p);
			boolean low = light >= 0 && d.soul() < d.soulMax() * LOW_SOUL;
			State s = new State(p.getId(), light, dark(p), low);
			State old = LAST.get(p.getUUID());
			if (s.equals(old)) continue;
			LAST.put(p.getUUID(), s);
			ServerPlayNetworking.send(p, s);
			for (ServerPlayer o : PlayerLookup.tracking(p)) if (o != p) ServerPlayNetworking.send(o, s);
		}
	}

	/**
	 * Заметность игрока для мобов (множитель дальности, на которой его замечают). Миксин в LivingEntity.getVisibilityPercent.
	 */
	public static double visibility(LivingEntity self, @Nullable Entity looker) {
		if (!(self instanceof ServerPlayer p) || !(looker instanceof Mob)) return 1;
		State s = LAST.get(p.getUUID());
		if (s == null) return 1;
		boolean spire = looker instanceof PaleSoldier;
		double m = 1;
		if (s.light() >= 0) m *= LIGHT_SEEN[Math.min(3, s.light())] * (ShieldStance.dual(p) ? 1.05 : 1);
		if (s.dark() > 0) {
			if (!spire) m *= SHADOW[s.dark() - 1];
			else if (s.dark() >= 3) m *= SPIRE_HUNTS_HOLLOW;
		}
		return m;
	}

	/**
	 * Сродство: чернильные существа не трогают Пустого, пока он их не тронет.
	 * Сейчас чернильные гусеницы и так мирные — правило для будущих чернильных существ (вызывать в их выборе цели).
	 */
	public static boolean akin(Mob inkCreature, ServerPlayer p) {
		State s = LAST.get(p.getUUID());
		return s != null && s.dark() >= 3 && inkCreature.getLastHurtByMob() != p;
	}
}
