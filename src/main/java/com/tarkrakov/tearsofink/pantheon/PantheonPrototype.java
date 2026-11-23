package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Operator-only, disposable proof of a mutable interior. All block writes are confined
 * to a fixed reservation in the separate empty pantheon_prototype dimension.
 * This is NOT the eventual temple, altar, boss system, or public teleport.
 */
public final class PantheonPrototype {
    public static final ResourceKey<Level> DIMENSION =
            ResourceKey.create(Registries.DIMENSION, TearsOfInk.id("pantheon_prototype"));

    private static final int MIN_X = -10, MAX_X = 10, MIN_Z = 0, MAX_Z = 20;
    private static final int MIN_Y = 78, MAX_Y = 90;
    private static final int WIDTH = MAX_X - MIN_X + 1;
    private static final int DEPTH = MAX_Z - MIN_Z + 1;
    private static final int VOLUME = WIDTH * DEPTH * (MAX_Y - MIN_Y + 1);
    private static final int MAX_VISITS_PER_TICK = 512;
    private static final int MAX_WRITES_PER_TICK = 128;
    private static final BlockPos OWNER_MARKER = new BlockPos(0, 78, -16);
    private static final BlockPos GATE_PROBE = new BlockPos(0, 80, -1);
    private static final Map<UUID, ReturnSpot> RETURNS = new HashMap<>();
    private static Job job;

