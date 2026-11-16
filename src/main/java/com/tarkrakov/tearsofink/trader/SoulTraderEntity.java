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
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12f, 1f));
	}

	public void setFountain(BlockPos pos) {
		this.fountain = pos.immutable();
	}

	public BlockPos fountain() {
		return fountain;
	}

	/** Открыть окно сделки игроку (через delay тиков). */
	public void openFor(ServerPlayer p, int delay) {
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
		p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("trader.tears-of-ink.farewell")
				.withStyle(net.minecraft.ChatFormatting.DARK_AQUA, net.minecraft.ChatFormatting.ITALIC));
		vanishAt = tickCount + 30;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) return;
		life++;
		if (openAt >= 0 && tickCount >= openAt) {
			openAt = -1;
			if (openFor != null && server.getPlayerByUUID(openFor) instanceof ServerPlayer p && p.distanceTo(this) <= SoulTrader.TRADE_RANGE + 2) {
				SoulTrader.sendOpen(p, this);
			}
		}
		if (tickCount % 20 == 0) {
			boolean near = !server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(SoulTrader.RADIUS), Player::isAlive).isEmpty();
			idle = near ? 0 : idle + 20;
			if (random.nextInt(3) == 0) {
				server.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.3, getZ(), 3, 0.25, 0.1, 0.25, 0.005);
			}
		}
		if (!vanishes()) return;
		if (idle >= SoulTrader.IDLE_TICKS || life >= SoulTrader.MAX_LIFE || (vanishAt >= 0 && tickCount >= vanishAt)
				|| !server.getBlockState(fountain).is(com.tarkrakov.tearsofink.block.ModBlocks.INK_FOUNTAIN)) {
			vanish(server);
		}
	}

	/** Исчезает сама (Торговка — да; Хранитель Жатвы — нет). */
	protected boolean vanishes() {
		return true;
	}

	private void vanish(ServerLevel server) {
		server.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.0, getZ(), 40, 0.3, 0.8, 0.3, 0.02);
		server.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 0.8f, 0.8f);
		discard();
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (player instanceof ServerPlayer sp) openFor(sp, 0);
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
