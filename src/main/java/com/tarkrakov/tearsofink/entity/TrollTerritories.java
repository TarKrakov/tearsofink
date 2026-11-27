package com.tarkrakov.tearsofink.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
import static com.tarkrakov.tearsofink.entity.TrollTerritoryRules.*;

/** Persistent reservation of centers and resident slots; unloading is not a death. */
final class TrollTerritories extends SavedData {
    private static final Factory<TrollTerritories> FACTORY=new Factory<>(TrollTerritories::new,TrollTerritories::load,null);
    private final Map<UUID,Zone> zones=new LinkedHashMap<>();
    static TrollTerritories of(ServerLevel l){return l.getDataStorage().computeIfAbsent(FACTORY,"toi_troll_territories");}
    Collection<Zone> zones(){return zones.values();}
    boolean available(BlockPos p){
        for(Zone z:zones.values())if(!separated(p.getX(),p.getZ(),z.center.getX(),z.center.getZ()))return false;
        return true;
    }
    Zone create(BlockPos center,List<BlockPos> beds){
        Zone z=new Zone(UUID.randomUUID(),center,beds);zones.put(z.id,z);setDirty();return z;
    }
    void reserve(Slot s,UUID member){s.member=member;s.readyAt=0;setDirty();}
    void failed(Slot s,long now){s.member=null;s.readyAt=now+200;setDirty();}
    void died(UUID zone,UUID member,long now){
        Zone z=zones.get(zone);if(z==null)return;
        for(Slot s:z.slots)if(member.equals(s.member)){s.member=null;s.readyAt=now+REPLENISH;setDirty();return;}
    }
    // Recover legacy persistent residents and a resident whose entity NBT was
    // saved before the world ledger. Never evict or teleport existing mobs.
    void loaded(TrollEntity mob){
        if(!mob.isAlive()||mob.homePoint()==null||(!mob.isPersistenceRequired()&&mob.territoryId()==null))return;
        UUID member=mob.getUUID();
        for(Zone z:zones.values())for(Slot s:z.slots)if(member.equals(s.member)){
            mob.bindTerritory(z.id,z.center,s.bed);return;
        }
        UUID id=mob.territoryId();BlockPos center=BlockPos.containing(mob.homePoint());
        BlockPos bed=BlockPos.containing(mob.bedPoint());
        Zone known=id==null?null:zones.get(id);
        if(known!=null)for(Slot s:known.slots)if(s.member==null&&s.bed.equals(bed)){
            reserve(s,member);mob.bindTerritory(known.id,known.center,s.bed);return;
        }
        // Legacy homes may already overlap. Reserve their actual positions so
        // all future zones respect the minimum gap without moving old entities.
        Zone z=new Zone(id!=null&&known==null?id:UUID.randomUUID(),center,List.of(bed));
        zones.put(z.id,z);reserve(z.slots.getFirst(),member);mob.bindTerritory(z.id,z.center,bed);
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
        final List<Slot> slots;
        Zone(UUID id,BlockPos center,List<BlockPos> beds){
            this.id=id;this.center=center;slots=new ArrayList<>();for(BlockPos b:beds)slots.add(new Slot(b));
        }
        CompoundTag save(){
            CompoundTag t=new CompoundTag();t.putUUID("Id",id);t.putLong("Center",center.asLong());
            ListTag list=new ListTag();for(Slot s:slots){CompoundTag n=new CompoundTag();n.putLong("Bed",s.bed.asLong());n.putLong("ReadyAt",s.readyAt);if(s.member!=null)n.putUUID("Member",s.member);list.add(n);}
            t.put("Slots",list);return t;
        }
        static Zone load(CompoundTag t){
            Zone z=new Zone(t.getUUID("Id"),BlockPos.of(t.getLong("Center")),List.of());
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
