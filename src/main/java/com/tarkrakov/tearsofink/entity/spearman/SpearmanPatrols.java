package com.tarkrakov.tearsofink.entity.spearman;

import com.tarkrakov.tearsofink.block.lantern.Lanterns;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Патрули копейщиков у дорог Бледнолесья: раз в 10 с возле каждого игрока в Землях, если копейщиков рядом мало,
 * ставит 1–2 на дорогу вне видимости (в тумане) — не в свете горящих фонарей.
 */
public final class SpearmanPatrols {
	private SpearmanPatrols() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(SpearmanPatrols::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % SpearmanTuning.PATROL_SPAWN_INTERVAL != 0) return;
		ServerLevel level = server.overworld();
		if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) return;
		InkLandsLayout layout = InkLandsLayout.of(level);
		if (layout == null) return;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			if (layout.depthInside(player.getBlockX(), player.getBlockZ()) < 0) continue;
			trySpawnNear(level, layout, player);
		}
	}

	private static void trySpawnNear(ServerLevel level, InkLandsLayout layout, ServerPlayer player) {
		int near = level.getEntities(ModMobs.PALE_SPEARMAN, player.getBoundingBox().inflate(SpearmanTuning.PATROL_COUNT_RADIUS),
				e -> e.isAlive()).size();
		if (near >= SpearmanTuning.PATROL_MAX_NEAR) return;

		// ближайшая дорога: точка на каждой дороге на том же расстоянии от центра, что и игрок
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
			double along = (random.nextBoolean() ? 1 : -1) * (SpearmanTuning.PATROL_SPAWN_MIN
					+ random.nextDouble() * (SpearmanTuning.PATROL_SPAWN_MAX - SpearmanTuning.PATROL_SPAWN_MIN));
			double t = d + along;
			double[] p = layout.roadPoint(bestRoad, t);
			int x = (int) Math.floor(p[0]), z = (int) Math.floor(p[1]);
			if (Math.hypot(x - px, z - pz) < SpearmanTuning.PATROL_SPAWN_MIN - 4) continue;
			BlockPos at = new BlockPos(x, 0, z);
			if (!level.hasChunkAt(at)) continue;
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			at = new BlockPos(x, y, z);
			if (!level.getBiome(at).is(ModBiomes.PALE_FOREST)) continue;
			if (Lanterns.isSafe(level, at)) continue;
			if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
			if (!level.getFluidState(at.below()).isEmpty() || level.getBlockState(at.below()).isAir()) continue;

			int count = 1 + random.nextInt(2);
			for (int i = 0; i < count; i++) {
				PaleSpearmanEntity mob = ModMobs.PALE_SPEARMAN.create(level);
				if (mob == null) return;
				mob.moveTo(x + 0.5 + i * 0.8, y, z + 0.5, random.nextFloat() * 360f, 0f);
				mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
				mob.setPatrol(bestRoad, t - SpearmanTuning.PATROL_SEGMENT / 2.0, t + SpearmanTuning.PATROL_SEGMENT / 2.0);
				level.addFreshEntity(mob);
			}
			return;
		}
	}
}
