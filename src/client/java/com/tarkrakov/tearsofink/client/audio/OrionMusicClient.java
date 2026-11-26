package com.tarkrakov.tearsofink.client.audio;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.pantheon.OrionMusic;
import com.tarkrakov.tearsofink.pantheon.OrionMusic.Stage;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Local stereo boss score. No sound is emitted into the world or sent to spectators. */
public final class OrionMusicClient {
    private static final float MIX_VOLUME = 0.65f;
    private static final int PREPARE_FADE = 24;
    private static final int EMERGENCY_FADE = 4;
    private static final int HEARTBEAT_TIMEOUT = 100;
    private static UUID fight;
    private static Stage stage = Stage.OFF;
    private static int sincePacket, retry;
    private static Track track;

    private OrionMusicClient() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(OrionMusic.Sync.TYPE,
                (payload, context) -> context.client().execute(() -> receive(context.client(), payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset(client));
        ClientTickEvents.END_CLIENT_TICK.register(OrionMusicClient::tick);
    }

    private static boolean inArenaWorld(Minecraft client) {
        return client.level != null && client.player != null && client.player.isAlive()
                && PantheonTemple.DIMENSION.equals(client.level.dimension());
    }

    public static boolean ownsMusic(Minecraft client) {
        return stage != Stage.OFF && inArenaWorld(client);
    }

    private static void receive(Minecraft client, OrionMusic.Sync packet) {
        if (packet.stage() == Stage.OFF || !inArenaWorld(client)) {
            reset(client);
            return;
        }
        sincePacket = 0;
        if (!packet.fight().equals(fight)) {
            stopTrack(client);
            fight = packet.fight();
            stage = Stage.OFF;
        }
        if (packet.stage() == stage) return;
        stage = packet.stage();
        switch (stage) {
            case FIRST -> start(client, false);
            case SECOND -> start(client, true);
            case FADING -> { if (track != null) track.fade(PREPARE_FADE); }
            case AWAKENING -> { if (track != null) track.fade(EMERGENCY_FADE); }
            case OFF -> reset(client);
        }
    }

    private static void tick(Minecraft client) {
        if (stage == Stage.OFF) return;
        if (!inArenaWorld(client)) {
            reset(client);
            return;
        }
        if (client.isPaused()) return;
        if (++sincePacket > HEARTBEAT_TIMEOUT) {
            reset(client);
            return;
        }
        if (retry > 0) retry--;
        if (track != null && track.isStopped()) {
            client.getSoundManager().stop(track);
            track = null;
        }
        // Recover after resource reload, sound device change or turning Music back on.
        // Never restart phase I while it is fading or during the awakening.
        if ((stage == Stage.FIRST || stage == Stage.SECOND) && retry == 0
                && client.options.getSoundSourceVolume(SoundSource.MUSIC) > 0
                && (track == null || !client.getSoundManager().isActive(track))) {
            start(client, stage == Stage.SECOND);
        }
    }

    private static void start(Minecraft client, boolean second) {
        stopTrack(client);
        retry = 40;
        if (client.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0) return;
        track = new Track(second);
        client.getSoundManager().play(track);
    }

    private static void stopTrack(Minecraft client) {
        if (track != null) {
            track.finish();
            client.getSoundManager().stop(track);
            track = null;
        }
    }

    private static void reset(Minecraft client) {
        stopTrack(client);
        fight = null;
        stage = Stage.OFF;
        sincePacket = retry = 0;
    }

    private static final class Track extends AbstractTickableSoundInstance implements FabricSoundInstance {
        private final ResourceLocation loopFile;
        private int fadeLeft;
        private float fadeStep;

        Track(boolean second) {
            super(SoundEvent.createVariableRangeEvent(TearsOfInk.id(second ? "orion_phase2" : "orion_phase1")),
                    SoundSource.MUSIC, RandomSource.create());
            loopFile = TearsOfInk.id(second ? "sounds/orion_phase2_loop.ogg" : "sounds/orion_phase1_loop.ogg");
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            looping = false; // The custom PCM stream itself loops, never restart the intro.
            volume = MIX_VOLUME;
            pitch = 1f;
        }

        void fade(int ticks) {
            if (fadeLeft == 0 || ticks < fadeLeft) {
                fadeLeft = ticks;
                fadeStep = volume / ticks;
            }
        }

        @Override public void tick() {
            if (fadeLeft > 0) {
                if (--fadeLeft == 0) {
                    volume = 0;
                    stop();
                } else volume = Math.max(0, volume - fadeStep);
            }
        }

        void finish() { stop(); }

        @Override public CompletableFuture<AudioStream> getAudioStream(
                SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
            return loader.getStream(id, false).thenCompose(intro ->
                    loader.getStream(loopFile, true).handle((loop, failure) -> {
                        if (failure != null) {
                            try { intro.close(); } catch (IOException ignored) {}
                            throw new CompletionException(failure);
                        }
                        if (!intro.getFormat().matches(loop.getFormat())
                                || !loop.getFormat().matches(intro.getFormat())) {
                            try { intro.close(); } catch (IOException ignored) {}
                            try { loop.close(); } catch (IOException ignored) {}
                            throw new CompletionException(new IOException("Orion score formats differ"));
                        }
                        return (AudioStream) new IntroThenLoop(intro, loop);
                    }));
        }
    }

    /** One OpenAL stream across the intro/loop boundary; the vanilla looping decoder
     * handles all subsequent loop boundaries without a client-tick gap. */
    static final class IntroThenLoop implements AudioStream {
        private AudioStream intro;
        private final AudioStream loop;
        private final AudioFormat format;

        IntroThenLoop(AudioStream intro, AudioStream loop) {
            this.intro = intro;
            this.loop = loop;
            this.format = intro.getFormat();
        }

        @Override public AudioFormat getFormat() { return format; }

        @Override public ByteBuffer read(int bytes) throws IOException {
            if (intro == null) return loop.read(bytes);
            ByteBuffer first = intro.read(bytes);
            if (first.remaining() >= bytes) return first;
            intro.close();
            intro = null;
            ByteBuffer next = loop.read(bytes - first.remaining());
            if (!first.hasRemaining()) return next;
            ByteBuffer joined = ByteBuffer.allocateDirect(first.remaining() + next.remaining());
            joined.put(first).put(next).flip();
            return joined;
        }

        @Override public void close() throws IOException {
            try {
                if (intro != null) intro.close();
            } finally {
                intro = null;
                loop.close();
            }
        }
    }
}
