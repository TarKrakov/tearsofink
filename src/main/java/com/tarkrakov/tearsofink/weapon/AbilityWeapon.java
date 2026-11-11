package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.skill.Skill;

/** Оружие с абилкой: R — в правой руке, Shift+R — в левой (щит). */
public interface AbilityWeapon {
	Skill ability();
}
