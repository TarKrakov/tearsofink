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
	/** Ближе этого к цели луч не рисуется (цель и так видно). */
	public static final double BEAM_HIDE_NEAR = 25;
	/** Ближе этого к цели, когда охотник уже в бою, — метка на экране пропадает. */
	public static final double MARKER_HIDE_NEAR = 25;
	private static final net.minecraft.resources.ResourceLocation MARKER =
			com.tarkrakov.tearsofink.TearsOfInk.id("textures/gui/hunt_marker.png");
	private static boolean engaged;

	private static boolean on;
	private static int preyId = -1, preyId2 = -1;
	private static int color;
	private static Vec3 target = Vec3.ZERO, shown = Vec3.ZERO;

	private HuntClient() {}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(Hunt.Beam.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
			boolean was = on;
			on = payload.on();
			preyId = payload.on() ? payload.entity() : -1;
			preyId2 = payload.on() ? payload.entity2() : -1;
			color = payload.color();
			engaged = payload.engaged();
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
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((g, delta) -> marker(g));
	}

	public static boolean isPrey(int entityId) {
		return on && (entityId == preyId || entityId == preyId2);
	}

	private static void render(WorldRenderContext ctx) {
		if (!on || ctx.matrixStack() == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Vec3 cam = ctx.camera().getPosition();
		if (Math.hypot(shown.x - cam.x, shown.z - cam.z) < BEAM_HIDE_NEAR) return; // вблизи луч не нужен
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

	/** Метка на экране: значок над горизонтом в сторону цели и расстояние. */
	private static void marker(net.minecraft.client.gui.GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (!on || mc.player == null || mc.options.hideGui) return;
		Vec3 eye = mc.player.getEyePosition();
		double dx = shown.x - eye.x, dz = shown.z - eye.z;
		double dist = Math.hypot(dx, dz);
		if (engaged && dist < MARKER_HIDE_NEAR) return; // дошёл и бой идёт — метка не нужна
		int w = g.guiWidth(), h = g.guiHeight();
		float yawTo = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float diff = net.minecraft.util.Mth.wrapDegrees(yawTo - mc.player.getYRot());
		double vfov = Math.toRadians(mc.options.fov().get());
		float half = (float) Math.toDegrees(Math.atan(Math.tan(vfov / 2) * w / (double) h));
		int x = w / 2 + Math.round(diff / half * (w / 2f));
		boolean off = x < 14 || x > w - 14;
		x = net.minecraft.util.Mth.clamp(x, 14, w - 14);
		int y = 22;
		float r = ((color >> 16) & 255) / 255f, gr = ((color >> 8) & 255) / 255f, b = (color & 255) / 255f;
		RenderSystem.enableBlend();
		g.setColor(Math.max(r, 0.35f), Math.max(gr, 0.35f), Math.max(b, 0.35f), off ? 0.6f : 1f);
		g.blit(MARKER, x - 8, y - 8, 0, 0, 16, 16, 16, 16);
		g.setColor(1, 1, 1, 1);
		String text = net.minecraft.network.chat.Component.translatable("hunt.tears-of-ink.marker", (int) dist).getString();
		g.drawCenteredString(mc.font, text, x, y + 10, 0xFFE8D8B0);
		if (off) g.drawCenteredString(mc.font, diff < 0 ? "◀" : "▶", x + (diff < 0 ? 12 : -12), y - 4, 0xFFE8D8B0);
	}
}
