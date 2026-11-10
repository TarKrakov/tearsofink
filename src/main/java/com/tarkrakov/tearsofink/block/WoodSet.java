package com.tarkrakov.tearsofink.block;

import net.minecraft.world.level.block.Block;

import java.util.List;

/** Полный набор блоков одной породы дерева. */
public record WoodSet(Block log, Block strippedLog, Block wood, Block strippedWood, Block planks, Block leaves,
					  Block sapling, Block stairs, Block slab, Block fence, Block fenceGate, Block door,
					  Block trapdoor, Block button, Block pressurePlate) {
	/** Порядок для творческой вкладки. */
	public List<Block> all() {
		return List.of(log, strippedLog, wood, strippedWood, planks, stairs, slab, fence, fenceGate,
				door, trapdoor, pressurePlate, button, leaves, sapling);
	}
}
