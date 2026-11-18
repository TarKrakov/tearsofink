package com.tarkrakov.tearsofink.sound;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** One positional voice, identified independently of the entity tracking range. */
public record StriderSong(UUID episode, double x, double y, double z, int part, float volume)
        implements CustomPacketPayload {
    public static final double DELIVERY_RANGE = 112;
    public static final Type<StriderSong> TYPE = new Type<>(TearsOfInk.id("strider_song_voice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StriderSong> CODEC = StreamCodec.of(
            (buf, song) -> {
                buf.writeUUID(song.episode);
                buf.writeDouble(song.x);
                buf.writeDouble(song.y);
                buf.writeDouble(song.z);
                buf.writeVarInt(song.part);
                buf.writeFloat(song.volume);
            },
            buf -> new StriderSong(buf.readUUID(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readVarInt(), buf.readFloat()));

    public static void init() {
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    }

    @Override public Type<StriderSong> type() { return TYPE; }
}
