package com.tarkrakov.tearsofink.client.vow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.shield.ShieldStanceClient;
import com.tarkrakov.tearsofink.vow.Aura;
import com.tarkrakov.tearsofink.weapon.shield.ShieldTuning;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Свет и тьма путей на клиенте (сервер — vow/Aura): ясность в тумане от внутреннего света, свечение и ореол светящихся,
 * чернильные вены и пустое лицо проклятых, искорки и капли. Только в Чернильных Землях.
 * Текстуры: textures/entity/aura/white.png (свет), veins_1..3.png (развёртка скина 64×64; 3 — с пустым лицом) — заглушки Claude.
 */
public final class AuraClient {
	// ===== Настройки — крутить здесь =====
	/** Ясность в тумане без щитов по ступени света 0…3 (0 — паломник 100–114; 18/26/44 — тусклый/обычный/яркий фонарь). */
	public static final float[] LIGHT = {14, 18, 26, 44};
	/** С двумя щитами — на ступень выше (+ заряд стойки сверху). */
	public static final float[] LIGHT_SHIELDS = {18, 26, 44, 52};
	/** Душа на исходе — свет во столько раз слабее (и мерцает). */
	public static final float LOW_FACTOR = 0.6f;
	/** Свет союзникам: игроки ближе этого (блоки) видят дальше — на эту долю света светящегося. */
	public static final double ALLY_RADIUS = 4;
	public static final float ALLY_SHARE = 0.5f;
	/**
	 * Сила свечения тела и ореола по ступени света 0…3. Свет складывается поверх скина (и ореол тоже ложится на тело спереди),
	 * поэтому числа малые: при сумме ~0,5 персонаж выгорает в белый силуэт без цветов (так было, 15 ноя). Больше — ярче.
	 */
	private static final float[] GLOW = {0.0f, 0.02f, 0.035f, 0.05f}, HALO = {0.05f, 0.07f, 0.1f, 0.13f};

	private static final ResourceLocation WHITE = TearsOfInk.id("textures/entity/aura/white.png");
	private static final ResourceLocation[] VEINS = {
			TearsOfInk.id("textures/entity/aura/veins_1.png"),
			TearsOfInk.id("textures/entity/aura/veins_2.png"),
			TearsOfInk.id("textures/entity/aura/veins_3.png")};

	private static final Map<Integer, Aura.State> STATES = new HashMap<>();

