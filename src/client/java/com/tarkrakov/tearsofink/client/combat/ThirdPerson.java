package com.tarkrakov.tearsofink.client.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Удары клинков от третьего лица (как видят другие и ты в F5): руки и корпус игрока.
 * Дорожка "third" в том же движении weapon_anims: общее время с первым лицом (length / hold / return / tempo),
 * повороты — градусы ПОВЕРХ обычной позы: arm — рука, которой бьют; other — вторая рука; body — корпус.
 * У дорожки свой impact (момент удара) — подгоняется под impact первого лица, чтобы удар совпадал.
 * Выключить всё третье лицо — ENABLED = false.
 */
public final class ThirdPerson {
	public static final boolean ENABLED = true;

	public record Key(float t, Vector3f arm, Vector3f other, Vector3f body, WeaponAnims.Ease ease) {}

	public record Track(float length, float impact, List<Key> keys) {}

	private static final class Playing {
		final WeaponAnims.Clip clip;
		final Archetype archetype;
		final long start = System.nanoTime();
		final float tempo;

		Playing(WeaponAnims.Clip clip, Archetype archetype, float tempo) {
			this.clip = clip;
			this.archetype = archetype;
			this.tempo = Math.max(0.1f, tempo);
		}

		float time() {
			return (System.nanoTime() - start) / 1e9f * tempo;
		}
	}

	/** id сущности → движения правой [0] и левой [1] руки. */
	private static final Map<Integer, Playing[]> PLAYING = new HashMap<>();

	private ThirdPerson() {}

	static Track parse(JsonObject o) {
		List<Key> keys = new ArrayList<>();
		Vector3f zero = new Vector3f();
		for (JsonElement el : o.getAsJsonArray("keyframes")) {
			JsonObject k = el.getAsJsonObject();
			keys.add(new Key(k.get("t").getAsFloat(), vec(k, "arm", zero), vec(k, "other", zero), vec(k, "body", zero),
					k.has("ease") ? WeaponAnims.Ease.valueOf(k.get("ease").getAsString().toUpperCase()) : WeaponAnims.Ease.EASE_IN_OUT));
		}
		keys.sort((a, b) -> Float.compare(a.t(), b.t()));
		float length = o.has("length") ? o.get("length").getAsFloat() : -1f;
		return new Track(length, o.has("impact") ? o.get("impact").getAsFloat() : -1f, keys);
	}

	private static Vector3f vec(JsonObject o, String key, Vector3f def) {
		if (!o.has(key)) return new Vector3f(def);
		JsonArray a = o.getAsJsonArray(key);
		return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
	}

	public static void play(int entity, HumanoidArm arm, Archetype a, String anim, float tempo) {
		WeaponAnims.Clip clip = WeaponAnims.clip(a, anim);
		Playing[] slots = PLAYING.computeIfAbsent(entity, k -> new Playing[2]);
		slots[arm.ordinal()] = clip == null || clip.third() == null ? null : new Playing(clip, a, tempo);
	}

	public static void clear() {
		PLAYING.clear();
	}

	/** Время движения первого лица → время дорожки третьего лица (подгонка по моменту удара и длине). */
	private static float remap(WeaponAnims.Clip c, float t) {
		Track tr = c.third();
		float lastFirst = c.keys().isEmpty() ? c.length() : c.keys().get(c.keys().size() - 1).t();
		float motionEnd = c.hold() > 0 ? lastFirst : c.length();
		float tl = tr.length() > 0 ? tr.length() : motionEnd;
		if (c.impact() > 0 && tr.impact() > 0) {
			if (t <= c.impact()) return t * tr.impact() / c.impact();
			if (t <= motionEnd) return tr.impact() + (t - c.impact()) * (tl - tr.impact()) / Math.max(0.01f, motionEnd - c.impact());
			return tl + (t - motionEnd);
		}
		if (t <= motionEnd) return t * tl / Math.max(0.01f, motionEnd);
		return tl + (t - motionEnd);
	}

