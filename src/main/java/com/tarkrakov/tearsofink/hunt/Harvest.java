package com.tarkrakov.tearsofink.hunt;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Алая Жатва — рейд на Арене Храбрецов (заглушка, 16 ноя). Арена — круг у низины Арены (InkLandsLayout.arenaCenter),
 * постройку автора подставим потом. Хранитель Жатвы у «двери» (сторона к Вестнику) — ПКМ: меню в чате.
 * Ход: запуск (сложность + мест 5/7/10) → 10 мин сбора (чат всем: «Принять участие» — телепорт) → 30 с подготовки,
 * Арена закрыта (невидимая граница), пустые места — охотники-союзники → 1/2/3 мини-босса подряд (здоровье от мест) →
 * победа: Дар каждому, кто бил босса (порог мягкий), лучшему по урону — ещё малый Дар; Дары забирают у Хранителя.
 * Павший участник не умирает — остаётся с вещами «за стеной». Перезарядка 30 мин на весь сервер (и при провале).
 */
public final class Harvest {
	// ===== Настройки — крутить здесь (код → перезапуск) =====
	public static final int COOLDOWN = 20 * 60 * 30, GATHER = 20 * 60 * 10, PREP = 20 * 30, BETWEEN = 20 * 5;
	public static final int[] SIZES = {5, 7, 10};
	/** Мини-боссов по сложности. */
	public static final int[] BOSSES = {1, 2, 3};
	/** Здоровье босса: × (BOSS_HP_BASE + BOSS_HP_PER_SLOT × мест). */
	public static final float BOSS_HP_BASE = 0.6f, BOSS_HP_PER_SLOT = 0.2f;
	/** Дар — тем, кто нанёс боссам хотя бы столько урона (доля от всего урона игроков) или хотя бы столько попаданий. */
	public static final float GIFT_SHARE = 0.02f;
	public static final int GIFT_HITS = 3;
	/** Геометрия заглушки: центр круга — на столько блоков от центра низины к центру мира; радиус круга. */
	public static final int ARENA_OFFSET = 55, ARENA_RADIUS = 22;

	private enum Phase { IDLE, GATHER, PREP, FIGHT }

	private static Phase phase = Phase.IDLE;
	private static long phaseEnd, cooldownUntil;
	private static int difficulty, size, bossesLeft;
	private static UUID starter;
	private static final Set<UUID> PARTICIPANTS = new LinkedHashSet<>();
	private static final Set<UUID> FALLEN = new LinkedHashSet<>();
	private static final List<UUID> BOTS = new ArrayList<>();
	private static UUID boss;
	private static final Map<UUID, float[]> DAMAGE = new HashMap<>(); // {урон, попаданий}
	private static final Map<UUID, Float> BOT_DAMAGE = new HashMap<>();
	private static UUID keeper;

	/** Дары к получению у Хранителя: сложность (0–2), +10 — малый Дар лучшему. Хранятся за игроком. */
	public static final AttachmentType<List<Integer>> GIFTS = AttachmentRegistry.<List<Integer>>builder()
			.persistent(Codec.INT.listOf()).copyOnDeath().initializer(List::of).buildAndRegister(TearsOfInk.id("harvest_gifts"));

