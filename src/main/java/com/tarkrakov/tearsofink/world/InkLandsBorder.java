package com.tarkrakov.tearsofink.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Край Земель — туманная петля: кто зашёл за край Бледнолесья глубже {@link #LOOP_DEPTH},
 * того незаметно переносит назад, к началу тумана (вместе с тем, на ком он едет).
 * Туман за краем сгущается на клиенте (InkFog).
 */
public final class InkLandsBorder {
	/** На сколько блоков за край можно зайти, прежде чем перенесёт. */
	public static final int LOOP_DEPTH = 40;
	/** Куда переносит: на столько блоков внутрь от края. */
	public static final int RETURN_DEPTH = 6;

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
		double target = Math.max(1, r + depth - RETURN_DEPTH); // depth < 0: уменьшаем радиус
		Vec3 ground = ModDimensions.onGround(level, (int) Math.floor(x * target / r), (int) Math.floor(z * target / r));
		Entity root = player.getRootVehicle();
		if (root != player) {
			root.teleportTo(ground.x, ground.y, ground.z); // вместе с седоками
		} else {
			player.teleportTo(ground.x, ground.y, ground.z);
		}
		player.resetFallDistance();
	}
}
