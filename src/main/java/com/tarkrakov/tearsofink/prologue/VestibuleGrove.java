package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/** The composed fallback grove. Coordinates are local to the original north-facing portal. */
final class VestibuleGrove {
	static final int VERSION = 1;
	static final int FLOOR = 64;
	static final int LEFT = -1, RIGHT = 2, NORTH = -8, SOUTH = 7;
	static final BlockPos ARRIVAL = new BlockPos(0, FLOOR + 1, 6);
	static final BlockPos GUIDE = new BlockPos(0, FLOOR + 1, -5);
	static final BlockPos GATE_MIN = new BlockPos(-1, FLOOR + 1, NORTH);
	static final BlockPos GATE_MAX = new BlockPos(1, FLOOR + 4, NORTH);
	// Bounds also include the original island, so edits anywhere on it can veto migration.
	static final BlockPos MIN = new BlockPos(-10, FLOOR - 3, -11);
	static final BlockPos MAX = new BlockPos(10, FLOOR + 18, 11);

	enum Part { ROCK, PATH, SOIL, LEAF, LOG_X, LOG_Y, LOG_Z, LANTERN, GATE }

	private record Tree(int x, int z, int height, Direction lean, Direction fork) {}

	// Broken rhythm in section and plan, with the taller pair framing the turn of the head at the guide.
	private static final Tree[] TREES = {
		new Tree(-4, 8, 10, Direction.NORTH, Direction.EAST),
		new Tree(-2, 5, 12, Direction.WEST, Direction.NORTH),
		new Tree(-3, 1, 10, Direction.EAST, Direction.NORTH),
		new Tree(-2, -3, 13, Direction.WEST, Direction.NORTH),
		new Tree(-4, -7, 11, Direction.NORTH, Direction.EAST),
		new Tree(0, -10, 10, Direction.SOUTH, Direction.WEST),
		new Tree(3, -9, 12, Direction.EAST, Direction.SOUTH),
		new Tree(3, -5, 13, Direction.EAST, Direction.WEST),
		new Tree(4, -1, 11, Direction.WEST, Direction.SOUTH),
		new Tree(3, 3, 10, Direction.NORTH, Direction.EAST),
		new Tree(4, 7, 12, Direction.WEST, Direction.NORTH),
		new Tree(1, 9, 11, Direction.NORTH, Direction.WEST)
	};

	private static final Map<BlockPos, Part> PLAN = createPlan();

	private VestibuleGrove() {}

	static Map<BlockPos, Part> plan() {
		return java.util.Collections.unmodifiableMap(PLAN);
	}

	private static Map<BlockPos, Part> createPlan() {
		Map<BlockPos, Part> scene = new LinkedHashMap<>();

		for (int z = NORTH - 2; z <= SOUTH + 2; z++) {
			for (int x = LEFT - 2; x <= RIGHT + 2; x++) {
				put(scene, new BlockPos(x, FLOOR - 1, z), Part.ROCK);
				put(scene, new BlockPos(x, FLOOR, z), x >= LEFT && x <= 1 ? Part.PATH : Part.ROCK);
			}
		}

		for (Tree tree : TREES) plant(scene, tree);

		// The floor-level thicket seals the lane. Above it, pockets between
		// sprays expose the crooked trunks and a second row of foliage.
		for (int z = NORTH - 1; z <= SOUTH + 1; z++) {
			edge(scene, LEFT - 1, -1, z, 0);
			edge(scene, RIGHT + 1, 1, z, 3);
		}
		for (int x = LEFT - 2; x <= RIGHT + 2; x++) {
			for (int h = 1; h <= 13; h++) {
				leaf(scene, x, h, NORTH - 2);
				leaf(scene, x, h, SOUTH + 2);
				if (h == 1 || h == 3 || (x + h) % 4 < 2) {
					leaf(scene, x, h, NORTH - 1);
					leaf(scene, x, h, SOUTH + 1);
				}
			}
		}

		// Close crown seams over the route, with a higher vault beside the guide and
		// over the arrival. The leaf clusters from individual branches show through below.
		for (int z = NORTH - 1; z <= SOUTH + 1; z++) {
			for (int x = LEFT - 1; x <= RIGHT + 1; x++) {
				int h = 8 + (z > -7 && z < -2 ? 2 : 0) + Math.floorMod(x * 2 + z, 3);
				leaf(scene, x, h, z);
				leaf(scene, x, h + 1, z);
				leaf(scene, x, 13, z);
				leaf(scene, x, 14, z);
			}
		}

		// Keep the floor, portal and lamps on their original squares. The right
		// lamp opens into a one-block recess capped by foliage.
		for (int y = FLOOR + 1; y <= FLOOR + 5; y++) {
			put(scene, new BlockPos(-2, y, NORTH), Part.LOG_Y);
			put(scene, new BlockPos(2, y, NORTH), Part.LOG_Y);
		}
		for (int x = -2; x <= 2; x++) put(scene, new BlockPos(x, FLOOR + 5, NORTH), Part.LOG_Y);
		put(scene, new BlockPos(-3, FLOOR + 1, NORTH + 1), Part.LANTERN);
		put(scene, new BlockPos(3, FLOOR + 1, NORTH + 1), Part.LANTERN);
		put(scene, new BlockPos(-3, FLOOR, NORTH + 1), Part.ROCK);
		for (BlockPos pos : BlockPos.betweenClosed(GATE_MIN, GATE_MAX)) put(scene, pos.immutable(), Part.GATE);
		return scene;
	}

