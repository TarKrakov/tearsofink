package com.tarkrakov.tearsofink.prologue;

import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.tarkrakov.tearsofink.prologue.VestibuleGrove.*;

/** Verifies the exact fallback block plan used by the server. */
public final class VestibuleGroveTest {
	private static final Map<BlockPos, Part> SCENE = plan();

	public static void main(String[] args) throws IOException {
		for (int z = NORTH; z <= SOUTH; z++) {
			for (int x = LEFT; x <= RIGHT; x++) {
				BlockPos feet = new BlockPos(x, FLOOR + 1, z);
				if (z == NORTH && x == RIGHT) continue; // portal's existing post
				require(SCENE.containsKey(feet.below()), "Missing floor at " + feet);
				require(clear(feet) && clear(feet.above()) && clear(feet.above(2)), "Route obstructed at " + feet);
			}
		}

		int width = GATE_MAX.getX() - GATE_MIN.getX() + 1;
		require(RIGHT - LEFT + 1 == width + 1, "Lane should be one block wider than the opening");
		for (BlockPos gate : BlockPos.betweenClosed(GATE_MIN, GATE_MAX))
			require(SCENE.get(gate) == Part.GATE, "Portal obstructed at " + gate);
		require(SCENE.get(new BlockPos(-3,FLOOR + 1,NORTH + 1)) == Part.LANTERN, "West lamp moved");
		require(SCENE.get(new BlockPos(3,FLOOR + 1,NORTH + 1)) == Part.LANTERN, "East lamp moved");
		require(clear(ARRIVAL) && clear(GUIDE.above(2)), "Spawn clearance");

		// A standing route can step onto blocks or descend, but cannot escape the trees.
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		pending.add(ARRIVAL);
		while (!pending.isEmpty()) {
			BlockPos current = pending.removeFirst();
			if (!visited.add(current)) continue;
			require(current.getX() >= LEFT && current.getX() <= RIGHT
					&& current.getZ() >= NORTH && current.getZ() <= SOUTH, "Open edge at " + current);
			for (BlockPos neighbor : new BlockPos[]{current.north(), current.east(), current.south(), current.west()}) {
				if (clear(neighbor) && clear(neighbor.above())) {
					if (clear(neighbor.below()))
						throw new AssertionError("Walkable drop at " + neighbor);
					pending.add(neighbor);
				} else if (clear(current.above(2)) && !clear(neighbor) && clear(neighbor.above()) && clear(neighbor.above(2))) {
					pending.add(neighbor.above());
				}
			}
		}
		require(visited.contains(GUIDE), "No route to guide");
		require(visited.contains(new BlockPos(0,FLOOR + 1,NORTH)), "No route to portal");
		for (int x : new int[]{LEFT, RIGHT})
			require(visited.contains(new BlockPos(x,FLOOR + 1,GUIDE.getZ())), "No lane past guide");

		// Trace samples from standing and elevated eye level. Each must meet a block
		// surface before reaching the edge of the planned terrain.
		for (int z = NORTH; z <= SOUTH; z++) {
			for (int x = LEFT; x <= RIGHT; x++) {
				if (z == NORTH && x == RIGHT) continue;
				for (double y : new double[]{FLOOR + 2.62, FLOOR + 3.7}) assertEnclosed(x + 0.5, y, z + 0.5);
			}
		}
		if (args.length == 1) {
			try (var out = Files.newBufferedWriter(Path.of(args[0]))) {
				for (var cell : SCENE.entrySet()) {
					var p = cell.getKey();
					out.write(p.getX() + "\t" + p.getY() + "\t" + p.getZ() + "\t" + cell.getValue() + "\n");
				}
			}
		}
		System.out.println("Vestibule fallback: " + SCENE.size() + " blocks, " + visited.size() + " stands, enclosed route and intact gate.");
	}

	private static void assertEnclosed(double x, double y, double z) {
		for (int az = 0; az < 360; az += 10) {
			for (int el = -80; el <= 90; el += 10) {
				double cos = Math.cos(Math.toRadians(el));
				double dx = Math.sin(Math.toRadians(az)) * cos;
				double dz = Math.cos(Math.toRadians(az)) * cos;
				double dy = Math.sin(Math.toRadians(el));
				boolean closed = false;
				for (double t = 0.1; t < 24; t += 0.125) {
					if (!clear(BlockPos.containing(x + dx*t, y + dy*t, z + dz*t))) {
						closed = true;
						break;
					}
				}
				require(closed, "Sightline to the void at " + x + "," + y + "," + z + " angle " + az + "," + el);
			}
		}
	}

	private static boolean clear(BlockPos pos) {
		Part part = SCENE.get(pos);
		return part == null || part == Part.GATE || part == Part.LANTERN;
	}

	private static void require(boolean test, String message) {
		if (!test) throw new AssertionError(message);
	}
}
