package com.tarkrakov.tearsofink.client.fog;

import com.tarkrakov.tearsofink.block.lantern.Lanterns;
import com.tarkrakov.tearsofink.item.HandLanternItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ясность от фонарей: насколько рассеяна мгла у камеры и где «пузыри» чистого воздуха.
 * Пересчитывается несколько раз в секунду вместе с туманом.
 */
public final class LanternClarity {
	// ===== Настройки — крутить здесь =====
	/** Видимость (в блоках) в самом сердце круга мирового фонаря. */
	public static final float CLEAR_FOG_END = 32f;
	/** Ручной фонарь в хотбаре (не в левой руке) работает слабее во столько раз. */
	public static final float HOTBAR_FACTOR = 0.8f;

	/** Пузыри ясности: {x, y, z, радиус}. */
	private static final List<double[]> BUBBLES = new ArrayList<>();
	/** 0 — фонарей рядом нет, 1 — камера в круге полной ясности. */
	private static float worldClarity = 0f;
	private static float handView = 0f;

	private LanternClarity() {}

	public static void sample(ClientLevel level, Vec3 camera) {
		BUBBLES.clear();
		float[] best = {0f};
		int reach = Lanterns.MAX_CLEAR_RADIUS + Lanterns.CLEAR_FADE;
		Lanterns.forEachLit(level, camera.x, camera.z, reach, (pos, state, lantern) -> {
			double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
			BUBBLES.add(new double[]{cx, cy, cz, lantern.clearRadius()});
			double dx = camera.x - cx, dy = (camera.y - cy) * 0.6, dz = camera.z - cz;
			double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
			float c = d <= lantern.clearRadius() ? 1f : (float) Math.max(0, 1 - (d - lantern.clearRadius()) / Lanterns.CLEAR_FADE);
			if (c > best[0]) best[0] = c;
		});
		worldClarity = best[0];
		handView = handLanternView(Minecraft.getInstance().player);
	}

	/** Видимость от ручного фонаря (0 — фонаря нет). */
	private static float handLanternView(LocalPlayer player) {
		if (player == null) return 0f;
		if (player.getOffhandItem().getItem() instanceof HandLanternItem lantern) return lantern.viewDistance();
		float best = 0f;
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.getItem() instanceof HandLanternItem lantern) best = Math.max(best, lantern.viewDistance() * HOTBAR_FACTOR);
		}
		return best;
	}

	/** Какой должна быть видимость с учётом фонарей (0 — фонари не влияют). */
	public static float targetFogEnd(float zoneFogEnd) {
		float world = worldClarity > 0f ? zoneFogEnd + (CLEAR_FOG_END - zoneFogEnd) * worldClarity : 0f;
		return Math.max(world, handView);
	}

	/** Находится ли точка внутри круга ясности (с запасом margin). */
	public static boolean inBubble(double x, double y, double z, double margin) {
		for (double[] b : BUBBLES) {
			double dx = x - b[0], dy = (y - b[1]) * 0.6, dz = z - b[2];
			double r = b[3] + margin;
			if (dx * dx + dy * dy + dz * dz <= r * r) return true;
		}
		return false;
	}
}
