package com.tarkrakov.tearsofink.block;

import com.tarkrakov.tearsofink.block.lantern.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** All parameters persist in the blockstate. No server ticker or update packets. */
public final class BattlefieldMonumentBlockEntity extends BlockEntity {
    public BattlefieldMonumentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTLEFIELD_MONUMENT, pos, state);
    }
}
