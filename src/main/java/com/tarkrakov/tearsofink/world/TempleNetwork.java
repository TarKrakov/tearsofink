package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.ruins.RuinStructure;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable version-two plan, computed from the same raw noise and road profiles as chunk generation. */
public final class TempleNetwork {
	public static final int ROAD_BUFFER = 24;
	// ruin_temple.nbt, pivot (11, 0, 16). Its south doorway sits on the last row.
	public static final int TEMPLATE_WIDTH = 22;
	public static final int TEMPLATE_LENGTH = 33;
	private static final int TERRAIN_ADAPTATION = 12;
	private static final int SITE_GAP = 130;
	private static final int[] OFFSETS = {48, 52, 56, 60, 64, 68, 72, 76};
	private static final int[] FOREST_MIN = {400, 660, 960};
	private static final int[] FOREST_MAX = {650, 940, 1270};
	private static final int OUTPOST_GAP = 72;
	private static final int COLUMN_BUDGET = 1440;
	private static final int[] PLATFORM_SHIFTS = {0, -8, 8, -16, 16};

	public record Inputs(RoadTerrain terrain, RegistryAccess registries, StructureTemplateManager templates, long seed) {}

	public record Temple(int x, int y, int z, int road, double along, Rotation rotation, boolean starter) {
		long chunk() {
			return ChunkPos.asLong(x >> 4, z >> 4);
		}
	}

	public record PathBlock(BlockPos floor, Direction up, boolean lantern) {}

	private record Connection(BlockPos edge, Direction towardRoad) {}

	private final List<Temple> temples;
	private final Map<Long, Temple> origins;
	private final Map<Long, List<PathBlock>> paths;

