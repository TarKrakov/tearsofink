package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.entity.patrol.ForestCamps;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWorldPlan;
import com.tarkrakov.tearsofink.world.herald.BloodSettlements;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import static com.tarkrakov.tearsofink.entity.TrollTerritoryRules.*;

/** Settles a cave once and replenishes confirmed vacant resident slots. */
final class TrollEncounters {
    private TrollEncounters(){}
    static void init(){
        ServerTickEvents.END_WORLD_TICK.register(TrollEncounters::tick);
        ServerEntityEvents.ENTITY_LOAD.register((e,l)->{if(ModDimensions.isLands(l)&&e instanceof TrollEntity t)TrollTerritories.of(l).loaded(t);});
        ServerEntityEvents.ENTITY_UNLOAD.register((e,l)->{
            if(ModDimensions.isLands(l)&&e instanceof TrollEntity t&&t.territoryId()!=null&&e.getRemovalReason()!=null&&e.getRemovalReason().shouldDestroy())
                TrollTerritories.of(l).died(t.territoryId(),t.getUUID(),l.getGameTime());
        });
    }
    private static void tick(ServerLevel l){
        if(!ModDimensions.isLands(l)||l.getDifficulty()==Difficulty.PEACEFUL||!l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        TrollTerritories data=TrollTerritories.of(l);
        for(ServerPlayer p:l.players()){
            if(!p.isAlive()||p.isSpectator()||Math.floorMod(l.getGameTime()+p.getUUID().hashCode(),300)!=0||!LoadedFaunaTerrain.search(l))continue;
            InkLandsLayout layout=InkLandsLayout.of(l);if(layout==null)continue;
            // Only replenish confirmed vacant slots; UUIDs of unloaded residents
            // remain occupied indefinitely. Reuse exactly the saved bed.
            boolean replenished=false;
            for(TrollTerritories.Zone z:data.zones()){
                if(!within(p.getX(),p.getZ(),z.center.getX()+.5,z.center.getZ()+.5,SPAWN_MAX+BED_MAX))continue;
                for(TrollTerritories.Slot slot:z.slots){
                    if(slot.vacant(l.getGameTime())&&nearEnough(p,slot.bed)&&farFromPlayers(l,slot.bed)
                        &&validBed(l,layout,slot.bed)&&spawn(l,data,z,slot))replenished=true;
                }
            }
            if(replenished||l.random.nextInt(3)!=0)continue;
            int desired = size(l.random.nextInt(100));
            for (int i = 0; i < 20; i++) {
                double angle = l.random.nextDouble() * Math.PI * 2;
                double distance = 39 + l.random.nextDouble() * 18;
                int x = (int) Math.floor(p.getX() + Math.cos(angle) * distance);
                int z = (int) Math.floor(p.getZ() + Math.sin(angle) * distance);
                if (!data.available(x, z, MIN_GAP - 6)) continue;
                BlockPos mouth = TrollCave.ground(l, x, z);
                if (mouth == null || !centerAllowed(l, layout, mouth) || !farFromPlayers(l, mouth)) continue;
                boolean inkwood = l.getBiome(mouth).is(ModBiomes.INKWOOD);
                Direction inward = Direction.fromYRot(Math.toDegrees(angle) - 90);
                boolean settled = false;
                for (int turn = 0; turn < 4; turn++, inward = inward.getClockWise()) {
                    BlockPos center = mouth.relative(inward, 6);
                    if (!data.available(center) || !centerAllowed(l, layout, center)) continue;
                    TrollCave.Plan cave = TrollCave.plan(l, mouth, inward, desired, inkwood);
                    if (cave == null || cave.beds().stream().anyMatch(bed ->
                            !nearEnough(p, bed) || !farFromPlayers(l, bed) || !bedAllowed(l, layout, bed))) continue;
                    cave.build(l, inkwood);
                    TrollTerritories.Zone zone = data.create(cave.center(), cave.entrance(), cave.beds());
                    for (TrollTerritories.Slot slot : zone.slots) spawn(l, data, zone, slot);
                    TrollTrees.thinAround(l, cave.center(), cave.entrance());
                    settled = true;
                    break;
                }
                if (settled) break;
            }
        }
    }

    private static boolean centerAllowed(ServerLevel level, InkLandsLayout layout, BlockPos pos) {
        int x = pos.getX(), z = pos.getZ();
        var battlefield = BattlefieldWorldPlan.of(layout);
        return layout.depthInside(x, z) >= CHASE && !layout.nearRoad(x, z, DEFEND + 12)
                && !Ruins.nearStart(layout, x, z) && !Sanctuary.near(layout, x, z, CHASE + 8)
                && !layout.nearHerald(x, z, 40) && !ForestCamps.near(level, pos, 38)
                && (battlefield == null || !battlefield.reserved(x, z, 20)) && bedAllowed(level, layout, pos);
    }

    private static boolean nearEnough(ServerPlayer p,BlockPos bed){
        return within(p.getX(),p.getZ(),bed.getX()+.5,bed.getZ()+.5,SPAWN_MAX);
    }
    private static boolean farFromPlayers(ServerLevel l,BlockPos bed){
        for(ServerPlayer p:l.players())if(p.isAlive()&&!p.isSpectator()&&distanceSq(p.getX(),p.getZ(),bed.getX()+.5,bed.getZ()+.5)<SPAWN_MIN*SPAWN_MIN)return false;
        return true;
    }
    /** Ground and complete mob volume must already be loaded and unoccupied. */
    static BlockPos site(ServerLevel l,int x,int z){
        var chunk=l.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)return null;
        int y=LoadedFaunaTerrain.surface(chunk,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        BlockPos p=new BlockPos(x,y,z);return clear(l,p)?p:null;
    }
    static boolean clear(ServerLevel l,BlockPos p){
        var chunk=LoadedFaunaTerrain.chunk(l,p);if(chunk==null)return false;
        Vec3 at=Vec3.atBottomCenterOf(p);AABB body=new AABB(at.x-1.3,at.y,at.z-1.3,at.x+1.3,at.y+4,at.z+1.3);
        var floor = chunk.getBlockState(p.below());
        if (floor.is(BlockTags.LOGS) || !floor.isFaceSturdy(l, p.below(), Direction.UP)
                || !LoadedFaunaTerrain.free(l, null, body) || !l.noCollision(body)) return false;
        for(BlockPos q:BlockPos.betweenClosed(BlockPos.containing(body.minX,body.minY,body.minZ),BlockPos.containing(body.maxX,body.maxY,body.maxZ)))
            if(!l.getFluidState(q).isEmpty())return false;
        return true;
    }
    private static boolean validBed(ServerLevel l,InkLandsLayout layout,BlockPos p){
        return clear(l, p) && bedAllowed(l, layout, p);
    }
    private static boolean bedAllowed(ServerLevel l, InkLandsLayout layout, BlockPos p) {
        if(layout.depthInside(p.getX(),p.getZ())<20||layout.nearRoad(p.getX(),p.getZ(),12)
            ||Ruins.nearStart(layout,p.getX(),p.getZ())||Sanctuary.near(layout,p.getX(),p.getZ(),40)
            ||BattlefieldFaunaBoundary.inside(l,p.getX(),p.getZ())||BloodSettlements.contains(l,p))return false;
        return l.getBiome(p).is(ModBiomes.PALE_FOREST)||l.getBiome(p).is(ModBiomes.INKWOOD);
    }
    private static boolean spawn(ServerLevel l,TrollTerritories data,TrollTerritories.Zone z,TrollTerritories.Slot s){
        if(s.member!=null||!clear(l,s.bed))return false;
        TrollEntity mob=ModMobs.TROLL.create(l);if(mob==null)return false;
        Vec3 at=Vec3.atBottomCenterOf(s.bed);mob.moveTo(at.x,at.y,at.z,l.random.nextFloat()*360,0);
        mob.bindTerritory(z.id,z.center,s.bed,z.entrance);data.reserve(s,mob.getUUID());
        if(l.addFreshEntity(mob))return true;
        data.failed(s,l.getGameTime());return false;
    }
}
