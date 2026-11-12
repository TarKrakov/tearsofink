package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.item.Descriptions;
import com.tarkrakov.tearsofink.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Расколотая подвеска спасения — базовое оружие Души (из саквояжа). Держится в левой руке; Shift+R — лечение. */
public class SalvationPendantItem extends Item implements AbilityWeapon {
	public SalvationPendantItem(Properties properties) {
		super(properties);
	}

	@Override
	public Skill ability() {
		return WeaponAbilities.SALVATION;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Descriptions.add(getDescriptionId(), tooltip);
	}
}
