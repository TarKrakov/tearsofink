package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.travel.Guide;
import com.tarkrakov.tearsofink.travel.LandsTravel;
import com.tarkrakov.tearsofink.travel.Travel;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static final int LOAD_TIMEOUT_TICKS = 1200;
	private record GateWait(int animationEnds, int deadline) {}
	private static final Map<ServerPlayer, GateWait> TRANSIT = new HashMap<>();
	private static final Map<ServerPlayer, Integer> ENTERING = new HashMap<>();

	private Prologue() {}

	public static void init() {
		DiaryEntries.init();
		StoryKeyItem.init();
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
			// Older characters already opened their gift before the storyline keys existed.
			if (PrologueItems.giftOpened(p)) StoryKeyItem.grant(p, com.tarkrakov.tearsofink.vow.Vows.vow(p));
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearTransit(handler.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> clearTransit(player));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) clearTransit(player);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerPlayer player : List.copyOf(TRANSIT.keySet())) clearTransit(player);
			ENTERING.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int now = server.getTickCount();
			if (now % 4 != 0 || ENTERING.isEmpty()) return;
			var level = Vestibule.level(server);
			for (ServerPlayer p : List.copyOf(ENTERING.keySet())) {
				if (level == null || p.isRemoved() || !p.isAlive() || p.hasDisconnected()) {
					ENTERING.remove(p);
				} else if (now >= ENTERING.get(p)) {
					ENTERING.remove(p);
					p.displayClientMessage(Component.translatable("message.tears-of-ink.destination_timeout"), true);
				} else if (tryEnterVestibule(p, level)) {
					ENTERING.remove(p);
				}
			}
		});

		// ворота Преддверия
		ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (!Vestibule.is(level) || level.getGameTime() % 4 != 0) return;
			Vestibule v = Vestibule.get(level);
			for (ServerPlayer p : java.util.List.copyOf(level.players())) { // копия: игрок уходит в другое измерение прямо в цикле
				if (!p.isAlive() || p.isSpectator()) {
					clearTransit(p);
					continue;
				}
				GateWait wait = TRANSIT.get(p);
				if (wait != null) {
					int now = p.server.getTickCount();
					ServerLevel lands = ModDimensions.lands(p.server);
					if (lands == null || now >= wait.deadline()) {
						clearTransit(p);
						BlockPos a = v.arrival();
						p.teleportTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
						p.displayClientMessage(Component.translatable("message.tears-of-ink.destination_timeout"), true);
					} else if (now >= wait.animationEnds()) {
						var arrival = ModDimensions.readyArrival(lands);
						if (arrival != null) {
							clearTransit(p);
							passGate(p, lands, arrival);
						} else if ((now - wait.animationEnds()) % 20 < 4) {
							ModDimensions.preloadArrival(lands);
							p.displayClientMessage(Component.translatable("message.tears-of-ink.destination_loading"), true);
						}
					}
				} else if (p.getBoundingBox().intersects(v.gate().inflate(0.2))) {
					if (com.tarkrakov.tearsofink.vow.Vows.mayPass(p)) {
						if (PrologueItems.giftOpened(p)) {
							StoryKeyItem.grant(p, com.tarkrakov.tearsofink.vow.Vows.vow(p));
							startTransit(p);
						} else {
							PrologueItems.ensureUnopenedGift(p);
							BlockPos a = v.arrival();
							p.teleportTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
							Guide.say(p, "gift.unopened");
						}
					}
					else { // без обета дальше не пройти — назад от ворот и окно выбора
						BlockPos a = v.arrival();
						p.teleportTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
						com.tarkrakov.tearsofink.vow.Vows.prompt(p);
						com.tarkrakov.tearsofink.vow.Vows.guidePromptOnce(p, "vow.gate");
					}
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
		int now = p.server.getTickCount();
		TRANSIT.put(p, new GateWait(now + TRANSIT_TICKS, now + LOAD_TIMEOUT_TICKS));
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


	private static void clearTransit(ServerPlayer p) {
		if (TRANSIT.remove(p) != null) unfreeze(p);
		ENTERING.remove(p);
	}

	private static void preloadArrival(ServerPlayer p) {
		ServerLevel lands = ModDimensions.lands(p.server);
		if (lands != null) ModDimensions.preloadArrival(lands);
	}

	private static void sign(ServerPlayer p) {
		LandsTravel t = Travel.get(p);
		if (t.stage() >= LandsTravel.STAGE_SIGNED) return;
		var inv = p.getInventory();
		int slot = -1;
		for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ModItems.INVITATION_LETTER)) { slot = i; break; }
		if (slot < 0) return;
		inv.getItem(slot).shrink(1);
		if (!inv.contains(new ItemStack(ModItems.DIM_MIRROR)) && !PersonalSlots.get(p,PersonalSlots.MIRROR).is(ModItems.DIM_MIRROR))
            PrologueItems.give(p, ModItems.DIM_MIRROR);
		Travel.set(p, t.withStage(LandsTravel.STAGE_SIGNED));
		DiaryEntries.unlock(p, DiaryEntries.COLLECTION_INVITATION);
		preloadArrival(p);
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
		if (level == null || ENTERING.containsKey(p)) return;
		preloadArrival(p);
		if (!tryEnterVestibule(p, level)) {
			ENTERING.put(p, p.server.getTickCount() + LOAD_TIMEOUT_TICKS);
			p.displayClientMessage(Component.translatable("message.tears-of-ink.destination_loading"), true);
		}
	}

	private static boolean tryEnterVestibule(ServerPlayer p, ServerLevel level) {
		Vestibule v = Vestibule.get(level);
		if (!v.prepare(level)) return false;
		ensureGuide(level, v);
		preloadArrival(p);
		BlockPos a = v.arrival();
		BlockPos g = v.guidePos();
		float yaw = (float) (Math.atan2(g.getZ() - a.getZ(), g.getX() - a.getX()) * 180 / Math.PI) - 90f;
		p.teleportTo(level, a.getX() + 0.5, a.getY(), a.getZ() + 0.5, yaw, 0f);
		level.playSound(null, a, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.5f);
		return true;
	}

	private static void ensureGuide(ServerLevel level, Vestibule v) {
		BlockPos g = v.guidePos();
		if (!level.getEntitiesOfClass(GuideEntity.class, new net.minecraft.world.phys.AABB(g).inflate(32)).isEmpty()) return;
		GuideEntity guide = ModMobs.GUIDE.create(level);
		if (guide == null) return;
		guide.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 180f, 0f);
		level.addFreshEntity(guide);
	}


	private static void passGate(ServerPlayer p, ServerLevel lands, ModDimensions.Arrival arrival) {
		LandsTravel t = Travel.get(p);
		if (t.stage() < LandsTravel.STAGE_ENTERED) Travel.set(p, t.withStage(LandsTravel.STAGE_ENTERED));
		Vec3 to = arrival.pos();
		p.teleportTo(lands, to.x, to.y, to.z, arrival.yaw(), 0f);
		lands.playSound(null, BlockPos.containing(to), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.6f);
	}
}
