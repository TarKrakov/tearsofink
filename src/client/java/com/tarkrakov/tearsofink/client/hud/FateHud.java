package com.tarkrakov.tearsofink.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.prologue.DiaryScreen;
import com.tarkrakov.tearsofink.prologue.FateProgress;
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

    public static void toggle() {
        enabled = !enabled;
        try {
            Files.createDirectories(PREF.getParent());
            Files.writeString(PREF, Boolean.toString(enabled));
        } catch (IOException ignored) { }
    }

    private record Line(List<FormattedCharSequence> label, List<FormattedCharSequence> task) {}

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.options.hideGui || mc.player == null || mc.level == null || mc.screen != null
                || mc.player.isSpectator() || !ModDimensions.isLands(mc.level)
                || mc.getDebugOverlay().showDebugScreen() || mc.player.isDeadOrDying()) return;
        int mask = DiaryScreen.fateMask;
        if (mask < 0 || !FateProgress.Goal.ARRIVAL.done(mask)) return;
        int w = g.guiWidth(), h = g.guiHeight();
        int available = Math.min(190, Math.max(76, w / 3 - 14));
        int limit = Math.min(h - 70, h / 2 - 10);
        if (limit < 24 || w < 160) return;
        List<Line> lines = new ArrayList<>();
        int height = 4;
        for (var track : FateProgress.Track.values()) {
            var goal = FateProgress.active(track, mask);
            if (goal == null) continue;
            var label = mc.font.split(Component.translatable(PREFIX + "track." + track.id), available - 10);
            var task = mc.font.split(Component.translatable(PREFIX + goal.id + ".hud"), available - 10);
            lines.add(new Line(label, task));
            height += 7 + 10 * (label.size() + task.size());
        }
        if (lines.isEmpty()) return;
        float scale = Math.min(1f, (float) limit / height);
        // Top-left, well clear of the central boss bar and bottom combat indicators.
        g.pose().pushPose();
        g.pose().translate(6, 6, 0);
        g.pose().scale(scale, scale, 1f);
        int y = 0;
        for (Line line : lines) {
            int boxH = 4 + 10 * (line.label().size() + line.task().size());
            g.fill(0, y, available, y + boxH, 0x80221A23);
            g.fill(0, y, 2, y + boxH, 0xFFC6A168);
            int yy = y + 2;
            for (var row : line.label()) {
                g.drawString(mc.font, row, 6, yy, 0xFFE7C58D, true);
                yy += 10;
            }
            for (var row : line.task()) {
                g.drawString(mc.font, row, 6, yy, 0xFFF0EAE2, true);
                yy += 10;
            }
            y += boxH + 3;
        }
        g.pose().popPose();
    }
}
