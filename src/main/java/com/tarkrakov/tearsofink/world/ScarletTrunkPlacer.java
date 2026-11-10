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
 * Багряное дерево: низкий ствол (3–5 до кроны), продолжающийся внутри кроны,
 * и 4–6 коротких ветвей во все стороны — широкая неровная раскидистая крона из округлых клубов.
 */
public class ScarletTrunkPlacer extends TrunkPlacer {
	public static final MapCodec<ScarletTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> trunkPlacerParts(instance).apply(instance, ScarletTrunkPlacer::new));

	// ===== Настройки формы =====
	private static final int MIN_BRANCHES = 4, MAX_BRANCHES = 6;
	private static final int MIN_BRANCH_LENGTH = 2, MAX_BRANCH_LENGTH = 4;
	/** Шанс, что ветвь на шаге поднимется вверх. */
	private static final float BRANCH_RISE_CHANCE = 0.35f;
	/** Сколько ствол продолжается внутри кроны. */
	private static final int CROWN_TRUNK_MIN = 1, CROWN_TRUNK_MAX = 2;

	private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

	public ScarletTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
		super(baseHeight, heightRandA, heightRandB);
	}

	@Override
	protected TrunkPlacerType<?> type() {
		return ModTreeTypes.SCARLET_TRUNK_PLACER;
	}

	@Override
	public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter,
															RandomSource random, int height, BlockPos origin, TreeConfiguration config) {
		setDirtAt(level, setter, random, origin.below(), config);
		List<FoliagePlacer.FoliageAttachment> foliage = new ArrayList<>();

		// Низкий ствол + продолжение внутри кроны
		int crownTrunk = CROWN_TRUNK_MIN + random.nextInt(CROWN_TRUNK_MAX - CROWN_TRUNK_MIN + 1);
		for (int i = 0; i < height + crownTrunk; i++) {
			log(level, setter, random, origin.above(i), config, Direction.Axis.Y);
		}
		BlockPos top = origin.above(height + crownTrunk);
		foliage.add(new FoliagePlacer.FoliageAttachment(top, 1, false));

		// Ветви расходятся от точки разветвления
		BlockPos fork = origin.above(height - 1);
		int branches = MIN_BRANCHES + random.nextInt(MAX_BRANCHES - MIN_BRANCHES + 1);
		int startDir = random.nextInt(DIRECTIONS.length);
		for (int b = 0; b < branches; b++) {
			int[] dir = DIRECTIONS[(startDir + b * 3 + random.nextInt(2)) % DIRECTIONS.length];
			BlockPos.MutableBlockPos p = fork.mutable();
			p.move(Direction.UP, random.nextInt(crownTrunk + 1));
			int length = MIN_BRANCH_LENGTH + random.nextInt(MAX_BRANCH_LENGTH - MIN_BRANCH_LENGTH + 1);
			Direction.Axis axis = dir[0] != 0 && dir[1] == 0 ? Direction.Axis.X : (dir[0] == 0 ? Direction.Axis.Z : Direction.Axis.X);
			for (int s = 0; s < length; s++) {
				p.move(dir[0], 0, dir[1]);
				log(level, setter, random, p, config, axis);
				if (random.nextFloat() < BRANCH_RISE_CHANCE) {
					p.move(Direction.UP);
					log(level, setter, random, p, config, Direction.Axis.Y);
				}
			}
			foliage.add(new FoliagePlacer.FoliageAttachment(p.above().immutable(), random.nextInt(2), false));
		}
		return foliage;
	}

	private void log(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter, RandomSource random,
					 BlockPos pos, TreeConfiguration config, Direction.Axis axis) {
		this.placeLog(level, setter, random, pos, config,
				s -> s.hasProperty(RotatedPillarBlock.AXIS) ? s.setValue(RotatedPillarBlock.AXIS, axis) : s);
	}
}
