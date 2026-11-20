package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.mixin.PlayerArmorMixin.EquippedArmor;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import com.tarkrakov.tearsofink.weapon.forge.Gear;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/** D-043 / D-097. Scales each equipped item's protection in the foreign world, without changing the item. */
public final class VanillaArmorPenalty {
	public static final double LAND_MULTIPLIER = EquipmentAffinity.EXTERNAL_IN_LANDS;
	private static final List<EquipmentSlot> SLOTS =
			List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	private VanillaArmorPenalty() {}

	public static void init() {
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> update(player));
	}

	public static void update(ServerPlayer player) {
		boolean lands = ModDimensions.isLands(player.level());
		var equipment = (EquippedArmor) player;
		for (EquipmentSlot slot : SLOTS) {
			// Читаем тот же снимок, с которым работают уже применённые ванилью модификаторы.
			var stack = equipment.tearsofink$getAppliedArmor(slot);
			if (stack.isEmpty()) continue;
			double multiplier = EquipmentAffinity.multiplier(lands, Gear.isModItem(stack));
			// ItemStack.forEachModifier добавляет чары, поэтому берём только модификаторы самого предмета.
			var modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
			if (modifiers.modifiers().isEmpty()) {
				modifiers = stack.getItem().getDefaultAttributeModifiers();
			}
			modifiers.forEach(slot, (attribute, modifier) -> rescale(player, attribute, modifier, multiplier));
		}
	}

	private static void rescale(ServerPlayer player, Holder<Attribute> attribute, AttributeModifier original,
								double multiplier) {
		if (!attribute.equals(Attributes.ARMOR) && !attribute.equals(Attributes.ARMOR_TOUGHNESS)) {
			return;
		}
		var instance = player.getAttribute(attribute);
		if (instance == null) return;
		var applied = instance.getModifier(original.id());
		double amount = original.amount() * multiplier;
		if (applied != null && applied.amount() != amount) {
			// Сохраняем ID и операцию. При смене части Minecraft сам удалит этот модификатор.
			instance.addOrUpdateTransientModifier(new AttributeModifier(original.id(), amount, original.operation()));
		}
	}
}
