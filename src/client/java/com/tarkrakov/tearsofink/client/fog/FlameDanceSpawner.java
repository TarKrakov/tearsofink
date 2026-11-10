package com.tarkrakov.tearsofink.client.fog;

import com.tarkrakov.tearsofink.block.lantern.FlameAnchor;
import com.tarkrakov.tearsofink.block.lantern.Lanterns;
import com.tarkrakov.tearsofink.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Держит по одному танцующему огоньку в каждом горящем фонаре рядом с игроком. */
public final class FlameDanceSpawner {
	/** В каком радиусе от игрока показывать огоньки (блоков). */
	public static final int RANGE = 48;
	private static final int CHECK_INTERVAL = 10;

	private static final Map<BlockPos, Particle> FLAMES = new HashMap<>();
	private static ClientLevel lastLevel;
	private static int ticks;

	private FlameDanceSpawner() {}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level != lastLevel) {
			FLAMES.clear();
			lastLevel = level;
		}
		if (level == null || mc.player == null || mc.isPaused() || ++ticks % CHECK_INTERVAL != 0) return;

		Set<BlockPos> seen = new HashSet<>();
		Vec3 p = mc.player.position();
		Lanterns.forEachLit(level, p.x, p.z, RANGE, (pos, state, lantern) -> {
			if (!(state.getBlock() instanceof FlameAnchor anchor)) return;
			Vec3 offset = anchor.flameOffset(state);
			if (offset == null) return;
			BlockPos key = pos.immutable();
			seen.add(key);
			Particle existing = FLAMES.get(key);
			if (existing == null || !existing.isAlive()) {
				Particle flame = mc.particleEngine.createParticle(ModParticles.FLAME_DANCE,
						pos.getX() + offset.x, pos.getY() + offset.y, pos.getZ() + offset.z, 0, 0, 0);
				if (flame != null) FLAMES.put(key, flame);
			}
		});
		// огоньки фонарей, которые далеко или исчезли, убираем
		Iterator<Map.Entry<BlockPos, Particle>> it = FLAMES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<BlockPos, Particle> e = it.next();
			if (!seen.contains(e.getKey()) || !e.getValue().isAlive()) {
				e.getValue().remove();
				it.remove();
			}
		}
	}
}
