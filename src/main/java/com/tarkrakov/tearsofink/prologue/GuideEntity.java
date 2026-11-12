package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.travel.Guide;
import com.tarkrakov.tearsofink.travel.LandsTravel;
import com.tarkrakov.tearsofink.travel.Travel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Господин Проводник — встречает у ворот Преддверия. С каждым новым гостем говорит отдельно
 * (реплики видит только он), дарит Потёртый саквояж и растворяется в тумане (потом снова появляется — для следующих).
 * Неуязвим, не ходит.
 */
public class GuideEntity extends PathfinderMob {
	public static final int ACT_IDLE = 0, ACT_TALK = 1, ACT_GIFT = 2, ACT_VANISH = 3;
	/** Реплик приветствия (guide.tears-of-ink.greet.1 … N); на какой отдаёт саквояж. */
	public static final int GREET_LINES = 5, GIFT_LINE = 4;
	/** Пауза между репликами (тики). */
	public static final int LINE_TICKS = 70;
	/** На какой секунде анимации gift «отдаёт» (тики) и длина анимации vanish. */
	public static final int GIFT_GIVE_TICK = 14, VANISH_TICKS = 36;
	/** Сколько тиков невидим после исчезновения. */
	public static final int HIDDEN_TICKS = 200;
	/** С какого расстояния замечает гостя. */
	public static final double GREET_RANGE = 14;

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(GuideEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> HIDDEN = SynchedEntityData.defineId(GuideEntity.class, EntityDataSerializers.BOOLEAN);

	public final AnimationState idleState = new AnimationState();
	public final AnimationState talkState = new AnimationState();
	public final AnimationState giftState = new AnimationState();
	public final AnimationState vanishState = new AnimationState();
	private int lastAction = -1;

	/** Разговоры: гость → тик, когда сказать следующую реплику, и её номер. */
	private final Map<UUID, int[]> talks = new HashMap<>();
	private int actionTicks, hiddenTicks;

	public GuideEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.setInvulnerable(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, ACT_IDLE);
		builder.define(HIDDEN, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 16f, 1f));
	}

	public int action() { return entityData.get(ACTION); }
	public boolean hidden() { return entityData.get(HIDDEN); }

	@Override
	public boolean isInvisible() {
		return hidden() || super.isInvisible();
	}

	@Override public boolean hurt(DamageSource source, float amount) { return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount); }
	@Override public boolean isPushable() { return false; }
	@Override protected void doPush(net.minecraft.world.entity.Entity e) {}
	@Override public boolean removeWhenFarAway(double d) { return false; }
	@Override public boolean canBeLeashed() { return false; }

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			int act = action();
			if (act != lastAction) {
				giftState.stop();
				vanishState.stop();
				if (act == ACT_GIFT) giftState.start(tickCount);
				if (act == ACT_VANISH) vanishState.start(tickCount);
				lastAction = act;
			}
			idleState.animateWhen(act == ACT_IDLE && !hidden(), tickCount);
			talkState.animateWhen(act == ACT_TALK, tickCount);
			return;
		}
		serverTick((ServerLevel) level());
	}

	private void setAction(int a) {
		if (action() != a) { entityData.set(ACTION, a); actionTicks = 0; }
	}

	private void serverTick(ServerLevel level) {
		actionTicks++;
		if (hidden()) {
			if (--hiddenTicks <= 0) {
				entityData.set(HIDDEN, false);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.4, getZ(), 30, 0.4, 0.9, 0.4, 0.01);
			}
			return;
		}
		if (action() == ACT_VANISH) {
			if (tickCount % 2 == 0) level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.2, getZ(), 6, 0.35, 0.8, 0.35, 0.01);
			if (actionTicks >= VANISH_TICKS) {
				entityData.set(HIDDEN, true);
				hiddenTicks = HIDDEN_TICKS;
				setAction(ACT_IDLE);
				level.sendParticles(com.tarkrakov.tearsofink.particle.ModParticles.INK_SMOKE, getX(), getY() + 1.4, getZ(), 40, 0.5, 1.0, 0.5, 0.02);
			}
			return;
		}
		// новые гости: подписали контракт, ещё не прошли ворота, ещё не говорили
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(GREET_RANGE), ServerPlayer::isAlive)) {
			LandsTravel t = Travel.get(p);
			if (t.stage() >= LandsTravel.STAGE_ENTERED || talks.containsKey(p.getUUID()) || greeted(p)) continue;
			talks.put(p.getUUID(), new int[]{tickCount + 20, 1});
		}
		boolean anyTalking = false;
		var it = talks.entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
			int[] st = e.getValue();
			if (p == null || p.level() != level) { it.remove(); continue; }
			if (st[1] > GREET_LINES) continue; // договорили (ждём ухода в ворота)
			anyTalking = true;
			getLookControl().setLookAt(p, 30f, 30f);
			if (tickCount < st[0]) continue;
			Guide.say(p, "greet." + st[1]);
			level.playSound(null, blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 0.4f, 0.4f);
			if (st[1] == GIFT_LINE) {
				setAction(ACT_GIFT);
				PrologueItems.give(p, ModItems.WORN_SATCHEL);
				p.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 0.8f);
			} else {
				setAction(ACT_TALK);
			}
			st[1]++;
			st[0] = tickCount + LINE_TICKS;
			if (st[1] > GREET_LINES) {
				markGreeted(p);
				// все договорили — растворяется
				if (talks.values().stream().allMatch(v -> v[1] > GREET_LINES)) {
					setAction(ACT_VANISH);
					level.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 1f, 0.6f);
				}
			}
		}
		if (!anyTalking && action() == ACT_TALK && actionTicks > 40) setAction(ACT_IDLE);
		if (action() == ACT_GIFT && actionTicks > 30) setAction(ACT_TALK);
	}

	private static final String GREETED_TAG = "tears_of_ink_greeted";

	private static boolean greeted(ServerPlayer p) {
		return p.getTags().contains(GREETED_TAG);
	}

	private static void markGreeted(ServerPlayer p) {
		p.addTag(GREETED_TAG);
	}
}
