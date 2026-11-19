package com.tarkrakov.tearsofink.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.adit.D060PhysicalAdits;
import com.tarkrakov.tearsofink.world.ruins.RuinPiece;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.StairBlock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.TreeSet;

/** Independent physical test, installed ONLY into a disposable CI checkout. */
public final class D060PhysicalProbe {
    private D060PhysicalProbe() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,env) ->
            dispatcher.register(Commands.literal("toi-d060-physical-full").requires(s -> s.hasPermission(4))
                .executes(c -> inspect(c.getSource().getServer()))));
    }

    private static int inspect(MinecraftServer server) {
        JsonObject out = new JsonObject();
        JsonArray failures = new JsonArray();
        out.add("failures",failures);
        var level = ModDimensions.lands(server);
        try {
            if (level == null) throw new IllegalStateException("Missing Ink Lands");
            var layout = InkLandsLayout.of(level);
            var source = level.getChunkSource();
            var height = LevelHeightAccessor.create(level.getMinBuildHeight(),level.getHeight());
            var terrain = new RoadTerrain(source.getGenerator(),height,source.randomState());
            out.addProperty("seed",Long.toString(level.getSeed()));
            out.addProperty("layout",layout.templeLayoutVersion());
            if (layout.hasTempleNetwork()) layout.prepareTempleNetwork(
                new TempleNetwork.Inputs(terrain,level.registryAccess(),level.getStructureManager(),level.getSeed()));
            var plan = layout.templeNetwork();
            // Identical road-first request sequence in both layouts. Then inspect
            // all remote structures and repeat the original late snapshot.
            // Arrival is still measured before any diagnostic loading.
            out.add("road_blocks_before",roadSamples(level,layout,terrain));
            var centers = Ruins.templePoints(layout);
            if (centers.size()!=13) failures.add("Temple quota "+centers.size()+"/13");
            var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("ruin_temple"));
            if (structure == null) throw new IllegalStateException("Missing temple structure");
            JsonArray temples = new JsonArray();
            var fountains = new HashSet<BlockPos>();
            int built = 0;
            for (int[] center : centers) {
                var chunk = level.getChunk(center[0]>>4,center[1]>>4);
                var start = chunk.getStartForStructure(structure);
                BlockPos fountain = null;
                if (start != null && start.isValid()) {
                    for (var piece : start.getPieces()) {
                        if (piece instanceof RuinPiece ruin) {
                            fountain = ruin.d060PhysicalFountain();
                            if (fountain!=null) break;
                        }
                    }
                }
                JsonObject site = new JsonObject();
                site.addProperty("center",center[0]+","+center[1]);
                boolean valid = fountain!=null && level.getBlockState(fountain).is(ModBlocks.INK_FOUNTAIN)
                    && fountains.add(fountain);
                site.addProperty("built",valid);
                if (fountain!=null) site.addProperty("fountain",fountain.toShortString());
                if (valid) built++; else failures.add("Missing or duplicate physical fountain "+center[0]+","+center[1]);
                if (Ruins.isStartTemple(layout,center[0],center[1]) && valid) {
                    out.addProperty("starter_fountain",fountain.toShortString());
                    out.addProperty("starter_bbox",start.getBoundingBox().toString());
                }
                temples.add(site);
            }
            out.addProperty("physical_temples",built);
            out.add("temples",temples);
            if (plan!=null) {
                JsonArray foundations = new JsonArray();
                for (var t:plan.temples()) {
                    if (!t.raised()) continue;
                    var foundation=foundationProbe(level,terrain,layout,t);
                    foundations.add(foundation);
                    if (!foundation.get("supported").getAsBoolean())
                        failures.add("Raised temple lacks dry continuous support "+t.x()+","+t.z());
                }
                out.add("raised_foundations",foundations);
                out.addProperty("raised_temple_count",foundations.size());
                JsonArray exits = new JsonArray();
                int connected = 0, expected = 0;
                for (var t : plan.temples()) {
                    if (t.starter() || t.road()<0) continue;
                    expected++;
                    JsonObject exit = exitProbe(level,layout,plan,t);
                    exits.add(exit);
                    if (exit.get("connected").getAsBoolean()) connected++;
                    else failures.add("No real walkable exit at "+t.x()+","+t.z()+" road "+t.road());
                }
                out.addProperty("connected_road_exits",connected);
                out.add("exits",exits);
                if (connected!=expected || expected!=9) failures.add("Road exit coverage "+connected+"/"+expected+" (target 9)");
            }
            out.add("adits",D060PhysicalAdits.inspect(level,layout,height,failures));
            out.add("road_blocks",roadSamples(level,layout,terrain));
        } catch (Exception | LinkageError e) {
            failures.add(e.toString());
            TearsOfInk.LOGGER.error("D060 physical probe failed",e);
        } finally {
            if (level!=null) ModDimensions.dropArrivalTickets(level);
        }
        out.addProperty("pass",failures.isEmpty());
        TearsOfInk.LOGGER.info("D060_FULL_PHYSICAL {}",out);
        return failures.isEmpty()?1:0;
    }

    /** Check every originally submerged column of the raised platform, not
     * just the nine planning samples. Load the real chunks and check the full
     * vertical column between the platform and the predicted raw solid bed.
     * Dry ground outside the platform is intentionally not required to flatten.
     */
    private static JsonObject foundationProbe(ServerLevel level,RoadTerrain terrain,
            InkLandsLayout layout,TempleNetwork.Temple t) {
        var facing=t.rotation().rotate(Direction.SOUTH);
        int hx=(facing.getAxis()==Direction.Axis.X?TempleNetwork.TEMPLATE_LENGTH:TempleNetwork.TEMPLATE_WIDTH)/2+1;
        int hz=(facing.getAxis()==Direction.Axis.Z?TempleNetwork.TEMPLATE_LENGTH:TempleNetwork.TEMPLATE_WIDTH)/2+1;
        JsonObject out=new JsonObject();
        JsonArray examples=new JsonArray();
        int wet=0,unsupported=0,reservationViolations=0,checked=0;
        for (int x=t.x()-hx;x<=t.x()+hx;x++) for (int z=t.z()-hz;z<=t.z()+hz;z++) {
            var raw=terrain.sample(x,z);
            if (!raw.water()) continue;
            wet++;
            int top=t.y()-1, bed=terrain.floor(x,z);
            if (layout.roadNear(x,z,TempleNetwork.ROAD_BUFFER-InkLandsLayout.ROAD_HALF_WIDTH)!=null)
                reservationViolations++;
            level.getChunk(x>>4,z>>4);
            boolean ok=top>raw.surface() && bed>=level.getMinBuildHeight() && bed<=top;
            for (int y=top;y>=Math.max(bed,level.getMinBuildHeight()) && ok;y--) {
                var pos=new BlockPos(x,y,z);
                var state=level.getBlockState(pos);
                checked++;
                // Stairs/partial blocks are legal on an approach crossing the
                // apron, but not as the solid mass under the temple platform.
                ok=state.getFluidState().isEmpty() && !state.is(BlockTags.LOGS)
                        && !state.is(BlockTags.LEAVES)
                        && (state.isFaceSturdy(level,pos,Direction.UP) || state.getBlock() instanceof StairBlock);
                if (!ok && examples.size()<12)
                    examples.add(pos.toShortString()+" "+state);
            }
            if (!ok) unsupported++;
        }
        out.addProperty("center",t.x()+","+t.y()+","+t.z());
        out.addProperty("original_wet_columns",wet);
        out.addProperty("checked_blocks",checked);
        out.addProperty("unsupported_columns",unsupported);
        out.addProperty("road_intrusions",reservationViolations);
        out.addProperty("supported",wet>0 && unsupported==0 && reservationViolations==0);
        out.add("examples",examples);
        return out;
    }

    private static boolean open(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && state.getCollisionShape(level,pos).isEmpty();
    }

    private static boolean standable(ServerLevel level, BlockPos feet) {
        var floor = feet.below();
        var state = level.getBlockState(floor);
        return !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)
            && state.getFluidState().isEmpty()
            && (state.isFaceSturdy(level,floor,Direction.UP) || state.getBlock() instanceof StairBlock)
            && open(level,feet) && open(level,feet.above());
    }

    /** Cardinal one-block-step connectivity across ACTUAL generated blocks.
     * This is a conservative block-walk check, not a player input simulation.
     */
    private static JsonObject exitProbe(ServerLevel level, InkLandsLayout layout, TempleNetwork plan, TempleNetwork.Temple t) {
        JsonObject out = new JsonObject();
        out.addProperty("center",t.x()+","+t.z());
        out.addProperty("road",t.road());
        var candidates = new ArrayList<BlockPos>();
        for (var column : plan.allPathBlocks()) {
            if (column.lantern()) continue;
            BlockPos p = column.floor();
            double dist = Math.hypot(p.getX()-t.x(),p.getZ()-t.z());
            if (dist>120) continue;
            if (plan.temples().stream().anyMatch(other -> other!=t && !other.starter() && other.road()>=0
                    && Math.hypot(p.getX()-other.x(),p.getZ()-other.z())<dist)) continue;
            var hit = layout.roadNear(p.getX(),p.getZ(),36-InkLandsLayout.ROAD_HALF_WIDTH);
            if (hit!=null && (int)hit[0]==t.road()) candidates.add(p.above());
        }
        candidates.sort(Comparator.comparingDouble(p -> layout.roadNear(p.getX(),p.getZ(),36-InkLandsLayout.ROAD_HALF_WIDTH)[2]));
        record Step(BlockPos pos,int steps) {}
        var queue = new ArrayDeque<Step>();
        var visited = new HashSet<BlockPos>();
        var usedStarts = new ArrayList<BlockPos>();
        for (BlockPos p : candidates) {
            if (usedStarts.size()>=5) break;
            if (standable(level,p) && visited.add(p)) {
                queue.add(new Step(p,0)); usedStarts.add(p);
            }
        }
        BlockPos connected = null;
        int steps = -1;
        while (!queue.isEmpty() && visited.size()<6000) {
            var at = queue.removeFirst();
            var hit = layout.roadAt(at.pos().getX(),at.pos().getZ());
            if (hit!=null && (int)hit[0]==t.road()
                    && level.getBlockState(at.pos().below()).is(ModBlocks.ASHEN_PATH)) {
                connected = at.pos(); steps = at.steps(); break;
            }
            if (at.steps()>=86) continue;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int dy : new int[]{0,1,-1}) {
                    BlockPos next = at.pos().relative(direction).offset(0,dy,0);
                    if (usedStarts.stream().noneMatch(p -> Math.abs(p.getX()-next.getX())<=52 && Math.abs(p.getZ()-next.getZ())<=52)) continue;
                    if (visited.contains(next) || !standable(level,next)) continue;
                    // The extra head cell is required when moving up a block.
                    if (dy>0 && !open(level,at.pos().above(2))) continue;
                    visited.add(next); queue.addLast(new Step(next,at.steps()+1));
                }
            }
        }
        out.addProperty("candidate_edge_columns",candidates.size());
        out.addProperty("walkable_starts",usedStarts.size());
        out.addProperty("visited",visited.size());
        out.addProperty("connected",connected!=null);
        out.addProperty("steps",steps);
        if (connected!=null) out.addProperty("lane",connected.toShortString());
        return out;
    }

    /** Identical seed, deterministic sample stations and exact BlockState strings. */
    private static JsonObject roadSamples(ServerLevel level, InkLandsLayout layout, RoadTerrain terrain) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        JsonArray stations = new JsonArray();
        var samples = new LinkedHashMap<BlockPos, RoadSample>();
        int bridges = 0, land = 0, lights = 0;
        for (int road=0;road<InkLandsLayout.ROADS;road++) {
            layout.roadHeight(road,1000,terrain);
            List<Integer> at = new ArrayList<>();
            for (boolean bridge : new boolean[]{false,true}) {
                for (int d=900;d<=2050;d+=7) {
                    double[] axis = layout.roadPoint(road,d);
                    if (layout.depthInside((int)axis[0],(int)axis[1])<70) continue;
                    if (layout.roadBridge(road,d-4)!=bridge || layout.roadBridge(road,d+4)!=bridge) continue;
                    boolean outpost=false;
                    for (int n=0;n<InkLandsLayout.OUTPOSTS.length;n++)
                        if (Math.abs(d-layout.outpostDistance(n))<100) outpost=true;
                    if (outpost) continue;
                    at.add(d);
                    if (bridge) bridges++; else land++;
                    break;
                }
            }
            for (int d : at) {
                stations.add(road+":"+d);
                double[] axis=layout.roadPoint(road,d);
                int cX=(int)Math.round(axis[0]), cZ=(int)Math.round(axis[1]);
                for (int dx=-8;dx<=8;dx++) for(int dz=-8;dz<=8;dz++) {
                    int x=cX+dx,z=cZ+dz;
                    var hit=layout.roadNear(x,z,InkLandsLayout.ROAD_SHOULDER);
                    if (hit==null || (int)hit[0]!=road) continue;
                    int y=(int)Math.round(layout.roadHeight(road,hit[1],terrain));
                    boolean onBridge = layout.roadBridge(road,hit[1]);
                    String zone = hit[2]<=InkLandsLayout.ROAD_HALF_WIDTH
                        ? (onBridge?"bridge_deck":"lane")
                        : (onBridge?(hit[2]<=InkLandsLayout.ROAD_HALF_WIDTH+1.0?"bridge_rail":"bridge_margin"):"shoulder");
                    for (int dy=-1;dy<=3;dy++)
                        sample(level,new BlockPos(x,y+dy,z),samples,digest,road+":"+d+"/"+zone+"/"+dy);
                }
                int k=Math.round((float)d/InkLandsLayout.ROAD_LANTERN_SPACING);
                var lamp=layout.roadLanternPost(road,k);
                if (lamp!=null) {
                    int y=(int)Math.round(layout.roadHeight(road,k*InkLandsLayout.ROAD_LANTERN_SPACING,terrain));
                    for(int dy=0;dy<=3;dy++)
                        sample(level,new BlockPos(lamp[0],y+dy,lamp[1]),samples,digest,road+":"+d+"/lantern/"+dy);
                    lights++;
                }
            }
        }
        JsonObject result=new JsonObject();
        result.add("stations",stations);
        result.addProperty("land_stations",land);
        result.addProperty("bridge_stations",bridges);
        result.addProperty("lantern_samples",lights);
        result.addProperty("blocks",samples.size());
        result.addProperty("sha256",HexFormat.of().formatHex(digest.digest()));
        // runServer uses run-server/ as cwd. Keep detailed states out of the
        // single-line JSON log and preserve the original digest/order exactly.
        Path output = Path.of("..","out","d060").toAbsolutePath().normalize();
        Path snapshot = Files.createTempFile(output,"road-samples-",".tsv");
        try (var writer = Files.newBufferedWriter(snapshot,StandardCharsets.UTF_8)) {
            for (var entry : samples.entrySet()) {
                BlockPos pos = entry.getKey();
                RoadSample sample = entry.getValue();
                writer.write(pos.getX()+","+pos.getY()+","+pos.getZ()+"\t"+sample.state()+"\t"+String.join(";",sample.tags())+"\n");
            }
        }
        result.addProperty("snapshot",snapshot.getFileName().toString());
        return result;
    }

    private record RoadSample(String state, TreeSet<String> tags) {}

    private static void sample(ServerLevel level,BlockPos pos,LinkedHashMap<BlockPos,RoadSample> samples,
                               MessageDigest digest,String tag) {
        RoadSample value = samples.get(pos);
        if (value == null) {
            String state = level.getBlockState(pos).toString();
            value = new RoadSample(state,new TreeSet<>());
            samples.put(pos,value);
            digest.update((pos.asLong()+":"+state+"\n").getBytes(StandardCharsets.UTF_8));
        }
        value.tags().add(tag);
    }
}
