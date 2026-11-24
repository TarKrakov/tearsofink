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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;

record PantheonRuinSite(BlockPos origin, Rotation rotation) {
    static final int WIDTH = 115, DEPTH = 144;
    private static final int[] DISTANCES = {112, 144, 176, 208, 240, 280, 320, 368, 416};
    private static final int DIRECTIONS = 32;
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

    BoundingBox footprint() {
        return BoundingBox.fromCorners(world(BlockPos.ZERO), world(new BlockPos(WIDTH - 1, 21, DEPTH - 1)));
    }

    static PantheonRuinSite choose(ServerLevel level, InkLandsLayout layout) throws IOException {
        var temples = Ruins.templePoints(layout);
        double[] start = Ruins.start(layout);
        int[] second = temples.stream()
                .filter(p -> !Ruins.isStartTemple(layout, p[0], p[1]))
                .min(Comparator.comparingDouble(p -> distanceSquared(p[0] - start[0], p[1] - start[1])))
                .orElse(null);
        if (second == null) return null;

        var terrain = new Terrain(level, layout);
        var scattered = new Scattered(level);
        var fields = BattlefieldWorldPlan.of(layout);
        TempleNetwork network = layout.templeNetwork();
        Path region = DimensionType.getStorageFolder(level.dimension(), level.getServer().getWorldPath(LevelResource.ROOT))
                .resolve("region");
        var checkedChunks = new HashMap<Long, Boolean>();
        double phase = ((layout.seed() >>> 24) & 255) * (Math.PI * 2 / 256);
        for (int distance : DISTANCES) {
            for (int angle = 0; angle < DIRECTIONS; angle++) {
                double direction = phase + angle * (Math.PI * 2 / DIRECTIONS);
                int dx = (int) Math.round(distance * Math.cos(direction));
                int dz = (int) Math.round(distance * Math.sin(direction));
                int x = second[0] + dx, z = second[1] + dz;
                Rotation rotation = rotation(dx, dz);
                BlockPos corner = new BlockPos(x, 0, z).subtract(StructureTemplate.transform(
                        ENTRY.atY(0), Mirror.NONE, rotation, BlockPos.ZERO));
                BoundingBox box = new PantheonRuinSite(corner, rotation).footprint();
                if (!outsideReserved(layout, temples, network, fields, box)) continue;
                if (!untouched(level, region, box, checkedChunks)) continue;
                int y = terrain.platform(box, x, z, rotation);
                if (y == Integer.MIN_VALUE || scattered.overlaps(box)) continue;
                return new PantheonRuinSite(corner.atY(y), rotation);
            }
        }
        return null;
    }

    private static boolean outsideReserved(InkLandsLayout layout, java.util.List<int[]> temples,
                                           TempleNetwork network, BattlefieldWorldPlan fields, BoundingBox box) {
        for (int[] temple : temples) {
            if (box.intersects(temple[0] - TEMPLE_MARGIN, temple[1] - TEMPLE_MARGIN,
                    temple[0] + TEMPLE_MARGIN, temple[1] + TEMPLE_MARGIN)) return false;
        }
        // Grid spacing is included in the margins so roads cannot pass between probes.
        for (int x = box.minX() - EDGE_MARGIN; x <= box.maxX() + EDGE_MARGIN + 7; x += 8) {
            for (int z = box.minZ() - EDGE_MARGIN; z <= box.maxZ() + EDGE_MARGIN + 7; z += 8) {
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

    private record Terrain(ServerLevel level, InkLandsLayout layout) {
        private int rawSurface(int x, int z) {
            var source = level.getChunkSource();
            return source.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                    level, source.randomState()) - 1;
        }

        private int surface(int x, int z) {
            return (int) Math.floor(layout.valleyHeight(x, z, rawSurface(x, z), this::rawSurface));
        }

        int platform(BoundingBox box, int x, int z, Rotation rotation) {
            int y = surface(x, z);
            int sea = level.getChunkSource().getGenerator().getSeaLevel();
            if (y <= sea + 2 || y + 22 >= level.getMaxBuildHeight()) return Integer.MIN_VALUE;

            // Limit both the approach step and cross-slope in the existing front clearing.
            for (int a = -2; a <= 2; a++) {
                for (int f = -12; f <= 5; f++) {
                    BlockPos offset = StructureTemplate.transform(new BlockPos(a, 0, f),
                            Mirror.NONE, rotation, BlockPos.ZERO);
                    if (Math.abs(surface(x + offset.getX(), z + offset.getZ()) - y) > 2) return Integer.MIN_VALUE;
                }
            }
            for (int px = box.minX(); px <= box.maxX() + 11; px += 12) {
                for (int pz = box.minZ(); pz <= box.maxZ() + 11; pz += 12) {
                    int surface = surface(Math.min(px, box.maxX()), Math.min(pz, box.maxZ()));
                    if (surface <= sea + 1 || surface < y - 16 || surface > y + 11) return Integer.MIN_VALUE;
                }
            }
            return y;
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

        boolean overlaps(BoundingBox box) {
            int spacing = placement.spacing();
            for (int rx = Math.floorDiv((box.minX() >> 4) - 6, spacing);
                 rx <= Math.floorDiv((box.maxX() >> 4) + 6, spacing); rx++) {
                for (int rz = Math.floorDiv((box.minZ() >> 4) - 6, spacing);
                     rz <= Math.floorDiv((box.maxZ() >> 4) + 6, spacing); rz++) {
                    ChunkPos origin = placement.getPotentialStructureChunk(level.getSeed(), rx * spacing, rz * spacing);
                    var prediction = predictions.computeIfAbsent(origin.toLong(),
                            key -> Optional.ofNullable(structure.scatteredFootprint(context(origin))));
                    if (prediction.isEmpty() || !prediction.get().intersects(box.minX(), box.minZ(), box.maxX(), box.maxZ())) continue;
                    var actual = starts.computeIfAbsent(origin.toLong(), key -> {
                        var ctx = context(origin);
                        var start = structure.generate(ctx.registryAccess(), ctx.chunkGenerator(), ctx.biomeSource(),
                                ctx.randomState(), ctx.structureTemplateManager(), ctx.seed(), origin, 0,
                                ctx.heightAccessor(), structure.biomes()::contains);
                        return start.isValid() ? Optional.of(start.getBoundingBox()) : Optional.empty();
                    });
                    if (actual.isPresent() && actual.get().intersects(box.minX(), box.minZ(), box.maxX(), box.maxZ())) return true;
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
