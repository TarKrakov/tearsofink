package com.tarkrakov.tearsofink.pantheon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** One canonical starter relic is represented by the saved occupied block state. */
public final class PantheonAltarBlock extends Block {
    public static final BooleanProperty RELIC = BooleanProperty.create("relic");
    public PantheonAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RELIC, true));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RELIC);
    }
}
