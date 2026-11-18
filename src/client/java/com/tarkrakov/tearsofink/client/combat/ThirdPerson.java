package com.tarkrakov.tearsofink.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.weapon.InkGreatswordItem;
import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Удары клинков от третьего лица (как видят другие, ты в F5, и как бьют охотники) — считаются сами из движений первого лица.
 * <p>
 * Каждый кадр берётся поза клинка от первого лица (weapon_anims/&lt;архетип&gt;.json) и переводится в позу Стива:
 * <ul>
 *   <li>рука — тянется туда, где от первого лица рукоять и клинок (влево, вправо, вверх, от себя);</li>
 *   <li>кисть — оружие в руке поворачивается так, чтобы лежать как от первого лица (у ванильной руки нет запястья);</li>
 *   <li>корпус — чуть доворачивается за широким махом;</li>
 *   <li>двуручное без второго оружия — вторая рука держит рукоять.</li>
 * </ul>
 * Время — общее с первым лицом. Подкрутка — поле "third" в файле движений: reach (насколько рука следует за клинком),
 * lead (0 — рука идёт за рукоятью, 1 — за кончиком), body (доворот корпуса), look (насколько рука следует за взглядом вверх/вниз).
 * Работает ТОЛЬКО в Чернильных землях и ТОЛЬКО с клинками архетипов. Выключить всё — ENABLED = false.
 */
public final class ThirdPerson {
	public static final boolean ENABLED = true;

	/** За сколько секунд новый удар перетекает из позы предыдущего. */
	private static final float BLEND = 0.12f;
	/** За сколько секунд рука поднимается из обычной позы в начале и опускается в конце. */
	private static final float RAMP_IN = 0.1f, RAMP_OUT = 0.15f;
	private static final float MAX_BODY = 25 * Mth.DEG_TO_RAD;

	/** Ванильный предмет в руке (ItemInHandLayer): Rx(−90)·Ry(180). */
	private static final Quaternionf LAYER = new Quaternionf().rotationX(-Mth.HALF_PI).rotateY(Mth.PI);
	/** Камера (x вправо, y вверх, z к себе) → пространство модели игрока (x влево, y вниз, z назад): поворот на 180° вокруг z. */
	private static final Quaternionf CAM_TO_MODEL = new Quaternionf().rotationZ(Mth.PI);

	/** Поза одной руки: углы руки, поворот кисти и точка, вокруг которой крутится оружие. */
	private record Pose(float x, float y, float z, Quaternionf wrist, Vector3f pivot, float push) {}

	private static final class Playing {
		final WeaponAnims.Clip clip;
		final Archetype archetype;
		final long start = System.nanoTime();
		final float tempo;
		final boolean authored;
		@Nullable Pose from;

		Playing(WeaponAnims.Clip clip, Archetype archetype, float tempo, boolean authored) {
			this.clip = clip;
			this.archetype = archetype;
			this.tempo = Math.max(0.1f, tempo);
			this.authored = authored;
		}

		float real() {
			return (System.nanoTime() - start) / 1e9f;
		}

		float time() {
			return real() * tempo;
		}
	}

	/** id сущности → движения рук [HumanoidArm.ordinal()]. */
	private static final Map<Integer, Playing[]> PLAYING = new HashMap<>();
	/** id сущности → последняя показанная поза рук (для перетекания и для кисти). */
	private static final Map<Integer, Pose[]> SHOWN = new HashMap<>();

	private ThirdPerson() {}

	public static void play(int entity, HumanoidArm arm, Archetype a, String anim, float tempo) {
		WeaponAnims.Clip clip = WeaponAnims.clip(a, anim);
		Playing[] slots = PLAYING.computeIfAbsent(entity, k -> new Playing[2]);
		Playing next = clip == null ? null : new Playing(clip, a, tempo, anim.startsWith("bb_author_"));
		if (next != null) {
			Pose[] shown = SHOWN.get(entity);
			Playing prev = slots[arm.ordinal()];
			if (shown != null && shown[arm.ordinal()] != null && prev != null && prev.archetype == a) next.from = shown[arm.ordinal()];
		}
		slots[arm.ordinal()] = next;
	}

	public static void clear() {
		PLAYING.clear();
		SHOWN.clear();
		AXES.clear();
	}

