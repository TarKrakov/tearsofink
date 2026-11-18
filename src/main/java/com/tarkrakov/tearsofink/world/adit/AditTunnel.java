package com.tarkrakov.tearsofink.world.adit;

import com.tarkrakov.tearsofink.block.ModBlocks;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.LinkedHashMap;
import java.util.Map;

/** The original two-wide switchback, in chunk-local coordinates before rotation/reflection. */
final class AditTunnel {
	private static final int HEADROOM = 3;
	private static final int FLIGHT_LENGTH = 4;
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState LINING = ModBlocks.ASPHODELITE_ROCK.defaultBlockState();
	private static final BlockState FLOOR = ModBlocks.RUIN_BRICKS.defaultBlockState();
	private static final BlockState PALE_SUPPORT = ModBlocks.STRIPPED_PALE_LOG.defaultBlockState();
	private static final BlockState LANTERN = ModBlocks.INK_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
	private static final Flight[] FLIGHTS = {
			new Flight(6, 4, Direction.EAST, 10, 4),
			new Flight(10, 6, Direction.SOUTH, 10, 10),
			new Flight(9, 10, Direction.WEST, 4, 10),
			new Flight(4, 9, Direction.NORTH, 4, 4)
	};

	private final ChunkPos chunk;
	private final int rotation;
	private final boolean reflected;
	final int sideShift;
	final int variant;
	final int entryLane;
	private final Map<BlockPos, BlockState> interior = new LinkedHashMap<>();
	private final Map<BlockPos, BlockState> shell = new LinkedHashMap<>();
	final BlockPos entrance;
	final BlockPos landing;

	private record Flight(int x, int z, Direction direction, int landingX, int landingZ) {}

