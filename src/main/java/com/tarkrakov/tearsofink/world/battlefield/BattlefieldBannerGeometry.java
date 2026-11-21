package com.tarkrakov.tearsofink.world.battlefield;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author's full vertical regular-army standard: all 31 original BBModel cubes,
 * 26 in pole and five in a top-jointed cloth chain.
 * Shared between client rendering and server safety reservation (four facings).
 * Source: blockbench/battlefielf_regular_standart1.bbmodel; do not modify author art.
 */
public final class BattlefieldBannerGeometry {
    public static final float UV_SIZE = 128f; // BB texture uv_width=128; PNG is 256px.
    public static final float WIND_CYCLE_TICKS = 80f;
    public static final float WIND_PHASE_STEP = 0.65f;
    // D-130: a steady *outward* lean keeps the vertical cloth clear of its own pole.
    // The squared positive wind impulse creates a stronger forward billow and a
    // gentle return that never folds backwards across the rigid shaft.
    public static final float[] WIND_BASE_LEAN = {0.025f, 0.015f, 0.014f, 0.012f, 0.010f};
    public static final float[] WIND_OUTWARD_RANGE = {0.115f, 0.070f, 0.080f, 0.090f, 0.100f};
    public static final float[][] TOP_HINGES = {{0f, 72f, -2.5f}, {0f, 62f, -2.5f}, {0f, 51f, -2.5f}, {0f, 40f, -2.5f}, {0f, 33f, -2.5f}}; // world-space model pixels
    public static final int[] POLE_PARTS = {0, 1, 2, 3, 4, 29, 30, 5, 6, 7, 8, 9, 11, 12, 13, 10, 16, 17, 14, 15, 18, 21, 22, 19, 20, 23};
    public static final int[] CLOTH_PARTS = {24, 25, 26, 27, 28};

    public record Cuboid(float x0, float y0, float z0, float x1, float y1, float z1, float[][] uv) {}

