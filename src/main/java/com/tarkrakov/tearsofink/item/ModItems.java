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
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Rarity;

/** Предметы мода (не блоки). */
public final class ModItems {
	/** Асфода: кусочки Асфоделита, материал для снаряжения. */
	public static final Item ASPHODA = register("asphoda", new Item(new Item.Properties()));
	/** Пигмент: падает с врагов и из сундуков, используется в крафтах. */
	public static final Item PIGMENT = register("pigment", new Item(new Item.Properties()));
	/** Исполненный контракт (награда за Охоту) — сдаётся Торговке душами за Пигмент. */
	public static final Item FULFILLED_CONTRACT = register("fulfilled_contract", new Item(new Item.Properties().stacksTo(16)) {
		@Override
		public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
									java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
			Descriptions.add(getDescriptionId(), tooltip);
		}
	});

	// ===== Материалы трав =====
	public static final Item BLOODROOT_CLOT = register("bloodroot_clot", new Item(new Item.Properties()));
	/** Мякоть Плотеягоды — съедобна (чуть слабее сырого мяса гусеницы), жарится в Печёную плотеягоду. */
	public static final Item FLESHBERRY_PULP = register("fleshberry_pulp", new Item(new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build())));
	/** Печёная плотеягода (чуть слабее жареного мяса гусеницы). */
	public static final Item ROASTED_FLESHBERRY = register("roasted_fleshberry", new Item(new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.5f).build())));
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
	/** Тусклый ручной фонарь (из саквояжа): слабее обычного. */
	public static final Item DIM_HAND_LANTERN = register("dim_hand_lantern",
			new HandLanternItem(18f, new Item.Properties().stacksTo(1)));
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
	/** Котомка паломника (Путь Души): потёртые щиты — полноценные щиты Души, но слабее (×0,7). */
	public static final Item WORN_RAM_SHIELD = register("worn_ram_shield",
			new SoulShieldItem(() -> WeaponAbilities.RAM, new Item.Properties().durability(300), SoulShieldItem.WORN_POWER));
	public static final Item WORN_MERCY_SHIELD = register("worn_mercy_shield",
			new SoulShieldItem(() -> WeaponAbilities.MERCY, new Item.Properties().durability(300), SoulShieldItem.WORN_POWER));
	/** Щит Души: Сияние (Shift+R). */
	public static final Item RADIANT_SHIELD = register("radiant_shield",
			new SoulShieldItem(() -> WeaponAbilities.RADIANCE, new Item.Properties().durability(500)));
	/** Щит Души: Милосердие — аура лечения (R / Shift+R). */
	public static final Item MERCY_SHIELD = register("mercy_shield",
			new SoulShieldItem(() -> WeaponAbilities.MERCY, new Item.Properties().durability(500)));
	/** Щит Души: Отпор — светлая волна, расталкивает (R / Shift+R). */
	public static final Item REPEL_SHIELD = register("repel_shield",
			new SoulShieldItem(() -> WeaponAbilities.REPEL, new Item.Properties().durability(500)));
	/** Скимитар (одноручный): Волна (R). Предмет spire_shadow_scimitar (15 ноя, модель автора; раньше wave_greatsword). */
	public static final Item WAVE_GREATSWORD = register("spire_shadow_scimitar",
			new InkGreatswordItem(() -> WeaponAbilities.WAVE, new Item.Properties(), false));
	/** Клинок Палача (двуручный): Рассечение (R). Предмет spire_torturer_greatsword (15 ноя, модель автора; раньше cleave_greatsword). */
	public static final Item CLEAVE_GREATSWORD = register("spire_torturer_greatsword",
			new InkGreatswordItem(() -> WeaponAbilities.CLEAVE, new Item.Properties()));

	/** Оружие охотников: линия и ступень — в самом предмете (HunterWeaponItem.make). */
	public static final Item HUNTER_GREATSWORD = register("hunter_greatsword",
			new com.tarkrakov.tearsofink.hunt.HunterWeaponItem(new Item.Properties(), true));
	public static final Item HUNTER_SABER = register("hunter_saber",
			new com.tarkrakov.tearsofink.hunt.HunterWeaponItem(new Item.Properties(), false));

	/** Оружие из перечня автора (архетипы и приёмы — по мере готовности движений). */
	/** Ревущая Секира (Шпиль, двуручная, парой — тоже, как Палач): урон 8, скорость 1,1; R — рывок с рассекающим взмахом (Чернила). Модель автора 15 ноя. */
	public static final Item ROARING_AXE = register("spire_roaring_axe",
			new com.tarkrakov.tearsofink.weapon.ArsenalWeaponItem(new Item.Properties(), true, com.tarkrakov.tearsofink.hunt.HunterType.SPIRE,
					() -> com.tarkrakov.tearsofink.weapon.combat.AxeMoves.RUSH, 6, -2.9f)); // 16 ноя: 9
	/** Разящий Кинжал (Шпиль, одноручный): урон 4, скорость 2,0; R — рывок с уколом (Чернила). Модель автора 15 ноя. */
	public static final Item RAPID_KNIFE = register("spire_rapid_knife",
			new com.tarkrakov.tearsofink.weapon.ArsenalWeaponItem(new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.SPIRE,
					() -> com.tarkrakov.tearsofink.weapon.combat.DaggerMoves.RUSH, 2, -2.0f)); // 16 ноя: 5
	public static final Item BLOOD_KATANA = register("blood_katana",
			new com.tarkrakov.tearsofink.weapon.ArsenalWeaponItem(new Item.Properties(), true, com.tarkrakov.tearsofink.hunt.HunterType.BLOOD));
	public static final Item BLOOD_OBSI_CLUB = register("blood_obsi_club",
			new com.tarkrakov.tearsofink.weapon.ArsenalWeaponItem(new Item.Properties(), true, com.tarkrakov.tearsofink.hunt.HunterType.BLOOD));
	public static final Item BLOOD_TALVAR = register("blood_talvar",
			new com.tarkrakov.tearsofink.weapon.ArsenalWeaponItem(new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.BLOOD, () -> com.tarkrakov.tearsofink.weapon.blood.BloodAbilities.BLOOD_ARC));
	public static final Item BLOOD_DUSSAK = register("blood_dussak",
			new com.tarkrakov.tearsofink.weapon.ArsenalWeaponItem(new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.BLOOD,
					() -> com.tarkrakov.tearsofink.weapon.blood.BloodAbilities.PARRY));

	// ===== Металлы и слитки линий (16 ноя) =====
	public static final Item RAW_PALE_METAL = register("raw_pale_metal", new Item(new Item.Properties()));
	public static final Item RAW_BLOOD_METAL = register("raw_blood_metal", new Item(new Item.Properties()));
	/** Чернильный металл — только с врагов Чернолесья и из сундуков (руды нет). */
	public static final Item RAW_INK_METAL = register("raw_ink_metal", new Item(new Item.Properties()));
	public static final Item PALE_INGOT = register("pale_ingot", new Item(new Item.Properties()));
	public static final Item BLOOD_INGOT = register("blood_ingot", new Item(new Item.Properties()));
	public static final Item INK_INGOT = register("ink_ingot", new Item(new Item.Properties()));

	// ===== Пробуждения (16 ноя) =====
	/** Осколки безумия — с Чернильных охотников (контракт с ним, убийство); идут в Проклятый алтарь. */
	public static final Item MADNESS_SHARD = register("madness_shard", new Item(new Item.Properties()));
	/** Чернильный отголосок — усиливает чернильное свойство на уровень (редко с врагов Чернолесья; гарантированно — особые заказы, позже). */
	public static final Item INK_ECHO = register("ink_echo", new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

	// ===== Возвышение в Купели (16 ноя): ресурсы линий =====
	/** Багряный Цветок — Кровь (Алая Жатва, позже). */
	public static final Item CRIMSON_FLOWER = register("crimson_flower", new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
	/** Символ Доблести — Шпиль (каждые 5 сложных контрактов). */
	public static final Item VALOR_SIGIL = register("valor_sigil", new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
	/** Эссенция надежды — с обезумевших охотников (особые заказы); Торговка меняет 3 → Частичку Надежды. */
	public static final Item HOPE_ESSENCE = register("hope_essence", new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
	/** Частичка Надежды — Чернила (особые заказы Торговки, позже). */
	public static final Item HOPE_MOTE = register("hope_mote", new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

	/** Дар Багряного Вестника — сумка Алой Жатвы (ПКМ — открыть). */
	public static final Item CRIMSON_GIFT = register("crimson_gift",
			new com.tarkrakov.tearsofink.hunt.CrimsonGiftItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE)));

	// ===== Обычное оружие линий (16 ноя; модели — заглушки-перекраски, автор нарисует) =====
	// итоговый урон / скорость: Бледное и Кровавое 5 и 8, Чернильное 6 и 9; Бледное быстрее
	/** Лёгкий клинок Бледнолесья: быстрые узкие колющие атаки.
	 * Урон/скорость — рабочий локальный баланс; текстура и движения временные. */
	public static final Item PALE_MISERICORDE = register("pale_misericorde",
			new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
					new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.SPIRE, 4, 2.1f));
	public static final Item PALE_SWORD = register("pale_sword", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.SPIRE, 5, 1.7f));
	/** Первый предмет утверждённой средней бледной линейки. Временный боевой профиль —
	 * характеристики существующего PALE_SWORD до согласования собственного приёма/баланса. */
	public static final Item PALE_LONGSWORD = register("pale_longsword", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.SPIRE, 5, 1.7f));
	public static final Item PALE_GREATSWORD = register("pale_greatsword", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), true, com.tarkrakov.tearsofink.hunt.HunterType.SPIRE, 8, 1.1f));
	/** Бледный линейный двуручник: рабочий локальный баланс, временный визуал. */
	public static final Item PALE_LINEAR_GREATSWORD = register("pale_linear_greatsword", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), true, com.tarkrakov.tearsofink.hunt.HunterType.SPIRE, 8, 1.1f));
	public static final Item BLOOD_SWORD = register("blood_sword", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.BLOOD, 5, 1.5f));
	public static final Item BLOOD_GREATSWORD = register("blood_greatsword", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), true, com.tarkrakov.tearsofink.hunt.HunterType.BLOOD, 8, 1.0f));
	/** Алый Катар: рабочий локальный баланс, временный визуал. */
	public static final Item BLOOD_KATAR = register("blood_katar", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.BLOOD, 4, 1.8f));
	/** Алая Пата: рабочий локальный баланс, временный визуал. */
	public static final Item BLOOD_PATA = register("blood_pata", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.BLOOD, 5, 1.5f));
	/** Алый кривой двуручник: рабочий локальный баланс, временный визуал. */
	public static final Item BLOOD_CURVED_GREATSWORD = register("blood_curved_greatsword", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), true, com.tarkrakov.tearsofink.hunt.HunterType.BLOOD, 8, 1f));
	public static final Item INK_SWORD = register("ink_sword", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.INK, 6, 1.5f));
	public static final Item INK_GREATSWORD = register("ink_greatsword", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), true, com.tarkrakov.tearsofink.hunt.HunterType.INK, 9, 1.0f));

	/** Чернильный Сакс: рабочий локальный баланс, временный визуал. */
	public static final Item INK_SEAX = register("ink_seax", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.INK, 5, 1.7f));
	/** Чернильный Фальшион: рабочий локальный баланс, временный визуал. */
	public static final Item INK_FALCHION = register("ink_falchion", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), false, com.tarkrakov.tearsofink.hunt.HunterType.INK, 6, 1.4f));
	/** Чернильный Крегсмессер: рабочий локальный баланс, временный визуал. */
	public static final Item INK_KRIEGSMESSER = register("ink_kriegsmesser", new com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem(
			new Item.Properties(), true, com.tarkrakov.tearsofink.hunt.HunterType.INK, 9, 1f));

	// ===== Добыча с фауны =====
	/** Шёлковая нить (бледные гусеницы) — работает в рецептах вместо ванильной нити. */
	public static final Item SILK_THREAD = register("silk_thread", new Item(new Item.Properties()));
	/** Сгусток крови (кровавые гусеницы, Кровосборник). */
	public static final Item BLOOD_CLOT = register("blood_clot", new Item(new Item.Properties()));
	/** Кристалл крови — редко застывает в Накопителе Кровосборника (для крафтов). */
	public static final Item BLOOD_CRYSTAL = register("blood_crystal", new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
	/** Чернила (чернильные гусеницы). */
	public static final Item INK = register("ink", new Item(new Item.Properties()));
	/** Мясо гусеницы: сырое хуже — но на стадии «Порченый» и ниже насыщает лучше жареного. */
	public static final Item RAW_CATERPILLAR_MEAT = register("raw_caterpillar_meat", new Item(new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.15f).build())));
	public static final Item COOKED_CATERPILLAR_MEAT = register("cooked_caterpillar_meat", new Item(new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build())));
	/** Яйцо призыва гусеницы (для креатива; вариант — по зоне, где выпустили). */
	public static final Item CATERPILLAR_SPAWN_EGG = register("caterpillar_spawn_egg",
			new SpawnEggItem(com.tarkrakov.tearsofink.entity.ModMobs.CATERPILLAR, 0x7FD8C9, 0x2E4B52, new Item.Properties()));

	public static final Item PALE_OWL_SPAWN_EGG = register("pale_owl_spawn_egg",
			new SpawnEggItem(com.tarkrakov.tearsofink.entity.ModMobs.PALE_OWL, 0xD9D8D0, 0x68666C, new Item.Properties()));
	public static final Item PALE_DEER_SPAWN_EGG = register("pale_deer_spawn_egg",
			new SpawnEggItem(com.tarkrakov.tearsofink.entity.ModMobs.PALE_DEER, 0xD7D6CA, 0x555660, new Item.Properties()));
	public static final Item PALE_STRIDER_SPAWN_EGG = register("pale_strider_spawn_egg",
			new SpawnEggItem(com.tarkrakov.tearsofink.entity.ModMobs.PALE_STRIDER, 0xA9A6A8, 0x362F39, new Item.Properties()));

	public static final Item PALE_SPEARMAN_SPAWN_EGG = register("pale_spearman_spawn_egg",
			new SpawnEggItem(com.tarkrakov.tearsofink.entity.ModMobs.PALE_SPEARMAN, 0xD8DCE0, 0x3A3F4A, new Item.Properties()));

	public static final Item PALE_SHIELDBEARER_SPAWN_EGG = register("pale_shieldbearer_spawn_egg",
			new SpawnEggItem(com.tarkrakov.tearsofink.entity.ModMobs.PALE_SHIELDBEARER, 0xC9CDD2, 0x5A4A3A, new Item.Properties()));
	public static final Item GUIDE_SPAWN_EGG = register("guide_spawn_egg",
			new SpawnEggItem(com.tarkrakov.tearsofink.entity.ModMobs.GUIDE, 0x1A1418, 0xB5475A, new Item.Properties()));
	public static final Item PALE_CROSSBOWMAN_SPAWN_EGG = register("pale_crossbowman_spawn_egg",
			new SpawnEggItem(com.tarkrakov.tearsofink.entity.ModMobs.PALE_CROSSBOWMAN, 0xE2E5E8, 0x6B3A2E, new Item.Properties()));

	// ===== Иконки достижений (не добываются, во вкладке не лежат) =====
	public static final Item ICON_INK_LANDS = register("icon_ink_lands", new Item(new Item.Properties()));
	public static final Item ICON_FIRST_FLAME = register("icon_first_flame", new Item(new Item.Properties()));
	public static final Item ICON_CURSED_VESSEL = register("icon_cursed_vessel", new Item(new Item.Properties()));
	public static final Item ICON_CURSE_ESSENCE = register("icon_curse_essence", new Item(new Item.Properties()));
	public static final Item ICON_ATONEMENT = register("icon_atonement", new Item(new Item.Properties()));
	public static final Item ICON_SOUL_PILGRIM = register("icon_soul_pilgrim", new Item(new Item.Properties()));
	public static final Item ICON_INK_SHATTERED_HEART = register("icon_ink_shattered_heart", new Item(new Item.Properties()));

	// ===== Книги навыков =====
	/** Книга Рывка (название и текст — рабочие, автор придумает свои). */
	public static final Item SKILL_BOOK_DASH = register("skill_book_dash",
			new SkillBookItem(() -> ModSkills.DASH, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	/** Чёрная книга (Путь Чернил): Чернильный прыжок. */
	public static final Item SKILL_BOOK_INK_JUMP = register("skill_book_ink_jump",
			new SkillBookItem(() -> ModSkills.INK_JUMP, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	/** Белые книги (Путь Души): Могучая поступь и Вознесение духа. */
	public static final Item SKILL_BOOK_STRIDE = register("skill_book_stride",
			new SkillBookItem(() -> ModSkills.STRIDE, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final Item SKILL_BOOK_ASCENSION = register("skill_book_ascension",
			new SkillBookItem(() -> ModSkills.ASCENSION, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

	private ModItems() {}

	/** Тусклое зеркальце: переход между обычным миром и Землями. Неотделимо от игрока (вне творческого). */
	public static final Item DIM_MIRROR = register("dim_mirror",
			new com.tarkrakov.tearsofink.travel.DimMirrorItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON)));

	// ===== Пролог =====
	public static final Item SEALED_LETTER = register("sealed_letter",
			new com.tarkrakov.tearsofink.prologue.PrologueItems.SealedLetter(new Item.Properties().stacksTo(1)));
	public static final Item INVITATION_LETTER = register("invitation_letter",
			new com.tarkrakov.tearsofink.prologue.PrologueItems.Invitation(new Item.Properties().stacksTo(1)));
	public static final Item WORN_SATCHEL = register("worn_satchel",
			new com.tarkrakov.tearsofink.prologue.PrologueItems.Satchel(new Item.Properties().stacksTo(1)));
	/** Котомка паломника — стартовый набор Пути Души. */
	public static final Item PILGRIM_BUNDLE = register("pilgrim_bundle",
			new com.tarkrakov.tearsofink.prologue.PrologueItems.PilgrimBundle(new Item.Properties().stacksTo(1)));
	/** Тусклый скимитар — стартовое оружие Пути Чернил: удары Скимитаров, урон 4, тусклая Волна (R). */
	public static final Item DIM_SCIMITAR = register("dim_scimitar",
			new com.tarkrakov.tearsofink.weapon.InkGreatswordItem(() -> com.tarkrakov.tearsofink.weapon.WeaponAbilities.DIM_WAVE,
					new Item.Properties(), false, com.tarkrakov.tearsofink.weapon.InkGreatswordItem.ONE_HANDED_DAMAGE - 2)); // итого 4 (охотничье подняли на +1, тусклый остаётся слабее)
	public static final Item DIARY = register("diary",
			new com.tarkrakov.tearsofink.prologue.PrologueItems.Diary(new Item.Properties().stacksTo(1)));
	/** Базовое оружие Чернил: Чернильная клякса (R). */
	public static final Item INK_LONGKNIFE = register("ink_longknife",
			new com.tarkrakov.tearsofink.weapon.InkLongknifeItem(new Item.Properties()));
	/** Базовое оружие Души: лечение (Shift+R в левой руке). */
	public static final Item SALVATION_PENDANT = register("salvation_pendant",
			new com.tarkrakov.tearsofink.weapon.SalvationPendantItem(new Item.Properties().stacksTo(1)));
	/** Только вид снаряда кляксы (во вкладке нет). */
	public static final Item INK_BLOT = register("ink_blot", new Item(new Item.Properties()));

	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, TearsOfInk.id(name), item);
	}

	public static void init() {
		ModArmor.init();
	}
}
