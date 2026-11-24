package com.tarkrakov.tearsofink.client.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.SkillKeys;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Local authoring mixer. Values are relative to the shipped mix, not game balance or server state. */
public final class AudioMixer {
    public static final int MAX_PERCENT = 300;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tearsofink-audio.json");

    public enum Group {
        FOREST, MUSIC, OWLS, CATERPILLARS, DIARY, DEER;
        public String key() { return name().toLowerCase(Locale.ROOT); }
        public Component label() { return Component.translatable("mixer.tears-of-ink.group." + key()); }
    }

    public record Entry(ResourceLocation id, Group group, SoundSource source, float previewVolume, String translation) {
        public String key() { return id.getPath(); }
        public Component label() { return Component.translatable(translation); }
    }

    // Explicit allowlist: Strider sounds and any unrelated/vanilla sounds never pass through this mixer.
    private static final List<Entry> ENTRIES = List.of(
        new Entry(TearsOfInk.id("pale_forest_air"), Group.FOREST, SoundSource.AMBIENT, 0.0750f, "mixer.tears-of-ink.event.pale_forest_air"),
        new Entry(TearsOfInk.id("pale_forest_bark"), Group.FOREST, SoundSource.AMBIENT, 0.0750f, "subtitles.tears-of-ink.pale_forest_bark"),
        new Entry(TearsOfInk.id("pale_forest_branch"), Group.FOREST, SoundSource.AMBIENT, 0.0750f, "subtitles.tears-of-ink.pale_forest_branch"),
        new Entry(TearsOfInk.id("pale_forest_fog_air"), Group.FOREST, SoundSource.AMBIENT, 0.0750f, "mixer.tears-of-ink.event.pale_forest_fog_air"),
        new Entry(TearsOfInk.id("pale_forest_memory_air"), Group.FOREST, SoundSource.AMBIENT, 0.0750f, "mixer.tears-of-ink.event.pale_forest_memory_air"),
        new Entry(TearsOfInk.id("pale_forest_rustle"), Group.FOREST, SoundSource.AMBIENT, 0.0375f, "subtitles.tears-of-ink.pale_forest_rustle"),
        new Entry(TearsOfInk.id("pale_forest_twig"), Group.FOREST, SoundSource.AMBIENT, 0.0750f, "subtitles.tears-of-ink.pale_forest_twig"),
        new Entry(TearsOfInk.id("pale_forest_fog_phrase_01"), Group.MUSIC, SoundSource.MUSIC, 0.0800f, "mixer.tears-of-ink.event.pale_forest_fog_phrase_01"),
        new Entry(TearsOfInk.id("pale_forest_fog_phrase_02"), Group.MUSIC, SoundSource.MUSIC, 0.0800f, "mixer.tears-of-ink.event.pale_forest_fog_phrase_02"),
        new Entry(TearsOfInk.id("pale_forest_fog_phrase_03"), Group.MUSIC, SoundSource.MUSIC, 0.0800f, "mixer.tears-of-ink.event.pale_forest_fog_phrase_03"),
        new Entry(TearsOfInk.id("pale_forest_memory_01"), Group.MUSIC, SoundSource.MUSIC, 0.0800f, "mixer.tears-of-ink.event.pale_forest_memory_01"),
        new Entry(TearsOfInk.id("pale_forest_memory_02"), Group.MUSIC, SoundSource.MUSIC, 0.0800f, "mixer.tears-of-ink.event.pale_forest_memory_02"),
        new Entry(TearsOfInk.id("caterpillar_blood_crawl"), Group.CATERPILLARS, SoundSource.NEUTRAL, 0.3500f, "subtitles.tears-of-ink.caterpillar_blood_crawl"),
        new Entry(TearsOfInk.id("caterpillar_ink_crawl"), Group.CATERPILLARS, SoundSource.NEUTRAL, 0.3000f, "subtitles.tears-of-ink.caterpillar_ink_crawl"),
        new Entry(TearsOfInk.id("caterpillar_pale_crawl"), Group.CATERPILLARS, SoundSource.NEUTRAL, 0.3500f, "subtitles.tears-of-ink.caterpillar_pale_crawl"),
        new Entry(TearsOfInk.id("pale_owl_hoot"), Group.OWLS, SoundSource.NEUTRAL, 0.2400f, "subtitles.tears-of-ink.pale_owl_hoot"),
        new Entry(TearsOfInk.id("pale_owl_wing"), Group.OWLS, SoundSource.NEUTRAL, 0.3000f, "subtitles.tears-of-ink.pale_owl_wing"),
        new Entry(TearsOfInk.id("pale_deer_voice"), Group.DEER, SoundSource.NEUTRAL, 0.5400f, "subtitles.tears-of-ink.pale_deer_voice"),
        new Entry(TearsOfInk.id("pale_deer_snort"), Group.DEER, SoundSource.NEUTRAL, 0.4200f, "subtitles.tears-of-ink.pale_deer_snort"),
        new Entry(TearsOfInk.id("pale_deer_step"), Group.DEER, SoundSource.NEUTRAL, 0.3600f, "subtitles.tears-of-ink.pale_deer_step"),
        new Entry(TearsOfInk.id("pale_deer_eat"), Group.DEER, SoundSource.NEUTRAL, 0.2800f, "subtitles.tears-of-ink.pale_deer_eat"),
        new Entry(TearsOfInk.id("pale_deer_alert_snort"), Group.DEER, SoundSource.NEUTRAL, 0.5400f, "subtitles.tears-of-ink.pale_deer_alert_snort"),
        new Entry(TearsOfInk.id("pale_deer_watch_roar"), Group.DEER, SoundSource.NEUTRAL, 0.8200f, "subtitles.tears-of-ink.pale_deer_watch_roar"),
        new Entry(TearsOfInk.id("pale_deer_roar"), Group.DEER, SoundSource.NEUTRAL, 0.6200f, "subtitles.tears-of-ink.pale_deer_roar"),
        new Entry(TearsOfInk.id("pale_deer_run_step"), Group.DEER, SoundSource.NEUTRAL, 0.4800f, "subtitles.tears-of-ink.pale_deer_run_step"),
        new Entry(TearsOfInk.id("diary_page_turn"), Group.DIARY, SoundSource.PLAYERS, 0.5000f, "subtitles.tears-of-ink.diary_page_turn"),
        new Entry(TearsOfInk.id("diary_quill_write"), Group.DIARY, SoundSource.PLAYERS, 0.5000f, "subtitles.tears-of-ink.diary_quill_write")
    );
    private static final Map<ResourceLocation, Entry> BY_ID =
            ENTRIES.stream().collect(Collectors.toUnmodifiableMap(Entry::id, e -> e));
    private static final Map<Group, List<Entry>> BY_GROUP =
            ENTRIES.stream().collect(Collectors.groupingBy(Entry::group));
    private static final class Config {
        Map<String, Integer> groups = new TreeMap<>();
        Map<String, Integer> events = new TreeMap<>();
    }

