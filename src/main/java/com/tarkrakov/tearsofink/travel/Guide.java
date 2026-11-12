package com.tarkrakov.tearsofink.travel;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

/** Голос Г-на Проводника в чате: свой цвет и курсив, чтобы его узнавали. */
public final class Guide {
	/** Цвет реплик Г-на Проводника (приглушённый багровый). */
	public static final int COLOR = 0xB5475A;

	private Guide() {}

	public static Style style() {
		return Style.EMPTY.withColor(TextColor.fromRgb(COLOR)).withItalic(true);
	}

	/** Реплика по ключу из ru_ru.json: guide.tears-of-ink.&lt;key&gt;. */
	public static void say(ServerPlayer player, String key, Object... args) {
		player.sendSystemMessage(Component.translatable("guide.tears-of-ink." + key, args).withStyle(style()));
	}

	/** Случайная реплика из набора key.1 … key.count. */
	public static void sayRandom(ServerPlayer player, String key, int count) {
		say(player, key + "." + (1 + player.getRandom().nextInt(count)));
	}
}
