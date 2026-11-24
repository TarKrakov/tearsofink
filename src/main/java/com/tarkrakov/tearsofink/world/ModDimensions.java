package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import com.tarkrakov.tearsofink.world.ruins.RuinPiece;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Измерение Чернильных Земель (описано в data/tears-of-ink/dimension/ink_lands.json). */
public final class ModDimensions {
	public static final ResourceKey<Level> INK_LANDS = ResourceKey.create(Registries.DIMENSION, TearsOfInk.id("ink_lands"));

	private ModDimensions() {}

	public static void init() {
		Registry.register(BuiltInRegistries.BIOME_SOURCE, TearsOfInk.id("ink_lands"), InkLandsBiomeSource.CODEC);
		InkLandsBorder.init();
	}

	public static boolean isLands(Level level) {
		return level.dimension() == INK_LANDS;
	}

	/** Gameplay inside the Lands or their isolated Pantheon. Worldgen and travel use isLands. */
	public static boolean hasLandsRules(Level level) {
		return isLands(level) || PantheonTemple.isTemple(level);
	}

	@Nullable
	public static ServerLevel lands(MinecraftServer server) {
		return server.getLevel(INK_LANDS);
	}


	private static final int LEGACY_SEARCH = 26;
	private static final int LANDING_RADIUS = 5;
	private static final TicketType<ChunkPos> ARRIVAL_TICKET = TicketType.create(
			"tears_of_ink_arrival", java.util.Comparator.comparingLong(ChunkPos::toLong), 200);
	private static final java.util.Map<ServerLevel, BlockPos[]> TEMPLE_SPOT = new java.util.WeakHashMap<>();

	public record Arrival(Vec3 pos, float yaw) {}

	/** Diagnostic commands may request the same arrival synchronously. Gameplay uses readyArrival. */
	public static Vec3 arrival(ServerLevel lands, int road) {
		return resolveArrival(lands, true).pos();
	}

	public static double[] arrivalXZ(InkLandsLayout layout) {
		double[] s = Ruins.start(layout);
		return new double[]{s[0], s[1]};
	}

	public static void preloadArrival(ServerLevel lands) {
		InkLandsLayout layout = InkLandsLayout.of(lands);
		if (layout == null) {
			var pos = new ChunkPos(BlockPos.containing(InkLandsLayout.RADIUS * 0.9, 0, 0));
			lands.getChunkSource().addRegionTicket(ARRIVAL_TICKET, pos, 0, pos);
			return;
		}
		double[] site = Ruins.start(layout);
		BlockPos center = BlockPos.containing(site[0], 0, site[1]);
		var chunk = new ChunkPos(center);
		// Only old or edited temples need the wider search once the generated start is available.
		BlockPos fountain = startFountain(lands, center);
		boolean legacy = chunksReady(lands, center, 0) && (fountain == null
				|| chunksReady(lands, fountain, 0) && !isFountain(lands, fountain));
		lands.getChunkSource().addRegionTicket(ARRIVAL_TICKET, chunk, legacy ? 2 : 1, chunk);
	}

	public static void dropArrivalTickets(ServerLevel lands) {
		InkLandsLayout layout = InkLandsLayout.of(lands);
		double[] site = layout == null ? new double[]{InkLandsLayout.RADIUS * 0.9, 0} : Ruins.start(layout);
		var chunk = new ChunkPos(BlockPos.containing(site[0], 0, site[1]));
		for (int radius = 0; radius <= 2; radius++) lands.getChunkSource().removeRegionTicket(ARRIVAL_TICKET, chunk, radius, chunk);
	}

	@Nullable
	public static Arrival readyArrival(ServerLevel lands) {
		return resolveArrival(lands, false);
	}

