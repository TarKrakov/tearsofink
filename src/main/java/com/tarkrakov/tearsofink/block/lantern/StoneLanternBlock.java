package com.tarkrakov.tearsofink.block.lantern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Каменный фонарь (модель автора): высокий, в два блока, как дверь.
 * Модель целиком в нижнем блоке, верхний — невидимый, только занимает место.
 * Горит/гаснет и разгоняет мглу нижний блок; огонёк — танцующая частица в камере.
 */
public class StoneLanternBlock extends Block implements EntityBlock, FogClearing, FlameAnchor {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

	/** Где горит огонёк — центр камеры (в блоках от угла нижнего блока). */
	private static final Vec3 FLAME = new Vec3(8 / 16.0, 18 / 16.0, 8 / 16.0);

	private static final VoxelShape LOWER = Shapes.or(
			Block.box(3, 0, 3, 13, 2, 13),   // основание
			Block.box(7, 2, 7, 9, 13, 9),    // столб
			Block.box(3, 13, 3, 13, 16, 13)); // площадка
	private static final VoxelShape UPPER = Shapes.or(
			Block.box(4, 0, 4, 12, 9, 12),   // камера и плашка
			Block.box(2, 9, 2, 14, 10, 14)); // крыша

	private final int clearRadius;

	public StoneLanternBlock(int clearRadius, Properties properties) {
		super(properties);
		this.clearRadius = clearRadius;
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(HALF, DoubleBlockHalf.LOWER).setValue(LIT, true).setValue(PLAYER_MADE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF, LIT, PLAYER_MADE);
	}

	@Override
	public int clearRadius() {
		return clearRadius;
	}

	@Override
	public boolean clears(BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(LIT);
	}

	@Override
	public Vec3 flameOffset(BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(LIT) ? FLAME : null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
	}

	// ----- Установка в два блока -----
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) return null;
		return Lanterns.placed(this.defaultBlockState(), context.getPlayer());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER) return true;
		BlockState below = level.getBlockState(pos.below());
		return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		DoubleBlockHalf half = state.getValue(HALF);
		boolean partnerSide = (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP);
		if (direction.getAxis() == Direction.Axis.Y && partnerSide) {
			boolean partnerOk = neighbor.is(this) && neighbor.getValue(HALF) != half;
			if (!partnerOk) return Blocks.AIR.defaultBlockState();
			// верх повторяет «горит» и «самодельный» за низом
			if (half == DoubleBlockHalf.UPPER) {
				return state.setValue(LIT, neighbor.getValue(LIT)).setValue(PLAYER_MADE, neighbor.getValue(PLAYER_MADE));
			}
		}
		return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos below = pos.below();
			BlockState lower = level.getBlockState(below);
			if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
				if (player.isCreative()) {
					level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				} else {
					level.destroyBlock(below, true, player); // выпадет из нижней половины
				}
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	// ----- Зажечь / погасить — всегда через нижний блок -----
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		BlockState lower = level.getBlockState(lowerPos);
		if (!lower.is(this)) return InteractionResult.PASS;
		return Lanterns.use(lower, level, lowerPos, player);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return Lanterns.destroyProgress(state, player, super.getDestroyProgress(state, player, level, pos));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new LanternBeaconBlockEntity(pos, state) : null;
	}
}
