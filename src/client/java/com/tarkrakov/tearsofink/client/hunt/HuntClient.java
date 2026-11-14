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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Лучи Охот в небо над целью (видны сквозь туман), метки на экране и затемнение целей.
 * Охот у игрока может быть две сразу (мировая и контракт) — у каждой свой луч и своя метка.
 */
public final class HuntClient {
	/** Насколько темнее цель Охоты (1 — как есть). */
	public static final float PREY_SHADE = 0.4f;
	private static final int BEAM_HEIGHT = 320;
	/** Ближе этого к цели луч не рисуется (цель и так видно). */
	public static final double BEAM_HIDE_NEAR = 25;
	/** Ближе этого к цели, когда охотник уже в бою, — метка на экране пропадает. */
	public static final double MARKER_HIDE_NEAR = 25;
	/** Сколько тиков без вестей с сервера — луч гаснет сам (на случай потерянного пакета). */
	private static final int STALE = 60;
	private static final net.minecraft.resources.ResourceLocation MARKER =
			com.tarkrakov.tearsofink.TearsOfInk.id("textures/gui/hunt_marker.png");

	private static final class State {
		int prey = -1, prey2 = -1, color, age;
		boolean engaged;
		Vec3 target = Vec3.ZERO, shown = Vec3.ZERO;
	}

	private static final Map<Integer, State> HUNTS = new LinkedHashMap<>();

	private HuntClient() {}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(Hunt.Beam.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
			if (!payload.on()) {
				HUNTS.remove(payload.hunt());
				return;
			}
			State s = HUNTS.get(payload.hunt());
			Vec3 at = new Vec3(payload.x(), payload.y(), payload.z());
			if (s == null) {
				s = new State();
				s.shown = at;
				HUNTS.put(payload.hunt(), s);
			}
			s.prey = payload.entity();
			s.prey2 = payload.entity2();
			s.color = payload.color();
			s.engaged = payload.engaged();
			s.target = at;
			s.age = 0;
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> HUNTS.clear());
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (HUNTS.isEmpty()) return;
			HUNTS.values().removeIf(s -> ++s.age > STALE);
			for (State s : HUNTS.values()) {
				var e = mc.level == null || s.prey < 0 ? null : mc.level.getEntity(s.prey);
				if (e != null) s.target = e.position();
				s.shown = s.shown.lerp(s.target, 0.35);
			}
		});
		WorldRenderEvents.AFTER_TRANSLUCENT.register(HuntClient::render);
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((g, delta) -> markers(g));
	}

	public static boolean isPrey(int entityId) {
		if (HUNTS.isEmpty() || entityId < 0) return false;
		for (State s : HUNTS.values()) if (entityId == s.prey || entityId == s.prey2) return true;
		return false;
	}

	private static void render(WorldRenderContext ctx) {
		if (HUNTS.isEmpty() || ctx.matrixStack() == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Vec3 cam = ctx.camera().getPosition();
		PoseStack pose = ctx.matrixStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
		RenderSystem.setShaderFogStart(1.0e6f); // луч не тонет в тумане
		RenderSystem.setShaderFogEnd(2.0e6f);
		boolean any = false;
		for (State s : HUNTS.values()) {
			if (Math.hypot(s.shown.x - cam.x, s.shown.z - cam.z) < BEAM_HIDE_NEAR) continue; // вблизи луч не нужен
			pose.pushPose();
			pose.translate(s.shown.x - cam.x - 0.5, s.shown.y - cam.y, s.shown.z - cam.z - 0.5);
			BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, ctx.tickCounter().getGameTimeDeltaPartialTick(false),
					1.0f, mc.level.getGameTime(), 0, BEAM_HEIGHT, 0xFF000000 | s.color, 0.25f, 0.32f);
			pose.popPose();
			any = true;
		}
		if (any) buffers.endBatch();
		RenderSystem.setShaderFogStart(fogStart);
		RenderSystem.setShaderFogEnd(fogEnd);
	}

	/** Метки на экране: значок над горизонтом в сторону цели и расстояние. */
	private static void markers(net.minecraft.client.gui.GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (HUNTS.isEmpty() || mc.player == null || mc.options.hideGui) return;
		int row = 0;
		for (State s : HUNTS.values()) {
			if (marker(g, mc, s, 22 + row * 26)) row++;
		}
	}

	private static boolean marker(net.minecraft.client.gui.GuiGraphics g, Minecraft mc, State s, int y) {
		Vec3 eye = mc.player.getEyePosition();
		double dx = s.shown.x - eye.x, dz = s.shown.z - eye.z;
		double dist = Math.hypot(dx, dz);
		if (s.engaged && dist < MARKER_HIDE_NEAR) return false; // дошёл и бой идёт — метка не нужна
		int w = g.guiWidth(), h = g.guiHeight();
		float yawTo = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float diff = net.minecraft.util.Mth.wrapDegrees(yawTo - mc.player.getYRot());
		double vfov = Math.toRadians(mc.options.fov().get());
		float half = (float) Math.toDegrees(Math.atan(Math.tan(vfov / 2) * w / (double) h));
		int x = w / 2 + Math.round(diff / half * (w / 2f));
		boolean off = x < 14 || x > w - 14;
		x = net.minecraft.util.Mth.clamp(x, 14, w - 14);
		float r = ((s.color >> 16) & 255) / 255f, gr = ((s.color >> 8) & 255) / 255f, b = (s.color & 255) / 255f;
		RenderSystem.enableBlend();
		g.setColor(Math.max(r, 0.35f), Math.max(gr, 0.35f), Math.max(b, 0.35f), off ? 0.6f : 1f);
		g.blit(MARKER, x - 8, y - 8, 0, 0, 16, 16, 16, 16);
		g.setColor(1, 1, 1, 1);
		String text = net.minecraft.network.chat.Component.translatable("hunt.tears-of-ink.marker", (int) dist).getString();
		g.drawCenteredString(mc.font, text, x, y + 10, 0xFFE8D8B0);
		if (off) g.drawCenteredString(mc.font, diff < 0 ? "◀" : "▶", x + (diff < 0 ? 12 : -12), y - 4, 0xFFE8D8B0);
		return true;
	}
}
