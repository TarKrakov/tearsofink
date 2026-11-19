package com.tarkrakov.tearsofink.travel;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Test-only. Used in a disposable dedicated server with two connected clients. */
public final class D060RespawnProbe {
    private static Trial trial;
    private record Trial(ServerPlayer a, ServerPlayer b, BlockPos targetA, BlockPos targetB,
                         int startTick, int releaseTick) {}
    private static boolean released;
    private static boolean isolated;

    private D060RespawnProbe() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            var manual = Commands.argument("targetB", BlockPosArgument.blockPos())
                .executes(c -> start(c.getSource().getServer(),
                    StringArgumentType.getString(c,"a"),
                    StringArgumentType.getString(c,"b"),
                    BlockPosArgument.getBlockPos(c,"targetA"),
                    BlockPosArgument.getBlockPos(c,"targetB")));
            var b = Commands.argument("b", StringArgumentType.word())
                .executes(c -> autoStart(c.getSource().getServer(),
                    StringArgumentType.getString(c,"a"), StringArgumentType.getString(c,"b")))
                .then(Commands.argument("targetA", BlockPosArgument.blockPos()).then(manual));
            dispatcher.register(Commands.literal("toi-d060-respawn")
                .requires(s -> s.hasPermission(4))
                .then(Commands.argument("a", StringArgumentType.word()).then(b)));
        });
        ServerTickEvents.END_SERVER_TICK.register(D060RespawnProbe::tick);
    }

    // Installed only into a temporary copy of ModDimensions.chunksReady.
    public static boolean hold(ServerLevel level, BlockPos pos, int radius) {
        return trial != null && !released && level == trial.a().serverLevel()
                && radius == 3 && pos.equals(trial.targetA());
    }

    /** Select actual generated fountains, not invented blocks. Test setup is administrative. */
    private static int autoStart(MinecraftServer server, String nameA, String nameB) {
        try {
            ServerPlayer a = server.getPlayerList().getPlayerByName(nameA);
            ServerPlayer b = server.getPlayerList().getPlayerByName(nameB);
            ServerLevel level = ModDimensions.lands(server);
            if (a == null || b == null || a == b || level == null)
                throw new IllegalStateException("Two clients must be connected");
            var layout = InkLandsLayout.of(level);
            var plan = layout.templeNetwork();
            if (plan == null || plan.temples().size() != 13)
                throw new IllegalStateException("Requires a complete generated V3 plan");
            var arrival = ModDimensions.arrival(level, 0);
            var template = level.getStructureManager().getOrCreate(Ruins.TEMPLE);
            var size = template.getSize();
            List<BlockPos> targets = new ArrayList<>();
            for (var temple : plan.temples()) {
                if (temple.starter() || temple.road() < 0) continue;
                var origin = new BlockPos(temple.x()-size.getX()/2, temple.y(), temple.z()-size.getZ()/2);
                var settings = new StructurePlaceSettings().setRotation(temple.rotation())
                    .setRotationPivot(new BlockPos(size.getX()/2,0,size.getZ()/2));
                var found = template.filterBlocks(origin,settings,ModBlocks.INK_FOUNTAIN);
                if (found.size() != 1) continue;
                var at = found.getFirst().pos();
                if (at.distSqr(BlockPos.containing(arrival)) < 65536) continue;
                if (!targets.isEmpty() && at.distSqr(targets.getFirst()) < 65536) continue;
                for (int dx=-1; dx<=1; dx++) for(int dz=-1; dz<=1; dz++)
                    level.getChunk((at.getX()>>4)+dx,(at.getZ()>>4)+dz);
                if (!level.getBlockState(at).is(ModBlocks.INK_FOUNTAIN)) continue;
                targets.add(at);
                if (targets.size()==2) break;
            }
            if (targets.size()!=2) throw new IllegalStateException("Could not locate two generated fountains");
            for (var player : List.of(a,b)) {
                player.setGameMode(GameType.SURVIVAL);
                player.teleportTo(level,arrival.x,arrival.y,arrival.z,0f,0f);
                Travel.set(player,Travel.get(player).withStage(LandsTravel.STAGE_ENTERED));
            }
            TearsOfInk.LOGGER.info("D060_RESPAWN_TARGETS {} and {}",targets.get(0),targets.get(1));
            return start(server,nameA,nameB,targets.get(0),targets.get(1));
        } catch (Exception failure) {
            JsonObject result = new JsonObject();
            result.addProperty("pass",false);
            result.addProperty("failure",failure.toString());
            TearsOfInk.LOGGER.error("D060 respawn setup failed",failure);
            TearsOfInk.LOGGER.info("D060_RESPAWN_RESULT {}",result);
            return 0;
        }
    }

    private static int start(MinecraftServer server, String nameA, String nameB, BlockPos targetA, BlockPos targetB) {
        if (trial != null) throw new IllegalStateException("Already running a respawn trial");
        ServerPlayer a = server.getPlayerList().getPlayerByName(nameA);
        ServerPlayer b = server.getPlayerList().getPlayerByName(nameB);
        ServerLevel level = ModDimensions.lands(server);
        if (a == null || b == null || a == b || level == null || a.serverLevel() != level
                || b.serverLevel() != level || !a.isAlive() || !b.isAlive()
                || !Travel.bound(a) || !Travel.bound(b))
            throw new IllegalStateException("Two live, distinct survival players in Ink Lands required");
        if (targetA.distSqr(targetB) < 65536 || targetA.distSqr(a.blockPosition()) < 65536
                || targetB.distSqr(b.blockPosition()) < 65536
                || !ModDimensions.chunksReady(level,targetA,3) || !ModDimensions.chunksReady(level,targetB,3)
                || !level.getBlockState(targetA).is(ModBlocks.INK_FOUNTAIN)
                || !level.getBlockState(targetB).is(ModBlocks.INK_FOUNTAIN))
            throw new IllegalStateException("Two preloaded real distant fountains required");
        Travel.set(a, Travel.get(a).withRespawn(targetA,false));
        Travel.set(b, Travel.get(b).withRespawn(targetB,false));
        Travel.preloadRespawn(a,targetA);
        Travel.preloadRespawn(b,targetB);
        int tick = server.getTickCount();
        released = false;
        isolated = false;
        trial = new Trial(a,b,targetA,targetB,tick,tick+100);
        // Same vanilla server packet handler as real death-screen requests; this
        // test injects the initial packet server-side, not by a network client.
        // In particular, there is no test-injected retry after release.
        a.hurt(a.damageSources().genericKill(), Float.MAX_VALUE);
        b.hurt(b.damageSources().genericKill(), Float.MAX_VALUE);
        if (a.isAlive() || b.isAlive()) {
            finish(server,"Initial death was prevented");
            return 0;
        }
        var packet = new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN);
        a.connection.handleClientCommand(packet);
        b.connection.handleClientCommand(packet);
        TearsOfInk.LOGGER.info("D060_RESPAWN_START {} {}", a.getUUID(), b.getUUID());
        return 1;
    }

    private static boolean restored(MinecraftServer server, ServerPlayer old, BlockPos target) {
        ServerPlayer p = server.getPlayerList().getPlayer(old.getUUID());
        return p != null && p != old && p.isAlive() && ModDimensions.isLands(p.level())
                && p.blockPosition().distSqr(target) <= 16
                && Travel.get(p).respawn().filter(target::equals).isPresent();
    }

    private static void tick(MinecraftServer server) {
        Trial t = trial;
        if (t == null) return;
        int tick = server.getTickCount();
        if (server.getPlayerList().getPlayer(t.a().getUUID()) == null
                || server.getPlayerList().getPlayer(t.b().getUUID()) == null) {
            finish(server,"A client disconnected"); return;
        }
        if (!released) {
            if (server.getPlayerList().getPlayer(t.a().getUUID()) != t.a() || t.a().isAlive()) {
                finish(server,"A respawned before destination became ready"); return;
            }
            if (restored(server,t.b(),t.targetB())) isolated = true;
            if (tick >= t.releaseTick()) {
                if (!isolated) { finish(server,"B did not respawn while A was held"); return; }
                released = true;
                TearsOfInk.LOGGER.info("D060_RESPAWN_RELEASE {}",tick);
            }
        }
        if (released && restored(server,t.a(),t.targetA()) && restored(server,t.b(),t.targetB())) {
            finish(server,null);
        } else if (tick >= t.releaseTick()+240) {
            finish(server,"No successful automatic retry after release");
        }
    }

    private static void finish(MinecraftServer server,String failure) {
        Trial t = trial;
        if (t == null) return;
        JsonObject result = new JsonObject();
        result.addProperty("pass",failure == null);
        result.addProperty("held_a",isolated);
        result.addProperty("released",released);
        result.addProperty("a_restored",restored(server,t.a(),t.targetA()));
        result.addProperty("b_restored",restored(server,t.b(),t.targetB()));
        result.addProperty("elapsed_ticks",server.getTickCount()-t.startTick());
        if (failure != null) result.addProperty("failure",failure);
        trial = null;
        released = true;
        TearsOfInk.LOGGER.info("D060_RESPAWN_RESULT {}",result);
    }
}
