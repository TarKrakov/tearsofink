package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.command.BattlefieldDecorCommands;
import com.tarkrakov.tearsofink.command.BattlefieldZonePreview;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Post-FULL, no-force-load installation of the actual battle. Worldgen solely
 * prepares virgin chunks and stamps private ground markers. Old visited chunks
 * do not contain those markers and can NEVER be retroactively populated here.
 *
 * Only SUCCESSFUL episodes are saved by id. Blocked positions retry after
 * a bounded delay; relog/order/reload cannot duplicate committed anchors.
 */
public final class BattlefieldInstallations extends SavedData {
    private static final Factory<BattlefieldInstallations> FACTORY =
            new Factory<>(BattlefieldInstallations::new, BattlefieldInstallations::load, null);
    private final Set<Long> attempted = new HashSet<>();
    private final Set<Long> cleanedChunks = new HashSet<>();
    private final Set<Long> connectedParts = new HashSet<>();
    private long nextTry;
    // Retry transiently blocked sites; only actual placed scenes are stored in SavedData.
    private final Map<Long, Long> blockedUntil = new HashMap<>();
    private final Map<Long, Long> connectionsBlockedUntil = new HashMap<>();

    private BattlefieldInstallations() {}

    private static BattlefieldInstallations load(CompoundTag nbt, HolderLookup.Provider registries) {
        BattlefieldInstallations data = new BattlefieldInstallations();
        for (long key : nbt.getLongArray("PlacedOrSkipped")) data.attempted.add(key);
        for (long key : nbt.getLongArray("CleanedChunks")) data.cleanedChunks.add(key);
        for (long key : nbt.getLongArray("JoinedTerrain")) data.connectedParts.add(key);
        return data;
    }