	private AditTunnel(ChunkPos chunk, int variant, int sideShift, int y, int lane) {
		this.chunk = chunk;
		this.rotation = variant % 4;
		this.reflected = variant >= 4;
		this.variant = variant;
		this.sideShift = sideShift;
		this.entryLane = lane;
		entrance = pos(1, y + 1, 4 + lane);
		buildPortal(y);
		int flight = 0;
		Flight last;
		do {
			last = FLIGHTS[flight % FLIGHTS.length];
			y = buildFlight(last, y);
			buildPlatform(last.landingX(), y, last.landingZ(), flight % 2 == 0);
			flight++;
		} while (y > AditSites.BOTTOM_Y);
		landing = pos(last.landingX(), y + 1, last.landingZ());
		// Six face neighbors of the usable space form a continuous lining, including door jambs and ceilings.
		interior.forEach((pos, state) -> {
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = pos.relative(direction);
				if (!interior.containsKey(neighbor)) shell.putIfAbsent(neighbor, LINING);
			}
		});
		// The apron is in the open. The lining begins at the timber frame.
		for (int z = 4; z <= 5; z++) {
			for (int dy = 1; dy <= HEADROOM; dy++) {
				shell.remove(pos(1, entrance.getY() - 1 + dy, z));
			}
			shell.remove(pos(2, entrance.getY() + HEADROOM, z));
		}
	}

	static AditTunnel at(ChunkPos chunk, int hint, int variant, int sideShift, int lane, ProtoChunk blocks) {
		BlockPos outside = position(chunk, variant % 4, variant >= 4, 1, hint, 4 + lane + sideShift);
		int top = blocks.getHeight(Heightmap.Types.WORLD_SURFACE_WG, outside.getX(), outside.getZ());
		for (int y = top; y > AditSites.BOTTOM_Y + FLIGHT_LENGTH; y--) {
			BlockPos floor = outside.atY(y);
			var ground = blocks.getBlockState(floor);
			if (!ground.getFluidState().isEmpty()) return null;
			if (!natural(ground) || !ground.isFaceSturdy(blocks, floor, Direction.UP)) continue;
			// Use the first actual terrain beneath the canopy. An isolated cave floor is not a surface approach.
			if (y > hint + AditSites.SURFACE_TOLERANCE || !open(blocks, floor.above()) || !open(blocks, floor.above(2))) {
				return null;
			}
			int otherZ = 5 - lane + sideShift;
			BlockPos other = position(chunk, variant % 4, variant >= 4, 1, y, otherZ);
			return dryApproach(blocks, floor) && dryApproach(blocks, other)
					? new AditTunnel(chunk, variant, sideShift, y, lane) : null;
		}
		return null;
	}

	private static boolean dryApproach(BlockGetter level, BlockPos floor) {
		for (int dy = 0; dy <= HEADROOM + 1; dy++) {
			if (!level.getFluidState(floor.above(dy)).isEmpty()) return false;
		}
		return true;
	}

	private static boolean open(BlockGetter level, BlockPos pos) {
		var state = level.getBlockState(pos);
		return state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
	}

	private void buildPortal(int y) {
		for (int x = 2; x <= 3; x++) {
			for (int z = 4; z <= 5; z++) {
				walkway(x, y, z, FLOOR);
			}
		}
		buildPlatform(4, y, 4, false);
		for (int z = 3; z <= 6; z++) {
			shell.put(pos(3, y + HEADROOM + 1, z), PALE_SUPPORT.setValue(RotatedPillarBlock.AXIS,
					rotation % 2 == 0 ? Direction.Axis.Z : Direction.Axis.X));
		}
		for (int z : new int[]{3, 6}) {
			for (int dy = 1; dy <= HEADROOM; dy++) shell.put(pos(3, y + dy, z), PALE_SUPPORT);
		}
		interior.put(pos(3, y + HEADROOM, 5), LANTERN);
	}

	private int buildFlight(Flight flight, int y) {
		Direction uphill = flight.direction().getOpposite();
		// Compute the world-facing direction from the same transform as the excavation.
		BlockPos a = pos(7, 0, 7), b = pos(7 + uphill.getStepX(), 0, 7 + uphill.getStepZ());
		Direction facing = Direction.fromDelta(b.getX() - a.getX(), 0, b.getZ() - a.getZ());
		BlockState stair = ModBlocks.RUIN_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
		for (int step = 0; step < FLIGHT_LENGTH; step++) {
			int x = flight.x() + flight.direction().getStepX() * step;
			int z = flight.z() + flight.direction().getStepZ() * step;
			for (int lane = 0; lane < 2; lane++) {
				walkway(x + (flight.direction().getAxis() == Direction.Axis.Z ? lane : 0), y,
						z + (flight.direction().getAxis() == Direction.Axis.X ? lane : 0), stair);
			}
			y--;
		}
		return y;
	}

	private void buildPlatform(int x, int y, int z, boolean lit) {
		for (int dx = 0; dx < 2; dx++) {
			for (int dz = 0; dz < 2; dz++) walkway(x + dx, y, z + dz, FLOOR);
		}
		if (lit) interior.put(pos(x, y + HEADROOM, z), LANTERN);
	}

	private void walkway(int x, int y, int z, BlockState floor) {
		interior.put(pos(x, y, z), floor);
		for (int h = 1; h <= HEADROOM; h++) interior.put(pos(x, y + h, z), AIR);
	}

	private BlockPos pos(int x, int y, int z) {
		return position(chunk, rotation, reflected, x, y, z + sideShift);
	}

	private static BlockPos position(ChunkPos chunk, int rotation, boolean reflected, int x, int y, int z) {
		if (reflected) z = 15 - z;
		for (int turn = 0; turn < rotation; turn++) {
			int nextX = 15 - z;
			z = x;
			x = nextX;
		}
		return chunk.getBlockAt(x, y, z);
	}

	String check(BlockGetter level, BoundingBox piece) {
		for (var changes : java.util.List.of(interior, shell)) {
			for (var change : changes.entrySet()) {
				BlockPos pos = change.getKey();
				if (!piece.isInside(pos) || (pos.getX() >> 4) != chunk.x || (pos.getZ() >> 4) != chunk.z
						|| level.isOutsideBuildHeight(pos)) return "out of piece or chunk bounds";
				BlockState existing = level.getBlockState(pos);
				if (changes == shell && change.getValue() == LINING && adequateLining(level, pos, existing)) continue;
				if (existing.hasBlockEntity() || existing.is(BlockTags.LOGS) || existing.is(BlockTags.LEAVES)) {
					return "tree or block entity in excavation";
				}
				if (pos.getY() >= entrance.getY() - 1 && !existing.getFluidState().isEmpty()) return "fluid at surface opening";
				if (!natural(existing) && !existing.canBeReplaced()
						&& !existing.is(Blocks.WATER) && !existing.is(Blocks.LAVA) && !existing.is(Blocks.GLOW_LICHEN)) {
					return "protected block in excavation: " + existing + " at " + pos.toShortString();
				}
			}
		}
		return null;
	}

	void place(ProtoChunk level) {
		shell.forEach((pos, state) -> {
			if (state != LINING || !adequateLining(level, pos, level.getBlockState(pos))) {
				level.setBlockState(pos, state, false);
			}
		});
		interior.forEach((pos, state) -> {
			level.setBlockState(pos, state, false);
			if (state.getBlock() instanceof EntityBlock entityBlock) {
				var entity = entityBlock.newBlockEntity(pos, state);
				if (entity != null) level.setBlockEntity(entity);
			}
		});
	}

	private static boolean adequateLining(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getFluidState().isEmpty() && !(state.getBlock() instanceof FallingBlock) && state.isCollisionShapeFullBlock(level, pos);
	}

	private static boolean natural(BlockState state) {
		return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)
				|| state.is(ConventionalBlockTags.ORES)
				|| state.is(ModBlocks.ASPHODELITE_ORE) || state.is(ModBlocks.DEEPSLATE_ASPHODELITE_ORE)
				|| state.is(ModBlocks.PALE_ORE) || state.is(ModBlocks.ASH)
				|| state.is(ModBlocks.ASHEN_SOIL) || state.is(ModBlocks.ASPHODELITE_ROCK)
				|| state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
				|| state.is(Blocks.DRIPSTONE_BLOCK) || state.is(Blocks.TUFF);
	}
}
