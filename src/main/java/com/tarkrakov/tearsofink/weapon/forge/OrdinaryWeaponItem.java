package com.tarkrakov.tearsofink.weapon.forge;

import com.tarkrakov.tearsofink.hunt.HunterType;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.weapon.InkGreatswordItem;

/**
 * Обычное оружие линии (крафт на верстаке из слитков и палок): лёгкий одноручный и тяжёлый двуручный меч.
 * Удары — архетипа (лёгкий — Скимитары, тяжёлый — Палачи), без приёма на R и без парного ПКМ.
 * Пассивки: Кровь — шанс кровотечения, Шпиль — быстрее, Чернила — замедляют (Gear).
 */
public class OrdinaryWeaponItem extends InkGreatswordItem {
	private final HunterType line;

	/** damage и speed — итоговые, как в игре (урон 5, скорость 1,5). */
	public OrdinaryWeaponItem(Properties properties, boolean twoHanded, HunterType line, int damage, float speed) {
		super(() -> null, properties, twoHanded, damage - 3, speed - 4f);
		this.line = line;
	}

	public HunterType line() {
		return line;
	}

	@Override
	public Skill ability() {
		return null;
	}
}
