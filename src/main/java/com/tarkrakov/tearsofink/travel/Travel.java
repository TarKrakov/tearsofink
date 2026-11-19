package com.tarkrakov.tearsofink.travel;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Переходы между обычным миром и Землями (зеркальце), возрождение в Землях,
 * кровати Земель (только запоминают место), неотделимость зеркальца.
 */
public final class Travel {
	/** Реплик при выбрасывании зеркальца (guide.tears-of-ink.toss.1 … N). */
	public static final int TOSS_LINES = 7;
	/** После стольких выбрасываний подряд — скрытая реплика. */
	public static final int TOSS_TIRED = 10;
	/** Через сколько тиков выброшенное зеркальце возвращается. */
	public static final int TOSS_RETURN_TICKS = 16;

	public static final AttachmentType<LandsTravel> DATA = AttachmentRegistry.<LandsTravel>builder()
			.persistent(LandsTravel.CODEC)
			.copyOnDeath()
			.initializer(LandsTravel::fresh)
			.buildAndRegister(TearsOfInk.id("lands_travel"));

	/** Начальный набор и предметы пролога: не теряются при смерти (но в сундук класть можно). */
	public static boolean keptOnDeath(ItemStack s) {
		return s.is(ModItems.DIARY) || s.is(ModItems.INK_LONGKNIFE) || s.is(ModItems.SALVATION_PENDANT) || s.is(ModItems.DIM_SCIMITAR)
				|| s.is(ModItems.WORN_RAM_SHIELD) || s.is(ModItems.WORN_MERCY_SHIELD) || s.is(ModItems.PILGRIM_BUNDLE)
				|| s.is(ModItems.DIM_HAND_LANTERN) || s.is(ModItems.WORN_SATCHEL) || s.is(ModItems.SEALED_LETTER)
				|| s.is(ModItems.INVITATION_LETTER);
	}

	/** Вещи, забранные при смерти, — вернуть после возрождения. */
	private static final java.util.Map<java.util.UUID, List<ItemStack>> KEPT = new java.util.HashMap<>();

	private record Toss(ItemEntity item, ServerPlayer owner, int at) {}
	private static final List<Toss> TOSSES = new ArrayList<>();

	private static final int LOAD_TIMEOUT_TICKS = 1200;
	private static final int RESPAWN_RADIUS = 1;
	private static final TicketType<UUID> RESPAWN_TICKET = TicketType.create("tears_of_ink_respawn", UUID::compareTo, 200);
	private static final TicketType<UUID> MIRROR_TICKET = TicketType.create("tears_of_ink_mirror", UUID::compareTo, LOAD_TIMEOUT_TICKS + 20);
	private static final Map<ServerPlayer, ChunkPos> RESPAWN_CHUNKS = new HashMap<>();
	private static final Set<ServerPlayer> RESPAWNING = new HashSet<>();
	private static final Map<ServerPlayer, MirrorArrival> MIRROR_ARRIVALS = new HashMap<>();
	private static final ThreadLocal<ServerLevel> RESPAWN_CONSTRUCTION = new ThreadLocal<>();

	private record MirrorArrival(ServerLevel level, Optional<BlockPos> last, float yaw, int deadline) {}
	private record Spawn(Vec3 pos, float yaw, boolean lostBed) {}

	private Travel() {}

	public static LandsTravel get(ServerPlayer p) {
		return p.getAttachedOrCreate(DATA);
	}

	public static void set(ServerPlayer p, LandsTravel t) {
		p.setAttached(DATA, t);
	}

	public static boolean isMirror(ItemStack stack) {
		return stack.is(ModItems.DIM_MIRROR);
	}

	/** Правила «неотделимости» действуют вне творческого режима. */
	public static boolean bound(ServerPlayer p) {
		return !p.isCreative() && !p.isSpectator();
	}

