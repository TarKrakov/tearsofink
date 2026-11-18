package com.tarkrakov.tearsofink.world.adit;

import com.mojang.serialization.MapCodec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.InkLandsAccess;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class AditStructure extends Structure {
	public static final MapCodec<AditStructure> CODEC = simpleCodec(AditStructure::new);
	private static final StructureType<AditStructure> TYPE = Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE, TearsOfInk.id("pale_adit"), () -> CODEC);
	static final StructurePieceType PIECE = Registry.register(
			BuiltInRegistries.STRUCTURE_PIECE, TearsOfInk.id("pale_adit_v1"), (StructurePieceType.ContextlessType) AditPiece::new);

	private final Map<RandomState, Map<Long, AditSites.Site>> sites = new WeakHashMap<>();

	public AditStructure(StructureSettings settings) {
		super(settings);
	}

	public static void init() {}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (!(context.biomeSource() instanceof InkLandsAccess access)) return Optional.empty();
		var chunk = context.chunkPos();
		if (!AditSites.withinSearchArea(chunk)) return Optional.empty();
		AditSites.Site site;
		synchronized (sites) {
			site = sites.computeIfAbsent(context.randomState(),
					state -> AditSites.plan(access.tearsofink$layout(state.sampler()), context)).get(chunk.toLong());
		}
		if (site == null) return Optional.empty();
		BlockPos center = chunk.getMiddleBlockPosition(site.surface());
		return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new AditPiece(chunk, site))));
	}

	/** The vanilla LIGHT step depends on INITIALIZE_LIGHT in a one-chunk neighborhood. */
	public static void finishDecoration(ServerLevel level, ChunkAccess chunk) {
		if (!ModDimensions.isLands(level) || !(chunk instanceof ProtoChunk proto) || chunk instanceof ImposterProtoChunk) return;
		var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("pale_adit"));
		if (structure == null) return;
		var start = proto.getStartForStructure(structure);
		if (start == null || !start.isValid()) return;
		for (var piece : start.getPieces()) {
			if (piece instanceof AditPiece adit) adit.finish(proto, level.getSeed());
		}
	}

	/** Only already generated, loaded passages with a real entrance/landing. */
	public static boolean nearBuiltPassage(ServerLevel level, BlockPos pos) {
		var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("pale_adit"));
		if (structure == null) return false;
		int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
			var chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
			if (chunk == null) continue;
			var start = chunk.getStartForStructure(structure);
			if (start == null || !start.isValid()) continue;
			for (var piece : start.getPieces()) {
				if (piece instanceof AditPiece adit && adit.nearPassage(pos)) return true;
			}
		}
		return false;
	}

	@Override
	public StructureType<?> type() {
		return TYPE;
	}
}
