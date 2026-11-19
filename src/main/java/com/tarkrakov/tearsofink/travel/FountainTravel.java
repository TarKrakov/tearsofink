package com.tarkrakov.tearsofink.travel;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.hunt.Hunt;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tarkrakov.tearsofink.travel.FountainTravelPackets.PAGE_SIZE;

/** All mutable transport state and decisions belong to the server thread. */
public final class FountainTravel {
	static final int PREPARATION_TICKS = DimMirrorItem.USE_TICKS;
	private static final int COOLDOWN_TICKS = 600;
	private static final int PVP_TICKS = 100;
	private static final int LOAD_TIMEOUT_TICKS = 200;
	private static final int MENU_TICKS = 600;
	private static final TicketType<UUID> TICKET = TicketType.create(
			"tears-of-ink:fountain_travel", UUID::compareTo, LOAD_TIMEOUT_TICKS + 20);

	private static final Map<ServerPlayer, Menu> MENUS = new HashMap<>();
	private static final Map<ServerPlayer, Transfer> TRANSFERS = new HashMap<>();
	private static final Map<UUID, Integer> PVP = new HashMap<>();
	private static final Map<ServerPlayer, Integer> HURT = new HashMap<>();

	private enum Phase { HOLDING, PREPARING, LOADING }

	private static final class Menu {
		final UUID session = UUID.randomUUID();
		final ServerLevel level;
		final BlockPos source;
		final int expires;
		final List<BlockPos> destinations;
		int page;

		Menu(ServerPlayer player, BlockPos source, FountainTravelData data) {
			this.level = player.serverLevel();
			this.source = source.immutable();
			this.expires = player.server.getTickCount() + MENU_TICKS;
			this.destinations = data.opened().stream().map(BlockPos::of).filter(pos -> FountainLanding.inside(level, pos))
					.sorted(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getY))
					.toList();
		}

