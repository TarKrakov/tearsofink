package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Испытательные бои для баланса: охотник против цели на закрытых площадках высоко в небе Земель,
 * без игроков (настоящий ИИ, приёмы, увороты). /tearsofink hunt duel <боёв на случай>.
 * В лог: строки «[duel] …» и итоговая таблица. Для автотеста на GitHub (workflow duel).
 */
public final class HuntDuels {
	private static final int ARENAS = 10, SIZE = 11, Y = 250, TIMEOUT = 20 * 120;
	private static final int BASE_X = 200, BASE_Z = -500, STEP = 48;

	private record Case(HuntTargets.Target target, int rank, PreyPrefix prefix) {}

	private static final class Arena {
		final BlockPos center;
		Case c;
		HunterEntity hunter;
		final List<Mob> preys = new ArrayList<>();
		long start;
		String desc;

		Arena(BlockPos center) {
			this.center = center;
		}
	}

	private static final Deque<Case> QUEUE = new ArrayDeque<>();
	private static final List<Arena> ARENA_LIST = new ArrayList<>();
	/** ключ → {побед охотника, всего, ничьих, сумма секунд} */
	private static final Map<String, double[]> STATS = new LinkedHashMap<>();
	private static boolean running;

	private HuntDuels() {}

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (running && ModDimensions.isLands(level)) tick(level);
		});
	}

	public static int start(ServerLevel lands, int perCase) {
		if (running) return 0;
		QUEUE.clear();
		STATS.clear();
		ARENA_LIST.clear();
		for (HuntTargets.Target t : HuntTargets.all()) {
			for (int rank = 0; rank < 3; rank++) {
				for (PreyPrefix p : PreyPrefix.values()) {
					for (int i = 0; i < perCase; i++) QUEUE.add(new Case(t, rank, p));
				}
			}
		}
		for (int i = 0; i < ARENAS; i++) {
			BlockPos c = new BlockPos(BASE_X + i * STEP, Y, BASE_Z);
			build(lands, c);
			ARENA_LIST.add(new Arena(c));
		}
		running = true;
		log("начало: " + QUEUE.size() + " боёв на " + ARENAS + " площадках");
		return QUEUE.size();
	}

	private static void build(ServerLevel level, BlockPos c) {
		for (int cx = (c.getX() - SIZE - 2) >> 4; cx <= (c.getX() + SIZE + 2) >> 4; cx++) {
			for (int cz = (c.getZ() - SIZE - 2) >> 4; cz <= (c.getZ() + SIZE + 2) >> 4; cz++) level.setChunkForced(cx, cz, true);
		}
		for (int x = -SIZE - 1; x <= SIZE + 1; x++) {
			for (int z = -SIZE - 1; z <= SIZE + 1; z++) {
				boolean wall = Math.abs(x) == SIZE + 1 || Math.abs(z) == SIZE + 1;
				level.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
				for (int y = 0; y < 8; y++) {
					level.setBlock(c.offset(x, y, z), wall ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
				}
				level.setBlock(c.offset(x, 8, z), Blocks.BARRIER.defaultBlockState(), 2);
			}
		}
	}

	private static void tick(ServerLevel level) {
		long now = level.getGameTime();
		boolean busy = false;
		for (Arena a : ARENA_LIST) {
			if (a.c != null) {
				busy = true;
				check(level, a, now);
			} else if (!QUEUE.isEmpty()) {
				busy = true;
				spawn(level, a, QUEUE.poll(), now);
			}
		}
		if (!busy) finish(level);
	}

	private static void spawn(ServerLevel level, Arena a, Case c, long now) {
		RandomSource random = level.random;
		a.c = c;
		a.start = now;
		a.preys.clear();
		BlockPos at = a.center.offset(-5, 0, 0);
		int count = c.prefix() == PreyPrefix.SPLIT ? 2 : 1;
		for (int i = 0; i < count; i++) {
			Mob m = Hunt.makePrey(level, c.target(), at.offset(0, 0, i * 3), c.rank(), c.prefix(), random);
			if (m == null) continue;
			m.removeTag(Hunt.PREY_TAG); // не Охота: защита «чужих целей» не трогает
			m.addTag("tearsofink.duel");
			level.addFreshEntity(m);
			a.preys.add(m);
		}
		HunterType type = HunterType.byIndex(random.nextInt(HunterType.values().length));
		ItemStack[] arms = Hunt.loadout(type, c.rank(), random);
		HunterEntity h = ModMobs.HUNTER.create(level);
		if (h == null || a.preys.isEmpty()) {
			a.c = null;
			return;
		}
		h.moveTo(a.center.getX() + 5.5, a.center.getY(), a.center.getZ() + 0.5, 90, 0);
		h.setup(type, c.rank(), arms[0], arms[1]);
		h.setFoe(a.preys.get(0), true);
		level.addFreshEntity(h);
		a.hunter = h;
		a.desc = type.id + " " + id(arms[0]) + (arms[1].isEmpty() ? "" : "+" + id(arms[1]));
	}

	private static String id(ItemStack s) {
		return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().replace("hunter_", "");
	}

	private static void check(ServerLevel level, Arena a, long now) {
		a.preys.removeIf(m -> !m.isAlive());
		boolean hunterAlive = a.hunter != null && a.hunter.isAlive();
		if (hunterAlive && !a.preys.isEmpty()) {
			LivingEntity closest = a.preys.get(0);
			for (Mob m : a.preys) if (a.hunter.distanceTo(m) < a.hunter.distanceTo(closest)) closest = m;
			a.hunter.setFoe(closest, true);
			if (now - a.start < TIMEOUT) return;
		}
		String result = !hunterAlive ? "prey" : a.preys.isEmpty() ? "hunter" : "draw";
		double secs = (now - a.start) / 20.0;
		Case c = a.c;
		log(String.format(java.util.Locale.ROOT, "%s r%d %s | %s | %s %.1fс | hp охотника %.0f",
				BuiltInRegistries.ENTITY_TYPE.getKey(c.target().type()).getPath(), c.rank(), c.prefix().id, a.desc, result, secs,
				hunterAlive ? a.hunter.getHealth() : 0));
		String key = BuiltInRegistries.ENTITY_TYPE.getKey(c.target().type()).getPath() + "|" + c.rank() + "|" + c.prefix().id;
		double[] st = STATS.computeIfAbsent(key, k -> new double[4]);
		if (result.equals("hunter")) st[0]++;
		if (result.equals("draw")) st[2]++;
		st[1]++;
		st[3] += secs;
		if (a.hunter != null) a.hunter.discard();
		for (Mob m : a.preys) {
			Hunt.forgetPrey(m.getUUID());
			m.discard();
		}
		a.preys.clear();
		a.c = null;
	}

	private static void finish(ServerLevel level) {
		running = false;
		log("ИТОГ — % побед охотника (ничьи) [средняя длина боя]");
		for (var e : STATS.entrySet()) {
			double[] s = e.getValue();
			log(String.format(java.util.Locale.ROOT, "%-34s %3.0f%% (%2.0f) [%4.1fс] из %d", e.getKey(),
					100 * s[0] / s[1], s[2], s[3] / s[1], (int) s[1]));
		}
		for (Arena a : ARENA_LIST) {
			BlockPos c = a.center;
			for (int cx = (c.getX() - SIZE - 2) >> 4; cx <= (c.getX() + SIZE + 2) >> 4; cx++) {
				for (int cz = (c.getZ() - SIZE - 2) >> 4; cz <= (c.getZ() + SIZE + 2) >> 4; cz++) level.setChunkForced(cx, cz, false);
			}
		}
		log("DONE");
	}

	private static void log(String msg) {
		TearsOfInk.LOGGER.info("[duel] " + msg);
	}
}
