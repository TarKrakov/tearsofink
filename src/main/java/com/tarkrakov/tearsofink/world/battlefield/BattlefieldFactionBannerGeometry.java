package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.block.BattlefieldBannerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exact UV-matched authored Soul and Ink Hunter banner cuboids and bone pivots.
 * No new visual assets. Separate full and bare geometry, independent of Spire.
 * Bare BBModels mistakenly report UV canvases 64 and 16 although their faces
 * exactly match the full rigs' 128-UV atlas, backed by identical original PNGs.
 * Use 128 for all four to preserve the authored texture.
 */
public final class BattlefieldFactionBannerGeometry {
    private BattlefieldFactionBannerGeometry() {}
    public static final float WIND_CYCLE_TICKS = 90f;
    public record Joint(float x,float y,float z,int depth,float phaseShift){}
    public record Part(BattlefieldBannerGeometry.Cuboid cube,int[] joints){}
    public record Model(float uvSize,Joint[] joints,Part[] parts){}
    private enum Kind {SOUL_FULL,SOUL_BARE,HUNTER_FULL,HUNTER_BARE}

    /** blockbench/battlefield_soul_flag_1.bbmodel : 24 exact cuboids, 15 authored cloth joints. */
    private static final Model SOUL_FULL = new Model(128f, new Joint[]{
            new Joint(-1f, 66f, 0f, 0, 0.01429f),
            new Joint(-1f, 66f, -0.5f, 1, 0.01429f),
            new Joint(-10f, 66f, -0.5f, 2, 0.01429f),
            new Joint(-21f, 72.5f, -0.5f, 3, 0.10714f),
            new Joint(-32f, 72.5f, -0.5f, 4, 0.10714f),
            new Joint(-32f, 72.5f, -0.5f, 5, 0.10714f),
            new Joint(-43f, 72.5f, -0.5f, 6, 0.10714f),
            new Joint(-21f, 66f, -0.5f, 3, 0.01429f),
            new Joint(-21f, 65f, -0.5f, 4, 0f),
            new Joint(-32f, 66f, -0.5f, 5, 0.01429f),
            new Joint(-43f, 65.5f, -0.5f, 6, 0.00714f),
            new Joint(-21f, 58.5f, -0.5f, 3, -0.09286f),
            new Joint(-21f, 58.5f, -0.5f, 4, -0.09286f),
            new Joint(-32f, 58.5f, -0.5f, 5, -0.09286f),
            new Joint(-43f, 58.5f, -0.5f, 5, -0.09286f)
    }, new Part[]{
            // 0 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-1f, 0f, -1f, 1f, 79f, 1f, new float[][]{{9f, 22f, 11f, 101f}, {11f, 22f, 13f, 101f}, {13f, 22f, 15f, 101f}, {15f, 22f, 17f, 101f}, {44f, 49f, 46f, 51f}, {46f, 49f, 48f, 51f}}), new int[]{}),
            // 1 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-9.9f, 53f, -0.5f, -0.9f, 79f, 0.5f, new float[][]{{0f, 22f, 9f, 48f}, {39f, 32f, 40f, 58f}, {22f, 0f, 31f, 26f}, {17f, 40f, 18f, 66f}, {0f, 48f, 9f, 49f}, {48f, 48f, 57f, 49f}}), new int[]{0, 1}),
            // 2 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-20.9f, 55f, -0.5f, -9.9f, 77f, 0.5f, new float[][]{{0f, 0f, 11f, 22f}, {18f, 40f, 19f, 62f}, {11f, 0f, 22f, 22f}, {19f, 40f, 20f, 62f}, {46f, 16f, 57f, 17f}, {46f, 17f, 57f, 18f}}), new int[]{0, 1, 2}),
            // 3 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-31.9f, 70f, -0.5f, -20.9f, 76f, 0.5f, new float[][]{{17f, 26f, 28f, 32f}, {35f, 45f, 36f, 51f}, {28f, 26f, 39f, 32f}, {8f, 49f, 9f, 55f}, {46f, 4f, 57f, 5f}, {46f, 5f, 57f, 6f}}), new int[]{0, 1, 2, 3, 4}),
            // 4 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-52.9f, 72f, -0.5f, -42.9f, 74f, 0.5f, new float[][]{{42f, 20f, 52f, 22f}, {35f, 51f, 36f, 53f}, {42f, 22f, 52f, 24f}, {50f, 51f, 51f, 53f}, {48f, 34f, 58f, 35f}, {48f, 35f, 58f, 36f}}), new int[]{0, 1, 2, 3, 4, 5, 6}),
            // 5 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-42.9f, 71f, -0.5f, -31.9f, 75f, 0.5f, new float[][]{{17f, 32f, 28f, 36f}, {0f, 51f, 1f, 55f}, {28f, 32f, 39f, 36f}, {1f, 51f, 2f, 55f}, {46f, 6f, 57f, 7f}, {46f, 7f, 57f, 8f}}), new int[]{0, 1, 2, 3, 4, 5}),
            // 6 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-31.9f, 63f, -0.5f, -20.9f, 69f, 0.5f, new float[][]{{31f, 0f, 42f, 6f}, {40f, 49f, 41f, 55f}, {31f, 6f, 42f, 12f}, {41f, 49f, 42f, 55f}, {46f, 8f, 57f, 9f}, {46f, 9f, 57f, 10f}}), new int[]{0, 1, 2, 7, 8}),
            // 7 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-42.9f, 64f, -0.5f, -31.9f, 68f, 0.5f, new float[][]{{17f, 36f, 28f, 40f}, {2f, 51f, 3f, 55f}, {28f, 36f, 39f, 40f}, {3f, 51f, 4f, 55f}, {46f, 10f, 57f, 11f}, {46f, 11f, 57f, 12f}}), new int[]{0, 1, 2, 7, 8, 9}),
            // 8 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-52.9f, 65f, -0.5f, -42.9f, 67f, 0.5f, new float[][]{{44f, 37f, 54f, 39f}, {51f, 51f, 52f, 53f}, {44f, 39f, 54f, 41f}, {6f, 52f, 7f, 54f}, {48f, 36f, 58f, 37f}, {48f, 45f, 58f, 46f}}), new int[]{0, 1, 2, 7, 8, 9, 10}),
            // 9 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-52.9f, 58f, -0.5f, -42.9f, 60f, 0.5f, new float[][]{{44f, 41f, 54f, 43f}, {7f, 52f, 8f, 54f}, {44f, 43f, 54f, 45f}, {52f, 20f, 53f, 22f}, {48f, 46f, 58f, 47f}, {48f, 47f, 58f, 48f}}), new int[]{0, 1, 2, 11, 12, 14}),
            // 10 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-42.9f, 57f, -0.5f, -31.9f, 61f, 0.5f, new float[][]{{39f, 24f, 50f, 28f}, {4f, 51f, 5f, 55f}, {39f, 28f, 50f, 32f}, {5f, 51f, 6f, 55f}, {46f, 14f, 57f, 15f}, {46f, 15f, 57f, 16f}}), new int[]{0, 1, 2, 11, 12, 13}),
            // 11 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-31.9f, 56f, -0.5f, -20.9f, 62f, 0.5f, new float[][]{{31f, 12f, 42f, 18f}, {42f, 49f, 43f, 55f}, {31f, 18f, 42f, 24f}, {43f, 49f, 44f, 55f}, {46f, 12f, 57f, 13f}, {46f, 13f, 57f, 14f}}), new int[]{0, 1, 2, 11, 12}),
            // 12 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 79f, -2f, 2f, 84f, 2f, new float[][]{{20f, 40f, 24f, 45f}, {24f, 40f, 28f, 45f}, {28f, 40f, 32f, 45f}, {32f, 40f, 36f, 45f}, {17f, 22f, 21f, 26f}, {22f, 45f, 26f, 49f}}), new int[]{}),
            // 13 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-0.5f, 84f, -0.75f, 0.5f, 96f, 0.75f, new float[][]{{38f, 40f, 39f, 52f}, {36f, 40f, 37.5f, 52f}, {34f, 45f, 35f, 57f}, {20f, 45f, 21.5f, 57f}, {22f, 52f, 23f, 53.5f}, {52f, 22f, 53f, 23.5f}}), new int[]{}),
            // 14 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-5f, 90.5f, -0.5f, 5f, 91.5f, 0.5f, new float[][]{{46f, 18f, 56f, 19f}, {23f, 52f, 24f, 53f}, {46f, 19f, 56f, 20f}, {24f, 52f, 25f, 53f}, {48f, 32f, 58f, 33f}, {48f, 33f, 58f, 34f}}), new int[]{}),
            // 15 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 48f, -2f, 2f, 53f, 2f, new float[][]{{40f, 32f, 44f, 37f}, {40f, 37f, 44f, 42f}, {42f, 0f, 46f, 5f}, {42f, 5f, 46f, 10f}, {26f, 45f, 30f, 49f}, {30f, 45f, 34f, 49f}}), new int[]{}),
            // 16 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 22f, -2f, 2f, 27f, 2f, new float[][]{{42f, 10f, 46f, 15f}, {42f, 15f, 46f, 20f}, {40f, 42f, 44f, 47f}, {44f, 32f, 48f, 37f}, {44f, 45f, 48f, 49f}, {46f, 0f, 50f, 4f}}), new int[]{}),
            // 17 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-6f, 24f, -1f, -2f, 25f, 1f, new float[][]{{48f, 49f, 52f, 50f}, {6f, 51f, 8f, 52f}, {50f, 0f, 54f, 1f}, {22f, 51f, 24f, 52f}, {31f, 24f, 35f, 26f}, {35f, 24f, 39f, 26f}}), new int[]{}),
            // 18 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(2f, 24f, -1f, 6f, 25f, 1f, new float[][]{{50f, 1f, 54f, 2f}, {24f, 51f, 26f, 52f}, {50f, 2f, 54f, 3f}, {26f, 51f, 28f, 52f}, {40f, 47f, 44f, 49f}, {0f, 49f, 4f, 51f}}), new int[]{}),
            // 19 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-6f, 50f, -1f, -2f, 51f, 1f, new float[][]{{50f, 3f, 54f, 4f}, {51f, 27f, 53f, 28f}, {50f, 24f, 54f, 25f}, {28f, 51f, 30f, 52f}, {4f, 49f, 8f, 51f}, {22f, 49f, 26f, 51f}}), new int[]{}),
            // 20 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(2f, 50f, -1f, 6f, 51f, 1f, new float[][]{{50f, 25f, 54f, 26f}, {51f, 28f, 53f, 29f}, {50f, 26f, 54f, 27f}, {51f, 29f, 53f, 30f}, {26f, 49f, 30f, 51f}, {30f, 49f, 34f, 51f}}), new int[]{}),
            // 21 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-4f, 89f, -0.25f, -3f, 93f, 0.25f, new float[][]{{21f, 22f, 22f, 26f}, {30f, 51f, 30.5f, 55f}, {50f, 27f, 51f, 31f}, {31f, 51f, 31.5f, 55f}, {25f, 52f, 26f, 52.5f}, {26f, 52f, 27f, 52.5f}}), new int[]{}),
            // 22 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(3f, 89f, -0.25f, 4f, 93f, 0.25f, new float[][]{{48f, 50f, 49f, 54f}, {32f, 51f, 32.5f, 55f}, {49f, 50f, 50f, 54f}, {33f, 51f, 33.5f, 55f}, {27f, 52f, 28f, 52.5f}, {28f, 52f, 29f, 52.5f}}), new int[]{}),
            // 23 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 94f, -0.25f, 2f, 95f, 0.25f, new float[][]{{50f, 31f, 54f, 32f}, {29f, 52f, 29.5f, 53f}, {50f, 50f, 54f, 51f}, {36f, 52f, 36.5f, 53f}, {51f, 30f, 55f, 30.5f}, {44f, 51f, 48f, 51.5f}}), new int[]{})
    });

