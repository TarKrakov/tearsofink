package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.skill.ModSkills;
import com.tarkrakov.tearsofink.effect.ModEffects;
import com.tarkrakov.tearsofink.weapon.InkGreatswordItem;
import com.tarkrakov.tearsofink.weapon.SoulShieldItem;
import com.tarkrakov.tearsofink.weapon.WeaponAbilities;
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

	// ===== Алхимия (Алхимический стол) =====
	/** Пустой флакон — сосуд для зелий мода. */
	public static final Item EMPTY_FLASK = register("empty_flask", new Item(new Item.Properties()));
	/** Отвар Слёзницы: 3 минуты «Утешения Слёзницы» (штраф за душу вдвое слабее). */
	public static final Item TEARBLOOM_BREW = register("tearbloom_brew",
			new BrewItem(() -> ModEffects.TEAR_SOLACE, 3 * 60 * 20, new Item.Properties().stacksTo(16)));
	/** Крепкий отвар Слёзницы: 8 минут. */
	public static final Item STRONG_TEARBLOOM_BREW = register("strong_tearbloom_brew",
			new BrewItem(() -> ModEffects.TEAR_SOLACE, 8 * 60 * 20, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

	/** Искупление — венок-крест с сердцем: +10 Души. */
	public static final Item ATONEMENT = register("atonement",
			new AtonementItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

	// ===== Оружие (названия — рабочие, автор меняет в ru_ru.json) =====
	/** Щит Души: Таран (Shift+R). */
	public static final Item RAM_SHIELD = register("ram_shield",
			new SoulShieldItem(() -> WeaponAbilities.RAM, new Item.Properties().durability(500)));
	/** Щит Души: Сияние (Shift+R). */
	public static final Item RADIANT_SHIELD = register("radiant_shield",
			new SoulShieldItem(() -> WeaponAbilities.RADIANCE, new Item.Properties().durability(500)));
	/** Щит Души: Милосердие — аура лечения (R / Shift+R). */
	public static final Item MERCY_SHIELD = register("mercy_shield",
			new SoulShieldItem(() -> WeaponAbilities.MERCY, new Item.Properties().durability(500)));
	/** Щит Души: Отпор — светлая волна, расталкивает (R / Shift+R). */
	public static final Item REPEL_SHIELD = register("repel_shield",
			new SoulShieldItem(() -> WeaponAbilities.REPEL, new Item.Properties().durability(500)));
	/** Двуручник Чернил: Волна (R). */
	public static final Item WAVE_GREATSWORD = register("wave_greatsword",
			new InkGreatswordItem(() -> WeaponAbilities.WAVE, new Item.Properties()));
	/** Двуручник Чернил: Рассечение (R). */
	public static final Item CLEAVE_GREATSWORD = register("cleave_greatsword",
			new InkGreatswordItem(() -> WeaponAbilities.CLEAVE, new Item.Properties()));

	// ===== Иконки достижений (не добываются, во вкладке не лежат) =====
	public static final Item ICON_INK_LANDS = register("icon_ink_lands", new Item(new Item.Properties()));
	public static final Item ICON_FIRST_FLAME = register("icon_first_flame", new Item(new Item.Properties()));
	public static final Item ICON_CURSED_VESSEL = register("icon_cursed_vessel", new Item(new Item.Properties()));
	public static final Item ICON_CURSE_ESSENCE = register("icon_curse_essence", new Item(new Item.Properties()));
	public static final Item ICON_ATONEMENT = register("icon_atonement", new Item(new Item.Properties()));

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
