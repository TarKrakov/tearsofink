package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.lantern.InkLanternBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;

/** Finishes the reserved approach columns after decoration, before their first light pass. */
public final class TemplePaths {
	private TemplePaths() {}

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var level = ModDimensions.lands(server);
			if (level == null) return;
			var layout = InkLandsLayout.of(level);
			if (layout == null || !layout.hasTempleNetwork()) return;
			// Warm the pure plan also on restarts that only load already generated chunks.
			var inputs = inputs(level);
			Util.backgroundExecutor().execute(() -> layout.prepareTempleNetwork(inputs));
		});
	}

	public static void finishDecoration(ServerLevel level, ChunkAccess chunk) {
		if (!ModDimensions.isLands(level) || !(chunk instanceof ProtoChunk) || chunk instanceof ImposterProtoChunk) return;
		var layout = InkLandsLayout.of(level);
		if (layout == null || !layout.hasTempleNetwork()) return;
		var plan = layout.templeNetwork();
		if (plan == null) {
			var pos = chunk.getPos();
			double depth = layout.depthInside(pos.getMiddleBlockX(), pos.getMiddleBlockZ());
			if (depth < 170 || depth > 1430) return;
			// A saved protochunk can resume at LIGHT before the startup worker has finished.
			plan = layout.prepareTempleNetwork(inputs(level));
		}
		for (var column : plan.path(chunk.getPos())) {
			BlockPos floor = column.floor();
			if (layout.roadNear(floor.getX(), floor.getZ(), TempleNetwork.ROAD_BUFFER - InkLandsLayout.ROAD_HALF_WIDTH) != null) continue;
			place(chunk, column);
		}
	}

	private static TempleNetwork.Inputs inputs(ServerLevel level) {
		var source = level.getChunkSource();
		var height = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
		var terrain = new RoadTerrain(source.getGenerator(), height, source.randomState());
		return new TempleNetwork.Inputs(terrain, level.registryAccess(), level.getStructureManager(), level.getSeed());
	}

	private static void place(ChunkAccess chunk, TempleNetwork.PathBlock column) {
		BlockPos floor = column.floor();
		BlockState material = (floor.asLong() & 3) == 0
				? ModBlocks.OVERGROWN_RUIN_BRICKS.defaultBlockState() : ModBlocks.CRACKED_RUIN_BRICKS.defaultBlockState();
		if (column.up() != null && !column.lantern()) {
			material = ModBlocks.CRACKED_RUIN_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, column.up());
		}
		chunk.setBlockState(floor, material, false);
		for (int dy = 1; dy <= 7; dy++) {
			BlockPos below = floor.below(dy);
			if (chunk.getBlockState(below).isSolid()) break;
			chunk.setBlockState(below, ModBlocks.ASPHODELITE_ROCK.defaultBlockState(), false);
		}
		for (int dy = 1; dy <= 3; dy++) {
			chunk.setBlockState(floor.above(dy), Blocks.AIR.defaultBlockState(), false);
		}
		if (column.lantern()) {
			chunk.setBlockState(floor.above(), ModBlocks.CRACKED_RUIN_BRICKS.defaultBlockState(), false);
			var state = ModBlocks.INK_LANTERN.defaultBlockState();
			BlockPos pos = floor.above(2);
			chunk.setBlockState(pos, state, false);
			chunk.setBlockEntity(((InkLanternBlock) state.getBlock()).newBlockEntity(pos, state));
		}
	}
}