	private static void drop(int id) {
		PLAYING.remove(id);
		SHOWN.remove(id);
	}

	/** Вызывается после обычной позы модели (HumanoidModel.setupAnim): руки, корпус и кисть ударов. */
	public static void apply(HumanoidModel<?> model, LivingEntity entity) {
		if (!ENABLED) return;
		int id = entity.getId();
		Playing[] slots = PLAYING.get(id);
		if (slots == null) return;
		if (entity.level() == null || entity.level().dimension() != ModDimensions.INK_LANDS) {
			drop(id);
			return;
		}
		Pose[] shown = SHOWN.computeIfAbsent(id, k -> new Pose[2]);
		float body = 0;
		int playing = 0;
		HumanoidArm supportFor = null;
		Vector3f supportPoint = null;
		float supportWeight = 0;
		for (HumanoidArm side : HumanoidArm.values()) {
			int i = side.ordinal();
			Playing p = slots[i];
			ItemStack stack = stackIn(entity, side);
			if (p != null && (Archetype.of(stack) != p.archetype || p.time() >= WeaponAnims.end(p.clip))) {
				slots[i] = null;
				p = null;
			}
			if (p == null) {
				shown[i] = null;
				continue;
			}
			playing++;
			float t = p.time();
			Solved s = solve(model, entity, stack, side, p.clip, t, p.authored);
			float w = weight(p, t);
			ModelPart arm = arm(model, side);
			float x = Mth.lerp(w, arm.xRot, s.x), y = Mth.lerp(w, arm.yRot, s.y), z = Mth.lerp(w, arm.zRot, s.z);
			Quaternionf wrist = new Quaternionf().slerp(s.wrist, w);
			float push = s.push * w;
			float transition = p.authored ? 0.09f : (p.archetype == Archetype.PALE_LINEAR_GREATSWORD ? 0.2f : BLEND);
			if (!p.clip.third().baked() && p.from != null && p.real() < transition) {
				float u = WeaponAnims.ease(WeaponAnims.Ease.EASE_OUT, p.real() / transition);
				x = Mth.lerp(u, p.from.x, x);
				y = Mth.lerp(u, p.from.y, y);
				z = Mth.lerp(u, p.from.z, z);
				wrist = new Quaternionf(p.from.wrist).slerp(wrist, u);
				push = Mth.lerp(u, p.from.push, push);
			}
			arm.xRot = x;
			arm.yRot = y;
			arm.zRot = z;
			shown[i] = new Pose(x, y, z, wrist, s.pivot, push);
			body += s.body * w;
			if (InkGreatswordItem.twoHanded(stack) && stackIn(entity, side.getOpposite()).isEmpty()) {
				supportFor = side;
				supportPoint = s.hand;
				supportWeight = w;
			}
		}
		if (playing == 0) {
			drop(id);
			return;
		}
		// двуручное в одной руке: вторая рука тоже на рукояти
		if (playing == 1 && supportFor != null && slots[supportFor.getOpposite().ordinal()] == null) {
			HumanoidArm other = supportFor.getOpposite();
			// к рукояти, но ближе к середине — чтобы рука не проходила сквозь грудь
			Vector3f grip = new Vector3f(supportPoint.x * 0.6f, supportPoint.y, supportPoint.z);
			float[] a = aim(model, grip, other == HumanoidArm.LEFT ? -1 : 1, WeaponAnims.ThirdTune.DEFAULT.reach(), WeaponAnims.ThirdTune.DEFAULT.look());
			ModelPart arm = arm(model, other);
			arm.xRot = Mth.lerp(supportWeight, arm.xRot, a[0]);
			arm.yRot = Mth.lerp(supportWeight, arm.yRot, a[1]);
			arm.zRot = Mth.lerp(supportWeight, arm.zRot, 0);
		}
		if (playing == 2) body = 0; // парой — корпус прямо
		if (body != 0) {
			body = Mth.clamp(body, -MAX_BODY, MAX_BODY);
			model.body.yRot += body;
			// плечи идут за поворотом корпуса (как в ванильном взмахе)
			model.rightArm.z = Mth.sin(model.body.yRot) * 5.0F;
			model.rightArm.x = -Mth.cos(model.body.yRot) * 5.0F;
			model.leftArm.z = -Mth.sin(model.body.yRot) * 5.0F;
			model.leftArm.x = Mth.cos(model.body.yRot) * 5.0F;
		}
	}

