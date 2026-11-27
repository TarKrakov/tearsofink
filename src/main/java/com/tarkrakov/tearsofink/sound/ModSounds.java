package com.tarkrakov.tearsofink.sound;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** Свои звуки мода (файлы — assets/tears-of-ink/sounds/, список — sounds.json). Заменить — положить .ogg поверх с тем же именем. */
public final class ModSounds {
    public static final SoundEvent TROLL_CHOMP = register("troll_chomp");
    public static final SoundEvent TROLL_SNORT = register("troll_snort");
    public static final SoundEvent TROLL_LAUGH = register("troll_laugh");
    public static final SoundEvent TROLL_SNORE = register("troll_snore");
    public static final SoundEvent TROLL_SLAM = register("troll_slam");
    public static final SoundEvent MOROK_WHISPER = register("morok_whisper");
    public static final SoundEvent MOROK_CRY = register("morok_cry");
    public static final SoundEvent MOROK_FUSE = register("morok_fuse");
    public static final SoundEvent MOROK_BURST = register("morok_burst");
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
			TearsOfInk.id("pale_owl_hoot"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_owl_hoot"), 50.0f));
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

	/** Pale Forest ambience pilot. */
	public static final SoundEvent PALE_FOREST_AIR = register("pale_forest_air");
	public static final SoundEvent PALE_FOREST_MEMORY_AIR = register("pale_forest_memory_air");
	public static final SoundEvent PALE_FOREST_FOG_AIR = register("pale_forest_fog_air");
	public static final SoundEvent PALE_FOREST_MEMORY_01 = register("pale_forest_memory_01");
	public static final SoundEvent PALE_FOREST_MEMORY_02 = register("pale_forest_memory_02");
	public static final SoundEvent PALE_FOREST_FOG_PHRASE_01 = register("pale_forest_fog_phrase_01");
	public static final SoundEvent PALE_FOREST_FOG_PHRASE_02 = register("pale_forest_fog_phrase_02");
	public static final SoundEvent PALE_FOREST_FOG_PHRASE_03 = register("pale_forest_fog_phrase_03");
	public static final SoundEvent PALE_FOREST_BARK = register("pale_forest_bark");
	public static final SoundEvent PALE_FOREST_BRANCH = register("pale_forest_branch");
	public static final SoundEvent PALE_FOREST_TWIG = register("pale_forest_twig");
	public static final SoundEvent PALE_FOREST_RUSTLE = register("pale_forest_rustle");

	public static final SoundEvent SOUL_SHIELD_SWING = register("soul_shield_swing");


	/** D-076 — independent deer palette, fixed delivery range. */
	public static final SoundEvent PALE_DEER_VOICE = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("pale_deer_voice"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_deer_voice"), 32.0f));
	public static final SoundEvent PALE_DEER_SNORT = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("pale_deer_snort"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_deer_snort"), 20.0f));
	public static final SoundEvent PALE_DEER_STEP = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("pale_deer_step"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_deer_step"), 18.0f));
	public static final SoundEvent PALE_DEER_EAT = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("pale_deer_eat"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_deer_eat"), 10.0f));
	public static final SoundEvent PALE_DEER_ALERT_SNORT = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("pale_deer_alert_snort"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_deer_alert_snort"), 24.0f));
	public static final SoundEvent PALE_DEER_WATCH_ROAR = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("pale_deer_watch_roar"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_deer_watch_roar"), 50.0f));
	public static final SoundEvent PALE_DEER_ROAR = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("pale_deer_roar"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_deer_roar"), 38.0f));
	public static final SoundEvent PALE_DEER_RUN_STEP = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("pale_deer_run_step"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("pale_deer_run_step"), 38.0f));

	private ModSounds() {}

	private static SoundEvent register(String name) {
		var id = TearsOfInk.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() { StriderSong.init(); }
}
