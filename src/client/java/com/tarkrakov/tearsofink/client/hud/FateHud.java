package com.tarkrakov.tearsofink.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.prologue.DiaryScreen;
import com.tarkrakov.tearsofink.prologue.FateProgress;
import com.tarkrakov.tearsofink.vow.Vow;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Read-only projection of the same personal server mask as the Historian's diary. */
public final class FateHud {
    private static final String PREFIX = "diary.tears-of-ink.fate.";
    private static final float HUD_SCALE = 0.5f;
    private static KeyMapping toggleKey;
    private static boolean enabled = true;
    private static final Path PREF = FabricLoader.getInstance().getConfigDir().resolve("tearsofink_fate_hud.txt");

    private FateHud() {}

    public static void init() {
        try {
            if (Files.isRegularFile(PREF)) enabled = !Files.readString(PREF).trim().equals("false");
        } catch (IOException ignored) { }
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.tears-of-ink.fate_hud",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.tears-of-ink"));
        HudRenderCallback.EVENT.register(FateHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggleKey.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    toggle();
                    mc.player.displayClientMessage(Component.translatable(PREFIX + (enabled ? "hud_on" : "hud_off")), true);
                }
            }
        });
    }

    public static Component keyLabel() {
        return toggleKey == null ? Component.literal("J") : toggleKey.getTranslatedKeyMessage();
    }

    public static boolean matches(int key, int scan) {
        return toggleKey != null && toggleKey.matches(key, scan);
    }

    public static boolean enabled() { return enabled; }

    public static void toggle() {
        enabled = !enabled;
        try {
            Files.createDirectories(PREF.getParent());
            Files.writeString(PREF, Boolean.toString(enabled));
        } catch (IOException ignored) { }
    }

    private record Theme(int panel, int bar, int title, int task) {}
    private record Line(List<FormattedCharSequence> label, List<FormattedCharSequence> task, Theme theme) {}

    private static final Theme STORY = new Theme(0x7A241F1C, 0xFFD8C39A, 0xFFF3E5C8, 0xFFF7F1E7);
    private static final Theme SOUL_CALL = new Theme(0x7A2A2416, 0xFFFFD36A, 0xFFFFE08A, 0xFFFFF3D1);
    private static final Theme INK_CALL = new Theme(0x7A231B2E, 0xFFB38CFF, 0xFFD3BBFF, 0xFFF0E8FF);
    private static final Theme NEUTRAL_CALL = new Theme(0x7A252028, 0xFFC8B6E8, 0xFFE0D6F4, 0xFFF2EEFA);
    private static final Theme CRAFT = new Theme(0x7A2A2018, 0xFFD28A47, 0xFFE9B172, 0xFFF8E7D2);

    private static Theme themeFor(FateProgress.Track track) {
        return switch (track) {
            case STORY -> STORY;
            case CALL -> switch (Vow.CLIENT) {
                case SOUL -> SOUL_CALL;
                case INK -> INK_CALL;
                default -> NEUTRAL_CALL;
            };
            case CRAFT -> CRAFT;
        };
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.options.hideGui || mc.player == null || mc.level == null || mc.screen != null
                || mc.player.isSpectator() || !ModDimensions.isLands(mc.level)
                || mc.getDebugOverlay().showDebugScreen() || mc.player.isDeadOrDying()) return;
        int mask = DiaryScreen.fateMask;
        if (mask < 0 || !FateProgress.Goal.ARRIVAL.done(mask)) return;
        int w = g.guiWidth(), h = g.guiHeight();
        int available = Math.min(190, Math.max(76, w / 2 - 18));
        int limit = Math.min(h - 70, h / 2 - 10);
        if (limit < 24 || w < 160) return;
        List<Line> lines = new ArrayList<>();
        int height = 4;
        for (var track : FateProgress.Track.values()) {
            var goal = FateProgress.active(track, mask, Vow.CLIENT);
            if (goal == null) continue;
            var label = mc.font.split(Component.translatable(PREFIX + "track." + track.id), available - 10);
            String taskKey = goal.textId(Vow.CLIENT) + ".hud";
            var task = mc.font.split(Component.translatable(PREFIX + taskKey), available - 10);
            lines.add(new Line(label, task, themeFor(track)));
            height += 7 + 10 * (label.size() + task.size());
        }
        if (lines.isEmpty()) return;
        float scale = HUD_SCALE * Math.min(1f, (float) limit / height);
        // Top-left, well clear of the central boss bar and bottom combat indicators.
        g.pose().pushPose();
        g.pose().translate(6, 6, 0);
        g.pose().scale(scale, scale, 1f);
        int y = 0;
        for (Line line : lines) {
            int boxH = 4 + 10 * (line.label().size() + line.task().size());
            g.fill(0, y, available, y + boxH, line.theme().panel());
            g.fill(0, y, 2, y + boxH, line.theme().bar());
            int yy = y + 2;
            for (var row : line.label()) {
                g.drawString(mc.font, row, 6, yy, line.theme().title(), true);
                yy += 10;
            }
            for (var row : line.task()) {
                g.drawString(mc.font, row, 6, yy, line.theme().task(), true);
                yy += 10;
            }
            y += boxH + 3;
        }
        g.pose().popPose();
    }
}
