package com.tarkrakov.tearsofink.world;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.zip.CRC32;

/** World-owned frozen plan; written before publishing it to generation threads. */
final class TemplePlanStore {
    private static final int MAGIC = 0x544F4950, FORMAT = 1;
    private static final int MAX_BYTES = 512 * 1024, MAX_PATHS = 4096;

    private TemplePlanStore() {}

    /** Returns null only for a genuinely absent file. Never replaces a damaged file. */
    static TempleNetwork restoreIfPresent(Path file, InkLandsLayout layout, TempleNetwork.Inputs inputs) {
        try {
            if (Files.notExists(file)) return null;
            long start = System.nanoTime();
            TempleNetwork plan = read(file, layout, inputs);
            TearsOfInk.LOGGER.info("D-060: restored {} temple sites from {} in {} ms",
                    plan.temples().size(), file, (System.nanoTime() - start) / 1_000_000);
            return plan;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Cannot safely restore temple network " + file, e);
        }
    }

    static TempleNetwork loadOrCreate(Path file, InkLandsLayout layout, TempleNetwork.Inputs inputs) {
        TempleNetwork restored = restoreIfPresent(file, layout, inputs);
        if (restored != null) return restored;
        long start = System.nanoTime();
        try {
            TempleNetwork plan = TempleNetwork.plan(layout, inputs);
            long computed = System.nanoTime();
            write(file, layout, inputs, plan);
            TearsOfInk.LOGGER.info("D-060: planned {} temple sites in {} ms; validated and saved in {} ms",
                    plan.temples().size(), (computed - start) / 1_000_000,
                    (System.nanoTime() - computed) / 1_000_000);
            if (plan.temples().size() != 13)
                TearsOfInk.LOGGER.warn("D-060 experimental layout is incomplete: {}/13; not accepted for default use",
                        plan.temples().size());
            return plan;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Cannot safely create temple network " + file, e);
        }
    }

    private static TempleNetwork read(Path file, InkLandsLayout layout, TempleNetwork.Inputs inputs) throws IOException {
        long size = Files.size(file);
        if (size < 40 || size > MAX_BYTES) throw new IOException("Invalid temple plan size");
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length != size) throw new IOException("Temple plan changed while reading");
        int payload = bytes.length - Long.BYTES;
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, payload);
        if (crc.getValue() != ByteBuffer.wrap(bytes, payload, Long.BYTES).getLong())
            throw new IOException("Temple plan checksum mismatch");
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes, 0, payload))) {
            if (in.readInt() != MAGIC || in.readInt() != FORMAT
                    || in.readInt() != layout.templeLayoutVersion()
                    || in.readLong() != inputs.seed() || in.readLong() != layout.seed())
                throw new IOException("Wrong temple plan world or format");
            int n = in.readInt();
            if (n < 1 || n > 13) throw new IOException("Invalid temple count");
            List<TempleNetwork.Temple> temples = new ArrayList<>(n);
            var chunks = new HashSet<Long>();
            int starterCount = 0;
            var start = Ruins.start(layout);
            for (int i = 0; i < n; i++) {
                int x = in.readInt(), y = in.readInt(), z = in.readInt(), road = in.readInt();
                double along = in.readDouble();
                Rotation rotation = Rotation.valueOf(in.readUTF());
                boolean starter = in.readBoolean();
                if (Math.abs((long)x) > 3000 || Math.abs((long)z) > 3000
                        || road < -1 || road >= InkLandsLayout.ROADS || !Double.isFinite(along)
                        || along < 0 || along > 3000
                        || (!starter && (y < inputs.terrain().level().getMinBuildHeight()
                        || y >= inputs.terrain().level().getMaxBuildHeight())))
                    throw new IOException("Invalid temple coordinates");
                if (starter) {
                    starterCount++;
                    if (x != (int)start[0] || z != (int)start[1] || road != Ruins.startRoad(layout))
                        throw new IOException("Wrong starting temple");
                }
                var temple = new TempleNetwork.Temple(x, y, z, road, along, rotation, starter);
                if (!chunks.add(temple.chunk())) throw new IOException("Duplicate temple start");
                temples.add(temple);
            }
            if (starterCount != 1) throw new IOException("Missing or duplicate starting temple");
            int m = in.readInt();
            if (m < 0 || m > MAX_PATHS) throw new IOException("Invalid path count");
            List<TempleNetwork.PathBlock> paths = new ArrayList<>(m);
            for (int i = 0; i < m; i++) {
                BlockPos floor = BlockPos.of(in.readLong());
                String direction = in.readUTF();
                Direction up = direction.isEmpty() ? null : Direction.valueOf(direction);
                boolean lantern = in.readBoolean();
                if (Math.abs((long)floor.getX()) > 3000 || Math.abs((long)floor.getZ()) > 3000
                        || floor.getY() < inputs.terrain().level().getMinBuildHeight()
                        || floor.getY() + 3 >= inputs.terrain().level().getMaxBuildHeight()
                        || (up != null && up.getAxis() == Direction.Axis.Y)
                        || layout.roadNear(floor.getX(), floor.getZ(),
                        TempleNetwork.ROAD_BUFFER - InkLandsLayout.ROAD_HALF_WIDTH) != null)
                    throw new IOException("Invalid approach column");
                paths.add(new TempleNetwork.PathBlock(floor, up, lantern));
            }
            if (in.read() != -1) throw new IOException("Trailing temple plan data");
            return new TempleNetwork(temples, paths);
        }
    }

    private static void write(Path file, InkLandsLayout layout, TempleNetwork.Inputs inputs,
                              TempleNetwork plan) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeInt(FORMAT);
            out.writeInt(layout.templeLayoutVersion());
            out.writeLong(inputs.seed());
            out.writeLong(layout.seed());
            out.writeInt(plan.temples().size());
            for (var temple : plan.temples()) {
                out.writeInt(temple.x()); out.writeInt(temple.y()); out.writeInt(temple.z());
                out.writeInt(temple.road()); out.writeDouble(temple.along());
                out.writeUTF(temple.rotation().name()); out.writeBoolean(temple.starter());
            }
            var paths = plan.allPathBlocks();
            if (paths.size() > MAX_PATHS) throw new IOException("Too many temple approach columns");
            out.writeInt(paths.size());
            for (var path : paths) {
                out.writeLong(path.floor().asLong());
                out.writeUTF(path.up() == null ? "" : path.up().name());
                out.writeBoolean(path.lantern());
            }
            out.flush();
            CRC32 crc = new CRC32();
            crc.update(bytes.toByteArray());
            out.writeLong(crc.getValue());
        }
        Files.createDirectories(file.getParent());
        Path tmp = Files.createTempFile(file.getParent(), "temple-plan-", ".tmp");
        try {
            Files.write(tmp, bytes.toByteArray());
            try (var channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) { channel.force(true); }
            // Validate the actual serialized bytes with the same checks as a restart,
            // before any plan is published or an authoritative file is installed.
            read(tmp, layout, inputs);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
