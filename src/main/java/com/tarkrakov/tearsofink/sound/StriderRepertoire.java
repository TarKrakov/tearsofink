package com.tarkrakov.tearsofink.sound;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.EnumSet;

/** Server-side composition selection and the shared mapping from packet voice to sound event. */
public enum StriderRepertoire {
    CLOSE_PRAYER("close_prayer"),
    ECHOES_OF_GRIEF("echoes_of_grief"),
    LIGHT_IN_EXILE("light_in_exile");

    public static final int PARTS = 3;
    private static final StriderRepertoire[] COMPOSITIONS = values();
    private static final int[] INITIAL_WEIGHTS = {45, 35, 20};
    // Symmetric weights on transitions keep the initial distribution stationary.
    private static final int[][] NEXT_WEIGHTS = {{0, 30, 15}, {30, 0, 5}, {15, 5, 0}};
    private static final int LEGACY_VOICES = 3;

    private final String soundName;

    StriderRepertoire(String soundName) {
        this.soundName = soundName;
    }

    public static StriderRepertoire fromIndex(int index) {
        return index >= 0 && index < COMPOSITIONS.length ? COMPOSITIONS[index] : null;
    }

    public static StriderRepertoire choose(RandomSource random, StriderRepertoire previous,
                                           EnumSet<StriderRepertoire> recent) {
        int[] weights = previous == null ? INITIAL_WEIGHTS : NEXT_WEIGHTS[previous.ordinal()];
        // If all three were sung by different members, still avoid the initiator's last one.
        boolean avoidAllRecent = recent.size() < COMPOSITIONS.length;
        int total = 0;
        for (var composition : COMPOSITIONS) {
            if (!avoidAllRecent || !recent.contains(composition)) total += weights[composition.ordinal()];
        }
        int roll = random.nextInt(total);
        for (var composition : COMPOSITIONS) {
            if (avoidAllRecent && recent.contains(composition)) continue;
            roll -= weights[composition.ordinal()];
            if (roll < 0) return composition;
        }
        throw new IllegalStateException("Invalid Strider song weights");
    }

    public int voice(int part) {
        return LEGACY_VOICES + ordinal() * PARTS + part;
    }

    public static ResourceLocation sound(int voice) {
        if (voice < 0 || voice >= LEGACY_VOICES + COMPOSITIONS.length * PARTS) return null;
        if (voice < LEGACY_VOICES) {
            return TearsOfInk.id(voice == 0 ? "strider_song" : "strider_song_" + (voice + 1));
        }
        int index = voice - LEGACY_VOICES;
        return TearsOfInk.id("strider_song_" + COMPOSITIONS[index / PARTS].soundName + "_" + (index % PARTS + 1));
    }
}
