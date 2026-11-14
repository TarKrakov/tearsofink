package com.tarkrakov.tearsofink.hunt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Цели Охоты: data/tears-of-ink/hunt_targets/*.json — {"entity": "minecraft:zombie", "weight": 10, "power": 1.25}.
 * power — своя сила цели (здоровье и урон), чтобы все цели были примерно равны по трудности.
 * Новая цель — новый файл (правка → /reload). Как цель называется в объявлении («выслеживает кого») —
 * ключ hunt.tears-of-ink.prey.<мод>.<имя> в ru_ru.json (нет ключа — просто название существа).
 */
public final class HuntTargets {
	/** power — своя сила цели: множитель и здоровья, и урона (1 — как задано рангом). */
	public record Target(EntityType<?> type, int weight, float power) {}

	private static final List<Target> TARGETS = new ArrayList<>();

	private HuntTargets() {}

	public static List<Target> all() {
		return List.copyOf(TARGETS);
	}

	@Nullable
	public static Target pick(RandomSource random) {
		int total = 0;
		for (Target t : TARGETS) total += t.weight();
		if (total <= 0) return null;
		int r = random.nextInt(total);
		for (Target t : TARGETS) {
			r -= t.weight();
			if (r < 0) return t;
		}
		return null;
	}

	public static void init() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return TearsOfInk.id("hunt_targets");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				TARGETS.clear();
				manager.listResources("hunt_targets", id -> id.getPath().endsWith(".json")).forEach((id, res) -> {
					try (Reader r = res.openAsReader()) {
						JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
						ResourceLocation entity = ResourceLocation.parse(o.get("entity").getAsString());
						var type = BuiltInRegistries.ENTITY_TYPE.getOptional(entity);
						if (type.isEmpty()) {
							TearsOfInk.LOGGER.warn("Цель Охоты {}: нет существа {}", id, entity);
							return;
						}
						int weight = o.has("weight") ? o.get("weight").getAsInt() : 10;
						float power = o.has("power") ? o.get("power").getAsFloat() : 1f;
						if (weight > 0) TARGETS.add(new Target(type.get(), weight, power));
					} catch (Exception e) {
						TearsOfInk.LOGGER.error("Цель Охоты {} не прочиталась: {}", id, e.toString());
					}
				});
			}
		});
	}
}
