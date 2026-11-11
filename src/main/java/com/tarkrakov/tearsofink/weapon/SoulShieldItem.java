package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.item.Descriptions;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Щит Души: блокирует как ванильный, абилка по Shift+R тратит Душу.
 * Не блокирует, если в правой руке двуручный меч.
 */
public class SoulShieldItem extends ShieldItem implements AbilityWeapon {
	private final Supplier<Skill> ability;

	public SoulShieldItem(Supplier<Skill> ability, Properties properties) {
		super(properties);
		this.ability = ability;
	}

	@Override
	public Skill ability() {
		return ability.get();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof InkGreatswordItem) {
			return InteractionResultHolder.pass(player.getItemInHand(hand));
		}
		return super.use(level, player, hand);
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
		return repair.is(ModItems.ASPHODA);
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getDescriptionId(); // без ванильной приписки про цвет баннера
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Descriptions.add(getDescriptionId(), tooltip);
	}
}
