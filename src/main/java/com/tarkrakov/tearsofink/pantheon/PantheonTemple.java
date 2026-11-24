package com.tarkrakov.tearsofink.pantheon;

import com.mojang.brigadier.context.CommandContext;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Operator-only installation of the author's entire Pantheon shell from Axiom.
 * A separate void dimension and a full-volume preflight protect existing saves.
 * This is a shell importer, not a scene replacement or a public boss encounter.
 */
public final class PantheonTemple {
    public static final ResourceKey<Level> DIMENSION =
            ResourceKey.create(Registries.DIMENSION, TearsOfInk.id("pantheon_temple"));

    private static final int WIDTH = 115, HEIGHT = 22, DEPTH = 144;
    private static final int VOLUME = WIDTH * HEIGHT * DEPTH;
    private static final int CHECKS_PER_TICK = 8192;
    private static final int PLACEMENTS_PER_TICK = 512;
    // Template X=0..114, Z=0..143. Local Y=0 is world Y=80.
    private static final BlockPos ORIGIN = new BlockPos(-57, 80, 0);
    private static final BlockPos CLAIM = new BlockPos(0, 78, -16);
    private static final BlockPos FINISHED = CLAIM.above();
    private static final BlockPos ENTRANCE = ORIGIN.offset(58, 1, 12);
    private static final String RESOURCE = "/data/tears-of-ink/pantheon/toi_panteon.schem.b64";

    private static final Map<UUID, ReturnSpot> RETURNS = new HashMap<>();
    private static Job job;

