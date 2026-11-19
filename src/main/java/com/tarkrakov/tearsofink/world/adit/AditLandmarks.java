package com.tarkrakov.tearsofink.world.adit;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.*;

/** One authored landmark per actually completed adit, including previously generated passages. */
final class AditLandmarks extends SavedData {
    private static final Factory<AditLandmarks> FACTORY = new Factory<>(AditLandmarks::new,AditLandmarks::load,null);
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int MAX_SLOPE=8, MAX_FOUNDATION=12, CLEARANCE=14;
    private static final BlockPos ANCHOR=new BlockPos(7,0,4);
    private final Set<Long> installed=new HashSet<>();
    private final Map<Long,Long> retry=new HashMap<>();

    private static AditLandmarks load(CompoundTag tag, HolderLookup.Provider registries) {
        AditLandmarks data=new AditLandmarks();
        for(long key:tag.getLongArray("Installed"))data.installed.add(key);
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("Installed",installed.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
    static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%100!=0)return;
            ServerLevel level=ModDimensions.lands(server);
            if(level==null||level.players().isEmpty())return;
            InkLandsLayout layout=InkLandsLayout.of(level);
            if(layout!=null)level.getDataStorage().computeIfAbsent(FACTORY,"toi_adit_landmarks").tick(level,layout);
        });
    }
    private void tick(ServerLevel level,InkLandsLayout layout) {
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("pale_adit"));
        if(structure==null)return;
        Set<Long> seen=new HashSet<>();
        long now=level.getGameTime();
        for(var player:level.players()){
            if(!player.isAlive()||player.isSpectator())continue;
            ChunkPos current=player.chunkPosition();
            for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
                ChunkPos owner=new ChunkPos(current.x+dx,current.z+dz);
                long key=owner.toLong();
                if(installed.contains(key)||now<retry.getOrDefault(key,0L)||!seen.add(key))continue;
                var chunk=level.getChunkSource().getChunkNow(owner.x,owner.z);
                if(chunk==null)continue;
                var start=chunk.getStartForStructure(structure);
                if(start==null||!start.isValid())continue;
                for(var piece:start.getPieces())if(piece instanceof AditPiece adit && adit.builtEntrance()!=null){
                    BlockPos entry=adit.builtEntrance();
                    if(Math.hypot(entry.getX()-player.getX(),entry.getZ()-player.getZ())>96)continue;
                    retry.put(key,now+200);
                    try{
                        BlockPos placed=installNear(level,layout,owner,entry,adit.outward());
                        if(placed!=null){
                            installed.add(key);retry.remove(key);setDirty();
                            TearsOfInk.LOGGER.info("[adit landmark] entrance={} landmark={}",entry.toShortString(),placed.toShortString());
                        }
                    }catch(RuntimeException ex){TearsOfInk.LOGGER.warn("[adit landmark] deferred at {}",entry,ex);}
                    return; // bounded to one actual site attempt per pass
                }
            }
        }
    }
    private static Map<BlockPos,BlockState> art(ServerLevel level){
        var template=level.getStructureManager().get(TearsOfInk.id("adit_landmark"));
        if(template.isEmpty())return Map.of();
        Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
        for(Block b:List.of(Blocks.DIRT,ModBlocks.RUIN_BRICKS,ModBlocks.CRACKED_RUIN_BRICKS,
                ModBlocks.OVERGROWN_RUIN_BRICKS,ModBlocks.RUIN_BRICK_WALL,
                ModBlocks.CRACKED_RUIN_BRICK_WALL,ModBlocks.OVERGROWN_RUIN_BRICK_WALL))
            for(var info:template.get().filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),b))
                blocks.put(info.pos().immutable(),info.state());
        if(blocks.size()!=136)throw new IllegalStateException("adit landmark: unexpected template block count "+blocks.size());
        return blocks;
    }
    private static BlockPos installNear(ServerLevel level,InkLandsLayout layout,ChunkPos owner,BlockPos entry,Direction outward){
        Map<BlockPos,BlockState> art=art(level);if(art.isEmpty())return null;
        Direction side=outward.getClockWise();
        int sign=(owner.toLong()^level.getSeed())%2==0?1:-1;
        Rotation[] rotations={Rotation.NONE,Rotation.CLOCKWISE_90,Rotation.CLOCKWISE_180,Rotation.COUNTERCLOCKWISE_90};
        Rotation rotation=rotations[(int)((owner.toLong()^level.getSeed())&3)];
        for(int distance:new int[]{12,16,20,24,28}){
            for(int lateral:new int[]{sign*distance,-sign*distance}){
                for(int forward:new int[]{5,10,15,-5}){
                    BlockPos center=entry.relative(side,lateral).relative(outward,forward);
                    if(!ModDimensions.chunksReady(level,center,7))continue;
                    if(tryPlace(level,layout,owner,entry,outward,center,rotation,art))return center;
                }
            }
        }
        return null;
    }
    private static boolean ground(BlockState s){
        return s.is(BlockTags.BASE_STONE_OVERWORLD)||s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)
                ||s.is(Blocks.GRAVEL)||s.is(Blocks.CLAY)||s.is(Blocks.TUFF)
                ||s.is(ModBlocks.ASHEN_SOIL)||s.is(ModBlocks.ASH)||s.is(ModBlocks.ASPHODELITE_ROCK)
                ||s.is(ModBlocks.ASPHODELITE_ORE)||s.is(ModBlocks.DEEPSLATE_ASPHODELITE_ORE)||s.is(ModBlocks.PALE_ORE);
    }
    private static boolean vegetation(BlockState s){
        return s.is(BlockTags.LEAVES)||s.is(ModBlocks.PALE_LEAVES)||s.is(ModBlocks.PALE_LOG)||s.is(ModBlocks.PALE_WOOD);
    }
    private static boolean replaceable(BlockState s){
        return !s.hasBlockEntity()&&s.getFluidState().isEmpty()
                &&(s.isAir()||s.canBeReplaced()||ground(s)||vegetation(s));
    }
    private static int surface(ServerLevel level,BlockPos column){
        var chunk=level.getChunkSource().getChunkNow(column.getX()>>4,column.getZ()>>4);
        if(chunk==null)return Integer.MIN_VALUE;
        int top=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,column.getX()&15,column.getZ()&15);
        for(int y=Math.min(top,level.getMaxBuildHeight()-1);y>=Math.max(level.getMinBuildHeight(),top-40);y--){
            BlockPos p=column.atY(y);BlockState s=chunk.getBlockState(p);
            if(!replaceable(s))return Integer.MIN_VALUE;
            if(ground(s)&&s.isFaceSturdy(level,p,Direction.UP))return y;
        }
        return Integer.MIN_VALUE;
    }
    private static boolean overlapsStructure(ServerLevel level,int minX,int minZ,int maxX,int maxZ){
        // Check references only in loaded chunks; never request a distant start.
        for(int cx=minX>>4;cx<=maxX>>4;cx++)for(int cz=minZ>>4;cz<=maxZ>>4;cz++){
            var chunk=level.getChunkSource().getChunkNow(cx,cz);
            if(chunk==null)return true;
            for(var start:chunk.getAllStarts().values())
                if(start.isValid()&&start.getBoundingBox().intersects(minX,minZ,maxX,maxZ))return true;
            for(var ref:chunk.getAllReferences().entrySet())for(long key:ref.getValue()){
                ChunkPos p=new ChunkPos(key);
                var source=level.getChunkSource().getChunkNow(p.x,p.z);
                if(source==null)return true;
                var start=source.getStartForStructure(ref.getKey());
                if(start!=null&&start.isValid()&&start.getBoundingBox().intersects(minX,minZ,maxX,maxZ))return true;
            }
        }
        return false;
    }
    private static boolean tryPlace(ServerLevel level,InkLandsLayout layout,ChunkPos owner,BlockPos entry,
                                    Direction outward,BlockPos center,Rotation rotation,Map<BlockPos,BlockState> art){
        int minX=center.getX()+99,minZ=center.getZ()+99,maxX=center.getX()-99,maxZ=center.getZ()-99;
        Map<BlockPos,BlockState> relative=new LinkedHashMap<>();
        for(var e:art.entrySet()){
            BlockPos p=StructureTemplate.transform(e.getKey().subtract(ANCHOR),Mirror.NONE,rotation,BlockPos.ZERO);
            relative.put(p,e.getValue().rotate(rotation));
            minX=Math.min(minX,center.getX()+p.getX());maxX=Math.max(maxX,center.getX()+p.getX());
            minZ=Math.min(minZ,center.getZ()+p.getZ());maxZ=Math.max(maxZ,center.getZ()+p.getZ());
        }
        // Keep the entire generating chunk of the adit pristine, plus one block at its boundary.
        if(minX<=owner.getMaxBlockX()+1&&maxX>=owner.getMinBlockX()-1
                &&minZ<=owner.getMaxBlockZ()+1&&maxZ>=owner.getMinBlockZ()-1)return false;
        if(layout.nearHerald(center.getX(),center.getZ(),100)||overlapsStructure(level,minX,minZ,maxX,maxZ))return false;
        int[][] heights=new int[maxX-minX+1][maxZ-minZ+1];List<Integer> samples=new ArrayList<>();
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
            int dx=x-entry.getX(),dz=z-entry.getZ();
            int forward=dx*outward.getStepX()+dz*outward.getStepZ();
            int side=dx*outward.getStepZ()-dz*outward.getStepX();
            if(dx*dx+dz*dz<36||forward>=-2&&Math.abs(side)<=3)return false;
            BlockPos col=new BlockPos(x,entry.getY()-1,z);
            if(!level.getWorldBorder().isWithinBounds(col)||!level.getBiome(col.above()).is(ModBiomes.PALE_FOREST)
                    ||layout.nearRoad(x,z,3)||layout.nearLake(x,z,InkLandsLayout.LAKE_ZONE_RADIUS+InkLandsLayout.LAKE_SLOPE)
                    ||layout.templeNetwork()!=null&&layout.templeNetwork().overlapsPath(x,z,3))return false;
            int y=surface(level,col);
            if(y==Integer.MIN_VALUE||Math.abs(y-col.getY())>10)return false;
            heights[x-minX][z-minZ]=y;samples.add(y);
        }
        Collections.sort(samples);int floor=samples.get(samples.size()/2);
        if(floor-MAX_FOUNDATION<level.getMinBuildHeight()||floor+CLEARANCE>=level.getMaxBuildHeight()
                ||samples.getLast()-samples.getFirst()>MAX_SLOPE)return false;
        Map<BlockPos,BlockState> edits=new LinkedHashMap<>();
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
            BlockPos p=new BlockPos(x,floor,z);
            if(!replaceable(level.getBlockState(p)))return false;
            edits.put(p,ModBlocks.ASHEN_SOIL.defaultBlockState());
            for(int d=1;d<=MAX_FOUNDATION;d++){
                BlockPos q=p.below(d);BlockState old=level.getBlockState(q);
                if(ground(old)&&old.isFaceSturdy(level,q,Direction.UP))break;
                if(!replaceable(old)||d==MAX_FOUNDATION)return false;
                edits.put(q,ModBlocks.ASPHODELITE_ROCK.defaultBlockState());
            }
            for(int y=1;y<=CLEARANCE;y++){
                BlockPos q=p.above(y);BlockState old=level.getBlockState(q);
                if(!replaceable(old))return false;
                if(!old.isAir())edits.put(q,Blocks.AIR.defaultBlockState());
            }
        }
        for(var e:relative.entrySet()){
            BlockPos p=center.atY(floor).offset(e.getKey());BlockState s=e.getValue();
            edits.put(p,s.is(Blocks.DIRT)?ModBlocks.ASHEN_SOIL.defaultBlockState():s);
        }
        AABB box=new AABB(minX-1,floor-MAX_FOUNDATION,minZ-1,maxX+2,floor+CLEARANCE+1,maxZ+2);
        if(!level.getEntitiesOfClass(LivingEntity.class,box,LivingEntity::isAlive).isEmpty())return false;
        Map<BlockPos,BlockState> before=new LinkedHashMap<>();
        List<BlockPos> ordered=new ArrayList<>(edits.keySet());
        ordered.sort(Comparator.comparingInt((BlockPos p)->p.getY()));
        try{
            for(BlockPos p:ordered){
                BlockState old=level.getBlockState(p),s=edits.get(p);
                if(old.equals(s))continue;
                before.put(p,old);
                if(!level.setBlock(p,s,FLAGS))throw new IllegalStateException("landmark write failed "+p);
            }
            return true;
        }catch(RuntimeException ex){
            List<BlockPos> restore=new ArrayList<>(before.keySet());Collections.reverse(restore);
            for(BlockPos p:restore)level.setBlock(p,before.get(p),FLAGS);
            throw ex;
        }
    }
}
