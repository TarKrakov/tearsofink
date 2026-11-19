package com.tarkrakov.tearsofink.world.adit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import java.util.ArrayList;

public final class D060PhysicalAdits {
    public static JsonObject inspect(ServerLevel level,InkLandsLayout layout,LevelHeightAccessor height,JsonArray failures) {
        var source=level.getChunkSource();
        var gen=source.getGenerator();
        var random=source.randomState();
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("pale_adit"));
        if(structure==null) throw new IllegalStateException("Missing adit structure");
        var ctx=new GenerationContext(level.registryAccess(),gen,gen.getBiomeSource(),random,
            level.getStructureManager(),level.getSeed(),new ChunkPos(0,0),height,structure.biomes()::contains);
        var plan=AditSites.plan(layout,ctx);
        JsonArray sites=new JsonArray();
        int built=0;
        var entrances=new ArrayList<BlockPos>();
        for(var entry:plan.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).toList()) {
            var cp=new ChunkPos(entry.getKey());
            var chunk=level.getChunk(cp.x,cp.z);
            var start=chunk.getStartForStructure(structure);
            AditPiece adit=null;
            if(start!=null && start.isValid()) for(var piece:start.getPieces())
                if(piece instanceof AditPiece p) { adit=p; break; }
            JsonObject site=new JsonObject();
            site.addProperty("chunk",cp.toString());
            site.addProperty("sector",entry.getValue().sector());
            site.addProperty("band",entry.getValue().band());
            boolean ok=adit!=null && adit.d060Entrance()!=null && adit.d060Landing()!=null;
            if(ok) {
                var entrance=adit.d060Entrance(); var landing=adit.d060Landing();
                entrances.add(entrance);
                site.addProperty("entrance",entrance.toShortString());
                site.addProperty("landing",landing.toShortString());
                int wrong=0;
                var expected=adit.d060Interior(cp);
                for(var cell:expected.entrySet()) {
                    var actual=level.getBlockState(cell.getKey());
                    var state=cell.getValue();
                    boolean matching=actual.getFluidState().isEmpty();
                    if(state.isAir()) matching &= actual.getCollisionShape(level,cell.getKey()).isEmpty();
                    else {
                        matching &= actual.is(state.getBlock());
                        if(matching && state.getBlock() instanceof StairBlock)
                            matching &= actual.getValue(StairBlock.FACING)==state.getValue(StairBlock.FACING);
                    }
                    if(!matching) wrong++;
                }
                site.addProperty("interior_cells",expected.size());
                site.addProperty("wrong_cells",wrong);
                ok=expected.size()>0 && wrong==0
                    && AditStructure.nearBuiltPassage(level,entrance)
                    && AditStructure.nearBuiltPassage(level,landing);
            }
            site.addProperty("built",ok);
            if(ok) built++; else failures.add("Physical adit missing/blocked "+cp);
            sites.add(site);
        }
        double minGap=Double.POSITIVE_INFINITY;
        for(int a=0;a<entrances.size();a++) for(int b=a+1;b<entrances.size();b++) {
            var x=entrances.get(a); var y=entrances.get(b);
            minGap=Math.min(minGap,Math.hypot(x.getX()-y.getX(),x.getZ()-y.getZ()));
        }
        if(plan.size()!=20 || built!=20 || minGap<300) failures.add("Physical adits "+built+"/20 or spacing <300");
        JsonObject result=new JsonObject();
        result.addProperty("planned",plan.size());
        result.addProperty("built",built);
        if(Double.isFinite(minGap)) result.addProperty("min_actual_entrance_gap",minGap);
        result.add("sites",sites);
        return result;
    }
}
