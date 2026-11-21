package com.tarkrakov.tearsofink.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Never placed; exists exclusively as eight pre-baked model states consumed
 * by the one client scene renderer. No per-weapon BE, entity or world tick.
 */
public final class BattlefieldWeaponMeshBlock extends Block {
    public static final IntegerProperty SOURCE = IntegerProperty.create("source", 0, 7);

    public BattlefieldWeaponMeshBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SOURCE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOURCE);
    }
}
