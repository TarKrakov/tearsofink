package com.tarkrakov.tearsofink.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Строки описания под предметом: ключи {@code <предмет>.desc1}…{@code .desc4} в языковом файле. */
public final class Descriptions {
	private static final int MAX_LINES = 4;
	/** Длинные строки описания переносятся по словам (символов в строке). */
	public static final int WRAP = 38;

	private Descriptions() {}

	/** Перенос по словам. */
	public static List<String> wrap(String text, int width) {
		List<String> out = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			if (line.length() > 0 && line.length() + 1 + word.length() > width) {
				out.add(line.toString());
				line.setLength(0);
			}
			if (line.length() > 0) line.append(' ');
			line.append(word);
		}
		if (line.length() > 0) out.add(line.toString());
		return out;
	}

	public static void add(String descriptionId, List<Component> tooltip) {
		for (int i = 1; i <= MAX_LINES; i++) {
			String key = descriptionId + ".desc" + i;
			if (!Language.getInstance().has(key)) break;
			for (String line : wrap(Language.getInstance().getOrDefault(key), WRAP)) {
				tooltip.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
			}
		}
	}
}
