package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.entity.CaterpillarEntity;
import com.tarkrakov.tearsofink.entity.EntityTuning;
import com.tarkrakov.tearsofink.entity.ModMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Гарантированные кровавые гусеницы в каждом пятне Кроволесья — расселяются при создании мира,
 * вместе с низиной. Чанк в самом центре пятна получает группу всегда, остальные чанки дна — с шансом.
 * Обычное (ванильное) появление существ работает поверх этого.
 */
final class BloodFauna {
	private BloodFauna() {}

	static void populate(WorldGenLevel level, InkLandsLayout layout, int minX, int minZ, RandomSource random) {
		int cx = minX + 8, cz = minZ + 8;
		long[] blob = layout.nearestBloodBlob(cx, cz);
		if (blob == null) return;
		double dx = cx - blob[0], dz = cz - blob[1];
		double dist = Math.sqrt(dx * dx + dz * dz);
		// только дно, с запасом от неровного края — чтобы точно попасть в Кроволесье
		if (dist > blob[2] - 12) return;

		// у ствола Вестника (корни) не селим — их бы замуровало деревом
		if (dist < 36) return;
		// «центральная» группа — в чанке чуть в стороне от ствола
		boolean centre = Math.abs(blob[0] + 44 - cx) <= 8 && Math.abs(blob[1] - cz) <= 8;
		int count;
		if (centre) {
			count = EntityTuning.BLOOD_CENTRE_GROUP;
		} else if (random.nextInt(100) < EntityTuning.BLOOD_CHUNK_CHANCE) {
			count = 1 + random.nextInt(EntityTuning.BLOOD_CHUNK_GROUP_MAX);
		} else {
			return;
		}
		for (int i = 0; i < count; i++) {
			int x = minX + 2 + random.nextInt(12), z = minZ + 2 + random.nextInt(12);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos at = new BlockPos(x, y, z);
			if (!level.getBlockState(at.below()).getFluidState().isEmpty()) continue;
			CaterpillarEntity c = ModMobs.CATERPILLAR.create(level.getLevel());
			if (c == null) continue;
			c.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0f);
			c.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.CHUNK_GENERATION, null);
			if (c.variant() != CaterpillarEntity.Variant.BLOOD) c.setVariant(CaterpillarEntity.Variant.BLOOD);
			level.addFreshEntityWithPassengers(c);
		}
	}
}