    /** blockbench/battlefield_soul_flag_withoutbannet1.bbmodel : 13 exact cuboids, 0 authored cloth joints. */
    private static final Model SOUL_BARE = new Model(128f, new Joint[]{
            
    }, new Part[]{
            // 0 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-1f, 0f, -1f, 1f, 79f, 1f, new float[][]{{9f, 22f, 11f, 101f}, {11f, 22f, 13f, 101f}, {13f, 22f, 15f, 101f}, {15f, 22f, 17f, 101f}, {44f, 49f, 46f, 51f}, {46f, 49f, 48f, 51f}}), new int[]{}),
            // 1 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 79f, -2f, 2f, 84f, 2f, new float[][]{{20f, 40f, 24f, 45f}, {24f, 40f, 28f, 45f}, {28f, 40f, 32f, 45f}, {32f, 40f, 36f, 45f}, {17f, 22f, 21f, 26f}, {22f, 45f, 26f, 49f}}), new int[]{}),
            // 2 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 48f, -2f, 2f, 53f, 2f, new float[][]{{40f, 32f, 44f, 37f}, {40f, 37f, 44f, 42f}, {42f, 0f, 46f, 5f}, {42f, 5f, 46f, 10f}, {26f, 45f, 30f, 49f}, {30f, 45f, 34f, 49f}}), new int[]{}),
            // 3 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 22f, -2f, 2f, 27f, 2f, new float[][]{{42f, 10f, 46f, 15f}, {42f, 15f, 46f, 20f}, {40f, 42f, 44f, 47f}, {44f, 32f, 48f, 37f}, {44f, 45f, 48f, 49f}, {46f, 0f, 50f, 4f}}), new int[]{}),
            // 4 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-6f, 24f, -1f, -2f, 25f, 1f, new float[][]{{48f, 49f, 52f, 50f}, {6f, 51f, 8f, 52f}, {50f, 0f, 54f, 1f}, {22f, 51f, 24f, 52f}, {31f, 24f, 35f, 26f}, {35f, 24f, 39f, 26f}}), new int[]{}),
            // 5 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(2f, 24f, -1f, 6f, 25f, 1f, new float[][]{{50f, 1f, 54f, 2f}, {24f, 51f, 26f, 52f}, {50f, 2f, 54f, 3f}, {26f, 51f, 28f, 52f}, {40f, 47f, 44f, 49f}, {0f, 49f, 4f, 51f}}), new int[]{}),
            // 6 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-6f, 50f, -1f, -2f, 51f, 1f, new float[][]{{50f, 3f, 54f, 4f}, {51f, 27f, 53f, 28f}, {50f, 24f, 54f, 25f}, {28f, 51f, 30f, 52f}, {4f, 49f, 8f, 51f}, {22f, 49f, 26f, 51f}}), new int[]{}),
            // 7 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(2f, 50f, -1f, 6f, 51f, 1f, new float[][]{{50f, 25f, 54f, 26f}, {51f, 28f, 53f, 29f}, {50f, 26f, 54f, 27f}, {51f, 29f, 53f, 30f}, {26f, 49f, 30f, 51f}, {30f, 49f, 34f, 51f}}), new int[]{}),
            // 8 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-0.5f, 84f, -1.25f, 0.5f, 96f, 0.25f, new float[][]{{38f, 40f, 39f, 52f}, {36f, 40f, 37.5f, 52f}, {34f, 45f, 35f, 57f}, {20f, 45f, 21.5f, 57f}, {22f, 52f, 23f, 53.5f}, {52f, 22f, 53f, 23.5f}}), new int[]{}),
            // 9 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-5f, 90.5f, -1f, 5f, 91.5f, 0f, new float[][]{{46f, 18f, 56f, 19f}, {23f, 52f, 24f, 53f}, {46f, 19f, 56f, 20f}, {24f, 52f, 25f, 53f}, {48f, 32f, 58f, 33f}, {48f, 33f, 58f, 34f}}), new int[]{}),
            // 10 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-4f, 89f, -0.75f, -3f, 93f, -0.25f, new float[][]{{21f, 22f, 22f, 26f}, {30f, 51f, 30.5f, 55f}, {50f, 27f, 51f, 31f}, {31f, 51f, 31.5f, 55f}, {25f, 52f, 26f, 52.5f}, {26f, 52f, 27f, 52.5f}}), new int[]{}),
            // 11 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(3f, 89f, -0.75f, 4f, 93f, -0.25f, new float[][]{{48f, 50f, 49f, 54f}, {32f, 51f, 32.5f, 55f}, {49f, 50f, 50f, 54f}, {33f, 51f, 33.5f, 55f}, {27f, 52f, 28f, 52.5f}, {28f, 52f, 29f, 52.5f}}), new int[]{}),
            // 12 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 94f, -0.75f, 2f, 95f, -0.25f, new float[][]{{50f, 31f, 54f, 32f}, {29f, 52f, 29.5f, 53f}, {50f, 50f, 54f, 51f}, {36f, 52f, 36.5f, 53f}, {51f, 30f, 55f, 30.5f}, {44f, 51f, 48f, 51.5f}}), new int[]{})
    });

