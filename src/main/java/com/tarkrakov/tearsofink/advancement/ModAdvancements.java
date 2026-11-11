package com.tarkrakov.tearsofink.advancement;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Достижения мода (вкладка «Tears of Ink»). Сами достижения — JSON в {@code data/tears-of-ink/advancement/},
 * тексты — в языковом файле ({@code advancements.tears-of-ink.<имя>.title / .description}).
 * Выдаются кодом в нужный момент.
 */
public final class ModAdvancements {
	/** Вошёл в Чернильные Земли. */
	public static final String INK_LANDS = "ink_lands";
	/** Зажёг первый фонарь. */
	public static final String FIRST_FLAME = "first_flame";
	/** Испил из фонтана. */
	public static final String CURSED_VESSEL = "cursed_vessel";
	/** Продал часть души. */
	public static final String CURSE_ESSENCE = "curse_essence";
	/** Очистил часть души. */
	public static final String ATONEMENT = "atonement";

	private ModAdvancements() {}

	public static void grant(ServerPlayer player, String name) {
		AdvancementHolder holder = player.server.getAdvancements().get(TearsOfInk.id(name));
		if (holder == null) return;
		if (player.getAdvancements().getOrStartProgress(holder).isDone()) return;
		player.getAdvancements().award(holder, "done");
	}
}
