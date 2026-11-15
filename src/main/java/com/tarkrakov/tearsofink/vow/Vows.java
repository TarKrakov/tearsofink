package com.tarkrakov.tearsofink.vow;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.travel.Guide;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Обеты (решено 15 ноя, документ «обеты-путь-души-и-чернил»): выбор пути у Проводника, «сон» чужих умений,
 * рост Души на Пути Души (пробуждения), обмен Чернил на Душу и смена обета у Торговки.
 * Тексты — vow.tears-of-ink.* и guide.tears-of-ink.vow.* в ru_ru.json (заглушки Claude).
 */
public final class Vows {
	// ===== Настройки — крутить здесь =====
	/** Обмен у Торговки: столько запаса Чернил отдаёшь … */
	public static final int EXCHANGE_INK = 10;
	/** … и столько Души получаешь. */
	public static final int EXCHANGE_SOUL = 5;
	/** Сколько всего Души можно получить обменом. */
	public static final int EXCHANGE_LIMIT = 10;
	/** Ниже этого запас Чернил обменом не опустить. */
	public static final int EXCHANGE_INK_FLOOR = 30;
	/** Продажа души, когда Душа выше 100 за счёт обмена: Чернил возвращается только столько (обратный ход с потерей). */
	public static final int UNEXCHANGE_INK = 5;
	/** Смена обета у Торговки: первая бесплатно, дальше — столько Пигмента (заглушка). */
	public static final int CHANGE_PIGMENT = 10;
	/** Пробуждение Души — обычное +5, у Авангарда и финального босса +10. */
	public static final int AWAKENING = 5, AWAKENING_GREAT = 10;
	/** Как часто напоминать о выборе тем, кто в Землях без обета (тики). */
	private static final int REMIND_TICKS = 20 * 30;

	public static final AttachmentType<VowData> DATA = AttachmentRegistry.<VowData>builder()
			.persistent(VowData.CODEC)
			.copyOnDeath()
			.initializer(VowData::fresh)
			.buildAndRegister(TearsOfInk.id("vow"));

