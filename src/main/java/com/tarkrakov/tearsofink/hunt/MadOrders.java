package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Особые заказы Торговки: убить обезумевшего охотника (любого типа). Охотник с модификаторами Охоты и «омонстрения»
 * (резвый — быстрее, гигант — крупнее и крепче), бьёт игрока. Награда: эссенция Души (Торговка меняет 3 → Частичка
 * Души), шанс на его оружие, Пигмент; Чернильный — ещё Чернильный отголосок. Перезарядка 5 мин с момента, как взял
 * (у каждого игрока своя). Сервер перезапустили — заказ и перезарядка сброшены (как у контрактов).
 */
public final class MadOrders {
	// ===== Настройки — крутить здесь (код → перезапуск) =====
	/** Перезарядка заказа с момента взятия и время на выполнение (тики). */
	public static final int COOLDOWN = 20 * 60 * 5, TIME = 20 * 60 * 15;
	/** Ранг: Опытный / Легендарный (Новичков не бывает). */
	public static final float LEGENDARY_CHANCE = 0.4f;
	/** Безумие: здоровье и урон охотника. */
	public static final float MAD_HP = 1.4f, MAD_DAMAGE = 1.15f;
	/** Модификаторы: резвый (шанс, скорость), гигант (шанс, размер, здоровье). */
	public static final float SWIFT_CHANCE = 0.3f, SWIFT_SPEED = 1.25f, GIANT_CHANCE = 0.3f, GIANT_SCALE = 1.25f, GIANT_HP = 1.3f;
	/** Куда идти: 150–300 блоков от Торговки; появляется, когда игрок ближе 64; ушёл дальше 112 — растворяется до возвращения. */
	public static final int MIN_DIST = 150, MAX_DIST = 300, SPAWN_DIST = 64, FORGET_DIST = 112;
	/** Награда: эссенций (Опытный / Легендарный), Пигмента, шанс на оружие. */
	public static final int[] ESSENCE = {0, 1, 2};
	public static final int PIGMENT_MIN = 3, PIGMENT_MAX = 5;
	public static final float[] WEAPON_CHANCE = {0f, 0.4f, 0.8f};
	/** Обмен у Торговки: столько эссенций Души → 1 Частичка Души. */
	public static final int ESSENCE_PER_MOTE = 3;

	/** Номер луча на клиенте (свой, не пересекается с Охотами). */
	private static final int BEAM_BASE = 900_000;

	public enum Mod { NONE, SWIFT, GIANT }

	static final class Order {
		final int id;
		final UUID owner;
		final HunterType type;
		final int rank;
		final Mod mod;
		final ItemStack main, off;
		final double x, z;
		final long end;
		double y;
		UUID hunter;

		Order(int id, UUID owner, HunterType type, int rank, Mod mod, ItemStack main, ItemStack off, double x, double y, double z, long end) {
			this.id = id;
			this.owner = owner;
			this.type = type;
			this.rank = rank;
			this.mod = mod;
			this.main = main;
			this.off = off;
			this.x = x;
			this.y = y;
			this.z = z;
			this.end = end;
		}
	}

	private static final Map<UUID, Order> ORDERS = new HashMap<>();
	private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
	private static int nextId = 1;

