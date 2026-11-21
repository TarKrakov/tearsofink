package com.tarkrakov.tearsofink.world.battlefield;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact cuboids and face UVs of the author's regular-army pole without fabric.
 * Shared by client renderer and server placement safety. Source art stays untouched.
 */
public final class BattlefieldBannerGeometry {
    public static final float UV_SIZE = 128f;
    public record Cuboid(float x0, float y0, float z0, float x1, float y1, float z1, float[][] uv) {}
    public static final Cuboid[] PARTS = {
        new Cuboid(-1f, 0f, -1.25f, 1f, 74f, 1.25f, new float[][] {{6f, 47f, 8f, 121f}, {0f, 40f, 2.5f, 114f}, {8f, 47f, 10f, 121f}, {3f, 40f, 5.5f, 114f}, {27f, 77f, 29f, 79.5f}, {32f, 77f, 34f, 79.5f}}),
        new Cuboid(-4f, 27f, -1f, 4f, 29f, 1f, new float[][] {{68f, 60f, 76f, 62f}, {18f, 70f, 20f, 72f}, {68f, 62f, 76f, 64f}, {51f, 74f, 53f, 76f}, {65f, 68f, 73f, 70f}, {70f, 12f, 78f, 14f}}),
        new Cuboid(-9f, 49f, -1f, 9f, 51f, 1f, new float[][] {{58f, 38f, 76f, 40f}, {34f, 77f, 36f, 79f}, {60f, 0f, 78f, 2f}, {36f, 77f, 38f, 79f}, {60f, 2f, 78f, 4f}, {60f, 4f, 78f, 6f}}),
        new Cuboid(-17f, 70f, -1f, 17f, 72f, 1f, new float[][] {{10f, 47f, 44f, 49f}, {38f, 77f, 40f, 79f}, {44f, 47f, 78f, 49f}, {77f, 51f, 79f, 53f}, {10f, 49f, 44f, 51f}, {44f, 49f, 78f, 51f}}),
        new Cuboid(-5f, 74f, -3f, 5f, 76f, 3f, new float[][] {{57f, 66f, 67f, 68f}, {24f, 72f, 30f, 74f}, {67f, 66f, 77f, 68f}, {72f, 35f, 78f, 37f}, {50f, 0f, 60f, 6f}, {50f, 6f, 60f, 12f}}),
        new Cuboid(10f, 72f, -4f, 12f, 74f, 2f, new float[][] {{77f, 56f, 79f, 58f}, {10f, 73f, 16f, 75f}, {57f, 77f, 59f, 79f}, {16f, 73f, 22f, 75f}, {53f, 70f, 55f, 76f}, {55f, 70f, 57f, 76f}}),
        new Cuboid(-13f, 72f, -4f, -11f, 74f, 2f, new float[][] {{59f, 77f, 61f, 79f}, {73f, 40f, 79f, 42f}, {61f, 77f, 63f, 79f}, {57f, 73f, 63f, 75f}, {30f, 72f, 32f, 78f}, {45f, 72f, 47f, 78f}}),
        new Cuboid(-6f, 76f, -3f, -2f, 78f, 3f, new float[][] {{74f, 54f, 78f, 56f}, {63f, 73f, 69f, 75f}, {10f, 75f, 14f, 77f}, {73f, 68f, 79f, 70f}, {10f, 66f, 14f, 72f}, {21f, 66f, 25f, 72f}}),
        new Cuboid(2f, 76f, -3f, 6f, 78f, 3f, new float[][] {{14f, 75f, 18f, 77f}, {69f, 73f, 75f, 75f}, {18f, 75f, 22f, 77f}, {74f, 14f, 80f, 16f}, {25f, 66f, 29f, 72f}, {29f, 66f, 33f, 72f}}),
        new Cuboid(2f, 78f, 1f, 6f, 80f, 5f, new float[][] {{75f, 30f, 79f, 32f}, {32f, 75f, 36f, 77f}, {36f, 75f, 40f, 77f}, {75f, 43f, 79f, 45f}, {70f, 14f, 74f, 18f}, {70f, 18f, 74f, 22f}}),
        new Cuboid(2f, 80f, 2f, 6f, 82f, 6f, new float[][] {{57f, 75f, 61f, 77f}, {61f, 75f, 65f, 77f}, {65f, 75f, 69f, 77f}, {69f, 75f, 73f, 77f}, {70f, 22f, 74f, 26f}, {70f, 26f, 74f, 30f}}),
        new Cuboid(2f, 82f, 0f, 6f, 85f, 5f, new float[][] {{74f, 16f, 78f, 19f}, {70f, 30f, 75f, 33f}, {74f, 19f, 78f, 22f}, {70f, 43f, 75f, 46f}, {14f, 67f, 18f, 72f}, {45f, 67f, 49f, 72f}}),
        new Cuboid(-6f, 82f, 0f, -2f, 85f, 5f, new float[][] {{22f, 74f, 26f, 77f}, {65f, 70f, 70f, 73f}, {74f, 22f, 78f, 25f}, {70f, 70f, 75f, 73f}, {57f, 68f, 61f, 73f}, {61f, 68f, 65f, 73f}}),
        new Cuboid(-6f, 80f, 2f, -2f, 82f, 6f, new float[][] {{75f, 70f, 79f, 72f}, {75f, 72f, 79f, 74f}, {73f, 75f, 77f, 77f}, {76f, 38f, 80f, 40f}, {33f, 70f, 37f, 74f}, {37f, 70f, 41f, 74f}}),
        new Cuboid(-6f, 78f, 1f, -2f, 80f, 5f, new float[][] {{51f, 76f, 55f, 78f}, {76f, 60f, 80f, 62f}, {76f, 62f, 80f, 64f}, {10f, 77f, 14f, 79f}, {41f, 70f, 45f, 74f}, {49f, 70f, 53f, 74f}}),
        new Cuboid(-4f, 84f, -2f, 4f, 91f, 3f, new float[][] {{30f, 51f, 38f, 58f}, {16f, 60f, 21f, 67f}, {38f, 51f, 46f, 58f}, {46f, 60f, 51f, 67f}, {58f, 33f, 66f, 38f}, {38f, 58f, 46f, 63f}}),
        new Cuboid(-1f, 89f, -11f, 1f, 92f, -7f, new float[][] {{49f, 67f, 51f, 70f}, {74f, 25f, 78f, 28f}, {17f, 77f, 19f, 80f}, {26f, 74f, 30f, 77f}, {40f, 75f, 42f, 79f}, {55f, 76f, 57f, 80f}}),
        new Cuboid(-3f, 89f, -7f, 3f, 93f, -1f, new float[][] {{33f, 66f, 39f, 70f}, {66f, 33f, 72f, 37f}, {39f, 66f, 45f, 70f}, {51f, 66f, 57f, 70f}, {60f, 6f, 66f, 12f}, {10f, 60f, 16f, 66f}}),
        new Cuboid(-24f, 95f, 0f, -4f, 98f, 3f, new float[][] {{50f, 12f, 70f, 15f}, {27f, 54f, 30f, 57f}, {50f, 15f, 70f, 18f}, {43f, 63f, 46f, 66f}, {50f, 18f, 70f, 21f}, {50f, 21f, 70f, 24f}}),
        new Cuboid(4f, 95f, 0f, 24f, 98f, 3f, new float[][] {{50f, 24f, 70f, 27f}, {18f, 67f, 21f, 70f}, {50f, 27f, 70f, 30f}, {74f, 51f, 77f, 54f}, {50f, 30f, 70f, 33f}, {10f, 51f, 30f, 54f}}),
        new Cuboid(-21f, 92f, 0.5f, -4f, 95f, 2.5f, new float[][] {{46f, 51f, 63f, 54f}, {19f, 77f, 21f, 80f}, {10f, 54f, 27f, 57f}, {21f, 77f, 23f, 80f}, {51f, 60f, 68f, 62f}, {60f, 57f, 77f, 59f}}),
        new Cuboid(-18f, 89f, 1f, -4f, 92f, 2f, new float[][] {{56f, 43f, 70f, 46f}, {29f, 77f, 30f, 80f}, {10f, 57f, 24f, 60f}, {73f, 77f, 74f, 80f}, {66f, 37f, 80f, 38f}, {10f, 72f, 24f, 73f}}),
        new Cuboid(-15f, 86f, 1f, -4f, 89f, 2f, new float[][] {{21f, 63f, 32f, 66f}, {74f, 77f, 75f, 80f}, {32f, 63f, 43f, 66f}, {77f, 74f, 78f, 77f}, {73f, 42f, 84f, 43f}, {74f, 28f, 85f, 29f}}),
        new Cuboid(4f, 92f, 0.5f, 21f, 95f, 2.5f, new float[][] {{46f, 54f, 63f, 57f}, {23f, 77f, 25f, 80f}, {56f, 40f, 73f, 43f}, {25f, 77f, 27f, 80f}, {21f, 61f, 38f, 63f}, {51f, 62f, 68f, 64f}}),
        new Cuboid(4f, 89f, 1f, 18f, 92f, 2f, new float[][] {{46f, 57f, 60f, 60f}, {75f, 77f, 76f, 80f}, {24f, 58f, 38f, 61f}, {76f, 77f, 77f, 80f}, {72f, 33f, 86f, 34f}, {72f, 34f, 86f, 35f}}),
        new Cuboid(4f, 86f, 1f, 15f, 89f, 2f, new float[][] {{63f, 51f, 74f, 54f}, {77f, 77f, 78f, 80f}, {63f, 54f, 74f, 57f}, {78f, 0f, 79f, 3f}, {74f, 29f, 85f, 30f}, {32f, 74f, 43f, 75f}})
    };
    private static final Map<Direction, List<BlockPos>> FOOTPRINTS = buildFootprints();

