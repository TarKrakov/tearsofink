package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.item.Descriptions;
import com.tarkrakov.tearsofink.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Supplier;

/**
 * Двуручный меч Чернил: бьёт сильно и медленно. Абилка по R тратит Чернила.
 * Пока он в правой руке, щит в левой не блокирует (фонарь — работает).
 */
public class InkGreatswordItem extends SwordItem implements AbilityWeapon {
	// ===== Настройки — крутить здесь =====
	/** Урон сверх материала (итого: 1 рука + 2 Асфода + это = 9) и скорость атаки (меч −2.4, двуручник медленнее). */
	public static final int DAMAGE = 6;
	public static final float ATTACK_SPEED = -3.0f;

	private final Supplier<Skill> ability;

	public InkGreatswordItem(Supplier<Skill> ability, Properties properties) {
		super(ModTiers.ASPHODA, properties.attributes(SwordItem.createAttributes(ModTiers.ASPHODA, DAMAGE, ATTACK_SPEED).withTooltip(false)));
		this.ability = ability;
	}

	@Override
	public Skill ability() {
		return ability.get();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Descriptions.add(getDescriptionId(), tooltip);
	}
}
