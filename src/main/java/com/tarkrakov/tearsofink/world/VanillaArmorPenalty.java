package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.mixin.PlayerArmorMixin.EquippedArmor;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/** D-043. Масштабирует временные модификаторы каждой надетой ванильной части. */
public final class VanillaArmorPenalty {
	public static final double LAND_MULTIPLIER = 0.60;
	private static final List<EquipmentSlot> SLOTS =
			List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	private VanillaArmorPenalty() {}

	public static void init() {
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> update(player));
	}

	public static void update(ServerPlayer player) {
		double multiplier = ModDimensions.isLands(player.level()) ? LAND_MULTIPLIER : 1.0;
		var equipment = (EquippedArmor) player;
		for (EquipmentSlot slot : SLOTS) {
			// Читаем тот же снимок, с которым работают уже применённые ванилью модификаторы.
			var stack = equipment.tearsofink$getAppliedArmor(slot);
			if (!(stack.getItem() instanceof ArmorItem armor) || stack.isEmpty()
					|| !BuiltInRegistries.ITEM.getKey(armor).getNamespace().equals("minecraft")) {
				continue;
			}
			// ItemStack.forEachModifier добавляет чары, поэтому берём только модификаторы самого предмета.
			var modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
			if (modifiers.modifiers().isEmpty()) {
				modifiers = armor.getDefaultAttributeModifiers();
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
		var applied = instance.getModifier(original.id());
		double amount = original.amount() * multiplier;
		if (applied != null && applied.amount() != amount) {
			// Сохраняем ID и операцию. При смене части Minecraft сам удалит этот модификатор.
			instance.addOrUpdateTransientModifier(new AttributeModifier(original.id(), amount, original.operation()));
		}
	}
}