	/** ItemInHandLayer: перед рисованием оружия повернуть его «кистью» (только когда идёт удар). */
	public static void wrist(LivingEntity entity, HumanoidArm side, PoseStack pose) {
		if (!ENABLED) return;
		Pose[] shown = SHOWN.get(entity.getId());
		if (shown == null) return;
		Pose p = shown[side.ordinal()];
		if (p == null) return;
		if (p.push != 0) pose.translate(0, 0, -p.push); // third.push — вперёд вдоль руки (щит не насаживается на кулак)
		pose.translate(p.pivot.x, p.pivot.y, p.pivot.z);
		pose.mulPose(p.wrist);
		pose.translate(-p.pivot.x, -p.pivot.y, -p.pivot.z);
	}

	private static ItemStack stackIn(LivingEntity e, HumanoidArm side) {
		return side == e.getMainArm() ? e.getMainHandItem() : e.getOffhandItem();
	}

	private static ModelPart arm(HumanoidModel<?> model, HumanoidArm side) {
		return side == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
	}

	/** Рука плавно входит в удар из обычной позы и плавно выходит в конце. Новый удар сразу после старого — без входа. */
	private static float weight(Playing p, float t) {
		WeaponAnims.Clip c = p.clip;
		if (c.third().baked()) return 1;
		float rampIn = p.authored ? 0.09f : (p.archetype == Archetype.PALE_LINEAR_GREATSWORD ? 0.18f : RAMP_IN);
		float in = p.from != null ? 1 : Mth.clamp(t / rampIn, 0, 1);
		float outTime = c.hold() > 0 ? Math.max(0.05f, c.back()) : RAMP_OUT;
		float out = Mth.clamp((WeaponAnims.end(c) - t) / outTime, 0, 1);
		return WeaponAnims.ease(WeaponAnims.Ease.EASE_IN_OUT, Math.min(in, out));
	}

	// ---------------- перевод позы первого лица ----------------

	private record Solved(float x, float y, float z, Quaternionf wrist, Vector3f pivot, float body, Vector3f hand, float push) {}

