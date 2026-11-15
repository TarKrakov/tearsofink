package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.hunt.HunterType;
import com.tarkrakov.tearsofink.skill.Skill;

import java.util.function.Supplier;

/**
 * Оружие из перечня автора (Секира, Кинжал, Катана, Язык Дракона, Тальвар, Дуссак…): 3D в руке, рисунок в инвентаре.
 * Линия (Шпиль / Кровь / Чернила) — кто его носит и чем платят приёмы; архетип — в Archetype; приём на R — ability.
 * Удары оружия Крови с шансом вешают кровотечение.
 */
public class ArsenalWeaponItem extends InkGreatswordItem {
	private final HunterType line;

	public ArsenalWeaponItem(Properties properties, boolean twoHanded, HunterType line, Supplier<Skill> ability) {
		super(ability, properties, twoHanded);
		this.line = line;
	}

	public ArsenalWeaponItem(Properties properties, boolean twoHanded, HunterType line) {
		this(properties, twoHanded, line, () -> null);
	}

	public HunterType line() {
		return line;
	}

	public boolean blood() {
		return line == HunterType.BLOOD;
	}
}
