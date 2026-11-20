package com.tarkrakov.tearsofink.compat.jaber;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import static com.tarkrakov.tearsofink.compat.jaber.JaberPath.*;

/** A single authoritative driver, only for JABER's owned frogs in the Ink Lands.
 * No direct reference to a third-party class, no teleport, no forced chunk generation. */
public final class JaberRouter {
    private JaberRouter() {}
    private static final Map<Frog, Trip> TRIPS = new WeakHashMap<>();
    private static final Set<Frog> PROMPTED = Collections.newSetFromMap(new WeakHashMap<>());
    private static Map<UUID, Vec3> originals;
    private static final Map<ServerLevel, Long> LAST_PLAN = new WeakHashMap<>();
    private static final int[][] CARDINAL = {{1,0},{-1,0},{0,1},{0,-1}};
    private static final double SPEED = 0.22;

    private static final class Trip {
        final UUID owner;
        final Point goal;
        List<Point> guide;
        int waypoint, next, failures;
        List<Node> path = List.of();
        long retry, sampleAt, messageAt, blockedUntil;
        Node blocked;
        Vec3 sample;
        Trip(UUID owner, Point goal, List<Point> guide) {
            this.owner = owner; this.goal = goal; this.guide = guide;
        }
    }

    public static boolean owned(Frog frog, ServerPlayer player) {
        return frog.getTags().contains("jaber_owner_" + player.getUUID());
    }

    public static void bind(Map<UUID, Vec3> map) { originals = map; }
    private static void clearOriginal(Frog frog) {
        if (originals != null) originals.remove(frog.getUUID());
    }
    public static void stop(Frog frog) {
        frog.getNavigation().stop();
        frog.setDeltaMovement(0, frog.getDeltaMovement().y, 0);
    }
    public static void cancelOwner(ServerPlayer player) {
        for (var e : List.copyOf(TRIPS.entrySet())) {
            if (e.getValue().owner.equals(player.getUUID())) {
                Frog frog = e.getKey();
                if (frog != null) { TRIPS.remove(frog); clearOriginal(frog); stop(frog); PROMPTED.remove(frog); }
            }
        }
        if (player.getVehicle() instanceof Frog frog) PROMPTED.remove(frog);
    }
    public static void forget(Frog frog) {
        TRIPS.remove(frog); PROMPTED.remove(frog); clearOriginal(frog);
    }
    public static void reset() { TRIPS.clear(); PROMPTED.clear(); LAST_PLAN.clear(); originals = null; }

    public static void command(Frog frog, ServerPlayer player, int x, int z, Map<UUID, Vec3> map) {
        bind(map);
        ServerLevel level = player.serverLevel();
        Point goal = new Point(x + 0.5, z + 0.5);
        Point from = point(frog);
        List<List<Point>> roads = new ArrayList<>();
        InkLandsLayout layout = InkLandsLayout.of(level);
        if (layout != null && from.distance(goal) > 24) {
            // Actual roads are five spokes with a deliberately ruined inner end.
            // The guide is geometric only; it never queries terrain or loads chunks.
            for (int road = 0; road < InkLandsLayout.ROADS; road++) {
                List<Point> line = new ArrayList<>();
                for (double along = InkLandsLayout.ROAD_END_RADIUS + InkLandsLayout.BROKEN_BRIDGE_LEN + 4;
                     along <= InkLandsLayout.RADIUS * InkLandsLayout.ROAD_START_FRACTION; along += 18) {
                    double[] xy = layout.roadPoint(road, along);
                    if (JaberCompat.inside(level, Mth.floor(xy[0]), Mth.floor(xy[1]))) {
                        line.add(new Point(xy[0], xy[1]));
                    } else if (!line.isEmpty()) { roads.add(List.copyOf(line)); line.clear(); }
                }
                if (!line.isEmpty()) roads.add(List.copyOf(line));
            }
        }
        Trip trip = new Trip(player.getUUID(), goal, JaberPath.guide(from, goal, roads));
        TRIPS.put(frog, trip); PROMPTED.add(frog);
        stop(frog);
    }
    private static Point point(Frog frog) { return new Point(frog.getX(), frog.getZ()); }

