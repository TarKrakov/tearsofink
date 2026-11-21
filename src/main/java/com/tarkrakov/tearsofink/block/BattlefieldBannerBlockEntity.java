package com.tarkrakov.tearsofink.block;

import com.tarkrakov.tearsofink.block.lantern.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores only its position and facing in the blockstate; intentionally no ticker or data packets. */
public final class BattlefieldBannerBlockEntity extends BlockEntity {
    public BattlefieldBannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTLEFIELD_BANNER, pos, state);
    }
}
