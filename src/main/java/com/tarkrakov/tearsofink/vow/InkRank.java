package com.tarkrakov.tearsofink.vow;

import com.tarkrakov.tearsofink.advancement.ModAdvancements;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.ink.InkTuning;
import net.minecraft.server.level.ServerPlayer;

/**
 * Стадия Чернил — по нынешнему максимуму энергии, а не по проданным частям Души.
 * Преимущества действуют только при обете Чернил (гибриды не получают бонусы обоих путей).
 */
public enum InkRank {
	NONE("none", 0),
	MARKED("marked", InkTuning.MARKED_AT),
	TAINTED("tainted", InkTuning.TAINTED_AT),
	HOLLOW("hollow", InkTuning.HOLLOW_AT);

	public final String id;
	public final int minimum;

	InkRank(String id, int minimum) {
		this.id = id;
		this.minimum = minimum;
	}

	public static InkRank fromMaximum(int maximum) {
		if (maximum >= HOLLOW.minimum) return HOLLOW;
		if (maximum >= TAINTED.minimum) return TAINTED;
		if (maximum >= MARKED.minimum) return MARKED;
		return NONE;
	}

	public static InkRank active(ServerPlayer player) {
		return Vows.vow(player) == Vow.INK ? fromMaximum(InkPowers.get(player).inkMax()) : NONE;
	}

	public boolean atLeast(InkRank other) {
		return ordinal() >= other.ordinal();
	}

	/** Advancement остаётся в истории при последующей потере ранга. */
	public static void grantReached(ServerPlayer player) {
		if (Vows.vow(player) != Vow.INK) return;
		InkRank rank = fromMaximum(InkPowers.get(player).inkMax());
		if (rank.atLeast(MARKED)) ModAdvancements.grant(player, "ink_marked");
		if (rank.atLeast(TAINTED)) ModAdvancements.grant(player, "ink_tainted");
		if (rank.atLeast(HOLLOW)) ModAdvancements.grant(player, "ink_hollow");
	}
}
