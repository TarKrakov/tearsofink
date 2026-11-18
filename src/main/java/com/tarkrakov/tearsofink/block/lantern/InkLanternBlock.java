package com.tarkrakov.tearsofink.block.lantern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Фонарь, который можно зажечь и погасить: Чернильный (малый) и Вечный.
 * Ставится как ванильный — на землю/забор или подвешенным под блок/цепь.
 */
public class InkLanternBlock extends LanternBlock implements EntityBlock, FogClearing {
	/** Only the road-generated standing lantern uses the authored road model; legacy lanterns remain unchanged. */
	public static final BooleanProperty ROAD = BooleanProperty.create("road");
	private static final VoxelShape ROAD_SHAPE = Block.box(2, 2, 3, 14, 14, 15);
	private final int clearRadius;
	private final boolean eternal;

	/**
	 * @param eternal вечный фонарь: всегда горит, не гаснет, не ломается в выживании
	 */
	public InkLanternBlock(int clearRadius, boolean eternal, Properties properties) {
		super(properties);
		this.clearRadius = clearRadius;
		this.eternal = eternal;
		this.registerDefaultState(this.defaultBlockState().setValue(LIT, true).setValue(PLAYER_MADE, false).setValue(ROAD, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		// у вечного эти свойства тоже есть, но не используются: он всегда горит и всегда мировой
		builder.add(LIT, PLAYER_MADE, ROAD);
	}

	public boolean isEternal() {
		return eternal;
	}

	@Override
	public int clearRadius() {
		return clearRadius;
	}

	@Override
	public boolean clears(BlockState state) {
		return eternal || state.getValue(LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return eternal ? state : Lanterns.placed(state, context.getPlayer());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (eternal) return InteractionResult.PASS;
		return Lanterns.use(state, level, pos, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(ROAD) && !state.getValue(HANGING)
				? ROAD_SHAPE : super.getShape(state, level, pos, context);
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
