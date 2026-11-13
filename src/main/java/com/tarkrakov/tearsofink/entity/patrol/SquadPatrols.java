package com.tarkrakov.tearsofink.entity.patrol;

import com.tarkrakov.tearsofink.block.lantern.Lanterns;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.entity.spearman.SpearmanTuning;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.UUID;

/**
 * Отряды бледного патруля у дорог Бледнолесья: раз в 10 с возле каждого игрока в Землях, если солдат рядом мало,
 * ставит отряд на ближайшую дорогу впереди/позади игрока (в тумане), не в свете горящих фонарей.
 * Состав — случайный из SquadTuning.SQUADS; первый в составе ведёт, остальные идут колонной.
 */
public final class SquadPatrols {
	private SquadPatrols() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(SquadPatrols::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % SpearmanTuning.PATROL_SPAWN_INTERVAL != 0) return;
		ServerLevel level = com.tarkrakov.tearsofink.world.ModDimensions.lands(server);
		if (level == null || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
		InkLandsLayout layout = InkLandsLayout.of(level);
		if (layout == null) return;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			if (layout.depthInside(player.getBlockX(), player.getBlockZ()) < 0) continue;
			trySpawnNear(level, layout, player);
		}
	}

	/** Передышка: игрок → игровое время, до которого рядом с ним патрули не появляются. */
	private static final java.util.Map<UUID, Long> RESPITE = new java.util.concurrent.ConcurrentHashMap<>();

	public static void grantRespite(ServerPlayer p) {
		RESPITE.put(p.getUUID(), p.serverLevel().getGameTime() + SquadTuning.RESPITE_TICKS);
	}

	/** Пояс глубины: 0 окраина, 1 лес, 2 глубь. */
	public static int tier(InkLandsLayout layout, double x, double z) {
		double depth = layout.depthInside((int) Math.floor(x), (int) Math.floor(z));
		return depth < SquadTuning.TIER1_DEPTH ? 0 : depth < SquadTuning.TIER2_DEPTH ? 1 : 2;
	}

	private static String pickSquad(RandomSource random, int tier) {
		String[] squads = SquadTuning.TIER_SQUADS[tier];
		int[] weights = SquadTuning.TIER_WEIGHTS[tier];
		int total = 0;
		for (int w : weights) total += w;
		int r = random.nextInt(total);
		for (int i = 0; i < squads.length; i++) {
			r -= weights[i];
			if (r < 0) return squads[i];
		}
		return squads[0];
	}

	private static EntityType<? extends PaleSoldier> typeOf(char c) {
		return switch (c) {
			case 'T' -> ModMobs.PALE_SHIELDBEARER;
			case 'C' -> ModMobs.PALE_CROSSBOWMAN;
			default -> ModMobs.PALE_SPEARMAN;
		};
	}

	private static void trySpawnNear(ServerLevel level, InkLandsLayout layout, ServerPlayer player) {
		Long until = RESPITE.get(player.getUUID());
		if (until != null) {
			if (level.getGameTime() < until) return; // передышка после зачистки
			RESPITE.remove(player.getUUID());
		}
		int tier = tier(layout, player.getX(), player.getZ());
		if (level.random.nextFloat() > SquadTuning.TIER_SPAWN_CHANCE[tier]) return;
		int near = level.getEntitiesOfClass(PaleSoldier.class, player.getBoundingBox().inflate(SpearmanTuning.PATROL_COUNT_RADIUS),
				PaleSoldier::isAlive).size();
		if (near >= SquadTuning.TIER_MAX_NEAR[tier]) return;

		BlockPos c = layout.center();
		double px = player.getX(), pz = player.getZ();
		double d = Math.sqrt((px - c.getX()) * (px - c.getX()) + (pz - c.getZ()) * (pz - c.getZ()));
		int bestRoad = -1;
		double bestDist = SpearmanTuning.PATROL_ROAD_REACH;
		for (int road = 0; road < InkLandsLayout.ROADS; road++) {
			double[] p = layout.roadPoint(road, d);
			double dist = Math.hypot(p[0] - px, p[1] - pz);
			if (dist < bestDist) {
				bestDist = dist;
				bestRoad = road;
			}
		}
		if (bestRoad < 0) return;

		RandomSource random = level.random;
		for (int attempt = 0; attempt < 8; attempt++) {
			double sign = random.nextBoolean() ? 1 : -1;
			double along = sign * (SpearmanTuning.PATROL_SPAWN_MIN
					+ random.nextDouble() * (SpearmanTuning.PATROL_SPAWN_MAX - SpearmanTuning.PATROL_SPAWN_MIN));
			double t = d + along;
			BlockPos at = groundOnRoad(level, layout, bestRoad, t);
			if (at == null || Math.hypot(at.getX() - px, at.getZ() - pz) < SpearmanTuning.PATROL_SPAWN_MIN - 4) continue;
			if (!level.getBiome(at).is(ModBiomes.PALE_FOREST) || Lanterns.isSafe(level, at)) continue;
			if (com.tarkrakov.tearsofink.world.ruins.Ruins.nearStart(layout, at.getX(), at.getZ())) continue; // у места прибытия тихо

			String squad = pickSquad(random, tier);
			UUID squadId = UUID.randomUUID();
			UUID leaderId = null;
			for (int i = 0; i < squad.length(); i++) {
				// строй: каждый следующий — чуть дальше по дороге (в сторону от игрока)
				BlockPos spot = groundOnRoad(level, layout, bestRoad, t + sign * i * SquadTuning.FORMATION_STEP);
				if (spot == null) spot = at;
				PaleSoldier mob = typeOf(squad.charAt(i)).create(level);
				if (mob == null) return;
				mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360f, 0f);
				mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.NATURAL, null);
				if (leaderId == null) leaderId = mob.getUUID();
				mob.joinSquad(squadId, leaderId);
				mob.setPower(SquadTuning.TIER_POWER[tier]);
				mob.setPatrol(bestRoad, t - SpearmanTuning.PATROL_SEGMENT / 2.0, t + SpearmanTuning.PATROL_SEGMENT / 2.0);
				level.addFreshEntity(mob);
			}
			return;
		}
	}

	/** Свободное место на земле на осевой дороги, или null. */
	private static BlockPos groundOnRoad(ServerLevel level, InkLandsLayout layout, int road, double t) {
		double[] p = layout.roadPoint(road, t);
		int x = (int) Math.floor(p[0]), z = (int) Math.floor(p[1]);
		BlockPos at = new BlockPos(x, 0, z);
		if (!level.hasChunkAt(at)) return null;
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		at = new BlockPos(x, y, z);
		if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()
				|| !level.getBlockState(at.above(2)).isAir()) return null;
		if (!level.getFluidState(at.below()).isEmpty() || level.getBlockState(at.below()).isAir()) return null;
		return at;
	}
}
