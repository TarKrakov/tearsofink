package com.tarkrakov.tearsofink.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Уборка после срезания земли (низины, выемки дорог). */
final class WorldgenCleanup {
	/** На сколько блоков выше среза искать оставшиеся «висячие» растения (высокая трава — 2 блока). */
	private static final int PLANT_CHECK_HEIGHT = 4;

	private WorldgenCleanup() {}

	/**
	 * Убирает растения, оставшиеся висеть над срезанной землёй. Их мог поставить соседний чанк
	 * раньше, чем здесь срезали рельеф (круг Слёзницы, трава и т.п.).
	 * @param fromY первый блок над тем местом, докуда срезали
	 */
	static void removeFloatingPlants(WorldGenLevel level, int x, int fromY, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, fromY, z);
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int i = 0; i < PLANT_CHECK_HEIGHT; i++, pos.move(0, 1, 0)) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty()) continue;
			// растения без коллизии (цветы, трава, травы) — убираем; листву, стволы и камень не трогаем
			if (state.getCollisionShape(level, pos).isEmpty()) level.setBlock(pos, air, 2);
		}
	}
}
