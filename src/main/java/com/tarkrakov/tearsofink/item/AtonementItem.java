package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.particle.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Искупление — венок-крест с сердцем. Зажать ПКМ: душа очищается, +10 к максимуму Души.
 * Вместе с душой уходят и чернила, полученные за её продажу (это считается само).
 * Работает только внутри Чернильных Земель.
 */
public class AtonementItem extends Item {
	// ===== Настройки — крутить здесь =====
	/** Сколько Души возвращает один венок. */
	public static final int SOUL_RETURNED = 10;
	/** Сколько тиков держать ПКМ (20 = 1 секунда). */
	public static final int USE_TICKS = 30;

	public AtonementItem(Properties properties) {
		super(properties);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_TICKS;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BLOCK;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) return stack;
		InkData data = InkPowers.get(player);
		if (!data.inside()) {
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
			return stack;
		}
		if (data.sold() <= 0) {
			player.sendSystemMessage(Component.translatable("message.tears-of-ink.soul_whole").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			return stack;
		}
		InkData after = data.withSold(data.sold() - SOUL_RETURNED / InkData.SOUL_STEP);
		InkPowers.update(player, after.withSoul(after.soul() + (after.soulMax() - data.soulMax())));

		player.sendSystemMessage(Component.translatable("message.tears-of-ink.atonement").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.8f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4f, 1.6f);
		server.sendParticles(ModParticles.TEAR_GLINT, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
		stack.consume(1, player);
		return stack;
	}
}
