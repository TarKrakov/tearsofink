package com.tarkrakov.tearsofink.travel;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Зеркальце нельзя отложить: реплика Проводника (не чаще раза в 3 с). */
public final class MirrorBond {
	private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

	private MirrorBond() {}

	static void clear() {
		LAST.clear();
	}

	public static void refuseStash(ServerPlayer p) {
		long now = p.server.getTickCount();
		Long last = LAST.get(p.getUUID());
		if (last != null && now - last < 60) return;
		LAST.put(p.getUUID(), now);
		Guide.say(p, "stash");
	}
}
