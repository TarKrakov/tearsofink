package com.tarkrakov.tearsofink.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import static com.tarkrakov.tearsofink.entity.TrollTerritoryRules.*;

/** Persistent reservation of centers and resident slots; unloading is not a death. */
public final class TrollTerritories extends SavedData {
    private static final Factory<TrollTerritories> FACTORY=new Factory<>(TrollTerritories::new,TrollTerritories::load,null);
    private final Map<UUID,Zone> zones=new LinkedHashMap<>();
    static TrollTerritories of(ServerLevel l){return l.getDataStorage().computeIfAbsent(FACTORY,"toi_troll_territories");}
    Collection<Zone> zones(){return zones.values();}

    public static boolean nearCave(ServerLevel level, BlockPos pos, int radius) {
        for (Zone zone : of(level).zones.values()) {
            if (zone.entrance != null && within(zone.center.getX(), zone.center.getZ(), pos.getX(), pos.getZ(), radius)) return true;
        }
        return false;
    }
    boolean available(BlockPos p){
        return available(p.getX(), p.getZ(), MIN_GAP);
    }
    boolean available(int x, int z, int gap) {
        for (Zone zone : zones.values()) {
            if (distanceSq(x, z, zone.center.getX(), zone.center.getZ()) < gap * gap) return false;
        }
        return true;
    }
    Zone create(BlockPos center,BlockPos entrance,List<BlockPos> beds){
        Zone z=new Zone(UUID.randomUUID(),center,entrance,beds);zones.put(z.id,z);setDirty();return z;
    }
    void reserve(Slot s,UUID member){s.member=member;s.readyAt=0;setDirty();}
    void failed(Slot s,long now){s.member=null;s.readyAt=now+200;setDirty();}
    void died(UUID zone,UUID member,long now){
        Zone z=zones.get(zone);if(z==null)return;
        for(Slot s:z.slots)if(member.equals(s.member)){s.member=null;s.readyAt=now+REPLENISH;setDirty();return;}
    }
    // Recover legacy residents and entities saved before the ledger. Discard
    // only a stale copy whose exact slot already records a different UUID.
    void loaded(TrollEntity mob){
        if(!mob.isAlive()||mob.homePoint()==null||(!mob.isPersistenceRequired()&&mob.territoryId()==null))return;
        UUID member=mob.getUUID();
        for(Zone z:zones.values())for(Slot s:z.slots)if(member.equals(s.member)){
            mob.bindTerritory(z.id,z.center,s.bed,z.entrance);return;
        }
        UUID id=mob.territoryId();BlockPos center=BlockPos.containing(mob.homePoint());
        BlockPos bed=BlockPos.containing(mob.bedPoint());
        Zone known=id==null?null:zones.get(id);
        if (known != null) {
            for (Slot slot : known.slots) {
                if (!slot.bed.equals(bed)) continue;
                if (slot.member == null) {
                    reserve(slot, member);
                    mob.bindTerritory(known.id, known.center, slot.bed, known.entrance);
                } else {
                    mob.retireStaleCopy();
                }
                return;
            }
            if (known.slots.size() == 1 && known.center.equals(center) && Objects.equals(known.entrance, mob.entrance())) {
                BlockPos existing = known.slots.getFirst().bed;
                double gap = distanceSq(existing.getX(), existing.getZ(), bed.getX(), bed.getZ());
                if (gap >= BED_MIN * BED_MIN && gap <= BED_MAX * BED_MAX) {
                    Slot recovered = new Slot(bed);
                    known.slots.add(recovered);
                    reserve(recovered, member);
                    return;
                }
            }
        }
        // Legacy homes may already overlap. Reserve their actual positions so
        // all future zones respect the minimum gap without moving old entities.
        Zone z=new Zone(id!=null&&known==null?id:UUID.randomUUID(),center,mob.entrance(),List.of(bed));
        zones.put(z.id,z);reserve(z.slots.getFirst(),member);mob.bindTerritory(z.id,z.center,bed,z.entrance);
    }
    static final class Slot {
        final BlockPos bed;
        UUID member;
        long readyAt;
        Slot(BlockPos bed){this.bed=bed;}
        boolean vacant(long now){return member==null&&now>=readyAt;}
    }
    static final class Zone {
        final UUID id;
        final BlockPos center;
        final BlockPos entrance;
        final List<Slot> slots;
        Zone(UUID id,BlockPos center,BlockPos entrance,List<BlockPos> beds){
            this.id=id;this.center=center;this.entrance=entrance;slots=new ArrayList<>();for(BlockPos b:beds)slots.add(new Slot(b));
        }
        CompoundTag save(){
            CompoundTag t=new CompoundTag();t.putUUID("Id",id);t.putLong("Center",center.asLong());
            if (entrance != null) t.putLong("Entrance", entrance.asLong());
            ListTag list=new ListTag();for(Slot s:slots){CompoundTag n=new CompoundTag();n.putLong("Bed",s.bed.asLong());n.putLong("ReadyAt",s.readyAt);if(s.member!=null)n.putUUID("Member",s.member);list.add(n);}
            t.put("Slots",list);return t;
        }
        static Zone load(CompoundTag t){
            BlockPos entrance = t.contains("Entrance", Tag.TAG_LONG) ? BlockPos.of(t.getLong("Entrance")) : null;
            Zone z=new Zone(t.getUUID("Id"),BlockPos.of(t.getLong("Center")),entrance,List.of());
            ListTag list=t.getList("Slots",Tag.TAG_COMPOUND);
            for(int i=0;i<list.size();i++){CompoundTag n=list.getCompound(i);Slot s=new Slot(BlockPos.of(n.getLong("Bed")));if(n.hasUUID("Member"))s.member=n.getUUID("Member");s.readyAt=Math.max(0,n.getLong("ReadyAt"));z.slots.add(s);}
            return z;
        }
    }
    private static TrollTerritories load(CompoundTag t,HolderLookup.Provider registries){
        TrollTerritories d=new TrollTerritories();ListTag list=t.getList("Zones",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){CompoundTag n=list.getCompound(i);if(n.hasUUID("Id")){Zone z=Zone.load(n);if(!z.slots.isEmpty())d.zones.put(z.id,z);}}
        return d;
    }
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider registries){
        ListTag list=new ListTag();for(Zone z:zones.values())list.add(z.save());t.put("Zones",list);return t;
    }
}
