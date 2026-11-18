package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.client.fog.LanternClarity;
import com.tarkrakov.tearsofink.entity.patrol.PaleSoldier;
import com.tarkrakov.tearsofink.sound.ModSounds;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Client-only soundscape. No gameplay state, fake creatures, world changes or chunk loading. */
public final class PaleForestAmbience {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final SoundEvent[] AIR = {ModSounds.PALE_FOREST_AIR, ModSounds.PALE_FOREST_MEMORY_AIR, ModSounds.PALE_FOREST_FOG_AIR};
    private static final SoundEvent[] MEMORY = {ModSounds.PALE_FOREST_MEMORY_01, ModSounds.PALE_FOREST_MEMORY_02};
    private static final SoundEvent[] FOG = {ModSounds.PALE_FOREST_FOG_PHRASE_01, ModSounds.PALE_FOREST_FOG_PHRASE_02, ModSounds.PALE_FOREST_FOG_PHRASE_03};
    private static final int[] MEMORY_TICKS = {560, 720}, FOG_TICKS = {480, 440, 500};
    private static final Layer[] wind = new Layer[3];
    private static final int[] windAge = new int[3];
    private static ClientLevel world;
    private static Layer music;
    private static SoundInstance detail;
    private static int musicAge, musicDuration, musicWait, woodWait, passageWait, dangerScan, combatHold;
    private static boolean darkPassage, fogMusic;
    private static float dark;
    private static int lastMemory = -1, lastFog = -1;

    private PaleForestAmbience() {}

    public static void init() { ClientTickEvents.END_CLIENT_TICK.register(PaleForestAmbience::tick); }

    public static boolean inForest(Minecraft c) {
        return c.level != null && c.player != null && ModDimensions.isLands(c.level)
                && c.level.hasChunkAt(c.player.blockPosition())
                && c.level.getBiome(c.player.blockPosition()).is(ModBiomes.PALE_FOREST);
    }

    private static int between(int lo, int hi) { return lo + RANDOM.nextInt(hi - lo + 1); }

    private static void reset(Minecraft c) {
        var engine = c.getSoundManager();
        for (int i = 0; i < wind.length; i++) {
            if (wind[i] != null) engine.stop(wind[i]);
            wind[i] = null; windAge[i] = 0;
        }
        if (music != null) engine.stop(music);
        if (detail != null) engine.stop(detail);
        music = null; detail = null; musicAge = 0; musicDuration = 0;
        musicWait = between(500, 900); woodWait = between(180, 400);
        passageWait = between(600, 1200); darkPassage = false; dark = 0f;
        dangerScan = 0; combatHold = 0; lastMemory = lastFog = -1;
    }

    private static void tick(Minecraft c) {
        if (world != c.level) { reset(c); world = c.level; }
        if (c.isPaused()) return;
        boolean here = inForest(c) && c.player.isAlive();
        var engine = c.getSoundManager();
        if (!here) {
            // Fade in the same dimension; hard reset on unload or dimension change.
            for (int i = 0; i < wind.length; i++) updateWind(c, i, 0f);
            if (music != null) {
                music.target = 0f;
                if (++musicAge > 40 && (music.quiet() || !engine.isActive(music))) {
                    engine.stop(music); music = null;
                }
            }
            if (detail != null) { engine.stop(detail); detail = null; }
            dark = 0f; darkPassage = false;
            passageWait = between(600, 1200); musicWait = between(500, 900); woodWait = between(180, 400);
            combatHold = 0;
            return;
        }

        // Read existing visual clarity, not world brightness: Soul, hand and world lanterns are already accounted for.
        float clarity = Mth.clamp((LanternClarity.targetFogEnd(10f) - 10f) / 22f, 0f, 1f);
        if (c.player.hurtTime > 0 || c.player.swinging) combatHold = 100;
        if (combatHold > 0) combatHold--;
        if (--dangerScan <= 0) {
            dangerScan = 20;
            if (!world.getEntitiesOfClass(PaleSoldier.class, c.player.getBoundingBox().inflate(12),
                    s -> s.isAlive() && s.isAggressiveSynced()).isEmpty()) combatHold = Math.max(combatHold, 60);
        }
        boolean fighting = combatHold > 0;
        boolean singing = StriderSongClient.audibleNear(c);
        if (clarity > 0.30f) { darkPassage = false; passageWait = Math.max(passageWait, 400); }
        else if (--passageWait <= 0) {
            darkPassage = !darkPassage;
            passageWait = darkPassage ? between(700, 1500) : between(900, 1800);
        }
        dark = approach(dark, darkPassage ? 1f - clarity : 0f, 0.004f);

        boolean musicEnabled = c.options.getSoundSourceVolume(SoundSource.MUSIC) > 0f;
        if (music != null) {
            musicAge++;
            music.target = !musicEnabled ? 0f : singing ? 0.025f : fighting ? 0.045f : 0.24f;
            if (!musicEnabled || musicAge > musicDuration + 60 || (musicAge > 40 && !engine.isActive(music))) {
                engine.stop(music); music = null;
                musicWait = between(1100, 2200);
            }
        } else if (musicEnabled && musicWait > 0) {
            if (!fighting && !singing) musicWait--;
        } else if (musicEnabled && !fighting && !singing) {
            fogMusic = dark > 0.52f;
            int index;
            if (fogMusic) {
                index = nextIndex(FOG.length, lastFog); lastFog = index;
                music = new Layer(FOG[index], SoundSource.MUSIC, false, 0.24f);
                musicDuration = FOG_TICKS[index];
            } else {
                index = nextIndex(MEMORY.length, lastMemory); lastMemory = index;
                music = new Layer(MEMORY[index], SoundSource.MUSIC, false, 0.24f);
                musicDuration = MEMORY_TICKS[index];
            }
            musicAge = 0; engine.play(music);
        }

        // One constant-level bed, smoothly redistributed among the three versions.
        float memory = music != null && !fogMusic ? 0.8f * (1f - dark) : 0f;
        float bed = (0.24f - 0.035f * clarity) * (fighting ? 0.42f : singing ? 0.65f : 1f);
        updateWind(c, 0, bed * Math.max(0f, 1f - dark - memory));
        updateWind(c, 1, bed * memory);
        updateWind(c, 2, bed * dark);

        if (--woodWait <= 0) {
            woodWait = dark > 0.5f ? between(260, 620) : between(180, 440);
            if (!fighting && !singing && c.options.getSoundSourceVolume(SoundSource.AMBIENT) > 0f
                    && (detail == null || !engine.isActive(detail))) playWood(c);
        }
    }