	public static void init() {
		FountainTravel.init();
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerPlayer player : List.copyOf(RESPAWN_CHUNKS.keySet())) releaseRespawn(player);
			for (ServerPlayer player : List.copyOf(MIRROR_ARRIVALS.keySet())) clearMirrorArrival(player);
			RESPAWNING.clear();
			ServerLevel lands = ModDimensions.lands(server);
			if (lands != null) ModDimensions.dropArrivalTickets(lands);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearLoading(handler.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p, from, to) -> {
			clearLoading(p);
			if (ModDimensions.isLands(to)) warmRespawn(p);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ModDimensions.isLands(handler.player.level())) warmRespawn(handler.player);
		});

		// ----- смерть: зеркальце остаётся при игроке -----
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer p && bound(p)
					&& !p.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
				int count = 0;
				List<ItemStack> kept = new ArrayList<>();
				var inv = p.getInventory();
				for (int i = 0; i < inv.getContainerSize(); i++) {
					ItemStack s = inv.getItem(i);
					if (isMirror(s)) {
						count += s.getCount();
						inv.setItem(i, ItemStack.EMPTY);
					} else if (keptOnDeath(s)) {
						kept.add(s.copy());
						inv.setItem(i, ItemStack.EMPTY);
					}
				}
				if (count > 0) set(p, get(p).withKeepMirror(count));
				if (!kept.isEmpty()) KEPT.put(p.getUUID(), kept);
			}
			return true;
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			clearLoading(oldPlayer);
			if (ModDimensions.isLands(newPlayer.level())) warmRespawn(newPlayer);
			LandsTravel t = get(newPlayer);
			if (t.keepMirror() > 0) {
				newPlayer.getInventory().add(new ItemStack(ModItems.DIM_MIRROR, t.keepMirror()));
				set(newPlayer, t = t.withKeepMirror(0));
			}
			List<ItemStack> kept = KEPT.remove(newPlayer.getUUID());
			if (kept != null) for (ItemStack s : kept) if (!newPlayer.getInventory().add(s)) newPlayer.drop(s, false);
			// умер в Землях — возрождение сразу в Землях (см. ServerPlayerRespawnMixin)
		});

		// ----- кровати в Землях: только запоминают место -----
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!ModDimensions.isLands(level) || player.isSpectator()) return InteractionResult.PASS;
			BlockState state = level.getBlockState(hit.getBlockPos());
			if (!(state.getBlock() instanceof BedBlock) || player.isShiftKeyDown()) return InteractionResult.PASS;
			if (player instanceof ServerPlayer sp) {
				BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? hit.getBlockPos()
						: hit.getBlockPos().relative(state.getValue(BedBlock.FACING));
				set(sp, get(sp).withRespawn(head, true));
				preloadRespawn(sp, head);
				sp.sendSystemMessage(Component.translatable("message.tears-of-ink.lands_bed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			}
			return InteractionResult.SUCCESS;
		});

		// ----- зеркальце нельзя повесить в рамку -----
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (entity instanceof ItemFrame && isMirror(player.getItemInHand(hand)) && player instanceof ServerPlayer sp && bound(sp)) {
				MirrorBond.refuseStash(sp);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});

		// ----- выброшенное зеркальце возвращается -----
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof ItemEntity item) || !isMirror(item.getItem())) return;
			if (!(item.getOwner() instanceof ServerPlayer owner) || !bound(owner)) return;
			item.setNoGravity(true);
			item.setDeltaMovement(item.getDeltaMovement().scale(0.3));
			item.setPickUpDelay(40);
			item.setInvulnerable(true);
			TOSSES.add(new Toss(item, owner, owner.server.getTickCount() + TOSS_RETURN_TICKS));
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickLoading(server.getTickCount());
			if (server.getTickCount() % 100 == 1) {
				ServerLevel lands = ModDimensions.lands(server);
				if (lands != null) for (ServerPlayer p : lands.players()) warmRespawn(p);
			}
			if (TOSSES.isEmpty()) return;
			int now = server.getTickCount();
			TOSSES.removeIf(t -> {
				ItemEntity item = t.item();
				if (item.isRemoved()) return true;
				if (item.level() instanceof ServerLevel sl) {
					item.setDeltaMovement(item.getDeltaMovement().scale(0.6));
					sl.sendParticles(ParticleTypes.ENCHANT, item.getX(), item.getY() + 0.3, item.getZ(), 2, 0.1, 0.1, 0.1, 0.4);
				}
				if (now < t.at()) return false;
				returnTossed(t);
				return true;
			});
		});
	}

	private static void returnTossed(Toss t) {
		ServerPlayer owner = t.owner();
		ItemStack stack = t.item().getItem().copy();
		t.item().discard();
		if (owner.isRemoved() || owner.hasDisconnected()) return;
		if (!owner.getInventory().add(stack)) owner.drop(stack, false); // инвентарь полон — пусть упадёт у ног (вернётся снова)
		owner.level().playSound(null, owner.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.6f);
		LandsTravel data = get(owner);
		int tosses = data.tosses() + 1;
		set(owner, data.withTosses(tosses));
		if (tosses == TOSS_TIRED) Guide.say(owner, "toss.tired");
		else Guide.sayRandom(owner, "toss", TOSS_LINES);
	}

	/** Кто-нибудь держит игрока целью (тогда зеркальце не работает). */
	public static boolean hunted(ServerPlayer p) {
		return !p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p && m.isAlive()).isEmpty();
	}

	/** Переход через зеркальце: из Земель — домой, отовсюду ещё — в Земли. */
	public static void mirrorTravel(ServerPlayer p) {
		LandsTravel t = get(p).withTosses(0);
		set(p, t);
		if (com.tarkrakov.tearsofink.prologue.Vestibule.is(p.level())) { // в Преддверии зеркало не нужно — ворота перед тобой
			Guide.say(p, "mirror_vestibule");
			return;
		}
		if (ModDimensions.isLands(p.level())) { goHome(p); return; }
		if (t.stage() < LandsTravel.STAGE_SIGNED && !p.isCreative()) {
			p.sendSystemMessage(Component.translatable("message.tears-of-ink.mirror_unsigned").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			return;
		}
		if (t.stage() == LandsTravel.STAGE_SIGNED) { com.tarkrakov.tearsofink.prologue.Prologue.enterVestibule(p); return; }
		goLands(p);
	}

	private static void goHome(ServerPlayer p) {
		set(p, get(p).withLast(p.blockPosition(), p.getYRot()));
		ServerLevel overworld = p.server.overworld();
		Optional<Vec3> bed = Optional.empty();
		BlockPos rp = p.getRespawnPosition();
		if (rp != null && p.getRespawnDimension() == Level.OVERWORLD) {
			BlockState s = overworld.getBlockState(rp);
			if (s.getBlock() instanceof BedBlock) {
				bed = BedBlock.findStandUpPosition(EntityType.PLAYER, overworld, rp, s.getValue(BedBlock.FACING), p.getRespawnAngle());
			}
		}
		Vec3 to;
		if (bed.isPresent()) {
			to = bed.get();
		} else {
			BlockPos s = overworld.getSharedSpawnPos();
			to = ModDimensions.onGround(overworld, s.getX(), s.getZ());
			p.sendSystemMessage(Component.translatable("message.tears-of-ink.mirror_no_home").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		}
		p.teleportTo(overworld, to.x, to.y, to.z, p.getYRot(), p.getXRot());
		arriveEffects(p);
	}


	private static void goLands(ServerPlayer p) {
		LandsTravel t = get(p);
		queueArrival(p, t.lastPos(), t.lastYaw());
	}

	public static void enterTemple(ServerPlayer p) {
		queueArrival(p, Optional.empty(), p.getYRot());
	}

	private static void queueArrival(ServerPlayer p, Optional<BlockPos> last, float yaw) {
		ServerLevel lands = ModDimensions.lands(p.server);
		if (lands == null || MIRROR_ARRIVALS.containsKey(p)) return;
		var request = new MirrorArrival(lands, last, yaw, p.server.getTickCount() + LOAD_TIMEOUT_TICKS);
		if (tryMirrorArrival(p, request)) return;
		MIRROR_ARRIVALS.put(p, request);
		if (request.last().isPresent()) {
			var chunk = new ChunkPos(request.last().get());
			lands.getChunkSource().addRegionTicket(MIRROR_TICKET, chunk, 0, p.getUUID());
		} else {
			ModDimensions.preloadArrival(lands);
		}
		waiting(p, "destination_loading");
	}

	private static boolean tryMirrorArrival(ServerPlayer p, MirrorArrival request) {
		Vec3 to;
		float yaw;
		if (request.last().isPresent()) {
			BlockPos pos = request.last().get();
			if (!ModDimensions.chunksReady(request.level(), pos, 0)) return false;
			to = Vec3.atBottomCenterOf(pos);
			yaw = request.yaw();
		} else {
			var arrival = ModDimensions.readyArrival(request.level());
			if (arrival == null) return false;
			to = arrival.pos();
			yaw = arrival.yaw();
		}
		p.teleportTo(request.level(), to.x, to.y, to.z, yaw, 0f);
		arriveEffects(p);
		return true;
	}

	/** Delay only the vanilla death packet until its original destination is ready. */
	public static boolean deferRespawn(ServerPlayer p) {
		if (respawnTarget(p, false) != null) return false;
		if (RESPAWNING.add(p)) waiting(p, "respawn_loading");
		prepareRespawn(p);
		return true;
	}

	public static ServerPlayer createRespawnPlayer(ServerLevel level, Supplier<ServerPlayer> create) {
		if (!ModDimensions.isLands(level)) return create.get();
		ServerLevel previous = RESPAWN_CONSTRUCTION.get();
		RESPAWN_CONSTRUCTION.set(level);
		try {
			return create.get();
		} finally {
			if (previous == null) RESPAWN_CONSTRUCTION.remove();
			else RESPAWN_CONSTRUCTION.set(previous);
		}
	}

	public static boolean skipProvisionalSpawn(ServerLevel level) {
		return RESPAWN_CONSTRUCTION.get() == level;
	}

	private static void tickLoading(int now) {
		for (ServerPlayer p : List.copyOf(RESPAWNING)) {
			// A dead entity can be removed from its level while the same player is
			// still waiting on the connection. Only discard a genuinely stale request.
			if (p.hasDisconnected() || p.server.getPlayerList().getPlayer(p.getUUID()) != p
					|| p.isAlive() || !ModDimensions.isLands(p.level())) {
				RESPAWNING.remove(p);
				continue;
			}
			// Keep the request until AFTER_RESPAWN acknowledges actual replacement.
			// A temporarily rejected/redeferred vanilla command is not completion.
			// Retry at most once per second, refreshing the existing bounded tickets.
			if (now % 20 != 0) continue;
			prepareRespawn(p);
			if (respawnTarget(p, false) != null) {
				p.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
			} else if (now % 100 == 0) {
				waiting(p, "respawn_loading");
			}
		}
		for (ServerPlayer p : List.copyOf(MIRROR_ARRIVALS.keySet())) {
			var request = MIRROR_ARRIVALS.get(p);
			if (!p.isAlive() || p.hasDisconnected()) {
				clearMirrorArrival(p);
			} else if (now >= request.deadline()) {
				clearMirrorArrival(p);
				waiting(p, "destination_timeout");
			} else if (tryMirrorArrival(p, request)) {
				clearMirrorArrival(p);
			} else if (now % 20 == 0 && request.last().isEmpty()) {
				ModDimensions.preloadArrival(request.level());
			}
		}
	}

	private static void prepareRespawn(ServerPlayer p) {
		warmRespawn(p);
		if (get(p).respawn().filter(pos -> ModDimensions.chunksReady(p.serverLevel(), pos, 3)).isPresent()) {
			// A loaded but unusable binding needs the original fallback as well.
			ModDimensions.preloadArrival(p.serverLevel());
		}
	}

	private static void waiting(ServerPlayer p, String key) {
		p.displayClientMessage(Component.translatable("message.tears-of-ink." + key), true);
	}

	private static void clearLoading(ServerPlayer p) {
		RESPAWNING.remove(p);
		releaseRespawn(p);
		clearMirrorArrival(p);
	}

	private static void clearMirrorArrival(ServerPlayer p) {
		var request = MIRROR_ARRIVALS.remove(p);
		if (request != null) request.last().ifPresent(pos -> request.level().getChunkSource()
				.removeRegionTicket(MIRROR_TICKET, new ChunkPos(pos), 0, p.getUUID()));
	}

	/** Minecraft still constructs the DimensionTransition and replaces the player in its regular respawn path. */
	public static DimensionTransition landsRespawn(ServerPlayer p, DimensionTransition.PostDimensionTransition post) {
		Spawn target = respawnTarget(p, true);
		if (target.lostBed()) {
			p.sendSystemMessage(Component.translatable("message.tears-of-ink.lands_bed_gone").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			set(p, get(p).withoutRespawn());
		}
		return new DimensionTransition(p.serverLevel(), target.pos(), Vec3.ZERO, target.yaw(), 0f, false, post);
	}

	@Nullable
	private static Spawn respawnTarget(ServerPlayer p, boolean load) {
		ServerLevel lands = p.serverLevel();
		LandsTravel t = get(p);
		boolean lostBed = false;
		if (t.respawn().isPresent()) {
			BlockPos pos = t.respawn().get();
			if (!load && !ModDimensions.chunksReady(lands, pos, 3)) return null;
			BlockState s = lands.getBlockState(pos);
			if (t.respawnBed()) {
				if (s.getBlock() instanceof BedBlock) {
					var at = BedBlock.findStandUpPosition(EntityType.PLAYER, lands, pos, s.getValue(BedBlock.FACING), p.getYRot());
					if (at.isPresent()) return new Spawn(at.get(), p.getYRot(), false);
				}
				lostBed = true;
			} else if (s.is(ModBlocks.INK_FOUNTAIN)) {
				return new Spawn(standNear(lands, pos), p.getYRot(), false);
			}
		}
		if (load) {
			Vec3 to = ModDimensions.arrival(lands, 0);
			return new Spawn(to, ModDimensions.arrivalYaw(lands, to), lostBed);
		}
		var arrival = ModDimensions.readyArrival(lands);
		return arrival == null ? null : new Spawn(arrival.pos(), arrival.yaw(), lostBed);
	}

	/** Refresh just one binding per player. The starting temple is shared. */
	public static void preloadRespawn(ServerPlayer p, BlockPos at) {
		var chunk = new ChunkPos(at);
		if (!chunk.equals(RESPAWN_CHUNKS.get(p))) {
			releaseRespawn(p);
			RESPAWN_CHUNKS.put(p, chunk);
		}
		p.serverLevel().getChunkSource().addRegionTicket(RESPAWN_TICKET, chunk, RESPAWN_RADIUS, p.getUUID());
	}

	private static void releaseRespawn(ServerPlayer p) {
		ChunkPos chunk = RESPAWN_CHUNKS.remove(p);
		ServerLevel lands = ModDimensions.lands(p.server);
		if (chunk != null && lands != null) lands.getChunkSource().removeRegionTicket(RESPAWN_TICKET, chunk, RESPAWN_RADIUS, p.getUUID());
	}

	private static void warmRespawn(ServerPlayer p) {
		var binding = get(p).respawn();
		if (binding.isPresent()) {
			preloadRespawn(p, binding.get());
		} else {
			releaseRespawn(p);
			ModDimensions.preloadArrival(p.serverLevel());
		}
	}
	/** Свободное место рядом с блоком (для фонтана). */
	private static Vec3 standNear(ServerLevel level, BlockPos pos) {
		for (BlockPos c : BlockPos.withinManhattan(pos, 2, 1, 2)) {
			if (c.equals(pos)) continue;
			if (level.getBlockState(c.below()).isFaceSturdy(level, c.below(), net.minecraft.core.Direction.UP)
					&& level.getBlockState(c).getCollisionShape(level, c).isEmpty()
					&& level.getBlockState(c.above()).getCollisionShape(level, c.above()).isEmpty()) {
				return Vec3.atBottomCenterOf(c);
			}
		}
		return Vec3.atBottomCenterOf(pos.above());
	}

	static void arriveEffects(ServerPlayer p) {
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.6f);
		p.serverLevel().sendParticles(com.tarkrakov.tearsofink.particle.ModParticles.INK_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.6, 0.4, 0.01);
	}
}
