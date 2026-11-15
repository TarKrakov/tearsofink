package com.tarkrakov.tearsofink.client.shield;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.weapon.SoulShieldItem;
import com.tarkrakov.tearsofink.weapon.shield.ShieldStance;
import com.tarkrakov.tearsofink.weapon.shield.ShieldTuning;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Стойка двух щитов у игроков (клиент): поза рук от третьего лица (assets/tears-of-ink/shield_stance.json, F3+T),
 * оба щита — в позе блока, свечение щитов в темноте и свет в тумане (растут с зарядом).
 */
public final class ShieldStanceClient {
	/** id сущности → заряд щитов. */
	private static final Map<Integer, Integer> CHARGE = new HashMap<>();
	/** Поза рук в стойке (градусы, для правой руки; левая — зеркально): front — рука, что держит блок, side — вторая, сбоку. */
	private static Vector3f front = new Vector3f(-55, -25, 0), side = new Vector3f(45, 40, 10);
	private static float look = 0.5f;

	private ShieldStanceClient() {}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(ShieldStance.Glow.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
			if (payload.charge() <= 0) CHARGE.remove(payload.entity());
			else CHARGE.put(payload.entity(), payload.charge());
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> CHARGE.clear());
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(ShieldStanceClient::tick);
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return TearsOfInk.id("shield_stance");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				var res = manager.getResource(TearsOfInk.id("shield_stance.json"));
				if (res.isEmpty()) return;
				try (Reader r = res.get().openAsReader()) {
					JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
					if (o.has("front")) front = vec(o.getAsJsonArray("front"));
					if (o.has("side")) side = vec(o.getAsJsonArray("side"));
					if (o.has("look")) look = o.get("look").getAsFloat();
				} catch (Exception e) {
					TearsOfInk.LOGGER.error("Поза стойки щитов не прочиталась: {}", e.toString());
				}
			}
		});
	}

	private static Vector3f vec(JsonArray a) {
		return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
	}

	/** Щиты Души в обеих руках, в Землях. */
	public static boolean dual(LivingEntity e) {
		return e != null && ModDimensions.isLands(e.level()) && ShieldStance.dual(e);
	}

	/** В сомкнутой стойке (оба щита подняты). */
	public static boolean stance(LivingEntity e) {
		return dual(e) && e.isUsingItem() && e.getUseItem().getItem() instanceof SoulShieldItem
				&& !(e instanceof Player p && p.getCooldowns().isOnCooldown(e.getUseItem().getItem()));
	}

	public static int charge(LivingEntity e) {
		return CHARGE.getOrDefault(e.getId(), 0);
	}

	/** Модель «в блоке» для щита: этот щит используют — или стойка двух щитов (оба подняты). */
	public static boolean blocking(ItemStack stack, LivingEntity e) {
		if (e == null || !e.isUsingItem()) return false;
		if (e.getUseItem() == stack) return true;
		return stance(e) && (e.getMainHandItem() == stack || e.getOffhandItem() == stack);
	}

	/** Поза рук от третьего лица (после обычной позы). */
	public static void pose(HumanoidModel<?> model, LivingEntity e) {
		if (!stance(e)) return;
		HumanoidArm using = e.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? e.getMainArm() : e.getMainArm().getOpposite();
		set(model, using, front, model.head.xRot * look);
		set(model, using.getOpposite(), side, model.head.xRot * look * 0.5f);
	}

	private static void set(HumanoidModel<?> model, HumanoidArm arm, Vector3f deg, float pitch) {
		float m = arm == HumanoidArm.RIGHT ? 1 : -1;
		ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
		part.xRot = deg.x * Mth.DEG_TO_RAD + pitch;
		part.yRot = m * deg.y * Mth.DEG_TO_RAD;
		part.zRot = m * deg.z * Mth.DEG_TO_RAD;
	}

	/** Свечение щита в руке: щиты Души парой светятся в темноте, ярче с зарядом. */
	public static int light(LivingEntity e, ItemStack stack, int light) {
		if (!(stack.getItem() instanceof SoulShieldItem) || !dual(e)) return light;
		int glow = Math.min(15, ShieldTuning.GLOW + ShieldTuning.GLOW_PER_CHARGE * charge(e));
		int block = Math.max(LightTexture.block(light), glow);
		return LightTexture.pack(block, LightTexture.sky(light));
	}

	/**
	 * Ореол: щит рисуется второй раз чуть крупнее «светящимся» слоем (как глаза паука — сложение света, без теней).
	 * Свет на блоки без сторонних модов не бросить, но сам щит выглядит светящимся. Ярче с зарядом.
	 */
	public static void halo(net.minecraft.client.renderer.ItemInHandRenderer renderer, LivingEntity e, ItemStack stack,
							net.minecraft.world.item.ItemDisplayContext ctx, boolean left, com.mojang.blaze3d.vertex.PoseStack pose,
							net.minecraft.client.renderer.MultiBufferSource buffers) {
		if (!(stack.getItem() instanceof SoulShieldItem) || !dual(e)) return;
		float f = Math.min(0.75f, HALO + HALO_PER_CHARGE * charge(e));
		net.minecraft.client.renderer.RenderType glow = net.minecraft.client.renderer.RenderType.eyes(
				net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
		net.minecraft.client.renderer.MultiBufferSource tinted = type -> new Tint(buffers.getBuffer(glow), f);
		pose.pushPose();
		pose.scale(HALO_SCALE, HALO_SCALE, HALO_SCALE);
		renderer.renderItem(e, stack, ctx, left, pose, tinted, LightTexture.FULL_BRIGHT);
		pose.popPose();
	}

	/** Сила ореола (0–1) и насколько он крупнее щита. */
	private static final float HALO = 0.18f, HALO_PER_CHARGE = 0.1f, HALO_SCALE = 1.06f;

	/** Приглушает цвет вершин (сила ореола). */
	private record Tint(com.mojang.blaze3d.vertex.VertexConsumer to, float f) implements com.mojang.blaze3d.vertex.VertexConsumer {
		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
			to.addVertex(x, y, z);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int b, int a) {
			to.setColor((int) (r * f), (int) (g * f), (int) (b * f), a);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
			to.setUv(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) {
			to.setUv1(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) {
			to.setUv2(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
			to.setNormal(x, y, z);
			return this;
		}
	}

	/** Искорки вокруг щитов у всех, кто держит два щита Души (чаще с зарядом). */
	public static void tick(net.minecraft.client.Minecraft mc) {
		if (mc.level == null || mc.isPaused() || !ModDimensions.isLands(mc.level)) return;
		var cam = mc.gameRenderer.getMainCamera().getPosition();
		for (Player p : mc.level.players()) {
			if (!dual(p) || p.distanceToSqr(cam) > 48 * 48) continue;
			int c = charge(p);
			if (p.tickCount % Math.max(1, 4 - c / 2) != 0) continue;
			boolean self = p == mc.player && mc.options.getCameraType().isFirstPerson();
			float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
			double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw); // вправо от тела
			for (int side = -1; side <= 1; side += 2) {
				double ox = rx * side * 0.55, oz = rz * side * 0.55;
				double x = p.getX() + ox + (p.getRandom().nextDouble() - 0.5) * 0.7;
				double y = p.getY() + (self ? 1.0 : 0.7) + p.getRandom().nextDouble() * 0.9;
				double z = p.getZ() + oz + (p.getRandom().nextDouble() - 0.5) * 0.7;
				mc.level.addParticle(com.tarkrakov.tearsofink.particle.ModParticles.TEAR_GLINT, x, y, z,
						(p.getRandom().nextDouble() - 0.5) * 0.01, 0.01 + p.getRandom().nextDouble() * 0.015, (p.getRandom().nextDouble() - 0.5) * 0.01);
			}
		}
	}

	/** Видимость в тумане с двумя щитами (как ручной фонарь). 0 — щитов нет. */
	public static float clarity(Player p) {
		if (!dual(p)) return 0;
		return ShieldTuning.CLARITY + ShieldTuning.CLARITY_PER_CHARGE * charge(p);
	}
}
