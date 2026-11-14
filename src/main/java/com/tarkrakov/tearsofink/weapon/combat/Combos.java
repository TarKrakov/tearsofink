package com.tarkrakov.tearsofink.weapon.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Связки ударов по архетипам — из data/tears-of-ink/weapon_combos/<архетип>.json (перечитываются по /reload).
 * Время — в тиках (20 тиков = 1 секунда).
 */
public final class Combos {
	/** Один удар связки. */
	public record Step(String anim, int duration, int hit, float damage, float reach, float arc, float knockback) {}

	/** Связка архетипа. */
	public record Combo(List<Step> steps, int resetTicks, float minStrength, String abilityAnim, int abilityTicks) {}

	private static final Map<Archetype, Combo> COMBOS = new EnumMap<>(Archetype.class);

	private Combos() {}

	public static Combo get(Archetype a) {
		Combo c = COMBOS.get(a);
		return c != null ? c : fallback();
	}

	private static Combo fallback() {
		return new Combo(List.of(new Step("attack1", 12, 5, 1f, 3f, 100f, 0.4f)), 30, 0.9f, "ability", 20);
	}

	public static void init() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return TearsOfInk.id("weapon_combos");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				COMBOS.clear();
				for (Archetype a : Archetype.values()) {
					var res = manager.getResource(TearsOfInk.id("weapon_combos/" + a.id + ".json"));
					if (res.isEmpty()) continue;
					try (Reader r = res.get().openAsReader()) {
						COMBOS.put(a, parse(JsonParser.parseReader(r).getAsJsonObject()));
					} catch (Exception e) {
						TearsOfInk.LOGGER.error("Связка {} не прочиталась: {}", a.id, e.toString());
					}
				}
			}
		});
	}

	private static Combo parse(JsonObject o) {
		List<Step> steps = new ArrayList<>();
		JsonArray arr = o.getAsJsonArray("steps");
		for (JsonElement el : arr) {
			JsonObject s = el.getAsJsonObject();
			steps.add(new Step(s.get("anim").getAsString(), s.get("duration").getAsInt(), s.get("hit").getAsInt(),
					s.get("damage").getAsFloat(), s.get("reach").getAsFloat(), s.get("arc").getAsFloat(),
					s.has("knockback") ? s.get("knockback").getAsFloat() : 0.4f));
		}
		return new Combo(steps, o.has("reset_ticks") ? o.get("reset_ticks").getAsInt() : 30,
				o.has("min_strength") ? o.get("min_strength").getAsFloat() : 0.9f,
				o.has("ability_anim") ? o.get("ability_anim").getAsString() : "ability",
				o.has("ability_ticks") ? o.get("ability_ticks").getAsInt() : 20);
	}
}
