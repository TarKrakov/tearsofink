package com.tarkrakov.tearsofink.item;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

import static com.tarkrakov.tearsofink.item.OrdinaryArmorItem.Weight.*;

final class ModArmor {
	static final DataComponentType<Integer> TIER = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, TearsOfInk.id("armor_tier"),
			DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 2))
					.networkSynchronized(ByteBufCodecs.VAR_INT).build());

	private static final List<OrdinaryArmorItem> ITEMS = new ArrayList<>();

	enum Region {
		PALE("pale", ModItems.PALE_INGOT),
		BLOOD("blood", ModItems.BLOOD_INGOT),
		INK("ink", ModItems.INK_INGOT);

		final String id;
		final Item ingot;

		Region(String id, Item ingot) {
			this.id = id;
			this.ingot = ingot;
		}
	}

	private ModArmor() {}

	static void init() {
		registerSet(Region.PALE, LIGHT, "headband", "quilted_jacket", "pants", "shoes");
		registerSet(Region.PALE, MEDIUM, "chain_hood", "brigandine", "reinforced_leggings", "leather_boots");
		registerSet(Region.PALE, HEAVY, "closed_helmet", "cuirass", "plate_leggings", "sabatons");

		registerSet(Region.BLOOD, LIGHT, "veil", "robe", "skirt", "sandals");
		registerSet(Region.BLOOD, MEDIUM, "reinforced_mask", "padded_mantle", "reinforced_skirt", "caligae");
		registerSet(Region.BLOOD, HEAVY, "closed_helmet", "chainmail", "plate_kilt", "armored_boots");

		registerSet(Region.INK, LIGHT, "woven_hood", "leather_jerkin", "cloth_pants", "travel_boots");
		registerSet(Region.INK, MEDIUM, "kettle_hat", "scale_armor", "padded_leggings", "high_boots");
		registerSet(Region.INK, HEAVY, "bascinet", "plate_cuirass", "plate_leggings", "armored_boots");
	}

	static List<OrdinaryArmorItem> items() {
		return List.copyOf(ITEMS);
	}

	private static void registerSet(Region region, OrdinaryArmorItem.Weight weight,
									String helmet, String chestplate, String leggings, String boots) {
		// Временный внешний вид. Для авторского комплекта достаточно заменить layers у материала этого набора.
		ArmorMaterial vanilla = weight.placeholder().value();
		var material = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
				TearsOfInk.id(region.id + "_" + weight.id()),
				new ArmorMaterial(weight.defense(), vanilla.enchantmentValue(), vanilla.equipSound(),
						() -> Ingredient.of(region.ingot), vanilla.layers(), weight.toughness(), 0));
		register(region, weight, material, Type.HELMET, helmet);
		register(region, weight, material, Type.CHESTPLATE, chestplate);
		register(region, weight, material, Type.LEGGINGS, leggings);
		register(region, weight, material, Type.BOOTS, boots);
	}

	private static void register(Region region, OrdinaryArmorItem.Weight weight, Holder<ArmorMaterial> material,
								 Type type, String name) {
		var item = new OrdinaryArmorItem(material, type, weight);
		ITEMS.add(Registry.register(BuiltInRegistries.ITEM, TearsOfInk.id(region.id + "_" + name), item));
	}
}
