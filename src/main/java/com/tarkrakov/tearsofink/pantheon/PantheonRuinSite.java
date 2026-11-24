package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.mixin.PantheonChunkMapAccessor;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.TempleNetwork;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWorldPlan;
import com.tarkrakov.tearsofink.world.ruins.RuinStructure;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;

record PantheonRuinSite(BlockPos origin, Rotation rotation, Profile profile) {
    enum Profile {
        FULL, COLLAPSED_ARENA;

        boolean keeps(BlockPos local) {
            return this == FULL || local.getZ() <= 39
                    || (local.getX() >= 28 && local.getX() <= 86 && local.getZ() <= 114);
        }
    }

    static final int WIDTH = 115, DEPTH = 144;
    private static final int[] DISTANCES = {112, 144, 176, 208, 240, 280, 320, 368, 416, 464, 512, 560};
    private static final int DIRECTIONS = 40;
    private static final int EDGE_MARGIN = 8;
    private static final int ROAD_MARGIN = 18;
    private static final int TEMPLE_MARGIN = 42;
    private static final BlockPos ENTRY = new BlockPos(58, 1, 8);

    BlockPos world(BlockPos local) {
        return origin.offset(StructureTemplate.transform(local, Mirror.NONE, rotation, BlockPos.ZERO));
    }

    BlockPos local(BlockPos world) {
        Rotation inverse = switch (rotation) {
            case NONE, CLOCKWISE_180 -> rotation;
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
        };
        return StructureTemplate.transform(world.subtract(origin), Mirror.NONE, inverse, BlockPos.ZERO);
    }

    BlockPos entrance() {
        return world(ENTRY);
    }

    static PantheonRuinSite choose(ServerLevel level, InkLandsLayout layout, PantheonTemple.Template template) throws IOException {
        var temples = Ruins.templePoints(layout);
        double[] start = Ruins.start(layout);
        int[] second = temples.stream()
                .filter(p -> !Ruins.isStartTemple(layout, p[0], p[1]))
                .min(Comparator.comparingDouble(p -> distanceSquared(p[0] - start[0], p[1] - start[1])))
                .orElse(null);
        if (second == null) return null;

        var full = new Footprint(template, Profile.FULL);
        var collapsed = new Footprint(template, Profile.COLLAPSED_ARENA);
        var terrain = new Terrain(level, layout, new HashMap<>());
        var scattered = new Scattered(level);
        var fields = BattlefieldWorldPlan.of(layout);
        TempleNetwork network = layout.templeNetwork();
        Path region = DimensionType.getStorageFolder(level.dimension(), level.getServer().getWorldPath(LevelResource.ROOT))
                .resolve("region");
        var checkedChunks = new HashMap<Long, Boolean>();
        double phase = ((layout.seed() >>> 24) & 255) * (Math.PI * 2 / 256);
        int blocked = 0, occupied = 0, uneven = 0, structures = 0;
        for (int distance : DISTANCES) {
            for (Profile profile : Profile.values()) {
                Footprint footprint = profile == Profile.FULL ? full : collapsed;
                for (int angle = 0; angle < DIRECTIONS; angle++) {
                    double direction = phase + angle * (Math.PI * 2 / DIRECTIONS);
                    int dx = (int) Math.round(distance * Math.cos(direction));
                    int dz = (int) Math.round(distance * Math.sin(direction));
                    int x = second[0] + dx, z = second[1] + dz;
                    Rotation facing = rotation(dx, dz);
                    for (Rotation turn : Rotation.values()) {
                        Rotation rotation = facing.getRotated(turn);
                        BlockPos corner = new BlockPos(x, 0, z).subtract(StructureTemplate.transform(
                                ENTRY.atY(0), Mirror.NONE, rotation, BlockPos.ZERO));
                        var site = new PantheonRuinSite(corner, rotation, profile);
                        BoundingBox box = footprint.bounds(site);
                        if (!outsideReserved(layout, temples, network, fields, box, site, footprint)) {
                            blocked++;
                            continue;
                        }
                        if (!untouched(level, region, box, checkedChunks)) {
                            occupied++;
                            continue;
                        }
                        int y = terrain.platform(box, x, z, site, footprint);
                        if (y == Integer.MIN_VALUE) {
                            uneven++;
                            continue;
                        }
                        if (scattered.overlaps(box, site, footprint)) {
                            structures++;
                            continue;
                        }
                        return new PantheonRuinSite(corner.atY(y), rotation, profile);
                    }
                }
            }
        }
        TearsOfInk.LOGGER.warn("Pantheon site search near temple {}, {}: reserved {}, occupied {}, terrain {}, scattered {}",
                second[0], second[1], blocked, occupied, uneven, structures);
        return null;
    }

