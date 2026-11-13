package com.tarkrakov.tearsofink.travel;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
		return s.is(ModItems.DIARY) || s.is(ModItems.INK_LONGKNIFE) || s.is(ModItems.SALVATION_PENDANT)
				|| s.is(ModItems.DIM_HAND_LANTERN) || s.is(ModItems.WORN_SATCHEL) || s.is(ModItems.SEALED_LETTER)
				|| s.is(ModItems.INVITATION_LETTER);
	}

	/** Вещи, забранные при смерти, — вернуть после возрождения. */
	private static final java.util.Map<java.util.UUID, List<ItemStack>> KEPT = new java.util.HashMap<>();

	private record Toss(ItemEntity item, ServerPlayer owner, int at) {}
	private static final List<Toss> TOSSES = new ArrayList<>();

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
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			dropTickets(server);
			com.tarkrakov.tearsofink.prologue.Prologue.dropTickets(server);
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
			if (server.getTickCount() % 600 == 1) {
				ServerLevel lands = ModDimensions.lands(server);
				if (lands != null) keepRespawnLoaded(lands);
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
		ServerLevel lands = ModDimensions.lands(p.server);
		if (lands == null) return;
		LandsTravel t = get(p);
		Vec3 to;
		float yaw = p.getYRot();
		if (t.lastPos().isPresent()) {
			BlockPos last = t.lastPos().get();
			to = Vec3.atBottomCenterOf(last);
			yaw = t.lastYaw();
		} else {
			to = ModDimensions.arrival(lands, p.getRandom().nextInt(InkLandsLayout.ROADS));
			yaw = ModDimensions.arrivalYaw(lands, to);
		}
		p.teleportTo(lands, to.x, to.y, to.z, yaw, 0f);
		arriveEffects(p);
	}

	/** После смерти в Землях: кровать в Землях → фонтан → край Бледнолесья. */
	/**
	 * Куда возрождать умершего в Землях (сразу в Землях, без захода в обычный мир):
	 * кровать в Землях → привязанный фонтан → край Бледнолесья (у каждого игрока своя дорога — место заранее прогружено).
	 */
	public static net.minecraft.world.level.portal.DimensionTransition landsRespawn(ServerPlayer p,
			net.minecraft.world.level.portal.DimensionTransition.PostDimensionTransition post) {
		ServerLevel lands = ModDimensions.lands(p.server);
		LandsTravel t = get(p);
		Vec3 to = null;
		float yaw = p.getYRot();
		if (t.respawn().isPresent()) {
			BlockPos pos = t.respawn().get();
			BlockState s = lands.getBlockState(pos);
			if (t.respawnBed()) {
				if (s.getBlock() instanceof BedBlock) {
					to = BedBlock.findStandUpPosition(EntityType.PLAYER, lands, pos, s.getValue(BedBlock.FACING), p.getYRot()).orElse(null);
				}
				if (to == null) {
					p.sendSystemMessage(Component.translatable("message.tears-of-ink.lands_bed_gone").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
					set(p, t.withoutRespawn());
				}
			} else if (s.is(com.tarkrakov.tearsofink.block.ModBlocks.INK_FOUNTAIN)) {
				to = standNear(lands, pos);
			}
		}
		if (to == null) {
			to = ModDimensions.arrival(lands, fallbackRoad(p));
			yaw = ModDimensions.arrivalYaw(lands, to);
		}
		return new net.minecraft.world.level.portal.DimensionTransition(lands, to, Vec3.ZERO, yaw, 0f, false, post);
	}

	/** Дорога, на краю которой возрождается игрок без кровати и фонтана (постоянная — чтобы держать место прогруженным). */
	private static int fallbackRoad(ServerPlayer p) {
		return Math.floorMod(p.getUUID().hashCode(), InkLandsLayout.ROADS);
	}

	private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> RESPAWN_TICKET =
			net.minecraft.server.level.TicketType.create("tears_of_ink_respawn", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 20 * 45);

	/** Сразу прогрузить место возрождения (кровать/фонтан), чтобы смерть не подвисала. */
	public static void preloadRespawn(ServerPlayer p, BlockPos at) {
		ServerLevel lands = ModDimensions.lands(p.server);
		if (lands == null) return;
		var cp = new net.minecraft.world.level.ChunkPos(at);
		lands.getChunkSource().addRegionTicket(RESPAWN_TICKET, cp, 3, cp);
		TICKETS.add(cp);
	}

	/** Наши «держи прогруженным» — снимаем при выключении сервера, чтобы сохранение не ждало их генерацию. */
	private static final java.util.Set<net.minecraft.world.level.ChunkPos> TICKETS = java.util.concurrent.ConcurrentHashMap.newKeySet();

	public static void dropTickets(net.minecraft.server.MinecraftServer server) {
		ServerLevel lands = ModDimensions.lands(server);
		if (lands != null) for (var cp : TICKETS) lands.getChunkSource().removeRegionTicket(RESPAWN_TICKET, cp, 3, cp);
		TICKETS.clear();
	}

	/** Пока игрок в Землях — держим место его возрождения прогруженным (возрождение без ожидания). */
	private static void keepRespawnLoaded(ServerLevel lands) {
		InkLandsLayout layout = InkLandsLayout.of(lands);
		for (ServerPlayer p : lands.players()) {
			LandsTravel t = get(p);
			BlockPos at;
			if (t.respawn().isPresent()) {
				at = t.respawn().get();
			} else if (layout != null) {
				double[] a = ModDimensions.arrivalXZ(layout);
				at = BlockPos.containing(a[0], 0, a[1]);
			} else continue;
			var cp = new net.minecraft.world.level.ChunkPos(at);
			lands.getChunkSource().addRegionTicket(RESPAWN_TICKET, cp, 3, cp);
			TICKETS.add(cp);
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

	private static void arriveEffects(ServerPlayer p) {
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.6f);
		p.serverLevel().sendParticles(com.tarkrakov.tearsofink.particle.ModParticles.INK_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.6, 0.4, 0.01);
	}
}
