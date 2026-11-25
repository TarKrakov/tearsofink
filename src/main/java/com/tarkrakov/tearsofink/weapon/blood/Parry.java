package com.tarkrakov.tearsofink.weapon.blood;

import com.tarkrakov.tearsofink.hunt.HunterEntity;
import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import com.tarkrakov.tearsofink.weapon.combat.Combat;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Стойка парирования Дуссака: пока стоит, любой удар вблизи (не стрелы) обнуляется, а клинок сам отвечает
 * росчерком с кровотечением. Парой — дольше и сильнее, отвечают оба клинка. Стоят и игроки, и охотники Крови.
 */
public final class Parry {
	private static final class State {
		long until, nextRiposte, resumeAt = -1;
		boolean dual;
		/** Одиночная стойка — Дуссаком в левой руке (у охотника: Тальвар + Дуссак). */
		boolean off;
		/** Охотник (у игрока — null, его ищем по списку игроков). */
		@Nullable LivingEntity mob;
	}

	private static final Map<UUID, State> STANCE = new HashMap<>();

	/** Сервер → клиент: игрок в стойке (true) / вышел (false). Пока стоишь — только ходить. */
	public record Lock(boolean on) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<Lock> TYPE = new Type<>(com.tarkrakov.tearsofink.TearsOfInk.id("parry_lock"));
		public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Lock> CODEC =
				net.minecraft.network.codec.StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.BOOL, Lock::on, Lock::new);

		@Override
		public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Свой игрок на клиенте в стойке (ставит клиент по Lock). */
	public static volatile boolean CLIENT_LOCK;

	private Parry() {}

	/** Каст Дуссака: ничего, кроме движения — ни ударов (любой рукой), ни щита, ни предметов, ни блоков. */
	private static boolean locked(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level world) {
		return world.isClientSide ? CLIENT_LOCK : active(player);
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Parry::tick);
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(Lock.TYPE, Lock.CODEC);
		// A parry can be interrupted by another technique; no blanket input lock.
		net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (player instanceof ServerPlayer p) cancelForAction(p);
			return net.minecraft.world.InteractionResult.PASS;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (STANCE.isEmpty()) return true;
			State s = STANCE.get(entity.getUUID());
			if (s == null || !melee(source, entity)) return true;
			long now = entity.getServer().getTickCount();
			if (now >= s.nextRiposte) riposte(entity, (LivingEntity) source.getEntity(), s, now);
			entity.level().playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.4f);
			return false; // удар пойман — урона нет
		});
	}

	/** Удар вблизи: бьёт само существо (не стрела, не взрыв). */
	private static boolean melee(DamageSource source, LivingEntity p) {
		return source.getEntity() instanceof LivingEntity by && by != p && source.getDirectEntity() == by
				&& by.distanceTo(p) <= BloodTuning.RIPOSTE_REACH + 1;
	}

	public static void cancelForAction(ServerPlayer p) {
		State s = STANCE.remove(p.getUUID());
		if (s != null) {
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new Lock(false));
			anim(p,s,"parry_release",4);
		}
	}
	public static boolean active(LivingEntity e) {
		return STANCE.containsKey(e.getUUID());
	}

	/** Игрок встаёт в стойку. dual — парой (движение обеих рук задаёт сам вызов; одиночной — ability_anim). */
	public static void start(ServerPlayer p, boolean dual) {
		start(p, dual, false);
	}

	/** off — одиночная стойка Дуссаком в левой руке (Shift+R, в правой другое оружие). */
	public static void start(ServerPlayer p, boolean dual, boolean off) {
		begin(p, dual, off, null);
		if (dual) Combat.play(p, Archetype.DUSSAK, "parry", "parry", BloodTuning.DOUBLE_PARRY_TICKS);
	}

	/** Охотник встаёт в стойку. off — одиночная стойка Дуссаком в левой руке. */
	public static void start(HunterEntity h, boolean dual, boolean off) {
		begin(h, dual, off, h);
		int ticks = dual ? BloodTuning.DOUBLE_PARRY_TICKS : BloodTuning.PARRY_TICKS;
		h.playAnim(Archetype.DUSSAK, dual || !off ? "parry" : null, dual || off ? "parry" : null, ticks);
	}

	private static void begin(LivingEntity e, boolean dual, boolean off, @Nullable LivingEntity mob) {
		State s = new State();
		long now = e.getServer().getTickCount();
		s.dual = dual;
		s.off = off;
		s.mob = mob;
		s.until = now + (dual ? BloodTuning.DOUBLE_PARRY_TICKS : BloodTuning.PARRY_TICKS);
		STANCE.put(e.getUUID(), s);
		if (e instanceof ServerPlayer sp) { // щит опущен, остальное заперто
			if (sp.isUsingItem()) sp.stopUsingItem();
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, new Lock(true));
		}
		// ДЗЫНЬ
		e.level().playSound(null, e.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.45f, 1.9f);
		e.level().playSound(null, e.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
	}

	/** Движение стойки у всех: игроку — через Combat, охотнику — его рассылкой. */
	private static void anim(LivingEntity e, State s, String name, int ticks) {
		String main = s.dual || !s.off ? name : null, off = s.dual || s.off ? name : null;
		if (e instanceof ServerPlayer p) Combat.play(p, Archetype.DUSSAK, main, off, ticks);
		else if (e instanceof HunterEntity h) h.playAnim(Archetype.DUSSAK, main, off, ticks);
	}

	private static void riposte(LivingEntity p, LivingEntity by, State s, long now) {
		s.nextRiposte = now + BloodTuning.RIPOSTE_GAP;
		s.resumeAt = now + BloodTuning.RIPOSTE_GAP;
		// Дуссак в левой бьёт так же, как в правой: урон самого Дуссака, а не того, что в правой руке
		double base = p instanceof HunterEntity h ? h.riposteBase()
				: s.off ? weaponDamage(p.getOffhandItem()) : p.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float dmg = (float) (base * (s.dual ? BloodTuning.DOUBLE_RIPOSTE_MULT : BloodTuning.RIPOSTE_MULT));
		DamageSource src = p instanceof ServerPlayer sp ? sp.damageSources().playerAttack(sp) : p.damageSources().mobAttack(p);
		if (by.isAlive() && by.distanceTo(p) <= BloodTuning.RIPOSTE_REACH && by.hurt(src, dmg)) {
			Bleeding.apply(by, p, s.dual ? BloodTuning.DOUBLE_RIPOSTE_BLEED : BloodTuning.RIPOSTE_BLEED);
			Vec3 d = by.position().subtract(p.position()).multiply(1, 0, 1).normalize();
			by.knockback(0.4, -d.x, -d.z);
		}
		anim(p, s, s.dual ? "dual_riposte" : "riposte", BloodTuning.RIPOSTE_GAP);
		if (!s.dual) { // одиночная стойка: спарировал и ответил — стойка окончена (усиленная парой держится до конца)
			s.until = now + BloodTuning.RIPOSTE_GAP;
			s.resumeAt = -1;
		}
		if (p.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CRIT, by.getX(), by.getY() + by.getBbHeight() * 0.6, by.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
			level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35f, 2.0f);
			level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1.1f);
		}
	}

	/** Урон оружия как в руке игрока: 1 (кулак) + прибавка урона самого предмета. */
	private static double weaponDamage(net.minecraft.world.item.ItemStack stack) {
		double dmg = 1;
		var mods = stack.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
				net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
		for (var e : mods.modifiers()) {
			if (e.attribute().is(Attributes.ATTACK_DAMAGE) && e.modifier().operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
				dmg += e.modifier().amount();
			}
		}
		return dmg;
	}

	/** Дуссак ещё в нужной руке (руках)? */
	private static boolean holding(LivingEntity e, State s) {
		boolean main = Archetype.of(e.getMainHandItem()) == Archetype.DUSSAK, off = Archetype.of(e.getOffhandItem()) == Archetype.DUSSAK;
		if (s.dual) return main && off;
		return s.off ? off : main;
	}

	private static void tick(MinecraftServer server) {
		if (STANCE.isEmpty()) return;
		long now = server.getTickCount();
		for (Iterator<Map.Entry<UUID, State>> it = STANCE.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			State s = e.getValue();
			LivingEntity p = s.mob != null ? s.mob : server.getPlayerList().getPlayer(e.getKey());
			boolean alive = p != null && p.isAlive() && !p.isRemoved();
			if (!alive || !holding(p, s) || now >= s.until) {
				it.remove();
				if (p instanceof ServerPlayer sp) net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, new Lock(false));
				if (alive) anim(p, s, "parry_release", 4);
				continue;
			}
			if (s.resumeAt >= 0 && now >= s.resumeAt) { // после ответа — снова в стойку
				s.resumeAt = -1;
				anim(p, s, "parry_resume", (int) (s.until - now));
			}
		}
	}
}
