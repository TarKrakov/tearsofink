package com.tarkrakov.tearsofink.client.hunt;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.hunt.Hunt;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.world.phys.Vec3;

/** Луч Охоты в небо над целью (виден сквозь туман) и затемнение цели. */
public final class HuntClient {
	/** Насколько темнее цель Охоты (1 — как есть). */
	public static final float PREY_SHADE = 0.4f;
	private static final int BEAM_HEIGHT = 320;

	private static boolean on;
	private static int preyId = -1;
	private static int color;
	private static Vec3 target = Vec3.ZERO, shown = Vec3.ZERO;

	private HuntClient() {}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(Hunt.Beam.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
			boolean was = on;
			on = payload.on();
			preyId = payload.on() ? payload.entity() : -1;
			color = payload.color();
			target = new Vec3(payload.x(), payload.y(), payload.z());
			if (!was) shown = target;
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {
			on = false;
			preyId = -1;
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (!on) return;
			var e = mc.level == null ? null : mc.level.getEntity(preyId);
			if (e != null) target = e.position();
			shown = shown.lerp(target, 0.35);
		});
		WorldRenderEvents.AFTER_TRANSLUCENT.register(HuntClient::render);
	}

	public static boolean isPrey(int entityId) {
		return on && entityId == preyId;
	}

	private static void render(WorldRenderContext ctx) {
		if (!on || ctx.matrixStack() == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Vec3 cam = ctx.camera().getPosition();
		PoseStack pose = ctx.matrixStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
		RenderSystem.setShaderFogStart(1.0e6f); // луч не тонет в тумане
		RenderSystem.setShaderFogEnd(2.0e6f);
		pose.pushPose();
		pose.translate(shown.x - cam.x - 0.5, shown.y - cam.y, shown.z - cam.z - 0.5);
		BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, ctx.tickCounter().getGameTimeDeltaPartialTick(false),
				1.0f, mc.level.getGameTime(), 0, BEAM_HEIGHT, 0xFF000000 | color, 0.25f, 0.32f);
		pose.popPose();
		buffers.endBatch();
		RenderSystem.setShaderFogStart(fogStart);
		RenderSystem.setShaderFogEnd(fogEnd);
	}
}