    @Override public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putLongArray("PlacedOrSkipped", attempted.stream().mapToLong(Long::longValue).toArray());
        nbt.putLongArray("CleanedChunks", cleanedChunks.stream().mapToLong(Long::longValue).toArray());
        nbt.putLongArray("JoinedTerrain", connectedParts.stream().mapToLong(Long::longValue).toArray());
        return nbt;
    }

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (!ModDimensions.isLands(level) || level.players().isEmpty()
                    || level.getGameTime() % 20 != 7) return;
            InkLandsLayout layout = InkLandsLayout.of(level);
            BattlefieldWorldPlan plan = BattlefieldWorldPlan.of(layout);
            if (plan == null || plan.fields().isEmpty()) return;
            level.getDataStorage().computeIfAbsent(FACTORY, "toi_battlefield_installations_v3")
                    .installNearby(level, plan);
        });
    }

    /**
     * New worldgen puts a stone-deep marker below the natural column. The first
     * release wrote it only if the stone happened to be at depths 4..10:
     * deep ash, caves and uneven ground could therefore create the clearing but
     * leave no marker. Detect that released terrain using its distinctive
     * ASHEN_PATH scar pattern, away from main roads, WITHOUT touching any blocks.
     *
     * This also safely upgrades already-generated v1 battlefields. Unlike a
     * blanket retrogen pass, an old unmodified chunk never has those scars.
     */
    public static boolean stamped(ServerLevel level, int x, int z) {
        var plan = BattlefieldWorldPlan.of(InkLandsLayout.of(level));
        if (plan == null) return false;
        for (var field : plan.fields()) {
            if (field.distance(x, z) <= 205)
                return stamped(level, field, x, z);
        }
        return false;
    }

    public static boolean stamped(ServerLevel level, BattlefieldWorldPlan.Field field, int x, int z) {
        var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return false;
        // Read both the exact intended slot and the new per-chunk proof
        // column. All data comes from the loaded chunk; no chunk generation.
        int[] testX = {x, (x & ~15) + 8};
        int[] testZ = {z, (z & ~15) + 8};
        for (int i = 0; i < 2; i++) {
            int px = testX[i], pz = testZ[i];
            int top = Math.min(level.getMaxBuildHeight() - 1,
                    chunk.getHeight(Heightmap.Types.WORLD_SURFACE, px & 15, pz & 15));
            for (int y = top; y >= Math.max(level.getMinBuildHeight(), top - 82); y--) {
                if (chunk.getBlockState(new BlockPos(px, y, pz)).is(ModBlocks.BATTLEFIELD_FIELD_MARKER))
                    return true;
            }
        }
        // Only authentic field scars count. A road's ASHEN_PATH cannot unlock
        // generation on a previously explored patch because the approach
        // predicate excludes the roadway itself.
        int markers = 0;
        for (int dx = 0; dx < 16; dx += 2) for (int dz = 0; dz < 16; dz += 2) {
            int px = (x & ~15) + dx, pz = (z & ~15) + dz;
            if (field.openness(px, pz) < .35 && field.approach(px, pz) < .24) continue;
            int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz) - 1;
            if (y < level.getMinBuildHeight()) continue;
            if (chunk.getBlockState(new BlockPos(px, y, pz)).is(ModBlocks.ASHEN_PATH)) {
                if (++markers >= 3) return true;
            }
        }
        return false;
    }

    /**
     * The release's first field could have missing deep-ground markers at its
     * exact center. Two genuinely scarred neighboring chunks are sufficient
     * to prove the same battlefield was created; an untouched old forest
     * cannot satisfy this evidence requirement.
     */
    public static boolean fieldReady(ServerLevel level, BattlefieldWorldPlan.Field field) {
        if (stamped(level, field, field.x(), field.z())) return true;
        int marked = 0;
        int chunkX = field.x() >> 4, chunkZ = field.z() >> 4;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int x = ((chunkX + dx) << 4) + 8;
            int z = ((chunkZ + dz) << 4) + 8;
            if (stamped(level, field, x, z) && ++marked >= 2) return true;
        }
        return false;
    }

    public static int placedCount(ServerLevel level, BattlefieldWorldPlan.Field field) {
        var saved = level.getDataStorage().computeIfAbsent(FACTORY, "toi_battlefield_installations_v3");
        int count = 0;
        for (var slot : field.placements()) if (saved.attempted.contains(key(field.index(), slot.id())))
            count++;
        return count;
    }

    private static long routeKey(int field, int link, int part) {
        return 0x1000_0000_0000_0000L | ((long) field << 40) | ((long) link << 20) | part;
    }

    public static int routeCount(ServerLevel level, BattlefieldWorldPlan.Field field) {
        var saved = level.getDataStorage().computeIfAbsent(FACTORY, "toi_battlefield_installations_v3");
        int done = 0;
        for (int r = 0; r < BattlefieldAccessRoutes.routes(); r++)
            for (int part = 0; part < BattlefieldAccessRoutes.parts(r); part++)
                if (saved.connectedParts.contains(routeKey(field.index(), r, part))) done++;
        return done;
    }
    public static int routeTotal() {
        int n = 0;
        for (int r = 0; r < BattlefieldAccessRoutes.routes(); r++)
            n += BattlefieldAccessRoutes.parts(r);
        return n;
    }

    /**
     * Only linked, newly-generated natural landscape may be moved. A high hill
     * or river can otherwise separate scene clusters from the intended route.
     * One short part per second, persisted by ID and safe to retry after login.
     */
    private void joinLandscape(ServerLevel level, BattlefieldWorldPlan.Field field, long now) {
        int tries = 0;
        for (int link = 0; link < BattlefieldAccessRoutes.routes(); link++)
            for (int section = 0; section < BattlefieldAccessRoutes.parts(link); section++) {
                long id = routeKey(field.index(), link, section);
                if (connectedParts.contains(id)
                        || connectionsBlockedUntil.getOrDefault(id, 0L) > now) continue;
                if (BattlefieldAccessRoutes.prepare(level, field, link, section)) {
                    connectedParts.add(id);
                    setDirty();
                    return;
                }
                connectionsBlockedUntil.put(id, now + 100L);
                if (++tries >= 2) return;
            }
    }

    private static long key(int field, int placement) {
        return (((long) field) << 32) | (placement & 0xffff_ffffL);
    }

    private void installNearby(ServerLevel level, BattlefieldWorldPlan plan) {
        long now = level.getGameTime();
        if (now < nextTry) return;
        nextTry = now + 10;
        int budget = 2; // Site sculpting and scene placement share a bounded tick budget.
        for (BattlefieldWorldPlan.Field field : plan.fields()) {
            if (!playerApproaching(level, field)) continue;
            // A full, new-generation center is mandatory even for peripheral traces.
            if (!fieldReady(level, field)) continue;
            // New area, or old field that retained generation fallout. Small
            // per-tick cleaning budget; never scan structures or older chunks.
            int cleanedThisTick = 0;
            for (ServerPlayer player : level.players()) {
                int pxc = player.getBlockX() >> 4, pzc = player.getBlockZ() >> 4;
                for (int dx = -3; dx <= 3 && cleanedThisTick < 2; dx++)
                    for (int dz = -3; dz <= 3 && cleanedThisTick < 2; dz++) {
                        int ccx = pxc + dx, ccz = pzc + dz;
                        long k = net.minecraft.world.level.ChunkPos.asLong(ccx, ccz);
                        if (cleanedChunks.contains(k)) continue;
                        int mx = ccx * 16 + 8, mz = ccz * 16 + 8;
                        if (field.openness(mx, mz) < .20
                                && field.approach(mx, mz) < .32) continue;
                        if (!stamped(level, field, mx, mz)) continue;
                        if (level.getChunkSource().getChunkNow(ccx, ccz) == null) continue;
                        // Do not mistake freshly dropped player items for terrain fallout.
                        if (playerUnderfoot(level, mx, mz, 13)) continue;
                        BattlefieldGroundCleanup.cleanChunk(level, field, ccx, ccz);
                        cleanedChunks.add(k);
                        cleanedThisTick++;
                        setDirty();
                    }
                if (cleanedThisTick >= 2) break;
            }

            // A few narrow graded earth tracks keep the terrain's dramatic
            // gorge and tall hill from splitting the battlefield in two.
            // Their own persistent progress is separate from the 27+ episode IDs.
            joinLandscape(level, field, now);
            for (BattlefieldWorldPlan.Placement slot : field.placements()) {
                if (budget <= 0) return;
                long id = key(field.index(), slot.id());
                if (attempted.contains(id) || blockedUntil.getOrDefault(id, 0L) > now) continue;
                BlockPos rootXZ = new BlockPos(slot.x(), 0, slot.z());
                if (!ModDimensions.chunksReady(level, rootXZ, 22)) continue;
                if (playerUnderfoot(level, slot.x(), slot.z(),
                        slot.kind() == BattlefieldWorldPlan.Kind.RELIC ? 9 : 19)) continue;
                // Recover safely after a power loss between the world write and
                // the SavedData flush. Never place a second overlapping anchor.
                if (hasAnchor(level, slot.x(), slot.z())) {
                    attempted.add(id);
                    setDirty();
                    continue;
                }
                // Individual stamps prevent the new field from spilling into
                // previously explored roadside chunks in an existing save.
                if (!stamped(level, field, slot.x(), slot.z())) {
                    // Old chunk or not-yet-proven site: NEVER retrogen over
                    // other saves, but don't permanently discard the episode.
                    blockedUntil.put(id, now + 200L);
                    continue;
                }
                if (slot.kind() != BattlefieldWorldPlan.Kind.RELIC
                        && !BattlefieldSitePreparation.prepare(level, field, slot)) {
                    blockedUntil.put(id, now + 200L);
                    continue;
                }
                boolean made;
                if (slot.kind() == BattlefieldWorldPlan.Kind.MONUMENT) {
                    var blade = BattlefieldMonumentGeometry.Blade.values()[slot.blade()];
                    made = BattlefieldDecorCommands.placePlannedMonument(level, rootXZ, slot.facing(),
                            blade, slot.variant(), slot.tableau());
                } else if (slot.kind() == BattlefieldWorldPlan.Kind.RELIC) {
                    made = BattlefieldRelicPlacement.place(level, rootXZ, slot.facing(),
                            slot.blade(), slot.variant());
                } else {
                    made = BattlefieldZonePreview.placePlanned(level, rootXZ,
                            slot.facing(), slot.scene(), slot.variant());
                }
                // A failed natural footprint is a stable omission. Never re-attempt
                // to overwrite a player's later building after the first evaluation.
                budget--;
                if (made) {
                    attempted.add(id);
                    setDirty();
                    if (slot.kind() != BattlefieldWorldPlan.Kind.RELIC) {
                        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                slot.x(), slot.z());
                        BattlefieldGroundCleanup.clearAfterScene(level,
                                new BlockPos(slot.x(), sy, slot.z()), 18);
                    }
                    TearsOfInk.LOGGER.info("[battlefield] placed {} {} @ {},{} (field {}, seed {})",
                            slot.kind(), slot.scene() == null ? slot.blade() : slot.scene().id(),
                            slot.x(), slot.z(), field.index(), field.seed());
                } else {
                    // A rough site can need another local pose/anchor; do not
                    // permanently mark an episode that never appeared.
                    blockedUntil.put(id, now + 400L);
                    TearsOfInk.LOGGER.warn("[battlefield] pending retry {} at {},{}; no scene written",
                            slot.kind(), slot.x(), slot.z());
                }
            }
        }
    }

    private static boolean hasAnchor(ServerLevel level, int x, int z) {
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            int px = x + dx, pz = z + dz;
            var chunk = level.getChunkSource().getChunkNow(px >> 4, pz >> 4);
            if (chunk == null) continue;
            int top = Math.min(level.getMaxBuildHeight() - 1,
                    chunk.getHeight(Heightmap.Types.WORLD_SURFACE, px & 15, pz & 15));
            for (int y = top + 3; y >= top - 5 && y >= level.getMinBuildHeight(); y--) {
                var state = chunk.getBlockState(new BlockPos(px, y, pz));
                if (state.is(ModBlocks.BATTLEFIELD_SCENE)
                        || state.is(ModBlocks.BATTLEFIELD_MONUMENT)
                        || state.is(ModBlocks.BATTLEFIELD_RELIC_PATCH)) return true;
            }
        }
        return false;
    }

    private static boolean playerApproaching(ServerLevel level, BattlefieldWorldPlan.Field field) {
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            // Road clue line extends ~125 blocks from the center.
            if (field.distance(player.getBlockX(), player.getBlockZ()) <= 205) return true;
        }
        return false;
    }

    private static boolean playerUnderfoot(ServerLevel level, int x, int z, int radius) {
        for (ServerPlayer player : level.players()) {
            if (Math.abs(player.getX() - x) < radius && Math.abs(player.getZ() - z) < radius) return true;
        }
        return false;
    }
}
