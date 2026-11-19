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
	// Last-resort working band for V3 only: remains distinct from the deep band (850+).
	static final int OUTER_RECOVERY_MIN = 200, OUTER_RECOVERY_MAX = 650;
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

	TempleNetwork(List<Temple> temples, List<PathBlock> walks) {
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

	List<PathBlock> allPathBlocks() {
		return paths.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.flatMap(entry -> entry.getValue().stream()).toList();
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
				if (layout.templeLayoutVersion() == 3)
					throw new IllegalStateException("Unexpected temple template dimensions: " + size);
				TearsOfInk.LOGGER.error("D-060 seed {}: unexpected temple template dimensions {}", inputs.seed(), size);
				return new TempleNetwork(temples, paths);
			}
			boolean[] outer = new boolean[InkLandsLayout.ROADS], inner = new boolean[InkLandsLayout.ROADS];
			outer[startRoad] = true;
			for (int road = 0; road < InkLandsLayout.ROADS; road++) {
				if (road != startRoad) {
					outer[road] = roadside(road, false);
					if (!outer[road] && layout.templeLayoutVersion() == 2) missing("external", road);
				}
			}
			for (int road = 0; road < InkLandsLayout.ROADS; road++) {
				inner[road] = roadside(road, true);
				if (!inner[road] && layout.templeLayoutVersion() == 2) missing("deep", road);
			}
			// Layout 2 remains reproducible. Only newly opted-in layout 3 gets the extra pass.
			if (layout.templeLayoutVersion() == 3) {
				for (int road = 0; road < InkLandsLayout.ROADS; road++) {
					if (!outer[road]) outer[road] = roadsideFallback(road, false);
					if (!inner[road]) inner[road] = roadsideFallback(road, true);
				}
				for (int road = 0; road < InkLandsLayout.ROADS; road++) {
					if (!outer[road]) outer[road] = roadsideGraphFallback(road, false, false);
					if (!inner[road]) inner[road] = roadsideGraphFallback(road, true, false);
				}
				for (int road = 0; road < InkLandsLayout.ROADS; road++) {
					if (!outer[road]) outer[road] = roadsideGraphFallback(road, false, true);
				}
				// Search the surviving buildable platforms instead of searching expensive
				// exits first. Earlier successful placements are never moved.
				for (int road = 0; road < InkLandsLayout.ROADS; road++) {
					if (!outer[road] && !roadsideSiteFallback(road, false)) missing("external", road);
					if (!inner[road] && !roadsideSiteFallback(road, true)) missing("deep", road);
				}
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

		/** Bounded second pass: spread terrain probes over the road, then try skewed natural exits.
		 * The footprint, depth bands and protected corridor checks are identical to the first pass.
		 */
		private boolean roadsideFallback(int road, boolean deep) {
			failures.clear();
			int budgetStart = samples.size();
			int low = deep ? 850 : 250, high = deep ? 1350 : 500;
			List<Integer> places = new ArrayList<>();
			for (int along = 650; along <= 2150; along += 6) {
				double[] axis = layout.roadPoint(road, along);
				double depth = layout.depthInside(Mth.floor(axis[0]), Mth.floor(axis[1]));
				if (outsideOutposts(along) && depth >= low - 90 && depth <= high + 90) places.add(along);
			}
			RandomSource random = RandomSource.create(layout.seed() ^ (0x7265747279L + 31L * road + (deep ? 337 : 0)));
			shuffle(places, random);
			int firstSide = random.nextBoolean() ? 1 : -1;
			// Reserve half the probe budget for a second, skewed search.
			for (int pass = 0; pass < 2; pass++) {
				slotColumnLimit = budgetStart + (pass == 0 ? 1200 : 2400);
				int[] skews = pass == 0 ? new int[]{0} : new int[]{-12, 12};
				int[] offsets = pass == 0 ? new int[]{64} : new int[]{76, 68, 56};
				search:
				for (int along : places) {
					layout.roadHeight(road, along, terrain);
					if (layout.roadBridge(road, along - 3) || layout.roadBridge(road, along + 3)) continue;
					for (int side : new int[]{firstSide, -firstSide}) {
						for (int skew : skews) {
							if (samples.size() >= slotColumnLimit) { reject("fallback column budget"); break search; }
							Connection connection = terrainConnection(road, along, side, skew);
							if (connection == null) continue;
							int siteAlong = along + skew;
							if (!outsideOutposts(siteAlong)) continue;
							double[] axis = layout.roadPoint(road, siteAlong);
							double[] dir = layout.roadDirection(road, siteAlong);
							for (int offset : offsets) {
								if (samples.size() >= slotColumnLimit) { reject("fallback column budget"); break search; }
								if (roadsideAt(road, siteAlong, axis, dir, side * offset, low, high, connection)) return true;
							}
						}
					}
				}
			}
			return false;
		}

		/** V3 fallback only: check the predicted finished shoulder and cut slope. */
		private Connection terrainConnection(int road, int along, int side, int skew) {
			for (int delta = -15; delta <= 15; delta++) {
				if (layout.roadBridge(road, along + delta)) {
					reject("bridge in fallback exit");
					return null;
				}
			}
			double[] axis = layout.roadPoint(road, along), dir = layout.roadDirection(road, along);
			double nx = -dir[1] * side, nz = dir[0] * side;
			int tx = (int) Math.round(axis[0] + nx * 34 + dir[0] * skew);
			int tz = (int) Math.round(axis[1] + nz * 34 + dir[1] * skew);
			Set<Long> lanterns = new HashSet<>();
			int k = Math.floorDiv(along, InkLandsLayout.ROAD_LANTERN_SPACING);
			for (int j = k - 1; j <= k + 2; j++) {
				int[] post = layout.roadLanternPost(road, j);
				if (post != null) lanterns.add(ChunkPos.asLong(post[0], post[1]));
			}
			for (int order = 0; order < 2; order++) {
				int x = (int) Math.round(axis[0]), z = (int) Math.round(axis[1]);
				int lastY = (int) Math.round(layout.roadHeight(road, along, terrain));
				while (x != tx || z != tz) {
					int ax = Math.abs(x - tx), az = Math.abs(z - tz);
					if (ax != 0 && (az == 0 || (order == 0 ? ax >= az : ax > az))) x += Integer.signum(tx - x);
					else z += Integer.signum(tz - z);
					if (lanterns.contains(ChunkPos.asLong(x, z)) || !pale(x, z)) break;
					var hit = layout.roadNear(x, z, InkLandsLayout.ROAD_SHOULDER + InkRoadsFeature.ACCESS_CUT_WIDTH);
					if (hit != null && ((int) hit[0] != road || !outsideOutposts(hit[1]) || layout.roadBridge(road, hit[1]))) break;
					var natural = sample(x, z);
					if (!dry(natural)) break;
					int y = InkRoadsFeature.plannedAccessSurface(layout, terrain, hit, natural.surface());
					if (Math.abs(y - lastY) > 1) break;
					if (!protectedRoad(x, z))
						return new Connection(new BlockPos(x, y, z), Direction.getNearest(-nx, 0, -nz));
					lastY = y;
				}
			}
			reject("fallback natural slope or obstacle");
			return null;
		}

		/** Last bounded pass: follow the actual natural contour without editing the reserved strip. */
		private boolean roadsideGraphFallback(int road, boolean deep, boolean widerOuter) {
			if (!widerOuter) failures.clear();
			slotColumnLimit = samples.size() + 6000;
			int low = deep ? 850 : widerOuter ? OUTER_RECOVERY_MIN : 250;
			int high = deep ? 1350 : widerOuter ? OUTER_RECOVERY_MAX : 500;
			List<Integer> places = new ArrayList<>();
			// Offset the sampling grid on the recovery pass to inspect fresh exits.
			for (int along = 650 + (widerOuter ? 8 : 0); along <= 2250; along += 16) {
				double[] axis = layout.roadPoint(road, along);
				double depth = layout.depthInside(Mth.floor(axis[0]), Mth.floor(axis[1]));
				if (outsideOutposts(along) && depth >= low - 90 && depth <= high + 90) places.add(along);
			}
			RandomSource random = RandomSource.create(layout.seed() ^ (0x6772617068L + road * 31L + (deep ? 337 : 0) + (widerOuter ? 601 : 0)));
			shuffle(places, random);
			places.sort(java.util.Comparator.comparingInt(Planner::lanternPriority));
			int firstSide = random.nextBoolean() ? 1 : -1;
			for (int along : places) {
				layout.roadHeight(road, along, terrain);
				for (int side : new int[]{firstSide, -firstSide}) {
					if (samples.size() >= slotColumnLimit) return reject("graph column budget");
					for (Connection connection : graphConnections(road, along, side)) {
						var near = layout.roadNear(connection.edge().getX(), connection.edge().getZ(), 40 - InkLandsLayout.ROAD_HALF_WIDTH);
						int exitAlong = near != null && (int) near[0] == road ? (int) Math.round(near[1]) : along;
						for (int shift : new int[]{0, -12, 12, -24, 24}) {
							int siteAlong = exitAlong + shift;
							if (!outsideOutposts(siteAlong)) continue;
							double[] axis = layout.roadPoint(road, siteAlong), dir = layout.roadDirection(road, siteAlong);
							for (int offset : new int[]{64, 76, 56, 68, 72, 60, 52, 48}) {
								if (samples.size() >= slotColumnLimit) return reject("graph column budget");
								if (roadsideAt(road, siteAlong, axis, dir, offset * side, low, high, connection)) {
									TearsOfInk.LOGGER.info("D-060 seed {}: winding exit recovered road {} {} slot (outer recovery band={})",
											inputs.seed(), road, deep ? "deep" : "external", widerOuter);
									return true;
								}
							}
						}
					}
				}
			}
			return false;
		}

		/**
		 * Last resort for still-empty V3 slots: evaluate a finite, distributed grid
		 * of actual temple sites BEFORE computing a natural road exit. This avoids
		 * spending the entire exit budget beside unsuitable terrain or ruins.
		 * No relaxation of biome, footprint, slope, water, or road reservations.
		 */
		private boolean roadsideSiteFallback(int road, boolean deep) {
			failures.clear(); // rejection counts always belong to this slot
			int startSamples = samples.size();
			slotColumnLimit = startSamples + 5000;
			int low = deep ? 850 : OUTER_RECOVERY_MIN;
			int high = deep ? 1350 : OUTER_RECOVERY_MAX;
			RandomSource random = RandomSource.create(layout.seed() ^ (0x7369746566697273L + road * 31L + (deep ? 337 : 0)));
			List<Integer> stations = new ArrayList<>();
			for (int along = 650 + random.nextInt(12); along <= 2250; along += 12) {
				if (outsideOutposts(along)) stations.add(along);
			}
			shuffle(stations, random);
			stations.sort(java.util.Comparator.comparingInt(Planner::lanternPriority));
			int firstSide = random.nextBoolean() ? 1 : -1;
			int viable = 0, exitProbes = 0;
			Set<Long> attempted = new HashSet<>();
			for (int along : stations) {
				layout.roadHeight(road, along, terrain);
				double[] axis = layout.roadPoint(road, along), dir = layout.roadDirection(road, along);
				for (int side : new int[]{firstSide, -firstSide}) {
					List<Temple> sites = new ArrayList<>();
					for (int offset : new int[]{60, 72, 84, 64, 76, 88, 56}) {
						if (samples.size() >= slotColumnLimit || viable >= 40) break;
						int x = (int) Math.round(axis[0] - dir[1] * offset * side);
						int z = (int) Math.round(axis[1] + dir[0] * offset * side);
						if (!attempted.add(ChunkPos.asLong(x,z))) continue;
						double depth = layout.depthInside(x,z);
						if (depth < low || depth > high || !clear(x,z)) { reject("site-first depth/biome/spacing"); continue; }
						Direction facing = Direction.getNearest(axis[0]-x, 0, axis[1]-z);
						int hx = facing.getAxis() == Direction.Axis.X ? TEMPLATE_LENGTH / 2 : TEMPLATE_WIDTH / 2;
						int hz = facing.getAxis() == Direction.Axis.Z ? TEMPLATE_LENGTH / 2 : TEMPLATE_WIDTH / 2;
						double nearest = offset - Math.abs(dir[1]) * (hx + TERRAIN_ADAPTATION)
								- Math.abs(dir[0]) * (hz + TERRAIN_ADAPTATION);
						if (nearest < ROAD_BUFFER - 4) { reject("site-first projected corridor"); continue; }
						int y = platform(x,z,facing,true);
						if (y == Integer.MIN_VALUE) continue;
						Temple temple = new Temple(x,y,z,road,along,facingRotation(facing),false);
						if (overlapsScattered(temple,List.of())) continue;
						sites.add(temple);
						viable++;
					}
					if (sites.isEmpty()) continue;
					// The lane need not be directly opposite the doorway, especially on
					// a bridge approach or at a curved cut. Keep the original 104-step
					// approach cap, but try a different natural exit on the same road.
					for (int shift : new int[]{0,-24,24,-40,40}) {
						if (exitProbes >= 36 || samples.size() >= slotColumnLimit) break;
						int exitAlong = along + shift;
						if (!outsideOutposts(exitAlong) || exitAlong < 650 || exitAlong > 2250) continue;
						exitProbes++;
						for (Connection connection : graphConnections(road,exitAlong,side,600)) {
							for (Temple temple : sites) {
								if (samples.size() >= slotColumnLimit) break;
								Direction facing = temple.rotation().rotate(Direction.SOUTH);
								for (int route = 0; route < 3; route++) {
									if (samples.size() >= slotColumnLimit) break;
									List<PathBlock> path = approach(temple,facing,connection,route);
									if (path.isEmpty() || overlapsExistingApproach(path) || overlapsScattered(temple,path)) continue;
									temples.add(temple);
									paths.addAll(path);
									TearsOfInk.LOGGER.info("D-060 seed {}: site-first recovered road {} {} slot, {} viable sites, {} exit probes, {} new columns",
											inputs.seed(),road,deep ? "deep" : "external",viable,exitProbes,samples.size()-startSamples);
									return true;
								}
							}
						}
					}
					if (viable >= 40 || exitProbes >= 36 || samples.size() >= slotColumnLimit) {
						reject("site-first bounded search limit");
						return false;
					}
				}
			}
			return false;
		}

		private boolean overlapsExistingApproach(List<PathBlock> candidate) {
			Set<Long> occupied = new HashSet<>();
			for (PathBlock p : paths) occupied.add(ChunkPos.asLong(p.floor().getX(),p.floor().getZ()));
			for (PathBlock p : candidate) {
				BlockPos floor = p.floor();
				if (occupied.contains(ChunkPos.asLong(floor.getX(),floor.getZ()))) return rejectOverlap();
				for (Temple t : temples) {
					if (Math.abs(floor.getX()-t.x()) <= TEMPLATE_LENGTH/2 + TERRAIN_ADAPTATION
							&& Math.abs(floor.getZ()-t.z()) <= TEMPLATE_LENGTH/2 + TERRAIN_ADAPTATION) return rejectOverlap();
				}
			}
			return false;
		}

		private boolean rejectOverlap() {
			reject("site-first existing temple/approach");
			return true;
		}

		private record ExitNode(int x, int z, int y, int steps, double priority) {}

		private List<Connection> graphConnections(int road, int along, int side) {
			return graphConnections(road,along,side,300);
		}

		private List<Connection> graphConnections(int road, int along, int side, int expansionLimit) {
			double[] axis = layout.roadPoint(road, along), dir = layout.roadDirection(road, along);
			double nx = -dir[1] * side, nz = dir[0] * side;
			Direction towardRoad = Direction.getNearest(-nx, 0, -nz);
			Set<Long> lanterns = new HashSet<>();
			int k = Math.floorDiv(along, InkLandsLayout.ROAD_LANTERN_SPACING);
			for (int j = k - 2; j <= k + 2; j++) {
				int[] post = layout.roadLanternPost(road, j);
				if (post != null) lanterns.add(ChunkPos.asLong(post[0], post[1]));
			}
			var heights = new HashMap<Long, Integer>();
			var best = new HashMap<Long, Integer>();
			var open = new java.util.PriorityQueue<ExitNode>(java.util.Comparator
					.comparingDouble(ExitNode::priority).thenComparingInt(ExitNode::x).thenComparingInt(ExitNode::z));
			for (int shift : new int[]{0, -6, 6, -12, 12}) {
				double[] p = layout.roadPoint(road, along + shift);
				int x = (int) Math.round(p[0]), z = (int) Math.round(p[1]);
				var hit = layout.roadAt(x, z);
				if (hit == null || (int) hit[0] != road) continue;
				int y = graphSurface(road, x, z, lanterns);
				long key = ChunkPos.asLong(x, z);
				heights.put(key, y);
				if (y == Integer.MIN_VALUE || best.putIfAbsent(key, 0) != null) continue;
				open.add(new ExitNode(x, z, y, 0, 48));
			}
			List<Connection> result = new ArrayList<>();
			int expanded = 0;
			while (!open.isEmpty() && expanded < expansionLimit && samples.size() < slotColumnLimit && result.size() < 3) {
				ExitNode at = open.remove();
				long atKey = ChunkPos.asLong(at.x(), at.z());
				if (best.getOrDefault(atKey, Integer.MAX_VALUE) != at.steps()) continue;
				expanded++;
				if (!protectedRoad(at.x(), at.z())) {
					BlockPos pos = new BlockPos(at.x(), at.y(), at.z());
					if (result.stream().noneMatch(c -> c.edge().distSqr(pos) < 36))
						result.add(new Connection(pos, towardRoad));
					continue;
				}
				if (at.steps() >= 64) continue;
				for (Direction step : Direction.Plane.HORIZONTAL) {
					int x = at.x() + step.getStepX(), z = at.z() + step.getStepZ();
					double dx = x - axis[0], dz = z - axis[1];
					double outward = dx * nx + dz * nz;
					if (outward < -3 || outward > 36 || Math.abs(dx * dir[0] + dz * dir[1]) > 22) continue;
					long key = ChunkPos.asLong(x, z);
					int steps = at.steps()+1;
					if (best.getOrDefault(key, Integer.MAX_VALUE) <= steps) continue;
					int y = heights.computeIfAbsent(key, ignored -> graphSurface(road, x, z, lanterns));
					if (y == Integer.MIN_VALUE || Math.abs(y - at.y()) > 1) continue;
					best.put(key, steps);
					open.add(new ExitNode(x, z, y, steps, steps + 2 * Math.max(0, ROAD_BUFFER - outward)));
				}
			}
			if (result.isEmpty()) reject("no walkable winding exit");
			if (expanded >= expansionLimit) reject("winding exit expansion limit");
			return result;
		}

		private int graphSurface(int road, int x, int z, Set<Long> lanterns) {
			if (lanterns.contains(ChunkPos.asLong(x, z)) || !pale(x, z)) return Integer.MIN_VALUE;
			var zone = layout.roadNear(x, z, ROAD_BUFFER - InkLandsLayout.ROAD_HALF_WIDTH);
			if (zone != null && ((int) zone[0] != road || !outsideOutposts(zone[1]))) return Integer.MIN_VALUE;
			var hit = layout.roadNear(x, z, InkLandsLayout.ROAD_SHOULDER + InkRoadsFeature.ACCESS_CUT_WIDTH);
			if (hit != null && ((int) hit[0] != road || !outsideOutposts(hit[1]))) return Integer.MIN_VALUE;
			boolean bridge = hit != null && layout.roadBridge(road, hit[1]);
			if (bridge && hit[2] <= InkLandsLayout.ROAD_HALF_WIDTH + InkLandsLayout.ROAD_SHOULDER)
				return Integer.MIN_VALUE; // never use bridge decks, rails or shoulders
			// The natural ground beside an ordinary bridge can be traversed.
			// No cut slope is generated for a bridge, and no blocks are modified here.
			if (bridge) hit = null;
			// A paved lane is cleared by the road feature even over wet raw noise.
			// Beyond it, remain conservative and require dry natural ground.
			if (hit != null && hit[2] <= InkLandsLayout.ROAD_HALF_WIDTH)
				return (int) Math.round(layout.roadHeight(road, hit[1], terrain));
			if (samples.size() >= slotColumnLimit && !samples.containsKey(ChunkPos.asLong(x,z))) return Integer.MIN_VALUE;
			var natural = sample(x, z);
			if (!dry(natural)) {
				// The exact shoulder fill case in InkRoadsFeature: when the surface is
				// below the shoulder level, it is replaced by solid rock, even in water.
				if (hit != null && hit[2] <= InkLandsLayout.ROAD_HALF_WIDTH + InkLandsLayout.ROAD_SHOULDER) {
					int shoulder = (int) Math.round(layout.roadHeight(road, hit[1], terrain)) - 1;
					if (natural.surface() < shoulder) return shoulder;
				}
				return Integer.MIN_VALUE;
			}
			return InkRoadsFeature.plannedAccessSurface(layout, terrain, hit, natural.surface());
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
			return findConnection(road, along, side, 0);
		}

		private Connection findConnection(int road, int along, int side, int skew) {
			// A diagonal exit must not enter a bridge anywhere in its connection band.
			// The legacy perpendicular exit keeps its original checks and geometry.
			if (skew != 0) {
				for (int delta = -15; delta <= 15; delta++) {
					if (layout.roadBridge(road, along + delta)) {
						reject("bridge near skewed connection");
						return null;
					}
				}
			}
			double[] axis = layout.roadPoint(road, along);
			double[] dir = layout.roadDirection(road, along);
			double nx = -dir[1] * side, nz = dir[0] * side;
			int x = (int) Math.round(axis[0]), z = (int) Math.round(axis[1]);
			int targetX = (int) Math.round(axis[0] + nx * 34 + dir[0] * skew),
					targetZ = (int) Math.round(axis[1] + nz * 34 + dir[1] * skew);
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
			return approach(temple,facing,connection,0);
		}

		private List<PathBlock> approach(Temple temple, Direction facing, Connection connection, int route) {
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
				} else if (x != targetX && (z == targetZ || route == 1
						|| route == 0 && Math.abs(x - targetX) >= Math.abs(z - targetZ))) {
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
			int[] grade = null;
			if (layout.templeLayoutVersion() == 3) {
				grade = TempleApproachGrade.solve(prior, naturalEdge, walk.stream().mapToInt(BlockPos::getY).toArray());
				if (grade == null) return rejectPath("graded path");
			}
			for (int i = 0; i < walk.size(); i++) {
				BlockPos pos = walk.get(i);
				int remaining = walk.size() - i - 1;
				int desired = Mth.clamp(pos.getY(), naturalEdge - remaining, naturalEdge + remaining);
				int target = grade == null ? Mth.clamp(desired, prior - 1, prior + 1) : grade[i];
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
