package com.tarkrakov.tearsofink.vow;

import com.tarkrakov.tearsofink.advancement.ModAdvancements;
import com.tarkrakov.tearsofink.ink.InkPowers;
import net.minecraft.server.level.ServerPlayer;

/**
 * Боевой ранг Души вычисляется из ТЕКУЩЕГО максимума (не текущей энергии и не истории Подвигов).
 * Бонусы рангов активны исключительно на Пути Души. Числа — пилот D-036.
 */
public enum SoulRank {
	NONE("none", 0),
	STEADFAST("steadfast", 115),
	LUMINOUS("luminous", 130),
	UNBENDING("unbending", 150);

	public final String id;
	public final int minimum;

	SoulRank(String id, int minimum) {
		this.id = id;
		this.minimum = minimum;
	}

	public static SoulRank fromMaximum(int maximum) {
		if (maximum >= UNBENDING.minimum) return UNBENDING;
		if (maximum >= LUMINOUS.minimum) return LUMINOUS;
		if (maximum >= STEADFAST.minimum) return STEADFAST;
		return NONE;
	}

	/** Вне Пути Души привилегии ранга спят даже при большом максимуме Души. */
	public static SoulRank active(ServerPlayer player) {
		return Vows.vow(player) == Vow.SOUL ? fromMaximum(InkPowers.get(player).soulMax()) : NONE;
	}

	public boolean atLeast(SoulRank other) {
		return ordinal() >= other.ordinal();
	}

	/** Исторические достижения остаются выданными после понижения ранга. */
	public static void grantReached(ServerPlayer player) {
		if (Vows.vow(player) != Vow.SOUL) return;
		SoulRank rank = fromMaximum(InkPowers.get(player).soulMax());
		if (rank.atLeast(STEADFAST)) ModAdvancements.grant(player, "soul_steadfast");
		if (rank.atLeast(LUMINOUS)) ModAdvancements.grant(player, "soul_luminous");
		if (rank.atLeast(UNBENDING)) ModAdvancements.grant(player, "soul_unbending");
	}
}
