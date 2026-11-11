package com.tarkrakov.tearsofink.item;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/** Зелье мода (варится на Алхимическом столе): выпил — эффект, остаётся Пустой флакон. */
public class BrewItem extends Item {
	private final Supplier<Holder<MobEffect>> effect;
	private final int durationTicks;

	public BrewItem(Supplier<Holder<MobEffect>> effect, int durationTicks, Properties properties) {
		super(properties);
		this.effect = effect;
		this.durationTicks = durationTicks;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 32;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide) entity.addEffect(new MobEffectInstance(effect.get(), durationTicks));
		if (entity instanceof Player player && player.getAbilities().instabuild) return stack;
		stack.shrink(1);
		ItemStack flask = new ItemStack(ModItems.EMPTY_FLASK);
		if (stack.isEmpty()) return flask;
		if (entity instanceof Player player && !player.getInventory().add(flask)) player.drop(flask, false);
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		int seconds = durationTicks / 20;
		tooltip.add(Component.translatable(effect.get().value().getDescriptionId())
				.append(String.format(" (%d:%02d)", seconds / 60, seconds % 60))
				.withStyle(effect.get().value().getCategory().getTooltipFormatting()));
		Descriptions.add(getDescriptionId(), tooltip);
	}
}