    private static boolean outsideReserved(InkLandsLayout layout, java.util.List<int[]> temples,
                                           TempleNetwork network, BattlefieldWorldPlan fields, BoundingBox box,
                                           PantheonRuinSite site, Footprint footprint) {
        for (int[] temple : temples) {
            if (footprint.intersects(site, temple[0] - TEMPLE_MARGIN, temple[1] - TEMPLE_MARGIN,
                    temple[0] + TEMPLE_MARGIN, temple[1] + TEMPLE_MARGIN, EDGE_MARGIN)) return false;
        }
        // Grid spacing is included in the margins so roads cannot pass between probes.
        for (int x = box.minX() - EDGE_MARGIN; x <= box.maxX() + EDGE_MARGIN + 7; x += 8) {
            for (int z = box.minZ() - EDGE_MARGIN; z <= box.maxZ() + EDGE_MARGIN + 7; z += 8) {
                if (!footprint.intersects(site, x, z, x, z, EDGE_MARGIN + 5)) continue;
                if (layout.depthInside(x, z) < 36
                        || layout.nearLake(x, z, InkLandsLayout.LAKE_ZONE_RADIUS + 18)
                        || layout.nearHerald(x, z, 105)
                        || layout.arenaCenter().distToLowCornerSqr(x, layout.arenaCenter().getY(), z) < 166 * 166
                        || layout.nearRoad(x, z, ROAD_MARGIN)
                        || (network != null && network.overlapsPath(x, z, 12))
                        || (fields != null && fields.reserved(x, z, 12))) return false;
            }
        }
        return true;
    }