	private Harvest() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Harvest::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> reset());
		// участник Жатвы не умирает: остаётся с вещами «за стеной»
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer p) || phase == Phase.IDLE || phase == Phase.GATHER || !PARTICIPANTS.contains(p.getUUID())) return true;
			fall(p);
			return false;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (boss != null && entity.getUUID().equals(boss) && source.getEntity() instanceof ServerPlayer p) {
				float[] d = DAMAGE.computeIfAbsent(p.getUUID(), k -> new float[2]);
				d[0] += Math.min(amount, entity.getHealth());
				d[1] += 1;
			} else if (boss != null && entity.getUUID().equals(boss) && source.getEntity() instanceof HunterEntity h && BOTS.contains(h.getUUID())) {
				BOT_DAMAGE.merge(h.getUUID(), Math.min(amount, entity.getHealth()), Float::sum);
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (boss != null && entity.getUUID().equals(boss)) bossDown(entity);
		});
		CommandRegistrationCallback.EVENT.register((d, reg, env) -> d.register(Commands.literal("harvest")
				.then(Commands.literal("start").then(Commands.argument("difficulty", IntegerArgumentType.integer(0, 2))
						.then(Commands.argument("size", IntegerArgumentType.integer(5, 10)).executes(c -> {
							start(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "difficulty"), IntegerArgumentType.getInteger(c, "size"));
							return 1;
						}))))
				.then(Commands.literal("join").executes(c -> {
					join(c.getSource().getPlayerOrException());
					return 1;
				}))
				.then(Commands.literal("begin").executes(c -> {
					begin(c.getSource().getPlayerOrException());
					return 1;
				}))
				.then(Commands.literal("claim").executes(c -> {
					claim(c.getSource().getPlayerOrException());
					return 1;
				}))));
	}

	private static void reset() {
		phase = Phase.IDLE;
		PARTICIPANTS.clear();
		FALLEN.clear();
		BOTS.clear();
		DAMAGE.clear();
		BOT_DAMAGE.clear();
		boss = null;
		keeper = null;
		starter = null;
	}

	// ===== Геометрия заглушки =====
	@org.jetbrains.annotations.Nullable
	static Vec3 arena(ServerLevel lands) {
		InkLandsLayout layout = InkLandsLayout.of(lands);
		if (layout == null) return null;
		BlockPos v = layout.arenaCenter();
		double len = Math.sqrt((double) v.getX() * v.getX() + (double) v.getZ() * v.getZ());
		double x = v.getX() - v.getX() / len * ARENA_OFFSET, z = v.getZ() - v.getZ() / len * ARENA_OFFSET;
		BlockPos g = new BlockPos((int) x, 0, (int) z);
		int y = lands.hasChunkAt(g) ? lands.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, g.getX(), g.getZ()) : 80;
		return new Vec3(x + 0.5, y, z + 0.5);
	}

	/** Сторона «двери» (к Вестнику) — единичный вектор. */
	private static Vec3 doorDir(ServerLevel lands, Vec3 a) {
		BlockPos v = InkLandsLayout.of(lands).arenaCenter();
		Vec3 d = new Vec3(v.getX() + 0.5 - a.x, 0, v.getZ() + 0.5 - a.z);
		return d.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : d.normalize();
	}

	private static Vec3 ground(ServerLevel lands, double x, double z) {
		int ix = Mth.floor(x), iz = Mth.floor(z);
		return new Vec3(x, lands.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz), z);
	}

	private static boolean inside(Vec3 a, Entity e, double r) {
		double dx = e.getX() - a.x, dz = e.getZ() - a.z;
		return dx * dx + dz * dz < r * r && Math.abs(e.getY() - a.y) < 16;
	}

	// ===== Меню у Хранителя (чат с кнопками) =====
	public static void menu(ServerPlayer p) {
		say(p, Component.translatable("harvest.tears-of-ink.keeper_hello"));
		long now = p.server.getTickCount();
		switch (phase) {
			case IDLE -> {
				if (now < cooldownUntil) say(p, Component.translatable("harvest.tears-of-ink.cooldown", (cooldownUntil - now) / 1200 + 1));
				else for (int diff = 0; diff < 3; diff++) {
					MutableComponent row = Component.translatable("harvest.tears-of-ink.diff." + diff).append(": ");
					for (int s : SIZES) row.append(button(Component.translatable("harvest.tears-of-ink.size", s), "/harvest start " + diff + " " + s)).append(" ");
					p.sendSystemMessage(row);
				}
			}
			case GATHER -> {
				p.sendSystemMessage(Component.translatable("harvest.tears-of-ink.gathering", PARTICIPANTS.size(), size, (phaseEnd - now) / 20)
						.withStyle(ChatFormatting.GRAY));
				if (!PARTICIPANTS.contains(p.getUUID())) p.sendSystemMessage(button(Component.translatable("harvest.tears-of-ink.join"), "/harvest join"));
				if (p.getUUID().equals(starter)) p.sendSystemMessage(button(Component.translatable("harvest.tears-of-ink.begin"), "/harvest begin"));
			}
			default -> say(p, Component.translatable("harvest.tears-of-ink.busy"));
		}
		int gifts = p.getAttachedOrCreate(GIFTS).size();
		if (gifts > 0) p.sendSystemMessage(button(Component.translatable("harvest.tears-of-ink.claim", gifts), "/harvest claim"));
	}

	private static MutableComponent button(Component text, String command) {
		return Component.literal("[").append(text).append("]").withStyle(s -> s.withColor(ChatFormatting.RED).withUnderlined(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text)));
	}

	private static void say(ServerPlayer p, Component c) {
		p.sendSystemMessage(c.copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
	}

	private static boolean nearKeeper(ServerPlayer p) {
		if (keeper == null || !(p.serverLevel().getEntity(keeper) instanceof HarvestKeeperEntity k)) return false;
		return p.distanceTo(k) < 8;
	}

	// ===== Действия =====
	static void start(ServerPlayer p, int diff, int slots) {
		if (phase != Phase.IDLE || p.server.getTickCount() < cooldownUntil || !nearKeeper(p)) return;
		ServerLevel lands = p.serverLevel();
		phase = Phase.GATHER;
		difficulty = diff;
		size = Mth.clamp(slots, 5, 10);
		starter = p.getUUID();
		PARTICIPANTS.clear();
		FALLEN.clear();
		DAMAGE.clear();
		PARTICIPANTS.add(p.getUUID());
		phaseEnd = p.server.getTickCount() + GATHER;
		Component call = Component.translatable("harvest.tears-of-ink.announce", Component.translatable("harvest.tears-of-ink.diff." + diff), GATHER / 1200)
				.withStyle(ChatFormatting.RED);
		for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
			o.sendSystemMessage(call);
			if (o != p) o.sendSystemMessage(button(Component.translatable("harvest.tears-of-ink.join_tp"), "/harvest join"));
			o.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.6f, 0.6f);
		}
		menu(p);
	}

	static void join(ServerPlayer p) {
		if (phase != Phase.GATHER || PARTICIPANTS.contains(p.getUUID())) return;
		if (PARTICIPANTS.size() >= size) {
			say(p, Component.translatable("harvest.tears-of-ink.full"));
			return;
		}
		ServerLevel lands = ModDimensions.lands(p.server);
		if (lands == null || p.level() != lands) { // телепорт — только из Земель
			say(p, Component.translatable("harvest.tears-of-ink.not_lands"));
			return;
		}
		Vec3 a = arena(lands);
		if (a == null) return;
		if (!inside(a, p, ARENA_RADIUS + 30)) {
			Vec3 d = doorDir(lands, a);
			Vec3 at = ground(lands, a.x - d.x * (ARENA_RADIUS - 4), a.z - d.z * (ARENA_RADIUS - 4));
			p.teleportTo(lands, at.x, at.y, at.z, p.getYRot(), p.getXRot());
		}
		PARTICIPANTS.add(p.getUUID());
		for (UUID id : PARTICIPANTS) {
			ServerPlayer o = p.server.getPlayerList().getPlayer(id);
			if (o != null) o.sendSystemMessage(Component.translatable("harvest.tears-of-ink.joined", p.getDisplayName(), PARTICIPANTS.size(), size)
					.withStyle(ChatFormatting.GRAY));
		}
	}

	static void begin(ServerPlayer p) {
		if (phase == Phase.GATHER && p.getUUID().equals(starter) && nearKeeper(p)) phaseEnd = p.server.getTickCount();
	}

	static void claim(ServerPlayer p) {
		if (!nearKeeper(p)) return;
		List<Integer> gifts = p.getAttachedOrCreate(GIFTS);
		if (gifts.isEmpty()) return;
		for (int g : gifts) {
			ItemStack bag = new ItemStack(ModItems.CRIMSON_GIFT);
			CrimsonGiftItem.setKind(bag, g);
			if (!p.getInventory().add(bag)) p.drop(bag, false);
		}
		p.setAttached(GIFTS, List.of());
		say(p, Component.translatable("harvest.tears-of-ink.claimed"));
		p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 0.6f);
	}

	// ===== Ход =====
	private static void tick(MinecraftServer server) {
		ServerLevel lands = ModDimensions.lands(server);
		if (lands == null) return;
		long now = server.getTickCount();
		Vec3 a = null;
		if (now % 20 == 0 || phase != Phase.IDLE) a = arena(lands);
		if (a == null) return;
		if (now % 20 == 0) {
			keeperTick(lands, a);
			ring(lands, a, phase == Phase.PREP || phase == Phase.FIGHT);
		}
		switch (phase) {
			case GATHER -> {
				if (now >= phaseEnd) toPrep(lands, a, now);
			}
			case PREP -> {
				wall(lands, a);
				if (now >= phaseEnd) {
					phase = Phase.FIGHT;
					bossesLeft = BOSSES[difficulty];
					spawnBoss(lands, a);
				}
			}
			case FIGHT -> {
				wall(lands, a);
				if (now % 10 == 0) fightTick(lands, a, now);
			}
			default -> {}
		}
	}

	private static void keeperTick(ServerLevel lands, Vec3 a) {
		boolean want = phase == Phase.IDLE || phase == Phase.GATHER;
		Entity k = keeper == null ? null : lands.getEntity(keeper);
		if (!want) {
			if (k != null) k.discard();
			keeper = null;
			return;
		}
		if (k != null && k.isAlive()) return;
		boolean near = false;
		for (ServerPlayer p : lands.players()) if (inside(a, p, 96)) near = true;
		if (!near) return;
		Vec3 d = doorDir(lands, a);
		Vec3 at = ground(lands, a.x + d.x * (ARENA_RADIUS - 3), a.z + d.z * (ARENA_RADIUS - 3));
		HarvestKeeperEntity e = ModMobs.HARVEST_KEEPER.create(lands);
		if (e == null) return;
		e.moveTo(at.x, at.y, at.z, (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG) + 180, 0);
		if (lands.addFreshEntity(e)) keeper = e.getUUID();
	}

	/** Граница Арены — кольцо искр (видно всегда, ярче во время Жатвы). */
	private static void ring(ServerLevel lands, Vec3 a, boolean closed) {
		var dust = new DustParticleOptions(closed ? new Vector3f(0.9f, 0.1f, 0.15f) : new Vector3f(0.5f, 0.15f, 0.2f), closed ? 1.6f : 1.0f);
		int n = closed ? 64 : 24;
		for (int i = 0; i < n; i++) {
			double ang = Math.PI * 2 * i / n;
			double x = a.x + Math.cos(ang) * ARENA_RADIUS, z = a.z + Math.sin(ang) * ARENA_RADIUS;
			lands.sendParticles(dust, x, a.y + 0.3 + (closed ? (i % 3) * 0.7 : 0), z, 1, 0, 0, 0, 0);
		}
	}

	private static void toPrep(ServerLevel lands, Vec3 a, long now) {
		List<ServerPlayer> here = new ArrayList<>();
		for (UUID id : PARTICIPANTS) {
			ServerPlayer p = lands.getServer().getPlayerList().getPlayer(id);
			if (p != null && p.level() == lands && inside(a, p, ARENA_RADIUS + 40)) here.add(p);
		}
		if (here.isEmpty()) { // никто не пришёл — пропуск, без перезарядки
			for (ServerPlayer o : lands.players()) o.sendSystemMessage(Component.translatable("harvest.tears-of-ink.skipped").withStyle(ChatFormatting.GRAY));
			reset();
			return;
		}
		PARTICIPANTS.clear();
		for (ServerPlayer p : here) {
			PARTICIPANTS.add(p.getUUID());
			if (!inside(a, p, ARENA_RADIUS - 1)) {
				Vec3 d = doorDir(lands, a);
				Vec3 at = ground(lands, a.x - d.x * (ARENA_RADIUS - 4), a.z - d.z * (ARENA_RADIUS - 4));
				p.teleportTo(lands, at.x, at.y, at.z, p.getYRot(), p.getXRot());
			}
		}
		// пустые места — охотники-союзники (ранг по сложности)
		RandomSource r = lands.random;
		BOTS.clear();
		for (int i = PARTICIPANTS.size(); i < size; i++) {
			HunterEntity h = ModMobs.HUNTER.create(lands);
			if (h == null) continue;
			HunterType type = HunterType.values()[r.nextInt(HunterType.values().length)];
			ItemStack[] arms = Hunt.loadout(type, difficulty, r);
			double ang = r.nextDouble() * Math.PI * 2;
			Vec3 at = ground(lands, a.x + Math.cos(ang) * (ARENA_RADIUS - 6), a.z + Math.sin(ang) * (ARENA_RADIUS - 6));
			h.moveTo(at.x, at.y, at.z, 0, 0);
			h.setup(type, difficulty, arms[0], arms[1]);
			if (lands.addFreshEntity(h)) BOTS.add(h.getUUID());
		}
		phase = Phase.PREP;
		phaseEnd = now + PREP;
		cooldownUntil = now + COOLDOWN;
		for (UUID id : PARTICIPANTS) {
			ServerPlayer p = lands.getServer().getPlayerList().getPlayer(id);
			if (p != null) {
				p.sendSystemMessage(Component.translatable("harvest.tears-of-ink.closed", PREP / 20).withStyle(ChatFormatting.RED));
				p.playNotifySound(SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1f, 0.5f);
			}
		}
	}

	/** Невидимая граница: участники внутри, чужие снаружи. */
	private static void wall(ServerLevel lands, Vec3 a) {
		for (ServerPlayer p : lands.players()) {
			boolean member = PARTICIPANTS.contains(p.getUUID()) && !FALLEN.contains(p.getUUID());
			boolean in = inside(a, p, ARENA_RADIUS);
			if (member && !in) push(p, a, ARENA_RADIUS - 2);
			else if (!member && inside(a, p, ARENA_RADIUS + 1) && !p.isSpectator()) push(p, a, ARENA_RADIUS + 4);
		}
	}

	private static void push(ServerPlayer p, Vec3 a, double r) {
		Vec3 d = new Vec3(p.getX() - a.x, 0, p.getZ() - a.z);
		d = d.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : d.normalize();
		ServerLevel l = p.serverLevel();
		Vec3 at = ground(l, a.x + d.x * r, a.z + d.z * r);
		p.teleportTo(l, at.x, at.y, at.z, p.getYRot(), p.getXRot());
		p.setDeltaMovement(Vec3.ZERO);
		p.hurtMarked = true;
	}

	private static void fall(ServerPlayer p) {
		FALLEN.add(p.getUUID());
		p.setHealth(p.getMaxHealth() * 0.5f);
		p.removeAllEffects();
		ServerLevel l = p.serverLevel();
		Vec3 a = arena(l);
		if (a != null) push(p, a, ARENA_RADIUS + 5);
		say(p, Component.translatable("harvest.tears-of-ink.fallen"));
	}

	private static void spawnBoss(ServerLevel lands, Vec3 a) {
		RandomSource r = lands.random;
		Vec3 d = doorDir(lands, a);
		Vec3 at = ground(lands, a.x + d.x * (ARENA_RADIUS - 5), a.z + d.z * (ARENA_RADIUS - 5));
		PreyPrefix prefix = r.nextBoolean() ? PreyPrefix.NONE : (r.nextBoolean() ? PreyPrefix.SWIFT : PreyPrefix.GIANT);
		Mob m = Hunt.makePrey(lands, HuntTargets.pick(r), BlockPos.containing(at), difficulty, prefix, r);
		if (m == null) return;
		m.removeTag(Hunt.PREY_TAG); // не цель Охоты — пусть не растворяется
		var hp = m.getAttribute(Attributes.MAX_HEALTH);
		hp.setBaseValue(hp.getBaseValue() * (BOSS_HP_BASE + BOSS_HP_PER_SLOT * size));
		m.setHealth(m.getMaxHealth());
		m.setCustomName(Component.translatable("harvest.tears-of-ink.boss", m.getType().getDescription()));
		m.setCustomNameVisible(true);
		if (!lands.addFreshEntity(m)) return;
		boss = m.getUUID();
		for (UUID id : BOTS) if (lands.getEntity(id) instanceof HunterEntity h) h.setFoe(m, true);
		lands.playSound(null, BlockPos.containing(at), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.2f, 0.9f);
		for (UUID id : PARTICIPANTS) {
			ServerPlayer p = lands.getServer().getPlayerList().getPlayer(id);
			if (p != null) p.sendSystemMessage(Component.translatable("harvest.tears-of-ink.door", m.getDisplayName()).withStyle(ChatFormatting.RED));
		}
	}

	private static long nextBossAt = -1;

	private static void fightTick(ServerLevel lands, Vec3 a, long now) {
		// все павшие или ушли — провал
		boolean anyone = false;
		for (UUID id : PARTICIPANTS) {
			ServerPlayer p = lands.getServer().getPlayerList().getPlayer(id);
			if (p != null && !FALLEN.contains(id) && p.level() == lands) anyone = true;
		}
		if (!anyone) {
			finish(lands, false);
			return;
		}
		if (nextBossAt >= 0 && now >= nextBossAt) {
			nextBossAt = -1;
			spawnBoss(lands, a);
		}
		Entity b = boss == null ? null : lands.getEntity(boss);
		if (b instanceof LivingEntity lb && !inside(a, lb, ARENA_RADIUS + 3)) { // босс не уходит с Арены
			Vec3 at = ground(lands, a.x, a.z);
			lb.teleportTo(at.x, at.y, at.z);
		}
		if (b instanceof Mob mob && mob.getTarget() == null) { // босс сразу ищет бойцов
			for (UUID id : PARTICIPANTS) {
				ServerPlayer p = lands.getServer().getPlayerList().getPlayer(id);
				if (p != null && !FALLEN.contains(id)) {
					mob.setTarget(p);
					break;
				}
			}
		}
	}

	private static void bossDown(LivingEntity e) {
		ServerLevel lands = (ServerLevel) e.level();
		Hunt.forgetPrey(e.getUUID());
		boss = null;
		bossesLeft--;
		if (bossesLeft > 0) {
			nextBossAt = lands.getServer().getTickCount() + BETWEEN;
			for (UUID id : PARTICIPANTS) {
				ServerPlayer p = lands.getServer().getPlayerList().getPlayer(id);
				if (p != null) p.sendSystemMessage(Component.translatable("harvest.tears-of-ink.next", bossesLeft).withStyle(ChatFormatting.GOLD));
			}
		} else {
			finish(lands, true);
		}
	}

	private static void finish(ServerLevel lands, boolean won) {
		for (UUID id : BOTS) {
			Entity e = lands.getEntity(id);
			if (e != null) e.discard();
		}
		if (boss != null) {
			Entity e = lands.getEntity(boss);
			if (e != null) e.discard();
			Hunt.forgetPrey(boss);
		}
		if (won) {
			float total = 0;
			for (float[] d : DAMAGE.values()) total += d[0];
			UUID best = null;
			float bestDmg = 0;
			for (var en : DAMAGE.entrySet()) if (en.getValue()[0] > bestDmg) {
				bestDmg = en.getValue()[0];
				best = en.getKey();
			}
			// игрок один — малый Дар, только если он перебил по урону каждого из охотников-союзников
			if (PARTICIPANTS.size() == 1) for (float b : BOT_DAMAGE.values()) if (b >= bestDmg) best = null;
			for (UUID id : PARTICIPANTS) {
				float[] d = DAMAGE.get(id);
				boolean earned = d != null && (d[0] >= total * GIFT_SHARE || d[1] >= GIFT_HITS);
				ServerPlayer p = lands.getServer().getPlayerList().getPlayer(id);
				if (!earned) {
					if (p != null) p.sendSystemMessage(Component.translatable("harvest.tears-of-ink.no_gift").withStyle(ChatFormatting.GRAY));
					continue;
				}
				List<Integer> g = new ArrayList<>(p != null ? p.getAttachedOrCreate(GIFTS) : List.of());
				g.add(difficulty);
				if (id.equals(best)) g.add(10 + difficulty);
				if (p != null) {
					p.setAttached(GIFTS, g);
					p.sendSystemMessage(Component.translatable(id.equals(best) ? "harvest.tears-of-ink.won_best" : "harvest.tears-of-ink.won")
							.withStyle(ChatFormatting.GOLD));
					p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1f);
				}
			}
			for (ServerPlayer o : lands.getServer().getPlayerList().getPlayers())
				o.sendSystemMessage(Component.translatable("harvest.tears-of-ink.over_won").withStyle(ChatFormatting.RED));
		} else {
			for (ServerPlayer o : lands.getServer().getPlayerList().getPlayers())
				o.sendSystemMessage(Component.translatable("harvest.tears-of-ink.over_lost").withStyle(ChatFormatting.DARK_RED));
		}
		long cd = cooldownUntil;
		reset();
		cooldownUntil = cd;
		nextBossAt = -1;
	}
}
