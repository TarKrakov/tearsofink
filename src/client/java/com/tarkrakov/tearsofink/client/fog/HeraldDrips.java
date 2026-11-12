package com.tarkrakov.tearsofink.client.fog;

import com.tarkrakov.tearsofink.block.DrippingLeavesBlock;
import com.tarkrakov.tearsofink.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Кровь с кроны Багряного Вестника. Обычные капли видны только у листвы рядом с игроком,
 * а крона Вестника высоко — поэтому для него капли бросаем отдельно, по всей кроне.
 */
public final class HeraldDrips {
	/** Сколько попыток найти каплю за тик (на каждого Вестника рядом). */
	public static final int ATTEMPTS_PER_TICK = 60;
	/** Насколько далеко от ствола искать листву (по горизонтали и вверх). */
	public static final int SPREAD = 60, HEIGHT = 140;
	/** Вестники дальше этого от игрока не капают. */
	public static final int RANGE = 96;

	private HeraldDrips() {}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null || mc.player == null || mc.isPaused() || LanternClarity.HERALDS.isEmpty()) return;
		RandomSource random = level.random;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (BlockPos heart : LanternClarity.HERALDS) {
			if (heart.distSqr(mc.player.blockPosition()) > (double) RANGE * RANGE) continue;
			for (int i = 0; i < ATTEMPTS_PER_TICK; i++) {
				pos.set(heart.getX() + random.nextInt(SPREAD * 2 + 1) - SPREAD,
						heart.getY() + random.nextInt(HEIGHT),
						heart.getZ() + random.nextInt(SPREAD * 2 + 1) - SPREAD);
				BlockState state = level.getBlockState(pos);
				if (!(state.getBlock() instanceof DrippingLeavesBlock)) continue;
				BlockPos below = pos.below();
				BlockState under = level.getBlockState(below);
				if (under.canOcclude() && under.isFaceSturdy(level, below, Direction.UP)) continue;
				level.addAlwaysVisibleParticle(ModParticles.BLOOD_DRIP_HANG, true,
						pos.getX() + 0.1 + random.nextDouble() * 0.8, pos.getY() - 0.05,
						pos.getZ() + 0.1 + random.nextDouble() * 0.8, 0, 0, 0);
			}
		}
	}
}
