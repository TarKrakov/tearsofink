package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tarkrakov.tearsofink.pantheon.PantheonArenaScenes.*;
import static com.tarkrakov.tearsofink.pantheon.PantheonArenaScenes.Scene.NEUTRAL;

/** One server-owned reconstruction job in the installed author's temple. */
final class PantheonArena {
    private static final int VISITS = 4096, WRITES = 512;
    private static final long BUDGET_NS = 3_000_000;
    private static final int LOAD_TIMEOUT = 400;
    private static final String JOURNAL = "tearsofink_pantheon_scene_v1.txt";
    private static final TicketType<ChunkPos> TICKET = TicketType.create("tearsofink_pantheon_arena",
            Comparator.comparingLong(ChunkPos::toLong));
    private static final BlockState CLOSED_GATE = Blocks.TINTED_GLASS.defaultBlockState();
    private static final BlockState OPEN_GATE = Blocks.AIR.defaultBlockState();
    private static final List<BlockPos> GATE = List.of(
            PantheonTemple.world(new BlockPos(57, 2, 39)), PantheonTemple.world(new BlockPos(58, 2, 39)),
            PantheonTemple.world(new BlockPos(57, 3, 39)), PantheonTemple.world(new BlockPos(58, 3, 39)));
    private static final ThreadLocal<Write> WRITE = new ThreadLocal<>();
    private static final AABB INTERIOR = new AABB(
            Vec3.atLowerCornerOf(PantheonTemple.world(new BlockPos(MIN_X, FLOOR, MIN_Z))),
            Vec3.atLowerCornerOf(PantheonTemple.world(new BlockPos(MAX_X + 1, CEILING + 1, MAX_Z + 1))));

    private enum Phase { LOADING, CHECKING, APPLYING }
    private record Write(ServerLevel level, BlockPos pos, BlockState state) {}
    private record Cell(BlockPos pos, BlockState[] states) {
        boolean accepts(BlockState state) {
            for (BlockState candidate : states) {
                if (candidate.equals(state)) return true;
            }
            return false;
        }
    }
    private record Layout(List<Cell> cells, List<ChunkPos> chunks, int freeColumns) {}
    private record Stats(int writes, double durationMs, double peakMs) {}

    private static Scene selected = NEUTRAL;
    private static Layout layout;
    private static Job job;
    private static boolean locked;
    private static boolean badJournal;
    private static String failure;
    private static Stats last;

    private static final class Job {
        final Scene target;
        final long began = System.nanoTime();
        final Map<Long, LevelChunk> chunks = new HashMap<>();
        Phase phase = Phase.LOADING;
        int loadingTicks;
        int cursor;
        int writes;
        long peak;
        boolean tickets;

        Job(Scene target) {
            this.target = target;
        }
    }

    private PantheonArena() {}

