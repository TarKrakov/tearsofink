package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.TempleNetwork;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Immutable seed-defined battlefield world plan, shared by chunk decoration, tree
 * placement and the post-FULL prop installer. Every query is read-only: chunk
 * generation order can never roll another location or another weapon pose.
 *
 * 1 mature battlefield in the Pale Forest is the first generation pass.
 * Further fields can be appended without moving this one or modifying old saves.
 */
public final class BattlefieldWorldPlan {
    private static final Map<InkLandsLayout, BattlefieldWorldPlan> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** Approximate radius, never a flat disk; the profile is an irregular ellipse. */
    public static final int FIELD_RADIUS = 79;
    public static final int RESERVATION = 105;
    private static final int GUIDE_LENGTH = 125;

    public enum Kind { MONUMENT, SCENE, TRACE }
    public record Placement(int id, Kind kind, int x, int z, Direction facing,
                            BattlefieldPosedScenes.Scenario scene, int variant, int blade, boolean tableau) {}

    public record Field(int index, int x, int z, int road, int roadX, int roadZ,
                        double inwardX, double inwardZ, long seed, List<Placement> placements) {
        public BlockPos center() { return new BlockPos(x, 0, z); }
        public Direction axis() { return Direction.getNearest((float) inwardX, 0f, (float) inwardZ); }
        public double distance(int px, int pz) { return Math.hypot(px - x, pz - z); }

        /** Irregular outline and broad, feathered edge; no circular terrain stamping. */
        public double openness(int px, int pz) {
            double u = (px - x) / 79d, v = (pz - z) / 70d;
            double a = Math.atan2(v, u);
            double limit = 1.0 + .085 * Math.sin(3 * a + (seed & 1023) * .031)
                    + .057 * Math.sin(7 * a - (seed >>> 10 & 1023) * .028)
                    + .030 * Math.cos(11 * a + (seed >>> 20 & 1023) * .022);
            double d = Math.hypot(u, v);
            return Math.max(0, Math.min(1, (limit - d) / .24));
        }

        /** Tangible, bent but continuous approach from a REAL major road. */
        public double approach(int px, int pz) {
            double dx = x - roadX, dz = z - roadZ;
            double length = Math.hypot(dx, dz);
            if (length < 1) return 0;
            double ax = px - roadX, az = pz - roadZ;
            double along = (ax * dx + az * dz) / (length * length);
            if (along < .07 || along > .96) return 0;
            double across = (ax * -dz + az * dx) / length;
            double bend = 2.6 * Math.sin(along * 7.5 + (seed & 255) * .023)
                    + 1.2 * Math.sin(along * 15.1);
            double spread = 3.8 + 2.5 * along;
            double t = Math.max(0, 1 - Math.abs(across - bend) / spread);
            return t * Math.min(1, Math.min((along - .07) * 18, (.96 - along) * 13));
        }

        /** How much original canopy to suppress; natural gaps remain at the edges. */
        public double thinning(int px, int pz) {
            return Math.max(openness(px, pz), approach(px, pz) * .54);
        }

        /** No random state: a column's weathered-ground pattern survives reload/order. */
        public int hash(int px, int pz, int salt) {
            long h = seed ^ (px * 341873128712L) ^ (pz * 132897987541L) ^ (salt * 42317861L);
            h ^= h >>> 27;
            h *= 0x3C79AC492BA7B653L;
            h ^= h >>> 33;
            h *= 0x1C69B3F74AC4AE35L;
            return (int) (h ^ (h >>> 27));
        }
    }

    private final List<Field> fields;
    private BattlefieldWorldPlan(List<Field> fields) { this.fields = List.copyOf(fields); }
    public List<Field> fields() { return fields; }