    private record ReturnSpot(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {}
    private record Placement(BlockPos relative, BlockState state) {}
    private record Tile(BlockPos relative, CompoundTag nbt) {}

    private record Template(BlockState[] states, List<Placement> placements, List<Tile> tiles) {}

    private static final class Job {
        final Template template;
        final boolean resume;
        int scan;
        int placed;
        boolean building;

        Job(Template template, boolean resume) {
            this.template = template;
            this.resume = resume;
        }
    }

    private PantheonTemple() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(Commands.literal("tearsofink")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("pantheon")
                                .then(Commands.literal("temple")
                                        .then(Commands.literal("install").executes(ctx -> install(ctx, false)))
                                        .then(Commands.literal("resume").executes(ctx -> install(ctx, true)))
                                        .then(Commands.literal("enter").executes(PantheonTemple::enter))
                                        .then(Commands.literal("leave").executes(PantheonTemple::leave))
                                        .then(Commands.literal("status").executes(PantheonTemple::status))))));
        ServerTickEvents.END_SERVER_TICK.register(PantheonTemple::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            job = null;
            RETURNS.clear();
        });
    }

    private static ServerLevel level(CommandSourceStack source) {
        ServerLevel temple = source.getServer().getLevel(DIMENSION);
        if (temple == null) source.sendFailure(Component.literal("Измерение пантеона не загружено."));
        return temple;
    }

    private static int install(CommandContext<CommandSourceStack> ctx, boolean resume) {
        var source = ctx.getSource();
        ServerLevel level = level(source);
        if (level == null) return 0;
        if (job != null) {
            source.sendFailure(Component.literal("Установка уже выполняется; /tearsofink pantheon temple status."));
            return 0;
        }
        boolean claimed = level.getBlockState(CLAIM).is(Blocks.LODESTONE);
        boolean finished = level.getBlockState(FINISHED).is(Blocks.EMERALD_BLOCK);
        if (finished || (claimed != resume)) {
            source.sendFailure(Component.literal(finished ? "Пантеон уже установлен." :
                    claimed ? "Незавершённая установка: используйте temple resume." :
                            "Ещё нет незавершённой установки: используйте temple install."));
            return 0;
        }
        try {
            job = new Job(load(), resume);
        } catch (Exception ex) {
            source.sendFailure(Component.literal("Не удалось прочитать авторскую схему: " + ex.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(resume
                ? "Проверяю незавершённый пантеон перед безопасным продолжением."
                : "Проверяю пустое место перед установкой авторского пантеона."), false);
        return 1;
    }

    private static Template load() throws Exception {
        byte[] encoded;
        try (InputStream resource = PantheonTemple.class.getResourceAsStream(RESOURCE)) {
            if (resource == null) throw new IllegalStateException("Нет встроенной схемы " + RESOURCE);
            encoded = resource.readAllBytes();
        }
        byte[] zipped = Base64.getMimeDecoder().decode(new String(encoded, StandardCharsets.US_ASCII));
        CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(zipped), NbtAccounter.unlimitedHeap());
        if (root.getInt("Width") != WIDTH || root.getInt("Height") != HEIGHT
                || root.getInt("Length") != DEPTH || root.getInt("Version") != 2) {
            throw new IllegalStateException("Размер или версия схемы отличаются от проверенного файла");
        }
        int[] offset = root.getIntArray("Offset");
        if (offset.length != 3 || offset[0] != 0 || offset[1] != 0 || offset[2] != 0)
            throw new IllegalStateException("Неожиданное смещение схемы");

        CompoundTag palette = root.getCompound("Palette");
        BlockState[] statesByPalette = new BlockState[root.getInt("PaletteMax")];
        for (String key : palette.getAllKeys()) {
            int n = palette.getInt(key);
            if (n < 0 || n >= statesByPalette.length || statesByPalette[n] != null)
                throw new IllegalStateException("Повреждена палитра");
            statesByPalette[n] = parseState(key);
        }
        byte[] bytes = root.getByteArray("BlockData");
        BlockState[] states = new BlockState[VOLUME];
        List<Placement> placements = new ArrayList<>();
        int cursor = 0;
        for (int index = 0; index < VOLUME; index++) {
            int value = 0, shift = 0, next;
            do {
                if (cursor >= bytes.length || shift > 28)
                    throw new IllegalStateException("Повреждены индексы блоков");
                next = bytes[cursor++] & 255;
                value |= (next & 127) << shift;
                shift += 7;
            } while ((next & 128) != 0);
            if (value >= statesByPalette.length || statesByPalette[value] == null)
                throw new IllegalStateException("Индекс вне палитры");
            BlockState state = statesByPalette[value];
            states[index] = state;
            if (!state.isAir()) placements.add(new Placement(relative(index), state));
        }
        if (cursor != bytes.length) throw new IllegalStateException("Лишние данные блоков");
        List<Tile> tiles = new ArrayList<>();
        ListTag blockEntities = root.getList("BlockEntities", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag entry = blockEntities.getCompound(i);
            int[] pos = entry.getIntArray("Pos");
            if (pos.length != 3 || pos[0] < 0 || pos[0] >= WIDTH
                    || pos[1] < 0 || pos[1] >= HEIGHT || pos[2] < 0 || pos[2] >= DEPTH)
                throw new IllegalStateException("Block entity вне схемы");
            tiles.add(new Tile(new BlockPos(pos[0], pos[1], pos[2]), entry.copy()));
        }
        return new Template(states, placements, tiles);
    }

    private static BlockState parseState(String descriptor) {
        int bracket = descriptor.indexOf('[');
        String id = bracket < 0 ? descriptor : descriptor.substring(0, bracket);
        ResourceLocation key = ResourceLocation.parse(id);
        Block block = BuiltInRegistries.BLOCK.get(key);
        if (block == Blocks.AIR && !id.equals("minecraft:air"))
            throw new IllegalStateException("Неизвестный блок: " + id);
        BlockState state = block.defaultBlockState();
        if (bracket < 0) return state;
        if (!descriptor.endsWith("]")) throw new IllegalStateException("Некорректный блок: " + descriptor);
        for (String part : descriptor.substring(bracket + 1, descriptor.length() - 1).split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) throw new IllegalStateException("Некорректное состояние: " + descriptor);
            Property<?> property = block.getStateDefinition().getProperty(pair[0]);
            if (property == null) throw new IllegalStateException("Неизвестное свойство: " + descriptor);
            state = stateWithProperty(state, property, pair[1]);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState stateWithProperty(BlockState state, Property<T> property, String value) {
        T parsed = property.getValue(value).orElseThrow(() ->
                new IllegalStateException("Неизвестное значение " + property.getName() + "=" + value));
        return state.setValue(property, parsed);
    }

    private static BlockPos relative(int index) {
        int x = index % WIDTH;
        int z = (index / WIDTH) % DEPTH;
        int y = index / (WIDTH * DEPTH);
        return new BlockPos(x, y, z);
    }

    private static BlockPos world(BlockPos relative) {
        return ORIGIN.offset(relative);
    }

    private static void tick(MinecraftServer server) {
        Job current = job;
        if (current == null) return;
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null) {
            job = null;
            return;
        }
        if (!current.building) {
            int checked = 0;
            while (checked++ < CHECKS_PER_TICK && current.scan < VOLUME) {
                int index = current.scan++;
                BlockState existing = level.getBlockState(world(relative(index)));
                BlockState expected = current.template.states()[index];
                boolean allowed = current.resume
                        ? existing.isAir() || existing.equals(expected)
                        : existing.isAir();
                if (!allowed) {
                    job = null; // Never replace a foreign or modified block.
                    server.getPlayerList().getPlayers().stream().filter(p -> p.hasPermissions(2))
                            .forEach(p -> p.sendSystemMessage(Component.literal(
                                    "Пантеон: отказ, занято место " + world(relative(index)).toShortString()
                                    + ". Чужие блоки не изменены.")));
                    return;
                }
            }
            if (current.scan < VOLUME) return;
            if (!current.resume) level.setBlock(CLAIM, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            current.building = true;
        }
        int writes = 0;
        while (writes < PLACEMENTS_PER_TICK && current.placed < current.template.placements().size()) {
            Placement placement = current.template.placements().get(current.placed++);
            BlockPos target = world(placement.relative());
            BlockState before = level.getBlockState(target);
            if (before.equals(placement.state())) continue;
            if (!before.isAir()) {
                job = null;
                return; // Someone edited the construction during the build. Never overwrite.
            }
            level.setBlock(target, placement.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            writes++;
        }
        if (current.placed < current.template.placements().size()) return;
        for (Tile tile : current.template.tiles()) {
            BlockPos pos = world(tile.relative());
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) continue;
            CompoundTag data = tile.nbt().copy();
            data.putString("id", data.getString("Id"));
            data.remove("Id");
            data.remove("Pos");
            data.putInt("x", pos.getX());
            data.putInt("y", pos.getY());
            data.putInt("z", pos.getZ());
            entity.loadWithComponents(data, level.registryAccess());
            entity.setChanged();
        }
        level.setBlock(FINISHED, Blocks.EMERALD_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        job = null;
        server.getPlayerList().getPlayers().stream().filter(p -> p.hasPermissions(2))
                .forEach(p -> p.sendSystemMessage(Component.literal(
                        "Авторский пантеон установлен. /tearsofink pantheon temple enter")));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = level(ctx.getSource());
        if (level == null) return 0;
        String status = job != null
                ? job.building ? "установка блоков " + job.placed + "/" + job.template.placements().size()
                               : "проверка пространства " + job.scan + "/" + VOLUME
                : level.getBlockState(FINISHED).is(Blocks.EMERALD_BLOCK) ? "храм установлен"
                : level.getBlockState(CLAIM).is(Blocks.LODESTONE) ? "установка прервана — temple resume"
                : "не установлен — temple install";
        ctx.getSource().sendSuccess(() -> Component.literal("Pantheon temple: " + status), false);
        return 1;
    }

    private static int enter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = level(source);
        if (level == null || !level.getBlockState(FINISHED).is(Blocks.EMERALD_BLOCK)) {
            source.sendFailure(Component.literal("Сначала завершите установку храма."));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        if (player.serverLevel().dimension() != DIMENSION)
            RETURNS.put(player.getUUID(), new ReturnSpot(player.serverLevel().dimension(),
                    player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        player.teleportTo(level, ENTRANCE.getX() + 0.5, ENTRANCE.getY(), ENTRANCE.getZ() + 0.5, 180f, 0f);
        source.sendSuccess(() -> Component.literal("Вы вошли в авторский храм. Выход: /tearsofink pantheon temple leave"), false);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        if (player.serverLevel().dimension() != DIMENSION) {
            source.sendFailure(Component.literal("Вы не в измерении храма."));
            return 0;
        }
        ReturnSpot previous = RETURNS.remove(player.getUUID());
        ServerLevel destination = previous == null ? null : source.getServer().getLevel(previous.dimension());
        if (destination != null) {
            player.teleportTo(destination, previous.x(), previous.y(), previous.z(), previous.yaw(), previous.pitch());
        } else {
            ServerLevel overworld = source.getServer().overworld();
            BlockPos fallback = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, fallback.getX() + 0.5, fallback.getY() + 1,
                    fallback.getZ() + 0.5, 0f, 0f);
        }
        source.sendSuccess(() -> Component.literal("Вы покинули храм."), false);
        return 1;
    }
}
