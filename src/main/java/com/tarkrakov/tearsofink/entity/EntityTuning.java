package com.tarkrakov.tearsofink.entity;

/** Настройки фауны — крутить здесь. */
public final class EntityTuning {
	/** Как часто гусеница ищет, что поесть (тиков; 20 = 1 с): от MIN до MIN+SPREAD — 3–5 минут. */
	public static final int EAT_COOLDOWN_MIN = 3 * 60 * 20, EAT_COOLDOWN_SPREAD = 2 * 60 * 20;
	/** В каком радиусе ищет растение. */
	public static final int EAT_SEARCH_RADIUS = 8;
	/** Чернильная гусеница на ходу оставляет высыхающую лужицу примерно раз в столько тиков. */
	public static final int INK_PUDDLE_CHANCE = 600;
	/** Появление в мире: вес и размер группы (бледные — чаще всех, кровавые — реже). */
	public static final int PALE_WEIGHT = 14, INK_WEIGHT = 10, BLOOD_WEIGHT = 10;

	/**
	 * Кровавые гусеницы расселяются при создании мира: центр каждого пятна Кроволесья — всегда
	 * группа из BLOOD_CENTRE_GROUP; каждый другой чанк дна — с шансом BLOOD_CHUNK_CHANCE % группа 1..BLOOD_CHUNK_GROUP_MAX.
	 * В среднем выходит ~12–25 гусениц на пятно (зависит от размера пятна).
	 */
	public static final int BLOOD_CENTRE_GROUP = 4, BLOOD_CHUNK_CHANCE = 14, BLOOD_CHUNK_GROUP_MAX = 3;

	private EntityTuning() {}
}
