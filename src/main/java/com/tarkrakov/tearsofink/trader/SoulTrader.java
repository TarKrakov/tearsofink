package com.tarkrakov.tearsofink.trader;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.travel.FountainTravel;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.item.ModItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Торговка душами (фракция Охотников, служит Чернильному Фонтану). Одна сущность — много воплощений:
 * Персонально проступает при второй активации; после вступления — Shift+ПКМ у лично открытого фонтана.
 * Покупает душу (−SOUL_STEP максимума Души → +INK_PER_SOUL_STEP к запасу Чернил) и принимает Исполненные контракты.
 * Тексты — trader.tears-of-ink.* в ru_ru.json.
 */
public final class SoulTrader {
	// ===== Настройки =====
	/** Сколько Пигмента за один Исполненный контракт (пока Охоты нет — контракт есть только в творческом). */
	public static final int PIGMENT_PER_CONTRACT = 3;
	/** Через сколько тиков после появления открывается окно сделки. */
	public static final int OPEN_DELAY = 20;
	/** Исчезает, если рядом (RADIUS) никого нет столько тиков; и в любом случае через MAX_LIFE. */
	public static final int IDLE_TICKS = 20 * 30, MAX_LIFE = 20 * 180;
	public static final double RADIUS = 8, TRADE_RANGE = 6;
	public static final int REQUIRED_FOUNTAINS = 2, INTRO_LINES = 4, INTRO_LINE_TICKS = 100;
	// Persist each delivered line, not a transient entity/session. Survives death and reconnect.
	private static final AttachmentType<Integer> INTRO = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.intRange(0, INTRO_LINES)).copyOnDeath().initializer(() -> 0)
			.buildAndRegister(TearsOfInk.id("trader_intro"));
	private static final Map<UUID, SoulTraderEntity> PERSONAL = new HashMap<>();

	public static final int ACTION_SELL = 1, ACTION_CONTRACTS = 2, ACTION_LEAVE = 3, ACTION_BOARD = 4, /** Исторический код запретной покупки (сохраняем ID старых клиентов, сервер его игнорирует). */
			ACTION_EXCHANGE = 5, /** Старый ID смены обета: сервер игнорирует. */ ACTION_VOW = 6, ACTION_ABANDON = 20,
			/** Особый заказ: взять / отказаться / отдать эссенции надежды. */
			ACTION_ORDER = 7, ACTION_ORDER_ABANDON = 8, ACTION_ESSENCE = 9;
	/** Взять контракт с доски: ACTION_TAKE + номер (0 лёгкий … 2 сложный). */
	public static final int ACTION_TAKE = 10;

	/**
	 * Сервер → клиент: открыть (или обновить) окно Торговки.
	 * vow — путь (0–2) | зарезервированные биты 2–9 (историческая цена смены) | обменяно Души &lt;&lt; 10 | продано шагов &lt;&lt; 16.
 * Формат сетевого сообщения прежний; зарезервированная стоимость теперь равна нулю.
	 */
	/** order — особый заказ: 0 можно взять, 1 идёт, 2+ — перезарядка (минут + 2). */
	public record Open(int entity, int soulMax, int inkMax, int contracts, int vow, int order) implements CustomPacketPayload {
		public static final Type<Open> TYPE = new Type<>(TearsOfInk.id("trader_open"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Open::entity, ByteBufCodecs.VAR_INT, Open::soulMax,
				ByteBufCodecs.VAR_INT, Open::inkMax, ByteBufCodecs.VAR_INT, Open::contracts, ByteBufCodecs.VAR_INT, Open::vow,
				ByteBufCodecs.VAR_INT, Open::order, Open::new);

		public com.tarkrakov.tearsofink.vow.Vow path() { return com.tarkrakov.tearsofink.vow.Vow.byIndex(vow & 3); }
		public int exchanged() { return (vow >> 10) & 0x3F; }
		public int sold() { return (vow >> 16) & 0xFF; }
		/** Историческое поле обратного обмена; новый сервер всегда отправляет 0, действие отключено. */
		public int exchangeBlock() { return (vow >> 24) & 0xF; }

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Клиент → сервер: выбор в окне. */
	public record Action(int entity, int action) implements CustomPacketPayload {
		public static final Type<Action> TYPE = new Type<>(TearsOfInk.id("trader_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Action::entity, ByteBufCodecs.VAR_INT, Action::action, Action::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private SoulTrader() {}

	public static void init() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> dismiss(handler.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> dismiss(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, player, alive) -> dismiss(oldPlayer));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer p) dismiss(p);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (SoulTraderEntity trader : List.copyOf(PERSONAL.values())) trader.discard();
			PERSONAL.clear();
		});
		PayloadTypeRegistry.playS2C().register(Open.TYPE, Open.CODEC);
		PayloadTypeRegistry.playC2S().register(Action.TYPE, Action.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> act(ctx.player(), payload)));
		// Shift+ПКМ по фонтану — до ванильной проверки «присел с предметом в руке → блок не трогаем»
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!player.isSecondaryUseActive() || !level.getBlockState(hit.getBlockPos()).is(ModBlocks.INK_FOUNTAIN)) return InteractionResult.PASS;
			if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
			if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) return InteractionResult.CONSUME;
			summon(sp, server, hit.getBlockPos());
			return InteractionResult.CONSUME;
		});
	}

	private static void say(ServerPlayer p, String key, Object... args) {
		p.sendSystemMessage(Component.translatable("trader.tears-of-ink." + key, args).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
	}

	static boolean eligiblePlayer(ServerPlayer p) {
		return p.isAlive() && !p.isSpectator() && ModDimensions.isLands(p.level()) && InkPowers.get(p).inside();
	}

	public static boolean introduced(ServerPlayer p) {
		return p.getAttachedOrCreate(INTRO) >= INTRO_LINES;
	}

	static boolean unlocked(ServerPlayer p) {
		return FountainTravel.openedCount(p) >= REQUIRED_FOUNTAINS && introduced(p);
	}

	public static boolean canTrade(ServerPlayer p, SoulTraderEntity trader) {
		return eligiblePlayer(p) && trader.getType() == ModMobs.SOUL_TRADER && trader.isAlive()
				&& trader.level() == p.level() && trader.ownedBy(p) && PERSONAL.get(p.getUUID()) == trader
				&& p.distanceTo(trader) <= TRADE_RANGE && unlocked(p);
	}

	static void nextIntroLine(ServerPlayer p) {
		int delivered = p.getAttachedOrCreate(INTRO);
		if (delivered >= INTRO_LINES || FountainTravel.openedCount(p) < REQUIRED_FOUNTAINS) return;
		say(p, "intro." + (delivered + 1));
		p.setAttached(INTRO, delivered + 1);
		if (delivered + 1 == INTRO_LINES) {
			say(p, "intro.complete");
			com.tarkrakov.tearsofink.prologue.FateProgress.mark(p, com.tarkrakov.tearsofink.prologue.FateProgress.Goal.TRADER);
		}
	}

	static void release(SoulTraderEntity trader, UUID owner) {
		if (owner != null) PERSONAL.remove(owner, trader);
	}

	private static void dismiss(ServerPlayer p) {
		SoulTraderEntity trader = PERSONAL.remove(p.getUUID());
		if (trader != null) trader.discard();
	}

	/** Called only after successful ordinary fountain activation; retry pending introductions on later touches. */
	public static void onFountainActivated(ServerPlayer p, ServerLevel level, BlockPos fountain) {
		if (eligiblePlayer(p) && p.level() == level && !introduced(p)
				&& FountainTravel.openedCount(p) >= REQUIRED_FOUNTAINS
				&& FountainTravel.hasOpenedFountain(p, fountain)) manifest(p, level, fountain, true);
	}

	/** Shift+right click. A locked player cannot open any transaction or another player's embodiment. */
	public static void summon(ServerPlayer p, ServerLevel level, BlockPos fountain) {
		if (!eligiblePlayer(p) || p.level() != level) return;
		if (FountainTravel.openedCount(p) < REQUIRED_FOUNTAINS) {
			say(p, "need_second_fountain");
			return;
		}
		if (!FountainTravel.hasOpenedFountain(p, fountain)) {
			say(p, "need_activation");
			return;
		}
		manifest(p, level, fountain, !introduced(p));
	}

	private static void manifest(ServerPlayer p, ServerLevel level, BlockPos fountain, boolean intro) {
		if (!level.getBlockState(fountain).is(ModBlocks.INK_FOUNTAIN)
				|| p.distanceToSqr(fountain.getX()+0.5, fountain.getY()+0.5, fountain.getZ()+0.5) > RADIUS*RADIUS) return;
		SoulTraderEntity existing = PERSONAL.get(p.getUUID());
		if (existing != null && existing.isAlive() && existing.level() == level
				&& existing.fountain().equals(fountain)) {
			existing.keepAlive();
			if (!intro) existing.openFor(p, 0);
			return;
		}
		BlockPos at = standingSpot(level, fountain, p);
		if (at == null) {
			say(p, "no_room");
			return;
		}
		SoulTraderEntity trader = ModMobs.SOUL_TRADER.create(level);
		if (trader == null) return;
		trader.setOwner(p.getUUID());
		double dx = p.getX() - (at.getX() + 0.5), dz = p.getZ() - (at.getZ() + 0.5);
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		trader.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0f);
		trader.setYHeadRot(yaw);
		trader.setYBodyRot(yaw);
		trader.setFountain(fountain);
		if (!level.addFreshEntity(trader)) return;
		if (existing != null) existing.discard();
		PERSONAL.put(p.getUUID(), trader);
		level.sendParticles(p, ParticleTypes.SQUID_INK, false, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 40, 0.3, 0.8, 0.3, 0.02);
		p.playNotifySound(SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 0.8f, 0.5f);
		p.playNotifySound(SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 1.0f, 0.6f);
		if (!intro) {
			say(p, "greet");
			trader.openFor(p, OPEN_DELAY);
		}
	}

	/** Где встать: сбоку от фонтана, лучше — за ним (лицом к игроку). Нужны два блока воздуха и пол. */
	private static BlockPos standingSpot(ServerLevel level, BlockPos fountain, Player p) {
		Direction away = Direction.getNearest(fountain.getX() + 0.5 - p.getX(), 0, fountain.getZ() + 0.5 - p.getZ());
		List<Direction> order = new ArrayList<>(List.of(away, away.getClockWise(), away.getCounterClockWise(), away.getOpposite()));
		for (int dist = 1; dist <= 2; dist++) {
			for (Direction d : order) {
				BlockPos pos = fountain.relative(d, dist);
				for (int dy : new int[]{0, 1, -1}) {
					BlockPos at = pos.above(dy);
					if (level.getBlockState(at).getCollisionShape(level, at).isEmpty()
							&& level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()
							&& !level.getBlockState(at.below()).getCollisionShape(level, at.below()).isEmpty()) return at;
				}
			}
		}
		return null;
	}

	public static int contracts(ServerPlayer p) {
		int n = 0;
		for (ItemStack s : p.getInventory().items) if (s.is(ModItems.FULFILLED_CONTRACT)) n += s.getCount();
		for (ItemStack s : p.getInventory().offhand) if (s.is(ModItems.FULFILLED_CONTRACT)) n += s.getCount();
		return n;
	}

	public static void sendOpen(ServerPlayer p, SoulTraderEntity trader) {
		if (!canTrade(p, trader)) return;
		com.tarkrakov.tearsofink.prologue.DiaryEntries.discover(p, "draft_soul_bargain");
		InkData d = InkPowers.get(p);
		var v = com.tarkrakov.tearsofink.vow.Vows.get(p);
		int packed = v.vow().ordinal() | (Math.min(63, v.exchanged()) << 10) | ((d.sold() + d.taint()) << 16);
		ServerPlayNetworking.send(p, new Open(trader.getId(), d.soulMax(), d.inkMax(), contracts(p), packed,
				com.tarkrakov.tearsofink.hunt.MadOrders.state(p)));
	}

	private static void act(ServerPlayer p, Action a) {
		if (!(p.level().getEntity(a.entity()) instanceof SoulTraderEntity trader) || !canTrade(p, trader)) return;
		trader.keepAlive();
		switch (a.action()) {
			case ACTION_SELL -> sell(p, trader);
			case ACTION_CONTRACTS -> takeContracts(p, trader);
			case ACTION_LEAVE -> trader.farewell(p);
			case ACTION_BOARD -> com.tarkrakov.tearsofink.hunt.HuntContracts.send(p, trader);
			case ACTION_ABANDON -> com.tarkrakov.tearsofink.hunt.HuntContracts.abandon(p, trader);
			case ACTION_ORDER -> {
				com.tarkrakov.tearsofink.hunt.MadOrders.take(p);
				sendOpen(p, trader);
			}
			case ACTION_ORDER_ABANDON -> {
				com.tarkrakov.tearsofink.hunt.MadOrders.abandon(p);
				sendOpen(p, trader);
			}
			case ACTION_ESSENCE -> {
				com.tarkrakov.tearsofink.hunt.MadOrders.tradeEssence(p);
				sendOpen(p, trader);
			}
			default -> {
				if (a.action() >= ACTION_TAKE && a.action() < ACTION_TAKE + 3) {
					com.tarkrakov.tearsofink.hunt.HuntContracts.take(p, trader, a.action() - ACTION_TAKE);
				}
			}
		}
	}

	private static void sell(ServerPlayer p, SoulTraderEntity trader) {
		InkData d = InkPowers.get(p);
		if (!d.inside()) return;
		if (com.tarkrakov.tearsofink.vow.Vows.unexchange(p)) { // Душа выше 100 от обмена — обратный ход с потерей
			InkData now = InkPowers.get(p);
			p.playNotifySound(SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
			say(p, "unexchanged", now.soulMax(), now.inkMax());
			sendOpen(p, trader);
			return;
		}
		if (d.inkMax() >= InkData.INK_CAP) {
			say(p, "ink_full");
			sendOpen(p, trader);
			return;
		}
		if (d.sold() + d.taint() >= (InkData.SOUL_START - InkData.SOUL_MIN) / InkData.SOUL_STEP) {
			say(p, "nothing_left");
			sendOpen(p, trader);
			return;
		}
		InkData after = d.withSold(d.sold() + 1);
		after = after.withSoul(Math.min(after.soul(), after.soulMax())).withInk(after.ink() + InkData.INK_PER_SOUL_STEP);
		InkPowers.update(p, after); // достижение «Суть Проклятья» выдаёт InkPowers
		ServerLevel level = p.serverLevel();
		level.sendParticles(p, ParticleTypes.SQUID_INK, false, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.3, 0.6, 0.3, 0.03);
		level.sendParticles(p, ParticleTypes.SOUL, false, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.2, 0.4, 0.2, 0.02);
		p.playNotifySound(SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
		p.playNotifySound(SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.3f, 1.6f);
		say(p, "sold", after.soulMax(), after.inkMax());
		sendOpen(p, trader);
	}

	/** Сдать выполненные контракты. */
	private static void takeContracts(ServerPlayer p, SoulTraderEntity trader) {
		int n = contracts(p);
		if (n <= 0) {
			say(p, "no_contracts");
			sendOpen(p, trader);
			return;
		}
		// контракт помнит охотника той Охоты — шанс получить его оружие
		java.util.List<ItemStack> weapons = new java.util.ArrayList<>();
		java.util.List<ItemStack> all = new java.util.ArrayList<>(p.getInventory().items);
		all.addAll(p.getInventory().offhand);
		for (ItemStack s : all) {
			if (!s.is(ModItems.FULFILLED_CONTRACT)) continue;
			for (int i = 0; i < s.getCount(); i++) {
				if (p.getRandom().nextFloat() >= com.tarkrakov.tearsofink.hunt.Hunt.weaponChance(s)) continue;
				ItemStack w = com.tarkrakov.tearsofink.hunt.Hunt.weaponOf(s);
				if (!w.isEmpty()) weapons.add(w);
			}
		}
		p.getInventory().clearOrCountMatchingItems(s -> s.is(ModItems.FULFILLED_CONTRACT), -1, p.inventoryMenu.getCraftSlots());
		for (ItemStack w : weapons) {
			say(p, "contract_weapon", w.getHoverName());
			if (!p.getInventory().add(w)) p.drop(w, false);
		}
		ItemStack reward = new ItemStack(ModItems.PIGMENT, n * PIGMENT_PER_CONTRACT);
		if (!p.getInventory().add(reward)) p.drop(reward, false);
		p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.6f);
		say(p, "contracts_taken", n, n * PIGMENT_PER_CONTRACT);
		com.tarkrakov.tearsofink.prologue.FateProgress.mark(p, com.tarkrakov.tearsofink.prologue.FateProgress.Goal.REDEEM);
		sendOpen(p, trader);
	}
}