    private static boolean untouched(ServerLevel level, Path directory, BoundingBox box,
                                     HashMap<Long, Boolean> checked) throws IOException {
        var chunkMap = (PantheonChunkMapAccessor) level.getChunkSource().chunkMap;
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                if (chunkMap.tearsofink$updatingChunk(chunk.toLong()) != null) return false;
                Boolean stored = checked.get(chunk.toLong());
                if (stored == null) {
                    stored = stored(directory, chunk);
                    checked.put(chunk.toLong(), stored);
                }
                if (stored) return false;
            }
        }
        return true;
    }

    private static boolean stored(Path directory, ChunkPos chunk) throws IOException {
        Path region = directory.resolve("r." + (chunk.x >> 5) + "." + (chunk.z >> 5) + ".mca");
        if (Files.notExists(region)) return false;
        try (var file = new RandomAccessFile(region.toFile(), "r")) {
            if (file.length() < 8192) throw new IOException("Incomplete region header: " + region);
            file.seek(4L * ((chunk.x & 31) + 32 * (chunk.z & 31)));
            return file.readInt() != 0;
        }
    }

    private static Rotation rotation(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
        return dz > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    private static double distanceSquared(double x, double z) {
        return x * x + z * z;
    }

    private record Terrain(ServerLevel level, InkLandsLayout layout, HashMap<Long, Integer> heights) {
        private int rawSurface(int x, int z) {
            var source = level.getChunkSource();
            return source.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                    level, source.randomState()) - 1;
        }

        private int surface(int x, int z) {
            long column = BlockPos.asLong(x, 0, z);
            return heights.computeIfAbsent(column, ignored ->
                    (int) Math.floor(layout.valleyHeight(x, z, rawSurface(x, z), this::rawSurface)));
        }

        int platform(BoundingBox box, int x, int z, PantheonRuinSite site, Footprint footprint) {
            Rotation rotation = site.rotation();
            int frontX = x + offset(rotation, 0, -9).getX();
            int frontZ = z + offset(rotation, 0, -9).getZ();
            int y = surface(frontX, frontZ) + 1;
            int sea = level.getChunkSource().getGenerator().getSeaLevel();
            if (y <= sea + 2 || y + 22 >= level.getMaxBuildHeight()) return Integer.MIN_VALUE;

            // Match the apron to the undisturbed trail at its outer end. Inside the footprint
            // the existing approach can be levelled, but a cliff or a flooded entry is unsuitable.
            for (int a = -1; a <= 1; a++) {
                for (int f = -9; f <= 5; f++) {
                    BlockPos delta = offset(rotation, a, f);
                    int height = surface(x + delta.getX(), z + delta.getZ());
                    if (height <= sea || height < y - 12 || height > y + 12) return Integer.MIN_VALUE;
                    if (f == -9 && Math.abs(height - (y - 1)) > 2) return Integer.MIN_VALUE;
                }
            }
            int low = 0;
            for (int px = box.minX(); px <= box.maxX() + 13; px += 14) {
                for (int pz = box.minZ(); pz <= box.maxZ() + 13; pz += 14) {
                    int sampleX = Math.min(px, box.maxX()), sampleZ = Math.min(pz, box.maxZ());
                    if (!footprint.intersects(site, sampleX, sampleZ, sampleX, sampleZ, 4)) continue;
                    int surface = surface(sampleX, sampleZ);
                    if (surface < y - 30 || surface > y + 35) return Integer.MIN_VALUE;
                    if (surface <= sea + 1 && ++low > 12) return Integer.MIN_VALUE;
                }
            }
            return y;
        }
    }

    private static BlockPos offset(Rotation rotation, int across, int forward) {
        return StructureTemplate.transform(new BlockPos(across, 0, forward), Mirror.NONE, rotation, BlockPos.ZERO);
    }

    /** Horizontal bounds of the architectural outline, including its empty interior. */
    private static final class Footprint {
        private final int[] left = new int[DEPTH];
        private final int[] right = new int[DEPTH];
        private final BlockPos first;
        private final BlockPos last;

        Footprint(PantheonTemple.Template template, Profile profile) {
            Arrays.fill(left, WIDTH);
            Arrays.fill(right, -1);
            for (var placement : template.placements()) {
                BlockPos pos = placement.relative();
                if (!profile.keeps(pos)) continue;
                left[pos.getZ()] = Math.min(left[pos.getZ()], pos.getX());
                right[pos.getZ()] = Math.max(right[pos.getZ()], pos.getX());
            }
            // The new walk-in approach extends from the original outer doorway.
            for (int z = 0; z <= 7; z++) {
                left[z] = Math.min(57, left[z]);
                right[z] = Math.max(59, right[z]);
            }
            int loX = WIDTH, hiX = -1, hiZ = 0;
            for (int z = 0; z < DEPTH; z++) {
                if (left[z] > right[z]) continue;
                loX = Math.min(loX, left[z]);
                hiX = Math.max(hiX, right[z]);
                hiZ = z;
            }
            first = new BlockPos(loX, 0, 0);
            last = new BlockPos(hiX, 21, hiZ);
        }

        BoundingBox bounds(PantheonRuinSite site) {
            return BoundingBox.fromCorners(site.world(first), site.world(last));
        }

        boolean intersects(PantheonRuinSite site, int x1, int z1, int x2, int z2, int margin) {
            var first = site.local(new BlockPos(x1, 0, z1));
            var last = site.local(new BlockPos(x2, 0, z2));
            int loX = Math.min(first.getX(), last.getX()) - margin;
            int hiX = Math.max(first.getX(), last.getX()) + margin;
            int loZ = Math.max(0, Math.min(first.getZ(), last.getZ()) - margin);
            int hiZ = Math.min(DEPTH - 1, Math.max(first.getZ(), last.getZ()) + margin);
            for (int z = loZ; z <= hiZ; z++) {
                if (left[z] <= right[z] && left[z] <= hiX && right[z] >= loX) return true;
            }
            return false;
        }
    }

    private static final class Scattered {
        private final ServerLevel level;
        private final RuinStructure structure;
        private final RandomSpreadStructurePlacement placement;
        private final HashMap<Long, Optional<BoundingBox>> predictions = new HashMap<>();
        private final HashMap<Long, Optional<BoundingBox>> starts = new HashMap<>();

        Scattered(ServerLevel level) {
            this.level = level;
            var id = TearsOfInk.id("ruin_scattered");
            structure = (RuinStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
            placement = (RandomSpreadStructurePlacement) level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE_SET).get(id).placement();
        }

        boolean overlaps(BoundingBox box, PantheonRuinSite site, Footprint footprint) {
            int spacing = placement.spacing();
            for (int rx = Math.floorDiv((box.minX() >> 4) - 6, spacing);
                 rx <= Math.floorDiv((box.maxX() >> 4) + 6, spacing); rx++) {
                for (int rz = Math.floorDiv((box.minZ() >> 4) - 6, spacing);
                     rz <= Math.floorDiv((box.maxZ() >> 4) + 6, spacing); rz++) {
                    ChunkPos origin = placement.getPotentialStructureChunk(level.getSeed(), rx * spacing, rz * spacing);
                    var prediction = predictions.computeIfAbsent(origin.toLong(),
                            key -> Optional.ofNullable(structure.scatteredFootprint(context(origin))));
                    if (prediction.isEmpty()) continue;
                    BoundingBox forecast = prediction.get();
                    if (!footprint.intersects(site, forecast.minX(), forecast.minZ(), forecast.maxX(), forecast.maxZ(), 6)) continue;
                    var actual = starts.computeIfAbsent(origin.toLong(), key -> {
                        var ctx = context(origin);
                        var start = structure.generate(ctx.registryAccess(), ctx.chunkGenerator(), ctx.biomeSource(),
                                ctx.randomState(), ctx.structureTemplateManager(), ctx.seed(), origin, 0,
                                ctx.heightAccessor(), structure.biomes()::contains);
                        return start.isValid() ? Optional.of(start.getBoundingBox()) : Optional.empty();
                    });
                    if (actual.isPresent()) {
                        BoundingBox ruin = actual.get();
                        if (footprint.intersects(site, ruin.minX(), ruin.minZ(), ruin.maxX(), ruin.maxZ(), 6)) return true;
                    }
                }
            }
            return false;
        }

        private Structure.GenerationContext context(ChunkPos chunk) {
            var source = level.getChunkSource();
            var generator = source.getGenerator();
            return new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
                    source.randomState(), level.getStructureManager(), level.getSeed(), chunk, level,
                    structure.biomes()::contains);
        }
    }
}