	private AuraClient() {}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(Aura.State.TYPE, (payload, ctx) -> ctx.client().execute(() -> STATES.put(payload.entity(), payload)));
		ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> STATES.clear());
		ClientTickEvents.END_CLIENT_TICK.register(AuraClient::tick);
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof PlayerRenderer pr) helper.register(new Layer(pr));
		});
	}

	private static Aura.State state(Player p) {
		return STATES.get(p.getId());
	}

	/** Свет одного игрока в тумане (0 — не светит). */
	private static float lightOf(Player p) {
		Aura.State s = state(p);
		boolean dual = ShieldStanceClient.dual(p);
		if (s == null || s.light() < 0) return ShieldStanceClient.clarity(p); // Путь Чернил: только два щита
		int st = Math.min(3, s.light());
		float v = dual ? LIGHT_SHIELDS[st] + ShieldTuning.CLARITY_PER_CHARGE * ShieldStanceClient.charge(p) : LIGHT[st];
		if (s.low()) v *= LOW_FACTOR * (0.9f + 0.1f * Mth.sin(p.tickCount * 0.9f)); // Душа на исходе — тускнеет и мерцает
		return v;
	}

	/** Видимость в тумане для своего игрока: свой свет или (часть) света светящегося рядом. */
	public static float clarity(Player me) {
		float best = lightOf(me);
		if (me.level() == null) return best;
		for (Player o : me.level().players()) {
			if (o == me || o.distanceToSqr(me) > ALLY_RADIUS * ALLY_RADIUS) continue;
			Aura.State s = state(o);
			if (s != null && s.light() >= 0) best = Math.max(best, lightOf(o) * ALLY_SHARE);
		}
		return best;
	}

	/** Искорки вокруг светящихся, капли чернил с рук проклятых. */
	private static void tick(Minecraft mc) {
		if (mc.level == null || mc.isPaused() || !ModDimensions.hasLandsRules(mc.level)) return;
		var cam = mc.gameRenderer.getMainCamera().getPosition();
		for (Player p : mc.level.players()) {
			Aura.State s = state(p);
			if (s == null || p.isInvisible() || p.distanceToSqr(cam) > 48 * 48) continue;
			boolean self = p == mc.player && mc.options.getCameraType().isFirstPerson();
			var rnd = p.getRandom();
			if (s.light() >= 0 && p.tickCount % (6 - Math.min(3, s.light())) == 0 && !(s.low() && rnd.nextBoolean())) {
				mc.level.addParticle(com.tarkrakov.tearsofink.particle.ModParticles.TEAR_GLINT,
						p.getX() + (rnd.nextDouble() - 0.5) * 0.9, p.getY() + (self ? 0.9 : 0.3) + rnd.nextDouble() * 1.6,
						p.getZ() + (rnd.nextDouble() - 0.5) * 0.9, 0, 0.012 + rnd.nextDouble() * 0.012, 0);
			}
			if (s.dark() >= 2 && p.tickCount % (s.dark() >= 3 ? 7 : 13) == 0 && !self) {
				float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
				double side = rnd.nextBoolean() ? 0.38 : -0.38;
				mc.level.addParticle(ParticleTypes.SQUID_INK, p.getX() - Mth.cos(yaw) * side, p.getY() + 0.65, p.getZ() - Mth.sin(yaw) * side, 0, -0.04, 0);
			}
		}
	}

	/** Слой поверх модели игрока: вены и лицо (тьма), свечение и ореол (свет). */
	private static final class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
		Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbAmount,
				float partialTick, float age, float headYaw, float headPitch) {
			if (p.isInvisible() || !ModDimensions.hasLandsRules(p.level())) return;
			Aura.State s = state(p);
			if (s == null) return;
			PlayerModel<AbstractClientPlayer> model = getParentModel();
			if (s.dark() > 0) {
				model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(VEINS[Math.min(3, s.dark()) - 1])), light,
						OverlayTexture.NO_OVERLAY, -1);
			}
			if (s.light() >= 0) {
				int st = Math.min(3, s.light());
				float k = s.low() ? 0.5f + 0.25f * Mth.sin(age * 0.9f) : 1f;
				renderSoulLight(model, pose, buffers, st, k);
			}
		}
	}

	public static void renderSoulLight(net.minecraft.client.model.EntityModel<?> model, PoseStack pose,
			MultiBufferSource buffers, int stage, float strength) {
		RenderType glow = RenderType.eyes(WHITE);
		model.renderToBuffer(pose, new Tint(buffers.getBuffer(glow), GLOW[stage] * strength),
				0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFF4DC);
		pose.pushPose();
		pose.scale(1.08f, 1.04f, 1.08f);
		model.renderToBuffer(pose, new Tint(buffers.getBuffer(glow), HALO[stage] * strength),
				0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFF4DC);
		pose.popPose();
	}

	public static void renderWhiteLight(net.minecraft.client.model.EntityModel<?> model, PoseStack pose,
			MultiBufferSource buffers, float strength) {
		int alpha = Mth.clamp(Math.round(strength * 255), 0, 255);
		model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE)),
				0xF000F0, OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF);
	}

	/** Приглушает цвет вершин (сила свечения). */
	private record Tint(VertexConsumer to, float f) implements VertexConsumer {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			to.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			to.setColor((int) (r * f), (int) (g * f), (int) (b * f), a);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			to.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			to.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			to.setUv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			to.setNormal(x, y, z);
			return this;
		}
	}
}