	/** Сервер → клиент: обет своего игрока. */
	public record Sync(int vow) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(TearsOfInk.id("vow_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Sync::vow, Sync::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Сервер → клиент: открыть окно выбора обета. */
	public record Open() implements CustomPacketPayload {
		public static final Type<Open> TYPE = new Type<>(TearsOfInk.id("vow_open"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Клиент → сервер: выбранный обет. */
	public record Choose(int vow) implements CustomPacketPayload {
		public static final Type<Choose> TYPE = new Type<>(TearsOfInk.id("vow_choose"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Choose> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Choose::vow, Choose::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Когда последний раз открывали окно (чтобы не заваливать). */
	private static final Map<UUID, Integer> PROMPTED = new HashMap<>();

	private Vows() {}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(Sync.TYPE, Sync.CODEC);
		PayloadTypeRegistry.playS2C().register(Open.TYPE, Open.CODEC);
		PayloadTypeRegistry.playC2S().register(Choose.TYPE, Choose.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Choose.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> choose(ctx.player(), Vow.byIndex(payload.vow()))));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.getPlayer();
			sync(p);
			PROMPTED.remove(p.getUUID());
			if (InkPowers.get(p).inside()) prompt(p);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PROMPTED.remove(handler.getPlayer().getUUID()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerTickEvents.END_SERVER_TICK.register(Vows::tick);
	}

	public static VowData get(ServerPlayer p) {
		return p.getAttachedOrCreate(DATA);
	}

	public static Vow vow(ServerPlayer p) {
		return get(p).vow();
	}

	private static void set(ServerPlayer p, VowData d) {
		p.setAttached(DATA, d);
		sync(p);
	}

	public static void sync(ServerPlayer p) {
		ServerPlayNetworking.send(p, new Sync(vow(p).ordinal()));
	}

	// ---------------- выбор ----------------

	/** Открыть окно выбора, если обета ещё нет (не чаще раза в несколько секунд). */
	public static boolean prompt(ServerPlayer p) {
		if (vow(p) != Vow.NONE) return false;
		int now = p.server.getTickCount();
		Integer last = PROMPTED.get(p.getUUID());
		if (last != null && now - last < 60) return false;
		PROMPTED.put(p.getUUID(), now);
		ServerPlayNetworking.send(p, new Open());
		return true;
	}

	/** Без обета дальше не пройти (ворота Преддверия). */
	public static boolean mayPass(ServerPlayer p) {
		return vow(p) != Vow.NONE;
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % REMIND_TICKS != 0) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (vow(p) == Vow.NONE && InkPowers.get(p).inside()) prompt(p); // уже прошедшие пролог — при входе и раз в полминуты
		}
	}

	private static void choose(ServerPlayer p, Vow v) {
		if (v == Vow.NONE || vow(p) != Vow.NONE) return;
		set(p, get(p).withVow(v));
		Guide.say(p, "vow.chosen." + v.id);
		mark(p, v);
	}

	/** Звук и свет обета. */
	private static void mark(ServerPlayer p, Vow v) {
		ServerLevel level = p.serverLevel();
		if (v == Vow.SOUL) {
			level.sendParticles(ModParticles.TEAR_GLINT, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.7, 0.4, 0.02);
			level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
			level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
		} else {
			level.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.3, 0.6, 0.3, 0.03);
			level.playSound(null, p.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1f, 0.7f);
			level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
		}
	}

	// ---------------- умения пути ----------------

	/** Умение доступно этому игроку: общее — всем, путевое — только своему обету. */
	public static boolean allows(ServerPlayer p, Skill s) {
		Vow path = s.path();
		return path == Vow.NONE || vow(p) == path;
	}

	/** Сообщение «книга спит» / «строки расплываются». */
	public static void asleep(ServerPlayer p, Skill s, boolean reading) {
		p.displayClientMessage(Component.translatable(reading ? "vow.tears-of-ink.book_blurred" : "vow.tears-of-ink.skill_asleep",
				Component.translatable(s.nameKey())).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
		p.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
	}

	// ---------------- пробуждения (Путь Души) ----------------

	/**
	 * Пробудить Душу (раз за игру на каждый id). Только на Пути Души — на Пути Чернил ничего не происходит.
	 * @return пробудилась ли
	 */
	public static boolean awaken(ServerPlayer p, String id, int amount) {
		VowData d = get(p);
		if (d.vow() != Vow.SOUL || d.awakenings().contains(id)) return false;
		InkData ink = InkPowers.get(p);
		set(p, d.withAwakening(id));
		InkPowers.update(p, ink.withExtra(ink.extra() + amount).withSoul(ink.soul() + amount));
		p.sendSystemMessage(Component.translatable("vow.tears-of-ink.awakening." + id, amount).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
		ServerLevel level = p.serverLevel();
		level.sendParticles(ModParticles.TEAR_GLINT, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.4, 0.8, 0.4, 0.03);
		level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
		level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4f, 0.7f);
		level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.6f);
		return true;
	}

	/** Отзовётся ли Чернильное сердце (мини-боссы и Авангард): только не на Пути Души. */
	public static boolean heartAnswers(ServerPlayer p) {
		return vow(p) != Vow.SOUL;
	}

	// ---------------- Торговка ----------------

	/** Почему нельзя обменять Чернила на Душу (ключ причины) или null — можно. */
	public static String exchangeBlock(ServerPlayer p) {
		InkData ink = InkPowers.get(p);
		VowData d = get(p);
		if (ink.sold() > 0) return "sold";
		if (d.exchanged() >= EXCHANGE_LIMIT) return "limit";
		if (ink.inkMax() - EXCHANGE_INK < EXCHANGE_INK_FLOOR) return "ink";
		return null;
	}

	/** Обменять: −EXCHANGE_INK к запасу Чернил → +EXCHANGE_SOUL Души. */
	public static boolean exchange(ServerPlayer p) {
		if (exchangeBlock(p) != null) return false;
		InkData ink = InkPowers.get(p);
		set(p, get(p).withExchanged(get(p).exchanged() + EXCHANGE_SOUL));
		InkPowers.update(p, ink.withInkBonus(ink.inkBonus() - EXCHANGE_INK).withExtra(ink.extra() + EXCHANGE_SOUL)
				.withSoul(ink.soul() + EXCHANGE_SOUL));
		return true;
	}

	/**
	 * Продажа шага души, когда Душа выше 100 за счёт обмена: обмен откатывается с потерей.
	 * @return true — откатили обмен (обычную продажу делать не надо)
	 */
	public static boolean unexchange(ServerPlayer p) {
		VowData d = get(p);
		if (d.exchanged() <= 0) return false;
		InkData ink = InkPowers.get(p);
		set(p, d.withExchanged(d.exchanged() - EXCHANGE_SOUL));
		InkPowers.update(p, ink.withExtra(ink.extra() - EXCHANGE_SOUL).withInkBonus(ink.inkBonus() + UNEXCHANGE_INK)
				.withInk(ink.ink() + UNEXCHANGE_INK));
		return true;
	}

	/** Цена смены обета в Пигменте (0 — бесплатно). */
	public static int changeCost(ServerPlayer p) {
		return get(p).changes() == 0 ? 0 : CHANGE_PIGMENT;
	}

	/** Сменить обет на другой путь. @return получилось ли (хватило Пигмента). */
	public static boolean change(ServerPlayer p) {
		VowData d = get(p);
		if (d.vow() == Vow.NONE) {
			prompt(p);
			return false;
		}
		int cost = changeCost(p);
		if (cost > 0 && !p.getAbilities().instabuild) {
			if (count(p) < cost) return false;
			p.getInventory().clearOrCountMatchingItems(s -> s.is(ModItems.PIGMENT), cost, p.inventoryMenu.getCraftSlots());
		}
		Vow next = d.vow().other();
		set(p, d.withVow(next).withChanges(d.changes() + 1));
		mark(p, next);
		return true;
	}

	private static int count(ServerPlayer p) {
		int n = 0;
		for (ItemStack s : p.getInventory().items) if (s.is(ModItems.PIGMENT)) n += s.getCount();
		for (ItemStack s : p.getInventory().offhand) if (s.is(ModItems.PIGMENT)) n += s.getCount();
		return n;
	}

	/** Сбросить обет (команда для тестов). */
	public static void reset(ServerPlayer p) {
		VowData d = get(p);
		InkData ink = InkPowers.get(p);
		int awakened = ink.extra() - d.exchanged();
		InkPowers.update(p, ink.withExtra(0).withInkBonus(ink.inkBonus() + d.exchanged() / EXCHANGE_SOUL * EXCHANGE_INK));
		set(p, VowData.fresh());
		PROMPTED.remove(p.getUUID());
		if (awakened < 0) TearsOfInk.LOGGER.warn("vow reset: extra < exchanged for {}", p.getName().getString());
	}
}
