package com.tarkrakov.tearsofink.world.herald;

import com.mojang.serialization.MapCodec;
import com.tarkrakov.tearsofink.world.InkLandsAccess;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Вестник появляется ровно в центре каждого пятна Кроволесья, на дне низины. */
public class BloodHeraldStructure extends Structure {
	public static final MapCodec<BloodHeraldStructure> CODEC = simpleCodec(BloodHeraldStructure::new);

	public BloodHeraldStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkGenerator generator = context.chunkGenerator();
		if (!(generator.getBiomeSource() instanceof InkLandsAccess access)) return Optional.empty();
		InkLandsLayout layout = access.tearsofink$layout(context.randomState().sampler());
		if (layout == null) return Optional.empty();
		ChunkPos chunk = context.chunkPos();
		long[] blob = layout.nearestBloodBlob(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
		if (blob == null) return Optional.empty();
		int bx = (int) blob[0], bz = (int) blob[1];
		if ((bx >> 4) != chunk.x || (bz >> 4) != chunk.z) return Optional.empty();

		// дно низины (как его выкопает генерация)
		java.util.function.IntBinaryOperator base = (x, z) ->
				generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - 1;
		double floor = layout.valleyHeight(bx, bz, base.applyAsInt(bx, bz), base);
		int y = Mth.floor(floor) + 1;

		BloodHerald.Layout herald = BloodHerald.layout(context.structureTemplateManager());
		BlockPos corner = new BlockPos(bx - herald.trunk().getX(), y, bz - herald.trunk().getZ());
		BlockPos center = new BlockPos(bx, y, bz);
		return Optional.of(new GenerationStub(center, builder ->
				builder.addPiece(new BloodHeraldPiece(context.structureTemplateManager(), corner))));
	}

	@Override
	public StructureType<?> type() {
		return BloodHerald.STRUCTURE_TYPE;
	}
}
