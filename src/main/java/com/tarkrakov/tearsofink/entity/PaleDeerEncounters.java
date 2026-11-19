package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import static com.tarkrakov.tearsofink.entity.PaleDeerTuning.*;

/** One resident herd per eligible territory. No vanilla CREATURE-cap competition or forced chunks. */
final class PaleDeerEncounters {
    private PaleDeerEncounters() {}
    static void init() { ServerTickEvents.END_WORLD_TICK.register(PaleDeerEncounters::tick); }
    private static void tick(ServerLevel level) {
        if (!ModDimensions.isLands(level) || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        for (ServerPlayer p:level.players()) {
            if (!p.isAlive() || p.isSpectator()
                    || Math.floorMod(level.getGameTime()+p.getUUID().hashCode(),SPAWN_INTERVAL)!=0
                    || !LoadedFaunaTerrain.loaded(level,new AABB(p.blockPosition()),2)
                    || !level.getBiome(p.blockPosition()).is(ModBiomes.PALE_FOREST)
                    || !LoadedFaunaTerrain.search(level)) continue;
            if (count(level,p.position())>=HERD_CAP) continue;
            attempt(level,p);
        }
    }
    private static int count(ServerLevel level,Vec3 center) {
        Set<UUID> groups=new HashSet<>();
        for (PaleDeerEntity d:level.getEntitiesOfClass(PaleDeerEntity.class,new AABB(center,center).inflate(ACTIVE_RADIUS,64,ACTIVE_RADIUS),
                deer -> deer.isAlive() && deer.herdId()!=null && deer.position().subtract(center).horizontalDistanceSqr()<=ACTIVE_RADIUS*ACTIVE_RADIUS))
            groups.add(d.herdId());
        return groups.size();
    }
    private static boolean eligible(ServerLevel level,long cell) {
        // Stable per seed/territory. Re-entering a cell cannot reroll its population.
        return new Random(level.getSeed() ^ Long.rotateLeft(cell*0x9e3779b97f4a7c15L,17)).nextInt(100)<OCCUPANCY_PERCENT;
    }
    private static boolean farFromPlayers(ServerLevel level,Vec3 at) {
        for (ServerPlayer p:level.players()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            double d=p.position().subtract(at).horizontalDistanceSqr();
            if (d<SPAWN_MIN*SPAWN_MIN) return false;
            // Prefer hidden spawns where extended lantern/aura clarity might reveal a new herd.
            if (d<72*72) {
                Vec3 eye=p.getEyePosition(), body=at.add(0,2,0);
                if (!LoadedFaunaTerrain.loaded(level,new AABB(eye,body),2)
                        || level.clip(new ClipContext(eye,body,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.BLOCK) return false;
            }
        }
        return true;
    }
    private static boolean canSpawn(ServerLevel level,Vec3 center) {
        if (count(level,center)>=HERD_CAP) return false;
        for (ServerPlayer p:level.players()) {
            if (p.isAlive() && !p.isSpectator()
                    && p.position().subtract(center).horizontalDistanceSqr()<=(ACTIVE_RADIUS+SPREAD)*(ACTIVE_RADIUS+SPREAD)
                    && count(level,p.position())>=HERD_CAP) return false;
        }
        return true;
    }
    private static int size(ServerLevel level) {
        int roll=level.random.nextInt(100);
        return (roll<25?3:roll<75?5:roll<95?7:9)+level.random.nextInt(2);
    }
    private static boolean allowedSpawn(ServerLevel level,InkLandsLayout layout,BlockPos at) {
        return layout.depthInside(at.getX(),at.getZ())>10
                && !layout.nearRoad(at.getX(),at.getZ(),4)
                && !Sanctuary.near(layout,at.getX(),at.getZ(),6)
                && farFromPlayers(level,Vec3.atBottomCenterOf(at));
    }
    private static void attempt(ServerLevel level,ServerPlayer player) {
        InkLandsLayout layout=InkLandsLayout.of(level);
        if (layout==null) return;
        PaleDeerHerd data=PaleDeerHerd.of(level);
        for (int i=0;i<SPAWN_COLUMNS;i++) {
            double angle=level.random.nextDouble()*Math.PI*2;
            double r=SPAWN_MIN+level.random.nextDouble()*(SPAWN_MAX-SPAWN_MIN);
            int x=Mth.floor(player.getX()+Math.cos(angle)*r), z=Mth.floor(player.getZ()+Math.sin(angle)*r);
            BlockPos center=site(level,x,z,true);
            if (center==null || Math.abs(center.getY()-player.getY())>24) continue;
            long cell=PaleDeerHerd.cell(center);
            if (!eligible(level,cell) || !data.available(cell,level.getGameTime())
                    || !allowedSpawn(level,layout,center) || !canSpawn(level,Vec3.atBottomCenterOf(center))) continue;

            int desired=size(level);
            List<BlockPos> sites=new ArrayList<>();
            sites.add(center);
            // Fit the actual herd to dry, open terrain rather than placing members inside trees.
            for (int j=0;j<desired*6 && sites.size()<desired;j++) {
                int dx=level.random.nextInt(29)-14,dz=level.random.nextInt(29)-14;
                if (dx*dx+dz*dz>14*14) continue;
                BlockPos at=site(level,x+dx,z+dz,true);
                if (at==null || PaleDeerHerd.cell(at)!=cell || Math.abs(at.getY()-center.getY())>4
                        || sites.stream().anyMatch(p -> p.distSqr(at)<3.5*3.5)
                        || !allowedSpawn(level,layout,at)) continue;
                sites.add(at);
            }
            if (sites.size()<HERD_MIN) continue;
            List<PaleDeerEntity> deer=new ArrayList<>();
            for (BlockPos at:sites) {
                PaleDeerEntity d=ModMobs.PALE_DEER.create(level);
                if (d==null) continue;
                d.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,level.random.nextFloat()*360,0);
                if (!LoadedFaunaTerrain.free(level,d,d.getBoundingBox()) || !level.noCollision(d.getBoundingBox())) continue;
                d.finalizeSpawn(level,level.getCurrentDifficultyAt(at),MobSpawnType.NATURAL,null);
                deer.add(d);
            }
            if (deer.size()<HERD_MIN) continue;
            PaleDeerHerd.Group g=data.create(level,center,true);
            for (PaleDeerEntity d:deer) {
                d.configureHerd(g.id);
                if (level.addFreshEntity(d)) data.join(g,d.getUUID());
            }
            if (!g.members.isEmpty()) return;
            // No entity was inserted (e.g. a spawn hook cancelled it): retry on a later encounter.
            g.replenishAt=level.getGameTime()+SPAWN_INTERVAL;
            data.setDirty();
            return;
        }
    }
    /** Surface only. Never asks the generator for a missing column or changes any block. */
    static BlockPos site(ServerLevel level,int x,int z,boolean spawn) {
        var chunk=LoadedFaunaTerrain.chunk(level,new BlockPos(x,0,z));
        if (chunk==null) return null;
        int y=LoadedFaunaTerrain.surface(chunk,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        // Heightmap includes occasional tall grass: try the first three already loaded blocks.
        for (int drop=0;drop<=2;drop++) {
            BlockPos at=new BlockPos(x,y-drop,z);
            if (at.getY()<=level.getMinBuildHeight()+1 || at.getY()+HEIGHT>=level.getMaxBuildHeight()) continue;
            Vec3 p=Vec3.atBottomCenterOf(at);
            AABB box=new AABB(p.x-WIDTH/2,p.y,p.z-WIDTH/2,p.x+WIDTH/2,p.y+HEIGHT,p.z+WIDTH/2);
            if (!LoadedFaunaTerrain.free(level,null,box)
                    || !level.getBiome(at).is(ModBiomes.PALE_FOREST)
                    || !level.getFluidState(at).isEmpty() || !level.getFluidState(at.below()).isEmpty()) continue;
            var below=level.getBlockState(at.below());
            if (spawn && !below.is(ModBlocks.ASHEN_SOIL)) continue;
            if (!spawn && !below.is(ModBlocks.ASHEN_SOIL) && !below.is(ModBlocks.ASHEN_PATH)) continue;
            if (!below.isFaceSturdy(level,at.below(),Direction.UP)) continue;
            if (spawn && !level.noCollision(box)) continue;
            if (spawn) {
                boolean ruin=false;
                for (BlockPos q:BlockPos.betweenClosed(at.offset(-2,-1,-2),at.offset(2,1,2))) {
                    var b=level.getBlockState(q);
                    if (b.is(ModBlocks.RUIN_BRICKS)||b.is(ModBlocks.CRACKED_RUIN_BRICKS)||b.is(ModBlocks.OVERGROWN_RUIN_BRICKS)
                            ||b.is(ModBlocks.RUIN_TILES)||b.is(ModBlocks.RUIN_BRICK_WALL)||b.is(ModBlocks.RUIN_PILLAR)) { ruin=true; break; }
                }
                if (ruin) continue;
            }
            return at;
        }
        return null;
    }
}