	private static void edge(Map<BlockPos, Part> scene, int x, int out, int z, int phase) {
		int rhythm = Math.floorMod(z + phase, 8);
		leaf(scene, x, 1, z);
		if (rhythm != 2 && rhythm != 3 || z == NORTH + 1) leaf(scene, x, 2, z);
		if (rhythm != 5 && rhythm != 6) leaf(scene, x, 3, z);
		for (int h = 1; h <= 14; h++) {
			leaf(scene, x + out, h, z);
			if (h < 9 || Math.floorMod(z - h, 4) != 0) leaf(scene, x + 2*out, h, z);
			if (h >= 4 && (rhythm + h / 2) % 5 <= 1) leaf(scene, x, h, z);
		}
		if (rhythm == 0 || rhythm == 7) {
			int h = 5 + (phase == 0 ? 1 : 0);
			leaf(scene, x, h, z);
			leaf(scene, x - out, h + 1, z);
			leaf(scene, x - out, h + 2, z);
			leaf(scene, x, h + 2, z);
		}
	}

	private static void plant(Map<BlockPos, Part> scene, Tree tree) {
		BlockPos base = new BlockPos(tree.x, FLOOR, tree.z);
		put(scene, base.below(2), Part.ROCK);
		put(scene, base.below(), Part.ROCK);
		put(scene, base, Part.SOIL);
		for (Direction root : new Direction[]{tree.lean, tree.fork.getOpposite()}) {
			BlockPos tip = base.relative(root);
			if (outsideLane(tip)) {
				put(scene, tip.below(2), Part.ROCK);
				put(scene, tip.below(), Part.ROCK);
				put(scene, tip, root.getAxis() == Direction.Axis.X ? Part.LOG_X : Part.LOG_Z);
			}
		}

		BlockPos trunk = base;
		for (int h = 1; h <= tree.height; h++) {
			trunk = trunk.above();
			put(scene, trunk, Part.LOG_Y);
			if (h == 3 || h == tree.height - 2) {
				BlockPos bend = trunk.relative(tree.lean);
				if (safeBranch(bend)) {
					put(scene, bend, tree.lean.getAxis() == Direction.Axis.X ? Part.LOG_X : Part.LOG_Z);
					trunk = bend;
				}
			}
			if (h == tree.height / 2 + 1) limb(scene, trunk, tree.fork, 4);
			if (h == tree.height - 2) limb(scene, trunk, tree.lean.getOpposite(), 3);
			if (h == tree.height - 1) limb(scene, trunk, tree.fork.getOpposite(), 3);
		}
		crown(scene, trunk.above(), 2);
	}

	private static void limb(Map<BlockPos, Part> scene, BlockPos start, Direction direction, int length) {
		BlockPos tip = start;
		for (int distance = 0; distance < length; distance++) {
			BlockPos step = tip.relative(direction);
			if (!safeBranch(step)) break;
			put(scene, step, direction.getAxis() == Direction.Axis.X ? Part.LOG_X : Part.LOG_Z);
			tip = step;
			if (distance % 2 == 0) {
				tip = tip.above();
				put(scene, tip, Part.LOG_Y);
			}
		}
		crown(scene, tip.above(), 2);
	}

