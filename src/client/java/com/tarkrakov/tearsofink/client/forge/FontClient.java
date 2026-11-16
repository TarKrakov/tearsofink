package com.tarkrakov.tearsofink.client.forge;

import com.tarkrakov.tearsofink.weapon.forge.AscensionFont;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Map;

/**
 * Клиент Купели: вода в живой Купели чёрная (Путь Чернил) или белая (Путь Души). Подменяем только цвет воды
 * в 9 клетках вокруг центра, остальная вода — как была.
 */
public final class FontClient {
	// ===== Цвет воды (RGB) — крутить здесь =====
	public static final int INK_WATER = 0x140A1C, SOUL_WATER = 0xF6F2EA;

	private static final Map<BlockPos, Integer> ACTIVE = new HashMap<>();
	private static final Map<BlockPos, Long> SEEN = new HashMap<>();

	private FontClient() {}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(AscensionFont.Sync.TYPE, (payload, ctx) -> ctx.client().execute(() -> set(payload.pos(), payload.color())));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.level == null) {
				if (!ACTIVE.isEmpty()) {
					ACTIVE.clear();
					SEEN.clear();
				}
				return;
			}
			long now = mc.level.getGameTime();
			// не пришло подтверждение 5 с (ушёл из Земель, сервер забыл) — вернуть обычную воду
			for (BlockPos p : Map.copyOf(SEEN).keySet()) if (now - SEEN.get(p) > 100) set(p, 0);
		});
		AscensionFont.clientInFont = player -> {
			BlockPos f = player.blockPosition();
			for (BlockPos c : ACTIVE.keySet()) if (Math.abs(c.getX() - f.getX()) <= 1 && c.getY() == f.getY() && Math.abs(c.getZ() - f.getZ()) <= 1) return true;
			return false;
		};
		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
			FluidRenderHandler base = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER);
			if (base == null) return;
			FluidRenderHandler tinted = new FluidRenderHandler() {
				@Override
				public TextureAtlasSprite[] getFluidSprites(BlockAndTintGetter view, BlockPos pos, FluidState state) {
					return base.getFluidSprites(view, pos, state);
				}

				@Override
				public int getFluidColor(BlockAndTintGetter view, BlockPos pos, FluidState state) {
					Integer c = pos == null ? null : colorAt(pos);
					return c != null ? c : base.getFluidColor(view, pos, state);
				}

				@Override
				public void reloadTextures(TextureAtlas atlas) {
					base.reloadTextures(atlas);
				}
			};
			FluidRenderHandlerRegistry.INSTANCE.register(Fluids.WATER, Fluids.FLOWING_WATER, tinted);
		});
	}

	private static Integer colorAt(BlockPos pos) {
		if (ACTIVE.isEmpty()) return null;
		for (var e : ACTIVE.entrySet()) {
			BlockPos c = e.getKey();
			if (Math.abs(c.getX() - pos.getX()) <= 1 && c.getY() == pos.getY() && Math.abs(c.getZ() - pos.getZ()) <= 1)
				return e.getValue() == AscensionFont.INK ? INK_WATER : SOUL_WATER;
		}
		return null;
	}

	private static void set(BlockPos pos, int color) {
		Minecraft mc = Minecraft.getInstance();
		Integer old = ACTIVE.get(pos);
		if (color == 0) {
			ACTIVE.remove(pos);
			SEEN.remove(pos);
		} else {
			ACTIVE.put(pos, color);
			if (mc.level != null) SEEN.put(pos, mc.level.getGameTime());
		}
		Integer now = color == 0 ? null : color;
		if (!java.util.Objects.equals(old, now) && mc.levelRenderer != null) {
			mc.levelRenderer.setBlocksDirty(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
		}
	}
}