	/** Вызывается после обычной позы модели игрока: добавить повороты ударов. */
	public static void apply(HumanoidModel<?> model, LivingEntity entity) {
		if (!ENABLED) return;
		Playing[] slots = PLAYING.get(entity.getId());
		if (slots == null) return;
		Vector3f bodyTotal = new Vector3f();
		boolean any = false;
		for (HumanoidArm side : HumanoidArm.values()) {
			Playing p = slots[side.ordinal()];
			if (p == null) continue;
			ItemCheck:
			{
				var stack = side == entity.getMainArm() ? entity.getMainHandItem() : entity.getOffhandItem();
				if (Archetype.of(stack) != p.archetype) {
					slots[side.ordinal()] = null;
					continue;
				}
			}
			WeaponAnims.Clip c = p.clip;
			float t = p.time();
			float lastFirst = c.keys().isEmpty() ? c.length() : c.keys().get(c.keys().size() - 1).t();
			float end = c.hold() > 0 ? lastFirst + c.hold() + c.back() : c.length();
			if (t >= end) {
				slots[side.ordinal()] = null;
				continue;
			}
			Key pose = pose(c, t);
			if (pose == null) continue;
			any = true;
			float m = side == HumanoidArm.LEFT ? -1 : 1; // левая рука — зеркально
			ModelPart arm = side == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
			ModelPart other = side == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
			add(arm, pose.arm(), m);
			add(other, pose.other(), m);
			bodyTotal.add(pose.body().x, m * pose.body().y, m * pose.body().z);
		}
		if (!any) {
			if (slots[0] == null && slots[1] == null) PLAYING.remove(entity.getId());
			return;
		}
		if (bodyTotal.lengthSquared() > 0) {
			float by = bodyTotal.y * Mth.DEG_TO_RAD;
			model.body.xRot += bodyTotal.x * Mth.DEG_TO_RAD;
			model.body.yRot += by;
			model.body.zRot += bodyTotal.z * Mth.DEG_TO_RAD;
			// плечи идут за поворотом корпуса (как в ванильном взмахе)
			model.rightArm.z = Mth.sin(model.body.yRot) * 5.0F;
			model.rightArm.x = -Mth.cos(model.body.yRot) * 5.0F;
			model.leftArm.z = -Mth.sin(model.body.yRot) * 5.0F;
			model.leftArm.x = Mth.cos(model.body.yRot) * 5.0F;
			model.rightArm.yRot += by;
			model.leftArm.yRot += by;
		}
	}

	private static void add(ModelPart part, Vector3f deg, float m) {
		part.xRot += deg.x * Mth.DEG_TO_RAD;
		part.yRot += m * deg.y * Mth.DEG_TO_RAD;
		part.zRot += m * deg.z * Mth.DEG_TO_RAD;
	}

	private static Key pose(WeaponAnims.Clip c, float t) {
		List<Key> keys = c.third().keys();
		if (keys.isEmpty()) return null;
		Key zero = new Key(0, new Vector3f(), new Vector3f(), new Vector3f(), WeaponAnims.Ease.EASE_IN_OUT);
		float lastFirst = c.keys().isEmpty() ? c.length() : c.keys().get(c.keys().size() - 1).t();
		if (c.hold() > 0 && t >= lastFirst) {
			Key last = keys.get(keys.size() - 1);
			float since = t - lastFirst - c.hold();
			if (since <= 0) return last;
			float u = WeaponAnims.ease(WeaponAnims.Ease.EASE_IN_OUT, Mth.clamp(since / Math.max(0.01f, c.back()), 0, 1));
			return mix(last, zero, u);
		}
		float tt = remap(c, t);
		Key a = zero, b = null;
		float ta = 0;
		float tl = c.third().length() > 0 ? c.third().length() : (c.hold() > 0 ? lastFirst : c.length());
		float tb = tl;
		for (Key k : keys) {
			if (k.t() <= tt) {
				a = k;
				ta = k.t();
			} else {
				b = k;
				tb = k.t();
				break;
			}
		}
		if (b == null) {
			if (c.hold() > 0) return a;
			b = zero;
			tb = Math.max(ta + 0.01f, tl);
		}
		float u = tb > ta ? Mth.clamp((tt - ta) / (tb - ta), 0, 1) : 1;
		return mix(a, b, WeaponAnims.ease(a.ease(), u));
	}

	private static Key mix(Key a, Key b, float u) {
		return new Key(0, WeaponAnims.lerp(a.arm(), b.arm(), u), WeaponAnims.lerp(a.other(), b.other(), u),
				WeaponAnims.lerp(a.body(), b.body(), u), a.ease());
	}
}
