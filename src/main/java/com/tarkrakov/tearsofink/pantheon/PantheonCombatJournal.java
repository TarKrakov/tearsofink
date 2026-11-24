package com.tarkrakov.tearsofink.pantheon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Records session ownership independently of the existing scene journal. */
final class PantheonCombatJournal {
    private PantheonCombatJournal() {}

    static CompoundTag read(MinecraftServer server) throws IOException {
        return NbtIo.read(path(server));
    }

    static void save(MinecraftServer server, CompoundTag data) throws IOException {
        Path file = path(server);
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "pantheon-combat-", ".tmp");
        try {
            NbtIo.write(data, temp);
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    static void clear(MinecraftServer server) throws IOException {
        Files.deleteIfExists(path(server));
    }

    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/tearsofink_pantheon_combat_v1.dat");
    }
}