	private TempleNetwork(List<Temple> temples, List<PathBlock> walks) {
		this.temples = List.copyOf(temples);
		Map<Long, Temple> origins = new HashMap<>();
		for (Temple temple : temples) origins.put(temple.chunk(), temple);
		this.origins = Map.copyOf(origins);
		Map<Long, List<PathBlock>> paths = new HashMap<>();
		for (PathBlock block : walks) {
			BlockPos pos = block.floor();
			paths.computeIfAbsent(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), key -> new ArrayList<>()).add(block);
		}
		paths.replaceAll((key, blocks) -> List.copyOf(blocks));
		this.paths = Map.copyOf(paths);
	}

	public List<Temple> temples() {
		return temples;
	}

	public List<int[]> points() {
		return temples.stream().map(t -> new int[]{t.x(), t.z()}).toList();
	}

	public Temple at(ChunkPos chunk) {
		return origins.get(chunk.toLong());
	}

	public List<PathBlock> path(ChunkPos chunk) {
		return paths.getOrDefault(chunk.toLong(), List.of());
	}

	/** Include the approach as well as the temple when reserving space from other structures. */
	public boolean overlapsPath(int x, int z, int margin) {
		for (int cx = (x - margin) >> 4; cx <= (x + margin) >> 4; cx++) {
			for (int cz = (z - margin) >> 4; cz <= (z + margin) >> 4; cz++) {
				for (PathBlock block : paths.getOrDefault(ChunkPos.asLong(cx, cz), List.of())) {
					if (Math.abs(block.floor().getX() - x) <= margin && Math.abs(block.floor().getZ() - z) <= margin) return true;
				}
			}
		}
		return false;
	}

	static TempleNetwork plan(InkLandsLayout layout, Inputs inputs) {
		return new Planner(layout, inputs).plan();
	}

	private static final class Planner {
		private final InkLandsLayout layout;
		private final RoadTerrain terrain;
		private final Inputs inputs;
		private final List<Temple> temples = new ArrayList<>();
		private final List<PathBlock> paths = new ArrayList<>();
		private final Map<Long, RoadTerrain.Sample> samples = new HashMap<>();
		private final Map<String, Integer> failures = new java.util.TreeMap<>();
		private int slotColumnLimit;

		private final Map<Long, Optional<BoundingBox>> scatteredBounds = new HashMap<>();
		private final Map<Long, Optional<BoundingBox>> scatteredStarts = new HashMap<>();
		private final RandomSpreadStructurePlacement scatteredPlacement;
		private final RuinStructure scatteredRuin;

		Planner(InkLandsLayout layout, Inputs inputs) {
			this.layout = layout;
			this.inputs = inputs;
			this.terrain = inputs.terrain();
			var set = inputs.registries().registryOrThrow(Registries.STRUCTURE_SET).get(TearsOfInk.id("ruin_scattered"));
			if (set != null && set.placement() instanceof RandomSpreadStructurePlacement spread
					&& set.structures().size() == 1 && set.structures().getFirst().structure().value() instanceof RuinStructure ruin) {
				scatteredPlacement = spread;
				scatteredRuin = ruin;
			} else {
				scatteredPlacement = null;
				scatteredRuin = null;
			}
		}

		TempleNetwork plan() {
			var start = Ruins.start(layout);
			int startRoad = Ruins.startRoad(layout);
			temples.add(new Temple((int) start[0], 0, (int) start[1], startRoad, 0, Rotation.NONE, true));
			var size = inputs.templates().getOrCreate(Ruins.TEMPLE).getSize();
			if (size.getX() != TEMPLATE_WIDTH || size.getZ() != TEMPLATE_LENGTH) {
				TearsOfInk.LOGGER.error("D-060 seed {}: unexpected temple template dimensions {}", inputs.seed(), size);
				return new TempleNetwork(temples, paths);
			}
			for (int road = 0; road < InkLandsLayout.ROADS; road++) {
				if (road != startRoad && !roadside(road, false)) missing("external", road);
			}
			for (int road = 0; road < InkLandsLayout.ROADS; road++) {
				if (!roadside(road, true)) missing("deep", road);
			}
			Set<Integer> sectors = new HashSet<>();
			for (int band = 0; band < FOREST_MIN.length; band++) {
				if (!forest(band, sectors)) missing("forest", band);
			}
			return new TempleNetwork(temples, paths);
		}

		private void missing(String kind, int slot) {
			TearsOfInk.LOGGER.error("D-060 seed {}: no site for {} slot {}. Candidate rejections: {}",
					inputs.seed(), kind, slot, failures);
		}

		private boolean roadside(int road, boolean deep) {
			failures.clear();
			slotColumnLimit = samples.size() + COLUMN_BUDGET;
			RandomSource random = RandomSource.create(layout.seed() ^ (0x74656d706c65L + road * 31L + (deep ? 337 : 0)));
			int side = random.nextBoolean() ? 1 : -1;
			int shift = random.nextInt(4);
			List<Integer> preferred = new ArrayList<>();
			List<Integer> fallback = new ArrayList<>();
			int low = deep ? 850 : 250;
			int high = deep ? 1200 : 500;
			for (int along = (deep ? 750 : 1480) + shift; along <= (deep ? 1510 : 2070); along += 4) {
				double[] axis = layout.roadPoint(road, along);
				double depth = layout.depthInside(Mth.floor(axis[0]), Mth.floor(axis[1]));
				if (outsideOutposts(along) && depth >= low - 65 && depth <= (deep ? 1350 : high) + 65) {
					(depth >= low && depth <= high ? preferred : fallback).add(along);
				}
			}
			shuffle(preferred, random);
			shuffle(fallback, random);
			// An existing road lantern reveals the first marker through the fog.
			preferred.sort(java.util.Comparator.comparingInt(Planner::lanternPriority));
			fallback.sort(java.util.Comparator.comparingInt(Planner::lanternPriority));
			for (List<Integer> places : List.of(preferred, fallback)) {
				int maxDepth = places == preferred ? high : (deep ? 1350 : high);
				for (int along : places) {
					layout.roadHeight(road, along, terrain);
					if (layout.roadBridge(road, along - 3) || layout.roadBridge(road, along + 3)) {
						reject("bridge at connection");
						continue;
					}
					for (int sideSign : new int[]{side, -side}) {
						if (samples.size() >= slotColumnLimit) return reject("column budget");
						Connection connection = findConnection(road, along, sideSign);
						if (connection == null) continue;
						for (int alignment : PLATFORM_SHIFTS) {
							int siteAlong = along + alignment;
							if (!outsideOutposts(siteAlong)) continue;
							double[] axis = layout.roadPoint(road, siteAlong);
							double[] dir = layout.roadDirection(road, siteAlong);
							for (int offset : OFFSETS) {
								if (samples.size() >= slotColumnLimit) return reject("column budget");
								if (roadsideAt(road, siteAlong, axis, dir, sideSign * offset, low, maxDepth, connection)) return true;
							}
						}
					}
				}
			}
			return false;
		}

		private static int lanternPriority(int along) {
			int distance = Math.floorMod(along, InkLandsLayout.ROAD_LANTERN_SPACING);
			distance = Math.min(distance, InkLandsLayout.ROAD_LANTERN_SPACING - distance);
			return distance <= 8 ? 0 : distance <= 14 ? 1 : 2;
		}

		private boolean outsideOutposts(double along) {
			for (int i = 0; i < InkLandsLayout.OUTPOSTS.length; i++) {
				if (Math.abs(along - layout.outpostDistance(i)) < OUTPOST_GAP) return false;
			}
			return true;
		}

		private boolean roadsideAt(int road, int along, double[] axis, double[] dir, int offset, int low, int high, Connection connection) {
			int x = (int) Math.round(axis[0] - dir[1] * offset);
			int z = (int) Math.round(axis[1] + dir[0] * offset);
			double depth = layout.depthInside(x, z);
			if (depth < low || depth > high || !clear(x, z)) return reject("depth/biome/nearby site");
			Direction facing = Direction.getNearest(axis[0] - x, 0, axis[1] - z);
			// Reject a projected footprint quickly before probing the real curved road.
			int hx = facing.getAxis() == Direction.Axis.X ? TEMPLATE_LENGTH / 2 : TEMPLATE_WIDTH / 2;
			int hz = facing.getAxis() == Direction.Axis.Z ? TEMPLATE_LENGTH / 2 : TEMPLATE_WIDTH / 2;
			double nearest = Math.abs(offset) - Math.abs(dir[1]) * (hx + TERRAIN_ADAPTATION)
					- Math.abs(dir[0]) * (hz + TERRAIN_ADAPTATION);
			if (nearest < ROAD_BUFFER - 4) return reject("projected road corridor");
			Rotation rotation = facingRotation(facing);
			int y = platform(x, z, facing, true);
			if (y == Integer.MIN_VALUE) return false;
			Temple temple = new Temple(x, y, z, road, along, rotation, false);
			List<PathBlock> path = approach(temple, facing, connection);
			if (path.isEmpty() || overlapsScattered(temple, path)) return false;
			temples.add(temple);
			paths.addAll(path);
			return true;
		}

		private boolean forest(int band, Set<Integer> usedSectors) {
			failures.clear();
			slotColumnLimit = samples.size() + COLUMN_BUDGET;
			RandomSource random = RandomSource.create(layout.seed() ^ (0x666f72657374L + band * 31L));
			List<BlockPos> candidates = new ArrayList<>();
			for (int sector = 0; sector < InkLandsLayout.ROADS; sector++) {
				if (usedSectors.contains(sector)) continue;
				for (int attempt = 0; attempt < 48; attempt++) {
					int r = InkLandsLayout.RADIUS - Mth.nextInt(random, FOREST_MIN[band], FOREST_MAX[band]);
					double[] left = layout.roadPoint(sector, r);
					double[] right = layout.roadPoint((sector + 1) % InkLandsLayout.ROADS, r);
					double from = Math.atan2(left[1], left[0]), to = Math.atan2(right[1], right[0]);
					double gap = Mth.positiveModulo(to - from, Math.PI * 2);
					double a = from + gap * Mth.lerp(random.nextDouble(), 0.29, 0.71);
					int x = Mth.floor(Math.cos(a) * r), z = Mth.floor(Math.sin(a) * r);
					double depth = layout.depthInside(x, z);
					if (depth >= FOREST_MIN[band] && depth <= FOREST_MAX[band]
							&& layout.roadNear(x, z, 135) == null && clear(x, z)) {
						candidates.add(new BlockPos(x, sector, z));
					}
				}
			}
			candidates.sort((a, b) -> Double.compare(space(b), space(a)));
			for (BlockPos at : candidates) {
				if (samples.size() >= slotColumnLimit) return reject("column budget");
				Rotation rotation = Rotation.getRandom(random);
				int y = platform(at.getX(), at.getZ(), rotation.rotate(Direction.SOUTH), false);
				if (y == Integer.MIN_VALUE) continue;
				Temple temple = new Temple(at.getX(), y, at.getZ(), -1, 0, rotation, false);
				if (overlapsScattered(temple, List.of())) continue;
				temples.add(temple);
				usedSectors.add(at.getY());
				return true;
			}
			return false;
		}

		private boolean overlapsScattered(Temple temple, List<PathBlock> path) {
			if (scatteredPlacement == null) {
				reject("missing scattered ruin placement");
				return true;
			}
			Direction facing = temple.rotation().rotate(Direction.SOUTH);
			int hx = (facing.getAxis() == Direction.Axis.X ? TEMPLATE_LENGTH : TEMPLATE_WIDTH) / 2 + TERRAIN_ADAPTATION;
			int hz = (facing.getAxis() == Direction.Axis.Z ? TEMPLATE_LENGTH : TEMPLATE_WIDTH) / 2 + TERRAIN_ADAPTATION;
			int spacing = scatteredPlacement.spacing();
			// Includes scattered ruins relocated towards a road by the monument algorithm.
			int cx = temple.x() >> 4, cz = temple.z() >> 4;
			for (int rx = Math.floorDiv(cx - 8, spacing); rx <= Math.floorDiv(cx + 8, spacing); rx++) {
				for (int rz = Math.floorDiv(cz - 8, spacing); rz <= Math.floorDiv(cz + 8, spacing); rz++) {
					ChunkPos origin = scatteredPlacement.getPotentialStructureChunk(inputs.seed(), rx * spacing, rz * spacing);
					var prediction = scatteredBounds.computeIfAbsent(origin.toLong(), key ->
							Optional.ofNullable(scatteredRuin.scatteredFootprint(scatteredContext(origin))));
					if (prediction.isEmpty() || !intersects(prediction.get(), temple, hx, hz, path)) continue;
					var actual = scatteredStarts.computeIfAbsent(origin.toLong(), key -> {
						var context = scatteredContext(origin);
						var start = scatteredRuin.generate(context.registryAccess(), context.chunkGenerator(), context.biomeSource(),
								context.randomState(), context.structureTemplateManager(), context.seed(), origin, 0,
								context.heightAccessor(), scatteredRuin.biomes()::contains);
						return start.isValid() ? Optional.of(start.getBoundingBox()) : Optional.empty();
					});
					if (actual.isPresent() && intersects(actual.get(), temple, hx, hz, path)) {
						reject("scattered ruin");
						return true;
					}
				}
			}
			return false;
		}

		private GenerationContext scatteredContext(ChunkPos origin) {
			var generator = terrain.generator();
			return new GenerationContext(inputs.registries(), generator, generator.getBiomeSource(),
					terrain.random(), inputs.templates(), inputs.seed(), origin, terrain.level(), scatteredRuin.biomes()::contains);
		}

		private static boolean intersects(BoundingBox box, Temple temple, int hx, int hz, List<PathBlock> path) {
			if (box.intersects(temple.x() - hx, temple.z() - hz, temple.x() + hx, temple.z() + hz)) return true;
			for (PathBlock column : path) {
				BlockPos pos = column.floor();
				if (box.intersects(pos.getX(), pos.getZ(), pos.getX(), pos.getZ())) return true;
			}
			return false;
		}

		private double space(BlockPos pos) {
			return temples.stream().mapToDouble(t -> pos.distToLowCornerSqr(t.x(), pos.getY(), t.z())).min().orElseThrow();
		}

		private boolean clear(int x, int z) {
			if (layout.pick(x, z, null) != ModBiomes.PALE_FOREST
					|| layout.nearLake(x, z, InkLandsLayout.LAKE_ZONE_RADIUS + InkLandsLayout.LAKE_SLOPE + 30)
					|| layout.nearHerald(x, z, 100)
					|| layout.arenaCenter().distToLowCornerSqr(x, 0, z) < Mth.square(InkLandsLayout.ARENA_VALLEY_RADIUS + 40)) return false;
			for (Temple temple : temples) {
				if (Math.hypot(temple.x() - x, temple.z() - z) < SITE_GAP) return false;
			}
			return true;
		}

		private int platform(int x, int z, Direction facing, boolean roadside) {
			int halfX = facing.getAxis() == Direction.Axis.X ? TEMPLATE_LENGTH / 2 : TEMPLATE_WIDTH / 2;
			int halfZ = facing.getAxis() == Direction.Axis.Z ? TEMPLATE_LENGTH / 2 : TEMPLATE_WIDTH / 2;
			int rx = halfX + TERRAIN_ADAPTATION, rz = halfZ + TERRAIN_ADAPTATION;
			// A 4-block mesh with a conservative 3-block guard covers the expanded rectangle.
			int nx = Mth.positiveCeilDiv(rx * 2, 4), nz = Mth.positiveCeilDiv(rz * 2, 4);
			for (int ix = 0; ix <= nx; ix++) for (int iz = 0; iz <= nz; iz++) {
				int px = x - rx + ix * 2 * rx / nx, pz = z - rz + iz * 2 * rz / nz;
				if (layout.roadNear(px, pz, ROAD_BUFFER + 3 - InkLandsLayout.ROAD_HALF_WIDTH) != null) {
					reject("expanded footprint near road");
					return Integer.MIN_VALUE;
				}
			}
			int[] ys = new int[9];
			int i = 0;
			for (int dx : new int[]{-halfX, 0, halfX}) for (int dz : new int[]{-halfZ, 0, halfZ}) {
				int px = x + dx, pz = z + dz;
				if (!pale(px, pz)) {
					reject("footprint biome");
					return Integer.MIN_VALUE;
				}
				var sample = sample(px, pz);
				if (!dry(sample)) {
					reject("wet footprint");
					return Integer.MIN_VALUE;
				}
				ys[i++] = sample.surface();
			}
			Arrays.sort(ys);
			if (ys[8] - ys[0] > (roadside ? 24 : 16)) {
				reject("steep footprint");
				return Integer.MIN_VALUE;
			}
			var entrance = sample(x + facing.getStepX() * (TEMPLATE_LENGTH / 2), z + facing.getStepZ() * (TEMPLATE_LENGTH / 2));
			return roadside ? Mth.clamp(entrance.surface(), ys[4] - 4, ys[4] + 4) : ys[4];
		}

		/** Walk the unmodified strip from the finished shoulder outward once per side. */
		private Connection findConnection(int road, int along, int side) {
			double[] axis = layout.roadPoint(road, along);
			double[] dir = layout.roadDirection(road, along);
			double nx = -dir[1] * side, nz = dir[0] * side;
			int x = (int) Math.round(axis[0]), z = (int) Math.round(axis[1]);
			int targetX = (int) Math.round(axis[0] + nx * 34), targetZ = (int) Math.round(axis[1] + nz * 34);
			int lastY = (int) Math.round(layout.roadHeight(road, along, terrain));
			boolean offShoulder = false;
			boolean firstGround = true;
			while (x != targetX || z != targetZ) {
				if (Math.abs(x - targetX) >= Math.abs(z - targetZ) && x != targetX) x += Integer.signum(targetX - x);
				else z += Integer.signum(targetZ - z);
				if (!offShoulder) {
					if (layout.roadNear(x, z, InkLandsLayout.ROAD_SHOULDER) != null) continue;
					offShoulder = true;
				}
				var here = sample(x, z);
				if (!dry(here) || !pale(x, z)) {
					reject("wet or non-forest connection");
					return null;
				}
				int difference = here.surface() - lastY;
				int lowestStep = firstGround ? -2 : -1;
				if (difference > 1 || difference < lowestStep) {
					reject("natural slope at connection");
					return null;
				}
				boolean outside = !protectedRoad(x, z);
				if (outside) {
					return new Connection(new BlockPos(x, here.surface(), z), Direction.getNearest(-nx, 0, -nz));
				}
				firstGround = false;
				lastY = here.surface();
			}
			reject("no protected edge connection");
			return null;
		}

		private List<PathBlock> approach(Temple temple, Direction facing, Connection connection) {
			int x = temple.x() + facing.getStepX() * (TEMPLATE_LENGTH / 2),
					z = temple.z() + facing.getStepZ() * (TEMPLATE_LENGTH / 2);
			Direction edge = connection.towardRoad().getClockWise();
			int targetX = connection.edge().getX(), targetZ = connection.edge().getZ();
			int naturalEdge = connection.edge().getY();
			List<BlockPos> walk = new ArrayList<>();
			for (int steps = 0; steps < 104 && (x != targetX || z != targetZ); steps++) {
				if (steps < 3) {
					x += facing.getStepX();
					z += facing.getStepZ();
				} else if (Math.abs(x - targetX) >= Math.abs(z - targetZ) && x != targetX) {
					x += Integer.signum(targetX - x);
				} else {
					z += Integer.signum(targetZ - z);
				}
				if (protectedRoad(x, z) || !pale(x, z)) return rejectPath("approach crosses reservation");
				var here = sample(x, z);
				if (!dry(here)) return rejectPath("wet approach");
				walk.add(new BlockPos(x, here.surface(), z));
			}
			if (walk.size() < 5 || x != targetX || z != targetZ) return rejectPath("no join");
			List<PathBlock> result = new ArrayList<>();
			Set<Long> occupied = new HashSet<>();
			int prior = temple.y();
			for (int i = 0; i < walk.size(); i++) {
				BlockPos pos = walk.get(i);
				int remaining = walk.size() - i - 1;
				int desired = Mth.clamp(pos.getY(), naturalEdge - remaining, naturalEdge + remaining);
				int target = Mth.clamp(desired, prior - 1, prior + 1);
				if (Math.abs(target - naturalEdge) > remaining || Math.abs(target - pos.getY()) > 3) return rejectPath("graded path");
				Direction up = target > prior ? Direction.getNearest(pos.getX() - (i == 0 ? temple.x() : walk.get(i - 1).getX()),
						0, pos.getZ() - (i == 0 ? temple.z() : walk.get(i - 1).getZ())) : null;
				BlockPos floor = pos.atY(target);
				result.add(new PathBlock(floor, up, false));
				occupied.add(ChunkPos.asLong(pos.getX(), pos.getZ()));
				prior = target;
			}
			for (int i = 0; i < walk.size(); i++) {
				PathBlock center = result.get(i);
				BlockPos side = center.floor().relative(edge);
				if (!protectedRoad(side.getX(), side.getZ()) && pale(side.getX(), side.getZ())
						&& !occupied.contains(ChunkPos.asLong(side.getX(), side.getZ()))) {
					var ground = sample(side.getX(), side.getZ());
					if (dry(ground) && Math.abs(ground.surface() - side.getY()) <= 3) {
						occupied.add(ChunkPos.asLong(side.getX(), side.getZ()));
						result.add(new PathBlock(side, center.up(), false));
					}
				}
			}
			boolean signposted = false;
			for (int i : new int[]{walk.size() - 2, walk.size() - 3, 2}) {
				BlockPos lamp = result.get(i).floor().relative(edge.getOpposite());
				if (!protectedRoad(lamp.getX(), lamp.getZ()) && pale(lamp.getX(), lamp.getZ())
						&& dry(sample(lamp.getX(), lamp.getZ())) && Math.abs(sample(lamp.getX(), lamp.getZ()).surface() - lamp.getY()) <= 3
						&& occupied.add(ChunkPos.asLong(lamp.getX(), lamp.getZ()))) {
					result.add(new PathBlock(lamp, null, true));
					signposted |= i >= walk.size() - 3;
				}
			}
			return signposted ? result : rejectPath("no marker at approach");
		}

		private boolean reject(String reason) {
			failures.merge(reason, 1, Integer::sum);
			return false;
		}

		private List<PathBlock> rejectPath(String reason) {
			reject(reason);
			return List.of();
		}

		private RoadTerrain.Sample sample(int x, int z) {
			return samples.computeIfAbsent(ChunkPos.asLong(x, z), key -> terrain.sample(x, z));
		}

		private boolean dry(RoadTerrain.Sample sample) {
			return !sample.water() && sample.surface() >= terrain.generator().getSeaLevel();
		}

		private boolean pale(int x, int z) {
			return layout.pick(x, z, null) == ModBiomes.PALE_FOREST
					&& layout.pick(x & ~3, z & ~3, null) == ModBiomes.PALE_FOREST;
		}

		private boolean protectedRoad(int x, int z) {
			return layout.roadNear(x, z, ROAD_BUFFER - InkLandsLayout.ROAD_HALF_WIDTH) != null;
		}
	}

	private static Rotation facingRotation(Direction facing) {
		return switch (facing) {
			case WEST -> Rotation.CLOCKWISE_90;
			case NORTH -> Rotation.CLOCKWISE_180;
			case EAST -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	private static <T> void shuffle(List<T> list, RandomSource random) {
		for (int i = list.size() - 1; i > 0; i--) {
			java.util.Collections.swap(list, i, random.nextInt(i + 1));
		}
	}
}
