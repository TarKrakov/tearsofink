package com.tarkrakov.tearsofink.entity.patrol;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.lantern.FogClearing;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
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
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
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

/**
 * D-084: convert the original barricade only when its whole site is already FULL.
 * No new structure starts, generation tickets, or temple-plan changes. A single
 * world-local ledger owns the exact protected blocks, chest roll, lamps and guards.
 */
public final class BarricadeGates extends SavedData {
    public static final int FLAME_TICKS = 5 * 60 * 20;
    public static final int SAFE_RADIUS = 24, PATROL_BUFFER = 40, GUARD_LEASH = 36;
    private static final int MAX_FILL = 12, MAX_CUT = 8, MAX_TREE_CLEAR = 24;
    private static final int INSTALL_RETRY = 20 * 10;
    private static final Factory<BarricadeGates> FACTORY = new Factory<>(BarricadeGates::new, BarricadeGates::load, null);
    private static final String[][] SIDES = {{"TS","SC"},{"TSC","SC"},{"TSC","TSC"}};
    private final Map<Integer,Gate> gates = new HashMap<>();
    private final Set<Long> protectedBlocks = new HashSet<>();
    private final Map<Long,Gate> lamps = new HashMap<>();
    // Retry clock is transient; absence of a gate is never persisted as a successful conversion.
    private final Map<Integer,Long> retryAt = new HashMap<>();

