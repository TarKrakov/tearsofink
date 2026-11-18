package com.tarkrakov.tearsofink.world.adit;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.ruins.RuinStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.HashMap;
import java.util.Map;

/** Selects starts using only the seed, layout, structure plans and base terrain. */
final class AditSites {
	private static final long NETWORK_SALT = 728619347L;
	private static final int ATTEMPTS_PER_SITE = 96;
	private static final int ROAD_BUFFER = 24;
	// An entrance can be offset from the chunk centre in either direction.
	private static final int MIN_CENTER_GAP = 300 + 2 * 12;
	static final int SURFACE_TOLERANCE = 12;
	static final int BOTTOM_Y = -12;

	private static final Band[] BANDS = {
			new Band(820, 1090, 0),
			new Band(1130, 1480, -1),
			new Band(1440, 1760, 0),
			new Band(1830, 2120, 1)
	};

	record Site(int sector, int band, int surface) {}
	private record Band(int inner, int outer, int roadside) {}

	private AditSites() {}

	static boolean withinSearchArea(ChunkPos chunk) {
		long x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
		long distanceSquared = x * x + z * z;
		return distanceSquared >= Mth.square(BANDS[0].inner() - 12)
				&& distanceSquared <= Mth.square(BANDS[BANDS.length - 1].outer() + 12);
	}

	static Map<Long, Site> plan(InkLandsLayout layout, GenerationContext context) {
		Map<Long, Site> sites = new HashMap<>();
		Map<Long, StructureStart> ruins = new HashMap<>();
		for (int sector = 0; sector < InkLandsLayout.ROADS; sector++) {
			for (int band = 0; band < BANDS.length; band++) {
				RandomSource random = RandomSource.create(context.seed() ^ (NETWORK_SALT + 31L * sector + band));
				boolean placed = false;
				for (int attempt = 0; attempt < ATTEMPTS_PER_SITE; attempt++) {
					Band region = BANDS[band];
					// If the road-side part of this band is unsafe, search the rest of the same sector and band.
					int roadside = attempt < ATTEMPTS_PER_SITE / 2 ? region.roadside() : 0;
					ChunkPos chunk = sample(layout, sector, region, roadside, random);
					if (!clear(layout, chunk, sites)) continue;
					int surface = surface(context, chunk);
					if (surface == Integer.MIN_VALUE || overlapsPlannedRuins(context, chunk, ruins)) continue;
					sites.put(chunk.toLong(), new Site(sector, band, surface));
					placed = true;
					break;
				}
				if (!placed) {
					TearsOfInk.LOGGER.warn("[adit] seed={} sector={} band={} skipped: no safe terrain/structure plan",
							context.seed(), sector + 1, band + 1);
				}
			}
		}
		return Map.copyOf(sites);
	}

	private static ChunkPos sample(InkLandsLayout layout, int sector, Band band, int roadside, RandomSource random) {
		double radius = Mth.nextInt(random, band.inner(), band.outer());
		double[] left = layout.roadPoint(sector, radius);
		double[] right = layout.roadPoint((sector + 1) % InkLandsLayout.ROADS, radius);
		double start = Math.atan2(left[1], left[0]);
		double span = Mth.positiveModulo(Math.atan2(right[1], right[0]) - start, 2 * Math.PI);
		double offset = Mth.nextInt(random, 50, 88) / radius;
		double angle = start + switch (roadside) {
			case -1 -> offset;
			case 1 -> span - offset;
			default -> span * (0.29 + 0.42 * random.nextDouble());
		};
		return new ChunkPos(Mth.floor(radius * Math.cos(angle)) >> 4, Mth.floor(radius * Math.sin(angle)) >> 4);
	}

