package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.lantern.InkLanternBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
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
		ServerWorldEvents.LOAD.register((server, level) -> {
			if (!ModDimensions.isLands(level)) return;
			var source = level.getChunkSource();
			if (source.getGenerator().getBiomeSource() instanceof InkLandsBiomeSource biomes) {
				biomes.pinTempleLayout(source.randomState().sampler());
				var layout = InkLandsLayout.of(level);
				if (layout != null && layout.hasTempleNetwork()) {
					layout.attachTemplePlanFile(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
							.resolve("data").resolve("tearsofink_temple_network_v" + layout.templeLayoutVersion() + ".bin"));
					// A saved plan is small; restore once here, not during the first arrival.
					layout.restoreTempleNetworkIfPresent(inputs(level));
				}
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var level = ModDimensions.lands(server);
			if (level == null) return;
			var layout = InkLandsLayout.of(level);
			if (layout == null || !layout.hasTempleNetwork()) return;
			if (layout.templeNetwork() != null) return;
			// First launch or one-time migration of an existing experimental world.
			var inputs = inputs(level);
			Util.backgroundExecutor().execute(() -> {
				try {
					layout.prepareTempleNetwork(inputs);
				} catch (RuntimeException failure) {
					com.tarkrakov.tearsofink.TearsOfInk.LOGGER.error("D-060 experimental temple plan failed", failure);
				}
			});
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
		for (var temple:plan.temples()) if (temple.raised()) raiseFoundation(chunk,layout,temple);
		for (var column : plan.path(chunk.getPos())) {
			BlockPos floor = column.floor();
			if (column.engineered() ? !TempleRoadAccess.canBuild(layout,floor.getX(),floor.getZ())
					: layout.roadNear(floor.getX(), floor.getZ(), TempleNetwork.ROAD_BUFFER - InkLandsLayout.ROAD_HALF_WIDTH) != null) continue;
			place(chunk, column);
		}
	}

	/** Chunk-local grounded soil/rock fill, before approach placement. */
	private static void raiseFoundation(ChunkAccess chunk,InkLandsLayout layout,TempleNetwork.Temple temple) {
		var facing=temple.rotation().rotate(net.minecraft.core.Direction.SOUTH);
		int hx=(facing.getAxis()==net.minecraft.core.Direction.Axis.X?TempleNetwork.TEMPLATE_LENGTH:TempleNetwork.TEMPLATE_WIDTH)/2+1;
		int hz=(facing.getAxis()==net.minecraft.core.Direction.Axis.Z?TempleNetwork.TEMPLATE_LENGTH:TempleNetwork.TEMPLATE_WIDTH)/2+1;
		var cp=chunk.getPos();
		int minX=Math.max(cp.getMinBlockX(),temple.x()-hx-TempleFoundation.APRON);
		int maxX=Math.min(cp.getMaxBlockX(),temple.x()+hx+TempleFoundation.APRON);
		int minZ=Math.max(cp.getMinBlockZ(),temple.z()-hz-TempleFoundation.APRON);
		int maxZ=Math.min(cp.getMaxBlockZ(),temple.z()+hz+TempleFoundation.APRON);
		for (int x=minX;x<=maxX;x++) for (int z=minZ;z<=maxZ;z++) {
			int dist=TempleFoundation.distance(x-temple.x(),z-temple.z(),hx,hz);
			if (dist>TempleFoundation.APRON || layout.roadNear(x,z,TempleNetwork.ROAD_BUFFER-InkLandsLayout.ROAD_HALF_WIDTH)!=null) continue;
			int top=TempleFoundation.top(temple.y(),dist);
			for (int y=top;y>=chunk.getMinBuildHeight();y--) {
				var pos=new BlockPos(x,y,z);
				var state=chunk.getBlockState(pos);
				if (state.isSolid() && state.getFluidState().isEmpty()
						&& !state.is(net.minecraft.tags.BlockTags.LOGS) && !state.is(net.minecraft.tags.BlockTags.LEAVES)) break;
				chunk.setBlockState(pos,(y>=top-1?ModBlocks.ASHEN_SOIL:ModBlocks.ASPHODELITE_ROCK).defaultBlockState(),false);
			}
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
			var state=chunk.getBlockState(below);
			if (state.isSolid() && (!column.engineered() || state.getFluidState().isEmpty()
					&& !state.is(net.minecraft.tags.BlockTags.LOGS) && !state.is(net.minecraft.tags.BlockTags.LEAVES))) break;
			chunk.setBlockState(below, ModBlocks.ASPHODELITE_ROCK.defaultBlockState(), false);
		}
		int clearance=column.engineered()?TempleRoadAccess.CUT+3:3;
		for (int dy = 1; dy <= clearance && floor.getY()+dy<chunk.getMaxBuildHeight(); dy++) {
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
