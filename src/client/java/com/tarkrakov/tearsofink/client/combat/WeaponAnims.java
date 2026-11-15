package com.tarkrakov.tearsofink.client.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Движения клинков от первого лица — из assets/tears-of-ink/weapon_anims/<архетип>.json (перечитываются по F3+T).
 * Каждое движение — ключевые позы во времени; между ними — плавный переход (ease).
 * relative (по умолчанию): поза — сдвиг и поворот от обычной стойки (Display), в пикселях и градусах.
 * absolute: поза — полные числа Display из Blockbench (rotation / translation / scale), можно переписывать прямо оттуда.
 */
public final class WeaponAnims {
	public enum Ease { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, BACK_OUT, HEAVY }

	public record Key(float t, Vector3f rot, Vector3f pos, Vector3f scale, Ease ease) {}

	/**
	 * hold — сколько секунд клинок стоит в последней позе (следующий удар начнётся прямо из неё),
	 * back — за сколько потом возвращается в стойку. Без hold движение идёт к стойке к моменту length.
	 */
	public record Clip(float length, boolean absolute, List<Key> keys, float hold, float back, float impact, ThirdPerson.Track third) {}

	private static final Map<String, Map<String, Clip>> CLIPS = new HashMap<>();

	private static final class Playing {
		final Clip clip;
		final Archetype archetype;
		final long start;
		final float busy; // сколько секунд клинок занят (по серверу, реальное время)
		final float tempo; // общая скорость архетипа: движение идёт быстрее/медленнее

		Playing(Clip clip, Archetype archetype, int busyTicks, float tempo) {
			this.clip = clip;
			this.archetype = archetype;
			this.start = System.nanoTime();
			this.busy = busyTicks / 20f;
			this.tempo = Math.max(0.1f, tempo);
		}

		float real() {
			return (System.nanoTime() - start) / 1e9f;
		}

		float end() {
			if (clip.hold() <= 0) return clip.length();
			return lastT(clip) + clip.hold() + clip.back();
		}

		/** Время внутри движения (с учётом общей скорости). */
		float time() {
			return real() * tempo;
		}
	}

	private static final Playing[] ARMS = new Playing[2];

	private WeaponAnims() {}

