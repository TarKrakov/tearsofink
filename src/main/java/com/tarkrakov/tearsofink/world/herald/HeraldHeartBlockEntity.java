package com.tarkrakov.tearsofink.world.herald;

import com.tarkrakov.tearsofink.block.lantern.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Маячок сердца Вестника — нужен только чтобы быстро находить Вестников рядом. */
public class HeraldHeartBlockEntity extends BlockEntity {
	public HeraldHeartBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.HERALD_HEART, pos, state);
	}
}