		int pages() { return Math.max(1, Mth.positiveCeilDiv(destinations.size(), PAGE_SIZE)); }
	}

	private static final class Transfer {
		final ServerLevel level;
		final BlockPos source;
		final BlockPos destination;
		final InteractionHand hand;
		final ItemStack mirror;
		final int slot;
		final int start;
		final List<ChunkPos> chunks;
		Phase phase;
		int timeout;

		Transfer(ServerPlayer player, BlockPos source, BlockPos destination, InteractionHand hand) {
			this.level = player.serverLevel();
			this.source = source;
			this.destination = destination;
			this.hand = hand;
			this.mirror = player.getItemInHand(hand);
			this.slot = player.getInventory().selected;
			this.start = player.server.getTickCount();
			this.chunks = FountainLanding.requiredChunks(destination);
			this.phase = source == null ? Phase.HOLDING : Phase.PREPARING;
		}
	}

	private FountainTravel() {}

	public static void init() {
		FountainTravelData.init();
		PayloadTypeRegistry.playS2C().register(FountainTravelPackets.Menu.TYPE, FountainTravelPackets.Menu.CODEC);
		PayloadTypeRegistry.playS2C().register(FountainTravelPackets.Close.TYPE, FountainTravelPackets.Close.CODEC);
		PayloadTypeRegistry.playC2S().register(FountainTravelPackets.Select.TYPE, FountainTravelPackets.Select.CODEC);
		PayloadTypeRegistry.playC2S().register(FountainTravelPackets.Close.TYPE, FountainTravelPackets.Close.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(FountainTravelPackets.Close.TYPE,
				(packet, context) -> context.server().execute(() ->
						MENUS.computeIfPresent(context.player(), (player, menu) -> menu.session.equals(packet.session()) ? null : menu)));
		ServerPlayNetworking.registerGlobalReceiver(FountainTravelPackets.Select.TYPE,
				(packet, context) -> context.server().execute(() -> select(context.player(), packet)));
		// UseItem is before the vanilla mirror cooldown. Block interaction still belongs to the block or the trader.
		UseItemCallback.EVENT.register((player, level, hand) -> level.isClientSide
				? InteractionResultHolder.pass(player.getItemInHand(hand)) : useMirror(player, level, hand));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> FountainTravelData.get(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			clear(handler.player);
			HURT.remove(handler.player);
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> clear(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, player, alive) -> clear(oldPlayer));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, dealt, blocked) -> {
			if (dealt > 0) damaged(entity, source);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			damaged(entity, source);
			if (entity instanceof ServerPlayer player) clear(player);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int now = server.getTickCount();
			for (ServerPlayer player : List.copyOf(TRANSFERS.keySet())) tick(player, now);
			MENUS.entrySet().removeIf(entry -> now >= entry.getValue().expires);
			PVP.values().removeIf(until -> now >= until);
			HURT.values().removeIf(until -> now >= until);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerPlayer player : List.copyOf(TRANSFERS.keySet())) clear(player);
			MENUS.clear();
			PVP.clear();
			HURT.clear();
		});
	}

	/** Read existing personal travel discoveries; no chunk loads. */
	public static boolean hasOpenedFountain(ServerPlayer player) {
		return !FountainTravelData.get(player).opened().isEmpty();
	}

	/** Personal unique manual activations, including fountains at the Ink cap. */
	/** Match the planned temple centre to the personally activated fountain, without loading chunks. */
	public static boolean hasOpenedNear(ServerPlayer player, int x, int z, int radius) {
		for (long packed : FountainTravelData.get(player).opened()) {
			BlockPos pos = BlockPos.of(packed);
			long dx = (long) pos.getX() - x, dz = (long) pos.getZ() - z;
			if (dx * dx + dz * dz <= (long) radius * radius) return true;
		}
		return false;
	}

	public static int openedCount(ServerPlayer player) {
		return FountainTravelData.get(player).opened().size();
	}

	public static boolean hasOpenedFountain(ServerPlayer player, BlockPos pos) {
		return FountainTravelData.get(player).opened().contains(pos.asLong());
	}

	public static void activate(ServerPlayer player, BlockPos pos) {
		if (ModDimensions.isLands(player.level()) && !player.isSpectator()) {
			FountainTravelData.get(player).activate(player, pos);
			com.tarkrakov.tearsofink.prologue.FateProgress.refresh(player, false);
		}
	}

	public static void open(ServerPlayer player, BlockPos source) {
		if (!ModDimensions.isLands(player.level()) || player.isSpectator() || player.isUsingItem() || !Travel.isMirror(player.getMainHandItem())
				|| !validSource(player, player.serverLevel(), source)) return;
		FountainTravelData data = FountainTravelData.get(player);
		if (!data.opened().contains(source.asLong())) {
			tell(player, "touch");
			return;
		}
		if (TRANSFERS.containsKey(player)) return;
		if (!ServerPlayNetworking.canSend(player, FountainTravelPackets.Menu.TYPE)) return;
		Menu menu = new Menu(player, source, data);
		MENUS.put(player, menu);
		sendPage(player, menu, data);
	}

	private static void sendPage(ServerPlayer player, Menu menu, FountainTravelData data) {
		int first = menu.page * PAGE_SIZE;
		List<BlockPos> rows = menu.destinations.subList(first, Math.min(first + PAGE_SIZE, menu.destinations.size()));
		ServerPlayNetworking.send(player, new FountainTravelPackets.Menu(menu.session, menu.source, data.last(), menu.page, menu.pages(), rows));
	}

	private static void select(ServerPlayer player, FountainTravelPackets.Select packet) {
		Menu menu = MENUS.get(player);
		if (menu == null || !menu.session.equals(packet.session()) || TRANSFERS.containsKey(player)) return;
		FountainTravelData data = FountainTravelData.get(player);
		if (player.server.getTickCount() >= menu.expires || player.isUsingItem() || player.isSecondaryUseActive() || !Travel.isMirror(player.getMainHandItem())
				|| !data.opened().contains(menu.source.asLong()) || !validSource(player, menu.level, menu.source)) {
			close(player, menu);
			tell(player, "source");
			return;
		}
		if (packet.page() < 0 || packet.page() >= menu.pages()) return;
		if (packet.row() == -1) {
			menu.page = packet.page();
			sendPage(player, menu, data);
			return;
		}
		if (packet.page() != menu.page || packet.row() < 0 || packet.row() >= PAGE_SIZE) return;
		int index = menu.page * PAGE_SIZE + packet.row();
		if (index >= menu.destinations.size()) return;
		BlockPos destination = menu.destinations.get(index);
		if (destination.equals(menu.source) || !data.opened().contains(destination.asLong())) return;
		String reason = refusal(player, data);
		if (reason != null) {
			tell(player, reason);
			return;
		}
		Transfer transfer = new Transfer(player, menu.source, destination, InteractionHand.MAIN_HAND);
		TRANSFERS.put(player, transfer);
		close(player, menu);
		progress(player, transfer, 0);
	}

	private static void close(ServerPlayer player, Menu menu) {
		MENUS.remove(player);
		ServerPlayNetworking.send(player, new FountainTravelPackets.Close(menu.session));
	}

	public static InteractionResultHolder<ItemStack> useMirror(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!ModDimensions.isLands(level) || player.isSpectator() || !Travel.isMirror(stack)) return InteractionResultHolder.pass(stack);
		// A UseItem packet after a fountain click must never additionally begin a mirror channel.
		if (player.pick(player.blockInteractionRange(), 1f, false) instanceof BlockHitResult block
				&& block.getType() == HitResult.Type.BLOCK && level.getBlockState(block.getBlockPos()).is(ModBlocks.INK_FOUNTAIN)) {
			return InteractionResultHolder.fail(stack);
		}
		if (!player.isSecondaryUseActive()) {
			if (player instanceof ServerPlayer server && !returning(server)) {
				MENUS.remove(server);
				cancel(server, "cancelled");
			}
			return InteractionResultHolder.pass(stack);
		}
		if (player instanceof ServerPlayer server) {
			if (TRANSFERS.containsKey(server) || server.isUsingItem()) return InteractionResultHolder.fail(stack);
			FountainTravelData data = FountainTravelData.get(server);
			BlockPos destination = data.last().or(() -> data.legacyBinding()).orElse(null);
			String reason = refusal(server, data);
			if (reason == null && (destination == null || !data.opened().contains(destination.asLong()))) reason = "no_last";
			if (reason == null && !FountainLanding.inside(server.serverLevel(), destination)) reason = "missing";
			if (reason != null) {
				tell(server, reason);
				return InteractionResultHolder.fail(stack);
			}
			Transfer transfer = new Transfer(server, null, destination, hand);
			TRANSFERS.put(server, transfer);
			progress(server, transfer, 0);
		}
		DimMirrorItem.startUse(level, player, hand);
		return InteractionResultHolder.success(stack);
	}

	static boolean returning(ServerPlayer player) {
		Transfer transfer = TRANSFERS.get(player);
		return transfer != null && transfer.phase == Phase.HOLDING;
	}

	static boolean finishMirror(ServerPlayer player) {
		Transfer transfer = TRANSFERS.get(player);
		if (transfer == null || transfer.phase != Phase.HOLDING) return false;
		String reason = invalid(player, transfer);
		if (reason == null) requestChunk(player, transfer, player.server.getTickCount());
		else cancel(player, reason);
		return true;
	}

	static void releaseMirror(ServerPlayer player) {
		if (returning(player)) cancel(player, "cancelled");
	}

	private static void tick(ServerPlayer player, int now) {
		Transfer transfer = TRANSFERS.get(player);
		String reason = invalid(player, transfer);
		if (reason != null) {
			cancel(player, reason);
			return;
		}
		int elapsed = now - transfer.start;
		switch (transfer.phase) {
			case HOLDING -> {
				if (elapsed % 5 == 0) progress(player, transfer, elapsed);
			}
			case PREPARING -> {
				if (elapsed >= PREPARATION_TICKS) requestChunk(player, transfer, now);
				else if (elapsed % 5 == 0) progress(player, transfer, elapsed);
			}
			case LOADING -> {
				if (now >= transfer.timeout) cancel(player, "timeout");
				else if (transfer.chunks.stream().allMatch(chunk ->
						transfer.level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null)) arrive(player, transfer);
			}
		}
	}

	private static String invalid(ServerPlayer player, Transfer transfer) {
		if (player.level() != transfer.level || !player.isAlive() || player.isRemoved()) return "cancelled";
		if (player.getItemInHand(transfer.hand) != transfer.mirror || !Travel.isMirror(transfer.mirror)
				|| transfer.hand == InteractionHand.MAIN_HAND && player.getInventory().selected != transfer.slot) return "mirror";
		if (transfer.phase == Phase.HOLDING && (!player.isUsingItem() || player.getUsedItemHand() != transfer.hand)) return "cancelled";
		if (transfer.source != null && (player.isShiftKeyDown() || player.isUsingItem())) return "cancelled";
		if (transfer.source != null && !validSource(player, transfer.level, transfer.source)) return "source";
		FountainTravelData data = FountainTravelData.get(player);
		if (!data.opened().contains(transfer.destination.asLong()) || !FountainLanding.inside(transfer.level, transfer.destination)) return "missing";
		if (transfer.source == null && data.last().or(() -> data.legacyBinding()).filter(transfer.destination::equals).isEmpty()) return "cancelled";
		return refusal(player, data);
	}

	private static String refusal(ServerPlayer player, FountainTravelData data) {
		if (!ModDimensions.isLands(player.level()) || !player.isAlive() || player.isSpectator() || player.isSleeping()) return "cancelled";
		if (player.isPassenger() || player.isVehicle()) return "riding";
		int now = player.server.getTickCount();
		if (PVP.getOrDefault(player.getUUID(), now) > now || HURT.getOrDefault(player, now) > now
				|| player.hurtTime > 0 || Travel.hunted(player)) return "combat";
		if (Hunt.blocksFountainTravel(player)) return "hunt";
		if (data.cooldownUntil() > time(player)) return "cooldown";
		return null;
	}

	private static boolean validSource(ServerPlayer player, ServerLevel level, BlockPos source) {
		if (!player.isAlive() || player.level() != level || !FountainLanding.inside(level, source)
				|| !player.canInteractWithBlock(source, 0) || level.getChunkSource().getChunkNow(source.getX() >> 4, source.getZ() >> 4) == null
				|| !level.getBlockState(source).is(ModBlocks.INK_FOUNTAIN)) return false;
		Vec3 base = Vec3.atBottomCenterOf(source);
		for (double height : new double[]{0.3, 0.65}) {
			BlockHitResult sight = level.clip(new ClipContext(player.getEyePosition(), base.add(0, height, 0),
					ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
			if (sight.getType() == HitResult.Type.BLOCK && sight.getBlockPos().equals(source)) return true;
		}
		return false;
	}

	private static void requestChunk(ServerPlayer player, Transfer transfer, int now) {
		transfer.phase = Phase.LOADING;
		transfer.timeout = now + LOAD_TIMEOUT_TICKS;
		for (ChunkPos chunk : transfer.chunks) {
			transfer.level.getChunkSource().addRegionTicket(TICKET, chunk, 0, player.getUUID());
		}
		tell(player, "loading");
	}

	private static void arrive(ServerPlayer player, Transfer transfer) {
		if (!transfer.level.getBlockState(transfer.destination).is(ModBlocks.INK_FOUNTAIN)) {
			cancel(player, "missing");
			return;
		}
		BlockPos feet = FountainLanding.find(transfer.level, player, transfer.destination);
		if (feet == null) {
			cancel(player, "unsafe");
			return;
		}
		String reason = invalid(player, transfer);
		if (reason != null) {
			cancel(player, reason);
			return;
		}
		Vec3 at = Vec3.atBottomCenterOf(feet);
		float yaw = (float) Math.toDegrees(Math.atan2(feet.getX() - transfer.destination.getX(), transfer.destination.getZ() - feet.getZ()));
		try {
			if (!player.teleportTo(transfer.level, at.x, at.y, at.z, Set.of(), yaw, 0)) {
				tell(player, "unsafe");
				return;
			}
			player.setDeltaMovement(Vec3.ZERO);
			player.resetFallDistance();
			FountainTravelData.get(player).cooldown(player, time(player) + COOLDOWN_TICKS);
			Travel.arriveEffects(player);
			tell(player, "arrived");
		} finally {
			cancel(player, null);
		}
	}

	private static void damaged(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof ServerPlayer player)) return;
		HURT.put(player, player.server.getTickCount() + 10);
		cancel(player, "damaged");
		if (source.getEntity() instanceof ServerPlayer attacker && attacker != player) {
			int until = player.server.getTickCount() + PVP_TICKS;
			PVP.put(player.getUUID(), until);
			PVP.put(attacker.getUUID(), until);
			cancel(attacker, "combat");
		}
	}

	private static void clear(ServerPlayer player) {
		MENUS.remove(player);
		cancel(player, null);
	}

	private static void cancel(ServerPlayer player, String reason) {
		Transfer transfer = TRANSFERS.remove(player);
		if (transfer == null) return;
		if (transfer.phase == Phase.LOADING) {
			for (ChunkPos chunk : transfer.chunks) {
				transfer.level.getChunkSource().removeRegionTicket(TICKET, chunk, 0, player.getUUID());
			}
		}
		if (transfer.phase == Phase.HOLDING && player.isUsingItem() && player.getUsedItemHand() == transfer.hand
				&& player.getUseItem() == transfer.mirror) player.stopUsingItem();
		if (reason != null) tell(player, reason);
	}

	private static long time(ServerPlayer player) {
		return player.server.overworld().getGameTime();
	}

	private static void progress(ServerPlayer player, Transfer transfer, int elapsed) {
		player.displayClientMessage(Component.translatable("fountain.tears-of-ink." + (transfer.source == null ? "holding" : "preparing"),
				Math.min(100, elapsed * 100 / PREPARATION_TICKS)), true);
	}

	private static void tell(ServerPlayer player, String reason) {
		player.displayClientMessage(Component.translatable("fountain.tears-of-ink." + reason), true);
	}
}
