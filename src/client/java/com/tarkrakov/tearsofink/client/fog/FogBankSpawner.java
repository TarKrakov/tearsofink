package com.tarkrakov.tearsofink.client.fog;

import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.world.ModBiomes;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Каждый тик раскидывает клубы тумана по участкам Чернильных пустошей вокруг игрока.
 * Работает и снаружи биома — поэтому издалека видна стена мглы.
 */
public final class FogBankSpawner {
	// ===== Настройки =====
	/** Сколько попыток заспавнить клуб за тик. */
	public static final int ATTEMPTS_PER_TICK = 4;
	/** Радиус вокруг игрока, где появляются клубы (в блоках). */
	public static final double MIN_RADIUS = 6, MAX_RADIUS = 72;
	/** Насколько выше поверхности висят клубы (в блоках). */
	public static final double MAX_HEIGHT_ABOVE_GROUND = 6;
	/** Глубоко внутри биома клубов меньше (туман там и так густой). 0..1 */
	public static final float INSIDE_REDUCTION = 0.6f;

	private FogBankSpawner() {}

	/** Оттенок клубов тумана по зонам (множитель цвета текстуры). */
	private static float[] tint(Holder<Biome> biome) {
		if (biome.is(ModBiomes.BLOODWOOD)) return new float[]{1.0f, 0.62f, 0.5f};
		if (biome.is(ModBiomes.INKWOOD)) return new float[]{0.42f, 0.38f, 0.5f};
		if (biome.is(ModBiomes.TAR_LAKE)) return new float[]{0.6f, 0.58f, 0.66f};
		return new float[]{1f, 1f, 1f};
	}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		Player player = mc.player;
		if (level == null || player == null || mc.isPaused() || InkFog.disabled) return;

		RandomSource random = level.random;
		float keepChance = 1f - INSIDE_REDUCTION * InkFog.strength();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int i = 0; i < ATTEMPTS_PER_TICK; i++) {
			if (random.nextFloat() > keepChance) continue;
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = MIN_RADIUS + random.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
			pos.set(x, (int) player.getY(), z);
			if (!InkFog.isInk(level, pos)) continue;
            // Scatter no new banks across a freshly discovered battlefield
            // while leaving the enclosing fog forest completely unchanged.
            if (random.nextFloat() > BattlefieldClarity.particleMultiplier(x + .5, z + .5)) continue;

			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			if (ground <= level.getMinBuildHeight() + 1) continue;
			double y = ground + 0.5 + random.nextDouble() * MAX_HEIGHT_ABOVE_GROUND;
			// у горящих фонарей клубы не появляются
			if (LanternClarity.inBubble(x + 0.5, y, z + 0.5, 3)) continue;

			// Цвет клуба зависит от зоны (передаём через «скорость» — частица её не использует)
			float[] tint = tint(level.getBiome(pos));
			level.addAlwaysVisibleParticle(ModParticles.FOG_BANK, true,
					x + random.nextDouble(), y, z + random.nextDouble(), tint[0], tint[1], tint[2]);
		}

        // Only in the discovered battlefield: a sparse, paper-thin mist
        // practically touching the local block collision surface. The outer
        // Pale Forest keeps its full billowing fog bank density.
        if (level.getGameTime() % 3 != 0
                || BattlefieldClarity.opennessAt(player.getX(), player.getZ()) < .32f) return;
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 2 + random.nextDouble() * 22;
        int gx = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
        int gz = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
        if (BattlefieldClarity.opennessAt(gx + .5, gz + .5) < .48f) return;
        pos.set(gx, player.getBlockY(), gz);
        if (!InkFog.isInk(level, pos)) return;
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz);
        BlockPos base = new BlockPos(gx, gy - 1, gz);
        var support = level.getBlockState(base);
        var shape = support.getCollisionShape(level, base);
        if (shape.isEmpty() || !support.getFluidState().isEmpty()
                || !support.isFaceSturdy(level, base, Direction.UP)) return;
        double surface = base.getY() + shape.max(Direction.Axis.Y) + .035d;
        // -2 flags the static ground plane in FogBankParticle.Provider.
        level.addAlwaysVisibleParticle(ModParticles.FOG_BANK, true,
                gx + .5d, surface, gz + .5d, -2d, 0d, 0d);

	}
}
