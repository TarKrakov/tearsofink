package com.tarkrakov.tearsofink.world.adit;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

final class AditPiece extends StructurePiece {
	private static final int[] LATERAL_OFFSETS = {0, -1, 1, -2, 2};
	private final int sector;
	private final int band;
	private final int surface;
	private boolean pending;
	private BlockPos entrance;
	private BlockPos landing;
	private int variant;
	private int sideShift;
	private int entryLane;

	AditPiece(ChunkPos chunk, AditSites.Site site) {
		super(AditStructure.PIECE, 0, new BoundingBox(chunk.getMinBlockX() + 1, AditSites.BOTTOM_Y - 5,
				chunk.getMinBlockZ() + 1, chunk.getMaxBlockX() - 1, site.surface() + AditSites.SURFACE_TOLERANCE + 4,
				chunk.getMaxBlockZ() - 1));
		this.sector = site.sector();
		this.band = site.band();
		this.surface = site.surface();
	}

	AditPiece(CompoundTag tag) {
		super(AditStructure.PIECE, tag);
		sector = tag.getInt("Sector");
		band = tag.getInt("Band");
		surface = tag.getInt("Surface");
		pending = tag.getBoolean("Pending");
		if (tag.contains("Entrance")) entrance = BlockPos.of(tag.getLong("Entrance"));
		if (tag.contains("Landing")) landing = BlockPos.of(tag.getLong("Landing"));
		variant = tag.getInt("Variant");
		sideShift = tag.getInt("SideShift");
		entryLane = tag.getInt("EntryLane");
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("Sector", sector);
		tag.putInt("Band", band);
		tag.putInt("Surface", surface);
		if (pending) tag.putBoolean("Pending", true);
		if (entrance != null) {
			tag.putLong("Entrance", entrance.asLong());
			tag.putLong("Landing", landing.asLong());
			tag.putInt("Variant", variant);
			tag.putInt("SideShift", sideShift);
			tag.putInt("EntryLane", entryLane);
		}
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
							BoundingBox writable, ChunkPos chunk, BlockPos pivot) {
		if (!ModDimensions.isLands(level.getLevel()) || entrance != null) return;
		if (structures.startsForStructure(chunk, structure -> !(structure instanceof AditStructure)).stream()
				.anyMatch(start -> AditSites.overlaps(chunk, start.getBoundingBox()))) {
			skip(level.getSeed(), chunk, "existing structure or foundation");
			return;
		}

		pending = true;
	}

	/** Run once both the owning chunk and its neighbors have completed decoration. */
	void finish(ProtoChunk blocks, long seed) {
		if (!pending || entrance != null) return;
		pending = false;
		blocks.setUnsaved(true);
		ChunkPos chunk = blocks.getPos();
		String failure = "no dry, walkable approach";
		for (int shift : LATERAL_OFFSETS) {
			for (int turn = 0; turn < 8; turn++) {
				for (int lane = 0; lane < 2; lane++) {
					AditTunnel tunnel = AditTunnel.at(chunk, surface, turn, shift, lane, blocks);
					if (tunnel == null) continue;
					String reason = tunnel.check(blocks, getBoundingBox());
					if (reason != null) {
						failure = reason;
						continue;
					}
					tunnel.place(blocks);
					entrance = tunnel.entrance;
					landing = tunnel.landing;
					variant = tunnel.variant;
					sideShift = tunnel.sideShift;
					entryLane = tunnel.entryLane;
					TearsOfInk.LOGGER.info("[adit] seed={} sector={} band={} entrance={} landing={}",
							seed, sector + 1, band + 1, entrance.toShortString(), landing.toShortString());
					return;
				}
			}
		}
		skip(seed, chunk, failure);
	}

	boolean nearPassage(BlockPos pos) {
		return entrance != null && (entrance.distSqr(pos) <= 64.0
				|| (landing != null && landing.distSqr(pos) <= 64.0));
	}

	private void skip(long seed, ChunkPos chunk, String reason) {
		TearsOfInk.LOGGER.warn("[adit] seed={} sector={} band={} chunk={} skipped: {}",
				seed, sector + 1, band + 1, chunk, reason);
	}
}