	private static Solved solve(HumanoidModel<?> model, LivingEntity entity, ItemStack stack, HumanoidArm side, WeaponAnims.Clip clip, float t, boolean authored) {
		boolean left = side == HumanoidArm.LEFT;
		float m = left ? -1 : 1;
		Archetype archetype = Archetype.of(stack);
		WeaponAnims.ThirdTune tune = WeaponAnims.thirdTune(archetype);
		WeaponAnims.ThirdFix fix = clip.third();
		BakedModel baked = Minecraft.getInstance().getItemRenderer().getModel(stack, entity.level(), entity, entity.getId());
		ItemTransforms tr = baked.getTransforms();
		ItemTransform tp = tr.getTransform(left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
		Vector3f grip = WeaponAnims.grip(archetype);
		// щит держат за середину (centre): рука тянется к середине, кисть крутит вокруг неё
		Vector3f[] ax = tune.centre() ? centre(baked) : axis(baked, tr.getTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND), grip);
		// mirror: левая рука считается как правая и отражается — парный удар синхронно
		boolean mirror = fix.mirror() && left;
		HumanoidArm calc = mirror ? HumanoidArm.RIGHT : side;
		WeaponAnims.Clip pose = clip;
		float poseTime = t;
		if (fix.source() != null) { // третье лицо — по другому движению
			WeaponAnims.Clip src = WeaponAnims.clip(archetype, fix.source());
			if (src != null) {
				pose = src;
				// Старые F5-источники короче авторских FP-фаз: проигрывать всю дугу, а не бросать руку посреди удара.
				if (authored) poseTime = Math.min(t, clip.length()) * src.length() / Math.max(0.01f, clip.length());
			}
		}
		if (fix.armSpace()) {
			// Local opt-in: author the shoulder, retaining this hand's exact Display.
			// Source rotations are deltas from the vanilla held-item arm (-18°, 0, 0).
			WeaponAnims.Key k = WeaponAnims.sample(pose, poseTime);
			float x = -Mth.PI / 10 + k.rot().x * Mth.DEG_TO_RAD;
			float y = m * k.rot().y * Mth.DEG_TO_RAD;
			float z = m * k.rot().z * Mth.DEG_TO_RAD;
			Vector3f hand = at(ax[0], tp, m).add(m / 16f, .125f, -.625f);
			LAYER.transform(hand);
			new Quaternionf().rotationZ(z).rotateY(y).rotateX(x).transform(hand);
			hand.add(-m * 5 / 16f, 2 / 16f, 0);
			// Model head origin is at 1.501 blocks, standing camera at 1.62.
			hand.set(-hand.x, -hand.y - .119f, hand.z);
			Quaternionf wrist = new Quaternionf();
			WeaponAnims.Clip wristClip = fix.wristSource() == null ? null : WeaponAnims.clip(archetype, fix.wristSource());
			if (wristClip != null) {
				Vector3f rotation = WeaponAnims.sample(wristClip, t).rot();
				if (fix.wristXYZ()) {
					wrist.rotationXYZ(rotation.x * Mth.DEG_TO_RAD, rotation.y * Mth.DEG_TO_RAD, rotation.z * Mth.DEG_TO_RAD);
				} else {
					Vector3f blade = at(ax[1], tp, m).sub(at(ax[0], tp, m)).normalize();
					wrist.rotationAxis(rotation.x * Mth.DEG_TO_RAD, blade);
				}
			}
			return new Solved(x + model.head.xRot * tune.look(), y + model.head.yRot, z,
					wrist, at(ax[0], tp, m), fix.baked() ? 0 : y * tune.body(), hand, 0);
		}
		Base b = base(tr, calc, pose, poseTime, tune, fix, grip, ax);
		Base bw = fix.wristHold() >= 0 ? base(tr, calc, pose, fix.wristHold(), tune, fix, grip, ax) : b;
		if (mirror) {
			b = b.mirrored(!tune.centre());
			bw = bw.mirrored(!tune.centre());
		}
		Quaternionf look = look(model, tune.look());
		float[] a = angles(look.transform(new Vector3f(b.dir)));
		// кисть: оружие от третьего лица лежит как от первого (wrist_hold — как в позе на той секунде, относительно руки)
		float[] aw = bw == b ? a : angles(look.transform(new Vector3f(bw.dir)));
		Quaternionf armQ = new Quaternionf().rotationY(aw[1]).rotateX(aw[0]);
		Quaternionf wrist = new Quaternionf(armQ).mul(LAYER).conjugate()
				.mul(look).mul(new Quaternionf().rotationY(-m * fix.turn() * Mth.DEG_TO_RAD)) // turn — к центру
				.mul(bw.item).mul(rot(tp, m).conjugate());
		return new Solved(a[0], a[1], 0, wrist, at(ax[0], tp, m), a[1] * tune.body(), b.hand, (tune.push() + (left ? fix.pushLeft() : fix.pushRight())) / 16f);
	}

	/** Поза первого лица в пространстве модели (до поворота головы): куда тянется рука и как лежит оружие. */
	private record Base(Vector3f dir, Quaternionf item, Vector3f hand) {
		Base mirrored(boolean flip) {
			// отражение по x; плоский клинок переворачиваем вокруг его оси y — лезвие «лицом», без выворота.
			// Щит (flip=false) не переворачиваем: иначе он развернётся к врагу тылом.
			Quaternionf q = new Quaternionf(item.x, -item.y, -item.z, item.w);
			if (flip) q.rotateY(Mth.PI);
			return new Base(new Vector3f(-dir.x, dir.y, dir.z), q,
					new Vector3f(-hand.x, hand.y, hand.z));
		}
	}

