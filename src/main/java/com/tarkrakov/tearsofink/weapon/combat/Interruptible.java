package com.tarkrakov.tearsofink.weapon.combat;

import net.minecraft.world.entity.LivingEntity;

/** Существо с замахом, который можно сбить (удар эфесом). */
public interface Interruptible {
	/**
	 * Сбить замах. Возвращает true, если замах был и его сбили (тогда существо ненадолго теряется на stagger тиков);
	 * false — замаха не было (тогда просто лёгкое отбрасывание).
	 */
	boolean interrupt(LivingEntity by, int stagger);
}
