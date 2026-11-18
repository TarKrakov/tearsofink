package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Охота (PvE). Два вида:
 * — мировая: раз в 15 минут бросок кубика (HuntTuning.WORLD_CHANCE), на весь сервер;
 * — по контракту: игрок берёт у Торговки (HuntContracts), цель и охотник появляются, когда он подходит к месту.
 * Одновременно может идти несколько Охот (мировая одна, контрактов — по одному на игрока).
 * Мировая: раз в 15 минут на весь сервер. Рядом со случайным игроком (не в храме) появляется меченая цель
 * (крепче, с лучом в небо) и охотник. Охотник ждёт игроков в стороне, не дождался — дерётся сам.
 * Контракт получает каждый, кто нанёс ≥ 10% урона игроков. Цель растерзала охотника — награда двойная.
 * Если рядом с целью никого — исход разыгрывается «вдали», в цифрах.
 */
public final class Hunt {
	public static final String PREY_TAG = "tearsofink.hunt_prey";
	private static final ResourceKey<LootTable> REWARD = ResourceKey.create(Registries.LOOT_TABLE, TearsOfInk.id("gameplay/hunt_reward"));
	private static final ResourceLocation PREY_BOOST = TearsOfInk.id("hunt_prey");
	/** Цель — не выше и не ниже игрока больше чем на столько блоков. */
	private static final int HEIGHT_GAP = 12;
	private static final int FOUNTAIN_BLOCK_RADIUS = 32;
	private static final String C_LINE = "HunterLine", C_TIER = "HunterTier", C_KIND = "HunterKind", C_DIFF = "Difficulty";