    /**
     * The temple plan is frozen per-world first. Never choose battlefield
     * coordinates before this immutable reservation exists: a background
     * temple worker cannot change our answer between two chunk threads.
     */
    public static BattlefieldWorldPlan of(InkLandsLayout layout) {
        if (layout == null || layout.hasTempleNetwork() && layout.templeNetwork() == null) return null;
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(layout, BattlefieldWorldPlan::build);
        }
    }

    private static BattlefieldWorldPlan build(InkLandsLayout layout) {
        long seed = layout.seed() ^ 0x42F17E1D0546L;
        int roadStart = (int) Math.floorMod(mix(seed), InkLandsLayout.ROADS);
        TempleNetwork temples = layout.templeNetwork();
        // Different sectors and distances: never reroll on terrain at chunk-load time.
        List<Field> accepted = new ArrayList<>();
        for (int attempt = 0; attempt < 80 && accepted.size() < 1; attempt++) {
            int road = (roadStart + attempt / 16) % InkLandsLayout.ROADS;
            int along = 1320 + (attempt % 8) * 87 + (int) Math.floorMod(mix(seed + attempt), 47) - 23;
            int side = (attempt & 1) == 0 ? 1 : -1;
            double[] roadPoint = layout.roadPoint(road, along);
            double[] dir = layout.roadDirection(road, along);
            double nx = -dir[1] * side, nz = dir[0] * side;
            // 120 blocks from the road: a field whose edge approaches the road,
            // with a long visible sequence of traces between.
            int x = (int) Math.round(roadPoint[0] + nx * GUIDE_LENGTH);
            int z = (int) Math.round(roadPoint[1] + nz * GUIDE_LENGTH);
            if (!safe(layout, temples, road, along, x, z)) continue;
            long fieldSeed = mix(seed ^ (long) x * 31 ^ (long) z * 131);
            int startX = (int) Math.round(roadPoint[0]);
            int startZ = (int) Math.round(roadPoint[1]);
            Field candidate = new Field(accepted.size(), x, z, road, startX, startZ,
                    nx, nz, fieldSeed, List.of());
            accepted.add(new Field(candidate.index, x, z, road, startX, startZ,
                    nx, nz, fieldSeed, compose(candidate)));
        }
        return new BattlefieldWorldPlan(accepted);
    }

    private static boolean safe(InkLandsLayout layout, TempleNetwork temples, int road, int along, int x, int z) {
        if (Math.abs(along - layout.outpostDistance(0)) < 160
                || Math.abs(along - layout.outpostDistance(1)) < 160) return false;
        if (Ruins.nearStart(layout, x, z)
                || layout.nearLake(x, z, 220)
                || layout.nearHerald(x, z, 175)
                || distSq(x, z, layout.arenaCenter().getX(), layout.arenaCenter().getZ()) < 235 * 235
                || layout.depthInside(x, z) < 240
                || layout.roadNear(x, z, 60) != null) return false;
        for (int angle = 0; angle < 12; angle++) {
            double a = angle * Math.PI / 6;
            int px = x + (int) Math.round(89 * Math.cos(a));
            int pz = z + (int) Math.round(82 * Math.sin(a));
            if (layout.pick(px, pz, null) != ModBiomes.PALE_FOREST
                    || layout.roadNear(px, pz, 11) != null
                    || layout.nearLake(px, pz, 45) || layout.nearHerald(px, pz, 50)) return false;
        }
        if (temples != null) {
            for (TempleNetwork.Temple temple : temples.temples()) {
                if (distSq(x, z, temple.x(), temple.z()) < 170 * 170) return false;
            }
            if (temples.overlapsPath(x, z, RESERVATION + 10)) return false;
        }
        return true;
    }

    private static long distSq(int x, int z, int a, int b) {
        return (long) (x - a) * (x - a) + (long) (z - b) * (z - b);
    }

    private static long mix(long v) {
        v ^= v >>> 30; v *= 0xBF58476D1CE4E5B9L;
        v ^= v >>> 27; v *= 0x94D049BB133111EBL;
        return v ^ (v >>> 31);
    }

    private static List<Placement> compose(Field field) {
        // The scene axis always respects the existing road -> battle approach.
        Direction axis = field.axis();
        List<Placement> placements = new ArrayList<>();
        // First place the two titanic silhouettes, inside deliberately empty space.
        monument(placements, field, 16, -22, 0, true);
        monument(placements, field, 36, 29, 1, true);
        // A tangled frontline with several connected, quieter rear episodes.
        var S = BattlefieldPosedScenes.Scenario.class;
        scene(placements, field, -35, -31, BattlefieldPosedScenes.Scenario.REGULAR_SQUAD_LINE_ANCHOR);
        scene(placements, field, -51, -9, BattlefieldPosedScenes.Scenario.REGULAR_SQUAD_RALLY_POINT);
        scene(placements, field, -39, 20, BattlefieldPosedScenes.Scenario.REGULAR_BARE_REMNANT);
        scene(placements, field, -16, -45, BattlefieldPosedScenes.Scenario.PALADIN_HOLDOUT_ANCHOR);
        scene(placements, field, 5, -48, BattlefieldPosedScenes.Scenario.PALADIN_SOUL_SHIELD_WALL);
        scene(placements, field, 28, -47, BattlefieldPosedScenes.Scenario.PALADIN_MERCY_HOLDOUT);
        scene(placements, field, 51, -36, BattlefieldPosedScenes.Scenario.PALADIN_BARE_RELIC);
        scene(placements, field, 51, 1, BattlefieldPosedScenes.Scenario.HUNTER_STRIKE_MARK);
        scene(placements, field, 57, 44, BattlefieldPosedScenes.Scenario.INK_HUNTER_SWEEP);
        scene(placements, field, 19, 49, BattlefieldPosedScenes.Scenario.HUNTER_BARE_TRACE);
        scene(placements, field, -10, 49, BattlefieldPosedScenes.Scenario.INK_HUNTER_LAST_TRACE);
        scene(placements, field, -34, 47, BattlefieldPosedScenes.Scenario.LINE_COLLAPSE);
        scene(placements, field, -13, 7, BattlefieldPosedScenes.Scenario.GRIND_CENTER);
        scene(placements, field, 15, 11, BattlefieldPosedScenes.Scenario.BREAKTHROUGH_LANE);
        scene(placements, field, 42, -17, BattlefieldPosedScenes.Scenario.ISOLATED_LAST_STAND_SMALL);
        scene(placements, field, -57, 38, BattlefieldPosedScenes.Scenario.REAR_GUARD_FALLBACK);
        scene(placements, field, -6, 30, BattlefieldPosedScenes.Scenario.MIXED_DEAD_GROUND);
        scene(placements, field, -55, -48, BattlefieldPosedScenes.Scenario.BLADE_GROVE);
        scene(placements, field, 39, 53, BattlefieldPosedScenes.Scenario.PALADIN_BROKEN_HALO);
        // The approach is a readable trail, not a uniformly repeated fence of signs.
        var traces = new BattlefieldPosedScenes.Scenario[] {
                BattlefieldPosedScenes.Scenario.LONE_BLADE,
                BattlefieldPosedScenes.Scenario.FINAL_TRACE,
                BattlefieldPosedScenes.Scenario.HUNTER_TRACE,
                BattlefieldPosedScenes.Scenario.ABANDONED_POST,
                BattlefieldPosedScenes.Scenario.FALLEN_EDGE,
                BattlefieldPosedScenes.Scenario.AFTERMATH_SCATTER
        };
        for (int i = 0; i < traces.length; i++) {
            double t = .12 + i * .134;
            int x = (int) Math.round(field.roadX + (field.x - field.roadX) * t);
            int z = (int) Math.round(field.roadZ + (field.z - field.roadZ) * t);
            int side = (i & 1) == 0 ? 1 : -1;
            x += (int) Math.round(-field.inwardZ * (side * 2.4));
            z += (int) Math.round(field.inwardX * (side * 2.4));
            placements.add(new Placement(placements.size(), Kind.TRACE, x, z, axis,
                    traces[i], Math.floorMod(field.hash(x, z, 101), 8), -1, false));
        }
        return List.copyOf(placements);
    }

    private static void monument(List<Placement> out, Field f, int forward, int right, int blade, boolean tableau) {
        int x = f.x + (int) Math.round(f.inwardX * forward - f.inwardZ * right);
        int z = f.z + (int) Math.round(f.inwardZ * forward + f.inwardX * right);
        int v = Math.floorMod(f.hash(x, z, 201), 8);
        out.add(new Placement(out.size(), Kind.MONUMENT, x, z, f.axis(), null, v, blade, tableau));
    }

    private static void scene(List<Placement> out, Field f, int forward, int right,
                              BattlefieldPosedScenes.Scenario scene) {
        int x = f.x + (int) Math.round(f.inwardX * forward - f.inwardZ * right);
        int z = f.z + (int) Math.round(f.inwardZ * forward + f.inwardX * right);
        int v = Math.floorMod(f.hash(x, z, scene.ordinal()), 8);
        out.add(new Placement(out.size(), Kind.SCENE, x, z, f.axis(), scene, v, -1, false));
    }

    /** O(fields), but callers first use a 200-block bounding circle. */
    public Field near(int x, int z, int margin) {
        for (Field f : fields) {
            if (Math.abs(x - f.x) > FIELD_RADIUS + margin || Math.abs(z - f.z) > FIELD_RADIUS + margin) continue;
            if (f.distance(x, z) <= FIELD_RADIUS + margin) return f;
        }
        return null;
    }

    /** Builders reserve the land itself; the approach remains traversable and unobstructed. */
    public boolean reserved(int x, int z, int margin) {
        for (Field f : fields) if (f.distance(x, z) < RESERVATION + margin) return true;
        return false;
    }
}
