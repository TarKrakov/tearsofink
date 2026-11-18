package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Map;

public final class OrdinaryArmorItem extends ArmorItem {
	public enum Tier {
		DIM(0, 1.0),
		CONTRASTED(0.25, 1.15),
		RUBRICS(0.5, 1.30);

		private final double protectionBonus;
		private final double durabilityMultiplier;

		Tier(double protectionBonus, double durabilityMultiplier) {
			this.protectionBonus = protectionBonus;
			this.durabilityMultiplier = durabilityMultiplier;
		}

		private static final Tier[] BY_ID = values();
	}

	enum Weight {
		LIGHT("light", ArmorMaterials.LEATHER, 0,
				new Piece(1, 110, 0.01), new Piece(3, 160, 0.02),
				new Piece(2, 150, 0.02), new Piece(1, 125, 0.01)),
		MEDIUM("medium", ArmorMaterials.CHAIN, 0.5f,
				new Piece(2, 185, 0), new Piece(4, 270, 0),
				new Piece(3, 250, 0), new Piece(2, 210, 0)),
		HEAVY("heavy", ArmorMaterials.IRON, 1,
				new Piece(3, 240, -0.01), new Piece(5, 355, -0.025),
				new Piece(4, 330, -0.03), new Piece(3, 275, -0.015));

		private final String id;
		private final Holder<ArmorMaterial> placeholder;
		private final float toughness;
		private final Map<Type, Piece> pieces;

		Weight(String id, Holder<ArmorMaterial> placeholder, float toughness,
			   Piece helmet, Piece chestplate, Piece leggings, Piece boots) {
			this.id = id;
			this.placeholder = placeholder;
			this.toughness = toughness;
			this.pieces = Map.of(Type.HELMET, helmet, Type.CHESTPLATE, chestplate,
					Type.LEGGINGS, leggings, Type.BOOTS, boots);
		}

		String id() { return id; }
		Holder<ArmorMaterial> placeholder() { return placeholder; }
		float toughness() { return toughness; }

		Map<Type, Integer> defense() {
			return Map.of(Type.HELMET, pieces.get(Type.HELMET).defense(),
					Type.CHESTPLATE, pieces.get(Type.CHESTPLATE).defense(),
					Type.LEGGINGS, pieces.get(Type.LEGGINGS).defense(),
					Type.BOOTS, pieces.get(Type.BOOTS).defense());
		}
	}

	private record Piece(int defense, int durability, double speed) {}

	/** У каждой ступени брони один слот пробуждения. */
	public static final int AWAKENING_SLOTS = 1;

	private final Piece piece;
	private final ItemAttributeModifiers defaultModifiers;

	OrdinaryArmorItem(Holder<ArmorMaterial> material, Type type, Weight weight) {
		super(material, type, new Properties().durability(weight.pieces.get(type).durability())
				.component(ModArmor.TIER, Tier.DIM.ordinal()));
		this.piece = weight.pieces.get(type);
		this.defaultModifiers = modifiers(Tier.DIM);
	}

	public Tier tier(ItemStack stack) {
		return Tier.BY_ID[Mth.clamp(stack.getOrDefault(ModArmor.TIER, 0), 0, Tier.BY_ID.length - 1)];
	}

	/** Серверный вызов для будущего возвышения. Обновляет ступень, атрибуты и лимит износа вместе. */
	public void setTier(ItemStack stack, Tier tier) {
		int maxDamage = (int) Math.round(piece.durability() * tier.durabilityMultiplier);
		int damage = (int) Math.round((double) stack.getDamageValue() / stack.getMaxDamage() * maxDamage);
		stack.set(ModArmor.TIER, tier.ordinal());
		stack.set(DataComponents.MAX_DAMAGE, maxDamage);
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers(tier));
		stack.setDamageValue(Math.min(damage, maxDamage - 1));
	}

	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers() {
		return defaultModifiers;
	}

	private ItemAttributeModifiers modifiers(Tier tier) {
		var slot = EquipmentSlotGroup.bySlot(getEquipmentSlot());
		var protectionId = TearsOfInk.id("armor/" + getEquipmentSlot().getName());
		var builder = ItemAttributeModifiers.builder()
				.add(Attributes.ARMOR, new AttributeModifier(protectionId, piece.defense() + tier.protectionBonus,
						AttributeModifier.Operation.ADD_VALUE), slot)
				.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(protectionId, getToughness() + tier.protectionBonus,
						AttributeModifier.Operation.ADD_VALUE), slot);
		if (piece.speed() != 0) {
			builder.add(Attributes.MOVEMENT_SPEED,
					new AttributeModifier(TearsOfInk.id("armor_speed/" + getEquipmentSlot().getName()),
							piece.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), slot);
		}
		return builder.build();
	}
}