	public static void init() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return TearsOfInk.id("weapon_anims");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				CLIPS.clear();
				GRIP.clear();
				for (Archetype a : Archetype.values()) {
					var res = manager.getResource(TearsOfInk.id("weapon_anims/" + a.id + ".json"));
					if (res.isEmpty()) continue;
					try (Reader r = res.get().openAsReader()) {
						JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
						CLIPS.put(a.id, parse(root));
						if (root.has("grip")) GRIP.put(a.id, vec(root, "grip", new Vector3f(8, 8, 8)));
					} catch (Exception e) {
						TearsOfInk.LOGGER.error("Движения {} не прочитались: {}", a.id, e.toString());
					}
				}
			}
		});
	}

	/**
	 * Рукоять архетипа (поле "grip" в файле движений): точка модели в пикселях Blockbench, где рука держит оружие.
	 * Если задана — движения крутят клинок вокруг рукояти (как кистью). Нет — вокруг точки крепления в руке (Палачи,
	 * Скимитары, Дуссак настроены так).
	 */
	private static final Map<String, Vector3f> GRIP = new HashMap<>();

	private static Map<String, Clip> parse(JsonObject root) {
		Map<String, Clip> out = new HashMap<>();
		JsonObject anims = root.getAsJsonObject("animations");
		for (var e : anims.entrySet()) {
			JsonObject o = e.getValue().getAsJsonObject();
			boolean absolute = o.has("absolute") && o.get("absolute").getAsBoolean();
			Vector3f zero = new Vector3f(), one = new Vector3f(1, 1, 1);
			List<Key> keys = new ArrayList<>();
			for (JsonElement el : o.getAsJsonArray("keyframes")) {
				JsonObject k = el.getAsJsonObject();
				keys.add(new Key(k.get("t").getAsFloat(), vec(k, "rotation", zero), vec(k, "translation", zero), vec(k, "scale", one),
						k.has("ease") ? Ease.valueOf(k.get("ease").getAsString().toUpperCase()) : Ease.EASE_IN_OUT));
			}
			keys.sort((a, b) -> Float.compare(a.t(), b.t()));
			out.put(e.getKey(), new Clip(o.get("length").getAsFloat(), absolute, keys,
					o.has("hold") ? o.get("hold").getAsFloat() : 0f, o.has("return") ? o.get("return").getAsFloat() : 0.3f,
					o.has("impact") ? o.get("impact").getAsFloat() : -1f,
					o.has("third") ? ThirdPerson.parse(o.getAsJsonObject("third")) : null));
		}
		return out;
	}

	private static Vector3f vec(JsonObject o, String key, Vector3f def) {
		if (!o.has(key)) return new Vector3f(def);
		JsonArray a = o.getAsJsonArray(key);
		return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
	}

	public static Clip clip(Archetype a, String anim) {
		Map<String, Clip> m = CLIPS.get(a.id);
		return m == null ? null : m.get(anim);
	}

	public static boolean has(Archetype a, String anim) {
		Map<String, Clip> m = CLIPS.get(a.id);
		return m != null && m.containsKey(anim);
	}

	/** Начать движение рукой arm. */
	public static void play(HumanoidArm arm, Archetype a, String anim, int busyTicks, float tempo) {
		Map<String, Clip> m = CLIPS.get(a.id);
		Clip clip = m == null ? null : m.get(anim);
		ARMS[arm.ordinal()] = clip == null ? null : new Playing(clip, a, busyTicks, tempo);
	}

	private static float lastT(Clip c) {
		return c.keys().isEmpty() ? 0 : c.keys().get(c.keys().size() - 1).t();
	}

	public static void stop(HumanoidArm arm) {
		ARMS[arm.ordinal()] = null;
	}

	/** Занят ли клинок ударом (по времени сервера; задержка в позе hold — уже не занят). */
	public static boolean busy(HumanoidArm arm) {
		Playing p = ARMS[arm.ordinal()];
		return p != null && p.real() < p.busy;
	}

	/**
	 * Вместо обычной стойки — поза движения. Возвращает false, если движения нет (рисуется как обычно).
	 */
	public static boolean apply(ItemDisplayContext ctx, ItemStack stack, ItemTransform rest, boolean left, PoseStack pose) {
		HumanoidArm arm;
		if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) arm = HumanoidArm.RIGHT;
		else if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) arm = HumanoidArm.LEFT;
		else return false;
		Playing p = ARMS[arm.ordinal()];
		if (p == null) return false;
		float t = p.time();
		if (t >= p.end() || Archetype.of(stack) != p.archetype) {
			ARMS[arm.ordinal()] = null;
			return false;
		}
		Clip c = p.clip;
		// стойка = обычная поза Display (в относительных — нули)
		Key restKey = c.absolute()
				? new Key(0, new Vector3f(rest.rotation), new Vector3f(rest.translation).mul(16), new Vector3f(rest.scale), Ease.EASE_IN_OUT)
				: new Key(0, new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1), Ease.EASE_IN_OUT);
		List<Key> keys = c.keys();
		if (c.hold() > 0 && !keys.isEmpty() && t >= lastT(c)) {
			// замер в последней позе, потом мягкий возврат в стойку
			Key last = keys.get(keys.size() - 1);
			float since = t - lastT(c) - c.hold();
			Key to = since <= 0 ? last : restKey;
			float u0 = since <= 0 ? 0 : ease(Ease.EASE_IN_OUT, Mth.clamp(since / Math.max(0.01f, c.back()), 0, 1));
			put(c, GRIP.get(p.archetype.id), rest, left, pose, lerp(last.rot(), to.rot(), u0), lerp(last.pos(), to.pos(), u0), lerp(last.scale(), to.scale(), u0));
			return true;
		}
		Key a = restKey, b = null;
		float ta = 0, tb = c.length();
		for (Key k : keys) {
			if (k.t() <= t) {
				a = k;
				ta = k.t();
			} else {
				b = k;
				tb = k.t();
				break;
			}
		}
		if (b == null) b = new Key(c.length(), restKey.rot(), restKey.pos(), restKey.scale(), Ease.EASE_IN_OUT);
		float u = tb > ta ? Mth.clamp((t - ta) / (tb - ta), 0, 1) : 1;
		u = ease(a.ease(), u);
		put(c, GRIP.get(p.archetype.id), rest, left, pose, lerp(a.rot(), b.rot(), u), lerp(a.pos(), b.pos(), u), lerp(a.scale(), b.scale(), u));
		return true;
	}

	/** Где рукоять оказывается в руке при обычной стойке (Display): та же цепочка, что у ItemTransform + сдвиг модели на −0,5. */
	private static Vector3f gripPoint(Vector3f grip, ItemTransform rest, float m) {
		Vector3f v = new Vector3f(grip).div(16f).sub(0.5f, 0.5f, 0.5f).mul(rest.scale);
		new Quaternionf().rotationXYZ(rest.rotation.x * Mth.DEG_TO_RAD, m * rest.rotation.y * Mth.DEG_TO_RAD,
				m * rest.rotation.z * Mth.DEG_TO_RAD).transform(v);
		return v.add(m * rest.translation.x, rest.translation.y, rest.translation.z);
	}

	private static void put(Clip c, Vector3f grip, ItemTransform rest, boolean left, PoseStack pose, Vector3f rot, Vector3f pos, Vector3f scl) {
		if (c.absolute()) {
			new ItemTransform(rot, pos.mul(0.0625f), scl).apply(left, pose);
		} else {
			float m = left ? -1 : 1;
			pose.translate(m * pos.x / 16f, pos.y / 16f, pos.z / 16f);
			Vector3f g = grip == null ? null : gripPoint(grip, rest, m);
			if (g != null) pose.translate(g.x, g.y, g.z); // крутить вокруг рукояти
			pose.mulPose(new Quaternionf().rotationXYZ(rot.x * Mth.DEG_TO_RAD, m * rot.y * Mth.DEG_TO_RAD, m * rot.z * Mth.DEG_TO_RAD));
			if (g != null) pose.translate(-g.x, -g.y, -g.z);
			pose.scale(scl.x, scl.y, scl.z);
			rest.apply(left, pose);
		}
	}

	static Vector3f lerp(Vector3f a, Vector3f b, float u) {
		return new Vector3f(a).lerp(b, u);
	}

	static float ease(Ease e, float u) {
		return switch (e) {
			case LINEAR -> u;
			case EASE_IN -> u * u * u;
			case EASE_OUT -> 1 - (1 - u) * (1 - u) * (1 - u);
			case EASE_IN_OUT -> u < 0.5f ? 4 * u * u * u : 1 - (float) Math.pow(-2 * u + 2, 3) / 2;
			case BACK_OUT -> {
				float c1 = 1.70158f, c3 = c1 + 1;
				yield 1 + c3 * (float) Math.pow(u - 1, 3) + c1 * (float) Math.pow(u - 1, 2);
			}
			// тяжёлое: долго набирает ход, потом резко доходит
			case HEAVY -> (float) Math.pow(u, 4);
		};
	}
}
