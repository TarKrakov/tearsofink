package com.tarkrakov.tearsofink.sound;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** Свои звуки мода (файлы — assets/tears-of-ink/sounds/, список — sounds.json). Заменить — положить .ogg поверх с тем же именем. */
public final class ModSounds {
	/** Щиты Души на полном заряде принимают удар — тихий светлый хор (заглушка Claude, синтез). */
	public static final SoundEvent SHIELD_CHOIR = register("shield_choir");
	/** Тихое перелистывание Дневника: отдельное событие для независимой настройки звука. */
	public static final SoundEvent DIARY_PAGE_TURN = register("diary_page_turn");
	/** Шорох пера при первой записи в Дневник (только личное уведомление). */
	public static final SoundEvent DIARY_QUILL_WRITE = register("diary_quill_write");

	private ModSounds() {}

	private static SoundEvent register(String name) {
		var id = TearsOfInk.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {}
}
