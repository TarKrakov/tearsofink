package com.tarkrakov.tearsofink.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Смоляное дерево: высокий прямой ствол с короткими сучьями (1–2 блока) в разных местах,
 * на сучьях — маленькие пучки листвы, наверху — одна плоская плотная крона.
 */
public class InkTrunkPlacer extends TrunkPlacer {
	public static final MapCodec<InkTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> trunkPlacerParts(instance).apply(instance, InkTrunkPlacer::new));

	// ===== Настройки формы =====
	/** Сучья появляются только в этой части ствола (доли высоты). */
	private static final float STUB_FROM = 0.3f, STUB_TO = 0.85f;
	/** Шанс сучка на каждом блоке ствола в этой части. */
	private static final float STUB_CHANCE = 0.3f;
	/** Насколько пучки на сучьях меньше верхней кроны (сдвиг радиуса). */
	private static final int TUFT_RADIUS_OFFSET = -1;
	/** Насколько верхняя крона шире базового радиуса. */
	private static final int CROWN_RADIUS_OFFSET = 1;

	public InkTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
		super(baseHeight, heightRandA, heightRandB);
	}

	@Override
	protected TrunkPlacerType<?> type() {
		return ModTreeTypes.INK_TRUNK_PLACER;
	}

	@Override
	public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter,
															RandomSource random, int height, BlockPos origin, TreeConfiguration config) {
		setDirtAt(level, setter, random, origin.below(), config);
		List<FoliagePlacer.FoliageAttachment> foliage = new ArrayList<>();

		for (int i = 0; i < height; i++) {
			log(level, setter, random, origin.above(i), config, Direction.Axis.Y);
		}
		foliage.add(new FoliagePlacer.FoliageAttachment(origin.above(height), CROWN_RADIUS_OFFSET, false));

		Direction last = null;
		for (int i = (int) (height * STUB_FROM); i < (int) (height * STUB_TO); i++) {
			if (random.nextFloat() >= STUB_CHANCE) continue;
			Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			if (dir == last) dir = dir.getClockWise();
			last = dir;
			int length = 1 + random.nextInt(2);
			BlockPos.MutableBlockPos p = origin.above(i).mutable();
			for (int s = 0; s < length; s++) {
				p.move(dir);
				log(level, setter, random, p, config, dir.getAxis());
			}
			foliage.add(new FoliagePlacer.FoliageAttachment(p.above().immutable(), TUFT_RADIUS_OFFSET, false));
			i++; // не ставим сучья вплотную друг к другу
		}
		return foliage;
	}

	private void log(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter, RandomSource random,
					 BlockPos pos, TreeConfiguration config, Direction.Axis axis) {
		this.placeLog(level, setter, random, pos, config,
				s -> s.hasProperty(RotatedPillarBlock.AXIS) ? s.setValue(RotatedPillarBlock.AXIS, axis) : s);
	}
}
