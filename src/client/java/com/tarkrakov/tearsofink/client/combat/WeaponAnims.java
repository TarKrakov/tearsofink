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
	public record Clip(float length, boolean absolute, List<Key> keys, float hold, float back, float impact, boolean smooth, boolean tight, ThirdFix third, boolean shortestRotation) {}

	/**
	 * Поправки третьего лица для одного движения (блок "third" внутри движения):
	 * wrist_hold — клинок в руке не крутится: поворот кисти берётся из позы на этой секунде движения (0 — с начала) и держится,
	 * клинок просто следует за рукой; mirror — левая рука повторяет правую зеркально (парные удары синхронно);
	 * center — руки ближе к середине (0.5 — вдвое); lateral — множитель движения рук влево-вправо (−1 — наоборот:
	 * сводятся в центр и расходятся в края); no_cross — руки не заходят за середину (не перекрещиваются);
	 * source — третье лицо считается по другому движению (первое лицо правят, а третье остаётся как было).
	 * arm_space: source rotations are shoulder deltas; wrist_source.x is edge roll about the blade axis.
	 * Import-only opt-ins: wrist_xyz uses full local XYZ; baked preserves the authored timeline (no extra F5 fades).
	 */
	public record ThirdFix(float wristHold, boolean mirror, float center, float lateral, boolean noCross, @org.jetbrains.annotations.Nullable String source,
						   float pushRight, float pushLeft, float turn, boolean followBlade, boolean armSpace, @org.jetbrains.annotations.Nullable String wristSource, boolean wristXYZ, boolean baked) {
		static final ThirdFix NONE = new ThirdFix(-1, false, 1, 1, false, null, 0, 0, 0, false, false, null, false, false);
	}

	/** Третье лицо архетипа (поле "third" в файле движений): как поза первого лица переводится в руку Стива. */
	public record ThirdTune(float reach, float lead, float body, float look, boolean centre, float push) {
		static final ThirdTune DEFAULT = new ThirdTune(1.3f, 0.5f, 0.3f, 0.6f, false, 0);
	}

	private static final Map<String, ThirdTune> THIRD = new HashMap<>();
	/** Camera-only composition, in model pixels; never used by ThirdPerson. */
	private static final Map<String, Vector3f> FIRST_PERSON_OFFSET = new HashMap<>();
	private static final Map<String, Vector3f> IDLE_OFFSET = new HashMap<>();

	public static ThirdTune thirdTune(Archetype a) {
		return THIRD.getOrDefault(a.id, ThirdTune.DEFAULT);
	}

	@org.jetbrains.annotations.Nullable
	public static Vector3f grip(Archetype a) {
		return GRIP.get(a.id);
	}

	/** smooth: сколько секунд новое движение перетекает из позы, где клинок реально был (без скачка между ударами). */
	private static final float BLEND = 0.12f;
	private static float blend(Playing p) {
		return p.archetype == Archetype.PALE_LINEAR_GREATSWORD ? 0.2f : BLEND;
	}

	private static final Map<String, Map<String, Clip>> CLIPS = new HashMap<>();

	private static final class Playing {
		final Clip clip;
		final Archetype archetype;
		final long start;
		final float busy; // сколько секунд клинок занят (по серверу, реальное время)
		final float tempo; // общая скорость архетипа: движение идёт быстрее/медленнее
		float fromIdle = 1; // composition captured with the displayed pose
		Key from; // smooth: поза, из которой перетекаем в начале

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
				THIRD.clear();
				FIRST_PERSON_OFFSET.clear();
				IDLE_OFFSET.clear();
				ThirdPerson.clear();
				for (Archetype a : Archetype.values()) {
					var res = manager.getResource(TearsOfInk.id("weapon_anims/" + a.id + ".json"));
					if (res.isEmpty()) continue;
					try (Reader r = res.get().openAsReader()) {
						JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
						CLIPS.put(a.id, parse(root));
						if (root.has("firstperson_offset")) FIRST_PERSON_OFFSET.put(a.id, vec(root, "firstperson_offset", new Vector3f()));
						if (root.has("firstperson_idle_offset")) IDLE_OFFSET.put(a.id, vec(root, "firstperson_idle_offset", new Vector3f()));
						if (root.has("grip")) GRIP.put(a.id, vec(root, "grip", new Vector3f(8, 8, 8)));
						if (root.has("third")) {
							JsonObject t = root.getAsJsonObject("third");
							ThirdTune d = ThirdTune.DEFAULT;
							THIRD.put(a.id, new ThirdTune(f(t, "reach", d.reach()), f(t, "lead", d.lead()), f(t, "body", d.body()), f(t, "look", d.look()),
									t.has("centre") && t.get("centre").getAsBoolean(), f(t, "push", d.push())));
						}
					} catch (Exception e) {
						TearsOfInk.LOGGER.error("Движения {} не прочитались: {}", a.id, e.toString());
					}
				}
				// Отдельная тестовая библиотека Blockbench: никогда не заменяет принятые клипы.
				var pilot = manager.getResource(TearsOfInk.id("weapon_anims/pale_linear_greatsword_bb_test.json"));
				if (pilot.isPresent()) {
					try (Reader r = pilot.get().openAsReader()) {
						Map<String, Clip> active = CLIPS.get(Archetype.PALE_LINEAR_GREATSWORD.id);
						if (active != null) {
							Map<String, Clip> test = parse(JsonParser.parseReader(r).getAsJsonObject());
							for (String name : test.keySet()) {
								if (!name.startsWith("bb_test_") || active.containsKey(name)) {
									throw new IllegalArgumentException("Invalid/duplicate Blockbench test clip: " + name);
								}
							}
							active.putAll(test);
						}
					} catch (Exception e) {
						TearsOfInk.LOGGER.error("Blockbench test animations could not be loaded: {}", e.toString());
					}
				}
				// Авторские FP-фазы находятся отдельно от исторических/парных клипов.
				var authored = manager.getResource(TearsOfInk.id("weapon_anims/pale_linear_greatsword_bb_author.json"));
				if (authored.isPresent()) {
					try (Reader r = authored.get().openAsReader()) {
						Map<String, Clip> active = CLIPS.get(Archetype.PALE_LINEAR_GREATSWORD.id);
						if (active != null) {
							Map<String, Clip> imported = parse(JsonParser.parseReader(r).getAsJsonObject());
							for (String name : imported.keySet()) {
								if (!name.startsWith("bb_author_") || active.containsKey(name))
									throw new IllegalArgumentException("Invalid/duplicate authored animation: " + name);
							}
							active.putAll(imported);
						}
					} catch (Exception e) {
						TearsOfInk.LOGGER.error("Author Blockbench animations could not be loaded: {}", e.toString());
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
					o.has("smooth") && (o.get("smooth").getAsJsonPrimitive().isString() || o.get("smooth").getAsBoolean()),
					o.has("smooth") && o.get("smooth").getAsJsonPrimitive().isString() && "tight".equals(o.get("smooth").getAsString()),
					o.has("third") ? fix(o.getAsJsonObject("third")) : ThirdFix.NONE,
					"slerp".equals(o.has("rotation_mode") ? o.get("rotation_mode").getAsString() : "")));
		}
		return out;
	}

	private static ThirdFix fix(JsonObject t) {
		return new ThirdFix(f(t, "wrist_hold", -1), t.has("mirror") && t.get("mirror").getAsBoolean(), f(t, "center", 1),
				f(t, "lateral", 1), t.has("no_cross") && t.get("no_cross").getAsBoolean(),
				t.has("source") ? t.get("source").getAsString() : null,
				push(t, 0), push(t, 1), f(t, "turn", 0), t.has("follow_blade") && t.get("follow_blade").getAsBoolean(), t.has("arm_space") && t.get("arm_space").getAsBoolean(), t.has("wrist_source") ? t.get("wrist_source").getAsString() : null,
				t.has("wrist_xyz") && t.get("wrist_xyz").getAsBoolean(), t.has("baked") && t.get("baked").getAsBoolean());
	}

	/** "push": 3 — обеим рукам; "push": [правая, левая] — по-разному. */
	private static float push(JsonObject t, int i) {
		if (!t.has("push")) return 0;
		com.google.gson.JsonElement e = t.get("push");
		return e.isJsonArray() ? e.getAsJsonArray().get(i).getAsFloat() : e.getAsFloat();
	}

	private static float f(JsonObject o, String key, float def) {
		return o.has(key) ? o.get(key).getAsFloat() : def;
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
		Playing prev = ARMS[arm.ordinal()];
		Playing next = clip == null ? null : new Playing(clip, a, busyTicks, tempo);
		if (next != null && clip.smooth() && !clip.absolute() && prev != null && prev.archetype == a && !prev.clip.absolute()
				&& prev.time() < end(prev.clip)) {
			Key previous = sample(prev.clip, prev.time(), REST);
			// Preserve the pre-greatsword handoff for every existing weapon:
			// capturing an already blended pose is reserved for the new archetype.
			next.from = a == Archetype.PALE_LINEAR_GREATSWORD ? shown(prev, previous) : previous;
			next.fromIdle = idleWeight(prev);
		} else if (next != null && a == Archetype.PALE_LINEAR_GREATSWORD && clip.smooth() && !clip.absolute()) {
			next.from = REST; // вход из покоя тоже плавный, даже если первая ключевая поза уже в замахе
		}
		ARMS[arm.ordinal()] = next;
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
		// This mount also applies when no clip is playing, so idle/attack/ability
		// transitions share the same camera frame without a composition pop.
		Archetype held = Archetype.of(stack);
		Vector3f offset = held == null ? null : FIRST_PERSON_OFFSET.get(held.id);
		if (offset != null) pose.translate((left ? -offset.x : offset.x) / 16f, offset.y / 16f, offset.z / 16f);
		Playing p = ARMS[arm.ordinal()];
		if (p != null && (p.time() >= end(p.clip) || held != p.archetype)) {
			ARMS[arm.ordinal()] = null;
			p = null;
		}
		Vector3f idle = held == null ? null : IDLE_OFFSET.get(held.id);
		if (idle != null) {
			float w = p == null ? 1 : idleWeight(p);
			pose.translate((left ? -idle.x : idle.x) * w / 16f, idle.y * w / 16f, idle.z * w / 16f);
		}
		if (p == null) return false;
		float t = p.time();
		Clip c = p.clip;
		// стойка = обычная поза Display (в относительных — нули)
		Key restKey = c.absolute()
				? new Key(0, new Vector3f(rest.rotation), new Vector3f(rest.translation).mul(16), new Vector3f(rest.scale), Ease.EASE_IN_OUT)
				: new Key(0, new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1), Ease.EASE_IN_OUT);
		Key now = sample(c, t, restKey);
		now = shown(p, now);
		put(c, GRIP.get(p.archetype.id), rest, left, pose, now.rot(), now.pos(), now.scale());
		return true;
	}

	/** Fade the idle-only composition with the same entry/return as the clip. */
	private static float idleWeight(Playing p) {
		float t = p.time();
		float start = lastT(p.clip) + Math.max(0, p.clip.hold());
		float target = ease(Ease.EASE_IN_OUT, Mth.clamp((t - start) / Math.max(.01f, end(p.clip) - start), 0, 1));
		if (p.from != null && p.real() < blend(p)) {
			float u = ease(Ease.EASE_OUT, p.real() / blend(p));
			return Mth.lerp(u, p.fromIdle, target);
		}
		return target;
	}

	/** Capture the displayed pose even when an interruption happens during the previous blend. */
	private static Key shown(Playing p, Key key) {
		if (p.from == null || p.real() >= blend(p)) return key;
		float u = ease(Ease.EASE_OUT, p.real() / blend(p));
		return new Key(0, p.clip.shortestRotation() ? rotationMix(p.from.rot(), key.rot(), u) : lerp(p.from.rot(), key.rot(), u), lerp(p.from.pos(), key.pos(), u),
				lerp(p.from.scale(), key.scale(), u), Ease.LINEAR);
	}

	private static final Key REST = new Key(0, new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1), Ease.EASE_IN_OUT);

	/** Когда движение заканчивается (секунды движения, с учётом hold и возврата). */
	public static float end(Clip c) {
		if (c.hold() <= 0) return c.length();
		return lastT(c) + c.hold() + c.back();
	}

	/** Поза относительного движения в момент t (секунды движения): поворот, сдвиг, масштаб от обычной стойки. */
	public static Key sample(Clip c, float t) {
		return sample(c, t, REST);
	}

	/** Optional orientation channel for the straight-blade pilot; legacy Euler curves remain the default. */
	private static Vector3f rotationMix(Vector3f a, Vector3f b, float u) {
		Quaternionf from = new Quaternionf().rotationXYZ(a.x * Mth.DEG_TO_RAD, a.y * Mth.DEG_TO_RAD, a.z * Mth.DEG_TO_RAD);
		Quaternionf to = new Quaternionf().rotationXYZ(b.x * Mth.DEG_TO_RAD, b.y * Mth.DEG_TO_RAD, b.z * Mth.DEG_TO_RAD);
		return from.slerp(to, u).getEulerAnglesXYZ(new Vector3f()).mul(180f / Mth.PI);
	}

	private static Key sample(Clip c, float t, Key restKey) {
		if (c.third().baked() && !c.keys().isEmpty() && t >= lastT(c)) return c.keys().get(c.keys().size() - 1);
		Key result = sampleEuler(c, t, restKey);
		if (!c.shortestRotation() || c.keys().isEmpty()) return result;
		List<Key> keys = c.keys();
		Key last = keys.get(keys.size() - 1);
		Vector3f rotation;
		if (c.hold() > 0 && t >= last.t()) {
			float u = ease(Ease.EASE_IN_OUT, Mth.clamp((t - last.t() - c.hold()) / Math.max(.01f, c.back()), 0, 1));
			rotation = rotationMix(last.rot(), restKey.rot(), u);
		} else {
			Key from = restKey, to = new Key(c.length(), restKey.rot(), restKey.pos(), restKey.scale(), Ease.LINEAR);
			for (Key key : keys) {
				if (key.t() > t) { to = key; break; }
				from = key;
			}
			float u = to.t() > from.t() ? Mth.clamp((t - from.t()) / (to.t() - from.t()), 0, 1) : 1;
			rotation = rotationMix(from.rot(), to.rot(), ease(from.ease(), u));
		}
		return new Key(t, rotation, result.pos(), result.scale(), Ease.LINEAR);
	}

	private static Key sampleEuler(Clip c, float t, Key restKey) {
		List<Key> keys = c.keys();
		if (c.hold() > 0 && !keys.isEmpty() && t >= lastT(c)) {
			// замер в последней позе, потом мягкий возврат в стойку
			Key last = keys.get(keys.size() - 1);
			float since = t - lastT(c) - c.hold();
			Key to = since <= 0 ? last : restKey;
			float u0 = since <= 0 ? 0 : ease(Ease.EASE_IN_OUT, Mth.clamp(since / Math.max(0.01f, c.back()), 0, 1));
			return new Key(t, lerp(last.rot(), to.rot(), u0), lerp(last.pos(), to.pos(), u0), lerp(last.scale(), to.scale(), u0), Ease.LINEAR);
		}
		// точки пути: стойка в начале (если первой позы в 0 нет), позы, стойка в конце (если нет hold)
		List<Key> pts = new ArrayList<>(keys.size() + 2);
		if (keys.isEmpty() || keys.get(0).t() > 0) pts.add(restKey);
		pts.addAll(keys);
		if (c.hold() <= 0 || keys.isEmpty())
			pts.add(new Key(c.length(), restKey.rot(), restKey.pos(), restKey.scale(), Ease.EASE_IN_OUT));
		int i = 0;
		while (i < pts.size() - 2 && pts.get(i + 1).t() <= t) i++;
		Key a = pts.get(i), b = pts.get(Math.min(i + 1, pts.size() - 1));
		float u = b.t() > a.t() ? Mth.clamp((t - a.t()) / (b.t() - a.t()), 0, 1) : 1;
		float raw = u;
		u = ease(a.ease(), u);
		if (!c.smooth()) return new Key(t, lerp(a.rot(), b.rot(), u), lerp(a.pos(), b.pos(), u), lerp(a.scale(), b.scale(), u), Ease.LINEAR);
		// smooth: кривая через все позы — клинок не останавливается в каждой
		Key a0 = pts.get(Math.max(i - 1, 0)), b1 = pts.get(Math.min(i + 2, pts.size() - 1));
		if (c.tight()) {
			float uh = a.ease() == Ease.EASE_IN_OUT ? raw : u; // кривая сама тормозит — без двойного торможения
			return new Key(t, tight(a0.rot(), a.rot(), b.rot(), b1.rot(), u, uh), tight(a0.pos(), a.pos(), b.pos(), b1.pos(), u, uh),
					tight(a0.scale(), a.scale(), b.scale(), b1.scale(), u, uh), Ease.LINEAR);
		}
		return new Key(t, spline(a0.rot(), a.rot(), b.rot(), b1.rot(), u), spline(a0.pos(), a.pos(), b.pos(), b1.pos(), u),
				spline(a0.scale(), a.scale(), b.scale(), b1.scale(), u), Ease.LINEAR);
	}

	/**
	 * "smooth": "tight" — тугая кривая: не перелетает позы. Где клинок разворачивается (оба конца отрезка — крайние позы),
	 * идёт как без кривой, со своим ease; где проходит позу насквозь — не останавливается в ней
	 * (касательные ограничены — Фрич–Карлсон). ue — время с ease, uh — для кривой.
	 */
	static Vector3f tight(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float ue, float uh) {
		float u2 = uh * uh, u3 = u2 * uh;
		float h00 = 2 * u3 - 3 * u2 + 1, h10 = u3 - 2 * u2 + uh, h01 = -2 * u3 + 3 * u2, h11 = u3 - u2;
		Vector3f r = new Vector3f();
		for (int k = 0; k < 3; k++) {
			float v0 = p0.get(k), v1 = p1.get(k), v2 = p2.get(k), v3 = p3.get(k);
			float s1 = slope(v1 - v0, v2 - v1), s2 = slope(v2 - v1, v3 - v2);
			r.setComponent(k, s1 == 0 && s2 == 0 ? v1 + (v2 - v1) * ue : h00 * v1 + h10 * s1 + h01 * v2 + h11 * s2);
		}
		return r;
	}

	private static float slope(float before, float after) {
		if (before * after <= 0) return 0;
		float m = (before + after) / 2;
		float lim = 3 * Math.min(Math.abs(before), Math.abs(after));
		return Math.copySign(Math.min(Math.abs(m), lim), m);
	}

	/** Кривая Катмулла–Рома: проходит через a и b, касательные — по соседям. */
	static Vector3f spline(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float u) {
		float u2 = u * u, u3 = u2 * u;
		Vector3f r = new Vector3f();
		for (int k = 0; k < 3; k++) {
			float v0 = p0.get(k), v1 = p1.get(k), v2 = p2.get(k), v3 = p3.get(k);
			r.setComponent(k, 0.5f * (2 * v1 + (-v0 + v2) * u + (2 * v0 - 5 * v1 + 4 * v2 - v3) * u2 + (-v0 + 3 * v1 - 3 * v2 + v3) * u3));
		}
		return r;
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
