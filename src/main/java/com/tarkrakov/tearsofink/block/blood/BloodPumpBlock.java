package com.tarkrakov.tearsofink.block.blood;

import com.mojang.serialization.MapCodec;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.lantern.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

/**
 * Помпа Кровосборника: два блока в высоту, модель автора с анимацией (рисуется как сундук — кодом).
 * Нижний блок — главный (качает, рисует модель), верхний — невидимый, только занимает место.
 * Накопитель всегда стоит сбоку от помпы (см. tankSide). Мировой: в выживании не ломается.
 * При установке из инвентаря сама ставит рядом Накопитель.
 */
public class BloodPumpBlock extends BaseEntityBlock {
	public static final MapCodec<BloodPumpBlock> CODEC = simpleCodec(BloodPumpBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

	/** Хитбокс по модели автора: низ — корпус целиком, верх — шток и крышка. */
	private static final VoxelShape LOWER = Block.box(0, 0, 0, 16, 16, 16);
	private static final VoxelShape UPPER = net.minecraft.world.phys.shapes.Shapes.or(
			Block.box(6, 0, 6, 10, 12, 10),   // шток
			Block.box(0, 12, 0, 16, 16, 16)); // крышка (в верхнем положении)

	public BloodPumpBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
	}

	/**
	 * С какой стороны от помпы стоит Накопитель. Сейчас — справа, если смотреть помпе «в лицо».
	 * (FACING — куда смотрит лицо помпы; игрок перед ней смотрит в обратную сторону.)
	 */
	public static Direction tankSide(Direction facing) {
		return facing.getOpposite().getClockWise();
	}

	/** Где Накопитель этой помпы (от нижнего блока). */
	public static BlockPos tankPos(BlockPos lowerPos, BlockState state) {
		return lowerPos.relative(tankSide(state.getValue(FACING)));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
	}

	// ----- Установка: два блока + Накопитель сбоку -----
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) return null;
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
		BlockPos tank = tankPos(pos, state);
		if (level.getBlockState(tank).canBeReplaced()) {
			level.setBlock(tank, ModBlocks.BLOOD_TANK.defaultBlockState().setValue(BloodTankBlock.FACING, state.getValue(FACING)), Block.UPDATE_ALL);
		}
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
		if (direction.getAxis() == Direction.Axis.Y && partnerSide && !(neighbor.is(this) && neighbor.getValue(HALF) != half)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}

	// ----- Качает нижний блок -----
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new BloodPumpBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BLOOD_PUMP, BloodPumpBlockEntity::serverTick);
	}
}
