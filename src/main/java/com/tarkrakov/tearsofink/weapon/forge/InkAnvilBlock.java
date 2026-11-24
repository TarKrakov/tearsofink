package com.tarkrakov.tearsofink.weapon.forge;

import com.tarkrakov.tearsofink.prologue.DiaryEntries;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Чернильная наковальня: ПКМ — окно ковки (раскладка 3×3) и заточки. */
public class InkAnvilBlock extends Block {
	private static final Component TITLE = Component.translatable("container.tears-of-ink.ink_anvil");
	/** Exact bounding volume of the authored model, instead of a full block. */
	private static final VoxelShape SHAPE = Block.box(1, 0, 4, 15, 12, 12);

	public InkAnvilBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide) {
			player.openMenu(state.getMenuProvider(level, pos));
			if (player instanceof ServerPlayer sp && ModDimensions.hasLandsRules(level)) {
				DiaryEntries.unlock(sp, DiaryEntries.INK_ANVIL);
				com.tarkrakov.tearsofink.prologue.FateProgress.mark(sp, com.tarkrakov.tearsofink.prologue.FateProgress.Goal.ANVIL);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider((id, inventory, player) ->
				new InkAnvilMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
	}
}
