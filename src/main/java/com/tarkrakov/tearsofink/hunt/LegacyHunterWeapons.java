package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Совместимость с техническими клинками старых контрактов и сохранений (D-107). */
public final class LegacyHunterWeapons {
	private LegacyHunterWeapons() {}

	public static Item legacyTarget(HunterType line, boolean greatsword) {
		return switch (line) {
			case BLOOD -> greatsword ? ModItems.BLOOD_DUSSAK : ModItems.BLOOD_TALVAR;
			// TODO(D-107): собственный охотничий арсенал Чернил вместо временного пула Шпиля.
			case SPIRE, INK -> greatsword ? ModItems.CLEAVE_GREATSWORD : ModItems.WAVE_GREATSWORD;
		};
	}

	/** Старые предметы видимыми больше не делаем; ID и содержимое старых миров не удаляются. */
	public static boolean isRetired(Item item) {
		return item == ModItems.HUNTER_GREATSWORD || item == ModItems.HUNTER_SABER
				|| item == ModItems.PALE_SWORD || item == ModItems.PALE_GREATSWORD
				|| item == ModItems.BLOOD_SWORD || item == ModItems.BLOOD_GREATSWORD
				|| item == ModItems.INK_SWORD || item == ModItems.INK_GREATSWORD
				|| item == ModItems.BLOOD_KATANA || item == ModItems.BLOOD_OBSI_CLUB;
	}

	/** Мигрируем только hunter_saber/greatsword, не перезаписываем обычные старые мечи. */
	public static ItemStack migrate(ItemStack source) {
		if (source.isEmpty() || !(source.getItem() instanceof HunterWeaponItem)) return source;
		Item target = legacyTarget(HunterWeaponItem.line(source), source.is(ModItems.HUNTER_GREATSWORD));
		ItemStack result = new ItemStack(target, source.getCount());
		var targetAttributes = result.get(DataComponents.ATTRIBUTE_MODIFIERS);
		// Сохранить ступень, заточку, пробуждения, имя и остальную пользовательскую информацию.
		result.applyComponents(source.getComponentsPatch());
		// Не переносить старую перекраску и модификаторы урона технической заглушки.
		result.remove(DataComponents.CUSTOM_MODEL_DATA);
		if (targetAttributes != null) result.set(DataComponents.ATTRIBUTE_MODIFIERS, targetAttributes);
		else result.remove(DataComponents.ATTRIBUTE_MODIFIERS);
		CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.remove("HunterLine"));
		return result;
	}

	/** Переводим сохранённые личные вещи при входе; сундуки и чужие контейнеры не переписываем. */
	public static void migrateInventory(ServerPlayer player) {
		var inv = player.getInventory();
		boolean changed = false;
		for (int slot = 0; slot < inv.getContainerSize(); slot++) {
			ItemStack stack = inv.getItem(slot);
			if (stack.getItem() instanceof HunterWeaponItem) {
				inv.setItem(slot, migrate(stack));
				changed = true;
			}
		}
		if (changed) player.inventoryMenu.broadcastChanges();
	}
}
