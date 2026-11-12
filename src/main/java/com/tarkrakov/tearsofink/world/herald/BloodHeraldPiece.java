package com.tarkrakov.tearsofink.world.herald;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.blood.BloodPumpBlock;
import com.tarkrakov.tearsofink.block.blood.BloodTankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Само дерево + Кровосборники у корней + «сердце» (маячок для тумана и капель крови). */
public class BloodHeraldPiece extends TemplateStructurePiece {
	public BloodHeraldPiece(StructureTemplateManager manager, BlockPos corner) {
		super(BloodHerald.PIECE_TYPE, 0, manager, BloodHerald.TEMPLATE, BloodHerald.TEMPLATE.toString(),
				BloodHerald.settings(), corner);
	}

	public BloodHeraldPiece(StructureTemplateManager manager, CompoundTag tag) {
		super(BloodHerald.PIECE_TYPE, tag, manager, id -> BloodHerald.settings());
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
	}

	@Override
	protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
							BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		super.postProcess(level, structures, generator, random, box, chunkPos, pivot);
		BloodHerald.Layout herald = BloodHerald.layout(level.getLevel().getStructureManager());
		BlockPos corner = this.templatePosition;

		// «сердце» — внутри ствола: по нему клиент разгоняет туман и роняет капли с кроны
		BlockPos heart = corner.offset(herald.trunk()).above(4);
		if (box.isInside(heart)) level.setBlock(heart, ModBlocks.HERALD_HEART.defaultBlockState(), 2);

		for (int i = 0; i < herald.pumps().size(); i++) {
			BlockPos pump = corner.offset(herald.pumps().get(i));
			Direction facing = herald.facings().get(i);
			BlockState lower = ModBlocks.BLOOD_PUMP.defaultBlockState().setValue(BloodPumpBlock.FACING, facing);
			set(level, box, pump, lower);
			set(level, box, pump.above(), lower.setValue(BloodPumpBlock.HALF, DoubleBlockHalf.UPPER));
			set(level, box, pump.relative(BloodPumpBlock.tankSide(facing)),
					ModBlocks.BLOOD_TANK.defaultBlockState().setValue(BloodTankBlock.FACING, facing));
		}
	}

	private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
		if (!box.isInside(pos)) return;
		level.setBlock(pos, state, 2);
		// опора под ногами
		BlockPos below = pos.below();
		if (state.is(ModBlocks.BLOOD_PUMP) && state.getValue(BloodPumpBlock.HALF) == DoubleBlockHalf.UPPER) return;
		if (level.getBlockState(below).canBeReplaced()) {
			level.setBlock(below, ModBlocks.BLOOD_SOIL.defaultBlockState(), 2);
		}
	}
}