	private static Base base(ItemTransforms tr, HumanoidArm side, WeaponAnims.Clip clip, float t, WeaponAnims.ThirdTune tune,
							 WeaponAnims.ThirdFix fix, @Nullable Vector3f grip, Vector3f[] ax) {
		boolean left = side == HumanoidArm.LEFT;
		float m = left ? -1 : 1;
		ItemTransform fp = tr.getTransform(left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
		WeaponAnims.Key k = WeaponAnims.sample(clip, t);
		Quaternionf rel = new Quaternionf().rotationXYZ(k.rot().x * Mth.DEG_TO_RAD, m * k.rot().y * Mth.DEG_TO_RAD, m * k.rot().z * Mth.DEG_TO_RAD);
		Vector3f[] ht = handTip(fp, m, rel, new Vector3f(m * k.pos().x / 16f, k.pos().y / 16f, k.pos().z / 16f), grip, ax);
		Vector3f target = new Vector3f(ht[1]).sub(ht[0]).mul(tune.lead()).add(ht[0]);
		if (fix.lateral() != 1) {
			Vector3f[] r = handTip(fp, m, new Quaternionf(), new Vector3f(), grip, ax);
			float restX = r[0].x + (r[1].x - r[0].x) * tune.lead();
			target.x = restX + fix.lateral() * (target.x - restX);
		}
		target.x *= fix.center();
		if (fix.noCross()) target.x = m * Math.max(m * target.x, 0.08f);
		Vector3f v = new Vector3f(target).sub(m * 5 / 16f, -0.245f, 0);
		v.mul(tune.reach(), tune.reach(), 1).normalize();
		Vector3f dir = new Vector3f(-v.x, -v.y, v.z);
		Quaternionf item = new Quaternionf(CAM_TO_MODEL).mul(rel).mul(rot(fp, m));
		if (fix.followBlade()) {
			// Let the shoulder lead the blade; the ordinary wrist solve below preserves
			// its full authored orientation (including edge roll), with no item translation.
			Vector3f bladeDirection = new Vector3f(ax[1]).sub(ax[0]).mul(fp.scale).normalize();
			item.transform(bladeDirection);
			dir.lerp(bladeDirection, 0.75f).normalize();
		}
		return new Base(dir, item, ht[0]);
	}

	/** [рукоять, кончик] от первого лица (пространство камеры). */
	private static Vector3f[] handTip(ItemTransform fp, float m, Quaternionf rel, Vector3f pos, @Nullable Vector3f grip, Vector3f[] ax) {
		Vector3f o = new Vector3f(m * 0.56f, -0.52f, -0.72f).add(pos);
		if (grip != null) {
			Vector3f g = at(grip, fp, m);
			return new Vector3f[]{rel.transform(at(ax[0], fp, m).sub(g)).add(g).add(o), rel.transform(at(ax[1], fp, m).sub(g)).add(g).add(o)};
		}
		return new Vector3f[]{rel.transform(at(ax[0], fp, m)).add(o), rel.transform(at(ax[1], fp, m)).add(o)};
	}

	/** Углы руки (x, y), чтобы она смотрела по направлению d (пространство модели). */
	private static float[] angles(Vector3f d) {
		float x = -(float) Math.acos(Mth.clamp(d.y, -1, 1));
		float y = Math.abs(Mth.sin(x)) > 1e-3f ? (float) Mth.atan2(-d.x, -d.z) : 0;
		return new float[]{x, y};
	}

	/** Углы руки (x, y), чтобы она смотрела из плеча на точку p (пространство камеры, от глаз). */
	private static float[] aim(HumanoidModel<?> model, Vector3f p, float m, float reach, float look) {
		Vector3f v = new Vector3f(p).sub(m * 5 / 16f, -0.245f, 0);
		v.mul(reach, reach, 1).normalize();
		return angles(look(model, look).transform(new Vector3f(-v.x, -v.y, v.z)));
	}

	/** Куда смотрит голова относительно корпуса: рука следует за взглядом. */
	private static Quaternionf look(HumanoidModel<?> model, float look) {
		return new Quaternionf().rotationY(model.head.yRot).rotateX(model.head.xRot * look);
	}

	/** Поворот стойки ItemTransform (как ItemTransform.apply: у левой руки y и z зеркально). */
	private static Quaternionf rot(ItemTransform t, float m) {
		return new Quaternionf().rotationXYZ(t.rotation.x * Mth.DEG_TO_RAD, m * t.rotation.y * Mth.DEG_TO_RAD, m * t.rotation.z * Mth.DEG_TO_RAD);
	}

	/** Где точка модели (пиксели Blockbench) оказывается в руке при стойке t. */
	private static Vector3f at(Vector3f px, ItemTransform t, float m) {
		Vector3f v = new Vector3f(px).div(16f).sub(0.5f, 0.5f, 0.5f).mul(t.scale);
		rot(t, m).transform(v);
		return v.add(m * t.translation.x, t.translation.y, t.translation.z);
	}

	// ---------------- рукоять и кончик оружия ----------------

	private static final Map<BakedModel, Vector3f[]> AXES = new IdentityHashMap<>();

	/**
	 * [рукоять, кончик] в пикселях модели. Есть grip — рукоять это он, кончик — самая дальняя точка.
	 * Нет — концы самой длинной оси модели; рукоять тот, что в стойке ближе к руке.
	 */
	private static Vector3f[] axis(BakedModel baked, ItemTransform fpRight, @Nullable Vector3f grip) {
		Vector3f[] cached = AXES.get(baked);
		if (cached != null && (grip == null || cached[0].equals(grip))) return cached;
		List<Vector3f> pts = points(baked);
		Vector3f[] out;
		if (pts.isEmpty()) {
			out = new Vector3f[]{grip != null ? new Vector3f(grip) : new Vector3f(2, 2, 8), new Vector3f(15, 15, 8)};
		} else if (grip != null) {
			Vector3f far = pts.get(0);
			for (Vector3f p : pts) if (p.distanceSquared(grip) > far.distanceSquared(grip)) far = p;
			out = new Vector3f[]{new Vector3f(grip), new Vector3f(far)};
		} else {
			Vector3f c = new Vector3f();
			for (Vector3f p : pts) c.add(p);
			c.div(pts.size());
			float[][] cov = new float[3][3];
			for (Vector3f p : pts) {
				float[] d = {p.x - c.x, p.y - c.y, p.z - c.z};
				for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) cov[i][j] += d[i] * d[j];
			}
			Vector3f dir = new Vector3f(1, 1, 0.1f).normalize();
			for (int n = 0; n < 40; n++) {
				Vector3f nd = new Vector3f(
						cov[0][0] * dir.x + cov[0][1] * dir.y + cov[0][2] * dir.z,
						cov[1][0] * dir.x + cov[1][1] * dir.y + cov[1][2] * dir.z,
						cov[2][0] * dir.x + cov[2][1] * dir.y + cov[2][2] * dir.z);
				if (nd.lengthSquared() < 1e-12f) break;
				dir = nd.normalize();
			}
			Vector3f hi = pts.get(0), lo = pts.get(0);
			for (Vector3f p : pts) {
				float pr = new Vector3f(p).sub(c).dot(dir);
				if (pr > new Vector3f(hi).sub(c).dot(dir)) hi = p;
				if (pr < new Vector3f(lo).sub(c).dot(dir)) lo = p;
			}
			boolean hiIsHandle = at(hi, fpRight, 1).length() < at(lo, fpRight, 1).length();
			out = hiIsHandle ? new Vector3f[]{new Vector3f(hi), new Vector3f(lo)} : new Vector3f[]{new Vector3f(lo), new Vector3f(hi)};
		}
		AXES.put(baked, out);
		return out;
	}