    static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var arena = Commands.literal("arena")
                    .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                    .then(Commands.literal("reset").executes(ctx -> request(ctx.getSource(), NEUTRAL)));
            for (Scene scene : Scene.values()) {
                arena.then(Commands.literal(scene.id()).executes(ctx -> request(ctx.getSource(), scene)));
            }
            dispatcher.register(Commands.literal("tearsofink").requires(source -> source.hasPermission(2))
                    .then(Commands.literal("pantheon").then(arena)));
        });
        ServerLifecycleEvents.SERVER_STARTED.register(PantheonArena::restore);
        ServerTickEvents.END_SERVER_TICK.register(PantheonArena::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> evacuate(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            release(server.getLevel(PantheonTemple.DIMENSION));
            job = null;
            layout = null;
            locked = false;
            badJournal = false;
            failure = null;
            last = null;
            selected = NEUTRAL;
        });
    }

    static boolean allowsWrite(Level level, BlockPos pos, BlockState state) {
        Write permission = WRITE.get();
        return permission != null && permission.level() == level && permission.pos().equals(pos)
                && permission.state().equals(state);
    }

    private static boolean put(ServerLevel level, BlockPos pos, BlockState state) {
        WRITE.set(new Write(level, pos, state));
        try {
            return level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        } finally {
            WRITE.remove();
        }
    }

    private static Layout inspect() throws Exception {
        var template = PantheonTemple.load();
        var free = new boolean[WIDTH][DEPTH];
        int columns = 0;
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                BlockState floor = PantheonTemple.authored(template, new BlockPos(x, FLOOR, z));
                if (!floor.is(ModBlocks.SANCTUM_FLOOR_TILE) && !floor.is(ModBlocks.SANCTUM_CARVED_TILE)) continue;
                boolean clear = true;
                for (int y = 2; y <= 21; y++) {
                    if (!PantheonTemple.authored(template, new BlockPos(x, y, z)).isAir()) {
                        clear = false;
                        break;
                    }
                }
                free[x - MIN_X][z - MIN_Z] = clear;
                if (clear) columns++;
            }
        }
        // The 57x47 approved central footprint must still be made of the original planar tiles.
        for (int x = 30; x <= 86; x++) {
            for (int z = 63; z <= 109; z++) {
                if (!free[x - MIN_X][z - MIN_Z]) throw new IOException("Изменилась геометрия центрального поля");
            }
        }
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        GATE.forEach(pos -> chunks.add(new ChunkPos(pos)));
        chunks.add(new ChunkPos(PantheonTemple.entrance()));
        List<Cell> cells = new ArrayList<>();
        Scene[] presets = Scene.values();
        for (int index = 0; index < VOLUME; index++) {
            BlockPos p = local(index);
            if (!free[p.getX() - MIN_X][p.getZ() - MIN_Z]) continue;
            BlockState neutral = PantheonTemple.authored(template, p);
            BlockState[] states = new BlockState[presets.length];
            boolean changes = false;
            for (Scene preset : presets) {
                BlockState state = preset.block(p, neutral, free);
                if (state.hasBlockEntity() || neutral.hasBlockEntity()) throw new IOException("Блок-сущность в маске арены");
                states[preset.ordinal()] = state;
                changes |= !state.equals(neutral);
            }
            if (!changes) continue;
            BlockPos world = PantheonTemple.world(p);
            cells.add(new Cell(world, states));
            chunks.add(new ChunkPos(world));
        }
        verifyBasin(template, cells);
        return new Layout(List.copyOf(cells), List.copyOf(chunks), columns);
    }

    private static void verifyBasin(PantheonTemple.Template template, List<Cell> cells) throws IOException {
        var island = new HashMap<BlockPos, BlockState>();
        for (Cell cell : cells) island.put(PantheonTemple.local(cell.pos()), cell.states()[Scene.ISLAND.ordinal()]);
        for (var entry : island.entrySet()) {
            if (!entry.getValue().is(Blocks.WATER)) continue;
            BlockPos source = entry.getKey();
            for (Direction direction : List.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)) {
                BlockPos neighbor = source.relative(direction);
                BlockState state = island.get(neighbor);
                if (state == null) state = PantheonTemple.authored(template, neighbor);
                if (direction != Direction.DOWN && state.is(Blocks.WATER)) continue;
                if (!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, neighbor)) {
                    throw new IOException("Негерметичная граница бассейна " + neighbor.toShortString());
                }
            }
        }
    }

    private static Path journal(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(JOURNAL);
    }

    private static void restore(MinecraftServer server) {
        Path file = journal(server);
        if (Files.notExists(file)) return;
        locked = true;
        try {
            String id = Files.readString(file).strip();
            for (Scene scene : Scene.values()) {
                if (scene.id().equals(id)) {
                    layout = inspect();
                    selected = scene;
                    job = new Job(scene);
                    return;
                }
            }
            throw new IOException("Неизвестная сцена: " + id);
        } catch (Exception ex) {
            badJournal = true;
            failure = ex.getMessage();
            TearsOfInk.LOGGER.error("Pantheon arena: cannot restore its saved scene", ex);
        }
    }

    private static void save(MinecraftServer server, Scene target) throws IOException {
        Path file = journal(server);
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "pantheon-arena-", ".tmp");
        try {
            Files.writeString(temp, target.id() + "\n");
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    static boolean preparing() { return job != null; }

    static boolean neutralReady() { return !locked && job == null && selected == NEUTRAL; }

    static int request(CommandSourceStack source, Scene target) {
        ServerLevel level = source.getServer().getLevel(PantheonTemple.DIMENSION);
        if (level == null || !PantheonTemple.ready(level) || badJournal || job != null) {
            source.sendFailure(Component.literal("Арена сейчас недоступна. Проверьте arena status."));
            return 0;
        }
        try {
            if (layout == null) layout = inspect();
            save(source.getServer(), target);
        } catch (Exception ex) {
            source.sendFailure(Component.literal("Арена: не удалось сохранить переход. " + ex.getMessage()));
            return 0;
        }
        selected = target;
        locked = true;
        failure = null;
        job = new Job(target);
        for (ServerPlayer player : List.copyOf(level.players())) evacuate(player);
        source.sendSuccess(() -> Component.literal("Арена закрыта для смены сцены: " + target.id()), false);
        return 1;
    }

    private static void evacuate(ServerPlayer player) {
        if (!locked || !PantheonTemple.isTemple(player.level()) || !player.isAlive() || player.getZ() < 39) return;
        Vec3 to = Vec3.atBottomCenterOf(PantheonTemple.entrance());
        player.stopRiding();
        player.connection.teleport(to.x, to.y, to.z, 0, 0);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
    }

    private static void tick(MinecraftServer server) {
        if (!locked) return;
        ServerLevel level = server.getLevel(PantheonTemple.DIMENSION);
        if (level == null) return;
        for (ServerPlayer player : List.copyOf(level.players())) evacuate(player);
        Job active = job;
        if (active == null || !PantheonTemple.ready(level)) return;

        if (active.phase == Phase.LOADING) {
            if (!active.tickets) {
                for (ChunkPos pos : layout.chunks()) level.getChunkSource().addRegionTicket(TICKET, pos, 0, pos);
                active.tickets = true;
            }
            if (++active.loadingTicks > LOAD_TIMEOUT) {
                fail(level, "истёк срок загрузки чанков");
                return;
            }
            for (ChunkPos pos : layout.chunks()) {
                var chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk == null) return;
                active.chunks.put(pos.toLong(), chunk);
            }
            if (!gate(level, CLOSED_GATE)) {
                fail(level, "проём занят посторонним блоком");
                return;
            }
            active.phase = Phase.CHECKING;
            return;
        }

        if (!level.getEntitiesOfClass(Entity.class, INTERIOR, entity -> !(entity instanceof ServerPlayer)).isEmpty()) {
            fail(level, "внутри маски осталась сущность");
            return;
        }
        runBatch(level, active);
    }

    private static void runBatch(ServerLevel level, Job active) {
        long start = System.nanoTime();
        int writes = 0;
        int visited = 0;
        while (active.cursor < layout.cells().size() && visited++ < VISITS && writes < WRITES
                && System.nanoTime() - start < BUDGET_NS) {
            Cell cell = layout.cells().get(active.cursor++);
            BlockPos pos = cell.pos();
            var chunk = active.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
            BlockState existing = chunk.getBlockState(pos);
            if (existing.hasBlockEntity() || !cell.accepts(existing)) {
                fail(level, "посторонний блок " + pos.toShortString());
                return;
            }
            if (active.phase == Phase.CHECKING) continue;
            BlockState target = cell.states()[active.target.ordinal()];
            if (!existing.equals(target)) {
                if (!put(level, pos, target)) {
                    fail(level, "запись отклонена в " + pos.toShortString());
                    return;
                }
                writes++;
            }
        }
        active.writes += writes;
        active.peak = Math.max(active.peak, System.nanoTime() - start);
        if (active.cursor != layout.cells().size()) return;
        if (active.phase == Phase.CHECKING) {
            active.phase = Phase.APPLYING;
            active.cursor = 0;
            return;
        }
        if (!gate(level, OPEN_GATE)) {
            fail(level, "проём изменён");
            return;
        }
        last = new Stats(active.writes, (System.nanoTime() - active.began) / 1_000_000.0, active.peak / 1_000_000.0);
        TearsOfInk.LOGGER.info("Pantheon arena: READY {} | {} writes | {} ms | max batch {} ms",
                selected.id(), last.writes(), last.durationMs(), last.peakMs());
        release(level);
        job = null;
        locked = false;
    }

    private static boolean gate(ServerLevel level, BlockState target) {
        for (BlockPos pos : GATE) {
            BlockState state = level.getBlockState(pos);
            if (!state.equals(OPEN_GATE) && !state.equals(CLOSED_GATE)) return false;
        }
        for (BlockPos pos : GATE) {
            if (!level.getBlockState(pos).equals(target) && !put(level, pos, target)) return false;
        }
        return true;
    }

    private static void fail(ServerLevel level, String why) {
        failure = why;
        release(level);
        job = null;
        TearsOfInk.LOGGER.error("Pantheon arena remains closed: {}", why);
    }

    private static void release(ServerLevel level) {
        if (level == null || job == null || !job.tickets) return;
        for (ChunkPos pos : layout.chunks()) level.getChunkSource().removeRegionTicket(TICKET, pos, 0, pos);
    }

    private static int status(CommandSourceStack source) {
        String phase = failure != null ? "CLOSED — " + failure
                : job != null ? (selected == NEUTRAL ? "RESTORING " : "PREPARING ") + job.phase
                + " " + job.cursor + "/" + layout.cells().size() + ", записей " + job.writes
                : selected == NEUTRAL ? "NEUTRAL" : "READY";
        String mask = layout == null ? "" : ", колонны маски " + layout.freeColumns() + ", ячеек " + layout.cells().size()
                + ", чанков " + layout.chunks().size();
        String timing = last == null ? "" : String.format(java.util.Locale.ROOT,
                ", последний переход %.1f мс, пик порции %.2f мс, записей %d", last.durationMs(), last.peakMs(), last.writes());
        source.sendSuccess(() -> Component.literal("Pantheon arena: " + phase + " | " + selected.id()
                + " | " + selected.lighting + " | конверт " + WIDTH + "×" + DEPTH + "×" + (CEILING - FLOOR + 1)
                + mask + timing), false);
        return 1;
    }
}
