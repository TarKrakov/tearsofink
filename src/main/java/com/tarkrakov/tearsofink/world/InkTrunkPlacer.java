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
 * Ствол чернильного дерева: кривой основной ствол, от верхней половины которого
 * отходят 2–5 длинных ветвей, тянущихся вверх. Листва — клочьями на концах ветвей и на макушке.
 */
public class InkTrunkPlacer extends TrunkPlacer {
	public static final MapCodec<InkTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> trunkPlacerParts(instance).apply(instance, InkTrunkPlacer::new));

	// ===== Настройки формы =====
	/** Шанс, что на очередном блоке ствол сместится вбок. */
	private static final float CROOK_CHANCE = 0.28f;
	/** Сколько ветвей: от MIN до MAX. */
	private static final int MIN_BRANCHES = 2, MAX_BRANCHES = 5;
	/** Длина ветви в блоках: от MIN до MAX. */
	private static final int MIN_BRANCH_LENGTH = 3, MAX_BRANCH_LENGTH = 6;
	/** Шанс, что ветвь на очередном шаге поднимется на блок вверх. */
	private static final float BRANCH_RISE_CHANCE = 0.55f;

	public InkTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
		super(baseHeight, heightRandA, heightRandB);
	}

	@Override
	protected TrunkPlacerType<?> type() {
		return ModTreeTypes.INK_TRUNK_PLACER;
	}

	@Override
	public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level,
															BiConsumer<BlockPos, BlockState> blockSetter,
															RandomSource random, int height, BlockPos origin,
															TreeConfiguration config) {
		setDirtAt(level, blockSetter, random, origin.below(), config);
		List<FoliagePlacer.FoliageAttachment> foliage = new ArrayList<>();
		List<BlockPos> trunk = new ArrayList<>();

		// --- Кривой основной ствол ---
		Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		BlockPos.MutableBlockPos cursor = origin.mutable();
		for (int i = 0; i < height; i++) {
			placeLog(level, blockSetter, random, cursor, config, Direction.Axis.Y);
			trunk.add(cursor.immutable());
			if (i >= 2 && i < height - 1 && random.nextFloat() < CROOK_CHANCE) {
				// Чаще кренится в одну сторону, иногда — вбок
				Direction step = random.nextInt(3) == 0
						? Direction.Plane.HORIZONTAL.getRandomDirection(random) : lean;
				cursor.move(step);
				placeLog(level, blockSetter, random, cursor, config, step.getAxis());
			}
			cursor.move(Direction.UP);
		}
		foliage.add(new FoliagePlacer.FoliageAttachment(cursor.immutable(), 0, false));

		// --- Длинные ветви от верхней половины ствола ---
		int branches = MIN_BRANCHES + random.nextInt(MAX_BRANCHES - MIN_BRANCHES + 1);
		List<Direction> used = new ArrayList<>();
		for (int b = 0; b < branches; b++) {
			int startIndex = trunk.size() / 2 + random.nextInt(Math.max(1, trunk.size() / 2 - 1));
			BlockPos start = trunk.get(Math.min(startIndex, trunk.size() - 2));

			Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			if (used.size() < 4) {
				for (int tries = 0; tries < 6 && used.contains(dir); tries++) {
					dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
				}
			}
			used.add(dir);

			int length = MIN_BRANCH_LENGTH + random.nextInt(MAX_BRANCH_LENGTH - MIN_BRANCH_LENGTH + 1);
			BlockPos.MutableBlockPos branch = start.mutable();
			for (int s = 0; s < length; s++) {
				branch.move(dir);
				placeLog(level, blockSetter, random, branch, config, dir.getAxis());
				if (random.nextFloat() < BRANCH_RISE_CHANCE) {
					branch.move(Direction.UP);
					placeLog(level, blockSetter, random, branch, config, Direction.Axis.Y);
				}
				// Изредка ветвь чуть виляет в сторону
				if (random.nextInt(4) == 0) {
					branch.move(random.nextBoolean() ? dir.getClockWise() : dir.getCounterClockWise());
					placeLog(level, blockSetter, random, branch, config, dir.getClockWise().getAxis());
				}
			}
			foliage.add(new FoliagePlacer.FoliageAttachment(branch.above().immutable(), 0, false));
		}
		return foliage;
	}

	private void placeLog(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
						  RandomSource random, BlockPos pos, TreeConfiguration config, Direction.Axis axis) {
		this.placeLog(level, blockSetter, random, pos, config,
				state -> state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state);
	}
}
