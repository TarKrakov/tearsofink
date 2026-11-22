package com.tarkrakov.tearsofink.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Rare no-collision, non-ticking anchor for a many-block-scale ancient sword. */
public final class BattlefieldMonumentBlock extends BaseEntityBlock {
    public static final MapCodec<BattlefieldMonumentBlock> CODEC = simpleCodec(BattlefieldMonumentBlock::new);
    public static final IntegerProperty BLADE = IntegerProperty.create("blade", 0, 1);
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);
    public static final BooleanProperty TABLEAU = BooleanProperty.create("tableau");

    public BattlefieldMonumentBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(BLADE, 0).setValue(VARIANT, 0).setValue(TABLEAU, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BattlefieldMonumentBlockEntity(pos, state);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HorizontalDirectionalBlock.FACING, rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, BLADE, VARIANT, TABLEAU);
    }
}
