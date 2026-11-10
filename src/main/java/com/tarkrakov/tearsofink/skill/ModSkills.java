package com.tarkrakov.tearsofink.skill;

import com.tarkrakov.tearsofink.TearsOfInk;

/** Все навыки мода. */
public final class ModSkills {
	/** Рывок: 5 Чернил, перезарядка 25 тиков (1.25 с). */
	public static final DashSkill DASH = Skills.register(new DashSkill(TearsOfInk.id("dash"), 5, 0, 25));

	private ModSkills() {}

	static void init() {
		DASH.init();
	}
}
