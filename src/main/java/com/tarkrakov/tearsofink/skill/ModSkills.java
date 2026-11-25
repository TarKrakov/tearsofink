package com.tarkrakov.tearsofink.skill;

import com.tarkrakov.tearsofink.TearsOfInk;

/** Все навыки мода. */
public final class ModSkills {
	/** Рывок: бесплатный, базовая перезарядка 18 тиков (0,9 с). */
	public static final DashSkill DASH = Skills.register(new DashSkill(TearsOfInk.id("dash"), com.tarkrakov.tearsofink.ink.InkTuning.DASH_COST, 0, com.tarkrakov.tearsofink.ink.InkTuning.DASH_COOLDOWN));

	/** Могучая поступь (белая книга, Путь Души; зеркало Рывка, та же клавиша): бесплатно, 30 тиков КД. */
	public static final MightyStrideSkill STRIDE = Skills.register(new MightyStrideSkill(TearsOfInk.id("mighty_stride"), 0, 0, 30));
	/** Чернильный прыжок (чёрная книга): второй прыжок в воздухе, 5 Чернил. */
	public static final InkJumpSkill INK_JUMP = Skills.register(new InkJumpSkill(TearsOfInk.id("ink_jump"), 5, 0, 8));
	/** Вознесение духа (белая книга): зажать прыжок — дух поднимает выше, 5 Души. */
	public static final SpiritAscensionSkill ASCENSION = Skills.register(new SpiritAscensionSkill(TearsOfInk.id("spirit_ascension"), 0, 5, 20));

	private ModSkills() {}

	static void init() {
		DASH.init();
		STRIDE.init();
		INK_JUMP.init();
		ASCENSION.init();
	}
}
