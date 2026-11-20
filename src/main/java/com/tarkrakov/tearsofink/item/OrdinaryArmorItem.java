package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
		// D-098: per-piece armor (HP-independent points), durability, total movement multiplier.
		LIGHT("light", ArmorMaterials.LEATHER, 0.25f,
				new Piece(1.5, 110, 0.02), new Piece(3.5, 160, 0.03),
				new Piece(2.5, 150, 0.03), new Piece(1.5, 125, 0.02)),
		MEDIUM("medium", ArmorMaterials.CHAIN, 0.5f,
				new Piece(2, 185, 0), new Piece(4, 270, 0),
				new Piece(3, 250, 0), new Piece(2, 210, 0)),
		HEAVY("heavy", ArmorMaterials.IRON, 0.75f,
				new Piece(2.5, 240, -0.025), new Piece(4.5, 355, -0.045),
				new Piece(3.5, 330, -0.05), new Piece(2.5, 275, -0.03));

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
			// ArmorMaterial accepts only integers; gameplay uses exact fractional modifiers below.
			return Map.of(Type.HELMET, materialDefense(Type.HELMET),
					Type.CHESTPLATE, materialDefense(Type.CHESTPLATE),
					Type.LEGGINGS, materialDefense(Type.LEGGINGS),
					Type.BOOTS, materialDefense(Type.BOOTS));
		}

		private int materialDefense(Type type) {
			return (int) Math.round(pieces.get(type).defense());
		}
	}

	private record Piece(double defense, int durability, double speed) {}

	/** У каждой ступени брони один слот пробуждения. */
	public static final int AWAKENING_SLOTS = 1;

	private final Piece piece;
	private final Weight weight;
	private final ItemAttributeModifiers defaultModifiers;
	private final ItemAttributeModifiers[] legacyModifiers;

	OrdinaryArmorItem(Holder<ArmorMaterial> material, Type type, Weight weight) {
		super(material, type, new Properties().durability(weight.pieces.get(type).durability())
				.component(ModArmor.TIER, Tier.DIM.ordinal()));
		this.weight = weight;
		this.piece = weight.pieces.get(type);
		this.defaultModifiers = modifiers(Tier.DIM);
		this.legacyModifiers = new ItemAttributeModifiers[Tier.BY_ID.length];
		// Migrate only an exact old, mod-generated component; never erase custom attributes.
		for (Tier tier : Tier.BY_ID) legacyModifiers[tier.ordinal()] = legacyModifiers(tier);
	}

	public String weightId() { return weight.id(); }

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

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotIndex, boolean selected) {
		super.inventoryTick(stack, level, entity, slotIndex, selected);
		if (level.isClientSide || weight == Weight.MEDIUM) return;
		var stored = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
		Tier tier = tier(stack);
		if (legacyModifiers[tier.ordinal()].equals(stored)) {
			stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers(tier));
		}
	}

	private ItemAttributeModifiers legacyModifiers(Tier tier) {
		double armor = piece.defense() + (weight == Weight.LIGHT ? -0.5 : weight == Weight.HEAVY ? 0.5 : 0);
		double toughness = weight == Weight.LIGHT ? 0 : weight == Weight.HEAVY ? 1 : 0.5;
		double speed = switch (getEquipmentSlot()) {
			case HEAD -> weight == Weight.LIGHT ? 0.01 : weight == Weight.HEAVY ? -0.01 : 0;
			case CHEST -> weight == Weight.LIGHT ? 0.02 : weight == Weight.HEAVY ? -0.025 : 0;
			case LEGS -> weight == Weight.LIGHT ? 0.02 : weight == Weight.HEAVY ? -0.03 : 0;
			case FEET -> weight == Weight.LIGHT ? 0.01 : weight == Weight.HEAVY ? -0.015 : 0;
			default -> 0;
		};
		return modifiers(tier, armor, toughness, speed);
	}

	private ItemAttributeModifiers modifiers(Tier tier) {
		return modifiers(tier, piece.defense(), weight.toughness(), piece.speed());
	}

	private ItemAttributeModifiers modifiers(Tier tier, double armor, double toughness, double speed) {
		var slot = EquipmentSlotGroup.bySlot(getEquipmentSlot());
		var protectionId = TearsOfInk.id("armor/" + getEquipmentSlot().getName());
		var builder = ItemAttributeModifiers.builder()
				.add(Attributes.ARMOR, new AttributeModifier(protectionId, armor + tier.protectionBonus,
						AttributeModifier.Operation.ADD_VALUE), slot)
				.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(protectionId, toughness + tier.protectionBonus,
						AttributeModifier.Operation.ADD_VALUE), slot);
		if (speed != 0) {
			builder.add(Attributes.MOVEMENT_SPEED,
					new AttributeModifier(TearsOfInk.id("armor_speed/" + getEquipmentSlot().getName()),
							speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), slot);
		}
		return builder.build();
	}
}
