package com.tarkrakov.tearsofink.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

import static com.tarkrakov.tearsofink.entity.PaleOwlTuning.*;

/** Short-lived local delay after a player kills an owl. No entity or chunk references. */
final class PaleOwlRecovery extends SavedData {
    private static final Factory<PaleOwlRecovery> FACTORY =
            new Factory<>(PaleOwlRecovery::new, PaleOwlRecovery::load, null);
    private final Map<BlockPos, Long> deaths = new HashMap<>();

    static PaleOwlRecovery of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "toi_pale_owl_recovery");
    }

    void record(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        prune(now);
        long until = now + KILL_REPLENISH_MIN + level.random.nextInt(KILL_REPLENISH_SPREAD + 1);
        deaths.merge(pos.immutable(), until, Math::max);
        setDirty();
    }

    boolean blocked(Vec3 at, long now) {
        for (var entry : deaths.entrySet()) {
            if (entry.getValue() <= now) continue;
            double dx = at.x - entry.getKey().getX() - .5;
            double dz = at.z - entry.getKey().getZ() - .5;
            if (dx * dx + dz * dz < KILL_REPLENISH_RADIUS * KILL_REPLENISH_RADIUS) return true;
        }
        return false;
    }

    void prune(long now) {
        if (deaths.values().removeIf(until -> until <= now)) setDirty();
    }

    private static PaleOwlRecovery load(CompoundTag tag, HolderLookup.Provider registries) {
        PaleOwlRecovery data = new PaleOwlRecovery();
        ListTag entries = tag.getList("Deaths", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            data.deaths.put(BlockPos.of(entry.getLong("Pos")), entry.getLong("Until"));
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        deaths.forEach((pos, until) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            entry.putLong("Until", until);
            entries.add(entry);
        });
        tag.put("Deaths", entries);
        return tag;
    }
}
