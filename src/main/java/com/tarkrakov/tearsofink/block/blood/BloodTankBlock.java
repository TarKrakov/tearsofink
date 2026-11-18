package com.tarkrakov.tearsofink.block.blood;

import com.mojang.serialization.MapCodec;
import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Накопитель Кровосборника: сюда помпа сливает кровь. ПКМ — забрать Сгустки (и Кристаллы),
 * в чат пишется, сколько крови осталось. Мировой: в выживании не ломается.
 */
public class BloodTankBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final MapCodec<BloodTankBlock> CODEC = simpleCodec(BloodTankBlock::new);

	public BloodTankBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof BloodTankBlockEntity tank)) return InteractionResult.PASS;
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (player instanceof net.minecraft.server.level.ServerPlayer p) com.tarkrakov.tearsofink.prologue.DiaryEntries.discover(p, "draft_blood_collector");
		int clots = tank.takeClots();
		int crystals = tank.takeCrystals();
		if (clots > 0) give(player, new ItemStack(ModItems.BLOOD_CLOT, clots));
		if (crystals > 0) give(player, new ItemStack(ModItems.BLOOD_CRYSTAL, crystals));
		if (clots > 0 || crystals > 0) {
			level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6f, 0.7f);
		}
		if (crystals > 0) {
			player.displayClientMessage(Component.translatable("message.tears-of-ink.blood_crystal", crystals), false);
		}
		if (clots > 0) {
			player.displayClientMessage(Component.translatable("message.tears-of-ink.blood_collected", clots, tank.blood()), false);
		} else {
			player.displayClientMessage(Component.translatable("message.tears-of-ink.blood_level", tank.blood(), BloodTuning.BLOOD_PER_CLOT), false);
		}
		return InteractionResult.CONSUME;
	}

	private static void give(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack)) player.drop(stack, false);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BloodTankBlockEntity(pos, state);
	}
}
