package com.tarkrakov.tearsofink.weapon.shield;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.patrol.PaleShieldbearerEntity;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.weapon.SoulShieldItem;
import com.tarkrakov.tearsofink.weapon.combat.Combat;
import com.tarkrakov.tearsofink.weapon.combat.Interruptible;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Стойка двух щитов Души (этап 1): щиты Души в обеих руках, ПКМ держать, только в Чернильных землях.
 * Блок спереди и с боков полный, со спины — половина; блок стоит Души, кончилась — щиты пробиты; Скала — не сдвинуть;
 * своевременный блок — бесплатно, нападающий сбит, снаряд летит обратно; каждый принятый удар заряжает щиты светом,
 * отпустил ПКМ — выброс конусом. Числа — {@link ShieldTuning}.
 */
public final class ShieldStance {
	private static final ResourceLocation ROCK = TearsOfInk.id("shield_rock");

	/** Заряд щитов у игрока (сервер). */
	private static final Map<UUID, Integer> CHARGE = new HashMap<>();
	/** Сколько урона щиты поглотили в этой стойке (сгорает вместе с зарядом). */
	private static final Map<UUID, Float> ABSORBED = new HashMap<>();
	/** До какого тика открыт ответ после своевременного блока. */
	private static final Map<UUID, Long> RIPOSTE = new HashMap<>();
	/** Когда у игрока последний раз звучал хор. */
	private static final Map<UUID, Long> CHOIR = new HashMap<>();
	/** Отражённые снаряды — развернуть на следующем тике (игра сама гасит их после блока). */
	private record Reflect(Projectile p, ServerPlayer by, Vec3 dir, double speed) {}

	private static final List<Reflect> REFLECT = new ArrayList<>();
	/** Удар со спины: пропускаем половину повторным вызовом — этот флаг, чтобы не поймать его снова. */
	private static boolean passing;

