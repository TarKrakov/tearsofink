package com.tarkrakov.tearsofink.world.adit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;

import java.util.HashSet;

/** Test-only access to the real package-private adit planner. */
public final class D060AditProbe {
    public static JsonObject inspect(ServerLevel level, InkLandsLayout layout, LevelHeightAccessor height, JsonArray failures) {
        var source = level.getChunkSource();
        var gen = source.getGenerator();
        var random = source.randomState();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TearsOfInk.id("pale_adit"));
        if (structure == null) throw new IllegalStateException("Missing pale_adit");
        var ctx = new GenerationContext(level.registryAccess(), gen, gen.getBiomeSource(),
                random, level.getStructureManager(), level.getSeed(), new ChunkPos(0,0), height, structure.biomes()::contains);
        long start = System.nanoTime();
        var plan = AditSites.plan(layout, ctx);
        var result = new JsonObject();
        result.addProperty("count", plan.size());
        result.addProperty("plan_ms", (System.nanoTime()-start)/1e6);
        var entries = plan.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).toList();
        var slots = new HashSet<String>();
        var sites = new JsonArray();
        int valid = 0;
        double minGap = Double.POSITIVE_INFINITY;
        for (var entry : entries) {
            var pos = new ChunkPos(entry.getKey());
            var site = entry.getValue();
            String slot = site.sector()+":"+site.band();
            if (!slots.add(slot)) failures.add("Duplicate adit slot "+slot);
            sites.add(pos.x+","+pos.z+","+site.sector()+","+site.band()+","+site.surface());
            for (var other : entries) if (!other.getKey().equals(entry.getKey())) {
                var b = new ChunkPos(other.getKey());
                minGap = Math.min(minGap, Math.hypot((pos.x-b.x)*16,(pos.z-b.z)*16));
            }
            var predicted = structure.generate(level.registryAccess(),gen,gen.getBiomeSource(),random,
                    level.getStructureManager(),level.getSeed(),pos,0,height,structure.biomes()::contains);
            if (predicted.isValid()) valid++;
            else failures.add("Invalid predicted adit StructureStart "+pos);
        }
        result.add("sites",sites);
        result.addProperty("valid_predicted_starts",valid);
        if (Double.isFinite(minGap)) result.addProperty("min_center_gap",minGap);
        if (plan.size()!=20 || slots.size()!=20 || valid!=20 || minGap<324)
            failures.add("Expected 20 unique adit starts with sufficient spacing");
        return result;
    }
}
