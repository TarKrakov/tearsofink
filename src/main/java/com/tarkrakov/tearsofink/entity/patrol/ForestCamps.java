package com.tarkrakov.tearsofink.entity.patrol;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.lantern.FogClearing;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** D-089. Persisted, seed-stable camps; placement is restricted to already FULL chunks. */
public final class ForestCamps extends SavedData {
    static final int CELL=288, JITTER=36, INSTALL_RADIUS=112, WAKE=72, LEASH=24, PATROL_BUFFER=40;
    private static final int[] COUNTS={463,230,242};
    private static final String[] ASSETS={"pale_watchtower_camp","pale_camp_fire","pale_camp_flat"};
    private static final Factory<ForestCamps> FACTORY=new Factory<>(ForestCamps::new,ForestCamps::load,null);
    private static final String[] PALETTE={"cracked_ruin_brick_slab","cracked_ruin_bricks","cracked_ruin_brick_stairs","cracked_ruin_tiles",
        "cracked_ruin_pillar","ink_lantern","overgrown_ruin_brick_slab","overgrown_ruin_bricks","overgrown_ruin_brick_stairs",
        "pale_fence","ruin_brick_slab","ruin_brick_stairs","ruin_bricks","ruin_tiles","ruin_pillar"};
    private final Map<Long,Camp> camps=new HashMap<>();
    private final Map<Long,Camp> lamps=new HashMap<>();
    private final Set<Long> authored=new HashSet<>();
    private final Map<Long,Long> retry=new HashMap<>();
    static ForestCamps of(ServerLevel level) {return level.getDataStorage().computeIfAbsent(FACTORY,"toi_forest_camps");}
    private static boolean onServer(Level level) {
        return level instanceof ServerLevel s&&ModDimensions.isLands(s)&&s.getServer().isSameThread();
    }
    static boolean lamp(Level level,BlockPos p) {return onServer(level)&&of((ServerLevel)level).lamps.containsKey(p.asLong());}
    static boolean authored(Level level,BlockPos p) {return onServer(level)&&of((ServerLevel)level).authored.contains(p.asLong());}
    static boolean near(ServerLevel level,BlockPos p,int radius) {
        return of(level).camps.values().stream().anyMatch(c->horizontal(c.center,p)<=radius*radius);
    }
    static boolean safe(ServerLevel level,BlockPos p) {
        long now=level.getGameTime();
        return of(level).camps.values().stream().anyMatch(c->horizontal(c.center,p)<=24*24
                &&p.getY()>=c.center.getY()-8&&p.getY()<=c.center.getY()+24&&c.lit(now));
    }
    static void ignited(ServerLevel level,BlockPos p) {
        ForestCamps data=of(level);Camp c=data.lamps.get(p.asLong());
        if(c!=null){c.flames.put(p.asLong(),level.getGameTime()+BarricadeGates.FLAME_TICKS);c.wasLit=true;data.setDirty();}
    }
    static void died(ServerLevel level,UUID uuid) {
        ForestCamps data=of(level);
        for(Camp c:data.camps.values())if(c.guards.remove(uuid)){
            if(c.guards.isEmpty())c.cooldown=level.getGameTime()+Outposts.RESPAWN;
            data.setDirty();return;
        }
    }
    private static double horizontal(BlockPos a,BlockPos b){double x=a.getX()-b.getX(),z=a.getZ()-b.getZ();return x*x+z*z;}
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private static RandomSource rng(ServerLevel l,long k){return RandomSource.create(l.getSeed()^(k*0x9e3779b97f4a7c15L)^0x43414d5053L);}
    private static double nearest(ServerLevel level,BlockPos p) {
        double n=Double.MAX_VALUE;
        for(ServerPlayer a:level.players())if(a.isAlive()&&!a.isSpectator())n=Math.min(n,horizontal(a.blockPosition(),p));
        return n;
    }
    private static BlockPos transform(BlockPos origin,Rotation r,BlockPos p){
        return origin.offset(StructureTemplate.transform(p,Mirror.NONE,r,BlockPos.ZERO));
    }
    private static final class Camp {
        final long key; final int type,tier; final BlockPos center,origin; final Rotation rotation;
        final Map<Long,Long> flames=new LinkedHashMap<>();
        final Set<Long> blocks=new HashSet<>();
        final List<BlockPos> fires=new ArrayList<>();
        final Set<UUID> guards=new LinkedHashSet<>();
        long cooldown; boolean wasLit; int missingChecks;
        Camp(long key,int type,int tier,BlockPos center,BlockPos origin,Rotation rotation){
            this.key=key;this.type=type;this.tier=tier;this.center=center;this.origin=origin;this.rotation=rotation;
        }
        BlockPos at(int x,int y,int z){return transform(origin,rotation,new BlockPos(x,y,z));}
        String tag(){return "toi_camp_"+Long.toUnsignedString(key);}
        boolean lit(long now){return flames.values().stream().anyMatch(t->t>now);}
        CompoundTag save(){
            CompoundTag t=new CompoundTag();
            t.putLong("Key",key);t.putInt("Type",type);t.putInt("Tier",tier);t.putLong("Center",center.asLong());
            t.putLong("Origin",origin.asLong());t.putString("Rotation",rotation.name());t.putLong("Cooldown",cooldown);t.putBoolean("WasLit",wasLit);
            t.putLongArray("Blocks",blocks.stream().mapToLong(Long::longValue).toArray());
            t.putLongArray("Fires",fires.stream().mapToLong(BlockPos::asLong).toArray());
            ListTag f=new ListTag(),g=new ListTag();
            flames.forEach((pos,until)->{CompoundTag n=new CompoundTag();n.putLong("Pos",pos);n.putLong("Until",until);f.add(n);});
            for(UUID u:guards){CompoundTag n=new CompoundTag();n.putUUID("Id",u);g.add(n);}
            t.put("Flames",f);t.put("Guards",g);return t;
        }
        static Camp load(CompoundTag t){
            Camp c=new Camp(t.getLong("Key"),t.getInt("Type"),t.getInt("Tier"),BlockPos.of(t.getLong("Center")),
                    BlockPos.of(t.getLong("Origin")),Rotation.valueOf(t.getString("Rotation")));
            c.cooldown=t.getLong("Cooldown");c.wasLit=t.getBoolean("WasLit");
            for(long p:t.getLongArray("Blocks"))c.blocks.add(p);
            for(long p:t.getLongArray("Fires"))c.fires.add(BlockPos.of(p));
            ListTag f=t.getList("Flames",Tag.TAG_COMPOUND),g=t.getList("Guards",Tag.TAG_COMPOUND);
            for(int i=0;i<f.size();i++){CompoundTag n=f.getCompound(i);c.flames.put(n.getLong("Pos"),n.getLong("Until"));}
            for(int i=0;i<g.size();i++)if(g.getCompound(i).hasUUID("Id"))c.guards.add(g.getCompound(i).getUUID("Id"));
            return c;
        }
    }
    private void index(Camp c){camps.put(c.key,c);authored.addAll(c.blocks);c.flames.keySet().forEach(p->lamps.put(p,c));}
    private static ForestCamps load(CompoundTag t,HolderLookup.Provider registries){
        ForestCamps data=new ForestCamps();ListTag l=t.getList("Camps",Tag.TAG_COMPOUND);
        for(int i=0;i<l.size();i++)data.index(Camp.load(l.getCompound(i)));return data;
    }
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider registries){
        ListTag l=new ListTag();for(Camp c:camps.values())l.add(c.save());t.put("Camps",l);return t;
    }
    static void tick(ServerLevel level,InkLandsLayout layout,boolean canSpawn){
        ForestCamps data=of(level);long now=level.getGameTime();
        for(Camp c:data.camps.values()){
            data.flames(level,c,now);
            if(nearest(level,c.center)<=WAKE*WAKE)data.garrison(level,c,canSpawn);
        }
        // One successful placement max per second. Stable candidates never reroll after a visit.
        for(ServerPlayer p:level.players()){
            if(!p.isAlive()||p.isSpectator())continue;
            int gx=Math.floorDiv(p.getBlockX(),CELL),gz=Math.floorDiv(p.getBlockZ(),CELL);
            for(int x=gx-1;x<=gx+1;x++)for(int z=gz-1;z<=gz+1;z++){
                long k=key(x,z);
                if(data.camps.containsKey(k)||now<data.retry.getOrDefault(k,0L))continue;
                RandomSource r=rng(level,k);
                int cx=x*CELL+CELL/2+r.nextInt(2*JITTER+1)-JITTER,cz=z*CELL+CELL/2+r.nextInt(2*JITTER+1)-JITTER;
                int roll=r.nextInt(100),type=roll<35?0:roll<75?1:2;
                Rotation rot=Rotation.values()[r.nextInt(4)];
                BlockPos base=new BlockPos(cx,0,cz);
                double distance=nearest(level,base);
                if(distance>INSTALL_RADIUS*INSTALL_RADIUS||distance<24*24||!ModDimensions.chunksReady(level,base,20))continue;
                data.retry.put(k,now+200);
                // Try nearby ground if the initial site conflicts with a ruin, lake or steep terrain.
                for(int[] offset:new int[][]{{0,0},{18,0},{-18,0},{0,18},{0,-18},{18,18},{-18,-18}}){
                    BlockPos site=base.offset(offset[0],0,offset[1]);
                    if(!ModDimensions.chunksReady(level,site,20)||nearest(level,site)<24*24
                        ||layout.depthInside(site.getX(),site.getZ())<24||layout.nearRoad(site.getX(),site.getZ(),40)
                        ||layout.nearLake(site.getX(),site.getZ(),40)||layout.nearHerald(site.getX(),site.getZ(),64)
                        ||Ruins.nearStart(layout,site.getX(),site.getZ())||Sanctuary.near(layout,site.getX(),site.getZ(),48)
                        ||near(level,site,170)||BarricadeGates.nearGate(level,site,64))continue;
                    Camp c=data.install(level,layout,k,type,rot,site);
                    if(c!=null){data.index(c);data.retry.remove(k);data.setDirty();
                        TearsOfInk.LOGGER.info("Installed forest camp {} at {}",ASSETS[type],c.center);
                        data.garrison(level,c,canSpawn);return;}
                }
            }
        }
    }
    private static boolean naturalTree(BlockState s){
        var id=BuiltInRegistries.BLOCK.getKey(s.getBlock());
        return s.is(BlockTags.LEAVES)||(id.getNamespace().equals("tears-of-ink")&&(id.getPath().endsWith("_log")||id.getPath().endsWith("_wood"))&&!id.getPath().startsWith("stripped_"));
    }
    private static boolean ground(BlockState s){
        var id=BuiltInRegistries.BLOCK.getKey(s.getBlock());
        return s.is(ModBlocks.ASHEN_SOIL)||s.is(ModBlocks.ASH)||s.is(ModBlocks.ASPHODELITE_ROCK)
                ||s.is(BlockTags.BASE_STONE_OVERWORLD)||s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)||s.is(Blocks.GRAVEL)
                ||id.getNamespace().equals("tears-of-ink")&&id.getPath().endsWith("_ore");
    }
    private static boolean replaceable(BlockState s){return !s.hasBlockEntity()&&s.getFluidState().isEmpty()
            &&(s.isAir()||s.canBeReplaced()||ground(s)||naturalTree(s));}
    private static int surface(ServerLevel level,int x,int z){
        var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);
        if(chunk==null)return Integer.MIN_VALUE;
        int top=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
        for(int y=Math.min(top,level.getMaxBuildHeight()-1);y>=Math.max(level.getMinBuildHeight(),top-48);y--){
            BlockPos p=new BlockPos(x,y,z);BlockState s=chunk.getBlockState(p);
            if(!s.getFluidState().isEmpty()||(!replaceable(s)&&!s.isAir()))return Integer.MIN_VALUE;
            if(ground(s)&&s.isFaceSturdy(level,p,Direction.UP))return y;
        }
        return Integer.MIN_VALUE;
    }
    private static List<Block> palette(){
        List<Block> list=new ArrayList<>(List.of(Blocks.DIRT,Blocks.CAMPFIRE,Blocks.LAPIS_BLOCK,Blocks.GOLD_BLOCK));
        for(String p:PALETTE)list.add(BuiltInRegistries.BLOCK.get(TearsOfInk.id(p)));return list;
    }
    private Camp install(ServerLevel level,InkLandsLayout layout,long key,int type,Rotation rot,BlockPos site){
        var asset=level.getStructureManager().get(TearsOfInk.id(ASSETS[type]));
        if(asset.isEmpty())return null;StructureTemplate template=asset.get();var size=template.getSize();
        BlockPos pivot=new BlockPos(size.getX()/2,0,size.getZ()/2);
        List<Integer> heights=new ArrayList<>();
        for(int dx=-6;dx<=6;dx+=3)for(int dz=-6;dz<=6;dz+=3){
            int y=surface(level,site.getX()+dx,site.getZ()+dz);if(y==Integer.MIN_VALUE)return null;heights.add(y);
        }
        Collections.sort(heights);int floor=heights.get(heights.size()/2);
        if(!level.getBiome(site.atY(floor+1)).is(ModBiomes.PALE_FOREST)
                ||floor-24<level.getMinBuildHeight()||floor+size.getY()+4>=level.getMaxBuildHeight())return null;
        BlockPos center=site.atY(floor);
        BlockPos origin=center.subtract(StructureTemplate.transform(pivot,Mirror.NONE,rot,BlockPos.ZERO));
        var settings=new StructurePlaceSettings().setRotation(rot).setIgnoreEntities(true);
        Map<BlockPos,BlockState> art=new HashMap<>();
        for(Block b:palette())for(var info:template.filterBlocks(origin,settings,b))art.put(info.pos(),info.state());
        List<BlockPos> markers=art.entrySet().stream().filter(e->e.getValue().is(Blocks.LAPIS_BLOCK)).map(Map.Entry::getKey).sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
        List<BlockPos> gold=art.entrySet().stream().filter(e->e.getValue().is(Blocks.GOLD_BLOCK)).map(Map.Entry::getKey).toList();
        List<BlockPos> light=art.entrySet().stream().filter(e->e.getValue().is(ModBlocks.INK_LANTERN)).map(Map.Entry::getKey).toList();
        if(art.size()!=COUNTS[type]||markers.size()!=(type==0?1:type==1?3:2)||gold.size()!=(type==1?1:0)||light.size()!=(type==0?4:type==1?1:2))return null;
        Map<BlockPos,BlockState> changes=new LinkedHashMap<>();
        // Adapt only the actual occupied bounding rectangle (not unused export margins).
        int minX=art.keySet().stream().mapToInt(BlockPos::getX).min().orElseThrow(),maxX=art.keySet().stream().mapToInt(BlockPos::getX).max().orElseThrow();
        int minZ=art.keySet().stream().mapToInt(BlockPos::getZ).min().orElseThrow(),maxZ=art.keySet().stream().mapToInt(BlockPos::getZ).max().orElseThrow();
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
            BlockPos p=new BlockPos(x,floor,z);int y=surface(level,x,z);
            if(y==Integer.MIN_VALUE||Math.abs(y-floor)>18||!level.getWorldBorder().isWithinBounds(p)
                    ||!level.getBiome(p.above()).is(ModBiomes.PALE_FOREST))return null;
            if(!replaceable(level.getBlockState(p)))return null;
            changes.put(p,ModBlocks.ASHEN_SOIL.defaultBlockState());
            for(int f=floor-1;f>=floor-20;f--){
                BlockPos at=p.atY(f);BlockState old=level.getBlockState(at);
                if(ground(old)&&old.isFaceSturdy(level,at,Direction.UP))break;
                if(!replaceable(old)||f==floor-20)return null;
                changes.put(at,ModBlocks.ASPHODELITE_ROCK.defaultBlockState());
            }
            // Open the occupied envelope and a small roof margin; no sky-high quarry.
            for(int f=floor+1;f<=floor+size.getY()+2;f++){
                BlockPos at=p.atY(f);BlockState old=level.getBlockState(at);
                if(!replaceable(old))return null;
                if(!old.isAir())changes.put(at,Blocks.AIR.defaultBlockState());
            }
        }
        var box=new AABB(minX-1,floor-20,minZ-1,maxX+2,floor+size.getY()+3,maxZ+2);
        if(!level.getEntitiesOfClass(LivingEntity.class,box,e->e.isAlive()&&!e.isSpectator()).isEmpty())return null;
        Map<BlockPos,BlockState> before=new LinkedHashMap<>();
        changes.keySet().forEach(p->before.put(p,level.getBlockState(p)));
        art.keySet().forEach(p->before.putIfAbsent(p,level.getBlockState(p)));
        int tier=SquadPatrols.tier(layout,center.getX(),center.getZ());
        Camp c=new Camp(key,type,tier,center,origin,rot);
        try{
            for(var e:changes.entrySet())if(!level.getBlockState(e.getKey()).equals(e.getValue())&&!level.setBlock(e.getKey(),e.getValue(),Block.UPDATE_CLIENTS))throw new IllegalStateException("terrain");
            RandomSource random=rng(level,key);
            if(!template.placeInWorld(level,origin,origin,settings,random,Block.UPDATE_CLIENTS))throw new IllegalStateException("template");
            for(var e:art.entrySet()){
                BlockPos p=e.getKey();
                if(!level.getBlockState(p).is(e.getValue().getBlock()))throw new IllegalStateException("incomplete camp");
                if(e.getValue().is(Blocks.DIRT))level.setBlock(p,ModBlocks.ASHEN_SOIL.defaultBlockState(),Block.UPDATE_CLIENTS);
                else c.blocks.add(p.asLong());
                if(e.getValue().is(Blocks.CAMPFIRE))c.fires.add(p);
            }
            for(BlockPos p:gold){level.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);c.blocks.remove(p.asLong());}
            List<BlockPos> selected=new ArrayList<>(markers);
            for(int i=selected.size()-1;i>0;i--)Collections.swap(selected,i,random.nextInt(i+1));
            int count=type==0?1:type==1?1+random.nextInt(2):2;
            ResourceKey<LootTable> loot=ResourceKey.create(Registries.LOOT_TABLE,Ruins.GATE_LOOT[tier]);
            for(int i=0;i<selected.size();i++){
                BlockPos p=selected.get(i);
                if(i<count){
                    if(!level.setBlock(p,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,rot.rotate(Direction.NORTH)),Block.UPDATE_CLIENTS))throw new IllegalStateException("chest");
                    RandomizableContainer.setBlockEntityLootTable(level,random,p,loot);
                }else{level.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);c.blocks.remove(p.asLong());}
            }
            for(BlockPos p:light){
                BlockState s=level.getBlockState(p);
                if(s.getValue(FogClearing.LIT))level.setBlock(p,s.setValue(FogClearing.LIT,false),Block.UPDATE_ALL);
                c.flames.put(p.asLong(),0L);
            }
            return c;
        }catch(RuntimeException ex){
            List<Map.Entry<BlockPos,BlockState>> rollback=new ArrayList<>(before.entrySet());Collections.reverse(rollback);
            for(var e:rollback)level.setBlock(e.getKey(),e.getValue(),Block.UPDATE_CLIENTS);
            TearsOfInk.LOGGER.warn("Forest camp placement rolled back at {}",center,ex);return null;
        }
    }
    private void flames(ServerLevel level,Camp c,long now){
        for(var e:c.flames.entrySet()){
            BlockPos p=BlockPos.of(e.getKey());var chunk=level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4);
            if(chunk==null)continue;BlockState s=chunk.getBlockState(p);long until=e.getValue();
            if(!s.is(ModBlocks.INK_LANTERN)||!s.getValue(FogClearing.LIT)){
                if(until!=0){e.setValue(0L);setDirty();}continue;
            }
            if(until==0){e.setValue(now+BarricadeGates.FLAME_TICKS);setDirty();}
            else if(until<=now){
                level.setBlock(p,s.setValue(FogClearing.LIT,false),Block.UPDATE_ALL);
                level.playSound(null,p,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.6f,.9f);
                e.setValue(0L);setDirty();
            }
        }
        boolean lit=c.lit(now);
        if(lit!=c.wasLit){if(!lit)c.cooldown=now;c.wasLit=lit;setDirty();}
    }
    static boolean canStand(ServerLevel level,PaleSoldier mob,BlockPos p){
        if(!ModDimensions.chunksReady(level,p,2)||!level.getFluidState(p).isEmpty()
                ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                ||level.getBlockState(p).is(Blocks.CAMPFIRE)||level.getBlockState(p.below()).is(Blocks.CAMPFIRE))return false;
        Vec3 delta=Vec3.atBottomCenterOf(p).subtract(mob.position());
        return level.noCollision(mob,mob.getBoundingBox().move(delta));
    }
    private void garrison(ServerLevel level,Camp c,boolean canSpawn){
        long now=level.getGameTime();
        if(!ModDimensions.chunksReady(level,c.center,48)||!level.isPositionEntityTicking(c.center))return;
        // Reconcile only in an entirely ticking local area. Do not treat unloaded guards as dead.
        boolean ticking=true;
        for(int dx:new int[]{-40,0,40})for(int dz:new int[]{-40,0,40})ticking&=level.isPositionEntityTicking(c.center.offset(dx,0,dz));
        if(ticking){
            for(PaleSoldier m:level.getEntitiesOfClass(PaleSoldier.class,new AABB(c.center).inflate(40),
                    e->e.isAlive()&&e.getTags().contains(c.tag())))if(c.guards.add(m.getUUID()))setDirty();
            if(c.guards.stream().anyMatch(u->level.getEntity(u)==null||!level.getEntity(u).isAlive())){
                if(++c.missingChecks>=5){c.missingChecks=0;if(c.guards.removeIf(u->level.getEntity(u)==null||!level.getEntity(u).isAlive())){
                    if(c.guards.isEmpty())c.cooldown=now+Outposts.RESPAWN;setDirty();}}
            }else c.missingChecks=0;
        }else c.missingChecks=0;
        if(!canSpawn||!c.guards.isEmpty()||c.lit(now)||now<c.cooldown||nearest(level,c.center)<12*12)return;
        int tier=c.tier;
        boolean easy=level.getDifficulty()==Difficulty.EASY;
        int spears=c.type==2?(tier==0?2:tier==1?2:3):tier+1;
        int shields=c.type==0?0:(tier==0||easy?0:1);
        int archers=c.type==0?tier+1:c.type==1?1:tier==2?2:1;
        List<PaleSoldier> plan=new ArrayList<>();List<AABB> boxes=new ArrayList<>();UUID squad=UUID.randomUUID();
        for(int i=0;i<archers+spears+shields;i++){
            boolean archer=i<archers,elevated=archer&&c.type!=2;
            PaleSoldier mob=archer?ModMobs.PALE_CROSSBOWMAN.create(level):i<archers+spears?ModMobs.PALE_SPEARMAN.create(level):ModMobs.PALE_SHIELDBEARER.create(level);
            if(mob==null)return;
            BlockPos at=null,rest=c.center.above();
            if(elevated){
                // Exact authored gold marker for the campfire archer, three clear full-block roof slots for tower.
                at=c.type==1?c.at(13,5,13):switch(i){case 0->c.at(5,15,6);case 1->c.at(7,15,8);default->c.at(5,15,8);};
                if(!canStand(level,mob,at))return;
                rest=at;
            }else{
                if(!c.fires.isEmpty())rest=c.fires.get((i-archers+c.fires.size()*4)%c.fires.size());
                List<BlockPos> candidates=new ArrayList<>();
                for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++){
                    BlockPos p=c.center.offset(dx,1,dz);
                    if(horizontal(c.center,p)>15*15)continue;
                    for(int dy=0;dy<=1;dy++)candidates.add(p.above(dy));
                }
                BlockPos focus=rest;
                candidates.sort(Comparator.comparingDouble(p->Math.abs(Math.sqrt(horizontal(p,focus))-3.5)+.02*horizontal(p,c.center)));
                for(BlockPos p:candidates){
                    if(!canStand(level,mob,p))continue;
                    if(level.players().stream().anyMatch(player->player.isAlive()&&!player.isSpectator()&&player.distanceToSqr(Vec3.atBottomCenterOf(p))<9*9))continue;
                    AABB box=mob.getBoundingBox().move(Vec3.atBottomCenterOf(p).subtract(mob.position()));
                    if(boxes.stream().anyMatch(b->b.inflate(.25).intersects(box)))continue;
                    at=p;break;
                }
                if(at==null)return;
            }
            mob.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,level.random.nextFloat()*360,0);
            if(boxes.stream().anyMatch(mob.getBoundingBox()::intersects)
                    ||level.players().stream().anyMatch(p->p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(mob)<9*9))return;
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(at),MobSpawnType.EVENT,null);
            mob.setPower(SquadTuning.TIER_POWER[tier]);mob.setCampHome(c.center,at,rest,elevated);
            mob.joinSquad(squad,mob.getUUID());mob.addTag(c.tag());
            boxes.add(mob.getBoundingBox());plan.add(mob);
        }
        List<PaleSoldier> added=new ArrayList<>();
        for(PaleSoldier m:plan){
            if(!level.addFreshEntity(m)){for(PaleSoldier a:added)a.discard();return;}
            added.add(m);
        }
        for(PaleSoldier m:added)c.guards.add(m.getUUID());setDirty();
    }
}
