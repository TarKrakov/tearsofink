package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/** Предметы мода (не блоки). */
public final class ModItems {
	/** Асфода: кусочки Асфоделита, материал для снаряжения. */
	public static final Item ASPHODA = register("asphoda", new Item(new Item.Properties()));
	/** Пигмент: падает с врагов и из сундуков, используется в крафтах. */
	public static final Item PIGMENT = register("pigment", new Item(new Item.Properties()));

	private ModItems() {}

	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, TearsOfInk.id(name), item);
	}

	public static void init() {}
}