	private static boolean clear(InkLandsLayout layout, ChunkPos chunk, Map<Long, Site> sites) {
		int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
		for (long previous : sites.keySet()) {
			ChunkPos other = new ChunkPos(previous);
			if (Math.hypot(cx - other.getMiddleBlockX(), cz - other.getMiddleBlockZ()) < MIN_CENTER_GAP) return false;
		}
		for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
			for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
				if (layout.pick(x, z, null) != ModBiomes.PALE_FOREST
						|| layout.pick(x & ~3, z & ~3, null) != ModBiomes.PALE_FOREST
						|| layout.roadNear(x, z, ROAD_BUFFER - InkLandsLayout.ROAD_HALF_WIDTH) != null
						|| layout.nearLake(x, z, InkLandsLayout.LAKE_ZONE_RADIUS + InkLandsLayout.LAKE_SLOPE)) return false;
			}
		}
		// The biome zoom can select a quart just outside the destination chunk.
		for (int x = chunk.getMinBlockX() - 4; x <= chunk.getMaxBlockX() + 4; x += 4) {
			for (int z = chunk.getMinBlockZ() - 4; z <= chunk.getMaxBlockZ() + 4; z += 4) {
				if (layout.pick(x, z, null) != ModBiomes.PALE_FOREST) return false;
			}
		}
		return !Ruins.nearTemple(layout, cx, cz) && !layout.nearHerald(cx, cz, 96)
				&& layout.arenaCenter().distSqr(chunk.getMiddleBlockPosition(0)) > Mth.square(InkLandsLayout.ARENA_VALLEY_RADIUS + 16);
	}

	private static int surface(GenerationContext context, ChunkPos chunk) {
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int dx : new int[]{2, 8, 13}) {
			for (int dz : new int[]{2, 8, 13}) {
				int x = chunk.getMinBlockX() + dx, z = chunk.getMinBlockZ() + dz;
				int y = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
						context.heightAccessor(), context.randomState()) - 1;
				min = Math.min(min, y);
				max = Math.max(max, y);
			}
		}
		if (min <= context.chunkGenerator().getSeaLevel() || max - min > SURFACE_TOLERANCE) return Integer.MIN_VALUE;
		return max;
	}

	/** Predict ordinary ruins with their own placement, seed and generator. No chunk access. */
	private static boolean overlapsPlannedRuins(GenerationContext context, ChunkPos chunk, Map<Long, StructureStart> predictions) {
		var set = context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(TearsOfInk.id("ruin_scattered"));
		if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement spread)
				|| set.structures().size() != 1) return true;
		if (!(set.structures().getFirst().structure().value() instanceof RuinStructure ruin)) return true;

		// References reach eight chunks. Includes the road-side relocation of monuments.
		int minX = Math.floorDiv(chunk.x - 8, spread.spacing()), maxX = Math.floorDiv(chunk.x + 8, spread.spacing());
		int minZ = Math.floorDiv(chunk.z - 8, spread.spacing()), maxZ = Math.floorDiv(chunk.z + 8, spread.spacing());
		for (int rx = minX; rx <= maxX; rx++) {
			for (int rz = minZ; rz <= maxZ; rz++) {
				ChunkPos origin = spread.getPotentialStructureChunk(context.seed(), rx * spread.spacing(), rz * spread.spacing());
				if (Math.abs(origin.x - chunk.x) > 8 || Math.abs(origin.z - chunk.z) > 8) continue;
				StructureStart start = predictions.get(origin.toLong());
				if (start == null) {
					var probe = new GenerationContext(context.registryAccess(), context.chunkGenerator(), context.biomeSource(),
							context.randomState(), context.structureTemplateManager(), context.seed(), origin,
							context.heightAccessor(), ruin.biomes()::contains);
					if (!ruin.mayOverlap(probe, chunk)) continue;
					start = ruin.generate(context.registryAccess(), context.chunkGenerator(), context.biomeSource(), context.randomState(),
							context.structureTemplateManager(), context.seed(), origin, 0, context.heightAccessor(), ruin.biomes()::contains);
					predictions.put(origin.toLong(), start);
				}
				if (start.isValid() && overlaps(chunk, start.getBoundingBox())) return true;
			}
		}
		return false;
	}

	static boolean overlaps(ChunkPos chunk, BoundingBox box) {
		// Reserve the whole column, including foundations beneath the structure's declared box.
		return box.intersects(chunk.getMinBlockX(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), chunk.getMaxBlockZ());
	}
}