    private static int nextIndex(int count, int previous) {
        if (previous < 0) return RANDOM.nextInt(count);
        return (previous + 1 + RANDOM.nextInt(count - 1)) % count;
    }

    private static void updateWind(Minecraft c, int i, float wanted) {
        var engine = c.getSoundManager();
        boolean audible = c.options.getSoundSourceVolume(SoundSource.AMBIENT) > 0f;
        if (!audible) wanted = 0f;
        Layer layer = wind[i];
        if (layer != null) {
            layer.target = wanted;
            if (++windAge[i] > 40 && (!engine.isActive(layer) || (wanted <= 0.001f && layer.quiet()))) {
                engine.stop(layer); wind[i] = null; layer = null;
            }
        }
        if (layer == null && wanted > 0.006f) {
            wind[i] = new Layer(AIR[i], SoundSource.AMBIENT, true, wanted);
            windAge[i] = 0; engine.play(wind[i]);
        }
    }

    private static void playWood(Minecraft c) {
        BlockPos from = c.player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // A bounded local search near actual pale logs. If no tree is loaded, stay silent.
        for (int attempt = 0; attempt < 64; attempt++) {
            int dx = between(-15, 15), dz = between(-15, 15);
            int r2 = dx * dx + dz * dz;
            if (r2 < 25 || r2 > 225) continue;
            pos.set(from.getX() + dx, from.getY(), from.getZ() + dz);
            if (!world.hasChunkAt(pos) || !world.getBiome(pos).is(ModBiomes.PALE_FOREST)) continue;
            for (int dy = -2; dy <= 6; dy++) {
                pos.setY(from.getY() + dy);
                var state = world.getBlockState(pos);
                if (!state.is(ModBlocks.PALE_LOG) && !state.is(ModBlocks.PALE_WOOD)) continue;
                SoundEvent event = switch (RANDOM.nextInt(8)) {
                    case 0, 1, 2 -> ModSounds.PALE_FOREST_TWIG;
                    case 3, 4 -> ModSounds.PALE_FOREST_BARK;
                    case 5 -> ModSounds.PALE_FOREST_BRANCH;
                    default -> ModSounds.PALE_FOREST_RUSTLE;
                };
                float volume = (0.20f + RANDOM.nextFloat() * 0.12f) * (dark > 0.5f ? 0.8f : 1f);
                detail = new SimpleSoundInstance(event, SoundSource.AMBIENT, volume, 0.96f + 0.08f * RANDOM.nextFloat(),
                        SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, false);
                c.getSoundManager().play(detail);
                return;
            }
        }
    }

    private static float approach(float current, float target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    private static final class Layer extends AbstractTickableSoundInstance {
        float target;

        Layer(SoundEvent event, SoundSource source, boolean loop, float target) {
            super(event, source, SoundInstance.createUnseededRandom());
            this.looping = loop;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.001f;
            this.pitch = 1f;
            this.target = target;
        }

        @Override public void tick() { volume = Math.max(0.001f, approach(volume, target, 0.0025f)); }

        boolean quiet() { return volume <= 0.002f; }
    }
}