	/** Заряд щитов игрока — всем, кто его видит (свечение). */
	public record Glow(int entity, int charge) implements CustomPacketPayload {
		public static final Type<Glow> TYPE = new Type<>(TearsOfInk.id("shield_glow"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Glow> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Glow::entity, ByteBufCodecs.VAR_INT, Glow::charge, Glow::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private ShieldStance() {}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(Glow.TYPE, Glow.CODEC);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(ShieldStance::allowDamage);
		ServerTickEvents.END_SERVER_TICK.register(ShieldStance::tick);
	}

	/** Щиты Души в обеих руках. */
	public static boolean dual(LivingEntity e) {
		return e.getMainHandItem().getItem() instanceof SoulShieldItem && e.getOffhandItem().getItem() instanceof SoulShieldItem;
	}

	/** Стоит в сомкнутой стойке прямо сейчас. */
	public static boolean inStance(LivingEntity e) {
		return e instanceof Player p && ModDimensions.isLands(p.level()) && dual(p) && p.isUsingItem()
				&& p.getUseItem().getItem() instanceof SoulShieldItem && !p.getCooldowns().isOnCooldown(p.getUseItem().getItem());
	}

	public static int charge(Player p) {
		return CHARGE.getOrDefault(p.getUUID(), 0);
	}

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (passing || !(entity instanceof ServerPlayer p) || !inStance(p)) return true;
		if (source.is(DamageTypeTags.BYPASSES_SHIELD)) return true;
		Vec3 from = source.getSourcePosition();
		if (from == null) return true; // падение, огонь, голод — щиты не помогут
		Vec3 look = p.getLookAngle().multiply(1, 0, 1);
		Vec3 to = from.subtract(p.position()).multiply(1, 0, 1);
		boolean covered = look.lengthSqr() < 1.0e-4 || to.lengthSqr() < 1.0e-4
				|| Math.toDegrees(Math.acos(Mth.clamp(look.normalize().dot(to.normalize()), -1, 1))) <= ShieldTuning.COVER_DEGREES;
		if (!covered) { // со спины — половина урона
			passing = true;
			try {
				p.hurt(source, amount * ShieldTuning.BACK_FRACTION);
			} finally {
				passing = false;
			}
			return false;
		}
		ServerLevel level = p.serverLevel();
		boolean perfect = p.getTicksUsingItem() <= ShieldTuning.PERFECT_TICKS;
		if (!perfect && !p.getAbilities().instabuild) {
			InkData d = InkPowers.get(p);
			float cost = amount * ShieldTuning.SOUL_PER_DAMAGE;
			if (d.soul() < cost) {
				breakShields(p);
				passing = true;
				try { // пробили — удар проходит
					p.hurt(source, amount);
				} finally {
					passing = false;
				}
				return false;
			}
			InkPowers.update(p, d.withSoul(d.soul() - cost));
		}
		setCharge(p, Math.min(ShieldTuning.CHARGE_MAX, charge(p) + 1));
		ABSORBED.merge(p.getUUID(), amount, Float::sum);
		if (charge(p) >= ShieldTuning.CHARGE_MAX) { // полный заряд — щиты поют
			long now = level.getGameTime();
			Long last = CHOIR.get(p.getUUID());
			if (last == null || now - last >= ShieldTuning.CHOIR_EVERY) {
				CHOIR.put(p.getUUID(), now);
				level.playSound(null, p.blockPosition(), com.tarkrakov.tearsofink.sound.ModSounds.SHIELD_CHOIR, SoundSource.PLAYERS,
						ShieldTuning.CHOIR_VOLUME, 0.96f + p.getRandom().nextFloat() * 0.08f);
			}
		}
		level.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, perfect ? 1.4f : 0.9f);
		Entity by = source.getEntity(), direct = source.getDirectEntity();
		if (perfect) {
			RIPOSTE.put(p.getUUID(), level.getGameTime() + ShieldTuning.RIPOSTE_TICKS); // ответ открыт
			level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.6f);
			level.sendParticles(ParticleTypes.END_ROD, p.getX() + look.x * 0.8, p.getY() + 1.2, p.getZ() + look.z * 0.8, 10, 0.3, 0.3, 0.3, 0.05);
			if (direct instanceof Projectile proj) {
				Vec3 v = proj.getDeltaMovement();
				REFLECT.add(new Reflect(proj, p, v.lengthSqr() < 1.0e-4 ? p.getLookAngle() : v.normalize().reverse(), Math.max(1.2, v.length())));
			} else if (by instanceof LivingEntity att && direct == by) {
				stagger(p, att, ShieldTuning.PERFECT_STAGGER, 0.5f);
			}
		}
		// Скала: щитоносец упирается в стойку и отскакивает оглушённым
		if (by instanceof PaleShieldbearerEntity sb && sb.chargeTicks() >= 0) sb.interrupt(p, ShieldTuning.PERFECT_STAGGER * 2);
		return false;
	}

	/** Сильнее против сильных: множитель урона Бастиона и выброса по этой цели. */
	public static float strongMult(LivingEntity t) {
		return t.getMaxHealth() >= ShieldTuning.STRONG_HP ? ShieldTuning.STRONG_MULT : 1f;
	}

	/** Ответ после своевременного блока: открыт — забрать (один удар). */
	public static boolean takeRiposte(ServerPlayer p) {
		Long until = RIPOSTE.remove(p.getUUID());
		return until != null && until >= p.serverLevel().getGameTime();
	}

	/** Сбить цель ответом (для удара Бастиона). */
	public static void riposteHit(ServerPlayer p, LivingEntity t) {
		stagger(p, t, ShieldTuning.PERFECT_STAGGER, 0.6f);
		p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 1.8f);
		p.serverLevel().sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 10, 0.3, 0.3, 0.3, 0.08);
	}

	/** Сбить нападающего: замах — сорвать, иначе — отбросить и замедлить. */
	private static void stagger(ServerPlayer p, LivingEntity t, int ticks, float knock) {
		boolean interrupted = t instanceof Interruptible i ? i.interrupt(p, ticks) : t instanceof ServerPlayer other && Combat.interrupt(other, ticks);
		Vec3 d = t.position().subtract(p.position()).multiply(1, 0, 1);
		if (d.lengthSqr() < 1.0e-4) d = p.getLookAngle();
		d = d.normalize();
		t.knockback(knock, -d.x, -d.z);
		t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, interrupted ? 3 : 1, false, false));
	}

	/** Душа кончилась — щиты пробиты: опускаются и недоступны, заряд сгорает. */
	private static void breakShields(ServerPlayer p) {
		p.getCooldowns().addCooldown(p.getMainHandItem().getItem(), ShieldTuning.BREAK_TICKS);
		p.getCooldowns().addCooldown(p.getOffhandItem().getItem(), ShieldTuning.BREAK_TICKS);
		p.stopUsingItem();
		setCharge(p, 0);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1f, 0.8f);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
	}

	/** Отпустил ПКМ в стойке (из щита): заряд — выброс конусом перед собой. */
	public static void release(ServerPlayer p) {
		int c = charge(p);
		if (c <= 0 || !ModDimensions.isLands(p.level()) || !dual(p)) return;
		float absorbed = ABSORBED.getOrDefault(p.getUUID(), 0f);
		setCharge(p, 0);
		ServerLevel level = p.serverLevel();
		Vec3 look = p.getLookAngle().multiply(1, 0, 1);
		look = look.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : look.normalize();
		// превозмогание: чем сильнее били — тем сильнее ответ
		float dmg = ShieldTuning.BURST_DAMAGE + ShieldTuning.BURST_DAMAGE_PER_CHARGE * c
				+ Math.min(ShieldTuning.ABSORB_BURST_CAP, absorbed * ShieldTuning.ABSORB_TO_BURST);
		float knock = ShieldTuning.BURST_KNOCKBACK + ShieldTuning.BURST_KNOCKBACK_PER_CHARGE * c;
		double r = ShieldTuning.BURST_RANGE;
		for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e != p && e.isAlive())) {
			Vec3 d = t.position().subtract(p.position()).multiply(1, 0, 1);
			if (d.length() > r + t.getBbWidth() / 2) continue;
			if (d.lengthSqr() > 1.0e-4 && Math.toDegrees(Math.acos(Mth.clamp(look.dot(d.normalize()), -1, 1))) > ShieldTuning.BURST_HALF_ANGLE) continue;
			if (t.hurt(p.damageSources().playerAttack(p), dmg * strongMult(t))) t.invulnerableTime = 0;
			stagger(p, t, ShieldTuning.BURST_STAGGER, knock);
		}
		for (int i = 0; i < 8 + c * 6; i++) {
			double a = (p.getRandom().nextDouble() * 2 - 1) * Math.toRadians(ShieldTuning.BURST_HALF_ANGLE);
			Vec3 dir = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), 0, look.x * Math.sin(a) + look.z * Math.cos(a));
			double dist = 0.8 + p.getRandom().nextDouble() * (r - 0.8);
			level.sendParticles(ParticleTypes.END_ROD, p.getX() + dir.x * dist, p.getY() + 0.6 + p.getRandom().nextDouble(), p.getZ() + dir.z * dist,
					1, 0, 0, 0, 0.02);
		}
		level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.7f);
		level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.6f);
	}

	/** Забрать весь заряд (Резонанс тратит его на силу). */
	public static int takeCharge(ServerPlayer p) {
		int c = charge(p);
		if (c > 0) setCharge(p, 0);
		return c;
	}

	private static void setCharge(ServerPlayer p, int c) {
		Integer old = CHARGE.get(p.getUUID());
		if (c <= 0) {
			CHARGE.remove(p.getUUID());
			ABSORBED.remove(p.getUUID());
		}
		else CHARGE.put(p.getUUID(), c);
		if ((old == null ? 0 : old) == c) return;
		Glow msg = new Glow(p.getId(), c);
		ServerPlayNetworking.send(p, msg);
		for (ServerPlayer o : PlayerLookup.tracking(p)) if (o != p) ServerPlayNetworking.send(o, msg);
	}

	private static void tick(MinecraftServer server) {
		if (!REFLECT.isEmpty()) {
			for (Reflect r : REFLECT) {
				if (r.p.isRemoved()) continue;
				r.p.setOwner(r.by);
				r.p.setDeltaMovement(r.dir.scale(r.speed));
				r.p.hasImpulse = true;
			}
			REFLECT.clear();
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			boolean stance = inStance(p);
			rock(p, stance);
			if (!dual(p) || !ModDimensions.isLands(p.level())) {
				if (CHARGE.containsKey(p.getUUID())) setCharge(p, 0); // сменил щиты или ушёл — заряд сгорает
			}
		}
	}

	/** Скала: в стойке не сдвинуть — ни отбрасывание, ни взрыв. */
	private static void rock(ServerPlayer p, boolean on) {
		for (var attr : List.of(Attributes.KNOCKBACK_RESISTANCE, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)) {
			AttributeInstance a = p.getAttribute(attr);
			if (a == null) continue;
			boolean has = a.getModifier(ROCK) != null;
			if (on && !has) a.addTransientModifier(new AttributeModifier(ROCK, 1.0, AttributeModifier.Operation.ADD_VALUE));
			else if (!on && has) a.removeModifier(ROCK);
		}
	}
}
