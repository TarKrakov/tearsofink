package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.weapon.InkGreatswordItem;
import com.tarkrakov.tearsofink.weapon.ModTiers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Оружие охотников: линия (Шпиль / Кровь / Чернила) и ступень (Тусклое / Контрастное / Рубрики) хранятся в самом предмете.
 * Удары — от архетипа (двуручник — Палачи, клинок — Скимитары). Своей абилки на R пока нет.
 */
public class HunterWeaponItem extends InkGreatswordItem {
	/** Прибавка к урону за ступень: Тусклое, Контрастное, Рубрики. */
	public static final int[] TIER_DAMAGE = {0, 1, 2};
	public static final int TIERS = 3;
	private static final String LINE = "HunterLine", TIER = "HunterTier";

	private final boolean twoHanded;

	public HunterWeaponItem(Properties properties, boolean twoHanded) {
		super(() -> null, properties, twoHanded);
		this.twoHanded = twoHanded;
	}

	@Override
	public Skill ability() {
		return null;
	}

	/** Готовый предмет нужной линии и ступени. */
	public ItemStack make(HunterType line, int tier) {
		ItemStack stack = new ItemStack(this);
		tier = Math.max(0, Math.min(TIERS - 1, tier));
		CompoundTag tag = new CompoundTag();
		tag.putInt(LINE, line.ordinal());
		tag.putInt(TIER, tier);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(line.ordinal() + 1));
		int base = twoHanded ? InkGreatswordItem.DAMAGE : InkGreatswordItem.ONE_HANDED_DAMAGE;
		float speed = twoHanded ? InkGreatswordItem.ATTACK_SPEED : InkGreatswordItem.ONE_HANDED_SPEED;
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
				SwordItem.createAttributes(ModTiers.ASPHODA, base + TIER_DAMAGE[tier], speed).withTooltip(false));
		return stack;
	}

	public static HunterType line(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d == null ? HunterType.SPIRE : HunterType.byIndex(d.copyTag().getInt(LINE));
	}

	public static int tier(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d == null ? 0 : Math.max(0, Math.min(TIERS - 1, d.copyTag().getInt(TIER)));
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable("hunt_weapon.tears-of-ink.tier." + tier(stack),
				Component.translatable("hunt_weapon.tears-of-ink.line." + line(stack).id),
				Component.translatable(getDescriptionId()));
	}
}
