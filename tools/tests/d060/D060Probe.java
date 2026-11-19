package com.tarkrakov.tearsofink.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.adit.D060AditProbe;
import com.tarkrakov.tearsofink.world.ruins.RuinPiece;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** Only copied into src/main/java inside the disposable Actions checkout. */
public final class D060Probe {
    private D060Probe() {}
    private static long arrivalStart, previousTick;
    private static int ticks;
    private static final List<Long> gaps = new ArrayList<>();

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(
            Commands.literal("toi-d060-probe").requires(s -> s.hasPermission(4))
                .then(Commands.literal("plan").executes(c -> probe(c.getSource().getServer(), false)))
                .then(Commands.literal("physical").executes(c -> probe(c.getSource().getServer(), true)))
                .then(Commands.literal("arrival").executes(c -> arrival(c.getSource().getServer())))));
        ServerTickEvents.END_SERVER_TICK.register(D060Probe::tick);
    }

    private static void emit(String kind, JsonObject result) {
        TearsOfInk.LOGGER.info("D060_{} {}", kind, result);
    }

    private static int arrival(MinecraftServer server) {
        ServerLevel level = ModDimensions.lands(server);
        if (level == null || arrivalStart != 0) return 0;
        arrivalStart = previousTick = System.nanoTime();
        ticks = 0;
        gaps.clear();
        ModDimensions.preloadArrival(level);
        return 1;
    }

    private static void tick(MinecraftServer server) {
        if (arrivalStart == 0) return;
        ServerLevel level = ModDimensions.lands(server);
        long now = System.nanoTime();
        if (ticks > 0) gaps.add(now - previousTick);
        previousTick = now;
        if (++ticks % 20 == 0) ModDimensions.preloadArrival(level);
        var target = ModDimensions.readyArrival(level);
        if (target == null && now - arrivalStart < 240_000_000_000L) return;
        JsonObject result = new JsonObject();
        result.addProperty("ready", target != null);
        result.addProperty("elapsed_ms", (now - arrivalStart) / 1e6);
        result.addProperty("ticks", ticks);
        gaps.sort(Long::compareTo);
        if (!gaps.isEmpty()) {
            result.addProperty("p50_gap_ms", gaps.get((gaps.size()-1)/2) / 1e6);
            result.addProperty("p95_gap_ms", gaps.get((int)Math.floor((gaps.size()-1)*.95)) / 1e6);
            result.addProperty("max_gap_ms", gaps.getLast() / 1e6);
        }
        if (target != null) {
            result.addProperty("position", target.pos().toString());
            result.addProperty("yaw", target.yaw());
        }
        arrivalStart = 0;
        // Keep the arrival ticket until the physical check has finished.
        emit("ARRIVAL", result);
    }

    private static void check(boolean valid, JsonArray failures, String message) {
        if (!valid) failures.add(message);
    }

    private static int probe(MinecraftServer server, boolean physical) {
        JsonObject result = new JsonObject();
        JsonArray failures = new JsonArray();
        result.add("failures", failures);
        try {
            inspect(server, physical, result, failures);
        } catch (Exception | LinkageError failure) {
            failure.printStackTrace();
            failures.add(failure.toString());
        } finally {
            if (physical) {
                var level = ModDimensions.lands(server);
                if (level != null) ModDimensions.dropArrivalTickets(level);
            }
        }
        result.addProperty("pass", failures.isEmpty());
        emit("RESULT", result);
        return failures.isEmpty() ? 1 : 0;
    }

    private static void inspect(MinecraftServer server, boolean physical, JsonObject result, JsonArray failures) {
        ServerLevel level = ModDimensions.lands(server);
        if (level == null) throw new IllegalStateException("No Ink Lands");
        InkLandsLayout layout = InkLandsLayout.of(level);
        var chunks = level.getChunkSource();
        var generator = chunks.getGenerator();
        var random = chunks.randomState();
        var height = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
        var terrain = new RoadTerrain(generator, height, random);
        var inputs = new TempleNetwork.Inputs(terrain, level.registryAccess(), level.getStructureManager(), level.getSeed());
        result.addProperty("seed", Long.toString(level.getSeed()));
        result.addProperty("layout", layout.templeLayoutVersion());
        result.addProperty("java", System.getProperty("java.version"));
        check(layout.templeLayoutVersion() == 3, failures, "World did not bind V3");
        if (!layout.hasTempleNetwork()) return;
        long start = System.nanoTime();
        boolean ready = layout.templeNetwork() != null;
        var plan = layout.prepareTempleNetwork(inputs);
        result.addProperty("plan_ready_on_probe", ready);
        result.addProperty("plan_wait_ms", (System.nanoTime() - start) / 1e6);
        result.addProperty("temples", plan.temples().size());
        check(plan.temples().size() == 13, failures, "Expected 13 temple sites");
        int[] outer = new int[5], deep = new int[5];
        int forest = 0, starter = 0, validStarts = 0, expandedOuter = 0;
        var origins = new HashSet<Long>();
        JsonArray sites = new JsonArray();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("ruin_temple"));
        if (structure == null) throw new IllegalStateException("Missing ruin_temple");
        TempleNetwork.Temple selected = null;
        for (var t : plan.temples()) {
            JsonObject site = new JsonObject();
            site.addProperty("x", t.x()); site.addProperty("y", t.y()); site.addProperty("z", t.z());
            site.addProperty("road", t.road()); site.addProperty("starter", t.starter());
            site.addProperty("raised",t.raised());
            site.addProperty("rotation", t.rotation().name());
            double depth = layout.depthInside(t.x(), t.z());
            site.addProperty("depth", depth);
            ChunkPos origin = new ChunkPos(t.x() >> 4, t.z() >> 4);
            check(origins.add(origin.toLong()), failures, "Duplicate temple chunk " + origin);
            if (t.starter()) {
                starter++;
                var original = Ruins.start(layout);
                check(t.x() == (int)original[0] && t.z() == (int)original[1]
                        && t.road() == Ruins.startRoad(layout), failures, "Starter mismatch");
                outer[t.road()]++;
            } else if (t.road() < 0) {
                forest++;
                check(layout.roadNear(t.x(), t.z(), 135) == null, failures, "Forest temple near road");
            } else if (depth >= TempleNetwork.OUTER_RECOVERY_MIN && depth <= TempleNetwork.OUTER_RECOVERY_MAX) {
                outer[t.road()]++;
                if (depth < 250 || depth > 500) expandedOuter++;
                if (selected == null) selected = t;
            } else if (depth >= 850 && depth <= 1350) {
                deep[t.road()]++;
                if (selected == null) selected = t;
            } else {
                failures.add("Wrong depth on road " + t.road() + ": " + depth);
            }
            var predicted = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                    random, level.getStructureManager(), level.getSeed(), origin, 0, height, structure.biomes()::contains);
            if (predicted.isValid()) validStarts++;
            else failures.add("Invalid predicted StructureStart at " + origin);
            sites.add(site);
        }
        result.add("sites", sites);
        result.addProperty("valid_predicted_starts", validStarts);
        result.addProperty("outer", Arrays.toString(outer));
        result.addProperty("deep", Arrays.toString(deep));
        result.addProperty("forest", forest);
        result.addProperty("expanded_outer_band", expandedOuter);
        check(starter == 1 && forest == 3 && Arrays.stream(outer).allMatch(x -> x == 1)
                && Arrays.stream(deep).allMatch(x -> x == 1), failures, "Wrong 10+3 slot distribution");
        var pathColumns = new HashSet<Long>();
        int duplicatePaths = 0, roadIntrusions = 0, markers = 0;
        for (var block : plan.allPathBlocks()) {
            BlockPos p = block.floor();
            if (!pathColumns.add(ChunkPos.asLong(p.getX(), p.getZ()))) duplicatePaths++;
            if (layout.roadNear(p.getX(), p.getZ(), TempleNetwork.ROAD_BUFFER - InkLandsLayout.ROAD_HALF_WIDTH) != null) roadIntrusions++;
            if (block.lantern()) markers++;
        }
        result.addProperty("path_columns", plan.allPathBlocks().size());
        result.addProperty("markers", markers);
        result.addProperty("duplicate_path_columns", duplicatePaths);
        result.addProperty("road_intrusions", roadIntrusions);
        check(duplicatePaths == 0 && roadIntrusions == 0, failures, "Approach columns overlap or invade reserved road");
        // Independent V1 layout, same world seed and raw sampler; no block generation.
        var legacy = InkLandsLayout.create(random.sampler());
        check(Arrays.equals(Ruins.start(layout), Ruins.start(legacy)), failures, "Starter moved from V1");
        int differences = 0, comparisons = 0;
        for (int road = 0; road < InkLandsLayout.ROADS; road++) {
            for (int d = InkLandsLayout.ROAD_END_RADIUS; d <= (int)(InkLandsLayout.RADIUS * InkLandsLayout.ROAD_START_FRACTION); d++) {
                var a = layout.roadPoint(road, d); var b = legacy.roadPoint(road, d);
                if (!Arrays.equals(a, b) || Double.compare(layout.roadHeight(road, d, terrain),
                        legacy.roadHeight(road, d, terrain)) != 0
                        || layout.roadBridge(road, d) != legacy.roadBridge(road, d)) differences++;
                comparisons++;
            }
        }
        result.addProperty("road_profile_probes", comparisons);
        result.addProperty("road_profile_differences", differences);
        check(differences == 0, failures, "Road profiles differ from V1");
        var adits = D060AditProbe.inspect(level, layout, height, failures);
        result.add("adits", adits);
        if (physical) {
            // Check one selected roadside temple, its nearby planned columns and the real
            // starter. This is a sample, not the full physical-generation acceptance.
            var first = plan.temples().stream().filter(TempleNetwork.Temple::starter).findFirst().orElseThrow();
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
                level.getChunk((first.x() >> 4) + dx, (first.z() >> 4) + dz);
            var starterChunk = level.getChunk(first.x() >> 4, first.z() >> 4);
            var built = starterChunk.getStartForStructure(structure);
            BlockPos fountain = null;
            if (built != null && built.isValid()) {
                for (var piece : built.getPieces()) if (piece instanceof RuinPiece ruin) {
                    fountain = ruin.startFountain();
                    if (fountain != null) break;
                }
            }
            result.addProperty("starter_start_valid", built != null && built.isValid());
            result.addProperty("starter_fountain_predicted", fountain == null ? "none" : fountain.toShortString());
            if (fountain != null) result.addProperty("starter_fountain_block", level.getBlockState(fountain).toString());
            check(fountain != null && level.getBlockState(fountain).is(ModBlocks.INK_FOUNTAIN), failures, "Missing real starter fountain");
            if (fountain != null) result.addProperty("starter_fountain", fountain.toShortString());
            if (selected == null) failures.add("No roadside temple available for physical test");
            else {
                JsonArray physicalSites = new JsonArray();
                for (var t : plan.temples()) {
                    if (t.starter() || t.road() < 0) continue;
                    JsonObject detail = new JsonObject();
                    physicalTemple(level, structure, plan, t, detail, failures);
                    physicalSites.add(detail);
                }
                result.add("physical_sites", physicalSites);
            }
        }
    }

    private static void physicalTemple(ServerLevel level, Structure structure, TempleNetwork plan,
                                       TempleNetwork.Temple t, JsonObject result, JsonArray failures) {
        long start = System.nanoTime();
        var chunk = level.getChunk(t.x() >> 4, t.z() >> 4);
        var built = chunk.getStartForStructure(structure);
        check(built != null && built.isValid(), failures, "Missing real roadside StructureStart");
        var template = level.getStructureManager().getOrCreate(Ruins.TEMPLE);
        var size = template.getSize();
        var origin = new BlockPos(t.x()-size.getX()/2, t.y(), t.z()-size.getZ()/2);
        var settings = new StructurePlaceSettings().setRotation(t.rotation())
                .setRotationPivot(new BlockPos(size.getX()/2, 0, size.getZ()/2));
        var fountains = template.filterBlocks(origin, settings, ModBlocks.INK_FOUNTAIN);
        check(fountains.size() == 1, failures, "Template fountain count");
        for (var f : fountains) {
            check(level.getBlockState(f.pos()).is(ModBlocks.INK_FOUNTAIN), failures, "Missing real roadside fountain " + f.pos());
        }
        int columns = 0, wrong = 0;
        for (var block : plan.allPathBlocks()) {
            var p = block.floor();
            // Attribute columns to the closest planned non-starter roadside temple.
            double dist = Math.hypot(p.getX()-t.x(), p.getZ()-t.z());
            if (dist > 120) continue;
            boolean closer = plan.temples().stream().anyMatch(other -> other != t && !other.starter() && other.road() >= 0
                    && Math.hypot(p.getX()-other.x(),p.getZ()-other.z()) < dist);
            if (closer) continue;
            columns++;
            var state = level.getBlockState(p);
            boolean ok;
            if (block.lantern() || block.up() == null) {
                ok = state.is(ModBlocks.OVERGROWN_RUIN_BRICKS) || state.is(ModBlocks.CRACKED_RUIN_BRICKS);
            } else {
                ok = state.is(ModBlocks.CRACKED_RUIN_BRICK_STAIRS) && state.getValue(StairBlock.FACING) == block.up();
            }
            if (block.lantern()) {
                ok &= level.getBlockState(p.above(2)).is(ModBlocks.INK_LANTERN);
            } else {
                for (int y=1; y<=2; y++) {
                    var at = p.above(y);
                    ok &= level.getBlockState(at).getCollisionShape(level,at).isEmpty()
                            && level.getFluidState(at).isEmpty();
                }
            }
            if (!ok) wrong++;
        }
        result.addProperty("physical_temple", t.x()+","+t.y()+","+t.z());
        result.addProperty("physical_columns", columns);
        result.addProperty("physical_column_failures", wrong);
        result.addProperty("physical_sample_ms", (System.nanoTime()-start)/1e6);
        check(columns > 0 && wrong == 0, failures, "Real approach sample incomplete or obstructed");
    }
}
