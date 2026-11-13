package com.tarkrakov.tearsofink.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Край Земель — стена тумана: у края клиент рисует плотные клубы (FogWallSpawner), туман сгущается (InkFog).
 * Кто зашёл в стену глубже {@link #LOOP_DEPTH}, того переносит в случайное место перед стеной,
 * в {@link #RETURN_DEPTH} блоках от неё (вместе с тем, на ком он едет).
 */
public final class InkLandsBorder {
	/** На сколько блоков за край можно зайти, прежде чем перенесёт. */
	public static final int LOOP_DEPTH = 8;
	/** Куда переносит: на столько блоков внутрь от края. */
	public static final int RETURN_DEPTH = 10;
	/** И вбок вдоль стены — случайно, до стольких блоков. */
	public static final int RETURN_SPREAD = 16;

	private InkLandsBorder() {}

	static void init() {
		ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (!ModDimensions.isLands(level) || level.getGameTime() % 10 != 0) return;
			InkLandsLayout layout = InkLandsLayout.of(level);
			if (layout == null) return;
			for (ServerPlayer player : java.util.List.copyOf(level.players())) {
				if (player.isSpectator()) continue;
				double depth = layout.depthInside(player.getBlockX(), player.getBlockZ());
				if (depth > -LOOP_DEPTH) continue;
				loopBack(level, player, depth);
			}
		});
	}

	private static void loopBack(ServerLevel level, ServerPlayer player, double depth) {
		double x = player.getX(), z = player.getZ();
		double r = Math.sqrt(x * x + z * z);
		if (r < 1) return;
		double angle = Math.atan2(z, x) + (player.getRandom().nextDouble() * 2 - 1) * RETURN_SPREAD / r;
		double nx = Math.cos(angle) * r, nz = Math.sin(angle) * r;
		// внутрь до нужной глубины (край рваный — уточняем по месту)
		double target = r;
		for (int i = 0; i < 6; i++) {
			double d = InkLandsLayout.edgeDepth(Math.cos(angle) * target, Math.sin(angle) * target);
			target = Math.max(1, target + d - RETURN_DEPTH);
		}
		Vec3 ground = ModDimensions.onGround(level, (int) Math.floor(Math.cos(angle) * target), (int) Math.floor(Math.sin(angle) * target));
		Entity root = player.getRootVehicle();
		if (root != player) {
			root.teleportTo(ground.x, ground.y, ground.z); // вместе с седоками
		} else {
			player.teleportTo(ground.x, ground.y, ground.z);
		}
		player.resetFallDistance();
	}
}
