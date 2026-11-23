package com.tarkrakov.tearsofink.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * First Pantheon column-capital prototype.
 * Four-sided extrusion: each 16x16 transparent PNG row is applied to both
 * X and Z axes. Keep PROFILE aligned with the original authored capital PNG.
 * The model is generated from the same rows; no world data is stored.
 */
public final class PantheonCapitalBlock extends Block {
    private static final String[] PROFILE = {
        ".#####....#####.",
        ".######..######.",
        "#######..#######",
        "################",
        "################",
        "################",
        "################",
        "################",
        ".###..####..###.",
        "..##.######.##..",
        ".....######.....",
        ".....######.....",
        "......####......",
        ".....######.....",
        ".....######.....",
        ".....######....."
    };
    private static final VoxelShape SHAPE = createShape();

    public PantheonCapitalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private static VoxelShape createShape() {
        VoxelShape result = Shapes.empty();
        for (int imageY = 0; imageY < 16; imageY++) {
            String row = PROFILE[imageY];
            int blockY = 15 - imageY;
            for (int x = 0; x < 16;) {
                if (row.charAt(x) != '#') {
                    x++;
                    continue;
                }
                int xStart = x;
                while (x < 16 && row.charAt(x) == '#') x++;
                for (int z = 0; z < 16;) {
                    if (row.charAt(z) != '#') {
                        z++;
                        continue;
                    }
                    int zStart = z;
                    while (z < 16 && row.charAt(z) == '#') z++;
                    result = Shapes.or(result, Block.box(xStart, blockY, zStart, x, blockY + 1, z));
                }
            }
        }
        return result;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
