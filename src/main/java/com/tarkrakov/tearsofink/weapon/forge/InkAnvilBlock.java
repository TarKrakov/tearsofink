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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Чернильная наковальня: ПКМ — окно ковки (раскладка 3×3) и заточки. */
public class InkAnvilBlock extends Block {
	private static final Component TITLE = Component.translatable("container.tears-of-ink.ink_anvil");

	public InkAnvilBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide) {
			player.openMenu(state.getMenuProvider(level, pos));
			if (player instanceof ServerPlayer sp && ModDimensions.isLands(level))
				DiaryEntries.unlock(sp, DiaryEntries.INK_ANVIL);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider((id, inventory, player) ->
				new InkAnvilMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
	}
}