    static BarricadeGates of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY,"toi_barricade_gates");
    }
    static void initProtection() {
        // Cancel before vanilla handles loot. Applies in creative too; other structures unchanged.
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> !protectedAt(world,pos));
    }
    private static boolean serverThread(ServerLevel level) {
        return ModDimensions.isLands(level) && level.getServer().isSameThread();
    }
    public static boolean protectedAt(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && serverThread(server) && of(server).protectedBlocks.contains(pos.asLong());
    }
    public static boolean isGateLantern(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && serverThread(server) && of(server).lamps.containsKey(pos.asLong());
    }
    /** Only the normal successful player ignition starts a fresh timed flame. */
    public static void ignited(ServerLevel level, BlockPos pos) {
        if (!serverThread(level)) return;
        BarricadeGates data=of(level);
        Gate g=data.lamps.get(pos.asLong());
        if (g!=null) {
            g.flames.put(pos.asLong(),level.getGameTime()+FLAME_TICKS);
            g.wasLit=true;
            data.setDirty();
        }
    }
    static boolean nearGate(ServerLevel level, BlockPos pos, int radius) {
        if (!ModDimensions.isLands(level)) return false;
        for (Gate g:of(level).gates.values()) if (horizontalSq(g.center,pos)<=radius*radius) return true;
        return false;
    }
    public static boolean suppressesSpawns(ServerLevel level, BlockPos pos) {
        if (!ModDimensions.isLands(level)) return false;
        for (Gate g:of(level).gates.values()) {
            if (Math.abs(g.center.getY()-pos.getY())>12 || horizontalSq(g.center,pos)>SAFE_RADIUS*SAFE_RADIUS) continue;
            if (g.isLit(level.getGameTime())) return true;
        }
        return false;
    }
    private static double horizontalSq(BlockPos a,BlockPos b) {
        double x=a.getX()-b.getX(),z=a.getZ()-b.getZ();
        return x*x+z*z;
    }
    static int id(int road,int number) { return road*100+number; }
    Gate get(int road,int number) { return gates.get(id(road,number)); }

    static final class Gate {
        final int id;
        final BlockPos center;
        final Set<Long> blocks=new HashSet<>();
        final Map<Long,Long> flames=new LinkedHashMap<>();
        final Set<UUID> guards=new LinkedHashSet<>();
        boolean wasLit;
        long cooldown;
        int missingChecks;
        Gate(int id,BlockPos center) { this.id=id; this.center=center.immutable(); }
        boolean isLit(long now) { return flames.values().stream().anyMatch(until -> until>now); }
        String guardTag() { return "toi_gate_"+id; }
        CompoundTag save() {
            CompoundTag t=new CompoundTag();
            t.putInt("Id",id); t.putLong("Center",center.asLong());
            t.putLongArray("Blocks",blocks.stream().mapToLong(Long::longValue).toArray());
            ListTag f=new ListTag(), m=new ListTag();
            flames.forEach((pos,until)->{CompoundTag c=new CompoundTag();c.putLong("Pos",pos);c.putLong("Until",until);f.add(c);});
            for(UUID u:guards){CompoundTag c=new CompoundTag();c.putUUID("Id",u);m.add(c);}
            t.put("Flames",f);t.put("Guards",m);t.putBoolean("WasLit",wasLit);t.putLong("Cooldown",cooldown);
            return t;
        }
        static Gate load(CompoundTag t) {
            Gate g=new Gate(t.getInt("Id"),BlockPos.of(t.getLong("Center")));
            for(long p:t.getLongArray("Blocks"))g.blocks.add(p);
            ListTag f=t.getList("Flames",Tag.TAG_COMPOUND),m=t.getList("Guards",Tag.TAG_COMPOUND);
            for(int i=0;i<f.size();i++){CompoundTag c=f.getCompound(i);g.flames.put(c.getLong("Pos"),c.getLong("Until"));}
            for(int i=0;i<m.size();i++)if(m.getCompound(i).hasUUID("Id"))g.guards.add(m.getCompound(i).getUUID("Id"));
            g.wasLit=t.getBoolean("WasLit");g.cooldown=t.getLong("Cooldown");
            return g;
        }
    }
    private void index(Gate g) {
        gates.put(g.id,g); protectedBlocks.addAll(g.blocks);
        for(long p:g.flames.keySet())lamps.put(p,g);
    }
    private static BarricadeGates load(CompoundTag tag, HolderLookup.Provider registries) {
        BarricadeGates data=new BarricadeGates();
        ListTag list=tag.getList("Gates",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)data.index(Gate.load(list.getCompound(i)));
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list=new ListTag();
        for(Gate g:gates.values())list.add(g.save());
        tag.put("Gates",list);
        return tag;
    }
    /** Time progresses even while a lamp's chunk is unloaded, but no block is queried then. */
    void tickFlames(ServerLevel level) {
        long now=level.getGameTime();
        for(Gate g:gates.values()) {
            for(var entry:g.flames.entrySet()) {
                BlockPos p=BlockPos.of(entry.getKey());
                var chunk=level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4);
                if(chunk==null)continue;
                BlockState s=chunk.getBlockState(p);
                if(!s.is(ModBlocks.INK_LANTERN)||!s.getValue(FogClearing.LIT)) {
                    if(entry.getValue()!=0){entry.setValue(0L);setDirty();}
                    continue;
                }
                long until=entry.getValue();
                if(until==0) { entry.setValue(now+FLAME_TICKS);setDirty(); }
                else if(until<=now) {
                    level.setBlock(p,s.setValue(FogClearing.LIT,false),Block.UPDATE_ALL);
                    level.playSound(null,p,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.6f,.9f);
                    entry.setValue(0L);setDirty();
                }
            }
            boolean lit=g.isLit(now);
            if(lit!=g.wasLit) {
                // When the last temporary flame expires, a wiped garrison may return.
                if(!lit)g.cooldown=now;
                g.wasLit=lit;setDirty();
            }
        }
    }
    void died(ServerLevel level,UUID member) {
        for(Gate g:gates.values())if(g.guards.remove(member)) {
            if(g.guards.isEmpty())g.cooldown=level.getGameTime()+Outposts.RESPAWN;
            setDirty();
        }
    }
    private static final BlockPos PIVOT=new BlockPos(4,0,7);
    private static Rotation rotation(double[] direction) {
        return Math.abs(direction[0])>=Math.abs(direction[1])
                ? (direction[0]>=0?Rotation.NONE:Rotation.CLOCKWISE_180)
                : (direction[1]>=0?Rotation.CLOCKWISE_90:Rotation.COUNTERCLOCKWISE_90);
    }
    private static BlockPos transform(BlockPos origin,Rotation rotation,int x,int y,int z) {
        return origin.offset(StructureTemplate.transform(new BlockPos(x,y,z),net.minecraft.world.level.block.Mirror.NONE,rotation,PIVOT));
    }
    private static int roadY(ServerLevel level,int x,int z) {
        var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);
        if(chunk==null)return Integer.MIN_VALUE;
        int top=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
        for(int y=top;y>=Math.max(top-64,level.getMinBuildHeight());y--) {
            if(chunk.getBlockState(new BlockPos(x,y,z)).is(ModBlocks.ASHEN_PATH))return y;
        }
        return Integer.MIN_VALUE;
    }
    private static boolean oldBarrier(InkLandsLayout layout,int road,double d,BlockPos p,BlockState s,int floor) {
        if(!(s.is(ModBlocks.PALE_FENCE)||s.is(ModBlocks.PALE_LOG))||p.getY()<floor+1||p.getY()>floor+2)return false;
        double[] hit=layout.roadNear(p.getX(),p.getZ(),0);
        return hit!=null&&(int)hit[0]==road&&Math.abs(hit[1]-d)<.75;
    }
    private static boolean terrain(BlockState s) {
        return s.is(ModBlocks.ASHEN_SOIL)||s.is(ModBlocks.BLOOD_SOIL)||s.is(ModBlocks.TAR_SOIL)
                ||s.is(ModBlocks.ASH)||s.is(ModBlocks.ASPHODELITE_ROCK)||s.is(ModBlocks.ASHEN_PATH)
                ||s.is(Blocks.STONE)||s.is(Blocks.DEEPSLATE)||s.is(Blocks.TUFF)||s.is(Blocks.DIRT)
                ||s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.GRAVEL)||s.is(Blocks.SAND);
    }
    private static boolean naturalTree(BlockState s) {
        if(s.is(BlockTags.LEAVES))return true;
        var id=BuiltInRegistries.BLOCK.getKey(s.getBlock());
        return id.getNamespace().equals("tears-of-ink")&&id.getPath().endsWith("_log")&&!id.getPath().startsWith("stripped_");
    }
    private static boolean replaceable(BlockState s) {
        return !s.hasBlockEntity()&&s.getFluidState().isEmpty()&&(s.isAir()||s.canBeReplaced()||terrain(s)||naturalTree(s));
    }
    private static List<Block> palette() {
        return List.of(Blocks.LAPIS_BLOCK,ModBlocks.STRIPPED_PALE_WOOD,ModBlocks.STRIPPED_PALE_LOG,
            ModBlocks.OVERGROWN_RUIN_BRICK_WALL,ModBlocks.CRACKED_RUIN_BRICK_WALL,
            ModBlocks.OVERGROWN_RUIN_BRICK_SLAB,ModBlocks.CRACKED_RUIN_BRICK_SLAB,
            ModBlocks.OVERGROWN_RUIN_BRICK_STAIRS,ModBlocks.PALE_FENCE,ModBlocks.PALE_STAIRS,
            ModBlocks.PALE_SLAB,ModBlocks.INK_LANTERN);
    }
    /** All edits preflighted before the first write. Refuse unrelated constructed blocks or BEs. */
    Gate tryInstall(ServerLevel level,InkLandsLayout layout,int road,int number) {
        int id=id(road,number);
        if(gates.containsKey(id))return gates.get(id);
        long now=level.getGameTime();
        if(now<retryAt.getOrDefault(id,0L))return null;
        double d=layout.outpostDistance(number);
        double[] p=layout.roadPoint(road,d);
        BlockPos column=BlockPos.containing(p[0],0,p[1]);
        if(!ModDimensions.chunksReady(level,column,16))return null;
        retryAt.put(id,now+INSTALL_RETRY);
        // There was no old barricade on bridge decks. Never build across a bridge or temple.
        if(layout.roadBridge(road,d)||Sanctuary.near(layout,column.getX(),column.getZ(),12))return null;
        // A gate can shift a maximum three blocks along the same barricade site to
        // avoid an existing roadside pole, but may not leave any legacy spike behind.
        for(double delta:new double[]{0,1.5,-1.5,3,-3}) {
            double[] candidate=layout.roadPoint(road,d+delta);
            BlockPos base=BlockPos.containing(candidate[0],0,candidate[1]);
            if(!ModDimensions.chunksReady(level,base,16))continue;
            Gate installed=installAt(level,layout,road,d,id,base,column);
            if(installed!=null)return installed;
        }
        return null;
    }
    private Gate installAt(ServerLevel level,InkLandsLayout layout,int road,double d,int id,BlockPos column,BlockPos oldCenter) {
        double[] p=layout.roadPoint(road,d);
        int roadY=roadY(level,column.getX(),column.getZ());
        if(roadY==Integer.MIN_VALUE)return null;
        BlockPos center=column.atY(roadY+1);
        if(center.getY()-MAX_FILL<level.getMinBuildHeight()||center.getY()+MAX_TREE_CLEAR>=level.getMaxBuildHeight())return null;
        Rotation rot=rotation(layout.roadDirection(road,d));
        BlockPos origin=center.subtract(PIVOT);
        Optional<StructureTemplate> asset=level.getStructureManager().get(TearsOfInk.id("barricade_gate"));
        if(asset.isEmpty())return null;
        StructureTemplate template=asset.get();
        if(template.getSize().getX()!=9||template.getSize().getY()!=8||template.getSize().getZ()!=16)return null;
        StructurePlaceSettings settings=new StructurePlaceSettings().setRotation(rot).setRotationPivot(PIVOT).setIgnoreEntities(true);
        Map<BlockPos,BlockState> authored=new HashMap<>();
        for(Block b:palette())for(var info:template.filterBlocks(origin,settings,b))authored.put(info.pos(),info.state());
        List<BlockPos> markers=template.filterBlocks(origin,settings,Blocks.LAPIS_BLOCK).stream().map(StructureTemplate.StructureBlockInfo::pos)
                .sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
        List<BlockPos> light=template.filterBlocks(origin,settings,ModBlocks.INK_LANTERN).stream().map(StructureTemplate.StructureBlockInfo::pos).toList();
        if(authored.size()!=246||markers.size()!=4||light.size()!=4||light.stream().anyMatch(pos->authored.get(pos).getValue(FogClearing.LIT)))return null;

        // Keep pre-existing road lamps untouched. A collision with actual art aborts conversion.
        Set<Long> reservedColumns=new HashSet<>();
        int k=(int)Math.floor(d/InkLandsLayout.ROAD_LANTERN_SPACING);
        for(int n=k-1;n<=k+1;n++) {
            int[] post=layout.roadLanternPost(road,n);
            if(post==null)continue;
            boolean conflicts=authored.keySet().stream().anyMatch(pos->pos.getX()==post[0]&&pos.getZ()==post[1]);
            if(conflicts)return null;
            reservedColumns.add(BlockPos.asLong(post[0],0,post[1]));
        }

        Map<BlockPos,BlockState> changes=new LinkedHashMap<>();
        Set<Long> protection=new HashSet<>();
        boolean hasOldBarrier=false;
        for(int x=0;x<9;x++)for(int z=1;z<15;z++) {
            BlockPos floor=transform(origin,rot,x,-1,z);
            if(!level.getWorldBorder().isWithinBounds(floor))return null;
            if(reservedColumns.contains(BlockPos.asLong(floor.getX(),0,floor.getZ())))continue;
            double[] hit=layout.roadNear(floor.getX(),floor.getZ(),0);
            boolean lane=hit!=null&&(int)hit[0]==road;
            int previousRoad=lane?roadY(level,floor.getX(),floor.getZ()):Integer.MIN_VALUE;
            if(lane&&previousRoad==Integer.MIN_VALUE)return null;
            // The empty entry/exit row can absorb one block of the old road's grade.
            if((x==0||x==8)&&lane) {
                if(Math.abs(previousRoad-floor.getY())>2)return null;
                floor=floor.atY(Math.max(floor.getY()-1,Math.min(floor.getY()+1,previousRoad)));
            }
            BlockState existing=level.getBlockState(floor);
            boolean oldFloor=previousRoad!=Integer.MIN_VALUE&&oldBarrier(layout,road,d,floor,existing,previousRoad);
            hasOldBarrier|=oldFloor;
            if(!oldFloor&&!replaceable(existing))return null;
            if(existing.hasBlockEntity())return null;
            BlockState wanted=(lane||z==7||z==8)?ModBlocks.ASHEN_PATH.defaultBlockState():ModBlocks.ASHEN_SOIL.defaultBlockState();
            // Always keep a dry solid pad; fill a modest unsupported column, not a bridge.
            changes.put(floor,wanted);
            protection.add(floor.asLong());
            boolean supported=false;
            for(int i=1;i<=MAX_FILL;i++) {
                BlockPos below=floor.below(i);
                BlockState old=level.getBlockState(below);
                if(old.hasBlockEntity()||!old.getFluidState().isEmpty())return null;
                if(terrain(old)&&old.isFaceSturdy(level,below,Direction.UP)){protection.add(below.asLong());supported=true;break;}
                boolean oldSupport=previousRoad!=Integer.MIN_VALUE&&oldBarrier(layout,road,d,below,old,previousRoad);
                hasOldBarrier|=oldSupport;
                if(!oldSupport&&!replaceable(old))return null;
                changes.put(below,ModBlocks.ASPHODELITE_ROCK.defaultBlockState());protection.add(below.asLong());
            }
            if(!supported)return null;
            // Clear only the compact site and any tree column crossing its roof.
            int top=level.getChunkSource().getChunkNow(floor.getX()>>4,floor.getZ()>>4)
                    .getHeight(Heightmap.Types.WORLD_SURFACE,floor.getX()&15,floor.getZ()&15);
            int clearTo=Math.max(center.getY()+7,top);
            if(clearTo>center.getY()+MAX_TREE_CLEAR)return null;
            for(int y=floor.getY()+1;y<=clearTo;y++) {
                BlockPos at=floor.atY(y);
                BlockState old=level.getBlockState(at);
                boolean placeholder=previousRoad!=Integer.MIN_VALUE&&oldBarrier(layout,road,d,at,old,previousRoad);
                hasOldBarrier|=placeholder;
                if(old.isAir())continue;
                if(!placeholder&&!replaceable(old))return null;
                if(terrain(old)&&y>center.getY()+MAX_CUT)return null;
                changes.put(at,Blocks.AIR.defaultBlockState());
            }
        }
        // Do not overwrite a modified/missing old site, or place a floating partial gate.
        if(!hasOldBarrier)return null;
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++) {
            int xx=oldCenter.getX()+dx,zz=oldCenter.getZ()+dz;
            double[] hit=layout.roadNear(xx,zz,0);
            if(hit==null||(int)hit[0]!=road||Math.abs(hit[1]-d)>=.75)continue;
            int floor=roadY(level,xx,zz);
            if(floor==Integer.MIN_VALUE)continue;
            for(int dy=1;dy<=2;dy++) {
                BlockPos at=new BlockPos(xx,floor+dy,zz);
                if(oldBarrier(layout,road,d,at,level.getBlockState(at),floor)&&!changes.containsKey(at))return null;
            }
        }
        for(BlockPos p0:authored.keySet()) {
            BlockState old=level.getBlockState(p0);
            if(old.hasBlockEntity()||(!old.isAir()&&!changes.containsKey(p0)))return null;
            protection.add(p0.asLong());
        }
        AABB zone=new AABB(center.getX()-9,center.getY()-2,center.getZ()-9,center.getX()+10,center.getY()+9,center.getZ()+10);
        if(!level.getEntitiesOfClass(Player.class,zone,e->e.isAlive()&&!e.isSpectator()).isEmpty())return null;
        for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,zone,LivingEntity::isAlive)) {
            if(authored.keySet().stream().anyMatch(at->e.getBoundingBox().intersects(new AABB(at))))return null;
        }

        // Snapshot all touched states. There were no foreign block entities in the edit set.
        Map<BlockPos,BlockState> before=new LinkedHashMap<>();
        changes.keySet().forEach(at->before.put(at,level.getBlockState(at)));
        authored.keySet().forEach(at->before.putIfAbsent(at,level.getBlockState(at)));
        Gate g=new Gate(id,center);
        try {
            for(var edit:changes.entrySet())if(!level.getBlockState(edit.getKey()).equals(edit.getValue())
                    &&!level.setBlock(edit.getKey(),edit.getValue(),Block.UPDATE_CLIENTS))throw new IllegalStateException("gate terrain write");
            RandomSource fixed=RandomSource.create(level.getSeed()^center.asLong()*31L^0x47415445L);
            if(!template.placeInWorld(level,origin,origin,settings,fixed,Block.UPDATE_CLIENTS))throw new IllegalStateException("gate template placement");
            for(var entry:authored.entrySet())if(!level.getBlockState(entry.getKey()).is(entry.getValue().getBlock()))
                throw new IllegalStateException("incomplete gate template");
            List<BlockPos> selected=new ArrayList<>(markers);
            for(int i=selected.size()-1;i>0;i--)Collections.swap(selected,i,fixed.nextInt(i+1));
            int count=1+fixed.nextInt(3);
            ResourceKey<LootTable> loot=ResourceKey.create(Registries.LOOT_TABLE,Ruins.CHEST_LOOT[SquadPatrols.tier(layout,p[0],p[1])]);
            for(int i=0;i<selected.size();i++) {
                BlockPos at=selected.get(i);
                if(i<count) {
                    // Face the nearest side of the approach, not the tower wall.
                    boolean inner=at.equals(transform(origin,rot,1,0,5))||at.equals(transform(origin,rot,1,0,10));
                    Direction face=rot.rotate(inner?Direction.WEST:Direction.EAST);
                    if(!level.setBlock(at,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,face),Block.UPDATE_CLIENTS))
                        throw new IllegalStateException("gate chest");
                    RandomizableContainer.setBlockEntityLootTable(level,fixed,at,loot);
                } else {
                    level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
                    protection.remove(at.asLong());
                }
            }
            for(BlockPos at:light) {
                if(!level.getBlockState(at).is(ModBlocks.INK_LANTERN)||level.getBlockState(at).getValue(FogClearing.LIT))
                    throw new IllegalStateException("gate lantern");
                g.flames.put(at.asLong(),0L);
            }
            g.blocks.addAll(protection);
        } catch(RuntimeException ex) {
            // Do not leave an edited road or an incomplete gate if a placement hook rejects a write.
            List<Map.Entry<BlockPos,BlockState>> rollback=new ArrayList<>(before.entrySet());
            Collections.reverse(rollback);
            for(var edit:rollback)level.setBlock(edit.getKey(),edit.getValue(),Block.UPDATE_CLIENTS);
            TearsOfInk.LOGGER.warn("Gate {} conversion rolled back",id,ex);
            return null;
        }
        index(g);setDirty();retryAt.remove(id);
        TearsOfInk.LOGGER.info("Installed roadside gate {} at {}", id, center);
        return g;
    }

    /** Adopt old outpost members across an update, never a passing road patrol. */
    void adoptLegacy(ServerLevel level,Gate g,int road,double d,Collection<UUID> known) {
        if(!g.guards.isEmpty())return;
        for(PaleSoldier mob:level.getEntitiesOfClass(PaleSoldier.class,new AABB(g.center).inflate(32),PaleSoldier::isAlive)) {
            if(!known.contains(mob.getUUID())&&(mob.patrolRoad!=road||Math.abs(mob.patrolFrom-(d-6))>.01||Math.abs(mob.patrolTo-(d-1))>.01))continue;
            mob.addTag(g.guardTag());mob.setGateHome(mob.blockPosition());g.guards.add(mob.getUUID());
        }
        if(!g.guards.isEmpty())setDirty();
    }
    private void reconcile(ServerLevel level,Gate g) {
        if(!ModDimensions.chunksReady(level,g.center,GUARD_LEASH+24)||!level.isPositionEntityTicking(g.center)) {g.missingChecks=0;return;}
        // Recovery of existing persistent soldiers if entity and world data were saved on different ticks.
        for(PaleSoldier mob:level.getEntitiesOfClass(PaleSoldier.class,new AABB(g.center).inflate(GUARD_LEASH+24),
                p->p.isAlive()&&p.getTags().contains(g.guardTag()))) {
            if(g.guards.add(mob.getUUID()))setDirty();
        }
        boolean missing=g.guards.stream().anyMatch(uuid->level.getEntity(uuid)==null||!level.getEntity(uuid).isAlive());
        if(!missing){g.missingChecks=0;return;}
        // Wait over several scans for entity loading. Ordinary chunk unload is not a death.
        if(++g.missingChecks<5)return;
        g.missingChecks=0;
        if(g.guards.removeIf(uuid->level.getEntity(uuid)==null||!level.getEntity(uuid).isAlive()))setDirty();
    }
    void garrison(ServerLevel level,InkLandsLayout layout,int road,double d,Gate g,double nearest,boolean canSpawn) {
        if(nearest>Outposts.WAKE)return;
        reconcile(level,g);
        if(!canSpawn||!g.guards.isEmpty()||g.isLit(level.getGameTime())||level.getGameTime()<g.cooldown)return;
        if(!ModDimensions.chunksReady(level,g.center,24))return;
        int tier=Math.max(0,Math.min(SIDES.length-1,SquadPatrols.tier(layout,g.center.getX(),g.center.getZ())));
        UUID squad=UUID.randomUUID();
        List<PaleSoldier> planned=new ArrayList<>();
        List<AABB> occupied=new ArrayList<>();
        for(int side=0;side<2;side++) {
            int sign=side==0?1:-1;
            String lineup=SIDES[tier][side];
            UUID leader=null;
            for(int i=0;i<lineup.length();i++) {
                char role=lineup.charAt(i);
                EntityType<? extends PaleSoldier> type=switch(role) {
                    case 'T'->ModMobs.PALE_SHIELDBEARER;
                    case 'C'->ModMobs.PALE_CROSSBOWMAN;
                    default->ModMobs.PALE_SPEARMAN;
                };
                PaleSoldier mob=type.create(level);
                if(mob==null)return;
                // Spawn outside the narrow arch; clearance under the top slabs is 2.5 blocks.
                double distance=i==0?6.5:9.0+i*1.0;
                BlockPos spot=null;
                for(int attempt=0;attempt<8;attempt++) {
                    double along=d+sign*(distance+(attempt/3)*2);
                    double[] p=layout.roadPoint(road,along), dir=layout.roadDirection(road,along);
                    double offset=attempt%3==0?(i==0?0:(i%2==0?1.25:-1.25)):attempt%3==1?1.65:-1.65;
                    int x=(int)Math.floor(p[0]-dir[1]*offset),z=(int)Math.floor(p[1]+dir[0]*offset);
                    int floor=roadY(level,x,z);
                    if(floor==Integer.MIN_VALUE)continue;
                    BlockPos at=new BlockPos(x,floor+1,z);
                    if(Math.abs(at.getY()-g.center.getY())>6||protectedAt(level,at))continue;
                    mob.moveTo(x+.5,at.getY(),z+.5,(float)Math.toDegrees(Math.atan2(-dir[0]*sign,dir[1]*sign)),0);
                    AABB box=mob.getBoundingBox();
                    if(occupied.stream().anyMatch(box::intersects)||!level.noCollision(mob,box))continue;
                    if(level.players().stream().anyMatch(player->!player.isSpectator()&&player.distanceToSqr(mob)<9*9))continue;
                    spot=at;break;
                }
                if(spot==null)return; // never spawn half a planned squad
                mob.finalizeSpawn(level,level.getCurrentDifficultyAt(spot),MobSpawnType.EVENT,null);
                mob.setPower(SquadTuning.TIER_POWER[tier]);mob.setGateHome(spot);mob.addTag(g.guardTag());
                if(leader==null)leader=mob.getUUID();
                mob.joinSquad(squad,leader);
                mob.setPatrol(road,sign==1?d+6:d-15,sign==1?d+15:d-6);
                occupied.add(mob.getBoundingBox());planned.add(mob);
            }
        }
        List<PaleSoldier> added=new ArrayList<>();
        for(PaleSoldier mob:planned) {
            if(!level.addFreshEntity(mob)) {for(PaleSoldier previous:added)previous.discard();return;}
            added.add(mob);
        }
        for(PaleSoldier mob:added)g.guards.add(mob.getUUID());
        setDirty();
    }
}
