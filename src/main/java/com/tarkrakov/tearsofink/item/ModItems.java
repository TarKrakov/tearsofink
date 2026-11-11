package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.skill.ModSkills;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** Предметы мода (не блоки). */
public final class ModItems {
	/** Асфода: кусочки Асфоделита, материал для снаряжения. */
	public static final Item ASPHODA = register("asphoda", new Item(new Item.Properties()));
	/** Пигмент: падает с врагов и из сундуков, используется в крафтах. */
	public static final Item PIGMENT = register("pigment", new Item(new Item.Properties()));

	// ===== Материалы трав =====
	public static final Item BLOODROOT_CLOT = register("bloodroot_clot", new Item(new Item.Properties()));
	public static final Item FLESHBERRY_PULP = register("fleshberry_pulp", new Item(new Item.Properties()));
	public static final Item INKROOT_ROOT = register("inkroot_root", new Item(new Item.Properties()));
	public static final Item TARWEED_EXTRACT = register("tarweed_extract", new Item(new Item.Properties()));
	public static final Item WATERBLOOM_JUICE = register("waterbloom_juice", new Item(new Item.Properties()));
	public static final Item DUCKWEED_PODS = register("duckweed_pods", new Item(new Item.Properties()));
	/** Трясинник (он же «кувшинка Трясинника») — ставится на воду. */
	public static final Item BOGLEAF = ModBlocks.bogleafItem();

	// ===== Ручные фонари (не светят — только рассеивают мглу) =====
	/** Видимость в тумане с фонарём в левой руке (в хотбаре — на 20% меньше). */
	public static final Item HAND_LANTERN = register("hand_lantern",
			new HandLanternItem(26f, new Item.Properties().stacksTo(1)));
	public static final Item BRIGHT_HAND_LANTERN = register("bright_hand_lantern",
			new HandLanternItem(44f, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

	/** Искупление — венок-крест с сердцем: +10 Души. */
	public static final Item ATONEMENT = register("atonement",
			new AtonementItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

	// ===== Книги навыков =====
	/** Книга Рывка (название и текст — рабочие, автор придумает свои). */
	public static final Item SKILL_BOOK_DASH = register("skill_book_dash",
			new SkillBookItem(() -> ModSkills.DASH, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

	private ModItems() {}

	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, TearsOfInk.id(name), item);
	}

	public static void init() {}
}
