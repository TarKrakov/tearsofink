package com.tarkrakov.tearsofink.entity.patrol;

import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.entity.event.lifecycle.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import java.util.List;

/** Roadside gate slots: only the authored gate can activate a garrison. */
public final class Outposts {
    public static final int RESPAWN = 20 * 60 * 20;
    public static final int WAKE = 72, FORGET = 140;
    private static final int INSTALL_WAKE = 112;
    public static final String[] SQUADS = {"SSC", "TSCC", "TTSSCC"}; // historical composition, not a fallback
    private Outposts() {}

    public static void init() {
        BarricadeGates.initProtection();
        ServerTickEvents.END_SERVER_TICK.register(Outposts::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if(entity instanceof PaleSoldier && entity.level() instanceof ServerLevel level && ModDimensions.isLands(level))
                BarricadeGates.of(level).died(level,entity.getUUID());
        });
    }
    private static void tick(MinecraftServer server) {
        if(server.getTickCount()%20!=0)return;
        ServerLevel level=ModDimensions.lands(server);
        if(level==null)return;
        BarricadeGates gates=BarricadeGates.of(level);
        gates.tickFlames(level);
        InkLandsLayout layout=InkLandsLayout.of(level);
        if(layout==null)return;
        boolean canSpawn=level.getDifficulty()!=Difficulty.PEACEFUL&&level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        for(int road=0;road<InkLandsLayout.ROADS;road++)for(int n=0;n<InkLandsLayout.OUTPOSTS.length;n++) {
            double d=layout.outpostDistance(n);
            double[] at=layout.roadPoint(road,d);
            double nearest=Double.MAX_VALUE;
            for(ServerPlayer player:level.players())if(player.isAlive()&&!player.isSpectator())
                nearest=Math.min(nearest,Math.hypot(player.getX()-at[0],player.getZ()-at[1]));
            var gate=gates.get(road,n);
            if(gate==null&&nearest<=INSTALL_WAKE) {
                gate=gates.tryInstall(level,layout,road,n);
                if(gate!=null)gates.adoptLegacy(level,gate,road,d,List.of());
            }
            if(gate!=null)gates.garrison(level,layout,road,d,gate,nearest,canSpawn);
            // Never activate a second, legacy placeholder garrison on an unconverted site.
        }
    }
}