	/** Луч Охоты и затемнение цели у клиентов. on=false — Охоты нет. */
	/** hunt — номер Охоты (у игрока их может быть две: мировая и контракт). */
	public record Beam(int hunt, boolean on, int entity, int entity2, double x, double y, double z, int color, boolean engaged) implements CustomPacketPayload {
		public static final Type<Beam> TYPE = new Type<>(TearsOfInk.id("hunt_beam"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Beam> CODEC = StreamCodec.of((buf, b) -> {
			buf.writeVarInt(b.hunt());
			buf.writeBoolean(b.on());
			buf.writeVarInt(b.entity());
			buf.writeVarInt(b.entity2());
			buf.writeDouble(b.x());
			buf.writeDouble(b.y());
			buf.writeDouble(b.z());
			buf.writeInt(b.color());
			buf.writeBoolean(b.engaged());
		}, buf -> new Beam(buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
				buf.readInt(), buf.readBoolean()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Одна Охота: мировая или по контракту. */
	static final class Active {
		final int id;
		/** null — мировая; иначе — чей контракт. */
		@Nullable
		final UUID owner;
		/** Цели (у разделённой — две). Убитые убираются из списка. */
		final List<UUID> preys = new ArrayList<>();
		@Nullable
		UUID hunter;
		HuntTargets.Target target;
		PreyPrefix prefix = PreyPrefix.NONE;
		HunterType type;
		int rank;
		ItemStack main = ItemStack.EMPTY, off = ItemStack.EMPTY;
		/** Какое оружие запомнит контракт. */
		ItemStack weapon = ItemStack.EMPTY;
		long end, engageAt;
		boolean engaged, hunterDead, spawned;
		/** Охотник победил, Чернильный бесится до этого времени. */
		long frenzyUntil = -1;
		/** Где (у контракта до появления — намеченное место). */
		double x, y, z;
		final Map<UUID, Float> damage = new HashMap<>();
		float hunterDamage;

		Active(int id, @Nullable UUID owner) {
			this.id = id;
			this.owner = owner;
		}

		boolean contract() {
			return owner != null;
		}
	}

	private static final List<Active> ACTIVE = new ArrayList<>();
	private static int nextId = 1;
	/** Резвая цель: отложенный второй удар. */
	private record Echo(UUID prey, UUID target, float damage, long at) {}
	private static final List<Echo> ECHOES = new ArrayList<>();
	private static boolean echoing;
	/** Множитель урона целей (по рангу охотника, силе цели и особенности). */
	private static final Map<UUID, Float> DAMAGE_MULT = new HashMap<>();
	private static long nextStart = -1;

	/** Во сколько раз усилен урон этого существа (цель Охоты), иначе 1. */
	public static float outgoingMultiplier(@Nullable Entity e) {
		if (e == null || echoing || DAMAGE_MULT.isEmpty()) return 1f;
		Float m = DAMAGE_MULT.get(e.getUUID());
		return m == null ? 1f : m;
	}

	/** Забыть усиление (цель пала или исчезла). */
	public static void forgetPrey(UUID id) {
		DAMAGE_MULT.remove(id);
	}

	private Hunt() {}

	public static void init() {
		HuntTargets.init();
		HuntDuels.init();
		HuntContracts.init();
		PayloadTypeRegistry.playS2C().register(Beam.TYPE, Beam.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Hunt::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			ACTIVE.clear();
			ECHOES.clear();
			DAMAGE_MULT.clear();
			nextStart = -1;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (ACTIVE.isEmpty()) return true;
			Entity by = source.getEntity();
			// резвая цель: после удара вблизи — ещё один быстрый
			if (!echoing && by != null && by == source.getDirectEntity() && entity.level() instanceof ServerLevel sl) {
				Active ab = byPrey(by.getUUID());
				if (ab != null && ab.prefix == PreyPrefix.SWIFT) {
					ECHOES.add(new Echo(by.getUUID(), entity.getUUID(), amount * HuntTuning.SWIFT_ECHO,
							sl.getServer().getTickCount() + HuntTuning.SWIFT_ECHO_DELAY));
				}
			}
			Active a = byPrey(entity.getUUID());
			if (a != null) {
				if (by instanceof ServerPlayer p) a.damage.merge(p.getUUID(), Math.min(amount, entity.getHealth()), Float::sum);
				else if (by instanceof HunterEntity) a.hunterDamage += Math.min(amount, entity.getHealth());
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(Hunt::onDeath);
		// цель Охоты, оставшаяся в мире после перезапуска или выгрузки, — растворяется
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.getTags().contains(PREY_TAG) && byPrey(entity.getUUID()) == null) entity.discard();
		});
	}

	@Nullable
	private static Active byPrey(UUID id) {
		for (Active a : ACTIVE) if (a.preys.contains(id)) return a;
		return null;
	}

	@Nullable
	private static Active byHunter(UUID id) {
		for (Active a : ACTIVE) if (id.equals(a.hunter)) return a;
		return null;
	}

	@Nullable
	private static Active world() {
		for (Active a : ACTIVE) if (!a.contract()) return a;
		return null;
	}

	/** Идёт ли мировая Охота. */
	public static boolean running() {
		return world() != null;
	}

	public static boolean blocksFountainTravel(ServerPlayer player) {
		if (contractOf(player.getUUID()) != null || MadOrders.state(player) == 1) return true;
		Active hunt = world();
		if (hunt == null || !hunt.spawned) return false;
		double dx = player.getX() - hunt.x, dz = player.getZ() - hunt.z;
		return dx * dx + dz * dz <= FOUNTAIN_BLOCK_RADIUS * FOUNTAIN_BLOCK_RADIUS;
	}

	/** Контракт игрока, если взят: {секунд осталось, появились ли цели}; иначе null. */
	@Nullable
	static Active contractOf(UUID player) {
		for (Active a : ACTIVE) if (player.equals(a.owner)) return a;
		return null;
	}

	// ===== Запуск =====

	private static void tick(MinecraftServer server) {
		ServerLevel lands = ModDimensions.lands(server);
		if (lands == null) return;
		long now = server.getTickCount();
		if (nextStart < 0) nextStart = now + HuntTuning.FIRST_DELAY;
		if (HuntTuning.WORLD_HUNTS && world() == null && now >= nextStart && now % 20 == 0) {
			if (lands.random.nextFloat() < HuntTuning.WORLD_CHANCE) {
				if (!start(lands, null)) return; // некого/негде — пробуем через секунду
			} else {
				nextStart = now + HuntTuning.INTERVAL; // кубик не выпал — ждём следующего броска
			}
		}
		if (ACTIVE.isEmpty()) return;
		tickEchoes(lands, now);
		for (Active a : List.copyOf(ACTIVE)) {
			if (!ACTIVE.contains(a)) continue;
			try {
				update(lands, a, now);
			} catch (Exception e) { // одна сломанная Охота не должна ронять сервер
				TearsOfInk.LOGGER.error("Охота {} сломалась, снимаю: {}", a.id, e.toString());
				finish(lands, a);
			}
		}
	}

	/** Начать мировую Охоту. who — рядом с кем (null — случайный подходящий игрок). Ответ — удалось ли. */
	/** Заказанная Охота (команда для тестов): что не задано (null) — как обычно, случайно. */
	public record Spec(@Nullable HunterType type, int rank, @Nullable ItemStack main, @Nullable ItemStack off,
					   @Nullable HuntTargets.Target target, @Nullable PreyPrefix prefix) {}

	public static boolean start(ServerLevel lands, @Nullable ServerPlayer who) {
		return start(lands, who, null);
	}

	public static boolean start(ServerLevel lands, @Nullable ServerPlayer who, @Nullable Spec spec) {
		if (world() != null) return false;
		List<ServerPlayer> candidates = new ArrayList<>();
		if (who != null) candidates.add(who);
		else for (ServerPlayer p : lands.players()) {
			if (p.isAlive() && !p.isSpectator() && !Sanctuary.contains(p)) candidates.add(p);
		}
		if (candidates.isEmpty()) return false; // все в храмах — ждём
		RandomSource random = lands.random;
		ServerPlayer target = candidates.get(random.nextInt(candidates.size()));
		HuntTargets.Target t = spec != null && spec.target() != null ? spec.target() : HuntTargets.pick(random);
		if (t == null) return false;
		BlockPos at = spot(lands, target, random);
		if (at == null) return false;
		Active a = new Active(nextId++, null);
		a.target = t;
		a.rank = spec != null && spec.rank() >= 0 ? spec.rank() : pickRank(random);
		a.type = spec != null && spec.type() != null ? spec.type() : HunterType.byIndex(random.nextInt(HunterType.values().length));
		a.prefix = spec != null && spec.prefix() != null ? spec.prefix() : pickPrefix(random);
		ItemStack[] arms = loadout(a.type, a.rank, random);
		if (spec != null && spec.main() != null) {
			arms = new ItemStack[]{spec.main(), spec.off() != null ? spec.off() : ItemStack.EMPTY};
		}
		a.main = arms[0];
		a.off = arms[1];
		a.weapon = (!a.off.isEmpty() && random.nextBoolean() ? a.off : a.main).copy(); // контракт помнит одно из двух
		long now = lands.getServer().getTickCount();
		a.end = now + HuntTuning.DURATION;
		if (!spawn(lands, a, at, target.position(), now)) return false;
		Mob prey = (Mob) lands.getEntity(a.preys.get(0));
		announce(lands, a, Component.translatable("hunt.tears-of-ink.start", hunterName(a), arms(a),
				preyTitle(prey, a.prefix), target.getDisplayName()).withStyle(ChatFormatting.GOLD));
		for (ServerPlayer p : lands.players()) p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.5f, 1.3f);
		return true;
	}

	/** Контракт: Охота с заранее известными целью, охотником и оружием; появится, когда игрок подойдёт к месту. */
	static boolean startContract(ServerLevel lands, ServerPlayer owner, HuntContracts.Offer o, double x, double z) {
		if (contractOf(owner.getUUID()) != null) return false;
		Active a = new Active(nextId++, owner.getUUID());
		a.target = o.target();
		a.rank = o.rank();
		a.type = o.type();
		a.prefix = o.prefix();
		a.main = o.main().copy();
		a.off = o.off().copy();
		a.weapon = (!a.off.isEmpty() && lands.random.nextBoolean() ? a.off : a.main).copy();
		a.x = x;
		a.z = z;
		a.y = owner.getY();
		a.end = lands.getServer().getTickCount() + HuntTuning.CONTRACT_TIME;
		ACTIVE.add(a);
		return true;
	}

	/** Отказаться от контракта (у Торговки). */
	static void abandon(ServerLevel lands, UUID owner) {
		Active a = contractOf(owner);
		if (a != null) finish(lands, a);
	}

	/** Цели и охотник появляются в мире. near — откуда идёт игрок (охотник встанет по другую сторону). */
	private static boolean spawn(ServerLevel lands, Active a, BlockPos at, net.minecraft.world.phys.Vec3 near, long now) {
		RandomSource random = lands.random;
		List<Mob> preys = new ArrayList<>();
		int count = a.prefix == PreyPrefix.SPLIT ? 2 : 1; // разделённая — две цели поменьше
		for (int i = 0; i < count; i++) {
			Mob m = makePrey(lands, a.target, i == 0 ? at : near2(lands, at, random), a.rank, a.prefix, random);
			if (m != null) preys.add(m);
		}
		if (preys.isEmpty()) return false;
		HunterEntity hunter = ModMobs.HUNTER.create(lands);
		if (hunter == null) {
			for (Mob m : preys) {
				forgetPrey(m.getUUID());
				m.discard();
			}
			return false;
		}
		BlockPos hp = near(lands, at, near, random);
		hunter.moveTo(hp.getX() + 0.5, hp.getY(), hp.getZ() + 0.5, 0, 0);
		hunter.setup(a.type, a.rank, a.main.copy(), a.off.copy());
		hunter.setFoe(preys.get(0), false);
		a.preys.clear();
		for (Mob m : preys) a.preys.add(m.getUUID());
		a.hunter = hunter.getUUID();
		a.engaged = false;
		a.hunterDead = false;
		a.damage.clear();
		a.hunterDamage = 0;
		a.engageAt = now + HuntTuning.HUNTER_WAIT;
		a.x = at.getX() + 0.5;
		a.y = at.getY();
		a.z = at.getZ() + 0.5;
		if (!ACTIVE.contains(a)) ACTIVE.add(a); // до появления в мире: иначе цель «чужая» и растворяется сразу
		boolean added = false;
		for (Mob m : preys) added |= lands.addFreshEntity(m);
		if (!added) {
			for (Mob m : preys) {
				forgetPrey(m.getUUID());
				m.discard();
			}
			hunter.discard();
			a.preys.clear();
			a.hunter = null;
			if (!a.contract()) ACTIVE.remove(a);
			return false;
		}
		lands.addFreshEntity(hunter);
		a.spawned = true;
		return true;
	}

	static Component hunterName(Active a) {
		return Component.translatable("hunt.tears-of-ink.hunter_name",
				Component.translatable("hunt.tears-of-ink.type." + a.type.id), Component.translatable("hunt.tears-of-ink.rank." + a.rank));
	}

	static Component arms(Active a) {
		return armsText(a.main, a.off);
	}

	static Component armsText(ItemStack main, ItemStack off) {
		return off.isEmpty()
				? Component.translatable("hunt.tears-of-ink.weapon_one", main.getHoverName())
				: Component.translatable("hunt.tears-of-ink.weapon_two", main.getHoverName(), off.getHoverName());
	}

	static int pickRank(RandomSource random) {
		int total = 0;
		for (int w : HuntTuning.RANK_WEIGHT) total += w;
		int r = random.nextInt(total);
		for (int i = 0; i < HuntTuning.RANK_WEIGHT.length; i++) {
			r -= HuntTuning.RANK_WEIGHT[i];
			if (r < 0) return i;
		}
		return 0;
	}

	/** Оружие охотника: один меч / пара одинаковых / клинок + двуручник в левой (правило рук игрока). {правая, левая} */
	static ItemStack[] loadout(HunterType type, int rank, RandomSource random) {
		if (type == HunterType.BLOOD) return bloodLoadout(rank, random);
		if (type == HunterType.SPIRE) return spireLoadout(rank, random);
		HunterWeaponItem great = (HunterWeaponItem) ModItems.HUNTER_GREATSWORD, saber = (HunterWeaponItem) ModItems.HUNTER_SABER;
		HunterWeaponItem kind = random.nextBoolean() ? great : saber;
		ItemStack weapon = kind.make(type, rank), offWeapon = ItemStack.EMPTY;
		int lw = 0;
		for (int w : HuntTuning.LOADOUT_WEIGHT) lw += w;
		int roll = random.nextInt(lw);
		if (roll >= HuntTuning.LOADOUT_WEIGHT[0] + HuntTuning.LOADOUT_WEIGHT[1]) { // разные
			weapon = saber.make(type, rank);
			offWeapon = great.make(type, rank);
		} else if (roll >= HuntTuning.LOADOUT_WEIGHT[0]) { // пара одинаковых
			offWeapon = kind.make(type, rank);
		}
		return new ItemStack[]{weapon, offWeapon};
	}

	/** Охотник Шпиля — оружие автора: Палач, Секира (двуручные), Скимитар, Кинжал (одноручные); ступень = ранг. */
	static ItemStack[] spireLoadout(int rank, RandomSource random) {
		int[] w = HuntTuning.SPIRE_LOADOUT[Math.max(0, Math.min(rank, HuntTuning.SPIRE_LOADOUT.length - 1))];
		int total = 0;
		for (int x : w) total += x;
		int r = random.nextInt(Math.max(1, total)), pick = 0;
		while (pick < w.length - 1 && r >= w[pick]) r -= w[pick++];
		net.minecraft.world.item.Item[] all = {ModItems.CLEAVE_GREATSWORD, ModItems.ROARING_AXE, ModItems.WAVE_GREATSWORD, ModItems.RAPID_KNIFE};
		net.minecraft.world.item.Item[] one = {ModItems.WAVE_GREATSWORD, ModItems.RAPID_KNIFE}, two = {ModItems.CLEAVE_GREATSWORD, ModItems.ROARING_AXE};
		ItemStack main, off = ItemStack.EMPTY;
		switch (pick) {
			case 0 -> main = new ItemStack(all[random.nextInt(all.length)]);
			case 1 -> {
				main = new ItemStack(all[random.nextInt(all.length)]);
				off = main.copy();
			}
			default -> {
				main = new ItemStack(one[random.nextInt(one.length)]);
				off = new ItemStack(two[random.nextInt(two.length)]);
			}
		}
		com.tarkrakov.tearsofink.weapon.forge.Gear.setTier(main, rank);
		if (!off.isEmpty()) com.tarkrakov.tearsofink.weapon.forge.Gear.setTier(off, rank);
		return new ItemStack[]{main, off};
	}

	/** Кровавый охотник — только оружие Крови (веса — HuntTuning.BLOOD_LOADOUT). */
	static ItemStack[] bloodLoadout(int rank, RandomSource random) {
		int[] w = HuntTuning.BLOOD_LOADOUT[Math.max(0, Math.min(rank, HuntTuning.BLOOD_LOADOUT.length - 1))];
		int total = 0;
		for (int x : w) total += x;
		int r = random.nextInt(Math.max(1, total)), pick = 0;
		while (pick < w.length - 1 && r >= w[pick]) r -= w[pick++];
		ItemStack t = new ItemStack(ModItems.BLOOD_TALVAR), d = new ItemStack(ModItems.BLOOD_DUSSAK);
		return switch (pick) {
			case 0 -> new ItemStack[]{t, ItemStack.EMPTY};
			case 1 -> new ItemStack[]{d, ItemStack.EMPTY};
			case 2 -> random.nextBoolean() ? new ItemStack[]{t, d} : new ItemStack[]{d, t};
			case 3 -> new ItemStack[]{t, t.copy()};
			default -> new ItemStack[]{d, d.copy()};
		};
	}

	static PreyPrefix pickPrefix(RandomSource random) {
		int total = 0;
		for (int w : HuntTuning.PREFIX_WEIGHT) total += w;
		int r = random.nextInt(total);
		for (int i = 0; i < HuntTuning.PREFIX_WEIGHT.length; i++) {
			r -= HuntTuning.PREFIX_WEIGHT[i];
			if (r < 0) return PreyPrefix.values()[i];
		}
		return PreyPrefix.NONE;
	}

	/** Меченая цель: крепче по рангу охотника, плюс особенность. */
	@Nullable
	static Mob makePrey(ServerLevel lands, HuntTargets.Target t, BlockPos at, int rank, PreyPrefix prefix, RandomSource random) {
		Entity e = t.type().create(lands);
		if (!(e instanceof Mob prey)) {
			if (e != null) e.discard();
			return null;
		}
		prey.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
		prey.finalizeSpawn(lands, lands.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
		prey.setPersistenceRequired();
		prey.addTag(PREY_TAG);
		float hp = HuntTuning.PREY_HP[rank] * t.hp(), dmg = HuntTuning.PREY_DAMAGE[rank] * t.damage();
		switch (prefix) {
			case GIANT -> {
				hp *= HuntTuning.GIANT_HP;
				dmg *= HuntTuning.GIANT_DAMAGE;
				boost(prey, Attributes.SCALE, HuntTuning.GIANT_SCALE);
			}
			case SPLIT -> {
				hp *= HuntTuning.SPLIT_STATS;
				dmg *= HuntTuning.SPLIT_STATS;
				boost(prey, Attributes.SCALE, HuntTuning.SPLIT_SCALE);
			}
			case SWIFT -> boost(prey, Attributes.MOVEMENT_SPEED, HuntTuning.SWIFT_SPEED);
			default -> {}
		}
		boost(prey, Attributes.MAX_HEALTH, hp);
		DAMAGE_MULT.put(prey.getUUID(), dmg); // урон умножается у всех целей одинаково (PreyDamageMixin)
		prey.setHealth(prey.getMaxHealth());
		if (prefix != PreyPrefix.NONE) {
			prey.setCustomName(Component.translatable("hunt.tears-of-ink.prefix_name." + prefix.id, prey.getType().getDescription()));
			prey.setCustomNameVisible(true);
		}
		return prey;
	}

	/** Место для второй половины — рядом с первой. */
	private static BlockPos near2(ServerLevel level, BlockPos at, RandomSource random) {
		for (int attempt = 0; attempt < 8; attempt++) {
			BlockPos g = ground(level, at.getX() + random.nextInt(7) - 3, at.getZ() + random.nextInt(7) - 3);
			if (g != null && Math.abs(g.getY() - at.getY()) <= 3) return g;
		}
		return at;
	}

	/** Как цель зовётся в объявлении: «резвого паука», «гигантского зомби», «разделённого надвое скелета». */
	public static Component preyTitle(Entity prey, PreyPrefix prefix) {
		Component name = preyName(prey);
		return prefix == PreyPrefix.NONE ? name : Component.translatable("hunt.tears-of-ink.prefix." + prefix.id, name);
	}

	private static void boost(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, float mult) {
		AttributeInstance inst = e.getAttribute(attr);
		if (inst == null) return;
		inst.addPermanentModifier(new AttributeModifier(PREY_BOOST, mult - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	/** То же, но по виду существа (для доски контрактов — цели ещё нет в мире). */
	public static Component preyTitle(net.minecraft.world.entity.EntityType<?> type, PreyPrefix prefix) {
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		Component name = Component.translatableWithFallback("hunt.tears-of-ink.prey." + id.getNamespace() + "." + id.getPath(),
				type.getDescription().getString());
		return prefix == PreyPrefix.NONE ? name : Component.translatable("hunt.tears-of-ink.prefix." + prefix.id, name);
	}

	public static Component preyName(Entity prey) {
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(prey.getType());
		String key = "hunt.tears-of-ink.prey." + id.getNamespace() + "." + id.getPath();
		return Component.translatableWithFallback(key, prey.getType().getDescription().getString());
	}

	@Nullable
	private static BlockPos spot(ServerLevel level, ServerPlayer player, RandomSource random) {
		InkLandsLayout layout = InkLandsLayout.of(level);
		for (int attempt = 0; attempt < 48; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			// маленькая дальность прорисовки мира — понемногу ближе (цель должна «жить», а не стоять замороженной)
			double shrink = attempt < 24 ? 1 : 0.7;
			double d = (HuntTuning.SPAWN_MIN + random.nextDouble() * (HuntTuning.SPAWN_MAX - HuntTuning.SPAWN_MIN)) * shrink;
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * d), z = (int) Math.floor(player.getZ() + Math.sin(angle) * d);
			BlockPos ground = ground(level, x, z);
			if (ground == null || !level.isPositionEntityTicking(ground)) continue;
			if (Math.abs(ground.getY() - player.getY()) > HEIGHT_GAP) continue; // примерно на уровне игрока
			if (layout != null && (layout.depthInside(x, z) < 20 || Sanctuary.near(layout, x, z, 8))) continue;
			return ground;
		}
		return null;
	}

	/** Место для охотника — в стороне от цели, подальше от игрока. */
	private static BlockPos near(ServerLevel level, BlockPos prey, net.minecraft.world.phys.Vec3 from, RandomSource random) {
		double away = Math.atan2(prey.getZ() - from.z, prey.getX() - from.x);
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = away + (random.nextDouble() - 0.5) * Math.PI;
			double d = HuntTuning.WAIT_MIN + random.nextDouble() * (HuntTuning.WAIT_MAX - HuntTuning.WAIT_MIN);
			BlockPos g = ground(level, (int) Math.floor(prey.getX() + Math.cos(angle) * d), (int) Math.floor(prey.getZ() + Math.sin(angle) * d));
			if (g != null && Math.abs(g.getY() - prey.getY()) <= 4) return g; // охотник — на одном уровне с целью
		}
		return prey;
	}

	/** Настоящая земля: сверху вниз, мимо крон и веток (на дереве цель недосягаема). */
	@Nullable
	static BlockPos ground(ServerLevel level, int x, int z) {
		BlockPos col = new BlockPos(x, 0, z);
		if (!level.hasChunkAt(col)) return null;
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		for (int y = top; y > top - 48 && y > level.getMinBuildHeight(); y--) {
			BlockPos at = new BlockPos(x, y, z);
			var below = level.getBlockState(at.below());
			if (below.getCollisionShape(level, at.below()).isEmpty()) continue;
			if (below.is(net.minecraft.tags.BlockTags.LOGS) || below.is(net.minecraft.tags.BlockTags.LEAVES)) continue; // ветка — ниже
			if (!level.getFluidState(at.below()).isEmpty() || !level.getFluidState(at).isEmpty()) return null;
			if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()
					|| !level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) continue;
			if (!level.canSeeSky(at) && y < top - 3 && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, at) == 0) return null; // пещера
			return at;
		}
		return null;
	}

	// ===== Ход Охоты =====

	private static List<LivingEntity> alive(ServerLevel lands, Active a) {
		List<LivingEntity> list = new ArrayList<>();
		for (UUID id : a.preys) {
			if (lands.getEntity(id) instanceof LivingEntity l && l.isAlive()) list.add(l);
		}
		return list;
	}

	@Nullable
	private static HunterEntity hunterOf(ServerLevel lands, Active a) {
		return a.hunter != null && lands.getEntity(a.hunter) instanceof HunterEntity h && h.isAlive() ? h : null;
	}

	private static void update(ServerLevel lands, Active a, long now) {
		if (a.contract() && !a.spawned) {
			waitForOwner(lands, a, now);
			return;
		}
		HunterEntity hunter = hunterOf(lands, a);
		if (a.frenzyUntil >= 0) { // охотник уже победил
			if (hunter == null || now >= a.frenzyUntil) finish(lands, a);
			else if (now % 20 == 0) {
				ServerPlayer near = hunter.level().getNearestPlayer(hunter, 32) instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator()
						&& !Sanctuary.contains(sp) ? sp : null;
				hunter.setFoe(near, true);
			}
			return;
		}
		List<LivingEntity> preys = alive(lands, a);
		if (now >= a.end) {
			announce(lands, a, Component.translatable(a.contract() ? "hunt.tears-of-ink.contract_timeout" : "hunt.tears-of-ink.timeout")
					.withStyle(ChatFormatting.GRAY));
			for (LivingEntity p : preys) vanish(lands, p);
			finish(lands, a);
			return;
		}
		if (preys.isEmpty()) { // цели пропали (выгрузились)
			if (a.contract()) unspawn(lands, a); // контракт подождёт, пока игрок вернётся
			else remote(lands, a);
			return;
		}
		LivingEntity prey = preys.get(0);
		a.x = prey.getX();
		a.y = prey.getY();
		a.z = prey.getZ();
		boolean playerNear = false, anyoneClose = false;
		for (ServerPlayer p : lands.players()) {
			if (p.isSpectator()) continue;
			for (LivingEntity pr : preys) {
				double d = p.distanceTo(pr);
				if (d < HuntTuning.PLAYER_ENGAGE) playerNear = true;
				if (d < HuntTuning.REMOTE) anyoneClose = true;
			}
		}
		if (!anyoneClose && now % 20 == 0) {
			if (a.contract()) unspawn(lands, a);
			else remote(lands, a);
			return;
		}
		if (hunter != null) {
			if (!a.engaged && (playerNear || now >= a.engageAt)) {
				a.engaged = true;
				announce(lands, a, Component.translatable("hunt.tears-of-ink.engage", hunter.getCustomName()).withStyle(ChatFormatting.GRAY));
			}
			LivingEntity closest = prey;
			for (LivingEntity pr : preys) if (hunter.distanceTo(pr) < hunter.distanceTo(closest)) closest = pr;
			hunter.setFoe(closest, a.engaged);
		}
		if (now % 10 == 0) beam(lands, a, prey.getId(), preys.size() > 1 ? preys.get(1).getId() : -1);
		if (now % 4 == 0) {
			for (LivingEntity pr : preys) {
				lands.sendParticles(ParticleTypes.SQUID_INK, pr.getX(), pr.getY() + pr.getBbHeight() * 0.6, pr.getZ(), 2, 0.3, 0.4, 0.3, 0.01);
			}
		}
	}

	/** Контракт ещё не появился: метка ведёт игрока к месту; подошёл — цель и охотник появляются. */
	private static void waitForOwner(ServerLevel lands, Active a, long now) {
		if (now >= a.end) {
			announce(lands, a, Component.translatable("hunt.tears-of-ink.contract_timeout").withStyle(ChatFormatting.GRAY));
			finish(lands, a);
			return;
		}
		ServerPlayer owner = lands.getServer().getPlayerList().getPlayer(a.owner);
		if (owner == null || owner.level() != lands) return;
		if (now % 10 == 0) beam(lands, a, -1, -1);
		if (now % 20 != 0) return;
		double dx = owner.getX() - a.x, dz = owner.getZ() - a.z;
		if (dx * dx + dz * dz > HuntTuning.CONTRACT_SPAWN_DIST * HuntTuning.CONTRACT_SPAWN_DIST) return;
		RandomSource random = lands.random;
		// место у отметки (рядом, если там вода или обрыв); не вышло — рядом с игроком, но не вплотную
		BlockPos at = null;
		for (int attempt = 0; attempt < 24 && at == null; attempt++) {
			int r = attempt < 12 ? 6 : 16;
			int x = (int) Math.floor(a.x) + random.nextInt(r * 2 + 1) - r, z = (int) Math.floor(a.z) + random.nextInt(r * 2 + 1) - r;
			BlockPos g = ground(lands, x, z);
			if (g != null && lands.isPositionEntityTicking(g) && !Sanctuary.near(lands, x, z, 8)) at = g;
		}
		if (at == null) at = spot(lands, owner, random);
		if (at == null) return;
		if (spawn(lands, a, at, owner.position(), now)) {
			announce(lands, a, Component.translatable("hunt.tears-of-ink.contract_found", hunterName(a)).withStyle(ChatFormatting.GOLD));
			owner.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.5f, 1.3f);
		}
	}

	/** Контракт: все ушли далеко — цель и охотник растворяются и появятся заново, когда игрок вернётся. */
	private static void unspawn(ServerLevel lands, Active a) {
		HunterEntity hunter = hunterOf(lands, a);
		if (hunter != null) hunter.discard();
		for (UUID id : a.preys) {
			Entity e = lands.getEntity(id);
			if (e != null) e.discard();
			forgetPrey(id);
		}
		a.preys.clear();
		a.hunter = null;
		a.spawned = false;
		a.engaged = false;
	}

	private static void beam(ServerLevel lands, Active a, int e1, int e2) {
		Beam beam = new Beam(a.id, true, e1, e2, a.x, a.y, a.z, a.type.beamColor, a.engaged);
		for (ServerPlayer p : audience(lands, a)) ServerPlayNetworking.send(p, beam);
	}

	private static void beamOff(ServerLevel lands, Active a) {
		Beam off = new Beam(a.id, false, -1, -1, 0, 0, 0, 0, false);
		for (ServerPlayer p : lands.getServer().getPlayerList().getPlayers()) ServerPlayNetworking.send(p, off);
	}

	/** Кому видно: мировую — всем в Землях; контракт — хозяину и тем, кто рядом. */
	private static List<ServerPlayer> audience(ServerLevel lands, Active a) {
		if (!a.contract()) return lands.players();
		List<ServerPlayer> list = new ArrayList<>();
		for (ServerPlayer p : lands.players()) {
			double dx = p.getX() - a.x, dz = p.getZ() - a.z;
			if (p.getUUID().equals(a.owner) || dx * dx + dz * dz < HuntTuning.CONTRACT_AUDIENCE * HuntTuning.CONTRACT_AUDIENCE) list.add(p);
		}
		return list;
	}

	/** Резвая цель: второй быстрый удар, если враг всё ещё рядом. */
	private static void tickEchoes(ServerLevel lands, long now) {
		if (ECHOES.isEmpty()) return;
		var it = ECHOES.iterator();
		List<Echo> due = new ArrayList<>();
		while (it.hasNext()) {
			Echo e = it.next();
			if (e.at() <= now) {
				due.add(e);
				it.remove();
			}
		}
		for (Echo e : due) {
			if (!(lands.getEntity(e.prey()) instanceof Mob m) || !m.isAlive()) continue;
			if (!(lands.getEntity(e.target()) instanceof LivingEntity t) || !t.isAlive()) continue;
			if (m.distanceTo(t) > 3.5 + t.getBbWidth() / 2) continue;
			m.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
			echoing = true; // второй удар — без повторного усиления
			try {
				t.hurt(m.damageSources().mobAttack(m), e.damage());
			} finally {
				echoing = false;
			}
		}
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (ACTIVE.isEmpty() || !(entity.level() instanceof ServerLevel lands)) return;
		Active ah = byHunter(entity.getUUID());
		if (ah != null) {
			if (ah.frenzyUntil >= 0) { // Чернильного в бешенстве одолели игроки
				announce(lands, ah, Component.translatable("hunt.tears-of-ink.frenzy_end").withStyle(ChatFormatting.GOLD));
				finish(lands, ah);
				return;
			}
			ah.hunterDead = true;
			announce(lands, ah, Component.translatable("hunt.tears-of-ink.devoured").withStyle(ChatFormatting.DARK_RED));
			return;
		}
		Active a = byPrey(entity.getUUID());
		if (a == null) return;
		a.preys.remove(entity.getUUID());
		forgetPrey(entity.getUUID());
		if (!a.preys.isEmpty()) { // разделённая: одна половина пала, вторая ещё жива
			announce(lands, a, Component.translatable("hunt.tears-of-ink.half_down").withStyle(ChatFormatting.GRAY));
			return;
		}
		HunterEntity hunter = hunterOf(lands, a);
		float players = 0;
		for (float f : a.damage.values()) players += f;
		boolean hunterWon = source.getEntity() instanceof HunterEntity && players < a.hunterDamage;
		if (hunterWon || players <= 0) {
			hunterWins(lands, a, hunter);
			return;
		}
		List<Component> names = new ArrayList<>();
		for (var e : a.damage.entrySet()) {
			if (e.getValue() < players * HuntTuning.CONTRIBUTION) continue;
			ServerPlayer p = lands.getServer().getPlayerList().getPlayer(e.getKey());
			if (p == null) continue;
			reward(lands, p, a, entity);
			names.add(p.getDisplayName());
		}
		MutableComponent list = Component.empty();
		for (int i = 0; i < names.size(); i++) {
			if (i > 0) list.append(", ");
			list.append(names.get(i));
		}
		announce(lands, a, Component.translatable(a.hunterDead ? "hunt.tears-of-ink.won_double" : "hunt.tears-of-ink.won", list)
				.withStyle(ChatFormatting.GOLD));
		finish(lands, a);
	}

	private static void hunterWins(ServerLevel lands, Active a, @Nullable HunterEntity hunter) {
		Component name = hunter != null ? hunter.getCustomName() : hunterName(a);
		announce(lands, a, Component.translatable(a.contract() ? "hunt.tears-of-ink.contract_lost" : "hunt.tears-of-ink.hunter_won", name)
				.withStyle(ChatFormatting.GRAY));
		if (hunter != null && a.type == HunterType.INK) { // Чернильный, убив цель, бросается на живых
			a.frenzyUntil = lands.getServer().getTickCount() + HuntTuning.INK_FRENZY;
			announce(lands, a, Component.translatable("hunt.tears-of-ink.frenzy", name).withStyle(ChatFormatting.DARK_PURPLE));
			beamOff(lands, a);
			return;
		}
		finish(lands, a);
	}

	/** Мировая Охота вдали от всех — исход в цифрах. */
	private static void remote(ServerLevel lands, Active a) {
		boolean hunterWins = !a.hunterDead && lands.random.nextFloat() < HuntTuning.REMOTE_HUNTER_WINS[a.rank];
		announce(lands, a, Component.translatable(hunterWins ? "hunt.tears-of-ink.remote_hunter" : "hunt.tears-of-ink.remote_prey", hunterName(a))
				.withStyle(ChatFormatting.GRAY));
		finish(lands, a);
	}

	/** Конец Охоты: охотник уходит, оставшиеся цели растворяются. */
	private static void finish(ServerLevel lands, Active a) {
		HunterEntity hunter = hunterOf(lands, a);
		if (hunter != null) vanish(lands, hunter);
		for (UUID id : a.preys) {
			Entity e = lands.getEntity(id);
			if (e != null && e.isAlive()) e.discard();
			forgetPrey(id);
		}
		ECHOES.removeIf(e -> a.preys.contains(e.prey()));
		a.preys.clear();
		ACTIVE.remove(a);
		if (!a.contract()) nextStart = lands.getServer().getTickCount() + HuntTuning.INTERVAL;
		beamOff(lands, a);
	}

	/** Растворяется в чернилах. */
	private static void vanish(ServerLevel level, Entity e) {
		level.sendParticles(ParticleTypes.SQUID_INK, e.getX(), e.getY() + 1, e.getZ(), 30, 0.4, 0.8, 0.4, 0.03);
		level.playSound(null, e.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 1f, 0.6f);
		e.discard();
	}

	/** Остановить все Охоты (команда). */
	public static void stop(ServerLevel lands) {
		for (Active a : List.copyOf(ACTIVE)) finish(lands, a);
	}

	/** Сдвинуть следующую Охоту (команда). */
	public static void scheduleIn(MinecraftServer server, int ticks) {
		nextStart = server.getTickCount() + ticks;
	}

	public static long ticksUntilNext(MinecraftServer server) {
		return world() != null ? 0 : Math.max(0, nextStart - server.getTickCount());
	}

	// ===== Награда =====

	private static void reward(ServerLevel lands, ServerPlayer p, Active a, LivingEntity prey) {
		int contracts = a.hunterDead ? 2 : 1; // цель растерзала охотника — ещё один контракт
		for (int i = 0; i < contracts; i++) give(p, contract(a.weapon, a.rank));
		LootTable table = lands.getServer().reloadableRegistries().getLootTable(REWARD);
		LootParams params = new LootParams.Builder(lands)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, prey.position())
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, p)
				.create(LootContextParamSets.GIFT);
		for (ItemStack s : table.getRandomItems(params)) give(p, s);
		if (a.type == HunterType.INK) { // охотник Чернил — Осколки безумия для Проклятого алтаря
			give(p, new ItemStack(ModItems.MADNESS_SHARD, com.tarkrakov.tearsofink.weapon.forge.Gear.SHARDS_CONTRACT_MIN
					+ lands.random.nextInt(com.tarkrakov.tearsofink.weapon.forge.Gear.SHARDS_CONTRACT_MAX - com.tarkrakov.tearsofink.weapon.forge.Gear.SHARDS_CONTRACT_MIN + 1)));
		}
		if (a.prefix != PreyPrefix.NONE) { // особая цель — больше Пигмента
			give(p, new ItemStack(ModItems.PIGMENT, HuntTuning.PREFIX_BONUS_PIGMENT_MIN
					+ lands.random.nextInt(HuntTuning.PREFIX_BONUS_PIGMENT_MAX - HuntTuning.PREFIX_BONUS_PIGMENT_MIN + 1)));
		}
		p.sendSystemMessage(Component.translatable("hunt.tears-of-ink.your_reward", contracts).withStyle(ChatFormatting.GOLD));
		if (a.contract() && p.getUUID().equals(a.owner) && a.rank >= 1) {
			com.tarkrakov.tearsofink.weapon.forge.AscensionFont.onContract(p, a.rank); // очки к Символу Доблести
			// временно, до Арены: Кровавый охотник в среднем/сложном контракте — шанс на Багряный Цветок
			if (a.type == HunterType.BLOOD && lands.random.nextFloat() < HuntTuning.TEMP_FLOWER_CHANCE)
				give(p, new ItemStack(ModItems.CRIMSON_FLOWER));
		}
		if (a.contract() && p.getUUID().equals(a.owner)) {
			com.tarkrakov.tearsofink.vow.Vows.awaken(p, "contract_" + a.rank, com.tarkrakov.tearsofink.vow.Vows.AWAKENING);
			com.tarkrakov.tearsofink.vow.Vows.heart(p, "contract_" + a.rank, com.tarkrakov.tearsofink.vow.Vows.SMALL_HEART, 0); // малое сердце (заглушка до боссов)
		}
		p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 0.8f);
	}

	private static void give(ServerPlayer p, ItemStack s) {
		if (!p.getInventory().add(s)) p.drop(s, false);
	}

	/** Исполненный контракт помнит оружие охотника той Охоты (Торговка может его отдать). */
	public static ItemStack contract(ItemStack weapon, int difficulty) {
		ItemStack c = new ItemStack(ModItems.FULFILLED_CONTRACT);
		CompoundTag tag = new CompoundTag();
		tag.putInt(C_DIFF, difficulty);
		tag.putString(C_KIND, BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString());
		if (weapon.getItem() instanceof HunterWeaponItem) { // у оружия Крови ступеней и линий нет — само по себе
			tag.putInt(C_LINE, HunterWeaponItem.line(weapon).ordinal());
			tag.putInt(C_TIER, HunterWeaponItem.tier(weapon));
		}
		c.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return c;
	}

	/** Шанс, что Торговка отдаст за этот контракт оружие охотника (по сложности: лёгкий, средний, сложный). */
	public static float weaponChance(ItemStack contract) {
		CustomData d = contract.get(DataComponents.CUSTOM_DATA);
		int diff = d == null ? 0 : d.copyTag().getInt(C_DIFF);
		return HuntTuning.CONTRACT_WEAPON_CHANCE[Math.max(0, Math.min(diff, HuntTuning.CONTRACT_WEAPON_CHANCE.length - 1))];
	}

	/** Оружие охотника из контракта (или пусто, если контракт без памяти). */
	public static ItemStack weaponOf(ItemStack contract) {
		CustomData d = contract.get(DataComponents.CUSTOM_DATA);
		if (d == null) return ItemStack.EMPTY;
		CompoundTag tag = d.copyTag();
		if (!tag.contains(C_KIND)) return ItemStack.EMPTY;
		var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString(C_KIND)));
		int diff = tag.getInt(C_DIFF);
		ItemStack out;
		if (item instanceof HunterWeaponItem w) out = w.make(HunterType.byIndex(tag.getInt(C_LINE)), 0);
		else if (item instanceof com.tarkrakov.tearsofink.weapon.InkGreatswordItem) out = new ItemStack(item); // настоящее оружие (Кровь, Шпиль)
		else return ItemStack.EMPTY;
		// 16 ноя: добыча — Тусклое; со сложного — с шансом Контрастное. Рубрики — только в Купели
		com.tarkrakov.tearsofink.weapon.forge.Gear.setTier(out, rewardTier(diff, java.util.concurrent.ThreadLocalRandom.current().nextFloat()));
		return out;
	}

	/** Ступень оружия-добычи: сложный (ранг 2) — Контрастное с шансом HuntTuning.HARD_CONTRAST_CHANCE, иначе Тусклое. */
	public static int rewardTier(int rank, float roll) {
		return rank >= 2 && roll < HuntTuning.HARD_CONTRAST_CHANCE ? 1 : 0;
	}

	/** Сообщение: мировая — всем в Землях; контракт — хозяину и тем, кто рядом. */
	private static void announce(ServerLevel lands, Active a, Component text) {
		for (ServerPlayer p : audience(lands, a)) p.sendSystemMessage(text);
	}
}