    private BattlefieldBannerGeometry() {}

    public static List<BlockPos> footprint(Direction facing) {
        List<BlockPos> cells = FOOTPRINTS.get(facing);
        if (cells == null) throw new IllegalArgumentException("Horizontal banner facing required");
        return cells;
    }

    private static Map<Direction, List<BlockPos>> buildFootprints() {
        Map<Direction, List<BlockPos>> result = new EnumMap<>(Direction.class);
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            Set<BlockPos> cells = new HashSet<>();
            for (Cuboid b : PARTS) {
                float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
                float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
                for (float x : new float[] {b.x0(), b.x1()}) {
                    for (float z : new float[] {b.z0(), b.z1()}) {
                        // Matches renderer: centered pole and Y rotation (180 - facing.toYRot).
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
                        minX = Math.min(minX, 0.5f + rx / 16f);
                        maxX = Math.max(maxX, 0.5f + rx / 16f);
                        minZ = Math.min(minZ, 0.5f + rz / 16f);
                        maxZ = Math.max(maxZ, 0.5f + rz / 16f);
                    }
                }
                // Half-open upper bounds: a face exactly on a cell edge doesn't occupy next cell.
                float epsilon = 0.00001f;
                for (int x = (int) Math.floor(minX + epsilon); x <= (int) Math.floor(maxX - epsilon); x++) {
                    for (int y = (int) Math.floor(b.y0() / 16f + epsilon); y <= (int) Math.floor(b.y1() / 16f - epsilon); y++) {
                        for (int z = (int) Math.floor(minZ + epsilon); z <= (int) Math.floor(maxZ - epsilon); z++) {
                            cells.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
            result.put(facing, List.copyOf(cells));
        }
        return Map.copyOf(result);
    }
}
