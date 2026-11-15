package com.tarkrakov.tearsofink.sound;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** Свои звуки мода (файлы — assets/tears-of-ink/sounds/, список — sounds.json). Заменить — положить .ogg поверх с тем же именем. */
public final class ModSounds {
	/** Щиты Души на полном заряде принимают удар — тихий светлый хор (заглушка Claude, синтез). */
	public static final SoundEvent SHIELD_CHOIR = register("shield_choir");

	private ModSounds() {}

	private static SoundEvent register(String name) {
		var id = TearsOfInk.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {}
}
