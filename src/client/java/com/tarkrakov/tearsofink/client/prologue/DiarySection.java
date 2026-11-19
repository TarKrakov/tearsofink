package com.tarkrakov.tearsofink.client.prologue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-only index; never changes server diary IDs or unlock rules. */
enum DiarySection {
	CHRONICLES("chronicles", "intro", "draft_ruins", "draft_memory"),
	WORLD("world", "first_steps", "draft_fog", "draft_region_pale", "fountain_hunt", "draft_fountain_travel", "draft_adit", "draft_region_blood", "draft_region_ink", "draft_region_lake", "draft_region_edge"),
	BESTIARY("bestiary", "caterpillar", "caterpillar_pale", "draft_pale_owl", "draft_pale_deer", "draft_pale_strider", "zombie", "skeleton", "spider", "pale_spearman", "pale_shieldbearer", "pale_crossbowman", "draft_hunter_spire", "draft_hunter_blood", "draft_hunter_ink", "caterpillar_blood", "caterpillar_ink"),
	CRAFT("craft", "pale_metal", "draft_asphoda", "ink_anvil", "draft_weaponcraft", "draft_armor", "draft_alchemy", "draft_blood_metal", "draft_blood_collector", "draft_ink_metal", "cursed_altar", "ascension_font"),
	TRIALS("trials", "draft_hunt_field", "draft_mad_order", "draft_harvest"),
	VOWS("vows", "draft_soul", "draft_soul_deeds", "draft_ink", "draft_ink_ranks", "draft_soul_bargain"),
	FATE("fate");

	private static final Map<String, DiarySection> INDEX = new HashMap<>();
	static {
		for (DiarySection section : values()) {
			for (String entry : section.entries) {
				if (INDEX.put(entry, section) != null) throw new IllegalStateException("Duplicate diary entry: " + entry);
			}
		}
	}

	final String id;
	private final List<String> entries;

	DiarySection(String id, String... entries) {
		this.id = id;
		this.entries = List.of(entries);
	}

	List<String> entries() { return entries; }

	/** Unknown future entries remain readable. */
	static DiarySection of(String id) {
		return INDEX.getOrDefault(id, CHRONICLES);
	}
}
