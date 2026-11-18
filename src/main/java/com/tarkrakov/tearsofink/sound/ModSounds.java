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

	/** Original pale soldier combat audio. */
	public static final SoundEvent PALE_CROSSBOW_AMBIENT = register("pale_crossbow_ambient");
	public static final SoundEvent PALE_CROSSBOW_BOLT_IMPACT = register("pale_crossbow_bolt_impact");
	public static final SoundEvent PALE_CROSSBOW_DEATH = register("pale_crossbow_death");
	public static final SoundEvent PALE_CROSSBOW_HURT = register("pale_crossbow_hurt");
	public static final SoundEvent PALE_CROSSBOW_RELOAD_BOLT = register("pale_crossbow_reload_bolt");
	public static final SoundEvent PALE_CROSSBOW_RELOAD_DRAW = register("pale_crossbow_reload_draw");
	public static final SoundEvent PALE_CROSSBOW_RELOAD_LATCH = register("pale_crossbow_reload_latch");
	public static final SoundEvent PALE_CROSSBOW_SHOOT = register("pale_crossbow_shoot");
	public static final SoundEvent PALE_CROSSBOW_STEP = register("pale_crossbow_step");
	public static final SoundEvent PALE_SHIELD_AMBIENT = register("pale_shield_ambient");
	public static final SoundEvent PALE_SHIELD_BASH_HIT = register("pale_shield_bash_hit");
	public static final SoundEvent PALE_SHIELD_BASH_SWING = register("pale_shield_bash_swing");
	public static final SoundEvent PALE_SHIELD_BLOCK = register("pale_shield_block");
	public static final SoundEvent PALE_SHIELD_CHARGE_BREAK_SOFT = register("pale_shield_charge_break_soft");
	public static final SoundEvent PALE_SHIELD_CHARGE_HIT = register("pale_shield_charge_hit");
	public static final SoundEvent PALE_SHIELD_CHARGE_PREPARE = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("pale_shield_charge_prepare"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_shield_charge_prepare"), 22.0f));
	public static final SoundEvent PALE_SHIELD_CHARGE_START = register("pale_shield_charge_start");
	public static final SoundEvent PALE_SHIELD_CHARGE_STEP = register("pale_shield_charge_step");
	public static final SoundEvent PALE_SHIELD_CHARGE_WALL = register("pale_shield_charge_wall");
	public static final SoundEvent PALE_SHIELD_DEATH = register("pale_shield_death");
	public static final SoundEvent PALE_SHIELD_DEFLECT = register("pale_shield_deflect");
	public static final SoundEvent PALE_SHIELD_DISABLED = register("pale_shield_disabled");
	public static final SoundEvent PALE_SHIELD_HURT = register("pale_shield_hurt");
	public static final SoundEvent PALE_SHIELD_RAISE = register("pale_shield_raise");
	public static final SoundEvent PALE_SHIELD_STEP = register("pale_shield_step");
	public static final SoundEvent PALE_SHIELD_STUN = register("pale_shield_stun");
	public static final SoundEvent PALE_SOLDIER_DODGE = register("pale_soldier_dodge");
	public static final SoundEvent PALE_SPEAR_AMBIENT = register("pale_spear_ambient");
	public static final SoundEvent PALE_SPEAR_DEATH = register("pale_spear_death");
	public static final SoundEvent PALE_SPEAR_HIT = register("pale_spear_hit");
	public static final SoundEvent PALE_SPEAR_HURT = register("pale_spear_hurt");
	public static final SoundEvent PALE_SPEAR_STEP = register("pale_spear_step");
	public static final SoundEvent PALE_SPEAR_THRUST = register("pale_spear_thrust");

	/** Approved Soul shield sound palette D. */
	public static final SoundEvent SOUL_SHIELD_BASTION = register("soul_shield_bastion");
	public static final SoundEvent SOUL_SHIELD_BLOCK = register("soul_shield_block");
	public static final SoundEvent SOUL_SHIELD_BREAK = register("soul_shield_break");
	public static final SoundEvent SOUL_SHIELD_BURST_FULL = register("soul_shield_burst_full");
	public static final SoundEvent SOUL_SHIELD_BURST_LOW = register("soul_shield_burst_low");
	public static final SoundEvent SOUL_SHIELD_CHARGE_1 = register("soul_shield_charge_1");
	public static final SoundEvent SOUL_SHIELD_CHARGE_2 = register("soul_shield_charge_2");
	public static final SoundEvent SOUL_SHIELD_CHARGE_3 = register("soul_shield_charge_3");
	public static final SoundEvent SOUL_SHIELD_CHARGE_4 = register("soul_shield_charge_4");
	public static final SoundEvent SOUL_SHIELD_CLAP = register("soul_shield_clap");
	public static final SoundEvent SOUL_SHIELD_CLAP_PREPARE = register("soul_shield_clap_prepare");
	public static final SoundEvent SOUL_SHIELD_FULL_CHARGE = register("soul_shield_full_charge");
	public static final SoundEvent SOUL_SHIELD_PERFECT = register("soul_shield_perfect");
	public static final SoundEvent SOUL_SHIELD_RAISE = register("soul_shield_raise");
	public static final SoundEvent SOUL_SHIELD_RIPOSTE = register("soul_shield_riposte");
	public static final SoundEvent SOUL_SHIELD_MERCY = register("soul_shield_mercy");
	public static final SoundEvent SOUL_SHIELD_RADIANCE = register("soul_shield_radiance");
	public static final SoundEvent SOUL_SHIELD_RAM_HIT = register("soul_shield_ram_hit");
	public static final SoundEvent SOUL_SHIELD_RAM_START = register("soul_shield_ram_start");
	public static final SoundEvent SOUL_SHIELD_REPEL = register("soul_shield_repel");
	public static final SoundEvent SOUL_SHIELD_RES_ADVANCE_IMPACT = register("soul_shield_res_advance_impact");
	public static final SoundEvent SOUL_SHIELD_RES_ADVANCE_START = register("soul_shield_res_advance_start");
	public static final SoundEvent SOUL_SHIELD_RES_AVALANCHE = register("soul_shield_res_avalanche");
	public static final SoundEvent SOUL_SHIELD_RES_FLASH = register("soul_shield_res_flash");
	public static final SoundEvent SOUL_SHIELD_RES_GRACE = register("soul_shield_res_grace");
	public static final SoundEvent SOUL_SHIELD_RES_HALO = register("soul_shield_res_halo");
	public static final SoundEvent SOUL_SHIELD_RES_INTERCESSOR = register("soul_shield_res_intercessor");
	public static final SoundEvent SOUL_SHIELD_RES_PHALANX = register("soul_shield_res_phalanx");
	public static final SoundEvent SOUL_SHIELD_RES_SHOCKWAVE = register("soul_shield_res_shockwave");
	public static final SoundEvent SOUL_SHIELD_RES_SUN = register("soul_shield_res_sun");
	public static final SoundEvent SOUL_SHIELD_RES_VEIL = register("soul_shield_res_veil");

	private ModSounds() {}

	private static SoundEvent register(String name) {
		var id = TearsOfInk.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() { StriderSong.init(); }
}