    private static Config config = defaults();
    private static boolean dirty;
    private static boolean openRequested;
    private AudioMixer() {}

    private static Config defaults() {
        Config c = new Config();
        for (Group group : Group.values()) c.groups.put(group.key(), 100);
        for (Entry entry : ENTRIES) c.events.put(entry.key(), 100);
        return c;
    }

    private static int clamp(int value) { return Math.max(0, Math.min(MAX_PERCENT, value)); }

    public static List<Entry> entries(Group group) { return BY_GROUP.getOrDefault(group, List.of()); }
    public static int groupPercent(Group group) { return config.groups.getOrDefault(group.key(), 100); }
    public static int eventPercent(Entry entry) { return config.events.getOrDefault(entry.key(), 100); }
    public static void setGroup(Group group, int value) {
        int v = clamp(value);
        if (v != groupPercent(group)) { config.groups.put(group.key(), v); dirty = true; }
    }
    public static void setEvent(Entry entry, int value) {
        int v = clamp(value);
        if (v != eventPercent(entry)) { config.events.put(entry.key(), v); dirty = true; }
    }
    public static void resetGroup(Group group) {
        setGroup(group, 100);
        for (Entry entry : entries(group)) setEvent(entry, 100);
    }

    /** Queried before the normal sound engine's volume clamp and category controls. */
    public static float gain(ResourceLocation id) {
        // No matching entry for every Strider event, including future song IDs.
        Entry e = BY_ID.get(id);
        if (e == null) return 1f;
        return Math.min(3f, groupPercent(e.group()) * eventPercent(e) / 10000f);
    }

    public static String exportJson() { return GSON.toJson(config) + "\n"; }

    private static void load() {
        if (!Files.exists(FILE)) { dirty = true; save(); return; }
        try {
            Config old = GSON.fromJson(Files.readString(FILE), Config.class);
            if (old == null) return;
            Config clean = defaults();
            clean.groups.replaceAll((key, value) -> readPercent(old.groups, key));
            clean.events.replaceAll((key, value) -> readPercent(old.events, key));
            // Preserve inactive values for a future re-enable, without applying them.
            if (old.groups != null) old.groups.forEach((key, value) -> { if (key != null) clean.groups.putIfAbsent(key, readPercent(old.groups, key)); });
            if (old.events != null) old.events.forEach((key, value) -> { if (key != null) clean.events.putIfAbsent(key, readPercent(old.events, key)); });
            config = clean;
            // A new sound added in a later version gets the default 100%.
        } catch (Exception ex) {
            TearsOfInk.LOGGER.warn("Cannot read audio mixer config {}; using default mix", FILE, ex);
        }
    }

    private static int readPercent(Map<String, Integer> map, String key) {
        Integer v = map == null ? null : map.get(key);
        return v == null ? 100 : clamp(v);
    }

    public static boolean save() {
        if (!dirty) return true;
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling(FILE.getFileName().toString() + ".tmp");
            Files.writeString(tmp, exportJson());
            try {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            return true;
        } catch (IOException | SecurityException ex) {
            TearsOfInk.LOGGER.warn("Cannot save audio mixer config {}", FILE, ex);
            return false;
        }
    }

    public static void init() {
        load();
        KeyMapping key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tears-of-ink.audio_mixer", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, SkillKeys.CATEGORY));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("inksound").executes(ctx -> {
                    openRequested = true;
                    return 1;
                })));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) {
                if (client.player != null && client.screen == null) openRequested = true;
            }
            if (openRequested) {
                openRequested = false;
                if (client.player != null) client.setScreen(new AudioMixerScreen());
            }
        });
    }
}
