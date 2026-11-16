package com.tarkrakov.tearsofink.weapon.combat;

import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Архетип клинка — набор ударов и движений. Новый клинок просто называет свой архетип и получает всё готовое.
 * Удары (урон, дуга, тайминги) — data/tears-of-ink/weapon_combos/<id>.json (правка → /reload),
 * движения от первого лица — assets/tears-of-ink/weapon_anims/<id>.json (правка → F3+T).
 */
public enum Archetype {
	/** Клинки Палачей: длинные прямые двуручные, тяжёлые и плавные. Пример — Меч Рассечения. */
	EXECUTIONER("executioner"),
	/** Разрывающие Скимитары: изогнутые одноручные, быстрые «танцующие» удары. Пример — Меч Волны. */
	SCIMITAR("scimitar"),
	/** Парящие Тальвары (Кровь): одноручные режущие, удары-танец, переходы клинка разворотом. */
	TALWAR("talwar"),
	/** Хищные Дуссаки (Кровь): короткие одноручные тесаки — тычок, разрыв, росчерк; R — парирование с ответом. */
	DUSSAK("dussak"),
	/** Бастион — удары двумя щитами Души (Путь Души, работает только парой щитов и в Землях): толчок правым, левым, рубящий обоими. */
	BASTION("bastion"),
	/** Ревущая Секира (Шпиль): одной рукой и парой. */
	AXE("axe"),
	/** Разящий Кинжал (Шпиль): быстрые уколы, пируэт, рывок с уколом; парой — росчерки и рывок с вонзанием. */
	DAGGER("dagger"),
	/** Бледный мизерикорд: колющие связки без расхода ресурсов. */
	MISERICORDE("misericorde"),
	/** Бледный полуторный клинок: независимый архетип для последующей авторской анимации. */
	PALE_LONGSWORD("pale_longsword"),
	/** Бледный линейный двуручник: независимый архетип для последующей авторской анимации. */
	PALE_LINEAR_GREATSWORD("pale_linear_greatsword"),
	/** Алый Катар: независимый архетип для последующей авторской анимации. */
	BLOOD_KATAR("blood_katar"),
	/** Алая Пата: независимый архетип для последующей авторской анимации. */
	BLOOD_PATA("blood_pata"),
	/** Алый кривой двуручник: независимый архетип для последующей авторской анимации. */
	BLOOD_CURVED_GREATSWORD("blood_curved_greatsword"),
	/** Чернильный Сакс: независимый архетип для последующей авторской анимации. */
	INK_SEAX("ink_seax"),
	/** Чернильный Фальшион: независимый архетип для последующей авторской анимации. */
	INK_FALCHION("ink_falchion"),
	/** Чернильный Крегсмессер: независимый архетип для последующей авторской анимации. */
	INK_KRIEGSMESSER("ink_kriegsmesser");

	public final String id;

	Archetype(String id) {
		this.id = id;
	}

	private static Map<Item, Archetype> items;

	private static Map<Item, Archetype> items() {
		if (items == null) {
			items = Map.ofEntries(
					Map.entry(ModItems.CLEAVE_GREATSWORD, EXECUTIONER),
					Map.entry(ModItems.WAVE_GREATSWORD, SCIMITAR),
					Map.entry(ModItems.DIM_SCIMITAR, SCIMITAR),
					Map.entry(ModItems.PALE_MISERICORDE, MISERICORDE),
					Map.entry(ModItems.PALE_SWORD, SCIMITAR), Map.entry(ModItems.PALE_LONGSWORD, PALE_LONGSWORD),
					Map.entry(ModItems.BLOOD_SWORD, SCIMITAR), Map.entry(ModItems.INK_SWORD, SCIMITAR),
					Map.entry(ModItems.PALE_LINEAR_GREATSWORD, PALE_LINEAR_GREATSWORD),
					Map.entry(ModItems.BLOOD_KATAR, BLOOD_KATAR),
					Map.entry(ModItems.BLOOD_PATA, BLOOD_PATA),
					Map.entry(ModItems.BLOOD_CURVED_GREATSWORD, BLOOD_CURVED_GREATSWORD),
					Map.entry(ModItems.INK_SEAX, INK_SEAX),
					Map.entry(ModItems.INK_FALCHION, INK_FALCHION),
					Map.entry(ModItems.INK_KRIEGSMESSER, INK_KRIEGSMESSER),
					Map.entry(ModItems.PALE_GREATSWORD, EXECUTIONER), Map.entry(ModItems.BLOOD_GREATSWORD, EXECUTIONER),
					Map.entry(ModItems.INK_GREATSWORD, EXECUTIONER),
					Map.entry(ModItems.HUNTER_GREATSWORD, EXECUTIONER),
					Map.entry(ModItems.HUNTER_SABER, SCIMITAR),
					Map.entry(ModItems.BLOOD_TALVAR, TALWAR),
					Map.entry(ModItems.BLOOD_DUSSAK, DUSSAK),
					Map.entry(ModItems.RAM_SHIELD, BASTION),
					Map.entry(ModItems.RADIANT_SHIELD, BASTION),
					Map.entry(ModItems.MERCY_SHIELD, BASTION),
					Map.entry(ModItems.REPEL_SHIELD, BASTION),
					Map.entry(ModItems.WORN_RAM_SHIELD, BASTION),
					Map.entry(ModItems.WORN_MERCY_SHIELD, BASTION),
					Map.entry(ModItems.ROARING_AXE, AXE),
					Map.entry(ModItems.RAPID_KNIFE, DAGGER));
		}
		return items;
	}

	@Nullable
	public static Archetype of(ItemStack stack) {
		return stack.isEmpty() ? null : items().get(stack.getItem());
	}

	@Nullable
	public static Archetype byId(String id) {
		for (Archetype a : values()) if (a.id.equals(id)) return a;
		return null;
	}
}
