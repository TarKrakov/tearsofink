package com.tarkrakov.tearsofink.entity.patrol;

import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Заставы Шпиля на дорогах (16 ноя): у баррикады стоит отряд, который не уходит. Перебили — через RESPAWN тиков
 * застава снова занята. Ушли далеко — отряд растворяется и встанет заново, когда вернётесь. Где стоят —
 * InkLandsLayout.OUTPOSTS (доли пути от Чернолесья к краю, на каждой дороге). Перезапуск сервера сбрасывает перезарядку.
 */
public final class Outposts {
	// ===== Настройки =====
	/** Через сколько занятая заново (тики): 20 мин. */
	public static final int RESPAWN = 20 * 60 * 20;
	/** Отряд встаёт, когда игрок ближе этого; растворяется, когда все дальше FORGET. */
	public static final int WAKE = 72, FORGET = 140;
	/** Состав по поясам глубины: T — щитоносец, S — копейщик, C — арбалетчик. */
	public static final String[] SQUADS = {"SSC", "TSCC", "TTSSCC"};

	private record Key(int road, int number) {}

	private static final class Post {
		final List<UUID> members = new ArrayList<>();
		long cooldownUntil;
		boolean bloodied;
	}

	private static final Map<Key, Post> POSTS = new HashMap<>();

	private Outposts() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Outposts::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof PaleSoldier) || !(source.getEntity() instanceof Player)) return;
			for (Post p : POSTS.values()) if (p.members.contains(entity.getUUID())) p.bloodied = true;
		});
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 40 != 0) return;
		ServerLevel level = ModDimensions.lands(server);
		if (level == null) return;
		if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
		InkLandsLayout layout = InkLandsLayout.of(level);
		if (layout == null) return;
		long now = level.getGameTime();
		for (int road = 0; road < InkLandsLayout.ROADS; road++) {
			for (int n = 0; n < InkLandsLayout.OUTPOSTS.length; n++) {
				Key key = new Key(road, n);
				Post post = POSTS.computeIfAbsent(key, k -> new Post());
				double d = layout.outpostDistance(n);
				double[] at = layout.roadPoint(road, d);
				double nearest = Double.MAX_VALUE;
				for (ServerPlayer p : level.players()) nearest = Math.min(nearest, Math.hypot(p.getX() - at[0], p.getZ() - at[1]));
				post.members.removeIf(id -> {
					Entity e = level.getEntity(id);
					return e == null || !e.isAlive();
				});
				if (!post.members.isEmpty()) {
					if (nearest > FORGET) { // все ушли — раствориться, без перезарядки
						for (UUID id : post.members) {
							Entity e = level.getEntity(id);
							if (e != null) e.discard();
						}
						post.members.clear();
						post.bloodied = false;
					}
					continue;
				}
				if (post.bloodied) { // перебили — ждать
					post.bloodied = false;
					post.cooldownUntil = now + RESPAWN;
				}
				if (now < post.cooldownUntil || nearest > WAKE) continue;
				spawn(level, layout, road, d, post);
			}
		}
	}

	private static void spawn(ServerLevel level, InkLandsLayout layout, int road, double d, Post post) {
		int tier = SquadPatrols.tier(layout, layout.roadPoint(road, d)[0], layout.roadPoint(road, d)[1]);
		String squad = SQUADS[Math.min(tier, SQUADS.length - 1)];
		UUID squadId = UUID.randomUUID(), leader = null;
		for (int i = 0; i < squad.length(); i++) {
			// стоят за баррикадой (со стороны Чернолесья), в два ряда
			double t = d - 2.5 - (i / 2) * 2.0;
			double[] p = layout.roadPoint(road, t);
			double[] dir = layout.roadDirection(road, t);
			double side = (i % 2 == 0 ? 1 : -1) * 1.2;
			int x = (int) Math.floor(p[0] - dir[1] * side), z = (int) Math.floor(p[1] + dir[0] * side);
			BlockPos at = new BlockPos(x, 0, z);
			if (!level.hasChunkAt(at)) return;
			at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			EntityType<? extends PaleSoldier> type = switch (squad.charAt(i)) {
				case 'T' -> ModMobs.PALE_SHIELDBEARER;
				case 'C' -> ModMobs.PALE_CROSSBOWMAN;
				default -> ModMobs.PALE_SPEARMAN;
			};
			PaleSoldier mob = type.create(level);
			if (mob == null) return;
			mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
			if (leader == null) leader = mob.getUUID();
			mob.joinSquad(squadId, leader);
			mob.setPower(SquadTuning.TIER_POWER[tier]);
			mob.setPatrol(road, d - 6, d - 1); // стоит у заставы
			if (level.addFreshEntity(mob)) post.members.add(mob.getUUID());
		}
	}
}
