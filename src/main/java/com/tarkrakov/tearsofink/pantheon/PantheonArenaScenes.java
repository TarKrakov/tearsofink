package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/** Presets and the measured interior of the bundled Axiom schematic. */
public final class PantheonArenaScenes {
    static final int MIN_X = 17, MAX_X = 99;
    static final int MIN_Z = 45, MAX_Z = 127;
    static final int FLOOR = 1, CEILING = 13;
    static final int WIDTH = MAX_X - MIN_X + 1, DEPTH = MAX_Z - MIN_Z + 1;
    static final int VOLUME = WIDTH * DEPTH * (CEILING - FLOOR + 1);
    private static final int CENTER_X = 58, CENTER_Z = 86;
    private static final int POOL_X_RADIUS = 38, POOL_Z_RADIUS = 36;

    enum Lighting { TWILIGHT }

    enum Scene {
        NEUTRAL(Lighting.TWILIGHT),
        FOREST(Lighting.TWILIGHT),
        ISLAND(Lighting.TWILIGHT);

        final Lighting lighting;

        Scene(Lighting lighting) {
            this.lighting = lighting;
        }

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        BlockState block(BlockPos local, BlockState neutral, boolean[][] free) {
            return switch (this) {
                case NEUTRAL -> neutral;
                case FOREST -> forest(local, neutral);
                case ISLAND -> island(local, neutral, free);
            };
        }
    }

    private static final int[][] TREES = {
            {35, 69, 6}, {42, 75, 5}, {76, 70, 7}, {81, 78, 5},
            {36, 86, 7}, {80, 91, 7}, {37, 100, 5}, {45, 103, 6}, {70, 101, 6}, {80, 104, 5},
            {22, 82, 8}, {22, 94, 8}, {94, 72, 8}, {94, 82, 8}, {94, 95, 8},
            {33, 54, 9}, {44, 54, 8}, {72, 54, 8}, {82, 54, 9},
            {33, 118, 9}, {46, 118, 8}, {72, 118, 8}, {82, 118, 9}
    };

    private PantheonArenaScenes() {}

    static BlockPos local(int index) {
        return new BlockPos(MIN_X + index % WIDTH, FLOOR + index / (WIDTH * DEPTH),
                MIN_Z + index / WIDTH % DEPTH);
    }

    static boolean contains(BlockPos p) {
        return p.getX() >= MIN_X && p.getX() <= MAX_X && p.getZ() >= MIN_Z && p.getZ() <= MAX_Z
                && p.getY() >= FLOOR && p.getY() <= CEILING;
    }

    private static BlockState forest(BlockPos p, BlockState neutral) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        if (y == FLOOR) {
            if (x < 30 || x > 86 || z < 63 || z > 109) return neutral;
            return Math.abs(x - CENTER_X) <= 1 && z <= CENTER_Z
                    ? ModBlocks.ASHEN_PATH.defaultBlockState() : ModBlocks.ASHEN_SOIL.defaultBlockState();
        }
        for (int[] tree : TREES) {
            int dx = Math.abs(x - tree[0]), dz = Math.abs(z - tree[1]), top = tree[2] + 1;
            if (dx == 0 && dz == 0 && y <= top) return ModBlocks.PALE_LOG.defaultBlockState();
            if (y >= top - 2 && y <= top + 2 && Math.max(dx, dz) <= (y >= top + 1 ? 1 : 2)
                    && dx + dz <= 3) return ModBlocks.PALE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        }
        if (y == 2 && Math.abs(x - CENTER_X) > 4 && Math.floorMod(x * 31 + z * 17, 19) == 0)
            return ModBlocks.PALE_GRASS.defaultBlockState();
        return neutral;
    }

    private static BlockState island(BlockPos p, BlockState neutral, boolean[][] free) {
        int dx = p.getX() - CENTER_X, dz = p.getZ() - CENTER_Z, y = p.getY();
        boolean island = dx * dx + dz * dz <= 85;
        boolean causeway = Math.abs(dx) <= 1 && dz <= 0;
        boolean basin = basin(free, dx, dz);
        boolean rim = !basin && (basin(free, dx + 1, dz) || basin(free, dx - 1, dz)
                || basin(free, dx, dz + 1) || basin(free, dx, dz - 1));
        if (y == FLOOR && (basin || rim) && Math.abs(dx) <= 28 && Math.abs(dz) <= 23)
            return ModBlocks.TAR_SOIL.defaultBlockState();
        if (y == 2) {
            if (rim || causeway && basin || island) return ModBlocks.TAR_SOIL.defaultBlockState();
            if (basin) return Blocks.WATER.defaultBlockState();
        }
        if (y == 3 && island) return ModBlocks.ASH.defaultBlockState();
        return neutral;
    }

    private static boolean basin(boolean[][] free, int dx, int dz) {
        if (!pool(dx, dz)) return false;
        int x = dx + CENTER_X - MIN_X, z = dz + CENTER_Z - MIN_Z;
        // The ellipse lies at least two cells inside the envelope.
        return free[x][z] && free[x - 1][z] && free[x + 1][z] && free[x][z - 1] && free[x][z + 1];
    }

    private static boolean pool(int dx, int dz) {
        return dx * dx * POOL_Z_RADIUS * POOL_Z_RADIUS + dz * dz * POOL_X_RADIUS * POOL_X_RADIUS
                <= POOL_X_RADIUS * POOL_X_RADIUS * POOL_Z_RADIUS * POOL_Z_RADIUS;
    }

    /** Label water by its bounded scene footprint. The font is outside this mask. */
    public static boolean usesLakeColor(BlockPos world) {
        BlockPos p = PantheonTemple.local(world);
        return contains(p) && p.getY() == 2 && pool(p.getX() - CENTER_X, p.getZ() - CENTER_Z);
    }
}
