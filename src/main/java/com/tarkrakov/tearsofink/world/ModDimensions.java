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

	/** Точка прибытия: на дороге у края Бледнолесья (дорога — по номеру, любой int). */
	public static Vec3 arrival(ServerLevel lands, int road) {
		InkLandsLayout layout = InkLandsLayout.of(lands);
		double[] p = layout != null ? layout.arrivalPoint(road) : new double[]{InkLandsLayout.RADIUS * 0.9, 0};
		return onGround(lands, (int) Math.floor(p[0]), (int) Math.floor(p[1]));
	}

	/** Поворот «лицом к центру Земель» (от края) — чтобы новичок не шёл сразу в стену тумана. */
	public static float faceInward(Vec3 at) {
		return (float) Math.toDegrees(Math.atan2(at.x, -at.z));
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
