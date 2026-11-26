package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.trader.SoulTraderEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Хранитель Алой Жатвы — стоит у «двери» Арены (заглушка: модель Торговки, красная). ПКМ — меню Жатвы в чате. */
public class HarvestKeeperEntity extends SoulTraderEntity {
	public HarvestKeeperEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		if (!Harvest.ENABLED && !level().isClientSide) {
			discard();
			return;
		}
		super.tick();
	}

	@Override
	protected boolean vanishes() {
		return false;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (player instanceof ServerPlayer sp) Harvest.menu(sp);
		return InteractionResult.sidedSuccess(level().isClientSide);
	}
}
