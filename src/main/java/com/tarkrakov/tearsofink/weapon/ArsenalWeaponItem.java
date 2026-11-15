package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.skill.Skill;

/**
 * Оружие из перечня автора (Секира, Кинжал, Катана, Язык Дракона, Тальвар, Дуссак…): 3D в руке, рисунок в инвентаре.
 * Пока без своего архетипа и приёмов — подключаются по мере готовности движений.
 */
public class ArsenalWeaponItem extends InkGreatswordItem {
	public ArsenalWeaponItem(Properties properties, boolean twoHanded) {
		super(() -> null, properties, twoHanded);
	}

	@Override
	public Skill ability() {
		return null;
	}
}