    public static void tick(Frog frog) {
        ServerLevel level = (ServerLevel) frog.level();
        frog.getNavigation().stop();
        ServerPlayer rider = null;
        for (Entity p : frog.getPassengers()) {
            if (p instanceof ServerPlayer player && owned(frog, player)) { rider = player; break; }
        }
        if (rider == null || !frog.isAlive()) {
            forget(frog); stop(frog); return;
        }
        Trip trip = TRIPS.get(frog);
        if (trip == null || !trip.owner.equals(rider.getUUID())) {
            clearOriginal(frog); stop(frog);
            if (PROMPTED.add(frog)) rider.displayClientMessage(Component.translatable("message.tears-of-ink.jaber.ready"), false);
            return;
        }
        long now = level.getGameTime();
        // Match the original driver's riding size and one-block step capability.
        var scale = frog.getAttribute(Attributes.SCALE);
        if (scale != null && scale.getBaseValue() != 2.0) scale.setBaseValue(2.0);
        var step = frog.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null && step.getBaseValue() < 1.0) step.setBaseValue(1.0);
        Terrain terrain = new Terrain(level, frog);
        if (frog.isInWater() && rider.getAirSupply() < 150) {
            stop(frog);
            if (terrain.clear(frog.getX(), frog.getY() + 0.24, frog.getZ())) frog.setDeltaMovement(0, 0.24, 0);
            trip.path = List.of(); trip.next = 0; trip.retry = now + 10;
            return;
        }
        if (point(frog).distance(trip.goal) <= 2.0 && frog.onGround() && !frog.isInWater()
                && terrain.clear(frog.getX(), frog.getY(), frog.getZ())) {
            finish(frog, rider); return;
        }
        // A waypoint is only a direction; reaching it under a bridge is not arrival.
        while (trip.waypoint < trip.guide.size() - 1 && point(frog).distance(trip.guide.get(trip.waypoint)) <= 2.2) {
            trip.waypoint++; trip.path = List.of(); trip.next = 0; trip.failures = 0;
        }
        Point localGoal = trip.guide.get(Math.min(trip.waypoint, trip.guide.size() - 1));
        if (trip.next < trip.path.size()) {
            Node node = trip.path.get(trip.next);
            if (point(frog).distance(node.point()) <= 0.32 && Math.abs(frog.getY() - node.y()) <= 0.65) {
                trip.next++; trip.sample = null;
            }
        }
        if (trip.next < trip.path.size()) {
            Node node = trip.path.get(trip.next);
            if (trip.sample == null) { trip.sample = frog.position(); trip.sampleAt = now; }
            if (!terrain.valid(node) || (now - trip.sampleAt >= 32 && frog.position().distanceToSqr(trip.sample) < 0.16)) {
                trip.blocked = node; trip.blockedUntil = now + 100;
                trip.path = List.of(); trip.next = 0; trip.sample = null; trip.retry = now + 12; trip.failures++;
                stop(frog); return;
            }
            if (now - trip.sampleAt >= 32) { trip.sample = frog.position(); trip.sampleAt = now; }
            move(frog, terrain, node);
            return;
        }
        stop(frog);
        if (!frog.onGround() && !frog.isInWater()) return;
        if (now < trip.retry || Objects.equals(LAST_PLAN.get(level), now)) return;
        LAST_PLAN.put(level, now); // at most one bounded search globally per world tick
        trip.retry = now + 12;
        Node start = terrain.start();
        Result result = start == null ? new Result(List.of(), false, 0) :
                JaberPath.find(start, localGoal, n -> terrain.edges(n, trip.blocked, now < trip.blockedUntil), 32, 900, 1.25);
        if (!result.path().isEmpty()) {
            trip.path = result.path(); trip.next = 0; trip.sample = null;
            trip.failures = 0;
            return;
        }
        trip.failures++;
        if (result.reached() && trip.waypoint < trip.guide.size() - 1) {
            trip.waypoint++; trip.failures = 0; return;
        }
        // If the suggested road is actually blocked, abandon this guide once, not
        // oscillate between it and the direct route. A real gap stays impassable.
        if (trip.failures >= 2 && trip.guide.size() > 1) {
            trip.guide = List.of(trip.goal); trip.waypoint = 0; trip.failures = 0;
        } else if (trip.failures >= 2) {
            trip.retry = now + 60;
            if (now >= trip.messageAt) {
                rider.displayClientMessage(Component.translatable("message.tears-of-ink.jaber.no_route"), true);
                trip.messageAt = now + 200;
            }
        }
    }
    private static void finish(Frog frog, ServerPlayer rider) {
        stop(frog); TRIPS.remove(frog); clearOriginal(frog);
        // Don't eject riders underwater or from a moving/airborne frog.
        for (Entity p : List.copyOf(frog.getPassengers())) p.stopRiding();
        rider.displayClientMessage(Component.translatable("message.tears-of-ink.jaber.arrived"), false);
    }
    private static void move(Frog frog, Terrain terrain, Node node) {
        Vec3 v = frog.getDeltaMovement();
        double dx = node.x() + 0.5 - frog.getX(), dz = node.z() + 0.5 - frog.getZ();
        double h = Math.hypot(dx, dz), dy = node.y() - frog.getY();
        if (!Double.isFinite(h) || !Double.isFinite(dy)) { stop(frog); return; }
        double speed = Math.min(h, frog.isInWater() ? 0.17 : frog.onGround() ? SPEED : 0.12);
        double vx = h < 0.04 ? 0 : dx / h * speed, vz = h < 0.04 ? 0 : dz / h * speed;
        if (!terrain.ready(frog.getX() + vx, frog.getY(), frog.getZ() + vz)) { stop(frog); return; }
        if (h > 0.05) {
            float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90);
            frog.setYRot(yaw); frog.setYBodyRot(yaw); frog.setYHeadRot(yaw);
        }
        double vy = v.y;
        if (frog.isInWater()) vy = Mth.clamp(dy * 0.30, -0.08, 0.30);
        else if (frog.onGround() && dy > 0.48 && terrain.clear(frog.getX(), frog.getY() + Math.min(1, dy), frog.getZ())) vy = 0.43;
        frog.setDeltaMovement(vx, vy, vz);
    }

    private record Surface(Node node, boolean road, boolean water) {}
    private static final class Terrain {
        final ServerLevel level;
        final Frog frog;
        final double half, height;
        final Map<BlockPos, Optional<Surface>> surfaces = new HashMap<>();
        Terrain(ServerLevel level, Frog frog) {
            this.level = level; this.frog = frog;
            half = Math.max(0.45, frog.getBbWidth() * 0.5) - 0.01;
            height = Math.max(2.45, frog.getBbHeight() + 1.8);
        }
        boolean ready(double x, double y, double z) {
            if (y < level.getMinBuildHeight() || y + height >= level.getMaxBuildHeight()) return false;
            for (int bx = Mth.floor(x - half) - 1; bx <= Mth.floor(x + half) + 1; bx++)
                for (int bz = Mth.floor(z - half) - 1; bz <= Mth.floor(z + half) + 1; bz++)
                    if (!JaberCompat.columnReady(level, bx, bz)) return false;
            return true;
        }
        boolean clear(double x, double y, double z) {
            if (!ready(x, y, z)) return false;
            AABB box = new AABB(x-half, y+0.015, z-half, x+half, y+height, z+half);
            if (level.getBlockCollisions(frog, box).iterator().hasNext()) return false;
            for (int bx = Mth.floor(box.minX); bx <= Mth.floor(box.maxX); bx++)
                for (int bz = Mth.floor(box.minZ); bz <= Mth.floor(box.maxZ); bz++)
                    for (int by = Mth.floor(box.minY); by <= Mth.floor(box.maxY); by++)
                        if (hazard(new BlockPos(bx, by, bz))) return false;
            return true;
        }
        boolean hazard(BlockPos p) {
            var s = level.getBlockState(p);
            return s.getFluidState().is(FluidTags.LAVA) || s.is(Blocks.CACTUS) || s.is(Blocks.MAGMA_BLOCK)
                    || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CAMPFIRE)
                    || s.is(Blocks.SOUL_CAMPFIRE) || s.is(Blocks.SWEET_BERRY_BUSH)
                    || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.WITHER_ROSE) || s.is(ModBlocks.INK_PUDDLE);
        }
        private Surface sample(BlockPos p) {
            if (!JaberCompat.shapeReady(level, p.getX(), p.getY(), p.getZ()) ||
                    p.getY() + 2 >= level.getMaxBuildHeight() || hazard(p)) return null;
            var state = level.getBlockState(p);
            boolean water = state.getFluidState().is(FluidTags.WATER);
            double y;
            if (water) {
                // Only open water surface, never a submerged shortcut underneath a bridge.
                if (level.getFluidState(p.above()).is(FluidTags.WATER)) return null;
                if (!state.getCollisionShape(level, p).isEmpty()) return null;
                y = p.getY() + 0.5;
            } else {
                if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS) || state.is(BlockTags.LEAVES)) return null;
                var shape = state.getCollisionShape(level, p);
                if (shape.isEmpty()) return null;
                double top = shape.max(Direction.Axis.Y);
                if (top <= 0 || top > 1.0) return null;
                y = p.getY() + Math.ceil(top * 2 - 0.0001) * 0.5;
            }
            if (!clear(p.getX()+0.5, y, p.getZ()+0.5)) return null;
            boolean road = state.is(ModBlocks.ASHEN_PATH) || state.is(ModBlocks.PALE_PLANKS);
            return new Surface(new Node(p.getX(), Mth.floor(y*2+0.01), p.getZ()),road,water);
        }
        Surface surface(BlockPos p) { return surfaces.computeIfAbsent(p, k -> Optional.ofNullable(sample(k))).orElse(null); }
        List<Surface> around(int x, int z, double y, double down, double up) {
            List<Surface> result = new ArrayList<>();
            for (int sy = Mth.floor(y - down) - 1; sy <= Mth.floor(y + up); sy++) {
                Surface s = surface(new BlockPos(x,sy,z));
                if (s != null && s.node.y() >= y - down - 0.001 && s.node.y() <= y + up + 0.001) result.add(s);
            }
            return result;
        }
        Node start() {
            return around(frog.getBlockX(),frog.getBlockZ(),frog.getY(),1.1,1.0).stream()
                    .min(Comparator.comparingDouble(s -> Math.abs(s.node.y()-frog.getY())))
                    .map(Surface::node).orElse(null);
        }
        boolean valid(Node n) {
            return around(n.x(),n.z(),n.y(),0.01,0.01).stream().anyMatch(s -> s.node.equals(n));
        }
        Iterable<Edge> edges(Node from, Node blocked, boolean avoid) {
            List<Edge> out = new ArrayList<>(8);
            for (int[] d : CARDINAL) {
                for (Surface surface : around(from.x()+d[0],from.z()+d[1],from.y(),1.5,1.0)) {
                    Node to = surface.node;
                    if (avoid && to.equals(blocked)) continue;
                    double high = Math.max(from.y(),to.y());
                    // Test the swept middle at the higher floor. Unlike the old free()
                    // loop, never treat the next step's supporting block as headroom.
                    if (!clear(from.x()+0.5,high,from.z()+0.5) ||
                            !clear((from.x()+to.x())*0.5+0.5,high,(from.z()+to.z())*0.5+0.5)) continue;
                    double cost = (surface.road ? 1.0 : surface.water ? 4.0 : 2.4) + Math.abs(to.y()-from.y())*0.5;
                    out.add(new Edge(to,cost));
                }
            }
            return out;
        }
    }
}
