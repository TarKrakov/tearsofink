package com.tarkrakov.tearsofink.block.lantern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Жаровня: большой огонь поселений и лагерей, широкий круг ясности. */
public class BrazierBlock extends Block implements EntityBlock, FogClearing {
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 8, 14);
	private final int clearRadius;

	public BrazierBlock(int clearRadius, Properties properties) {
		super(properties);
		this.clearRadius = clearRadius;
		this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true).setValue(PLAYER_MADE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT, PLAYER_MADE);
	}

	@Override
	public int clearRadius() {
		return clearRadius;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return Lanterns.placed(this.defaultBlockState(), context.getPlayer());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return Lanterns.use(state, level, pos, player);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return Lanterns.destroyProgress(state, player, super.getDestroyProgress(state, player, level, pos));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LanternBeaconBlockEntity(pos, state);
	}
}
