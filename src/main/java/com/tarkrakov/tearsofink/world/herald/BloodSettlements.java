package com.tarkrakov.tearsofink.world.herald;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.blood.BloodPumpBlock;
import com.tarkrakov.tearsofink.block.lantern.FogClearing;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import java.util.*;

/**
 * D-090. One modular peaceful settlement near an existing Herald, not inside
 * the worldgen pipeline. All writes are planned in loaded chunks and the actual
 * Herald structure start determines the immutable tree/pump mask.
 */
public final class BloodSettlements extends SavedData {
    private static final Factory<BloodSettlements> FACTORY=new Factory<>(BloodSettlements::new,BloodSettlements::load,null);
    private static final int WAKE=112, RETRY=20*5, MAX_FOUNDATION=12, MAX_EDITS=18000;
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
    private static final ResourceKey<LootTable> LOOT=ResourceKey.create(Registries.LOOT_TABLE,TearsOfInk.id("chests/settlement_supplies"));
    private static final String[] NAMES={"blood_wall","blood_wall_corner","blood_gate","blood_house","blood_settlement_center"};
    private static final int[] COUNTS={186,175,348,277,185};
    private static final BlockPos WALL_ANCHOR=new BlockPos(2,0,4), CORNER_ANCHOR=new BlockPos(5,0,5),
            GATE_ANCHOR=new BlockPos(3,0,4), HOUSE_ANCHOR=new BlockPos(6,0,6), CROSS_ANCHOR=new BlockPos(6,0,6);
    private final Map<Long,Site> sites=new HashMap<>();
    private final Set<Long> lamps=new HashSet<>();
    private final Map<Long,Long> retry=new HashMap<>();
    private final Map<Long,Integer> nextCandidate=new HashMap<>();
    // x/z offset from the actual trunk, quarter turn, half-width/depth, length of left gate wing.
    // These are candidates measured against the current authored Herald (not forced placements).
    // Compact layouts at the end are the fallback for the smallest forest and uneven boundaries.
    private static final int[][] CANDIDATES={
        {28,6,3,20,18,9},{28,10,2,18,18,7},{28,6,0,20,19,9},
        {-16,-24,1,18,18,7},{-8,-24,2,22,18,11},{-12,-24,1,18,20,7},
        {28,4,3,20,22,9},{28,8,2,22,18,9},{-16,-24,2,20,19,9},
        {-24,-20,1,18,19,7},{30,8,2,18,22,6},{28,14,3,20,18,9},
        {-20,-28,1,22,19,11},{-20,-24,1,20,22,9},{-20,-40,1,20,22,9},
        {48,8,1,22,19,11},{0,52,1,20,22,9},{-16,-44,1,22,19,11},
        {-14,-22,2,15,18,4},{-8,-24,2,20,18,9},{-16,-22,1,16,18,5},
        {-10,-24,3,18,18,9},{28,8,0,15,19,4},{-14,-22,1,20,15,9},
        {-8,-26,2,15,20,4},{-8,-26,1,20,15,9},{28,10,2,15,18,4},
        {28,8,3,20,15,9}
    };
    private final Map<StructureTemplate,List<BlockPos>> heraldMasks=new WeakHashMap<>();
    private record Site(BlockPos center,int radius,int houses,long[] lamps){}
    private static BloodSettlements of(ServerLevel level){return level.getDataStorage().computeIfAbsent(FACTORY,"toi_blood_settlements");}
    public static boolean protectsLamp(Level level,BlockPos p){
        return level instanceof ServerLevel server&&ModDimensions.isLands(server)&&server.getServer().isSameThread()
                &&of(server).lamps.contains(p.asLong());
    }
    public static boolean contains(ServerLevel level,BlockPos p){
        if(!ModDimensions.isLands(level)||!level.getServer().isSameThread())return false;
        for(Site site:of(level).sites.values())if(Math.abs(p.getX()-site.center.getX())<=site.radius
                &&Math.abs(p.getZ()-site.center.getZ())<=site.radius&&Math.abs(p.getY()-site.center.getY())<=20)return true;
        return false;
    }
    private static BloodSettlements load(CompoundTag tag,HolderLookup.Provider registries){
        BloodSettlements d=new BloodSettlements();ListTag list=tag.getList("Sites",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){
            CompoundTag t=list.getCompound(i);long[] lamps=t.getLongArray("Lamps");
            d.sites.put(t.getLong("Herald"),new Site(BlockPos.of(t.getLong("Center")),t.getInt("Radius"),t.getInt("Houses"),lamps));
            for(long p:lamps)d.lamps.add(p);
        }return d;
    }
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider registries){
        ListTag list=new ListTag();sites.forEach((key,site)->{
            CompoundTag n=new CompoundTag();n.putLong("Herald",key);n.putLong("Center",site.center.asLong());
            n.putInt("Radius",site.radius);n.putInt("Houses",site.houses);n.putLongArray("Lamps",site.lamps);list.add(n);
        });t.put("Sites",list);return t;
    }
    public static void init(){
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%100!=0)return;
            ServerLevel level=ModDimensions.lands(server);
            if(level==null||level.players().isEmpty())return;
            InkLandsLayout layout=InkLandsLayout.of(level);
            if(layout!=null)of(level).tick(level,layout);
        });
    }
    private void tick(ServerLevel level,InkLandsLayout layout){
        long now=level.getGameTime();
        Set<Long> seen=new HashSet<>();
        for(ServerPlayer p:level.players()){
            if(!p.isAlive()||p.isSpectator())continue;
            long[] blob=layout.nearestBloodBlob(p.getBlockX(),p.getBlockZ());
            if(blob==null)continue;
            BlockPos trunk=new BlockPos((int)blob[0],0,(int)blob[1]);
            long key=trunk.asLong();
            // Arena is a separate authored encounter; do not build a settlement there.
            if(trunk.getX()==layout.arenaCenter().getX()&&trunk.getZ()==layout.arenaCenter().getZ())continue;
            if(!seen.add(key)||sites.containsKey(key)||now<retry.getOrDefault(key,0L)
                    ||horizontal(trunk,p.blockPosition())>WAKE*WAKE)continue;
            retry.put(key,now+RETRY);
            try{
                Site built=tryBuild(level,layout,trunk,(int)blob[2],key);
                if(built!=null){
                    sites.put(key,built);for(long lamp:built.lamps)lamps.add(lamp);
                    retry.remove(key);nextCandidate.remove(key);setDirty();
                    TearsOfInk.LOGGER.info("Blood settlement: Herald {}, square {}, houses {}",trunk,built.center,built.houses);
                    return;
                }
            }catch(RuntimeException ex){TearsOfInk.LOGGER.warn("Blood settlement skipped at {}",trunk,ex);}
        }
    }
    private static double horizontal(BlockPos a,BlockPos b){
        double x=a.getX()-b.getX(),z=a.getZ()-b.getZ();return x*x+z*z;
    }
    private static BlockPos rotate(BlockPos p,Rotation r){return StructureTemplate.transform(p,Mirror.NONE,r,BlockPos.ZERO);}
    private static BlockPos point(BlockPos origin,int x,int y,int z,Rotation r){return origin.offset(rotate(new BlockPos(x,y,z),r));}
    private static Rotation add(Rotation a,Rotation b){
        // Explicit order, independent of enum ordinal.
        var order=List.of(Rotation.NONE,Rotation.CLOCKWISE_90,Rotation.CLOCKWISE_180,Rotation.COUNTERCLOCKWISE_90);
        return order.get((order.indexOf(a)+order.indexOf(b))%4);
    }
    private record Art(Map<BlockPos,BlockState> blocks,int minX,int maxX,int minZ,int maxZ,int top){}
    private static List<Block> palette(){
        List<Block> list=new ArrayList<>(List.of(Blocks.DIRT,Blocks.RED_BED,Blocks.CHEST,ModBlocks.BLOOD_SOIL,
                ModBlocks.INK_LANTERN,ModBlocks.GOLDEN_SCARLET_LEAVES,ModBlocks.BLOODROOT,ModBlocks.FLESHBERRY));
        for(String name:List.of("stripped_scarlet_log","scarlet_slab","scarlet_fence","scarlet_leaves","scarlet_stairs","scarlet_planks"))
            list.add(BuiltInRegistries.BLOCK.get(TearsOfInk.id(name)));
        return list;
    }
    private static Art read(StructureTemplate template,int expected){
        Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
        for(Block b:palette())for(var info:template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),b))
            blocks.put(info.pos().immutable(),info.state());
        if(blocks.size()!=expected)throw new IllegalStateException("Unexpected settlement template block count "+blocks.size()+"/"+expected);
        return new Art(blocks,blocks.keySet().stream().mapToInt(BlockPos::getX).min().orElseThrow(),
                blocks.keySet().stream().mapToInt(BlockPos::getX).max().orElseThrow(),
                blocks.keySet().stream().mapToInt(BlockPos::getZ).min().orElseThrow(),
                blocks.keySet().stream().mapToInt(BlockPos::getZ).max().orElseThrow(),
                blocks.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow());
    }
    private static Art[] assets(ServerLevel level){
        Art[] arts=new Art[NAMES.length];
        for(int i=0;i<arts.length;i++){
            var template=level.getStructureManager().get(TearsOfInk.id(NAMES[i]));
            if(template.isEmpty())return null;arts[i]=read(template.get(),COUNTS[i]);
        }
        return arts;
    }
    /** Exact authored non-air positions in the height range which a settlement may change. */
    private List<BlockPos> mask(StructureTemplate template){
        return heraldMasks.computeIfAbsent(template,t->{
            CompoundTag raw=t.save(new CompoundTag());
            ListTag blocks=raw.getList("blocks",Tag.TAG_COMPOUND);
            ListTag palette=raw.getList("palette",Tag.TAG_COMPOUND);
            if(palette.isEmpty())throw new IllegalStateException("Herald palette unavailable");
            List<BlockPos> result=new ArrayList<>();
            for(int i=0;i<blocks.size();i++){
                CompoundTag block=blocks.getCompound(i);ListTag p=block.getList("pos",Tag.TAG_INT);
                String name=palette.getCompound(block.getInt("state")).getString("Name");
                if(name.equals("minecraft:air")||name.equals("minecraft:cave_air")||name.equals("minecraft:void_air")||name.equals("minecraft:structure_void"))continue;
                if(p.getInt(1)<=24)result.add(new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2)));
            }
            if(result.isEmpty())throw new IllegalStateException("Empty Herald protection mask");
            return List.copyOf(result);
        });
    }
    private static final class Protection {
        final Set<BlockPos> voxels=new HashSet<>();
        final Set<Long> rootFloor=new HashSet<>(),collectorColumns=new HashSet<>();
        boolean protectedAt(BlockPos p){
            return voxels.contains(p)||collectorColumns.contains(column(p));
        }
        boolean floorProtected(BlockPos p){return rootFloor.contains(column(p))||collectorColumns.contains(column(p));}
    }
    private static long column(BlockPos p){return p.atY(0).asLong();}
    private record HeraldSite(BlockPos trunk,Protection protect){}
    private HeraldSite herald(ServerLevel level,BlockPos center){
        var chunk=level.getChunkSource().getChunkNow(center.getX()>>4,center.getZ()>>4);
        if(chunk==null)return null;
        var registered=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BloodHerald.TEMPLATE);
        var start=registered==null?null:chunk.getStartForStructure(registered);
        if(start==null||!start.isValid())return null;
        var template=level.getStructureManager().get(BloodHerald.TEMPLATE);
        if(template.isEmpty())return null;
        BloodHerald.Layout h=BloodHerald.layout(level.getStructureManager());
        for(var p:start.getPieces())if(p instanceof BloodHeraldPiece){
            var b=p.getBoundingBox();var size=template.get().getSize();
            // Refuse an old, differently sized authored tree rather than guess its coordinates.
            if(b.getXSpan()!=size.getX()||b.getYSpan()!=size.getY()||b.getZSpan()!=size.getZ())return null;
            BlockPos corner=new BlockPos(b.minX(),b.minY(),b.minZ()),trunk=corner.offset(h.trunk());
            if(trunk.getX()!=center.getX()||trunk.getZ()!=center.getZ()
                    ||!level.getBlockState(trunk.above(4)).is(ModBlocks.HERALD_HEART))return null;
            Protection protection=new Protection();
            for(BlockPos pos:mask(template.get())){
                BlockPos world=corner.offset(pos);protection.voxels.add(world);
                if(pos.getY()<=3)protection.rootFloor.add(column(world));
            }
            protection.voxels.add(trunk.above(4));
            for(int i=0;i<h.pumps().size();i++){
                BlockPos pump=corner.offset(h.pumps().get(i));
                BlockPos tank=pump.relative(BloodPumpBlock.tankSide(h.facings().get(i)));
                for(BlockPos machine:List.of(pump,tank))for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)
                    protection.collectorColumns.add(column(machine.offset(dx,0,dz)));
            }
            return new HeraldSite(trunk,protection);
        }
        return null;
    }
    private record Piece(Art art,BlockPos anchor,BlockPos target,Rotation rotation,int trim,int variant,boolean strict){}
    private static final class Plan{
        final BlockPos center;
        final Protection protect;
        final Map<BlockPos,BlockState> art=new LinkedHashMap<>();
        final Map<BlockPos,Integer> columns=new HashMap<>();
        int houses;
        Plan(BlockPos center,Protection protect){this.center=center;this.protect=protect;}
        boolean add(Piece p){
            Art a=p.art;int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
            Map<BlockPos,BlockState> placed=new LinkedHashMap<>();
            for(var e:a.blocks.entrySet()){
                BlockPos local=e.getKey();BlockState s=e.getValue();
                if(p.trim>0&&local.getX()>=2+p.trim)continue;
                if(p.variant>=0){
                    if(s.is(Blocks.RED_BED)&&(p.variant==4||p.variant==2&&local.getX()==7))continue;
                    if(s.is(Blocks.CHEST)&&(p.variant==1||((p.variant==2||p.variant==3)&&local.getX()==7)))continue;
                }
                BlockPos q=p.target.offset(rotate(local.subtract(p.anchor),p.rotation));
                if(p.strict&&protect.protectedAt(q))return false;
                if(!p.strict&&(protect.protectedAt(q)||q.getY()==center.getY()&&protect.floorProtected(q)))continue;
                if(s.is(Blocks.DIRT))s=ModBlocks.BLOOD_SOIL.defaultBlockState();
                if(s.is(ModBlocks.INK_LANTERN))s=s.setValue(FogClearing.LIT,true).setValue(FogClearing.PLAYER_MADE,false);
                placed.put(q,s.rotate(p.rotation));
                minX=Math.min(minX,q.getX());maxX=Math.max(maxX,q.getX());minZ=Math.min(minZ,q.getZ());maxZ=Math.max(maxZ,q.getZ());
            }
            if(placed.isEmpty())return false;
            if(p.variant==4)for(int x:new int[]{5,7}){
                BlockPos q=p.target.offset(rotate(new BlockPos(x,5,4).subtract(p.anchor),p.rotation));
                BlockState s=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,x==5?Direction.EAST:Direction.WEST).rotate(p.rotation);
                if(protect.protectedAt(q))return false;placed.put(q,s);
            }
            // Whole room, stairs, gate passage and plaza must remain accessible;
            // only walls may occupy the free voxels between roots.
            for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
                BlockPos floor=new BlockPos(x,center.getY(),z);
                if(p.strict){
                    if(protect.floorProtected(floor)||columns.containsKey(floor))return false;
                    for(int y=1;y<=a.top+1;y++)if(protect.protectedAt(floor.above(y)))return false;
                }else if(protect.collectorColumns.contains(column(floor)))return false;
            }
            for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
                BlockPos floor=new BlockPos(x,center.getY(),z);
                columns.merge(floor,Math.max(a.top+2,5),Math::max);
            }
            // The segment endpoints can share an ornamental leaf/flower; never erase a solid building block with vegetation.
            placed.forEach((pos,s)->art.merge(pos,s,(old,n)->plant(old)&&!plant(n)?n:old));
            return true;
        }
        void court(int rx,int rz,Rotation rot){
            for(int x=-rx+1;x<rx;x++)for(int z=-rz+1;z<rz;z++)
                columns.merge(point(center,x,0,z,rot),16,Math::max);
        }
    }
    private static boolean row(Plan plan,Art[] assets,BlockPos start,Rotation rot,int span){
        for(int k=0;k<span;){
            int n=Math.min(9,span-k);
            if(!plan.add(new Piece(assets[0],WALL_ANCHOR,point(start,k,0,0,rot),rot,n==9?0:n,-1,false)))return false;
            k+=n;
        }
        return true;
    }
    private static Plan plan(Art[] a,HeraldSite h,BlockPos center,Rotation rot,int rx,int rz,int left,RandomSource r){
        Plan p=new Plan(center,h.protect);
        // For all four corners local authored arms point north and east.
        int[][] corners={{-rx,rz},{rx,rz},{rx,-rz},{-rx,-rz}};
        Rotation[] turns={Rotation.NONE,Rotation.COUNTERCLOCKWISE_90,Rotation.CLOCKWISE_180,Rotation.CLOCKWISE_90};
        // Gate first: avoid an ornamental wall overhang blocking its strict bounds.
        if(!p.add(new Piece(a[2],GATE_ANCHOR,point(center,-rx+4+left,0,rz,rot),rot,0,-1,true)))return null;
        for(int i=0;i<4;i++){
            Rotation turn=add(rot,turns[i]);BlockPos corner=point(center,corners[i][0],0,corners[i][1],rot);
            if(!p.add(new Piece(a[1],CORNER_ANCHOR,corner,turn,0,-1,false)))return null;
        }
        // Both gate wings already have a continuous 14-block opening/pillars between x=3..16.
        BlockPos south=point(center,-rx+4,0,rz,rot);
        if(!row(p,a,south,rot,left)
                ||!row(p,a,point(south,left+14,0,0,rot),rot,2*rx-21-left))return null;
        for(int i=1;i<4;i++){
            Rotation turn=add(rot,turns[i]);BlockPos corner=point(center,corners[i][0],0,corners[i][1],rot);
            if(!row(p,a,point(corner,4,0,0,turn),turn,(i%2==0?2*rx:2*rz)-7))return null;
        }
        if(!p.add(new Piece(a[4],CROSS_ANCHOR,center,rot,0,-1,true)))return null;
        int hx=rx-8,hz=rz-8;
        List<int[]> plots=new ArrayList<>(List.of(new int[]{-hx,-hz},new int[]{hx,-hz},
                new int[]{-hx,hz},new int[]{hx,hz},new int[]{0,-hz},new int[]{0,hz},new int[]{-hx,0},new int[]{hx,0}));
        List<Integer> variants=new ArrayList<>(List.of(0,1,2,3,4));
        for(int i=variants.size()-1;i>0;i--)Collections.swap(variants,i,r.nextInt(i+1));
        int goal=Math.min(rx,rz)<=16?3:4+r.nextInt(2);
        for(int[] q:plots){
            BlockPos target=point(center,q[0],0,q[1],rot);
            Rotation face=Math.abs(q[0])>Math.abs(q[1])?(q[0]>0?Rotation.CLOCKWISE_90:Rotation.COUNTERCLOCKWISE_90)
                    :q[1]>0?Rotation.CLOCKWISE_180:Rotation.NONE;
            if(p.add(new Piece(a[3],HOUSE_ANCHOR,target,add(rot,face),0,variants.get(p.houses),true))){
                if(++p.houses>=goal)break;
            }
        }
        if(p.houses<(Math.min(rx,rz)<=16?2:3))return null;
        p.court(rx,rz,rot);return p;
    }
    private Site tryBuild(ServerLevel level,InkLandsLayout layout,BlockPos trunk,int radius,long key){
        HeraldSite h=herald(level,trunk);if(h==null)return null;
        Art[] a=assets(level);if(a==null)return null;
        RandomSource random=RandomSource.create(level.getSeed()^key^0x534554544c45L);
        Rotation[] rotations={Rotation.NONE,Rotation.CLOCKWISE_90,Rotation.CLOCKWISE_180,Rotation.COUNTERCLOCKWISE_90};
        List<Integer> order=new ArrayList<>();
        // Seed-stable shuffle within comparable sizes. Try larger villages before compact fallback.
        for(int[] group:new int[][]{{0,12},{12,18},{18,CANDIDATES.length}}){
            List<Integer> part=new ArrayList<>();
            for(int i=group[0];i<group[1];i++)part.add(i);
            for(int i=part.size()-1;i>0;i--)Collections.swap(part,i,random.nextInt(i+1));
            order.addAll(part);
        }
        int first=nextCandidate.getOrDefault(key,0),attempts=0,checked=0;
        for(;checked<Math.min(12,order.size())&&attempts<2;checked++){
            int index=order.get((first+checked)%order.size());int[] c=CANDIDATES[index];
            Rotation rot=rotations[c[2]];
            BlockPos center=h.trunk.below().offset(c[0],0,c[1]);
            int extent=Math.max(c[3],c[4])+7;
            if(!ModDimensions.chunksReady(level,center,extent+2)
                    ||!level.getWorldBorder().isWithinBounds(center)
                    ||Sanctuary.near(layout,center.getX(),center.getZ(),extent+4)
                    ||Ruins.nearStart(layout,center.getX(),center.getZ()))continue;
            if(level.players().stream().anyMatch(player->player.isAlive()&&!player.isSpectator()
                    &&Math.abs(player.getBlockX()-center.getX())<=extent&&Math.abs(player.getBlockZ()-center.getZ())<=extent
                    &&Math.abs(player.getBlockY()-center.getY())<=24))continue;
            Plan p=plan(a,h,center,rot,c[3],c[4],c[5],RandomSource.create(level.getSeed()^key^index));
            if(p==null)continue;
            // Cheap geographical filter before scanning terrain or doing any write.
            boolean inForest=true;
            for(BlockPos q:p.columns.keySet()){
                if(horizontal(q,h.trunk)>(radius+4)*(radius+4)
                        ||layout.pick(q.getX(),q.getZ(),null)!=ModBiomes.BLOODWOOD){inForest=false;break;}
            }
            if(!inForest)continue;
            attempts++;
            Site built=install(level,layout,h,p,extent,radius,random);
            if(built!=null)return built;
        }
        nextCandidate.put(key,(first+Math.max(1,checked))%order.size());return null;
    }
    private static boolean plant(BlockState s){
        return s.is(BlockTags.LEAVES)||s.is(ModBlocks.GOLDEN_SCARLET_LEAVES)||s.is(ModBlocks.BLOODROOT)
                ||s.is(ModBlocks.FLESHBERRY)||s.is(ModBlocks.SCARLET_GRASS)||s.is(ModBlocks.TALL_SCARLET_GRASS);
    }
    private static boolean ground(BlockState s){
        var id=BuiltInRegistries.BLOCK.getKey(s.getBlock());
        return s.is(ModBlocks.BLOOD_SOIL)||s.is(ModBlocks.ASHEN_SOIL)||s.is(ModBlocks.ASH)||s.is(ModBlocks.ASPHODELITE_ROCK)
                ||s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)||s.is(BlockTags.BASE_STONE_OVERWORLD)
                ||s.is(Blocks.GRAVEL)||id.getNamespace().equals("tears-of-ink")&&id.getPath().endsWith("_ore");
    }
    private static boolean replaceable(BlockState s){
        if(s.hasBlockEntity()||!s.getFluidState().isEmpty()||s.is(ModBlocks.HERALD_HEART)||s.is(ModBlocks.BLOOD_PUMP)||s.is(ModBlocks.BLOOD_TANK))return false;
        // Normal trees only; stripped logs, glowstone and constructed blocks are never classified as terrain.
        return s.isAir()||ground(s)||plant(s)||s.is(ModBlocks.SCARLET.log())||s.is(ModBlocks.SCARLET.wood())||s.canBeReplaced();
    }
    private Site install(ServerLevel level,InkLandsLayout layout,HeraldSite h,Plan p,int extent,int radius,RandomSource r){
        var protect=h.protect;Map<BlockPos,BlockState> edits=new LinkedHashMap<>();
        // Prevent edits while a person is standing inside the future village.
        for(ServerPlayer player:level.players())if(player.isAlive()&&!player.isSpectator()
                &&Math.abs(player.getBlockX()-p.center.getX())<=extent
                &&Math.abs(player.getBlockZ()-p.center.getZ())<=extent
                &&Math.abs(player.getBlockY()-p.center.getY())<=24)return null;
        for(var entry:p.columns.entrySet()){
            BlockPos floor=entry.getKey();int clearance=entry.getValue();
            if(!level.getWorldBorder().isWithinBounds(floor)||floor.getY()-MAX_FOUNDATION<level.getMinBuildHeight()
                    ||floor.getY()+clearance>=level.getMaxBuildHeight()
                    ||horizontal(floor,h.trunk)>(radius+4)*(radius+4)
                    ||!level.getBiome(floor.above()).is(ModBiomes.BLOODWOOD)
                    ||layout.isRoad(floor.getX(),floor.getZ()))return null;
            if(protect.collectorColumns.contains(column(floor)))continue;
            if(!protect.floorProtected(floor)){
                if(!replaceable(level.getBlockState(floor)))return null;
                edits.put(floor,ModBlocks.BLOOD_SOIL.defaultBlockState());
                for(int d=1;d<=MAX_FOUNDATION;d++){
                    BlockPos below=floor.below(d);BlockState old=level.getBlockState(below);
                    if(protect.protectedAt(below))return null;
                    if(ground(old)&&old.isFaceSturdy(level,below,Direction.UP))break;
                    if(!replaceable(old)||d==MAX_FOUNDATION)return null;
                    edits.put(below,ModBlocks.ASPHODELITE_ROCK.defaultBlockState());
                }
            }
            for(int y=1;y<=clearance;y++){
                BlockPos at=floor.above(y);
                if(protect.protectedAt(at))continue;
                BlockState old=level.getBlockState(at);
                if(!replaceable(old))return null;
                if(!old.isAir())edits.put(at,Blocks.AIR.defaultBlockState());
            }
        }
        for(var entry:p.art.entrySet()){
            BlockPos q=entry.getKey();
            if(protect.protectedAt(q)||q.getY()==p.center.getY()&&protect.floorProtected(q))continue;
            if(!replaceable(level.getBlockState(q)))return null;
            edits.put(q,entry.getValue());
        }
        edits.entrySet().removeIf(e->level.getBlockState(e.getKey()).equals(e.getValue()));
        if(edits.isEmpty()||edits.size()>MAX_EDITS)return null;
        // Preflight all exact coordinates. No writes at all to the authored Herald,
        // machine columns, or root foundations, including rollback and shape repair.
        for(BlockPos q:edits.keySet())if(protect.protectedAt(q)||q.getY()<=p.center.getY()&&protect.floorProtected(q))return null;
        var box=new AABB(p.center).inflate(extent,20,extent);
        for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,box,LivingEntity::isAlive)){
            AABB bb=e.getBoundingBox();
            for(BlockPos q:BlockPos.betweenClosed(BlockPos.containing(bb.minX,bb.minY,bb.minZ),BlockPos.containing(bb.maxX,bb.maxY,bb.maxZ))){
                BlockState s=edits.get(q);
                if(s!=null&&!s.isAir()&&!s.getCollisionShape(level,q).isEmpty())return null;
            }
        }
        List<BlockPos> ordered=new ArrayList<>(edits.keySet());
        ordered.sort(Comparator.comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        Map<BlockPos,BlockState> before=new LinkedHashMap<>();
        try{
            for(BlockPos q:ordered){
                BlockState old=level.getBlockState(q),wanted=edits.get(q);before.put(q,old);
                if(!old.equals(wanted)&&!level.setBlock(q,wanted,FLAGS))throw new IllegalStateException("write failed "+q);
            }
            for(var e:p.art.entrySet()){
                BlockPos q=e.getKey();
                if(protect.protectedAt(q)||q.getY()==p.center.getY()&&protect.floorProtected(q))continue;
                BlockState st=level.getBlockState(q);
                if(st.getBlock() instanceof FenceBlock){
                    BlockState updated=Block.updateFromNeighbourShapes(st,level,q);
                    if(!updated.isAir()&&!updated.equals(st))level.setBlock(q,updated,FLAGS);
                }
                if(st.is(Blocks.CHEST))RandomizableContainer.setBlockEntityLootTable(level,r,q,LOOT);
            }
            long[] lamps=p.art.keySet().stream().filter(q->!protect.protectedAt(q)&&level.getBlockState(q).is(ModBlocks.INK_LANTERN))
                    .mapToLong(BlockPos::asLong).toArray();
            return new Site(p.center,extent,p.houses,lamps);
        }catch(RuntimeException ex){
            List<BlockPos> restore=new ArrayList<>(before.keySet());Collections.reverse(restore);
            for(BlockPos q:restore)level.setBlock(q,before.get(q),FLAGS);
            TearsOfInk.LOGGER.warn("Blood settlement rolled back at {}",p.center,ex);return null;
        }
    }
}
