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

    public enum Kind { MONUMENT, SCENE, TRACE, RELIC }
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


        /**
         * Own ruts, blast-like depressions and eroded rims, generated on NEW
         * terrain only. Seed-locked, no full-disk flattening. Reserve a generous
         * halo for all real posed scenes and titan weapons.
         * Positive = pit depth, negative = low broken rim.
         */
        public int scar(int px, int pz) {
            if (openness(px, pz) < .32) return 0;
            for (Placement slot : placements) {
                int dx = px - slot.x(), dz = pz - slot.z();
                int halo = slot.kind() == Kind.MONUMENT ? 19 : slot.kind() == Kind.RELIC ? 6 : 14;
                if (dx * dx + dz * dz < halo * halo) return 0;
            }
            int result = 0;
            for (int i = 0; i < 16; i++) {
                long h = mix(seed + i * 0x9E3779B97F4A7C15L);
                int cx = x + (int) Math.floorMod(h, 135) - 67;
                int cz = z + (int) Math.floorMod(h >>> 16, 125) - 62;
                double radius = 5.0 + Math.floorMod(h >>> 32, 10);
                double rx = (px - cx) * (1.0 + (h & 3) * .09);
                double rz = (pz - cz) * (1.0 + ((h >>> 2) & 3) * .08);
                double d = Math.hypot(rx, rz);
                double ragged = radius * (1 + .12 * Math.sin(Math.atan2(rz,rx) * 5 + i));
                if (d < ragged) {
                    int depth = (int) Math.ceil((ragged - d) / ragged
                            * (3 + Math.floorMod(h >>> 40, 4)));
                    result = Math.max(result, depth);
                } else if (d < ragged + 2 && (hash(px, pz, 0xAC00 + i) & 3) == 0) {
                    result = Math.min(result, -1);
                }
            }
            // Two narrow diagonal tears, interrupted by the scene reservations.
            for (int i = 0; i < 2; i++) {
                double fx = px - x + (i == 0 ? 26 : -33);
                double fz = pz - z + (i == 0 ? 35 : -27);
                double along = (fx + fz * .63) / 1.18;
                double side = (fz - fx * .63) / 1.18;
                if (Math.abs(along) < 29 && Math.abs(side) < 3.5
                        && Math.abs(Math.sin(along * .19 + i) * 1.2 + side) < 2.6)
                    result = Math.max(result, 2 + ((hash(px, pz, 0x7900 + i) >>> 1) & 1));
            }
            return result;
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
        } else {
            // Layout v1 has no frozen TempleNetwork. The historical temples are
            // still deterministic and must retain the same protection.
            for (int[] temple : Ruins.templePoints(layout)) {
                if (distSq(x, z, temple[0], temple[1]) < 170 * 170) return false;
            }
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
        // D-137: append new slot IDs AFTER the original 27. A patch is a
        // SINGLE inert anchor for 7..19 individual ancient weapons. Do not
        // pack all arms into the flag tableaux; leave wide quiet corridors.
        relicBeds(field, placements);
        // Append-only. All D-135/D-137 saved slot IDs and poses are unchanged.
        projectileBeds(field, placements);
        // Three restrained, faction-specific fallen-standard episodes.
        // Append new IDs after every existing weapon and projectile bed.
        scene(placements, field, -58, -32, BattlefieldPosedScenes.Scenario.REGULAR_BARE_REMNANT);
        scene(placements, field, -12, -61, BattlefieldPosedScenes.Scenario.PALADIN_BARE_RELIC);
        scene(placements, field, 61, 20, BattlefieldPosedScenes.Scenario.HUNTER_BARE_TRACE);
        // D-140: independent, low-cost buried standards. Keep every original
        // slot and all three earlier bare episodes at their saved IDs.
        barePoleBeds(field, placements);
        return List.copyOf(placements);
    }

    private static void relicBeds(Field field, List<Placement> placements) {
        // Position is forward/right relative to the road-facing battle axis.
        // Each area has its own "density grammar", not equal random scattering.
        int[][] areas = {
                {-42, -24, 5, 0}, // regular wall / blade grove
                {-14,  -8, 8, 2}, // fought-over central ground
                { 35, -18, 5, 1}, // collapse and discarded gear
                {-33,  35, 5, 0}, // rear of the first battle line
                { 27,  35, 4, 4}, // hunter traces; lighter density
                { -4, -41, 5, 3}, // Soul defenders' shields
                { 25,   6, 4, 2}  // between lines, shattered front
        };
        for (int area = 0; area < areas.length; area++) {
            var group = areas[area];
            int done = 0;
            for (int attempt = 0; attempt < group[2] * 24 && done < group[2]; attempt++) {
                int roll = field.hash(area * 101 + attempt, area * 137 - attempt, 0xB17);
                double a = (roll & 0xffff) * (Math.PI * 2 / 65536d);
                double radius = 5 + ((roll >>> 16) & 255) / 255d * 23;
                double forward = group[0] + Math.cos(a) * radius;
                double right = group[1] + Math.sin(a) * radius;
                int x = field.x + (int) Math.round(field.inwardX * forward - field.inwardZ * right);
                int z = field.z + (int) Math.round(field.inwardZ * forward + field.inwardX * right);
                if (field.openness(x, z) < .40) continue;
                // Keep a legible, navigable earthen corridor between the
                // large ruins and the high far bank; do not turn it into a
                // thicket of separate static weapon anchors.
                if (BattlefieldAccessRoutes.nearCorridor(field, x, z, 6)) continue;
                boolean crowded = false;
                for (Placement p : placements) {
                    double min = switch (p.kind()) {
                        case MONUMENT -> 22d;
                        case SCENE -> 11d;
                        case TRACE -> 9d;
                        case RELIC -> 13d;
                    };
                    if ((long)(x - p.x()) * (x - p.x()) + (long)(z - p.z()) * (z - p.z())
                            < min * min) { crowded = true; break; }
                }
                if (crowded) continue;
                int variant = (roll >>> 11) & 7;
                placements.add(new Placement(placements.size(), Kind.RELIC, x, z, field.axis(),
                        null, variant, group[3], false));
                done++;
            }
        }
    }

    /** Coherent impact zones, not a uniform carpet of flying ammunition. */
    private static void projectileBeds(Field field, List<Placement> placements) {
        // Forward/right positions of six distinct old volleys: broken front,
        // soul shield line, hunter flank and the abandoned rear positions.
        // Kind.RELIC makes one inert patch represent many embedded projectiles.
        int[][] sectors = {
                {-28, -43, 5, 3}, {12, -32, 7, 3}, {44, 11, 6, 2},
                {-32, 29, 5, 2}, {24, 40, 7, 2}, {-8, 17, 6, 2}
        };
        for (int group = 0; group < sectors.length; group++) {
            int[] area = sectors[group];
            int placed = 0;
            for (int attempt = 0; attempt < 52 && placed < area[3]; attempt++) {
                int roll = field.hash(group * 103 + attempt, group * 179 - attempt, 0xA220);
                double angle = (roll & 65535) * (2 * Math.PI / 65536d);
                double distance = 2.5 + ((roll >>> 16) & 255) / 255d * 12;
                double forward = area[0] + Math.cos(angle) * distance;
                double right = area[1] + Math.sin(angle) * distance;
                int x = field.x + (int) Math.round(field.inwardX * forward - field.inwardZ * right);
                int z = field.z + (int) Math.round(field.inwardZ * forward + field.inwardX * right);
                if (field.openness(x, z) < .46
                        || BattlefieldAccessRoutes.nearCorridor(field, x, z, 3)) continue;
                boolean crowded = false;
                for (Placement other : placements) {
                    double gap = switch (other.kind()) {
                        case MONUMENT -> 17d;
                        case SCENE, TRACE -> 8d;
                        case RELIC -> other.blade() >= 5 ? 10d : 8d;
                    };
                    if ((long)(x - other.x()) * (x - other.x())
                            + (long)(z - other.z()) * (z - other.z()) < gap * gap) {
                        crowded = true;
                        break;
                    }
                }
                if (crowded) continue;
                placements.add(new Placement(placements.size(), Kind.RELIC, x, z, field.axis(),
                        null, (roll >>> 10) & 7, area[2], false));
                placed++;
            }
        }
    }

    /**
     * Sparse groups of genuinely bare author-drawn standards, sunk into
     * different pieces of earth rather than vertical scenic banner blocks.
     * Their independent patch anchors are installed even when a full faction
     * scene cannot pass its more demanding flag-envelope placement checks.
     * Never assign new IDs until ALL former slots have already been appended.
     */
    private static void barePoleBeds(Field field, List<Placement> placements) {
        // forward, right, patch style, max patch anchors. The historical
        // armies fought together: these are traces of a broken allied line.
        int[][] zones = {
                {-43, -24, 8, 2},  // regular-army broken line
                {-37,  36, 8, 1},  // regular fallback
                { -6, -41, 9, 2},  // Soul shield holdout
                { 39, -29, 9, 1},  // Soul rearward loss
                { 37,  34,10, 2},  // Ink Hunters' old flank
                {  8,  47,10, 1}   // scattered hunter remnants
        };
        for (int zone = 0; zone < zones.length; zone++) {
            int[] spec = zones[zone];
            int made = 0;
            for (int attempt = 0; attempt < 96 && made < spec[3]; attempt++) {
                int roll = field.hash(zone * 151 + attempt * 3, zone * 193 - attempt, 0xB4A2);
                double angle = (roll & 65535) * (Math.PI * 2 / 65536d);
                double distance = 2d + ((roll >>> 16) & 255) / 255d * 17d;
                double forward = spec[0] + Math.cos(angle) * distance;
                double right = spec[1] + Math.sin(angle) * distance;
                int x = field.x + (int) Math.round(field.inwardX * forward - field.inwardZ * right);
                int z = field.z + (int) Math.round(field.inwardZ * forward + field.inwardX * right);
                if (field.openness(x, z) < .50
                        || BattlefieldAccessRoutes.nearCorridor(field, x, z, 4)) continue;
                boolean occupied = false;
                for (Placement other : placements) {
                    double gap = switch (other.kind()) {
                        case MONUMENT -> 17d;
                        case SCENE, TRACE -> 10d;
                        case RELIC -> other.blade() >= 8 ? 12d : 8d;
                    };
                    if ((long)(x - other.x()) * (x - other.x())
                            + (long)(z - other.z()) * (z - other.z()) < gap * gap) {
                        occupied = true;
                        break;
                    }
                }
                if (occupied) continue;
                placements.add(new Placement(placements.size(), Kind.RELIC, x, z, field.axis(),
                        null, (roll >>> 9) & 7, spec[2], false));
                made++;
            }
        }
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
