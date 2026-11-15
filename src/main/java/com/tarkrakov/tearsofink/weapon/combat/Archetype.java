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
	BASTION("bastion");

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
					Map.entry(ModItems.HUNTER_GREATSWORD, EXECUTIONER),
					Map.entry(ModItems.HUNTER_SABER, SCIMITAR),
					Map.entry(ModItems.BLOOD_TALVAR, TALWAR),
					Map.entry(ModItems.BLOOD_DUSSAK, DUSSAK),
					Map.entry(ModItems.RAM_SHIELD, BASTION),
					Map.entry(ModItems.RADIANT_SHIELD, BASTION),
					Map.entry(ModItems.MERCY_SHIELD, BASTION),
					Map.entry(ModItems.REPEL_SHIELD, BASTION),
					Map.entry(ModItems.WORN_RAM_SHIELD, BASTION),
					Map.entry(ModItems.WORN_MERCY_SHIELD, BASTION));
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
