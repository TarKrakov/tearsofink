package com.tarkrakov.tearsofink.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Invisible single anchor for a rare, manually previewed posed composition.
 * The authored weapons are rendered from their existing baked models by the
 * client; no static prop needs its own entity or per-tick updates.
 */
public final class BattlefieldSceneBlock extends BaseEntityBlock {
    public static final MapCodec<BattlefieldSceneBlock> CODEC = simpleCodec(BattlefieldSceneBlock::new);
    public static final IntegerProperty SCENE = IntegerProperty.create("scene", 0, 33); // existing IDs 0/1 are preserved
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);
    // Old chunks have no "layout" property: Minecraft uses default 0.
    // Never repurpose layout 0; all new scenes explicitly set layout 1.
    public static final IntegerProperty LAYOUT = IntegerProperty.create("layout", 0, 2);

    public BattlefieldSceneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(SCENE, 0).setValue(VARIANT, 0).setValue(LAYOUT, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BattlefieldSceneBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, SCENE, VARIANT, LAYOUT);
    }
}
