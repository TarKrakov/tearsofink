package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.travel.Guide;
import com.tarkrakov.tearsofink.travel.LandsTravel;
import com.tarkrakov.tearsofink.travel.Travel;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Пролог: письмо при первом входе → распечатать → подписать контракт (окно) → зеркальце ведёт в Преддверие →
 * Г-н Проводник дарит саквояж → ворота → край Бледнолесья.
 */
public final class Prologue {
	/** Игрок нажал «подписать» в окне письма. */
	public record SignContract() implements CustomPacketPayload {
		public static final Type<SignContract> TYPE = new Type<>(TearsOfInk.id("sign_contract"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SignContract> CODEC = StreamCodec.unit(new SignContract());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Переход через ворота: игрок замирает, экран затягивает бирюзовой пеленой (клиент), потом перенос. */
	public record GateTransit(int ticks) implements CustomPacketPayload {
		public static final Type<GateTransit> TYPE = new Type<>(TearsOfInk.id("gate_transit"));
		public static final StreamCodec<RegistryFriendlyByteBuf, GateTransit> CODEC =
				net.minecraft.network.codec.ByteBufCodecs.VAR_INT.map(GateTransit::new, GateTransit::ticks).cast();

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Сколько длится переход сквозь ворота (тики). */
	public static final int TRANSIT_TICKS = 60;
	private static final net.minecraft.resources.ResourceLocation FREEZE = TearsOfInk.id("gate_freeze");
	/** Кто сейчас проходит ворота: игрок → тиков осталось. */
	private static final java.util.Map<java.util.UUID, Integer> TRANSIT = new java.util.HashMap<>();
	/** Куда игрок попадёт из Преддверия (выбрано заранее, чтобы место успело прогрузиться). */
	private static final java.util.Map<java.util.UUID, Integer> ARRIVAL_ROAD = new java.util.HashMap<>();
	private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> ARRIVAL_TICKET =
			net.minecraft.server.level.TicketType.create("tears_of_ink_arrival", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 20 * 180);

	private Prologue() {}

	public static void init() {
		DiaryEntries.init();
		PayloadTypeRegistry.playC2S().register(SignContract.TYPE, SignContract.CODEC);
		PayloadTypeRegistry.playS2C().register(GateTransit.TYPE, GateTransit.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SignContract.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> sign(ctx.player())));

		// первый вход в мир — письмо (каждому игроку один раз)
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.getPlayer();
			LandsTravel t = Travel.get(p);
			if (t.stage() == 0) {
				PrologueItems.give(p, ModItems.SEALED_LETTER);
				Travel.set(p, t.withStage(LandsTravel.STAGE_LETTER));
			}
		});

		// ворота Преддверия
		ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (!Vestibule.is(level) || level.getGameTime() % 4 != 0) return;
			Vestibule v = Vestibule.get(level);
			for (ServerPlayer p : java.util.List.copyOf(level.players())) { // копия: игрок уходит в другое измерение прямо в цикле
				if (p.isSpectator()) continue;
				Integer left = TRANSIT.get(p.getUUID());
				if (left != null) {
					left -= 4;
					if (left <= 0) {
						TRANSIT.remove(p.getUUID());
						unfreeze(p);
						passGate(p);
					} else {
						TRANSIT.put(p.getUUID(), left);
					}
				} else if (p.getBoundingBox().intersects(v.gate().inflate(0.2))) {
					startTransit(p);
				}
				else if (p.getY() < level.getMinBuildHeight() + 20) { // упал с островка — обратно
					BlockPos a = v.arrival();
					p.teleportTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
					p.resetFallDistance();
				}
			}
		});
	}

	private static void startTransit(ServerPlayer p) {
		TRANSIT.put(p.getUUID(), TRANSIT_TICKS);
		preloadArrival(p);
		var speed = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		var jump = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
		var mod = new net.minecraft.world.entity.ai.attributes.AttributeModifier(FREEZE, -1.0,
				net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		if (speed != null && !speed.hasModifier(FREEZE)) speed.addTransientModifier(mod);
		if (jump != null && !jump.hasModifier(FREEZE)) jump.addTransientModifier(mod);
		p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		p.hurtMarked = true;
		ServerPlayNetworking.send(p, new GateTransit(TRANSIT_TICKS));
		p.playNotifySound(SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.5f, 1.4f);
	}

	private static void unfreeze(ServerPlayer p) {
		var speed = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		var jump = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
		if (speed != null) speed.removeModifier(FREEZE);
		if (jump != null) jump.removeModifier(FREEZE);
	}

	/** Заранее загрузить место прибытия на краю Бледнолесья (в фоне), чтобы переход не подвисал. */
	private static void preloadArrival(ServerPlayer p) {
		ServerLevel lands = ModDimensions.lands(p.server);
		if (lands == null) return;
		int road = ARRIVAL_ROAD.computeIfAbsent(p.getUUID(), k -> p.getRandom().nextInt(InkLandsLayout.ROADS));
		InkLandsLayout layout = InkLandsLayout.of(lands);
		if (layout == null) return;
		double[] a = layout.arrivalPoint(road);
		var cp = new net.minecraft.world.level.ChunkPos(BlockPos.containing(a[0], 0, a[1]));
		lands.getChunkSource().addRegionTicket(ARRIVAL_TICKET, cp, 6, cp); // с запасом: чтобы и вид вокруг успел сгенерироваться
	}

	private static void sign(ServerPlayer p) {
		LandsTravel t = Travel.get(p);
		if (t.stage() >= LandsTravel.STAGE_SIGNED) return;
		var inv = p.getInventory();
		int slot = -1;
		for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ModItems.INVITATION_LETTER)) { slot = i; break; }
		if (slot < 0) return;
		inv.getItem(slot).shrink(1);
		if (!inv.contains(new ItemStack(ModItems.DIM_MIRROR))) PrologueItems.give(p, ModItems.DIM_MIRROR);
		Travel.set(p, t.withStage(LandsTravel.STAGE_SIGNED));
		// письмо вылетает и сгорает
		ServerLevel level = p.serverLevel();
		Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(0.9));
		level.sendParticles(ModParticles.PAPER_SCRAP, at.x, at.y, at.z, 14, 0.25, 0.25, 0.25, 0.03);
		level.sendParticles(ModParticles.EMBER, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.02);
		level.sendParticles(ModParticles.INK_SMOKE, at.x, at.y + 0.2, at.z, 8, 0.2, 0.2, 0.2, 0.01);
		level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.3f);
		p.sendSystemMessage(Component.translatable("message.tears-of-ink.contract_signed").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
	}

	/** Первый взгляд в зеркальце после подписи — в Преддверие. */
	public static void enterVestibule(ServerPlayer p) {
		ServerLevel level = Vestibule.level(p.server);
		if (level == null) return;
		Vestibule v = Vestibule.get(level);
		v.ensurePlaced(level);
		ensureGuide(level, v);
		preloadArrival(p);
		BlockPos a = v.arrival();
		BlockPos g = v.guidePos();
		float yaw = (float) (Math.atan2(g.getZ() - a.getZ(), g.getX() - a.getX()) * 180 / Math.PI) - 90f;
		p.teleportTo(level, a.getX() + 0.5, a.getY(), a.getZ() + 0.5, yaw, 0f);
		level.playSound(null, a, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.5f);
	}

	private static void ensureGuide(ServerLevel level, Vestibule v) {
		BlockPos g = v.guidePos();
		level.getChunk(g.getX() >> 4, g.getZ() >> 4);
		if (!level.getEntitiesOfClass(GuideEntity.class, new net.minecraft.world.phys.AABB(g).inflate(32)).isEmpty()) return;
		GuideEntity guide = ModMobs.GUIDE.create(level);
		if (guide == null) return;
		guide.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 180f, 0f);
		level.addFreshEntity(guide);
	}

	private static void passGate(ServerPlayer p) {
		LandsTravel t = Travel.get(p);
		if (t.stage() < LandsTravel.STAGE_ENTERED) Travel.set(p, t.withStage(LandsTravel.STAGE_ENTERED));
		ServerLevel lands = ModDimensions.lands(p.server);
		if (lands == null) return;
		Integer road = ARRIVAL_ROAD.remove(p.getUUID());
		Vec3 to = ModDimensions.arrival(lands, road != null ? road : p.getRandom().nextInt(InkLandsLayout.ROADS));
		p.teleportTo(lands, to.x, to.y, to.z, p.getYRot(), 0f);
		lands.playSound(null, BlockPos.containing(to), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.6f);
	}

}
