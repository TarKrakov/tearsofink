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

	private Prologue() {}

	public static void init() {
		DiaryEntries.init();
		PayloadTypeRegistry.playC2S().register(SignContract.TYPE, SignContract.CODEC);
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
			for (ServerPlayer p : level.players()) {
				if (p.isSpectator()) continue;
				if (p.getBoundingBox().intersects(v.gate().inflate(0.2))) passGate(p);
				else if (p.getY() < level.getMinBuildHeight() + 20) { // упал с островка — обратно
					BlockPos a = v.arrival();
					p.teleportTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
					p.resetFallDistance();
				}
			}
		});
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
		Vec3 to = ModDimensions.arrival(lands, p.getRandom().nextInt(InkLandsLayout.ROADS));
		p.teleportTo(lands, to.x, to.y, to.z, p.getYRot(), 0f);
		lands.playSound(null, BlockPos.containing(to), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.6f);
	}

}
