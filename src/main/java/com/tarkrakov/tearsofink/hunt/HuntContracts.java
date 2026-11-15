package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.trader.SoulTraderEntity;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Доска контрактов у Торговки: у каждого игрока своя, три контракта — лёгкий (Новичок, Тусклое оружие),
 * средний (Опытный, Контрастное), сложный (Легендарный, Рубрики). Обновляется раз в 5 минут; взятый контракт
 * освобождает место только при обновлении. Активный контракт у игрока — один.
 * Взял — место в 150–300 блоках от храма, туда ведёт метка; цель и охотник появляются, когда игрок подходит.
 */
public final class HuntContracts {
	/** Один контракт на доске. rank — сложность (0 лёгкий … 2 сложный). */
	public record Offer(int rank, HuntTargets.Target target, PreyPrefix prefix, HunterType type, ItemStack main, ItemStack off) {}

	private static final class Board {
		final Offer[] offers = new Offer[3];
		final boolean[] taken = new boolean[3];
		long refreshAt;
	}

	private static final Map<UUID, Board> BOARDS = new HashMap<>();

	/** Строка доски для окна. */
	public record Entry(int rank, Component target, Component hunter, Component arms, boolean taken) {}

	/** Сервер → клиент: доска контрактов (и активный контракт, если взят). */
	public record BoardData(int entity, List<Entry> entries, int refreshSeconds, boolean hasActive, Component active,
							int activeSeconds) implements CustomPacketPayload {
		public static final Type<BoardData> TYPE = new Type<>(TearsOfInk.id("hunt_board"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BoardData> CODEC = StreamCodec.of((buf, d) -> {
			buf.writeVarInt(d.entity());
			buf.writeVarInt(d.entries().size());
			for (Entry e : d.entries()) {
				buf.writeVarInt(e.rank());
				ComponentSerialization.STREAM_CODEC.encode(buf, e.target());
				ComponentSerialization.STREAM_CODEC.encode(buf, e.hunter());
				ComponentSerialization.STREAM_CODEC.encode(buf, e.arms());
				buf.writeBoolean(e.taken());
			}
			buf.writeVarInt(d.refreshSeconds());
			buf.writeBoolean(d.hasActive());
			ComponentSerialization.STREAM_CODEC.encode(buf, d.active());
			buf.writeVarInt(d.activeSeconds());
		}, buf -> {
			int entity = buf.readVarInt();
			int n = buf.readVarInt();
			List<Entry> list = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				list.add(new Entry(buf.readVarInt(), ComponentSerialization.STREAM_CODEC.decode(buf),
						ComponentSerialization.STREAM_CODEC.decode(buf), ComponentSerialization.STREAM_CODEC.decode(buf), buf.readBoolean()));
			}
			return new BoardData(entity, list, buf.readVarInt(), buf.readBoolean(), ComponentSerialization.STREAM_CODEC.decode(buf), buf.readVarInt());
		});

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private HuntContracts() {}

	static void init() {
		PayloadTypeRegistry.playS2C().register(BoardData.TYPE, BoardData.CODEC);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> BOARDS.clear());
	}

	private static Board board(ServerPlayer p) {
		long now = p.server.getTickCount();
		Board b = BOARDS.computeIfAbsent(p.getUUID(), k -> new Board());
		if (b.offers[0] == null || now >= b.refreshAt) {
			RandomSource random = p.getRandom();
			for (int i = 0; i < 3; i++) {
				HuntTargets.Target t = HuntTargets.pick(random);
				HunterType type = HunterType.byIndex(random.nextInt(HunterType.values().length));
				ItemStack[] arms = Hunt.loadout(type, i, random);
				b.offers[i] = t == null ? null : new Offer(i, t, Hunt.pickPrefix(random), type, arms[0], arms[1]);
				b.taken[i] = false;
			}
			b.refreshAt = now + HuntTuning.BOARD_REFRESH;
		}
		return b;
	}

	/** Открыть доску в окне Торговки. */
	public static void send(ServerPlayer p, SoulTraderEntity trader) {
		Board b = board(p);
		List<Entry> list = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			Offer o = b.offers[i];
			if (o == null) continue;
			Component hunter = Component.translatable("hunt.tears-of-ink.hunter_name",
					Component.translatable("hunt.tears-of-ink.type." + o.type().id), Component.translatable("hunt.tears-of-ink.rank." + o.rank()));
			list.add(new Entry(i, Hunt.preyTitle(o.target().type(), o.prefix()), hunter, Hunt.armsText(o.main(), o.off()), b.taken[i]));
		}
		long now = p.server.getTickCount();
		Hunt.Active a = Hunt.contractOf(p.getUUID());
		Component active = Component.empty();
		int left = 0;
		if (a != null) {
			double dx = p.getX() - a.x, dz = p.getZ() - a.z;
			active = Component.translatable(a.spawned ? "trader.tears-of-ink.board.active_found" : "trader.tears-of-ink.board.active",
					Hunt.preyTitle(a.target.type(), a.prefix), Hunt.hunterName(a), (int) Math.sqrt(dx * dx + dz * dz));
			left = (int) Math.max(0, (a.end - now) / 20);
		}
		ServerPlayNetworking.send(p, new BoardData(trader.getId(), list, (int) Math.max(0, (b.refreshAt - now) / 20), a != null, active, left));
	}

	/** Взять контракт i. */
	public static void take(ServerPlayer p, SoulTraderEntity trader, int i) {
		ServerLevel level = p.serverLevel();
		Board b = board(p);
		if (i < 0 || i > 2 || b.offers[i] == null || b.taken[i]) {
			send(p, trader);
			return;
		}
		if (Hunt.contractOf(p.getUUID()) != null) {
			say(p, "board.busy");
			send(p, trader);
			return;
		}
		double[] at = place(level, trader, p.getRandom());
		if (at == null) {
			say(p, "board.no_place");
			send(p, trader);
			return;
		}
		Offer o = b.offers[i];
		if (!Hunt.startContract(level, p, o, at[0], at[1])) {
			send(p, trader);
			return;
		}
		b.taken[i] = true;
		level.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.5f);
		level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.3f, 1.6f);
		say(p, "board.accepted", Hunt.preyTitle(o.target().type(), o.prefix()), HuntTuning.CONTRACT_TIME / 20 / 60);
		send(p, trader);
	}

	/** Отказаться от активного контракта. */
	public static void abandon(ServerPlayer p, SoulTraderEntity trader) {
		if (Hunt.contractOf(p.getUUID()) == null) {
			send(p, trader);
			return;
		}
		Hunt.abandon(p.serverLevel(), p.getUUID());
		say(p, "board.abandoned");
		send(p, trader);
	}

	/** Место контракта: 150–300 блоков от храма, внутри Земель, не у другого храма. */
	private static double[] place(ServerLevel level, SoulTraderEntity trader, RandomSource random) {
		InkLandsLayout layout = InkLandsLayout.of(level);
		for (int attempt = 0; attempt < 64; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double d = HuntTuning.CONTRACT_DIST_MIN + random.nextDouble() * (HuntTuning.CONTRACT_DIST_MAX - HuntTuning.CONTRACT_DIST_MIN);
			double x = trader.getX() + Math.cos(angle) * d, z = trader.getZ() + Math.sin(angle) * d;
			if (layout != null && (layout.depthInside((int) x, (int) z) < 30 || Sanctuary.near(layout, x, z, 16))) continue;
			return new double[]{x, z};
		}
		return null;
	}

	private static void say(ServerPlayer p, String key, Object... args) {
		p.sendSystemMessage(Component.translatable("trader.tears-of-ink." + key, args).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
	}
}