    /** blockbench/battlefield_hunter_flag_1.bbmodel : 17 exact cuboids, 8 authored cloth joints. */
    private static final Model HUNTER_FULL = new Model(128f, new Joint[]{
            new Joint(-1f, 58f, 0f, 0, -0.1f),
            new Joint(-11f, 58f, 0f, 1, -0.1f),
            new Joint(-21f, 58f, 0f, 2, -0.1f),
            new Joint(-31f, 58f, 0f, 3, -0.1f),
            new Joint(-41f, 60.25f, 0f, 4, -0.06786f),
            new Joint(-51f, 60.25f, 0f, 5, -0.06786f),
            new Joint(-41f, 55.75f, 0f, 4, -0.13214f),
            new Joint(-51f, 55.75f, 0f, 5, -0.13214f)
    }, new Part[]{
            // 0 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 69f, -2f, 2f, 73f, 2f, new float[][]{{36f, 34f, 40f, 38f}, {32f, 37f, 36f, 41f}, {38f, 11f, 42f, 15f}, {38f, 15f, 42f, 19f}, {38f, 19f, 42f, 23f}, {38f, 23f, 42f, 27f}}), new int[]{}),
            // 1 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-11f, 49.5f, -0.5f, -1f, 66.5f, 0.5f, new float[][]{{0f, 0f, 10f, 17f}, {30f, 33f, 31f, 50f}, {10f, 0f, 20f, 17f}, {31f, 33f, 32f, 50f}, {40f, 10f, 50f, 11f}, {40f, 37f, 50f, 38f}}), new int[]{0}),
            // 2 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-21f, 51.5f, -0.5f, -11f, 64.5f, 0.5f, new float[][]{{8f, 17f, 18f, 30f}, {18f, 40f, 19f, 53f}, {18f, 17f, 28f, 30f}, {19f, 40f, 20f, 53f}, {40f, 38f, 50f, 39f}, {40f, 39f, 50f, 40f}}), new int[]{0, 1}),
            // 3 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-41f, 54f, -0.5f, -31f, 62f, 0.5f, new float[][]{{28f, 22f, 38f, 30f}, {32f, 41f, 33f, 49f}, {30f, 0f, 40f, 8f}, {33f, 41f, 34f, 49f}, {42f, 0f, 52f, 1f}, {42f, 1f, 52f, 2f}}), new int[]{0, 1, 2, 3}),
            // 4 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-31f, 52.5f, -0.5f, -21f, 63.5f, 0.5f, new float[][]{{20f, 0f, 30f, 11f}, {20f, 40f, 21f, 51f}, {28f, 11f, 38f, 22f}, {21f, 40f, 22f, 51f}, {40f, 40f, 50f, 41f}, {40f, 41f, 50f, 42f}}), new int[]{0, 1, 2}),
            // 5 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-51f, 59f, -0.5f, -41f, 61.5f, 0.5f, new float[][]{{30f, 8f, 40f, 10.5f}, {16f, 43f, 17f, 45.5f}, {16f, 30f, 26f, 32.5f}, {43f, 16f, 44f, 18.5f}, {42f, 2f, 52f, 3f}, {42f, 3f, 52f, 4f}}), new int[]{0, 1, 2, 3, 4}),
            // 6 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-51f, 54.5f, -0.5f, -41f, 57f, 0.5f, new float[][]{{26f, 30f, 36f, 32.5f}, {17f, 43f, 18f, 45.5f}, {16f, 33f, 26f, 35.5f}, {43f, 19f, 44f, 21.5f}, {42f, 6f, 52f, 7f}, {42f, 7f, 52f, 8f}}), new int[]{0, 1, 2, 3, 6}),
            // 7 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-61f, 59.5f, -0.5f, -51f, 61f, 0.5f, new float[][]{{8f, 40f, 18f, 41.5f}, {28f, 43f, 29f, 44.5f}, {40f, 8f, 50f, 9.5f}, {29f, 43f, 30f, 44.5f}, {42f, 4f, 52f, 5f}, {42f, 5f, 52f, 6f}}), new int[]{0, 1, 2, 3, 4, 5}),
            // 8 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-61f, 55f, -0.5f, -51f, 56.5f, 0.5f, new float[][]{{40f, 29f, 50f, 30.5f}, {8f, 44f, 9f, 45.5f}, {40f, 31f, 50f, 32.5f}, {9f, 44f, 10f, 45.5f}, {8f, 42f, 18f, 43f}, {42f, 11f, 52f, 12f}}), new int[]{0, 1, 2, 3, 6, 7}),
            // 9 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-1f, 0f, -1f, 1f, 71f, 1f, new float[][]{{0f, 17f, 2f, 88f}, {2f, 17f, 4f, 88f}, {4f, 17f, 6f, 88f}, {6f, 17f, 8f, 88f}, {12f, 43f, 14f, 45f}, {14f, 43f, 16f, 45f}}), new int[]{}),
            // 10 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-1f, 73f, -1f, 1f, 81f, 1f, new float[][]{{24f, 36f, 26f, 44f}, {36f, 38f, 38f, 46f}, {38f, 38f, 40f, 46f}, {40f, 0f, 42f, 8f}, {40f, 42f, 42f, 44f}, {42f, 42f, 44f, 44f}}), new int[]{}),
            // 11 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-4f, 77f, -0.9f, 4f, 78f, 0.9f, new float[][]{{42f, 12f, 50f, 13f}, {38f, 29f, 40f, 30f}, {42f, 13f, 50f, 14f}, {43f, 22f, 45f, 23f}, {20f, 15f, 28f, 17f}, {38f, 27f, 46f, 29f}}), new int[]{}),
            // 12 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-3f, 81f, -0.9f, 3f, 82f, 0.9f, new float[][]{{42f, 14f, 48f, 15f}, {43f, 23f, 45f, 24f}, {42f, 15f, 48f, 16f}, {43f, 24f, 45f, 25f}, {40f, 33f, 46f, 35f}, {40f, 35f, 46f, 37f}}), new int[]{}),
            // 13 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 82f, -0.4f, 2f, 92f, 0.4f, new float[][]{{8f, 30f, 12f, 40f}, {22f, 40f, 23f, 50f}, {12f, 30f, 16f, 40f}, {23f, 40f, 24f, 50f}, {42f, 26f, 46f, 27f}, {8f, 43f, 12f, 44f}}), new int[]{}),
            // 14 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-1f, 92f, -0.3f, 1f, 102f, 0.3f, new float[][]{{26f, 33f, 28f, 43f}, {34f, 41f, 34.5f, 51f}, {28f, 33f, 30f, 43f}, {35f, 41f, 35.5f, 51f}, {43f, 25f, 45f, 25.5f}, {10f, 44f, 12f, 44.5f}}), new int[]{}),
            // 15 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-0.5f, 102f, -0.2f, 0.5f, 107f, 0.2f, new float[][]{{42f, 16f, 43f, 21f}, {26f, 43f, 26.5f, 48f}, {42f, 21f, 43f, 26f}, {27f, 43f, 27.5f, 48f}, {44f, 16f, 45f, 16.5f}, {44f, 17f, 45f, 17.5f}}), new int[]{}),
            // 16 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 43f, -2f, 2f, 47f, 2f, new float[][]{{20f, 11f, 24f, 15f}, {24f, 11f, 28f, 15f}, {32f, 33f, 36f, 37f}, {16f, 36f, 20f, 40f}, {20f, 36f, 24f, 40f}, {36f, 30f, 40f, 34f}}), new int[]{})
    });

