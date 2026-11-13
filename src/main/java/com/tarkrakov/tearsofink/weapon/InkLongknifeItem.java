package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.item.Descriptions;
import com.tarkrakov.tearsofink.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Чернильный длинный нож — базовое оружие Чернил (из саквояжа). Абилка R: Чернильная клякса. */
public class InkLongknifeItem extends SwordItem implements AbilityWeapon {
	/** Урон сверх материала (итого 1 + 2 + это = 5) и скорость атаки (быстрее меча). */
	public static final int DAMAGE = 2;
	public static final float ATTACK_SPEED = -2.0f;

	public InkLongknifeItem(Properties properties) {
		super(ModTiers.ASPHODA, properties.attributes(SwordItem.createAttributes(ModTiers.ASPHODA, DAMAGE, ATTACK_SPEED).withTooltip(false)));
	}

	@Override
	public Skill ability() {
		return WeaponAbilities.BLOT;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Descriptions.add(getDescriptionId(), tooltip);
	}
}