    private enum Scene { FOREST, ISLAND }
    private record ReturnSpot(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {}

    private static final class Job {
        final Scene target;
        final long started = System.nanoTime();
        int cursor;
        int writes;
        long slowestTick;
        Job(Scene target) { this.target = target; }
    }

    private PantheonPrototype() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) ->
                dispatcher.register(Commands.literal("tearsofink")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("pantheon")
                                .then(Commands.literal("prototype")
                                        .then(Commands.literal("init").executes(PantheonPrototype::initialize))
                                        .then(Commands.literal("enter").executes(PantheonPrototype::enter))
                                        .then(Commands.literal("leave").executes(PantheonPrototype::leave))
                                        .then(Commands.literal("forest").executes(ctx -> switchTo(ctx, Scene.FOREST)))
                                        .then(Commands.literal("island").executes(ctx -> switchTo(ctx, Scene.ISLAND)))
                                        .then(Commands.literal("status").executes(PantheonPrototype::status))))));

        ServerTickEvents.END_SERVER_TICK.register(PantheonPrototype::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (player.serverLevel().dimension() == DIMENSION && isOwned(player.serverLevel())) {
                // Never resume inside a partly replaced scene after a restart/reconnect.
                toStaging(player, player.serverLevel());
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            job = null;
            RETURNS.clear();
        });
    }

    private static ServerLevel level(CommandSourceStack source) {
        ServerLevel result = source.getServer().getLevel(DIMENSION);
        if (result == null) source.sendFailure(Component.literal("Техническое измерение пантеона не загружено."));
        return result;
    }

    private static void loadReservation(ServerLevel level) {
        // Only the separate test dimension; at most eight small, otherwise empty chunks.
        for (int cx = -1; cx <= 0; cx++) {
            for (int cz = -2; cz <= 1; cz++) level.getChunk(cx, cz);
        }
    }

    private static boolean isOwned(ServerLevel level) {
        return level.getBlockState(OWNER_MARKER).is(Blocks.LODESTONE);
    }

    private static int initialize(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = level(source);
        if (level == null) return 0;
        loadReservation(level);
        if (isOwned(level)) {
            source.sendSuccess(() -> Component.literal("Прототип уже зарезервирован. Используйте forest / island / status."), false);
            return 1;
        }
        // Refuse to replace even a single foreign block: first installation is VOID ONLY.
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = -11; x <= 11; x++) for (int z = -20; z <= 21; z++) for (int y = 78; y <= 92; y++) {
            if (!level.getBlockState(at.set(x, y, z)).isAir()) {
                source.sendFailure(Component.literal("Отказ: техническая зона не пуста. Никакие чужие блоки не изменены: " + at.toShortString()));
                return 0;
            }
        }
        // Staging platform never belongs to the swapped interior.
        for (int x = -4; x <= 4; x++) for (int z = -19; z <= -2; z++)
            set(level, at.set(x, 78, z), (Math.abs(x) == 3 && (z == -9 || z == -16))
                    ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
        for (int z = -19; z <= -2; z++) for (int y = 79; y <= 81; y++) {
            set(level, at.set(-4, y, z), Blocks.GLASS.defaultBlockState());
            set(level, at.set(4, y, z), Blocks.GLASS.defaultBlockState());
        }
        for (int x = -4; x <= 4; x++) for (int y = 79; y <= 81; y++)
            set(level, at.set(x, y, -19), Blocks.GLASS.defaultBlockState());
        for (int x = -2; x <= 2; x++) set(level, at.set(x, 78, -1), Blocks.SMOOTH_STONE.defaultBlockState());

        // Fixed outside envelope: walls and roof are never touched by scene replacement.
        for (int y = 79; y <= 91; y++) {
            for (int z = -1; z <= 21; z++) {
                set(level, at.set(-11, y, z), Blocks.GLASS.defaultBlockState());
                set(level, at.set(11, y, z), Blocks.GLASS.defaultBlockState());
            }
            for (int x = -10; x <= 10; x++) {
                set(level, at.set(x, y, -1), Blocks.GLASS.defaultBlockState());
                set(level, at.set(x, y, 21), Blocks.GLASS.defaultBlockState());
            }
        }
        for (int x = -11; x <= 11; x++) for (int z = -1; z <= 21; z++)
            set(level, at.set(x, 92, z), Blocks.GLASS.defaultBlockState());
        gate(level, false);
        // Marker is written last. An interrupted installation fails closed on the next init.
        set(level, OWNER_MARKER, Blocks.LODESTONE.defaultBlockState());
        begin(level, Scene.FOREST);
        source.sendSuccess(() -> Component.literal("Прототип зарезервирован в отдельном пустом измерении. Готовится лес; /tearsofink pantheon prototype enter."), false);
        return 1;
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static void gate(ServerLevel level, boolean open) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        BlockState state = open ? Blocks.AIR.defaultBlockState() : Blocks.TINTED_GLASS.defaultBlockState();
        for (int x = -2; x <= 2; x++) for (int y = 79; y <= 83; y++)
            set(level, at.set(x, y, -1), state);
    }

    private static void toStaging(ServerPlayer player, ServerLevel level) {
        player.teleportTo(level, 0.5, 79.0, -13.5, 0f, 0f);
    }

    private static int enter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel target = level(source);
        if (target == null) return 0;
        loadReservation(target);
        if (!isOwned(target)) {
            source.sendFailure(Component.literal("Сначала выполните /tearsofink pantheon prototype init."));
            return 0;
        }
        if (player.serverLevel() != target) {
            RETURNS.put(player.getUUID(), new ReturnSpot(player.serverLevel().dimension(),
                    player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        }
        toStaging(player, target);
        source.sendSuccess(() -> Component.literal("Тестовый хаб. Лес: /tearsofink pantheon prototype forest; остров: ... island; назад: ... leave."), false);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        if (player.serverLevel().dimension() != DIMENSION) {
            source.sendFailure(Component.literal("Вы не в техническом измерении пантеона."));
            return 0;
        }
        ReturnSpot previous = RETURNS.remove(player.getUUID());
        ServerLevel destination = previous == null ? null : source.getServer().getLevel(previous.dimension());
        if (destination != null) {
            destination.getChunk(BlockPos.containing(previous.x(), previous.y(), previous.z()).getX() >> 4,
                    BlockPos.containing(previous.x(), previous.y(), previous.z()).getZ() >> 4);
            player.teleportTo(destination, previous.x(), previous.y(), previous.z(), previous.yaw(), previous.pitch());
        } else {
            // An operator still has an exit after a server restart (when in-memory return is lost).
            ServerLevel overworld = source.getServer().overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            overworld.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
            int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
            player.teleportTo(overworld, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, player.getYRot(), 0f);
        }
        source.sendSuccess(() -> Component.literal("Вы покинули технический прототип."), false);
        return 1;
    }

    private static int switchTo(CommandContext<CommandSourceStack> ctx, Scene scene) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = level(source);
        if (level == null) return 0;
        loadReservation(level);
        if (!isOwned(level)) {
            source.sendFailure(Component.literal("Технический объём ещё не зарезервирован. Запустите init."));
            return 0;
        }
        if (job != null) {
            source.sendFailure(Component.literal("Перестройка уже выполняется. Проверьте status."));
            return 0;
        }
        begin(level, scene);
        source.sendSuccess(() -> Component.literal("Вход закрыт, участники выведены в хаб. Поэтапная смена сцены: " + scene), false);
        return 1;
    }

    private static void begin(ServerLevel level, Scene scene) {
        for (ServerPlayer player : level.players()) toStaging(player, level);
        gate(level, false);
        job = new Job(scene);
    }

    private static void tick(MinecraftServer server) {
        if (job == null) return;
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null || !isOwned(level)) {
            job = null;
            return;
        }
        Job active = job;
        long started = System.nanoTime();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int examined = 0;
        int writes = 0;
        while (active.cursor < VOLUME && examined < MAX_VISITS_PER_TICK && writes < MAX_WRITES_PER_TICK) {
            int index = active.cursor++;
            int x = MIN_X + index % WIDTH;
            int z = MIN_Z + (index / WIDTH) % DEPTH;
            int y = MIN_Y + index / (WIDTH * DEPTH);
            BlockState desired = sceneBlock(active.target, x, y, z);
            if (!level.getBlockState(at.set(x, y, z)).equals(desired)) {
                level.setBlock(at, desired, Block.UPDATE_CLIENTS);
                writes++;
            }
            examined++;
        }
        active.writes += writes;
        active.slowestTick = Math.max(active.slowestTick, System.nanoTime() - started);
        if (active.cursor >= VOLUME) {
            gate(level, true);
            job = null;
            TearsOfInk.LOGGER.info("Pantheon prototype: {} ready; {} writes in {} ms, max batch {} ms",
                    active.target, active.writes, (System.nanoTime() - active.started) / 1_000_000,
                    active.slowestTick / 1_000_000.0);
        }
    }

    private static BlockState sceneBlock(Scene scene, int x, int y, int z) {
        if (scene == Scene.FOREST) {
            if (y == MIN_Y) return Blocks.GRASS_BLOCK.defaultBlockState();
            int[][] trees = {{-7, 5}, {7, 5}, {-7, 16}, {7, 16}};
            for (int[] tree : trees) {
                int dx = Math.abs(x - tree[0]), dz = Math.abs(z - tree[1]);
                if (dx == 0 && dz == 0 && y >= 79 && y <= 83)
                    return Blocks.OAK_LOG.defaultBlockState();
                if (y >= 82 && y <= 85 && dx <= 2 && dz <= 2 && dx + dz <= 3)
                    return Blocks.OAK_LEAVES.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }

        // An enclosed, shallow water scene with a raised moss island and stone stepping path.
        if (y == MIN_Y) return Blocks.STONE.defaultBlockState();
        int dx = x, dz = z - 10;
        boolean island = dx * dx + dz * dz <= 16;
        if (y == 79) {
            if (island) return Blocks.STONE.defaultBlockState();
            if (x == 0 && z <= 7 && z % 2 == 0) return Blocks.STONE.defaultBlockState();
            return Blocks.WATER.defaultBlockState();
        }
        if (y == 80 && island) return Blocks.MOSS_BLOCK.defaultBlockState();
        if (y == 81 && island && (dx * dx + dz * dz == 9)) return Blocks.MOSS_CARPET.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = level(source);
        if (level == null) return 0;
        // A status request does not create terrain or overwrite a missing reservation.
        if (!isOwned(level)) {
            source.sendSuccess(() -> Component.literal("Прототип не создан; /tearsofink pantheon prototype init."), false);
            return 1;
        }
        Job active = job;
        String text;
        if (active != null) {
            text = "PREPARING " + active.target + " | " + (100 * active.cursor / VOLUME)
                    + "% | записано " + active.writes + " блоков";
        } else if (!level.getBlockState(GATE_PROBE).isAir()) {
            text = "CLOSED / RECOVERY REQUIRED — повторно запустите forest или island";
        } else {
            Block block = level.getBlockState(new BlockPos(0, MIN_Y, 10)).getBlock();
            text = "OPEN | " + (block == Blocks.GRASS_BLOCK ? "FOREST" : block == Blocks.STONE ? "ISLAND" : "UNKNOWN");
        }
        source.sendSuccess(() -> Component.literal("Pantheon prototype: " + text
                + " | защищённый объём " + WIDTH + "×" + DEPTH + "×" + (MAX_Y - MIN_Y + 1)
                + " | отдельное измерение, без боссов"), false);
        return 1;
    }
}
