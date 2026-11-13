package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.weapon.AbilityWeapon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * Панелька характеристик оружия мода рядом с обычной подсказкой: урон, скорость атаки, стоимость и перезарядка абилки.
 * Подписи — stats.tears-of-ink.* в ru_ru.json.
 */
public final class WeaponStats {
	private WeaponStats() {}

	public static List<Component> lines(ItemStack stack) {
		List<Component> out = new ArrayList<>();
		if (!(stack.getItem() instanceof AbilityWeapon weapon)) return out;
		ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		double damage = 0, speed = 0;
		boolean melee = false;
		for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
			if (e.attribute().is(Attributes.ATTACK_DAMAGE) && e.modifier().id().equals(Item.BASE_ATTACK_DAMAGE_ID)) { damage = e.modifier().amount(); melee = true; }
			if (e.attribute().is(Attributes.ATTACK_SPEED) && e.modifier().id().equals(Item.BASE_ATTACK_SPEED_ID)) speed = e.modifier().amount();
		}
		out.add(Component.translatable("stats.tears-of-ink.title").withStyle(ChatFormatting.GRAY));
		if (melee) {
			out.add(Component.translatable("stats.tears-of-ink.damage", fmt(1 + damage)).withStyle(ChatFormatting.WHITE));
			out.add(Component.translatable("stats.tears-of-ink.speed", fmt(4 + speed)).withStyle(ChatFormatting.WHITE));
		}
		Skill skill = weapon.ability();
		if (skill.inkCost() > 0) out.add(Component.translatable("stats.tears-of-ink.cost_ink", skill.inkCost()).withStyle(ChatFormatting.DARK_PURPLE));
		if (skill.soulCost() > 0) out.add(Component.translatable("stats.tears-of-ink.cost_soul", skill.soulCost()).withStyle(ChatFormatting.AQUA));
		if (skill.cooldownTicks() > 0) out.add(Component.translatable("stats.tears-of-ink.cooldown", fmt(skill.cooldownTicks() / 20.0)).withStyle(ChatFormatting.GRAY));
		return out;
	}

	private static String fmt(double v) {
		return Math.abs(v - Math.rint(v)) < 0.05 ? String.valueOf((long) Math.rint(v)) : String.format(java.util.Locale.ROOT, "%.1f", v);
	}

	/** Рисует панельку справа от обычной подсказки (или слева, если справа не влезает). */
	public static void render(GuiGraphics g, ItemStack stack, int mouseX, int mouseY) {
		List<Component> stats = lines(stack);
		if (stats.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int mainWidth = 0;
		for (Component c : Screen.getTooltipFromItem(mc, stack)) mainWidth = Math.max(mainWidth, font.width(c));
		int statsWidth = 0;
		for (Component c : stats) statsWidth = Math.max(statsWidth, font.width(c));
		int boxX = mouseX + 12 + mainWidth + 10;               // где начнётся панелька (как у подсказки: +12 от мыши)
		if (boxX + statsWidth + 6 > g.guiWidth()) boxX = mouseX + 12 - statsWidth - 14; // справа не влезает — слева
		g.renderComponentTooltip(font, stats, boxX - 12, mouseY);
	}
}
