package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent, bounded authoring session. Does not replace the live altar, hub or original schematic. */
final class PantheonEditor {
    private static final int X0 = 17, X1 = 99, Y0 = 1, Y1 = 21, Z0 = 45, Z1 = 127;
    private static final int W = X1 - X0 + 1, D = Z1 - Z0 + 1, H = Y1 - Y0 + 1;
    private static final int SIZE = W * D * H;
    private static final AABB AREA = new AABB(
            Vec3.atLowerCornerOf(PantheonTemple.world(new BlockPos(X0, Y0, Z0))),
            Vec3.atLowerCornerOf(PantheonTemple.world(new BlockPos(X1 + 1, Y1 + 1, Z1 + 1))));
    private static final String FILE = "tearsofink_pantheon_edit_v1.dat";
    private static BlockState[] baseline, backup;
    private static UUID owner;
    private static String problem;
    private static int rollback = -1;

    private PantheonEditor() {}

    static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(Commands.literal("tearsofink").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("pantheon").then(Commands.literal("edit")
                                .then(Commands.literal("start").executes(ctx -> start(ctx.getSource())))
                                .then(Commands.literal("save").executes(ctx -> save(ctx.getSource())))
                                .then(Commands.literal("cancel").executes(ctx -> cancel(ctx.getSource())))
                                .then(Commands.literal("export").executes(ctx -> export(ctx.getSource())))
                                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))))));
        ServerLifecycleEvents.SERVER_STARTED.register(PantheonEditor::load);
        ServerTickEvents.END_SERVER_TICK.register(PantheonEditor::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            baseline = null;
            backup = null;
            owner = null;
            problem = null;
            rollback = -1;
        });
    }

    static boolean active() { return backup != null || problem != null; }
    static boolean hasBaseline() { return baseline != null; }
    static boolean isOwner(ServerPlayer player) { return owner != null && owner.equals(player.getUUID()); }

    private static BlockPos local(int index) {
        return new BlockPos(X0 + index % W, Y0 + index / (W * D), Z0 + index / W % D);
    }
    private static boolean inside(BlockPos world) {
        BlockPos p = PantheonTemple.local(world);
        return p.getX() >= X0 && p.getX() <= X1 && p.getY() >= Y0 && p.getY() <= Y1
                && p.getZ() >= Z0 && p.getZ() <= Z1;
    }
    private static boolean editingPlayer(ServerLevel level, Player player) {
        return backup != null && problem == null && rollback < 0
                && player instanceof ServerPlayer sp && isOwner(sp) && sp.hasPermissions(2)
                && sp.isCreative() && level.players().size() == 1 && level.players().contains(sp);
    }
    static boolean mayBreak(Level level, Player player, BlockPos pos) {
        return level instanceof ServerLevel server && PantheonTemple.isTemple(server)
                && inside(pos) && editingPlayer(server, player) && !server.getBlockState(pos).hasBlockEntity();
    }
    static boolean mayWrite(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server) || !PantheonTemple.isTemple(server)
                || !inside(pos) || server.players().size() != 1
                || !editingPlayer(server, server.players().getFirst())) return false;
        // Functional blocks/containers stay intact. No fluids in this first authoring tool.
        return !server.getBlockState(pos).hasBlockEntity() && !state.hasBlockEntity()
                && state.getFluidState().isEmpty();
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE);
    }
    private static void load(MinecraftServer server) {
        try {
            Path path = file(server);
            if (Files.notExists(path)) return;
            CompoundTag root = NbtIo.read(path);
            if (root == null || root.getInt("Version") != 1) throw new IOException("Invalid edit journal");
            if (root.contains("Baseline")) baseline = decode(root.getCompound("Baseline"));
            if (root.contains("Backup")) {
                backup = decode(root.getCompound("Backup"));
                owner = root.getUUID("Owner");
                TearsOfInk.LOGGER.info("Pantheon authoring session restored; use edit save or edit cancel");
            }
        } catch (Exception ex) {
            problem = ex.toString();
            TearsOfInk.LOGGER.error("Pantheon edit journal cannot be read; scene changes disabled", ex);
        }
    }

    private static void persist(MinecraftServer server, BlockState[] saved, BlockState[] pending, UUID author) throws IOException {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 1);
        if (saved != null) root.put("Baseline", encode(saved));
        if (pending != null) {
            root.put("Backup", encode(pending));
            root.putUUID("Owner", author);
        }
        Path path = file(server);
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), "pantheon-edit-", ".tmp");
        try {
            NbtIo.write(root, temp);
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }

    private static String descriptor(BlockState state) {
        CompoundTag tag = NbtUtils.writeBlockState(state);
        StringBuilder key = new StringBuilder(tag.getString("Name"));
        CompoundTag props = tag.getCompound("Properties");
        if (!props.isEmpty()) {
            key.append('[');
            boolean first = true;
            for (String name : props.getAllKeys().stream().sorted().toList()) {
                if (!first) key.append(',');
                key.append(name).append('=').append(props.getString(name));
                first = false;
            }
            key.append(']');
        }
        return key.toString();
    }

    private static CompoundTag encode(BlockState[] states) {
        Map<BlockState, Integer> palette = new LinkedHashMap<>();
        int[] indices = new int[states.length];
        for (int i = 0; i < states.length; i++)
            indices[i] = palette.computeIfAbsent(states[i], ignored -> palette.size());
        CompoundTag tag = new CompoundTag(), names = new CompoundTag();
        palette.forEach((state, id) -> names.putString(Integer.toString(id), descriptor(state)));
        tag.put("Palette", names);
        tag.putIntArray("Blocks", indices);
        return tag;
    }
    private static BlockState[] decode(CompoundTag tag) throws IOException {
        int[] indices = tag.getIntArray("Blocks");
        if (indices.length != SIZE) throw new IOException("Incorrect arena baseline dimensions");
        CompoundTag names = tag.getCompound("Palette");
        BlockState[] palette = new BlockState[names.size()];
        for (int i = 0; i < palette.length; i++) palette[i] = PantheonTemple.parseState(names.getString(Integer.toString(i)));
        BlockState[] states = new BlockState[SIZE];
        for (int i = 0; i < SIZE; i++) {
            int n = indices[i];
            if (n < 0 || n >= palette.length) throw new IOException("Invalid arena palette index");
            states[i] = palette[n];
        }
        return states;
    }
    private static BlockState[] snapshot(ServerLevel level) {
        // Chunks already belong to the installed temple; synchronous loading only on explicit author command.
        for (int x = -3; x <= 2; x++) for (int z = 2; z <= 7; z++) level.getChunk(x, z);
        BlockState[] states = new BlockState[SIZE];
        for (int i = 0; i < SIZE; i++) states[i] = level.getBlockState(PantheonTemple.world(local(i)));
        return states;
    }
    static PantheonTemple.Template baselineTemplate() throws Exception { return template(baseline); }
    private static PantheonTemple.Template template(BlockState[] states) throws Exception {
        var original = PantheonTemple.load();
        if (states == null) return original;
        BlockState[] all = original.states().clone();
        for (int i = 0; i < SIZE; i++) {
            BlockPos p = local(i);
            all[p.getX() + 115 * (p.getZ() + 144 * p.getY())] = states[i];
        }
        return PantheonTemple.withStates(original, all);
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
    private static int success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
    private static boolean clearArea(ServerLevel level) {
        return level.getEntitiesOfClass(Entity.class, AREA, entity -> !(entity instanceof ServerPlayer)).isEmpty();
    }
    private static ServerLevel temple(CommandSourceStack source) { return source.getServer().getLevel(PantheonTemple.DIMENSION); }
    private static boolean authorize(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer sp && isOwner(sp) && sp.hasPermissions(2);
    }

    private static int start(CommandSourceStack source) {
        ServerLevel level = temple(source);
        if (active()) return fail(source, "Редактор уже открыт или повреждён журнал. edit status.");
        if (level == null || !(source.getEntity() instanceof ServerPlayer sp) || sp.level() != level || !sp.isCreative())
            return fail(source, "Войдите в Пантеон в творческом режиме.");
        if (!PantheonTemple.ready(level) || PantheonCombat.sceneOwned() || !PantheonArena.neutralReady()
                || level.players().size() != 1)
            return fail(source, "Нужен готовый нейтральный Пантеон без испытания и других игроков.");
        try {
            BlockState[] before = snapshot(level);
            if (!clearArea(level)) return fail(source, "Уберите посторонние сущности из арены.");
            persist(source.getServer(), baseline, before, sp.getUUID());
            backup = before;
            owner = sp.getUUID();
            return success(source, "Редактор открыт. X -40..42, Y 81..101, Z 45..127. Ритуалы заблокированы. edit save / cancel.");
        } catch (Exception ex) { return fail(source, "Не удалось создать резервную копию: " + ex.getMessage()); }
    }

    private static int save(CommandSourceStack source) {
        ServerLevel level = temple(source);
        if (problem != null || backup == null || rollback >= 0 || level == null || !authorize(source))
            return fail(source, "Нет доступной вам сессии редактирования.");
        if (level.players().size() != 1 || !editingPlayer(level, (ServerPlayer) source.getEntity()))
            return fail(source, "Сохранение доступно автору в творческом режиме, одному в Пантеоне.");
        try {
            BlockState[] next = snapshot(level);
            if (!clearArea(level)) return fail(source, "Уберите посторонние сущности из арены.");
            for (int i = 0; i < SIZE; i++) {
                if (!next[i].equals(backup[i]) && (next[i].hasBlockEntity() || backup[i].hasBlockEntity()
                        || !next[i].getFluidState().isEmpty()))
                    return fail(source, "Недопустимое изменение на " + PantheonTemple.world(local(i)).toShortString()
                            + ": блок-сущность или жидкость.");
            }
            PantheonArena.validateEditedTemplate(template(next));
            // Durable chunks first; the journal still owns the backup if interrupted here.
            level.getChunkSource().save(true);
            persist(source.getServer(), next, null, null);
            baseline = next;
            backup = null;
            owner = null;
            PantheonArena.invalidateLayout();
            return success(source, "Нейтральная арена сохранена. Защита восстановлена, ритуалы доступны.");
        } catch (Exception ex) { return fail(source, "Изменения не приняты, редактор остаётся открыт: " + ex.getMessage()); }
    }

    private static int cancel(CommandSourceStack source) {
        ServerLevel level = temple(source);
        if (problem != null || backup == null || rollback >= 0 || level == null || !authorize(source))
            return fail(source, "Нет доступной вам сессии редактирования.");
        try {
            BlockState[] actual = snapshot(level);
            for (int i = 0; i < SIZE; i++)
                if (!actual[i].equals(backup[i]) && (actual[i].hasBlockEntity() || backup[i].hasBlockEntity()))
                    return fail(source, "Сначала восстановите защищённый блок-сущность на "
                            + PantheonTemple.world(local(i)).toShortString());
            for (ServerPlayer sp : level.players()) if (sp.getZ() >= 39 && sp.isAlive()) PantheonArena.sendToHub(sp);
            rollback = 0;
            return success(source, "Начат откат постройки. Ритуалы закрыты до окончания.");
        } catch (Exception ex) { return fail(source, "Не удалось начать откат: " + ex.getMessage()); }
    }

    private static void tick(MinecraftServer server) {
        if (rollback < 0 || backup == null) return;
        ServerLevel level = server.getLevel(PantheonTemple.DIMENSION);
        if (level == null) return;
        for (ServerPlayer sp : level.players()) if (sp.getZ() >= 39 && sp.isAlive()) PantheonArena.sendToHub(sp);
        int reads = 0, writes = 0;
        long start = System.nanoTime();
        try {
            while (rollback < SIZE && reads++ < 4096 && writes < 512 && System.nanoTime() - start < 3_000_000) {
                BlockPos pos = PantheonTemple.world(local(rollback));
                BlockState expected = backup[rollback], current = level.getBlockState(pos);
                if (!current.equals(expected)) {
                    if (current.hasBlockEntity() || expected.hasBlockEntity())
                        throw new IOException("Protected block entity changed: " + pos.toShortString());
                    if (!PantheonTemple.setTempleBlock(level, pos, expected, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE))
                        throw new IOException("Cannot restore " + pos.toShortString());
                    writes++;
                }
                rollback++;
            }
            if (rollback == SIZE) {
                level.getChunkSource().save(true);
                persist(server, baseline, null, null);
                backup = null;
                owner = null;
                rollback = -1;
                PantheonArena.invalidateLayout();
                for (ServerPlayer sp : level.players())
                    sp.sendSystemMessage(Component.literal("Редактирование отменено. Нейтральная арена восстановлена."));
            }
        } catch (Exception ex) {
            rollback = -1;
            TearsOfInk.LOGGER.error("Pantheon edit rollback interrupted; backup kept", ex);
            for (ServerPlayer sp : level.players())
                sp.sendSystemMessage(Component.literal("Откат прерван, копия сохранена: " + ex.getMessage()));
        }
    }

    private static int export(CommandSourceStack source) {
        if (active() || baseline == null) return fail(source, "Сначала сохраните изменённую нейтральную арену.");
        try {
            // Merge only edited region into the original schematic; keep original station NBT and marker data.
            CompoundTag root = PantheonTemple.readSchematic();
            BlockState[] states = baselineTemplate().states();
            Map<BlockState, Integer> ids = new LinkedHashMap<>();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            for (BlockState state : states) {
                int id = ids.computeIfAbsent(state, ignored -> ids.size());
                while ((id & ~127) != 0) { bytes.write((id & 127) | 128); id >>>= 7; }
                bytes.write(id);
            }
            CompoundTag palette = new CompoundTag();
            ids.forEach((state, id) -> palette.putInt(descriptor(state), id));
            root.put("Palette", palette);
            root.putInt("PaletteMax", ids.size());
            root.putByteArray("BlockData", bytes.toByteArray());
            Path dir = source.getServer().getWorldPath(LevelResource.ROOT).resolve("pantheon_exports");
            Files.createDirectories(dir);
            Path out = Files.createTempFile(dir, "toi_panteon_edited_", ".schem");
            NbtIo.writeCompressed(root, out);
            return success(source, "Экспорт сохранён: " + out.toAbsolutePath());
        } catch (Exception ex) { return fail(source, "Ошибка экспорта: " + ex.getMessage()); }
    }

    private static int status(CommandSourceStack source) {
        return success(source, problem != null ? "ERROR: " + problem
                : backup != null ? "Редактирование: " + owner + (rollback >= 0 ? ", откат " + rollback + "/" + SIZE : "")
                : baseline != null ? "Редактор закрыт. Пользовательская нейтральная арена." : "Редактор закрыт. Исходная арена.");
    }
}
