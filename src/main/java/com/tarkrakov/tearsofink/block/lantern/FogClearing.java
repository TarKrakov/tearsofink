package com.tarkrakov.tearsofink.block.lantern;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Блок, который разгоняет мглу вокруг себя (фонари, жаровня).
 * Пока горит: рассеивает туман и клубы, светит и не даёт появляться нашим врагам.
 */
public interface FogClearing {
	/** Горит ли. */
	BooleanProperty LIT = BlockStateProperties.LIT;
	/** Поставлен игроком в выживании (можно забрать; считается в лимит). Иначе — мировой. */
	BooleanProperty PLAYER_MADE = BooleanProperty.create("player_made");

	/** Радиус полной ясности (в блоках); дальше ясность плавно спадает. */
	int clearRadius();

	/** Разгоняет ли мглу в этом состоянии. */
	default boolean clears(BlockState state) {
		return !state.hasProperty(LIT) || state.getValue(LIT);
	}

	/** Самодельный ли (считается в лимит «Пламени»). */
	default boolean isPlayerMade(BlockState state) {
		return state.hasProperty(PLAYER_MADE) && state.getValue(PLAYER_MADE);
	}
}
