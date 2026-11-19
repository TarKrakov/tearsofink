package com.tarkrakov.tearsofink.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import static com.tarkrakov.tearsofink.entity.PaleDeerTuning.*;

/** A world-local ledger, NOT a chunk ticket. Members are looked up by UUID without loading chunks. */
final class PaleDeerHerd extends SavedData {
    private static final Factory<PaleDeerHerd> FACTORY = new Factory<>(PaleDeerHerd::new, PaleDeerHerd::load, null);
    enum Mode { TRAVEL, GRAZE, REST, PANIC, RECOVER }
    final Map<UUID, Group> groups = new HashMap<>();
    private final Map<Long, UUID> cells = new HashMap<>();

    static PaleDeerHerd of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "toi_pale_deer_herds");
    }
    static long cell(BlockPos p) {
        return ((long)Math.floorDiv(p.getX(), CELL) << 32) ^ (Math.floorDiv(p.getZ(), CELL) & 0xffffffffL);
    }
    static int duration(ServerLevel level, int min, int spread) { return min + level.random.nextInt(spread + 1); }
    boolean available(long cell, long now) {
        Group g = groups.get(cells.get(cell));
        return g == null || (g.members.isEmpty() && now >= g.replenishAt);
    }
    Group create(ServerLevel level, BlockPos home, boolean natural) {
        long cell = cell(home);
        if (natural) {
            UUID old = cells.get(cell);
            if (old != null) groups.remove(old);
        }
        Group g = new Group(UUID.randomUUID(), home, natural, cell);
        g.remaining = duration(level, GRAZE_MIN, GRAZE_SPREAD);
        groups.put(g.id, g);
        if (natural) cells.put(cell, g.id);
        setDirty();
        return g;
    }
    Group get(UUID id) { return groups.get(id); }
    void join(Group g, UUID member) { if (g.members.add(member)) setDirty(); }
    void died(ServerLevel level, Group g, UUID member) {
        if (!g.members.remove(member)) return;
        g.guards.remove(member);
        if (g.members.isEmpty()) {
            if (g.natural) g.replenishAt = level.getGameTime() + duration(level, RESPAWN_MIN, RESPAWN_SPREAD);
            else groups.remove(g.id);
        }
        setDirty();
    }
    List<PaleDeerEntity> active(ServerLevel level, Group g) {
        List<PaleDeerEntity> list = new ArrayList<>(g.members.size());
        for (UUID id : g.members) {
            if (level.getEntity(id) instanceof PaleDeerEntity deer && deer.isAlive()
                    && level.isPositionEntityTicking(deer.blockPosition()) && deer.activeNearPlayer(level) && !deer.isNoAi()) list.add(deer);
        }
        list.sort(Comparator.comparing(PaleDeerEntity::getUUID));
        return list;
    }
    void alarm(ServerLevel level, Group g, PaleDeerEntity caller, BlockPos threat) {
        g.threat = threat;
        if (g.mode == Mode.PANIC) {
            g.remaining = Math.max(g.remaining, PANIC_MIN);
            setDirty();
            return;
        }
        g.mode = Mode.PANIC;
        g.alarm++;
        g.caller = caller.getUUID();
        g.remaining = duration(level, PANIC_MIN, PANIC_SPREAD);
        g.target = escapeTarget(level, g, caller.blockPosition());
        g.guards.clear();
        setDirty();
    }
    private BlockPos escapeTarget(ServerLevel level, Group g, BlockPos from) {
        Vec3 away = Vec3.atCenterOf(from).subtract(Vec3.atCenterOf(g.threat));
        away = new Vec3(away.x, 0, away.z);
        if (away.horizontalDistanceSqr() < .1) away = Vec3.directionFromRotation(0, level.random.nextFloat() * 360);
        away = away.normalize();
        int dist = duration(level, ESCAPE_MIN, ESCAPE_MAX - ESCAPE_MIN);
        if (LoadedFaunaTerrain.search(level)) {
            for (int r : new int[]{dist, 45, 24}) for (double angle : new double[]{0, 35, -35, 70, -70}) {
                double a=Math.toRadians(angle), c=Math.cos(a), s=Math.sin(a);
                BlockPos p=PaleDeerEncounters.site(level,from.getX()+(int)Math.round((away.x*c-away.z*s)*r),
                        from.getZ()+(int)Math.round((away.z*c+away.x*s)*r),false);
                if (p!=null) return p;
            }
        }
        return from.offset((int)Math.round(away.x * dist), 0, (int)Math.round(away.z * dist));
    }
    private static double horizontalSq(BlockPos a, BlockPos b) {
        double x = a.getX() - b.getX(), z = a.getZ() - b.getZ();
        return x*x + z*z;
    }
    private BlockPos nextSite(ServerLevel level, Group g, BlockPos center) {
        if (!LoadedFaunaTerrain.search(level)) return null;
        // Return in stages even if the home itself is in an unloaded chunk.
        if (horizontalSq(center,g.home)>HOME_RADIUS*HOME_RADIUS) {
            Vec3 toward=Vec3.atCenterOf(g.home).subtract(Vec3.atCenterOf(center));
            double length=toward.horizontalDistance();
            for (int i=0;i<5;i++) {
                int r=ROAM_MAX-i*7;
                BlockPos p=PaleDeerEncounters.site(level,center.getX()+(int)Math.round(toward.x/length*r),
                        center.getZ()+(int)Math.round(toward.z/length*r),false);
                if (p!=null) return p;
            }
        }
        for (int i = 0; i < 8; i++) {
            double a = level.random.nextDouble()*Math.PI*2;
            int r = duration(level, ROAM_MIN, ROAM_MAX - ROAM_MIN);
            int x = center.getX() + (int)Math.round(Math.cos(a)*r);
            int z = center.getZ() + (int)Math.round(Math.sin(a)*r);
            BlockPos p = PaleDeerEncounters.site(level, x, z, false);
            if (p != null && horizontalSq(p, g.home) <= HOME_RADIUS*HOME_RADIUS) return p;
        }
        return PaleDeerEncounters.site(level, g.home.getX(), g.home.getZ(), false);
    }
    private void assignGuards(ServerLevel level, Group g, List<PaleDeerEntity> deer) {
        g.guards.clear();
        int n = deer.size() >= 7 ? 2 : deer.size() >= 2 ? 1 : 0;
        List<PaleDeerEntity> candidates = new ArrayList<>(deer);
        // Rotate, then prefer individuals already on the corresponding edge of the herd.
        g.guardHeading += 100 + level.random.nextInt(161);
        for (int i = 0; i < n; i++) {
            BlockPos p = g.guardPoint(i);
            candidates.sort(Comparator.comparingDouble(d -> d.distanceToSqr(Vec3.atBottomCenterOf(p))));
            PaleDeerEntity selected = candidates.removeFirst();
            g.guards.add(selected.getUUID());
        }
        g.guardIn = duration(level, GUARD_MIN, GUARD_SPREAD);
    }
    void tick(ServerLevel level, Group g, PaleDeerEntity driver) {
        long now = level.getGameTime();
        if (g.lastTick == now) return;
        g.lastTick = now;
        if (g.remaining > 0) --g.remaining;
        if (g.guardIn > 0) --g.guardIn;
        // The persisted clock advances only while an actual member is being simulated.
        if (now % SENSE_INTERVAL == 0 || g.remaining == 0) {
            List<PaleDeerEntity> deer = active(level, g);
            if (deer.isEmpty()) return;
            double x = 0, z = 0;
            for (PaleDeerEntity d : deer) { x += d.getX(); z += d.getZ(); }
            BlockPos center = BlockPos.containing(x/deer.size(), driver.getY(), z/deer.size());
            switch (g.mode) {
                case TRAVEL -> {
                    if (g.remaining == 0 || horizontalSq(center, g.target) < 7*7) {
                        g.mode = Mode.GRAZE;
                        g.target = PaleDeerEncounters.site(level, center.getX(), center.getZ(), false);
                        if (g.target == null) g.target = center;
                        g.remaining = duration(level, GRAZE_MIN, GRAZE_SPREAD);
                        g.guardIn = 0;
                    }
                }
                case GRAZE -> {
                    if (g.remaining == 0) {
                        g.mode = Mode.REST;
                        g.remaining = duration(level, REST_MIN, REST_SPREAD);
                    }
                }
                case REST, RECOVER -> {
                    if (g.remaining == 0) {
                        BlockPos target = nextSite(level, g, center);
                        if (target != null) {
                            g.target = target;
                            g.mode = Mode.TRAVEL;
                            g.remaining = duration(level, WALK_MIN, WALK_SPREAD);
                            g.guards.clear();
                        } else g.remaining = 40;
                    }
                }
                case PANIC -> {
                    // Close pursuers keep the group moving without generating another roar episode.
                    if (now % 20 == 0) {
                        ServerPlayer pursuer=null;
                        double nearest=14*14;
                        for (ServerPlayer p:level.players()) {
                            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
                            for (PaleDeerEntity d:deer) {
                                double distance=d.distanceToSqr(p);
                                if (distance<nearest) { nearest=distance; pursuer=p; }
                            }
                        }
                        if (pursuer!=null) {
                            g.remaining=Math.max(g.remaining,100);
                            g.threat=pursuer.blockPosition();
                            Vec3 away=Vec3.atCenterOf(center).subtract(pursuer.position());
                            Vec3 route=Vec3.atCenterOf(g.target).subtract(Vec3.atCenterOf(center));
                            if (horizontalSq(center,g.target)<8*8 || away.x*route.x+away.z*route.z<=0)
                                g.target=escapeTarget(level,g,center);
                        }
                    }
                    if (g.remaining == 0) {
                        g.mode = Mode.RECOVER;
                        g.target = center;
                        g.remaining = duration(level, RECOVER_MIN, RECOVER_SPREAD);
                        g.guardIn = 0;
                    }
                }
            }
            if (g.mode == Mode.GRAZE || g.mode == Mode.REST) {
                int count = deer.size() >= 7 ? 2 : deer.size() >= 2 ? 1 : 0;
                if (g.guardIn == 0 || g.guards.size() != count
                        || g.guards.stream().anyMatch(id -> deer.stream().noneMatch(d -> d.getUUID().equals(id)))) {
                    assignGuards(level, g, deer);
                }
            } else g.guards.clear();
        }
        setDirty();
    }
    static final class Group {
        final UUID id;
        final BlockPos home;
        final boolean natural;
        final long cell;
        final Set<UUID> members = new LinkedHashSet<>();
        final List<UUID> guards = new ArrayList<>();
        Mode mode = Mode.GRAZE;
        BlockPos target, threat;
        UUID caller;
        long alarm, replenishAt, lastTick = Long.MIN_VALUE;
        int remaining, guardIn, guardHeading;
        Group(UUID id, BlockPos home, boolean natural, long cell) {
            this.id=id; this.home=home; this.natural=natural; this.cell=cell;
            target=home; threat=home; guardHeading=Math.floorMod(id.hashCode(),360);
        }
        BlockPos guardPoint(int i) {
            double a = Math.toRadians(guardHeading + i*180);
            return target.offset((int)Math.round(Math.cos(a)*GUARD_DISTANCE), 0,
                    (int)Math.round(Math.sin(a)*GUARD_DISTANCE));
        }
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id); t.putLong("Home", home.asLong()); t.putBoolean("Natural", natural);
            t.putLong("Cell", cell); t.putLong("Target", target.asLong()); t.putLong("Threat", threat.asLong());
            t.putString("Mode", mode.name()); t.putLong("Alarm", alarm); t.putLong("Replenish", replenishAt);
            t.putInt("Remaining", remaining); t.putInt("GuardIn", guardIn); t.putInt("GuardHeading", guardHeading);
            if (caller != null) t.putUUID("Caller", caller);
            ListTag m = new ListTag(), w = new ListTag();
            for (UUID u : members) { CompoundTag v=new CompoundTag(); v.putUUID("Id",u); m.add(v); }
            for (UUID u : guards) { CompoundTag v=new CompoundTag(); v.putUUID("Id",u); w.add(v); }
            t.put("Members",m); t.put("Guards",w);
            return t;
        }
        static Group load(CompoundTag t) {
            Group g = new Group(t.getUUID("Id"), BlockPos.of(t.getLong("Home")), t.getBoolean("Natural"), t.getLong("Cell"));
            g.target=BlockPos.of(t.getLong("Target")); g.threat=BlockPos.of(t.getLong("Threat"));
            try { g.mode=Mode.valueOf(t.getString("Mode")); } catch (IllegalArgumentException e) { g.mode=Mode.GRAZE; }
            g.alarm=t.getLong("Alarm"); g.replenishAt=t.getLong("Replenish");
            g.remaining=Math.max(0,t.getInt("Remaining")); g.guardIn=Math.max(0,t.getInt("GuardIn"));
            g.guardHeading=t.getInt("GuardHeading");
            if (t.hasUUID("Caller")) g.caller=t.getUUID("Caller");
            ListTag m=t.getList("Members",Tag.TAG_COMPOUND), w=t.getList("Guards",Tag.TAG_COMPOUND);
            for (int i=0;i<m.size();i++) if (m.getCompound(i).hasUUID("Id")) g.members.add(m.getCompound(i).getUUID("Id"));
            for (int i=0;i<w.size();i++) if (w.getCompound(i).hasUUID("Id")) g.guards.add(w.getCompound(i).getUUID("Id"));
            return g;
        }
    }
    private static PaleDeerHerd load(CompoundTag tag, HolderLookup.Provider registries) {
        PaleDeerHerd data = new PaleDeerHerd();
        ListTag list=tag.getList("Herds",Tag.TAG_COMPOUND);
        for (int i=0;i<list.size();i++) {
            CompoundTag t=list.getCompound(i);
            if (!t.hasUUID("Id")) continue;
            Group g=Group.load(t);
            data.groups.put(g.id,g);
            if (g.natural) data.cells.put(g.cell,g.id);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list=new ListTag();
        for (Group g:groups.values()) list.add(g.save());
        tag.put("Herds",list);
        return tag;
    }
}