    /** blockbench/battlefield_hunter_flag_withoutbanner1.bbmodel : 9 exact cuboids, 0 authored cloth joints. */
    private static final Model HUNTER_BARE = new Model(128f, new Joint[]{
            
    }, new Part[]{
            // 0 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 69f, -2f, 2f, 73f, 2f, new float[][]{{36f, 34f, 40f, 38f}, {32f, 37f, 36f, 41f}, {38f, 11f, 42f, 15f}, {38f, 15f, 42f, 19f}, {38f, 19f, 42f, 23f}, {38f, 23f, 42f, 27f}}), new int[]{}),
            // 1 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 43f, -2f, 2f, 47f, 2f, new float[][]{{20f, 11f, 24f, 15f}, {24f, 11f, 28f, 15f}, {32f, 33f, 36f, 37f}, {16f, 36f, 20f, 40f}, {20f, 36f, 24f, 40f}, {36f, 30f, 40f, 34f}}), new int[]{}),
            // 2 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-1f, 73f, -1f, 1f, 81f, 1f, new float[][]{{24f, 36f, 26f, 44f}, {36f, 38f, 38f, 46f}, {38f, 38f, 40f, 46f}, {40f, 0f, 42f, 8f}, {40f, 42f, 42f, 44f}, {42f, 42f, 44f, 44f}}), new int[]{}),
            // 3 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-4f, 77f, -0.9f, 4f, 78f, 0.9f, new float[][]{{42f, 12f, 50f, 13f}, {38f, 29f, 40f, 30f}, {42f, 13f, 50f, 14f}, {43f, 22f, 45f, 23f}, {20f, 15f, 28f, 17f}, {38f, 27f, 46f, 29f}}), new int[]{}),
            // 4 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-3f, 81f, -0.9f, 3f, 82f, 0.9f, new float[][]{{42f, 14f, 48f, 15f}, {43f, 23f, 45f, 24f}, {42f, 15f, 48f, 16f}, {43f, 24f, 45f, 25f}, {40f, 33f, 46f, 35f}, {40f, 35f, 46f, 37f}}), new int[]{}),
            // 5 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-2f, 82f, -0.4f, 2f, 92f, 0.4f, new float[][]{{8f, 30f, 12f, 40f}, {22f, 40f, 23f, 50f}, {12f, 30f, 16f, 40f}, {23f, 40f, 24f, 50f}, {42f, 26f, 46f, 27f}, {8f, 43f, 12f, 44f}}), new int[]{}),
            // 6 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-1f, 92f, -0.3f, 1f, 102f, 0.3f, new float[][]{{26f, 33f, 28f, 43f}, {34f, 41f, 34.5f, 51f}, {28f, 33f, 30f, 43f}, {35f, 41f, 35.5f, 51f}, {43f, 25f, 45f, 25.5f}, {10f, 44f, 12f, 44.5f}}), new int[]{}),
            // 7 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-0.5f, 102f, -0.2f, 0.5f, 107f, 0.2f, new float[][]{{42f, 16f, 43f, 21f}, {26f, 43f, 26.5f, 48f}, {42f, 21f, 43f, 26f}, {27f, 43f, 27.5f, 48f}, {44f, 16f, 45f, 16.5f}, {44f, 17f, 45f, 17.5f}}), new int[]{}),
            // 8 cube
            new Part(new BattlefieldBannerGeometry.Cuboid(-1f, 0f, -1f, 1f, 71f, 1f, new float[][]{{0f, 17f, 2f, 88f}, {2f, 17f, 4f, 88f}, {4f, 17f, 6f, 88f}, {6f, 17f, 8f, 88f}, {12f, 43f, 14f, 45f}, {14f, 43f, 16f, 45f}}), new int[]{})
    });

