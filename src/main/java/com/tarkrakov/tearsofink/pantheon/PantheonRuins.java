package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.travel.FountainTravel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Places a frozen world-owned site during normal chunk generation only. */
public final class PantheonRuins {
    private static final BlockPos PROOF = new BlockPos(60, -1, 12);
    private static final BlockPos KEEPER = PROOF.above(2);
    private static final BlockState SEAL = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    private static final BlockState FOUNDATION = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    private static final BlockState STAMP = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final String NONE = "No safe dry site for the Pantheon ruins or their compact gateway";
    private static volatile Instance active;
    private enum SiteStatus { PLANNING, NO_SITE, ERROR, RESERVED }
    private static SiteStatus siteStatus = SiteStatus.PLANNING;
    private static String status = "Waiting for the temple plan";
    private static boolean attempted;
    private static final Set<Long> SUPPORTED_CHUNKS = new HashSet<>();

    private record Section(List<Map.Entry<BlockPos, BlockState>> blocks,
                           List<Map.Entry<BlockPos, BlockState>> bases, Set<BlockPos> clearance) {}
    private record Instance(ServerLevel level, PantheonRuinSite site, Map<Long, Section> sections) {}

    private PantheonRuins() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(Commands.literal("pantheon").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    int opened = FountainTravel.openedCount(player);
                    Component hint = opened < 2
                            ? Component.translatable("message.tears-of-ink.pantheon.activate_fountains", opened)
                            : navigationHint(player.server);
                    ctx.getSource().sendSuccess(() -> hint, false);
                    return 1;
                })));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            active = null;
            SUPPORTED_CHUNKS.clear();
            attempted = false;
            siteStatus = SiteStatus.PLANNING;
            status = "Waiting for the temple plan";
        });
        ServerWorldEvents.LOAD.register((server, level) -> {
            if (ModDimensions.isLands(level) && Files.exists(planFile(level))) loadSite(level, null);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            if (attempted) {
                repairLoadedSupports();
                return;
            }
            ServerLevel level = ModDimensions.lands(server);
            if (level == null) return;
            InkLandsLayout layout = InkLandsLayout.of(level);
            if (layout == null || (layout.hasTempleNetwork() && layout.templeNetwork() == null)) return;
            loadSite(level, layout);
        });
    }

    private static Path planFile(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve("tearsofink_pantheon_ruins_v1.txt");
    }

    private static void loadSite(ServerLevel level, InkLandsLayout layout) {
        attempted = true;
        try {
            Path file = planFile(level);
            PantheonRuinSite site = Files.exists(file) ? read(file, level) : null;
            if (site == null && layout == null) {
                attempted = false;
                return;
            }
            var template = PantheonTemple.load();
            if (site == null) site = chooseAndSave(file, level, layout, template);
            if (site == null) {
                siteStatus = SiteStatus.NO_SITE;
                status = NONE;
                TearsOfInk.LOGGER.warn("Pantheon ruins: {}", status);
                return;
            }
            var instance = new Instance(level, site, blueprint(site, template, level.getSeed()));
            if (site.profile() == PantheonRuinSite.Profile.EXISTING_GATEWAY) installInEmptyAir(instance);
            active = instance;
            siteStatus = SiteStatus.RESERVED;
            String placement = site.profile() == PantheonRuinSite.Profile.EXISTING_GATEWAY
                    ? "; compact gateway installed" : "; placed as new chunks generate";
            status = "Reserved at " + site.entrance().toShortString() + placement;
            TearsOfInk.LOGGER.info("Pantheon ruins: {}", status);
        } catch (Exception failure) {
            siteStatus = SiteStatus.ERROR;
            status = "Site unavailable: " + failure.getMessage();
            TearsOfInk.LOGGER.error("Pantheon ruins are disabled for this save", failure);
        }
    }

    public static String status() {
        return status;
    }

    public static Component navigationHint(MinecraftServer server) {
        if (ModDimensions.lands(server) == null) {
            return Component.translatable("message.tears-of-ink.pantheon.lands_unavailable");
        }
        return switch (siteStatus) {
            case PLANNING -> Component.translatable("message.tears-of-ink.pantheon.site_planning");
            case NO_SITE -> Component.translatable("message.tears-of-ink.pantheon.site_missing");
            case ERROR -> Component.translatable("message.tears-of-ink.pantheon.site_error");
            case RESERVED -> {
                BlockPos entrance = active.site().entrance();
                yield Component.translatable("message.tears-of-ink.pantheon.site_found",
                        entrance.getX(), entrance.getY(), entrance.getZ());
            }
        };
    }

    public static BlockPos entrance(ServerLevel level) {
        Instance current = active;
        return current != null && current.level() == level ? current.site().entrance() : null;
    }

    static BlockPos keeper(ServerLevel level) {
        Instance current = active;
        if (current == null || current.level() != level) return null;
        BlockPos proof = current.site().world(PROOF);
        var chunk = level.getChunkSource().getChunkNow(proof.getX() >> 4, proof.getZ() >> 4);
        return chunk != null && chunk.getBlockState(proof).equals(STAMP) ? current.site().world(KEEPER) : null;
    }

    static boolean protectedAt(ServerLevel level, BlockPos pos) {
        Instance current = active;
        if (current == null || current.level() != level) return false;
        var local = current.site().local(pos);
        return local.getX() >= 51 && local.getX() <= 64 && local.getY() >= -1 && local.getY() <= 6
                && local.getZ() >= 7 && local.getZ() <= 19;
    }

    public static void finishDecoration(ServerLevel level, ChunkAccess chunk) {
        Instance current = active;
        if (current == null || current.level() != level || !(chunk instanceof ProtoChunk)
                || chunk instanceof ImposterProtoChunk
                || current.site().profile() == PantheonRuinSite.Profile.EXISTING_GATEWAY) return;
        Section section = current.sections().get(chunk.getPos().toLong());
        if (section == null) return;

        // A structure's containers are never replaced even on a conflicting datapack.
        for (var block : section.blocks()) {
            if (chunk.getBlockState(block.getKey()).hasBlockEntity()) {
                TearsOfInk.LOGGER.error("Pantheon ruins: reserved chunk {} contains a foreign block entity", chunk.getPos());
                return;
            }
        }
        for (var block : section.blocks()) chunk.setBlockState(block.getKey(), block.getValue(), false);
        for (var base : section.bases()) {
            for (BlockPos support : supportedColumn(chunk, base.getKey(), section.clearance(), false)) {
                chunk.setBlockState(support, FOUNDATION, false);
            }
        }
    }

    private static void installInEmptyAir(Instance instance) throws IOException {
        ServerLevel level = instance.level();
        BlockPos proof = instance.site().world(PROOF);
        if (level.getBlockState(proof).equals(STAMP)) return;
        for (Section section : instance.sections().values()) {
            for (var block : section.blocks()) {
                BlockState existing = level.getBlockState(block.getKey());
                if (!existing.isAir() && !existing.equals(block.getValue())) {
                    throw new IOException("Compact Pantheon site changed at " + block.getKey().toShortString());
                }
            }
        }
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (var entry : instance.sections().entrySet()) {
            Section section = entry.getValue();
            for (var block : section.blocks()) {
                if (!block.getKey().equals(proof) && !block.getValue().isAir()
                        && level.getBlockState(block.getKey()).isAir()) level.setBlock(block.getKey(), block.getValue(), flags);
            }
            ChunkPos chunk = new ChunkPos(entry.getKey());
            var loaded = level.getChunk(chunk.x, chunk.z);
            for (var base : section.bases()) {
                for (BlockPos support : supportedColumn(loaded, base.getKey(), section.clearance(), true)) {
                    if (level.getBlockState(support).isAir()) level.setBlock(support, FOUNDATION, flags);
                }
            }
        }
        level.getChunkSource().save(true);
        if (!level.setBlock(proof, STAMP, flags)) throw new IOException("Could not mark the compact Pantheon as installed");
    }

    private static void repairLoadedSupports() {
        Instance current = active;
        if (current == null) return;
        int remaining = 4;
        for (var entry : current.sections().entrySet()) {
            long key = entry.getKey();
            if (SUPPORTED_CHUNKS.contains(key)) continue;
            ChunkPos pos = new ChunkPos(key);
            var chunk = current.level().getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) continue;
            Section section = entry.getValue();
            SUPPORTED_CHUNKS.add(key);
            for (var base : section.bases()) {
                if (!chunk.getBlockState(base.getKey()).equals(base.getValue())) continue;
                for (BlockPos support : supportedColumn(chunk, base.getKey(), section.clearance(), true)) {
                    if (!chunk.getBlockState(support).isAir()) continue;
                    chunk.setBlockState(support, FOUNDATION, false);
                    current.level().getChunkSource().blockChanged(support);
                }
            }
            if (--remaining == 0) break;
        }
    }

    private static List<BlockPos> supportedColumn(ChunkAccess chunk, BlockPos base, Set<BlockPos> clearance,
                                                  boolean airOnly) {
        List<BlockPos> fill = new ArrayList<>();
        for (int y = base.getY() - 1; y >= Math.max(base.getY() - 56, chunk.getMinBuildHeight()); y--) {
            BlockPos pos = base.atY(y);
            if (clearance.contains(pos)) return List.of();
            BlockState existing = chunk.getBlockState(pos);
            if (existing.hasBlockEntity()) return List.of();
            if (existing.getFluidState().isEmpty() && !existing.is(BlockTags.LOGS) && !existing.is(BlockTags.LEAVES)
                    && existing.isFaceSturdy(chunk, pos, Direction.UP)) return fill;
            if (!existing.isAir() && (airOnly || !existing.canBeReplaced())) return List.of();
            fill.add(pos);
        }
        return List.of();
    }

    private static Map<Long, Section> blueprint(PantheonRuinSite site, PantheonTemple.Template template, long seed) {
        var blocks = new LinkedHashMap<BlockPos, BlockState>();
        for (var placement : template.placements()) {
            BlockPos pos = placement.relative();
            if (!site.profile().keeps(pos)) continue;
            BlockState state = placement.state();
            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            if (!id.startsWith("sanctum_") && !id.startsWith("pantheon_column_")) continue;
            boolean room = pos.getZ() <= 19 && pos.getY() <= 6;
            long wear = Mth.getSeed(pos.getX() / 5, pos.getY() / 3, pos.getZ() / 5) ^ seed;
            long detail = Mth.getSeed(pos.getX(), pos.getY(), pos.getZ()) ^ seed;
            if (!room && pos.getY() >= 3 && (Math.floorMod(wear, 9) < 2
                    || Math.floorMod(detail, 100) < pos.getY() + 3)) continue;
            if ((state.is(ModBlocks.SANCTUM_TILE) || state.is(ModBlocks.SANCTUM_WALL)
                    || state.is(ModBlocks.SANCTUM_BRICK) || state.is(ModBlocks.SANCTUM_CARVED_TILE))
                    && Math.floorMod(detail, 16) < 3) state = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            put(blocks, site, pos, state);
        }

        // Clear the traversable cell volumes before they encounter trees or surface blocks.
        for (int z = 0; z <= 18; z++) {
            for (int x = 55; x <= 61; x++) {
                if (z < 8 && (x < 57 || x > 59)) continue;
                for (int y = 1; y <= (z < 7 ? 13 : 4); y++) {
                    blocks.putIfAbsent(site.world(new BlockPos(x, y, z)), AIR);
                }
            }
        }
        // The doorway is X57–58. Preserve the author's surrounding walls and close the entire arch.
        for (int y = 1; y <= 4; y++) {
            for (int x = 56; x <= 59; x++) put(blocks, site, new BlockPos(x, y, 19), SEAL);
        }
        for (int z = 0; z <= 7; z++) {
            int y = z < 2 ? -1 : 0;
            for (int x = 57; x <= 58; x++) {
                put(blocks, site, new BlockPos(x, y, z), Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                if (y < 0) put(blocks, site, new BlockPos(x, 0, z), AIR);
            }
        }
        put(blocks, site, PROOF, STAMP);
        put(blocks, site, new BlockPos(58, 5, 12), Blocks.SHROOMLIGHT.defaultBlockState());
        var byChunk = new HashMap<Long, java.util.ArrayList<Map.Entry<BlockPos, BlockState>>>();
        blocks.forEach((pos, state) -> byChunk.computeIfAbsent(new ChunkPos(pos).toLong(),
                key -> new java.util.ArrayList<>()).add(Map.entry(pos, state)));
        var frozen = new HashMap<Long, Section>();
        byChunk.forEach((chunk, cells) -> {
            var lowest = new LinkedHashMap<Long, Map.Entry<BlockPos, BlockState>>();
            var clearance = new HashSet<BlockPos>();
            for (var cell : cells) {
                BlockPos pos = cell.getKey();
                if (cell.getValue().isAir()) {
                    clearance.add(pos);
                    continue;
                }
                // Keep roof overhangs and arches. Ground the masonry's lowest load-bearing courses.
                if (pos.getY() > site.origin().getY() + 4 || pos.equals(site.world(PROOF))) continue;
                long column = BlockPos.asLong(pos.getX(), 0, pos.getZ());
                lowest.merge(column, cell, (a, b) -> a.getKey().getY() <= b.getKey().getY() ? a : b);
            }
            frozen.put(chunk, new Section(List.copyOf(cells), List.copyOf(lowest.values()), Set.copyOf(clearance)));
        });
        return Map.copyOf(frozen);
    }

    private static void put(Map<BlockPos, BlockState> blocks, PantheonRuinSite site, BlockPos pos, BlockState state) {
        blocks.put(site.world(pos), state.rotate(site.rotation()));
    }

    private static PantheonRuinSite chooseAndSave(Path file, ServerLevel level, InkLandsLayout layout,
                                                  PantheonTemple.Template template) throws IOException {
        PantheonRuinSite site = PantheonRuinSite.choose(level, layout, template);
        if (site == null) return null;
        String data = site.origin().asLong() + " " + site.rotation().name() + " " + site.profile().name();
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "pantheon-ruins-", ".tmp");
        try {
            Files.writeString(temp, "2 " + level.getSeed() + " " + data + "\n");
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        return site;
    }

    private static PantheonRuinSite read(Path file, ServerLevel level) throws IOException {
        String[] fields = Files.readString(file).strip().split(" ");
        if (fields.length < 3 || Long.parseLong(fields[1]) != level.getSeed())
            throw new IOException("Unknown Pantheon site format or world seed");
        boolean legacy = fields[0].equals("1");
        if (legacy && fields.length == 3 && fields[2].equals("unavailable")) return null;
        if ((!legacy && !fields[0].equals("2")) || fields.length != (legacy ? 4 : 5))
            throw new IOException("Invalid Pantheon site");
        BlockPos origin = BlockPos.of(Long.parseLong(fields[2]));
        if (Math.abs(origin.getX()) > 3000 || Math.abs(origin.getZ()) > 3000
                || origin.getY() <= level.getMinBuildHeight() + 33 || origin.getY() >= level.getMaxBuildHeight() - 22) {
            throw new IOException("Pantheon site is outside valid world bounds");
        }
        var profile = legacy ? PantheonRuinSite.Profile.FULL : PantheonRuinSite.Profile.valueOf(fields[4]);
        return new PantheonRuinSite(origin, Rotation.valueOf(fields[3]), profile);
    }
}
