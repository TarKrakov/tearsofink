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

	// ===== Багряный Вестник: светокамень в кроне разгоняет мглу (не до конца) =====
	/** Видимость у самого Вестника — силуэт дерева проступает сквозь мглу. */
	public static final float HERALD_FOG_END = 56f;
	/** В этом радиусе (по горизонтали) от ствола — полная сила, дальше плавно спадает за HERALD_FADE блоков. */
	public static final int HERALD_RADIUS = 45, HERALD_FADE = 45;

	/** Сердца Вестников рядом (для капель крови). */
	public static final List<net.minecraft.core.BlockPos> HERALDS = new ArrayList<>();
	private static float heraldClarity = 0f;

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
        for (var morok : level.getEntitiesOfClass(com.tarkrakov.tearsofink.entity.MorokEntity.class,
                new net.minecraft.world.phys.AABB(camera,camera).inflate(12), e -> e.isAlive())) {
            double r = com.tarkrakov.tearsofink.entity.MorokTuning.LIGHT_RADIUS;
            BUBBLES.add(new double[]{morok.getX(),morok.getY()+.32,morok.getZ(),r});
            double d = camera.distanceTo(morok.position().add(0,.32,0));
            float c = (float)Math.max(0,1-Math.max(0,d-r)/com.tarkrakov.tearsofink.entity.MorokTuning.LIGHT_FADE);
            best[0] = Math.max(best[0],c*.6f);
        }
		worldClarity = best[0];
		handView = handLanternView(Minecraft.getInstance().player);

		// Вестники: ищем «сердца» в чанках вокруг
		HERALDS.clear();
		float herald = 0f;
		int reach2 = HERALD_RADIUS + HERALD_FADE + 16;
		int cx0 = ((int) Math.floor(camera.x) - reach2) >> 4, cx1 = ((int) Math.floor(camera.x) + reach2) >> 4;
		int cz0 = ((int) Math.floor(camera.z) - reach2) >> 4, cz1 = ((int) Math.floor(camera.z) + reach2) >> 4;
		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cz = cz0; cz <= cz1; cz++) {
				var chunk = level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
				if (chunk == null) continue;
				for (var pos : chunk.getBlockEntitiesPos()) {
					if (!(chunk.getBlockEntity(pos) instanceof com.tarkrakov.tearsofink.world.herald.HeraldHeartBlockEntity)) continue;
					HERALDS.add(pos.immutable());
					double dx = camera.x - (pos.getX() + 0.5), dz = camera.z - (pos.getZ() + 0.5);
					double d = Math.sqrt(dx * dx + dz * dz);
					float c = d <= HERALD_RADIUS ? 1f : (float) Math.max(0, 1 - (d - HERALD_RADIUS) / HERALD_FADE);
					herald = Math.max(herald, c);
				}
			}
		}
		heraldClarity = herald;
	}

	/** Видимость от ручного фонаря (0 — фонаря нет). */
	private static float handLanternView(LocalPlayer player) {
		if (player == null) return 0f;
		if (player.getOffhandItem().getItem() instanceof HandLanternItem lantern) return lantern.viewDistance();
		float best = com.tarkrakov.tearsofink.client.vow.AuraClient.clarity(player); // внутренний свет Пути Души / два щита / свет союзника рядом
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.getItem() instanceof HandLanternItem lantern) best = Math.max(best, lantern.viewDistance() * HOTBAR_FACTOR);
		}
		return best;
	}

	/** Какой должна быть видимость с учётом фонарей (0 — фонари не влияют). */
	public static float targetFogEnd(float zoneFogEnd) {
		float world = worldClarity > 0f ? zoneFogEnd + (CLEAR_FOG_END - zoneFogEnd) * worldClarity : 0f;
		float herald = heraldClarity > 0f ? zoneFogEnd + (HERALD_FOG_END - zoneFogEnd) * heraldClarity : 0f;
		return Math.max(Math.max(world, handView), herald);
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
