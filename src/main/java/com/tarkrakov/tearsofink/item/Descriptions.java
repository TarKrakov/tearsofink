package com.tarkrakov.tearsofink.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Строки описания под предметом: ключи {@code <предмет>.desc1}…{@code .desc4} в языковом файле. */
public final class Descriptions {
	private static final int MAX_LINES = 4;

	private Descriptions() {}

	public static void add(String descriptionId, List<Component> tooltip) {
		for (int i = 1; i <= MAX_LINES; i++) {
			String key = descriptionId + ".desc" + i;
			if (!Language.getInstance().has(key)) break;
			tooltip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		}
	}
}