    public static final Cuboid[] PARTS = {
        // 0: pole
        new Cuboid(-1f, 0f, -1.25f, 1f, 74f, 1.25f, new float[][] {{6f, 47f, 8f, 121f}, {0f, 40f, 2.5f, 114f}, {8f, 47f, 10f, 121f}, {3f, 40f, 5.5f, 114f}, {27f, 77f, 29f, 79.5f}, {32f, 77f, 34f, 79.5f}}),
        // 1: pole
        new Cuboid(-4f, 27f, -1f, 4f, 29f, 1f, new float[][] {{68f, 60f, 76f, 62f}, {18f, 70f, 20f, 72f}, {68f, 62f, 76f, 64f}, {51f, 74f, 53f, 76f}, {65f, 68f, 73f, 70f}, {70f, 12f, 78f, 14f}}),
        // 2: pole
        new Cuboid(-9f, 49f, -1f, 9f, 51f, 1f, new float[][] {{58f, 38f, 76f, 40f}, {34f, 77f, 36f, 79f}, {60f, 0f, 78f, 2f}, {36f, 77f, 38f, 79f}, {60f, 2f, 78f, 4f}, {60f, 4f, 78f, 6f}}),
        // 3: pole
        new Cuboid(-17f, 70f, -1f, 17f, 72f, 1f, new float[][] {{10f, 47f, 44f, 49f}, {38f, 77f, 40f, 79f}, {44f, 47f, 78f, 49f}, {77f, 51f, 79f, 53f}, {10f, 49f, 44f, 51f}, {44f, 49f, 78f, 51f}}),
        // 4: pole
        new Cuboid(-5f, 74f, -3f, 5f, 76f, 3f, new float[][] {{57f, 66f, 67f, 68f}, {24f, 72f, 30f, 74f}, {67f, 66f, 77f, 68f}, {72f, 35f, 78f, 37f}, {50f, 0f, 60f, 6f}, {50f, 6f, 60f, 12f}}),
        // 5: pole
        new Cuboid(-6f, 76f, -3f, -2f, 78f, 3f, new float[][] {{74f, 54f, 78f, 56f}, {63f, 73f, 69f, 75f}, {10f, 75f, 14f, 77f}, {73f, 68f, 79f, 70f}, {10f, 66f, 14f, 72f}, {21f, 66f, 25f, 72f}}),
        // 6: pole
        new Cuboid(2f, 76f, -3f, 6f, 78f, 3f, new float[][] {{14f, 75f, 18f, 77f}, {69f, 73f, 75f, 75f}, {18f, 75f, 22f, 77f}, {74f, 14f, 80f, 16f}, {25f, 66f, 29f, 72f}, {29f, 66f, 33f, 72f}}),
        // 7: pole
        new Cuboid(2f, 78f, 1f, 6f, 80f, 5f, new float[][] {{75f, 30f, 79f, 32f}, {32f, 75f, 36f, 77f}, {36f, 75f, 40f, 77f}, {75f, 43f, 79f, 45f}, {70f, 14f, 74f, 18f}, {70f, 18f, 74f, 22f}}),
        // 8: pole
        new Cuboid(2f, 80f, 2f, 6f, 82f, 6f, new float[][] {{57f, 75f, 61f, 77f}, {61f, 75f, 65f, 77f}, {65f, 75f, 69f, 77f}, {69f, 75f, 73f, 77f}, {70f, 22f, 74f, 26f}, {70f, 26f, 74f, 30f}}),
        // 9: pole
        new Cuboid(2f, 82f, 0f, 6f, 85f, 5f, new float[][] {{74f, 16f, 78f, 19f}, {70f, 30f, 75f, 33f}, {74f, 19f, 78f, 22f}, {70f, 43f, 75f, 46f}, {14f, 67f, 18f, 72f}, {45f, 67f, 49f, 72f}}),
        // 10: pole
        new Cuboid(-4f, 84f, -2f, 4f, 91f, 3f, new float[][] {{30f, 51f, 38f, 58f}, {16f, 60f, 21f, 67f}, {38f, 51f, 46f, 58f}, {46f, 60f, 51f, 67f}, {58f, 33f, 66f, 38f}, {38f, 58f, 46f, 63f}}),
        // 11: pole
        new Cuboid(-6f, 82f, 0f, -2f, 85f, 5f, new float[][] {{22f, 74f, 26f, 77f}, {65f, 70f, 70f, 73f}, {74f, 22f, 78f, 25f}, {70f, 70f, 75f, 73f}, {57f, 68f, 61f, 73f}, {61f, 68f, 65f, 73f}}),
        // 12: pole
        new Cuboid(-6f, 80f, 2f, -2f, 82f, 6f, new float[][] {{75f, 70f, 79f, 72f}, {75f, 72f, 79f, 74f}, {73f, 75f, 77f, 77f}, {76f, 38f, 80f, 40f}, {33f, 70f, 37f, 74f}, {37f, 70f, 41f, 74f}}),
        // 13: pole
        new Cuboid(-6f, 78f, 1f, -2f, 80f, 5f, new float[][] {{51f, 76f, 55f, 78f}, {76f, 60f, 80f, 62f}, {76f, 62f, 80f, 64f}, {10f, 77f, 14f, 79f}, {41f, 70f, 45f, 74f}, {49f, 70f, 53f, 74f}}),
        // 14: pole
        new Cuboid(-24f, 95f, 0f, -4f, 98f, 3f, new float[][] {{50f, 12f, 70f, 15f}, {27f, 54f, 30f, 57f}, {50f, 15f, 70f, 18f}, {43f, 63f, 46f, 66f}, {50f, 18f, 70f, 21f}, {50f, 21f, 70f, 24f}}),
        // 15: pole
        new Cuboid(4f, 95f, 0f, 24f, 98f, 3f, new float[][] {{50f, 24f, 70f, 27f}, {18f, 67f, 21f, 70f}, {50f, 27f, 70f, 30f}, {74f, 51f, 77f, 54f}, {50f, 30f, 70f, 33f}, {10f, 51f, 30f, 54f}}),
        // 16: pole
        new Cuboid(-1f, 89f, -11f, 1f, 92f, -7f, new float[][] {{49f, 67f, 51f, 70f}, {74f, 25f, 78f, 28f}, {17f, 77f, 19f, 80f}, {26f, 74f, 30f, 77f}, {40f, 75f, 42f, 79f}, {55f, 76f, 57f, 80f}}),
        // 17: pole
        new Cuboid(-3f, 89f, -7f, 3f, 93f, -1f, new float[][] {{33f, 66f, 39f, 70f}, {66f, 33f, 72f, 37f}, {39f, 66f, 45f, 70f}, {51f, 66f, 57f, 70f}, {60f, 6f, 66f, 12f}, {10f, 60f, 16f, 66f}}),
        // 18: pole
        new Cuboid(-21f, 92f, 0.5f, -4f, 95f, 2.5f, new float[][] {{46f, 51f, 63f, 54f}, {19f, 77f, 21f, 80f}, {10f, 54f, 27f, 57f}, {21f, 77f, 23f, 80f}, {51f, 60f, 68f, 62f}, {60f, 57f, 77f, 59f}}),
        // 19: pole
        new Cuboid(4f, 92f, 0.5f, 21f, 95f, 2.5f, new float[][] {{46f, 54f, 63f, 57f}, {23f, 77f, 25f, 80f}, {56f, 40f, 73f, 43f}, {25f, 77f, 27f, 80f}, {21f, 61f, 38f, 63f}, {51f, 62f, 68f, 64f}}),
        // 20: pole
        new Cuboid(4f, 89f, 1f, 18f, 92f, 2f, new float[][] {{46f, 57f, 60f, 60f}, {75f, 77f, 76f, 80f}, {24f, 58f, 38f, 61f}, {76f, 77f, 77f, 80f}, {72f, 33f, 86f, 34f}, {72f, 34f, 86f, 35f}}),
        // 21: pole
        new Cuboid(-18f, 89f, 1f, -4f, 92f, 2f, new float[][] {{56f, 43f, 70f, 46f}, {29f, 77f, 30f, 80f}, {10f, 57f, 24f, 60f}, {73f, 77f, 74f, 80f}, {66f, 37f, 80f, 38f}, {10f, 72f, 24f, 73f}}),
        // 22: pole
        new Cuboid(-15f, 86f, 1f, -4f, 89f, 2f, new float[][] {{21f, 63f, 32f, 66f}, {74f, 77f, 75f, 80f}, {32f, 63f, 43f, 66f}, {77f, 74f, 78f, 77f}, {73f, 42f, 84f, 43f}, {74f, 28f, 85f, 29f}}),
        // 23: pole
        new Cuboid(4f, 86f, 1f, 15f, 89f, 2f, new float[][] {{63f, 51f, 74f, 54f}, {77f, 77f, 78f, 80f}, {63f, 54f, 74f, 57f}, {78f, 0f, 79f, 3f}, {74f, 29f, 85f, 30f}, {32f, 74f, 43f, 75f}}),
        // 24: flag_root
        new Cuboid(-14f, 62f, -3f, 13f, 73f, -2f, new float[][] {{0f, 0f, 27f, 11f}, {47f, 72f, 48f, 83f}, {0f, 11f, 27f, 22f}, {48f, 72f, 49f, 83f}, {51f, 64f, 78f, 65f}, {51f, 65f, 78f, 66f}}),
        // 25: flag1
        new Cuboid(-13f, 51f, -3f, 12f, 62f, -2f, new float[][] {{0f, 22f, 25f, 33f}, {49f, 74f, 50f, 85f}, {25f, 22f, 50f, 33f}, {50f, 74f, 51f, 85f}, {66f, 8f, 91f, 9f}, {66f, 9f, 91f, 10f}}),
        // 26: flag2
        new Cuboid(-12f, 40f, -3f, 11f, 51f, -2f, new float[][] {{27f, 0f, 50f, 11f}, {43f, 74f, 44f, 85f}, {27f, 11f, 50f, 22f}, {44f, 74f, 45f, 85f}, {66f, 10f, 89f, 11f}, {66f, 11f, 89f, 12f}}),
        // 27: flag3
        new Cuboid(-13f, 33f, -3f, 12f, 40f, -2f, new float[][] {{6f, 40f, 31f, 47f}, {15f, 77f, 16f, 84f}, {31f, 40f, 56f, 47f}, {16f, 77f, 17f, 84f}, {66f, 6f, 91f, 7f}, {66f, 7f, 91f, 8f}}),
        // 28: flag4
        new Cuboid(-15f, 26f, -3f, 14f, 33f, -2f, new float[][] {{0f, 33f, 29f, 40f}, {42f, 75f, 43f, 82f}, {29f, 33f, 58f, 40f}, {14f, 77f, 15f, 84f}, {56f, 46f, 85f, 47f}, {60f, 59f, 89f, 60f}}),
        // 29: pole
        new Cuboid(10f, 72f, -4f, 12f, 74f, 2f, new float[][] {{77f, 56f, 79f, 58f}, {10f, 73f, 16f, 75f}, {57f, 77f, 59f, 79f}, {16f, 73f, 22f, 75f}, {53f, 70f, 55f, 76f}, {55f, 70f, 57f, 76f}}),
        // 30: pole
        new Cuboid(-13f, 72f, -4f, -11f, 74f, 2f, new float[][] {{59f, 77f, 61f, 79f}, {73f, 40f, 79f, 42f}, {61f, 77f, 63f, 79f}, {57f, 73f, 63f, 75f}, {30f, 72f, 32f, 78f}, {45f, 72f, 47f, 78f}})
    };
    private static final Map<Direction, List<BlockPos>> FOOTPRINTS = makeFootprints();