	/** [середина модели, точка над ней] — для щитов: их держат за середину, а не за край. */
	private static Vector3f[] centre(BakedModel baked) {
		List<Vector3f> pts = points(baked);
		if (pts.isEmpty()) return new Vector3f[]{new Vector3f(8, 8, 8), new Vector3f(8, 16, 8)};
		Vector3f lo = new Vector3f(Float.MAX_VALUE), hi = new Vector3f(-Float.MAX_VALUE);
		for (Vector3f p : pts) {
			lo.min(p);
			hi.max(p);
		}
		Vector3f c = new Vector3f(lo).add(hi).mul(0.5f);
		return new Vector3f[]{c, new Vector3f(c).add(0, 8, 0)};
	}

	private static List<Vector3f> points(BakedModel baked) {
		List<Vector3f> out = new ArrayList<>();
		RandomSource random = RandomSource.create(42);
		List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
		sides.add(null);
		for (Direction d : sides) {
			for (BakedQuad q : baked.getQuads(null, d, random)) {
				int[] v = q.getVertices();
				int stride = v.length / 4;
				for (int i = 0; i < 4; i++) {
					out.add(new Vector3f(Float.intBitsToFloat(v[i * stride]), Float.intBitsToFloat(v[i * stride + 1]),
							Float.intBitsToFloat(v[i * stride + 2])).mul(16f));
				}
			}
		}
		return out;
	}
}
