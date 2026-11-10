package com.tarkrakov.tearsofink.block.lantern;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Блок, в котором танцует огонёк-частица, привязанный к одной точке. */
public interface FlameAnchor {
	/** Где огонёк (в блоках от угла блока) или null, если сейчас не горит. */
	Vec3 flameOffset(BlockState state);
}
