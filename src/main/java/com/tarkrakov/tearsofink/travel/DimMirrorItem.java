package com.tarkrakov.tearsofink.travel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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
 * Тусклое зеркальце: зажать ПКМ ~2,5 с (зеркало светлеет) → из Земель домой, иначе — в Земли.
 * Только в безопасности (никто не держит целью), не верхом; любой урон прерывает.
 */
public class DimMirrorItem extends Item {
	/** Сколько держать (тики). */
	public static final int USE_TICKS = 50;
	/** Перезарядка после перехода (тики). */
	public static final int COOLDOWN = 60;

	public DimMirrorItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isPassenger()) {
			if (player instanceof ServerPlayer sp) refuse(sp, "mirror_riding");
			return InteractionResultHolder.fail(stack);
		}
		if (player instanceof ServerPlayer sp && Travel.hunted(sp)) {
			refuse(sp, "mirror_busy");
			return InteractionResultHolder.fail(stack);
		}
		startUse(level, player, hand);
		return InteractionResultHolder.consume(stack);
	}

	static void startUse(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		if (!level.isClientSide) level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.5f);
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer sp)) return;
		if (!FountainTravel.returning(sp) && (sp.hurtTime > 0 || sp.isPassenger())) { // ударили / сел верхом — зеркало мутнеет
			sp.stopUsingItem();
			refuse(sp, "mirror_busy");
			return;
		}
		int used = getUseDuration(stack, entity) - remaining;
		if (used % 3 == 0) {
			int n = 1 + used / 12;
			server.sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getY() + 1.4, sp.getZ(), n * 3, 0.4, 0.5, 0.4, 0.6);
			server.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.2, sp.getZ(), n / 2, 0.3, 0.4, 0.3, 0.01);
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer sp && !FountainTravel.finishMirror(sp)) {
			sp.getCooldowns().addCooldown(this, COOLDOWN);
			Travel.mirrorTravel(sp);
		}
		return stack;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
		if (entity instanceof ServerPlayer player) FountainTravel.releaseMirror(player);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_TICKS;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.TOOT_HORN; // подносит к лицу — заглядывает в зеркало
	}

	private static void refuse(ServerPlayer sp, String key) {
		sp.sendSystemMessage(Component.translatable("message.tears-of-ink." + key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		sp.playNotifySound(SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.6f, 0.6f);
	}
}