    private BattlefieldBannerGeometry() {}

    /** Shared wind angle for renderer and server-side swept envelope. */
    public static float windAngle(int joint, double phase) {
        double wind = (1d + Math.sin(phase - joint * WIND_PHASE_STEP)) * 0.5d;
        return WIND_BASE_LEAN[joint] + WIND_OUTWARD_RANGE[joint] * (float) (wind * wind);
    }

    /** Cached occupied cells for placement, including a swept wind envelope. */
    public static List<BlockPos> footprint(Direction facing) {
        List<BlockPos> cells = FOOTPRINTS.get(facing);
        if (cells == null) throw new IllegalArgumentException("Horizontal facing required: " + facing);
        return cells;
    }

    /**
     * Conservative sweep over all wind phases, without server animation or ticking.
     * Cloth Y/Z is rotated around true Blockbench TOP pivots, deepest pivot first
     * when transforming each rendered point (same chain as PoseStack on client).
     */
    private static Map<Direction, List<BlockPos>> makeFootprints() {
        Map<Direction, List<BlockPos>> result = new EnumMap<>(Direction.class);
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            Set<BlockPos> cells = new HashSet<>();
            for (int part : POLE_PARTS) {
                Cuboid box = PARTS[part];
                addCells(cells, facing, box.x0(), box.x1(), box.y0(), box.y1(), box.z0(), box.z1());
            }
            for (int sample = 0; sample < 256; sample++) {
                double phase = sample * (Math.PI * 2 / 256d);
                for (int i = 0; i < CLOTH_PARTS.length; i++) {
                    Cuboid box = PARTS[CLOTH_PARTS[i]];
                    float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
                    float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
                    for (float y : new float[] {box.y0(), box.y1()}) {
                        for (float z : new float[] {box.z0(), box.z1()}) {
                            double yy = y, zz = z;
                            for (int joint = i; joint >= 0; joint--) {
                                float[] pivot = TOP_HINGES[joint];
                                double angle = windAngle(joint, phase);
                                double c = Math.cos(angle), sn = Math.sin(angle);
                                double dy = yy - pivot[1], dz = zz - pivot[2];
                                yy = pivot[1] + dy * c - dz * sn;
                                zz = pivot[2] + dy * sn + dz * c;
                            }
                            minY = Math.min(minY, (float) yy);
                            maxY = Math.max(maxY, (float) yy);
                            minZ = Math.min(minZ, (float) zz);
                            maxZ = Math.max(maxZ, (float) zz);
                        }
                    }
                    // One model pixel of allowance covers inter-sample extrema / aliasing.
                    addCells(cells, facing, box.x0(), box.x1(), minY - 1f, maxY + 1f, minZ - 1f, maxZ + 1f);
                }
            }
            result.put(facing, List.copyOf(cells));
        }
        return Map.copyOf(result);
    }

    private static void addCells(Set<BlockPos> cells, Direction facing,
                                 float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        float worldMinX = Float.POSITIVE_INFINITY, worldMaxX = Float.NEGATIVE_INFINITY;
        float worldMinZ = Float.POSITIVE_INFINITY, worldMaxZ = Float.NEGATIVE_INFINITY;
        for (float x : new float[] {minX, maxX}) {
            for (float z : new float[] {minZ, maxZ}) {
                // Matches renderer: translate(.5,0,.5), Y rotation(180-facing.toYRot()).
                float rx = switch (facing) {
                    case NORTH -> x;
                    case SOUTH -> -x;
                    case EAST -> -z;
                    case WEST -> z;
                    default -> throw new IllegalStateException();
                };
                float rz = switch (facing) {
                    case NORTH -> z;
                    case SOUTH -> -z;
                    case EAST -> x;
                    case WEST -> -x;
                    default -> throw new IllegalStateException();
                };
                worldMinX = Math.min(worldMinX, .5f + rx / 16f);
                worldMaxX = Math.max(worldMaxX, .5f + rx / 16f);
                worldMinZ = Math.min(worldMinZ, .5f + rz / 16f);
                worldMaxZ = Math.max(worldMaxZ, .5f + rz / 16f);
            }
        }
        final float epsilon = .00001f;
        for (int x = (int) Math.floor(worldMinX + epsilon); x <= (int) Math.floor(worldMaxX - epsilon); x++) {
            for (int y = (int) Math.floor(minY / 16f + epsilon); y <= (int) Math.floor(maxY / 16f - epsilon); y++) {
                for (int z = (int) Math.floor(worldMinZ + epsilon); z <= (int) Math.floor(worldMaxZ - epsilon); z++) {
                    cells.add(new BlockPos(x, y, z));
                }
            }
        }
    }
}
