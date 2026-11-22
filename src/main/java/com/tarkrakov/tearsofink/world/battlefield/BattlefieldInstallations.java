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
import java.util.Set;

/**
 * Post-FULL, no-force-load installation of the actual battle. Worldgen solely
 * prepares virgin chunks and stamps private ground markers. Old visited chunks
 * do not contain those markers and can NEVER be retroactively populated here.
 *
 * One immutable episode per id, persisted BEFORE subsequent attempts, so
 * relog/order/reload do not duplicate blocks or reshuffle narrative roles.
 */
public final class BattlefieldInstallations extends SavedData {
    private static final Factory<BattlefieldInstallations> FACTORY =
            new Factory<>(BattlefieldInstallations::new, BattlefieldInstallations::load, null);
    private final Set<Long> attempted = new HashSet<>();
    private long nextTry;

    private BattlefieldInstallations() {}

    private static BattlefieldInstallations load(CompoundTag nbt, HolderLookup.Provider registries) {
        BattlefieldInstallations data = new BattlefieldInstallations();
        for (long key : nbt.getLongArray("PlacedOrSkipped")) data.attempted.add(key);
        return data;
    }

    @Override public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putLongArray("PlacedOrSkipped", attempted.stream().mapToLong(Long::longValue).toArray());
        return nbt;
    }

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (!ModDimensions.isLands(level) || level.players().isEmpty()
                    || level.getGameTime() % 20 != 7) return;
            InkLandsLayout layout = InkLandsLayout.of(level);
            BattlefieldWorldPlan plan = BattlefieldWorldPlan.of(layout);
            if (plan == null || plan.fields().isEmpty()) return;
            level.getDataStorage().computeIfAbsent(FACTORY, "toi_battlefield_installations_v1")
                    .installNearby(level, plan);
        });
    }

    /** Explicitly no level.getChunk() and no synchronous block-generation access. */
    public static boolean stamped(ServerLevel level, int x, int z) {
        var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return false;
        int top = Math.min(level.getMaxBuildHeight() - 1,
                chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15));
        int low = Math.max(level.getMinBuildHeight(), top - 65);
        for (int y = top; y >= low; y--) {
            if (chunk.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.BATTLEFIELD_FIELD_MARKER))
                return true;
        }
        return false;
    }

    private static long key(int field, int placement) {
        return (((long) field) << 32) | (placement & 0xffff_ffffL);
    }

    private void installNearby(ServerLevel level, BattlefieldWorldPlan plan) {
        long now = level.getGameTime();
        if (now < nextTry) return;
        nextTry = now + 20;
        int budget = 5; // Limit chunk inspections and placements per second.
        for (BattlefieldWorldPlan.Field field : plan.fields()) {
            if (!playerApproaching(level, field)) continue;
            // A full, new-generation center is mandatory even for peripheral traces.
            if (!stamped(level, field.x(), field.z())) continue;
            for (BattlefieldWorldPlan.Placement slot : field.placements()) {
                if (budget <= 0) return;
                long id = key(field.index(), slot.id());
                if (attempted.contains(id)) continue;
                BlockPos rootXZ = new BlockPos(slot.x(), 0, slot.z());
                if (!ModDimensions.chunksReady(level, rootXZ, 22)) continue;
                if (playerUnderfoot(level, slot.x(), slot.z())) continue;
                // Individual stamps prevent the new field from spilling into
                // previously explored roadside chunks in an existing save.
                if (!stamped(level, slot.x(), slot.z())) {
                    attempted.add(id);
                    setDirty();
                    continue;
                }
                boolean made;
                if (slot.kind() == BattlefieldWorldPlan.Kind.MONUMENT) {
                    var blade = BattlefieldMonumentGeometry.Blade.values()[slot.blade()];
                    made = BattlefieldDecorCommands.placePlannedMonument(level, rootXZ, slot.facing(),
                            blade, slot.variant(), slot.tableau());
                } else {
                    made = BattlefieldZonePreview.placePlanned(level, rootXZ,
                            slot.facing(), slot.scene(), slot.variant());
                }
                // A failed natural footprint is a stable omission. Never re-attempt
                // to overwrite a player's later building after the first evaluation.
                attempted.add(id);
                setDirty();
                budget--;
                if (made) {
                    TearsOfInk.LOGGER.info("[battlefield] placed {} {} @ {},{} (field {}, seed {})",
                            slot.kind(), slot.scene() == null ? slot.blade() : slot.scene().id(),
                            slot.x(), slot.z(), field.index(), field.seed());
                } else {
                    TearsOfInk.LOGGER.debug("[battlefield] omitted blocked {} @ {},{}",
                            slot.kind(), slot.x(), slot.z());
                }
            }
        }
    }

    private static boolean playerApproaching(ServerLevel level, BattlefieldWorldPlan.Field field) {
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            // Road clue line extends ~125 blocks from the center.
            if (field.distance(player.getBlockX(), player.getBlockZ()) <= 205) return true;
        }
        return false;
    }

    private static boolean playerUnderfoot(ServerLevel level, int x, int z) {
        for (ServerPlayer player : level.players()) {
            if (Math.abs(player.getX() - x) < 9 && Math.abs(player.getZ() - z) < 9) return true;
        }
        return false;
    }
}
