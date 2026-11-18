package com.tarkrakov.tearsofink.client.prologue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-only index; never changes server diary IDs or unlock rules. */
enum DiarySection {
	CHRONICLES("chronicles", "intro", "draft_memory", "draft_ruins"),
	WORLD("world", "first_steps", "draft_fog", "fountain_hunt", "draft_fountain_travel", "draft_region_pale", "draft_region_blood", "draft_region_ink", "draft_region_lake", "draft_region_edge", "draft_adit"),
	BESTIARY("bestiary", "caterpillar", "caterpillar_pale", "caterpillar_blood", "caterpillar_ink", "pale_spearman", "pale_shieldbearer", "pale_crossbowman", "zombie", "skeleton", "spider", "draft_pale_owl", "draft_pale_strider", "draft_hunter_spire", "draft_hunter_blood", "draft_hunter_ink"),
	CRAFT("craft", "pale_metal", "ink_anvil", "cursed_altar", "ascension_font", "draft_asphoda", "draft_blood_metal", "draft_ink_metal", "draft_armor", "draft_alchemy", "draft_blood_collector"),
	TRIALS("trials", "draft_hunt_field", "draft_mad_order", "draft_harvest"),
	VOWS("vows", "draft_soul", "draft_ink", "draft_soul_bargain", "draft_soul_deeds", "draft_ink_ranks");

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

	/** Unknown future entries remain readable. */
	static DiarySection of(String id) {
		return INDEX.getOrDefault(id, CHRONICLES);
	}
}
