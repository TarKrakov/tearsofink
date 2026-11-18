package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.sound.StriderRepertoire;
import com.tarkrakov.tearsofink.sound.StriderSong;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class StriderSongClient {
    private static final Map<UUID, SoundInstance> playing = new HashMap<>();
    private static ClientLevel world;

    static void init() {
        ClientPlayNetworking.registerGlobalReceiver(StriderSong.TYPE,
                (song, context) -> context.client().execute(() -> receive(context.client(), song)));
        ClientTickEvents.END_CLIENT_TICK.register(StriderSongClient::update);
    }

    private static void update(Minecraft client) {
        var engine = client.getSoundManager();
        if (world != client.level) {
            playing.values().forEach(engine::stop);
            playing.clear();
            world = client.level;
        } else {
            playing.values().removeIf(voice -> !engine.isActive(voice));
        }
    }

    private static void receive(Minecraft client, StriderSong song) {
        update(client);
        var engine = client.getSoundManager();
        if (song.voiceIndex() < 0) {
            SoundInstance voice = playing.remove(song.episode());
            if (voice != null) engine.stop(voice);
            return;
        }
        var sound = StriderRepertoire.sound(song.voiceIndex());
        if (world == null || sound == null || playing.containsKey(song.episode()) || playing.size() >= 4) return;
        var voice = new SimpleSoundInstance(sound, SoundSource.NEUTRAL, song.volume(), 1,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR,
                song.x(), song.y(), song.z(), false);
        engine.play(voice);
        playing.put(song.episode(), voice);
    }
}
