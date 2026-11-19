package com.tarkrakov.tearsofink.trader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Воплощение Торговки душами у фонтана. Не ходит, неуязвима, не сохраняется в мире:
 * растворяется, когда рядом долго никого нет (или по «Уйти»), и проступает снова по Shift+ПКМ.
 */
public class SoulTraderEntity extends PathfinderMob {
	private BlockPos fountain = BlockPos.ZERO;
	private int idle, life;
	private UUID owner;
	private int nextIntroAt = SoulTrader.OPEN_DELAY;
	private UUID openFor;
	private int openAt = -1;
	private int vanishAt = -1;

	public SoulTraderEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		this.setInvulnerable(true);
		this.setNoGravity(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void registerGoals() {
		// The permanent Harvest Keeper retains its shared look goal.
		if (!vanishes()) this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12f, 1f));
	}

	void setOwner(UUID owner) { this.owner = owner; }

	public boolean ownedBy(ServerPlayer player) {
		return owner != null && owner.equals(player.getUUID());
	}

	/** Vanilla ChunkMap uses this before tracking/spawning the entity for a client. */
	@Override
	public boolean broadcastToPlayer(ServerPlayer player) {
		return (!vanishes() || ownedBy(player)) && super.broadcastToPlayer(player);
	}

	@Override
	public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
		if (!level().isClientSide) SoulTrader.release(this, owner);
		super.remove(reason);
	}

	public void setFountain(BlockPos pos) {
		this.fountain = pos.immutable();
	}

	public BlockPos fountain() {
		return fountain;
	}

	/** Открыть окно сделки игроку (через delay тиков). */
	public void openFor(ServerPlayer p, int delay) {
		if (!SoulTrader.canTrade(p, this)) return;
		keepAlive();
		if (delay <= 0) {
			SoulTrader.sendOpen(p, this);
			return;
		}
		openFor = p.getUUID();
		openAt = tickCount + delay;
	}

	public void keepAlive() {
		idle = 0;
		vanishAt = -1;
	}

	/** «Уйти»: прощается и растворяется, если с ней больше никто не говорит. */
	public void farewell(ServerPlayer p) {
		if (!SoulTrader.canTrade(p, this)) return;
		p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("trader.tears-of-ink.farewell")
				.withStyle(net.minecraft.ChatFormatting.DARK_AQUA, net.minecraft.ChatFormatting.ITALIC));
		vanishAt = tickCount + 30;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) return;
		if (!vanishes()) {
			// Keep Harvest Keeper's old shared ambient particles and behavior.
			if (tickCount % 20 == 0 && random.nextInt(3) == 0)
				server.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.3, getZ(), 3, 0.25, 0.1, 0.25, 0.005);
			return;
		}
		ServerPlayer p = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
		if (p == null || p.level() != server || !SoulTrader.eligiblePlayer(p)) {
			discard();
			return;
		}
		life++;
		double distance = p.distanceTo(this);
		if (!server.getBlockState(fountain).is(com.tarkrakov.tearsofink.block.ModBlocks.INK_FOUNTAIN)
				|| life >= SoulTrader.MAX_LIFE || (vanishAt >= 0 && tickCount >= vanishAt)) {
			vanish(server, p);
			return;
		}
		if (distance <= SoulTrader.RADIUS) {
			idle = 0;
			getLookControl().setLookAt(p, 30f, 30f);
			if (distance <= SoulTrader.TRADE_RANGE && !SoulTrader.introduced(p) && tickCount >= nextIntroAt) {
				SoulTrader.nextIntroLine(p);
				nextIntroAt = tickCount + SoulTrader.INTRO_LINE_TICKS;
			}
		} else if (++idle >= SoulTrader.IDLE_TICKS) {
			vanish(server, p);
			return;
		}
		if (openAt >= 0 && tickCount >= openAt) {
			openAt = -1;
			if (ownedBy(p) && p.getUUID().equals(openFor)) SoulTrader.sendOpen(p, this);
			openFor = null;
		}
		if (tickCount % 20 == 0 && random.nextInt(3) == 0) {
			server.sendParticles(p, ParticleTypes.SQUID_INK, false, getX(), getY() + 0.3, getZ(), 3, 0.25, 0.1, 0.25, 0.005);
		}
	}

	/** Исчезает сама (Торговка — да; Хранитель Жатвы — нет). */
	protected boolean vanishes() {
		return true;
	}

	private void vanish(ServerLevel server, ServerPlayer owner) {
		server.sendParticles(owner, ParticleTypes.SQUID_INK, false, getX(), getY() + 1.0, getZ(), 40, 0.3, 0.8, 0.3, 0.02);
		if (owner.distanceTo(this) <= SoulTrader.RADIUS)
			owner.playNotifySound(SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 0.8f, 0.8f);
		discard();
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (player instanceof ServerPlayer sp && ownedBy(sp)) openFor(sp, 0);
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	// ===== Не боец, не вещь в мире =====
	@Override
	public boolean hurt(DamageSource source, float amount) {
		return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(net.minecraft.world.entity.Entity entity) {
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