	private MadOrders() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(MadOrders::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(MadOrders::onDeath);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ORDERS.clear();
			COOLDOWNS.clear();
			nextId = 1;
		});
	}

	// ===== Для окна Торговки =====
	/** 0 — можно взять, 1 — заказ идёт, 2+ — перезарядка: (минут осталось + 2). */
	public static int state(ServerPlayer p) {
		if (ORDERS.containsKey(p.getUUID())) return 1;
		long now = p.server.getTickCount();
		Long cd = COOLDOWNS.get(p.getUUID());
		if (cd != null && cd > now) return 2 + (int) Math.ceil((cd - now) / 1200.0);
		return 0;
	}

	/** Взять заказ (у Торговки). */
	public static void take(ServerPlayer p) {
		if (state(p) != 0 || !(p.level() instanceof ServerLevel lands) || !ModDimensions.isLands(lands)) return;
		RandomSource r = lands.random;
		HunterType type = HunterType.values()[r.nextInt(HunterType.values().length)];
		int rank = r.nextFloat() < LEGENDARY_CHANCE ? 2 : 1;
		float roll = r.nextFloat();
		Mod mod = roll < SWIFT_CHANCE ? Mod.SWIFT : roll < SWIFT_CHANCE + GIANT_CHANCE ? Mod.GIANT : Mod.NONE;
		ItemStack[] arms = type == HunterType.BLOOD ? Hunt.bloodLoadout(rank, r) : Hunt.loadout(type, rank, r);
		BlockPos at = null;
		for (int i = 0; i < 16 && at == null; i++) {
			double ang = r.nextDouble() * Math.PI * 2, dist = MIN_DIST + r.nextDouble() * (MAX_DIST - MIN_DIST);
			at = Hunt.ground(lands, (int) (p.getX() + Math.cos(ang) * dist), (int) (p.getZ() + Math.sin(ang) * dist));
		}
		double x = at != null ? at.getX() + 0.5 : p.getX() + MIN_DIST, z = at != null ? at.getZ() + 0.5 : p.getZ();
		long now = p.server.getTickCount();
		Order o = new Order(nextId++, p.getUUID(), type, rank, mod, arms[0], arms[1], x, at != null ? at.getY() : p.getY(), z, now + TIME);
		ORDERS.put(p.getUUID(), o);
		com.tarkrakov.tearsofink.prologue.DiaryEntries.discover(p, "draft_mad_order");
		COOLDOWNS.put(p.getUUID(), now + COOLDOWN);
		say(p, "order.tears-of-ink.taken", name(o));
		p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.4f, 0.7f);
	}

	/** Отказаться (перезарядка остаётся). */
	public static void abandon(ServerPlayer p) {
		Order o = ORDERS.get(p.getUUID());
		if (o == null || !(p.level() instanceof ServerLevel lands)) return;
		finish(lands, o);
		say(p, "order.tears-of-ink.abandoned");
	}

	/** Отдать эссенции: каждые 3 → Частичка Души. */
	public static void tradeEssence(ServerPlayer p) {
		int have = 0;
		for (ItemStack s : p.getInventory().items) if (s.is(ModItems.HOPE_ESSENCE)) have += s.getCount();
		int motes = have / ESSENCE_PER_MOTE;
		if (motes == 0) return;
		int take = motes * ESSENCE_PER_MOTE;
		for (ItemStack s : p.getInventory().items) {
			if (take <= 0) break;
			if (!s.is(ModItems.HOPE_ESSENCE)) continue;
			int t = Math.min(take, s.getCount());
			s.shrink(t);
			take -= t;
		}
		ItemStack out = new ItemStack(ModItems.HOPE_MOTE, motes);
		if (!p.getInventory().add(out)) p.drop(out, false);
		say(p, "order.tears-of-ink.essence_traded", motes);
		p.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.3f);
	}

	static Component name(Order o) {
		Component base = Component.translatable("order.tears-of-ink.mad_name",
				Component.translatable("hunt.tears-of-ink.type." + o.type.id), Component.translatable("hunt.tears-of-ink.rank." + o.rank));
		return o.mod == Mod.NONE ? base : Component.translatable("order.tears-of-ink.mod." + o.mod.name().toLowerCase(Locale.ROOT), base);
	}

	private static void say(ServerPlayer p, String key, Object... args) {
		p.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
	}

	// ===== Ход =====
	private static void tick(MinecraftServer server) {
		long now = server.getTickCount();
		if (now % 10 != 0) return;
		COOLDOWNS.values().removeIf(until -> until <= now);
		if (ORDERS.isEmpty()) return;
		ServerLevel lands = ModDimensions.lands(server);
		if (lands == null) return;
		for (Order o : java.util.List.copyOf(ORDERS.values())) {
			ServerPlayer owner = server.getPlayerList().getPlayer(o.owner);
			if (now >= o.end) {
				if (owner != null) say(owner, "order.tears-of-ink.timeout");
				finish(lands, o);
				continue;
			}
			HunterEntity h = o.hunter == null ? null : lands.getEntity(o.hunter) instanceof HunterEntity e && e.isAlive() ? e : null;
			if (owner == null || owner.level() != lands) {
				if (h != null) unspawn(o, h);
				continue;
			}
			double dx = owner.getX() - o.x, dz = owner.getZ() - o.z, d2 = dx * dx + dz * dz;
			if (h == null) {
				o.hunter = null;
				if (d2 < SPAWN_DIST * SPAWN_DIST) spawn(lands, o, owner);
			} else {
				double hx = owner.getX() - h.getX(), hz = owner.getZ() - h.getZ();
				if (hx * hx + hz * hz > FORGET_DIST * FORGET_DIST) {
					unspawn(o, h);
				} else {
					h.setFoe(owner, true);
				}
			}
			beam(lands, o, owner, h);
		}
	}

	private static void spawn(ServerLevel lands, Order o, ServerPlayer owner) {
		HunterEntity h = ModMobs.HUNTER.create(lands);
		if (h == null) return;
		BlockPos g = Hunt.ground(lands, (int) Math.floor(o.x), (int) Math.floor(o.z));
		if (g == null) return;
		h.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0, 0);
		h.setup(o.type, o.rank, o.main.copy(), o.off.copy());
		float hp = MAD_HP * (o.mod == Mod.GIANT ? GIANT_HP : 1f);
		h.madden(hp, MAD_DAMAGE, name(o));
		if (o.mod == Mod.GIANT) h.getAttribute(Attributes.SCALE).addPermanentModifier(new AttributeModifier(
				com.tarkrakov.tearsofink.TearsOfInk.id("mad_giant"), GIANT_SCALE - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		if (o.mod == Mod.SWIFT) h.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier(
				com.tarkrakov.tearsofink.TearsOfInk.id("mad_swift"), SWIFT_SPEED - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		h.setFoe(owner, true);
		if (!lands.addFreshEntity(h)) return;
		o.hunter = h.getUUID();
		o.y = g.getY();
		say(owner, "order.tears-of-ink.found", name(o));
		owner.playNotifySound(SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 0.5f, 1.2f);
	}

	private static void unspawn(Order o, HunterEntity h) {
		h.discard();
		o.hunter = null;
	}

	private static void beam(ServerLevel lands, Order o, ServerPlayer owner, HunterEntity h) {
		Hunt.Beam b = new Hunt.Beam(BEAM_BASE + o.id, true, h == null ? -1 : h.getId(), -1,
				h == null ? o.x : h.getX(), h == null ? o.y : h.getY(), h == null ? o.z : h.getZ(), o.type.beamColor, h != null);
		ServerPlayNetworking.send(owner, b);
	}

	private static void finish(ServerLevel lands, Order o) {
		ORDERS.remove(o.owner);
		if (o.hunter != null && lands.getEntity(o.hunter) instanceof HunterEntity h) h.discard();
		ServerPlayer owner = lands.getServer().getPlayerList().getPlayer(o.owner);
		if (owner != null) ServerPlayNetworking.send(owner, new Hunt.Beam(BEAM_BASE + o.id, false, -1, -1, 0, 0, 0, 0, false));
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof HunterEntity h) || !(entity.level() instanceof ServerLevel lands)) return;
		Order o = null;
		for (Order x : ORDERS.values()) if (h.getUUID().equals(x.hunter)) o = x;
		if (o == null) return;
		ServerPlayer owner = lands.getServer().getPlayerList().getPlayer(o.owner);
		o.hunter = null;
		finish(lands, o);
		if (owner == null) return;
		RandomSource r = lands.random;
		give(owner, new ItemStack(ModItems.HOPE_ESSENCE, ESSENCE[o.rank]));
		give(owner, new ItemStack(ModItems.PIGMENT, PIGMENT_MIN + r.nextInt(PIGMENT_MAX - PIGMENT_MIN + 1)));
		// усилитель пробуждений гарантированно: Чернильный — Отголосок, Кровавый — Кристалл крови, Шпиль — один из двух
		boolean echo = o.type == HunterType.INK || (o.type == HunterType.SPIRE && r.nextBoolean());
		give(owner, new ItemStack(echo ? ModItems.INK_ECHO : ModItems.BLOOD_CRYSTAL));
		if (r.nextFloat() < WEAPON_CHANCE[o.rank]) {
			ItemStack w = (!o.off.isEmpty() && r.nextBoolean() ? o.off : o.main).copy();
			com.tarkrakov.tearsofink.weapon.forge.Gear.setTier(w, Hunt.rewardTier(o.rank, r.nextFloat())); // Рубрики — только в Купели
			give(owner, w);
		}
		say(owner, "order.tears-of-ink.done", name(o));
		owner.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 0.7f);
	}

	private static void give(ServerPlayer p, ItemStack s) {
		if (s.isEmpty()) return;
		if (!p.getInventory().add(s)) p.drop(s, false);
	}
}
