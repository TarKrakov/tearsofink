package com.tarkrakov.tearsofink.travel;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class FountainLanding {
	private static final int RADIUS = 3;
	private static final int[] HEIGHT_OFFSETS = {0, 1, -1, 2, -2};
	private static final int MIN_BLOCK_LIGHT = 2;
	private static final int MIN_SKY_LIGHT = 8;

	private FountainLanding() {}

	static boolean inside(ServerLevel level, BlockPos pos) {
		return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
				&& InkLandsLayout.edgeDepth(pos.getX() + 0.5, pos.getZ() + 0.5) >= 0;
	}


	static java.util.List<ChunkPos> requiredChunks(BlockPos fountain) {
		// Include blocks beside the standing box queried by the collision scanner.
		int margin = RADIUS + 1;
		var chunks = new java.util.ArrayList<ChunkPos>(4);
		for (int cx = (fountain.getX() - margin) >> 4; cx <= (fountain.getX() + margin) >> 4; cx++) {
			for (int cz = (fountain.getZ() - margin) >> 4; cz <= (fountain.getZ() + margin) >> 4; cz++) {
				chunks.add(new ChunkPos(cx, cz));
			}
		}
		return chunks;
	}

	static BlockPos find(ServerLevel level, ServerPlayer player, BlockPos fountain) {
		for (int distance = 1; distance <= RADIUS; distance++) {
			for (int dy : HEIGHT_OFFSETS) {
				for (int x = -distance; x <= distance; x++) {
					for (int z = -distance; z <= distance; z++) {
						if (Math.max(Math.abs(x), Math.abs(z)) != distance) continue;
						BlockPos feet = fountain.offset(x, dy, z);
						if (safe(level, player, feet)) return feet;
					}
				}
			}
		}
		return null;
	}

	private static boolean safe(ServerLevel level, ServerPlayer player, BlockPos feet) {
		if (!inside(level, feet) || level.isOutsideBuildHeight(feet.below()) || level.isOutsideBuildHeight(feet.above())) return false;
		Vec3 at = Vec3.atBottomCenterOf(feet);
		AABB body = player.getDimensions(Pose.STANDING).makeBoundingBox(at);
		if (!level.getWorldBorder().isWithinBounds(body.minX, body.minZ)
				|| !level.getWorldBorder().isWithinBounds(body.maxX, body.maxZ) || body.maxY > level.getMaxBuildHeight()) return false;
		BlockPos floorPos = feet.below();
		BlockState floor = level.getBlockState(floorPos);
		if (!floor.isFaceSturdy(level, floorPos, Direction.UP) || !floor.isCollisionShapeFullBlock(level, floorPos)
				|| !floor.getFluidState().isEmpty() || hazard(floor)) return false;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(body.minX, body.minY, body.minZ),
				BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
			BlockState state = level.getBlockState(pos);
			if (!state.getFluidState().isEmpty() || hazard(state)) return false;
		}
		if (level.getBrightness(LightLayer.BLOCK, feet) < MIN_BLOCK_LIGHT
				&& level.getBrightness(LightLayer.SKY, feet) - level.getSkyDarken() < MIN_SKY_LIGHT) return false;
		return level.noCollision(player, body) && level.getEntities(player, body, entity -> entity.isAlive() && entity.isPickable()).isEmpty();
	}

	private static boolean hazard(BlockState state) {
		return state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(Blocks.MAGMA_BLOCK)
				|| state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
				|| state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POINTED_DRIPSTONE)
				|| state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)
				|| state.is(ModBlocks.INK_FOUNTAIN);
	}
}
