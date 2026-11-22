package com.tarkrakov.tearsofink.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** One static anchor for a dense bed of ancient weapons, not item entities. */
public final class BattlefieldRelicPatchBlock extends BaseEntityBlock {
    public static final MapCodec<BattlefieldRelicPatchBlock> CODEC = simpleCodec(BattlefieldRelicPatchBlock::new);
    public static final IntegerProperty STYLE = IntegerProperty.create("style", 0, 7);
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);
    public BattlefieldRelicPatchBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(STYLE, 0).setValue(VARIANT, 0));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BattlefieldRelicPatchBlockEntity(pos, state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, STYLE, VARIANT);
    }
}