	@Nullable
	private static Arrival resolveArrival(ServerLevel lands, boolean load) {
		InkLandsLayout layout = InkLandsLayout.of(lands);
		double[] site = layout != null ? Ruins.start(layout) : new double[]{0, 0, InkLandsLayout.RADIUS * 0.9, 0};
		if (layout != null) {
			BlockPos center = BlockPos.containing(site[0], 0, site[1]);
			if (!region(lands, center, 0, load)) return null;
			BlockPos[] cached = TEMPLE_SPOT.get(lands);
			if (cached != null && region(lands, cached[1], LANDING_RADIUS, load)
					&& isFountain(lands, cached[1]) && canStand(lands, cached[0])) {
				Vec3 to = Vec3.atBottomCenterOf(cached[0]);
				return new Arrival(to, arrivalYaw(lands, to));
			}
			BlockPos fountain = startFountain(lands, center);
			if (fountain != null && !region(lands, fountain, LANDING_RADIUS, load)) return null;
			if (!isFountain(lands, fountain)) {
				if (!region(lands, center, LEGACY_SEARCH, load)) return null;
				fountain = findLegacyFountain(lands, center);
				if (fountain != null && !region(lands, fountain, LANDING_RADIUS, load)) return null;
			}
			BlockPos feet = fountain == null ? null : standByFountain(lands, fountain);
			if (feet != null) {
				TEMPLE_SPOT.put(lands, new BlockPos[]{feet, fountain});
				Vec3 to = Vec3.atBottomCenterOf(feet);
				return new Arrival(to, arrivalYaw(lands, to));
			}
			TEMPLE_SPOT.remove(lands);
		}
		BlockPos ground = BlockPos.containing(site[2], 0, site[3]);
		if (!region(lands, ground, 0, load)) return null;
		int y = lands.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ground.getX(), ground.getZ());
		Vec3 to = Vec3.atBottomCenterOf(ground.atY(y));
		return new Arrival(to, faceInward(to));
	}

	private static boolean region(ServerLevel level, BlockPos center, int radius, boolean load) {
		if (!load) return chunksReady(level, center, radius);
		for (int x = (center.getX() - radius) >> 4; x <= (center.getX() + radius) >> 4; x++) {
			for (int z = (center.getZ() - radius) >> 4; z <= (center.getZ() + radius) >> 4; z++) level.getChunk(x, z);
		}
		return true;
	}

	public static boolean chunksReady(ServerLevel level, BlockPos center, int radius) {
		for (int x = (center.getX() - radius) >> 4; x <= (center.getX() + radius) >> 4; x++) {
			for (int z = (center.getZ() - radius) >> 4; z <= (center.getZ() + radius) >> 4; z++) {
				if (level.getChunkSource().getChunkNow(x, z) == null) return false;
			}
		}
		return true;
	}

	@Nullable
	private static BlockPos startFountain(ServerLevel level, BlockPos center) {
		var chunk = level.getChunkSource().getChunkNow(center.getX() >> 4, center.getZ() >> 4);
		if (chunk == null) return null;
		var temple = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("ruin_temple"));
		var start = temple == null ? null : chunk.getStartForStructure(temple);
		if (start != null && start.isValid()) {
			for (var piece : start.getPieces()) {
				if (piece instanceof RuinPiece ruin) {
					BlockPos fountain = ruin.startFountain();
					if (fountain != null) return fountain;
				}
			}
		}
		return null;
	}

	private static boolean isFountain(ServerLevel level, @Nullable BlockPos fountain) {
		return fountain != null && level.getBlockState(fountain).is(ModBlocks.INK_FOUNTAIN);
	}

	/** Compatibility with temples built before structure starts were saved or edited by players. */
	@Nullable
	private static BlockPos findLegacyFountain(ServerLevel level, BlockPos center) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = center.getX() - LEGACY_SEARCH; x <= center.getX() + LEGACY_SEARCH; x++) {
			for (int z = center.getZ() - LEGACY_SEARCH; z <= center.getZ() + LEGACY_SEARCH; z++) {
				var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
				int minY = Math.max(level.getMinBuildHeight(), top - 40);
				for (int y = Math.min(level.getMaxBuildHeight() - 1, top + 30); y >= minY;) {
					var section = chunk.getSection(chunk.getSectionIndex(y));
					int bottom = Math.max(minY, (y >> 4) << 4);
					if (!section.maybeHas(state -> state.is(ModBlocks.INK_FOUNTAIN))) {
						y = bottom - 1;
						continue;
					}
					for (; y >= bottom; y--) {
						if (chunk.getBlockState(pos.set(x, y, z)).is(ModBlocks.INK_FOUNTAIN)) return pos.immutable();
					}
				}
			}
		}
		return null;
	}

	@Nullable
	private static BlockPos standByFountain(ServerLevel lands, BlockPos fountain) {
		for (int dist = 2; dist <= 4; dist++) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				for (int dy = 0; dy >= -1; dy--) {
					BlockPos at = fountain.relative(d, dist).above(dy);
					if (canStand(lands, at)) return at;
				}
			}
		}
		return null;
	}

	private static boolean canStand(ServerLevel lands, BlockPos feet) {
		BlockPos head = feet.above(), floor = feet.below();
		var body = lands.getBlockState(feet);
		var above = lands.getBlockState(head);
		var support = lands.getBlockState(floor);
		return lands.getWorldBorder().isWithinBounds(feet) && body.getFluidState().isEmpty() && above.getFluidState().isEmpty()
				&& body.getCollisionShape(lands, feet).isEmpty() && above.getCollisionShape(lands, head).isEmpty()
				&& support.isFaceSturdy(lands, floor, Direction.UP);
	}

	public static float arrivalYaw(ServerLevel lands, Vec3 at) {
		BlockPos[] cached = TEMPLE_SPOT.get(lands);
		if (cached != null && at.distanceToSqr(Vec3.atBottomCenterOf(cached[0])) < 4) {
			return (float) Math.toDegrees(Math.atan2(-(cached[1].getX() + 0.5 - at.x), cached[1].getZ() + 0.5 - at.z));
		}
		return faceInward(at);
	}
	/** Поворот спиной к ближайшему краю (перпендикулярно от него) — чтобы новичок не шёл сразу в стену тумана. */
	public static float faceInward(Vec3 at) {
		double s = 6;
		double gx = InkLandsLayout.edgeDepth(at.x + s, at.z) - InkLandsLayout.edgeDepth(at.x - s, at.z);
		double gz = InkLandsLayout.edgeDepth(at.x, at.z + s) - InkLandsLayout.edgeDepth(at.x, at.z - s);
		if (gx * gx + gz * gz < 1.0E-6) { gx = -at.x; gz = -at.z; }
		return (float) Math.toDegrees(Math.atan2(-gx, gz));
	}

	/** Центр блока на поверхности (чанк загружается). */
	public static Vec3 onGround(ServerLevel level, int x, int z) {
		level.getChunk(x >> 4, z >> 4);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return new Vec3(x + 0.5, y, z + 0.5);
	}

	public static BlockPos blockOf(Vec3 v) {
		return BlockPos.containing(v);
	}
}
