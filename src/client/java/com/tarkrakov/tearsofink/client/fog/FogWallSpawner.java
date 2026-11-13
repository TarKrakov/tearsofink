package com.tarkrakov.tearsofink.client.fog;

import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Стена тумана на краю Земель: когда игрок близко к краю, вдоль края вокруг него встают плотные клубы
 * (те же, что туман, но гуще). Только рядом с игроком — на FPS почти не влияет.
 */
public final class FogWallSpawner {
	/** С какого расстояния до края начинает вставать стена (блоки). */
	public static final double NOTICE = 40;
	/** Клубов за тик (у самого края; дальше — меньше). */
	public static final int PER_TICK = 5;
	/** Ширина стены вокруг игрока вдоль края (в обе стороны) и её толщина (от края внутрь / наружу). */
	public static final double SPREAD = 26, INNER = 3, OUTER = 9;
	public static final double MAX_HEIGHT = 9;

	private FogWallSpawner() {}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		Player player = mc.player;
		if (level == null || player == null || mc.isPaused() || InkFog.disabled || !ModDimensions.isLands(level)) return;
		double px = player.getX(), pz = player.getZ();
		double depth = InkLandsLayout.edgeDepth(px, pz);
		if (depth > NOTICE) return;
		double r = Math.sqrt(px * px + pz * pz);
		if (r < 1) return;
		RandomSource random = level.random;
		int count = depth <= 8 ? PER_TICK : Math.max(1, (int) Math.round(PER_TICK * (NOTICE - depth) / (NOTICE - 8)));
		double base = Math.atan2(pz, px);
		for (int i = 0; i < count; i++) {
			double angle = base + (random.nextDouble() * 2 - 1) * SPREAD / r;
			double want = -OUTER + random.nextDouble() * (OUTER + INNER); // глубина клуба относительно края
			double rad = r;
			for (int k = 0; k < 4; k++) {
				double d = InkLandsLayout.edgeDepth(Math.cos(angle) * rad, Math.sin(angle) * rad);
				rad += d - want;
			}
			double x = Math.cos(angle) * rad, z = Math.sin(angle) * rad;
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
			if (ground <= level.getMinBuildHeight() + 1) continue;
			double y = ground + random.nextDouble() * MAX_HEIGHT;
			level.addAlwaysVisibleParticle(ModParticles.FOG_WALL, true, x, y, z, 1, 1, 1);
		}
	}
}
