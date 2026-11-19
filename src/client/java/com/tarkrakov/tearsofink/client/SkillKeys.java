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

	public static net.minecraft.network.chat.Component weaponKeyLabel() {
		return WEAPON_KEY == null ? net.minecraft.network.chat.Component.literal("R") : WEAPON_KEY.getTranslatedKeyMessage();
	}

	public static void init() {
		bind(ModSkills.DASH, "key.tears-of-ink.dash", GLFW.GLFW_KEY_LEFT_ALT);
		WEAPON_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.tears-of-ink.weapon_ability", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(SkillKeys::tick);
	}

	private static void bind(Skill skill, String name, int defaultKey) {
		KeyMapping key = KeyBindingHelper.registerKeyBinding(new KeyMapping(name, InputConstants.Type.KEYSYM, defaultKey, CATEGORY));
		KEYS.put(key, skill);
	}

	// прыжки путей: Чернильный прыжок (второй в воздухе) и Вознесение духа (Shift+пробел с земли)
	private static boolean jumpWas, airUsed, groundWas;

	private static void jumps(Minecraft mc, LocalPlayer player) {
		boolean jump = mc.options.keyJump.isDown();
		boolean ground = player.onGround() || player.isInWater() || player.onClimbable() || player.getAbilities().flying || player.isPassenger();
		boolean lands = mc.level != null && mc.level.dimension() == com.tarkrakov.tearsofink.world.ModDimensions.INK_LANDS;
		com.tarkrakov.tearsofink.vow.Vow vow = com.tarkrakov.tearsofink.vow.Vow.CLIENT;
		// Путь Души: Shift+пробел с земли — Вознесение (игра уже подбросила в этом же тике — сервер видит отрыв)
		if (lands && mc.screen == null && vow == com.tarkrakov.tearsofink.vow.Vow.SOUL && jump && !jumpWas && groundWas && !ground
				&& mc.options.keyShift.isDown()) {
			ClientPlayNetworking.send(new SkillPayloads.Use(com.tarkrakov.tearsofink.skill.ModSkills.ASCENSION.id(), 0, 0, false));
		}
		if (ground) {
			airUsed = false;
		} else if (lands && mc.screen == null) {
			if (vow == com.tarkrakov.tearsofink.vow.Vow.INK && jump && !jumpWas && !airUsed && !player.isFallFlying()) {
				airUsed = true;
				ClientPlayNetworking.send(new SkillPayloads.Use(com.tarkrakov.tearsofink.skill.ModSkills.INK_JUMP.id(), 0, 0, false));
			}
		}
		jumpWas = jump;
		groundWas = ground;
	}

	private static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player != null) jumps(mc, player);
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
