package com.tarkrakov.tearsofink.world.herald;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * «Сердце» Багряного Вестника: невидимый маячок внутри ствола. Ставится генерацией.
 * По нему клиент находит Вестника: рядом туман реже, с кроны капает кровь.
 */
public class HeraldHeartBlock extends BaseEntityBlock {
	public static final MapCodec<HeraldHeartBlock> CODEC = simpleCodec(HeraldHeartBlock::new);

	public HeraldHeartBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HeraldHeartBlockEntity(pos, state);
	}
}
