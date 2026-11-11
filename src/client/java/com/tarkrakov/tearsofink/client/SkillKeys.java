package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.skill.ModSkills;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Клавиши навыков (переназначаются в Настройки → Управление → Tears of Ink).
 * Новый активный навык — ещё одна строка в {@link #init}.
 */
public final class SkillKeys {
	public static final String CATEGORY = "key.categories.tears-of-ink";
	private static final Map<KeyMapping, Skill> KEYS = new LinkedHashMap<>();
	/** Абилка оружия: R — правая рука, Shift+R — левая (щит). */
	private static KeyMapping WEAPON_KEY;

	private SkillKeys() {}

	public static void init() {
		bind(ModSkills.DASH, "key.tears-of-ink.dash", GLFW.GLFW_KEY_LEFT_ALT);
		WEAPON_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.tears-of-ink.weapon_ability", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(SkillKeys::tick);
	}

	private static void bind(Skill skill, String name, int defaultKey) {
		KeyMapping key = KeyBindingHelper.registerKeyBinding(new KeyMapping(name, InputConstants.Type.KEYSYM, defaultKey, CATEGORY));
		KEYS.put(key, skill);
	}

	private static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		while (WEAPON_KEY.consumeClick()) {
			if (player == null || mc.screen != null) continue;
			ClientPlayNetworking.send(new SkillPayloads.UseWeapon(player.isShiftKeyDown(),
					player.input.leftImpulse, player.input.forwardImpulse));
		}
		for (Map.Entry<KeyMapping, Skill> e : KEYS.entrySet()) {
			while (e.getKey().consumeClick()) {
				if (player == null || mc.screen != null) continue;
				ClientPlayNetworking.send(new SkillPayloads.Use(e.getValue().id(),
						player.input.leftImpulse, player.input.forwardImpulse, player.isShiftKeyDown()));
			}
		}
	}
}