    public static Model model(BattlefieldBannerBlock.Faction faction, boolean bare) {
        return switch(faction){
            case SOUL -> bare ? SOUL_BARE : SOUL_FULL;
            case HUNTER -> bare ? HUNTER_BARE : HUNTER_FULL;
            case REGULAR -> throw new IllegalArgumentException("Regular banner has dedicated rig");
        };
    }
    private static Kind kind(BattlefieldBannerBlock.Faction faction,boolean bare) {
        return switch(faction){
            case SOUL -> bare ? Kind.SOUL_BARE : Kind.SOUL_FULL;
            case HUNTER -> bare ? Kind.HUNTER_BARE : Kind.HUNTER_FULL;
            case REGULAR -> throw new IllegalArgumentException("Regular banner has dedicated rig");
        };
    }
    /** Client wave only; outboard descendants flutter more, branching tails independently. */
    public static float windYaw(Joint j,double phase) {
        return (.012f+Math.min(7,j.depth())*.0065f)
                *(float)Math.sin(phase-j.depth()*.57f-j.phaseShift());
    }
    public static float windRoll(Joint j,double phase) {
        return windYaw(j,phase)*.35f;
    }
    private static final Map<Kind,Map<Direction,List<BlockPos>>> FOOTPRINTS = makeFootprints();
    /** One shared swept safety envelope for server placement and scene composition. */
    public static List<BlockPos> footprint(BattlefieldBannerBlock.Faction faction,boolean bare,Direction facing) {
        if(faction==BattlefieldBannerBlock.Faction.REGULAR)
            return bare ? BattlefieldBannerGeometry.bareFootprint(facing) : BattlefieldBannerGeometry.footprint(facing);
        var cells=FOOTPRINTS.get(kind(faction,bare)).get(facing);
        if(cells==null)throw new IllegalArgumentException("Horizontal direction required");
        return cells;
    }
    private static Map<Kind,Map<Direction,List<BlockPos>>> makeFootprints() {
        var out=new EnumMap<Kind,Map<Direction,List<BlockPos>>>(Kind.class);
        for(var kind:Kind.values()) {
            Model model=switch(kind) {
                case SOUL_FULL -> SOUL_FULL;
                case SOUL_BARE -> SOUL_BARE;
                case HUNTER_FULL -> HUNTER_FULL;
                case HUNTER_BARE -> HUNTER_BARE;
            };
            var dirs=new EnumMap<Direction,List<BlockPos>>(Direction.class);
            for(var facing:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}) {
                Set<BlockPos> cells=new HashSet<>();
                for(Part part:model.parts()) {
                    var box=part.cube();
                    int samples=part.joints().length==0 ? 1 : 160;
                    for(int t=0;t<samples;t++) {
                        double phase=t*(Math.PI*2d/samples);
                        float minX=Float.POSITIVE_INFINITY,minY=Float.POSITIVE_INFINITY,minZ=Float.POSITIVE_INFINITY;
                        float maxX=Float.NEGATIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY;
                        for(float bx:new float[]{box.x0(),box.x1()})
                            for(float by:new float[]{box.y0(),box.y1()})
                                for(float bz:new float[]{box.z0(),box.z1()}) {
                                    double x=bx,y=by,z=bz;
                                    // Matrix order in BER: Y then Z; apply Z then Y to points.
                                    for(int n=part.joints().length-1;n>=0;n--) {
                                        Joint j=model.joints()[part.joints()[n]];
                                        double yaw=windYaw(j,phase),roll=windRoll(j,phase);
                                        double dx=x-j.x(),dy=y-j.y(),dz=z-j.z();
                                        double xx=dx*Math.cos(roll)-dy*Math.sin(roll);
                                        double yy=dx*Math.sin(roll)+dy*Math.cos(roll);
                                        x=j.x()+xx*Math.cos(yaw)+dz*Math.sin(yaw);
                                        y=j.y()+yy;
                                        z=j.z()-xx*Math.sin(yaw)+dz*Math.cos(yaw);
                                    }
                                    minX=Math.min(minX,(float)x);maxX=Math.max(maxX,(float)x);
                                    minY=Math.min(minY,(float)y);maxY=Math.max(maxY,(float)y);
                                    minZ=Math.min(minZ,(float)z);maxZ=Math.max(maxZ,(float)z);
                                }
                        float pad=part.joints().length==0?0f:1f;
                        addCells(cells,facing,minX-pad,maxX+pad,minY-pad,maxY+pad,minZ-pad,maxZ+pad);
                    }
                }
                dirs.put(facing,List.copyOf(cells));
            }
            out.put(kind,Map.copyOf(dirs));
        }
        return Map.copyOf(out);
    }
    /** Match the existing regular rig's (0.5,0,0.5) origin and FACING transform. */
    private static void addCells(Set<BlockPos> cells,Direction dir,
                                 float x0,float x1,float y0,float y1,float z0,float z1) {
        float loX=Float.POSITIVE_INFINITY,hiX=Float.NEGATIVE_INFINITY;
        float loZ=Float.POSITIVE_INFINITY,hiZ=Float.NEGATIVE_INFINITY;
        for(float x:new float[]{x0,x1})for(float z:new float[]{z0,z1}) {
            float rx=switch(dir){case NORTH->x;case SOUTH->-x;case EAST->-z;case WEST->z;default->throw new IllegalArgumentException();};
            float rz=switch(dir){case NORTH->z;case SOUTH->-z;case EAST->x;case WEST->-x;default->throw new IllegalArgumentException();};
            loX=Math.min(loX,.5f+rx/16f);hiX=Math.max(hiX,.5f+rx/16f);
            loZ=Math.min(loZ,.5f+rz/16f);hiZ=Math.max(hiZ,.5f+rz/16f);
        }
        float eps=.00001f;
        for(int x=(int)Math.floor(loX+eps);x<Math.ceil(hiX-eps);x++)
            for(int z=(int)Math.floor(loZ+eps);z<Math.ceil(hiZ-eps);z++)
                for(int y=(int)Math.floor(y0/16f+eps);y<Math.ceil(y1/16f-eps);y++)
                    cells.add(new BlockPos(x,y,z));
    }
}