	private static void crown(Map<BlockPos, Part> scene, BlockPos center, int radius) {
		for (int y = -2; y <= 1; y++) {
			int reach = y == 1 || y == -2 ? radius - 1 : radius;
			for (int x = -reach; x <= reach; x++) {
				for (int z = -reach; z <= reach; z++) {
					if (Math.abs(x) + Math.abs(z) <= reach + 1)
						leaf(scene, center.getX() + x, center.getY() - FLOOR + y, center.getZ() + z);
				}
			}
		}
	}

	private static boolean outsideLane(BlockPos pos) {
		return pos.getX() < LEFT || pos.getX() > RIGHT || pos.getZ() < NORTH || pos.getZ() > SOUTH;
	}

	private static boolean safeBranch(BlockPos pos) {
		return (outsideLane(pos) || pos.getY() >= FLOOR + 5) && withinBounds(pos);
	}

	private static boolean withinBounds(BlockPos pos) {
		return pos.getX() >= MIN.getX() && pos.getX() <= MAX.getX()
			&& pos.getY() >= MIN.getY() && pos.getY() <= MAX.getY()
			&& pos.getZ() >= MIN.getZ() && pos.getZ() <= MAX.getZ();
	}

	private static void leaf(Map<BlockPos, Part> scene, int x, int height, int z) {
		BlockPos pos = new BlockPos(x, FLOOR + height, z);
		if (safeBranch(pos)) scene.putIfAbsent(pos, Part.LEAF);
	}

	private static void put(Map<BlockPos, Part> scene, BlockPos pos, Part part) {
		if (!withinBounds(pos)) throw new IllegalArgumentException("Vestibule block outside ticketed footprint: " + pos);
		scene.put(pos, part);
	}

	static void place(ServerLevel level) {
		for (var entry : PLAN.entrySet()) {
			if (level.getBlockState(entry.getKey()).isAir()) {
				level.setBlock(entry.getKey(), state(entry.getValue()), Block.UPDATE_CLIENTS);
			}
		}
	}

	static boolean intersectsAddition(double x, double y, double z) {
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(x - 0.31, y, z - 0.31),
				BlockPos.containing(x + 0.31, y + 1.81, z + 0.31))) {
			if (PLAN.containsKey(pos) && original(pos).isAir()) return true;
		}
		return false;
	}

	static boolean matchesOriginal(ServerLevel level) {
		return matchesOriginal(level::getBlockState);
	}

	static boolean matchesOriginal(java.util.function.Function<BlockPos, BlockState> blocks) {
		for (BlockPos pos : BlockPos.betweenClosed(MIN, MAX)) {
			if (blocks.apply(pos) != original(pos)) return false;
		}
		return true;
	}

	/** Exact previous fallback signature, including its floor, frame, lights and portal orientation. */
	static BlockState original(BlockPos pos) {
		int x = pos.getX(), y = pos.getY() - FLOOR, z = pos.getZ();
		if (z == NORTH) {
			if ((Math.abs(x) == 2 && y >= 1 && y <= 5) || (Math.abs(x) <= 2 && y == 5)) return state(Part.LOG_Y);
			if (Math.abs(x) <= 1 && y >= 1 && y <= 4) return state(Part.GATE);
		}
		if (Math.abs(x) == 3 && y == 1 && z == NORTH + 1) return state(Part.LANTERN);
		if (y >= -3 && y <= 0 && x >= -9 && x <= 9 && z >= -10 && z <= 9 && x*x + z*z <= 100)
			return state(y == 0 && Math.abs(x) <= 1 ? Part.PATH : Part.ROCK);
		return Blocks.AIR.defaultBlockState();
	}

	static BlockState state(Part part) {
		return switch (part) {
			case ROCK -> ModBlocks.ASPHODELITE_ROCK.defaultBlockState();
			case PATH -> ModBlocks.ASHEN_PATH.defaultBlockState();
			case SOIL -> ModBlocks.ASHEN_SOIL.defaultBlockState();
			case LEAF -> ModBlocks.PALE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
			case LOG_X -> ModBlocks.PALE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
			case LOG_Y -> ModBlocks.PALE_LOG.defaultBlockState();
			case LOG_Z -> ModBlocks.PALE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
			case LANTERN -> ModBlocks.ETERNAL_LANTERN.defaultBlockState();
			case GATE -> ModBlocks.VESTIBULE_GATE.defaultBlockState().setValue(VestibuleGateBlock.AXIS, Direction.Axis.X);
		};
	}
}
