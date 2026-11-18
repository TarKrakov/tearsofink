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

	/** Quiet source with a long server delivery range, independent of volume. */
	public static final SoundEvent STRIDER_SONG = Registry.register(BuiltInRegistries.SOUND_EVENT,
			TearsOfInk.id("strider_song"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("strider_song"), (float) StriderSong.DELIVERY_RANGE));

	/** Оригинальные короткие звуки ползания, по три сэмпла на вид. */
	public static final SoundEvent CATERPILLAR_PALE_CRAWL = register("caterpillar_pale_crawl");
	public static final SoundEvent CATERPILLAR_BLOOD_CRAWL = register("caterpillar_blood_crawl");
	public static final SoundEvent CATERPILLAR_INK_CRAWL = register("caterpillar_ink_crawl");

	/** Quiet positional owl sounds. */
	public static final SoundEvent PALE_OWL_HOOT = Registry.register(BuiltInRegistries.SOUND_EVENT,
			TearsOfInk.id("pale_owl_hoot"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_owl_hoot"), 26.0f));
	public static final SoundEvent PALE_OWL_WING = register("pale_owl_wing");

	private ModSounds() {}

	private static SoundEvent register(String name) {
		var id = TearsOfInk.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() { StriderSong.init(); }
}
