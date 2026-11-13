package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Измерение Чернильных Земель (описано в data/tears-of-ink/dimension/ink_lands.json). */
public final class ModDimensions {
	public static final ResourceKey<Level> INK_LANDS = ResourceKey.create(Registries.DIMENSION, TearsOfInk.id("ink_lands"));

	private ModDimensions() {}

	public static void init() {
		Registry.register(BuiltInRegistries.BIOME_SOURCE, TearsOfInk.id("ink_lands"), InkLandsBiomeSource.CODEC);
		InkLandsBorder.init();
	}

	public static boolean isLands(Level level) {
		return level.dimension() == INK_LANDS;
	}

	@Nullable
	public static ServerLevel lands(MinecraftServer server) {
		return server.getLevel(INK_LANDS);
	}

	/** Точка прибытия: на дороге у края Бледнолесья, рядом со стартовым храмом (одна на мир; road больше не важен). */
	public static Vec3 arrival(ServerLevel lands, int road) {
		InkLandsLayout layout = InkLandsLayout.of(lands);
		double[] p = layout != null ? arrivalXZ(layout) : new double[]{InkLandsLayout.RADIUS * 0.9, 0};
		return onGround(lands, (int) Math.floor(p[0]), (int) Math.floor(p[1]));
	}

	/** {x, z} места прибытия. */
	public static double[] arrivalXZ(InkLandsLayout layout) {
		double[] s = com.tarkrakov.tearsofink.world.ruins.Ruins.start(layout);
		return new double[]{s[2], s[3]};
	}

	/** Поворот у места прибытия — лицом к стартовому храму; в остальных местах — спиной к краю. */
	public static float arrivalYaw(ServerLevel lands, Vec3 at) {
		InkLandsLayout layout = InkLandsLayout.of(lands);
		if (layout != null) {
			double[] s = com.tarkrakov.tearsofink.world.ruins.Ruins.start(layout);
			if (Math.hypot(s[2] - at.x, s[3] - at.z) < 8) {
				return (float) Math.toDegrees(Math.atan2(-(s[0] - at.x), s[1] - at.z));
			}
		}
		return faceInward(at);
	}

	/** Поворот спиной к ближайшему краю (перпендикулярно от него) — чтобы новичок не шёл сразу в стену тумана. */
	public static float faceInward(Vec3 at) {
		double s = 6;
		double gx = InkLandsLayout.edgeDepth(at.x + s, at.z) - InkLandsLayout.edgeDepth(at.x - s, at.z);
		double gz = InkLandsLayout.edgeDepth(at.x, at.z + s) - InkLandsLayout.edgeDepth(at.x, at.z - s);
		if (gx * gx + gz * gz < 1.0E-6) { gx = -at.x; gz = -at.z; }
		return (float) Math.toDegrees(Math.atan2(-gx, gz));
	}

	/** Центр блока на поверхности (чанк загружается). */
	public static Vec3 onGround(ServerLevel level, int x, int z) {
		level.getChunk(x >> 4, z >> 4);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return new Vec3(x + 0.5, y, z + 0.5);
	}

	public static BlockPos blockOf(Vec3 v) {
		return BlockPos.containing(v);
	}
}
