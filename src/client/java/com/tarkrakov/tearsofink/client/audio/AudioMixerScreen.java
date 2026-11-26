package com.tarkrakov.tearsofink.client.audio;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Paginated live mixer. Does not pause the world; test in a safe spot. */
final class AudioMixerScreen extends Screen {
    private static AudioMixer.Group selected = AudioMixer.Group.FOREST;
    private int page, pageCount, left, panelWidth, sliderX, sliderWidth;
    private final List<Label> labels = new ArrayList<>();
    private SoundInstance preview;
    private Component notice = Component.empty();

    private record Label(Component text, int y) {}

    AudioMixerScreen() { super(text("title")); }

    private static Component text(String key, Object... args) {
        return Component.translatable("mixer.tears-of-ink." + key, args);
    }

    @Override
    protected void init() {
        labels.clear();
        panelWidth = Math.min(width - 12, 680);
        left = (width - panelWidth) / 2;
        sliderWidth = Math.max(48, (panelWidth - 105) * 45 / 100);
        sliderX = left + panelWidth - sliderWidth - 96;

        var groups = AudioMixer.Group.values();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> changeGroup(-1))
                .bounds(left, 23, 22, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> changeGroup(1))
                .bounds(left + panelWidth - 22, 23, 22, 20).build());

        labels.add(new Label(text("group_volume"), 51));
        controls(46, text("group_volume"), AudioMixer.groupPercent(selected),
                value -> AudioMixer.setGroup(selected, value), null, () -> {
                    AudioMixer.resetGroup(selected);
                    rebuild();
                });

        List<AudioMixer.Entry> events = AudioMixer.entries(selected);
        int rows = Math.max(1, (height - 130) / 22);
        pageCount = Math.max(1, (events.size() + rows - 1) / rows);
        page = Math.max(0, Math.min(page, pageCount - 1));

        for (int i = 0; i < rows && page * rows + i < events.size(); i++) {
            var entry = events.get(page * rows + i);
            int y = 78 + i * 22;
            labels.add(new Label(entry.label(), y + 6));
            controls(y, entry.label(), AudioMixer.eventPercent(entry),
                    value -> AudioMixer.setEvent(entry, value), entry, () -> {
                        AudioMixer.setEvent(entry, 100);
                        rebuild();
                    });
        }

        var prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuild(); })
                .bounds(left, height - 48, 22, 20).build());
        prev.active = page > 0;
        var next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuild(); })
                .bounds(left + panelWidth - 22, height - 48, 22, 20).build());
        next.active = page + 1 < pageCount;
        int third = (panelWidth - 8) / 3;
        addRenderableWidget(Button.builder(text("stop"), b -> stopPreview())
                .bounds(left, height - 24, third, 20).build());
        addRenderableWidget(Button.builder(text("copy"), b -> {
            if (AudioMixer.save()) {
                minecraft.keyboardHandler.setClipboard(AudioMixer.exportJson());
                notice = text("copied");
            } else notice = text("save_error");
        }).bounds(left + third + 4, height - 24, third, 20).build());
        addRenderableWidget(Button.builder(text("done"), b -> onClose())
                .bounds(left + 2 * (third + 4), height - 24, third, 20).build());
    }

    private void changeGroup(int direction) {
        var groups = AudioMixer.Group.values();
        selected = groups[Math.floorMod(selected.ordinal() + direction, groups.length)];
        page = 0;
        rebuild();
    }

    private void rebuild() {
        stopPreview();
        clearFocus();
        rebuildWidgets();
    }

    private void controls(int y, Component caption, int initial, IntConsumer change,
                          AudioMixer.Entry entry, Runnable reset) {
        EditBox number = new EditBox(font, sliderX + sliderWidth + 3, y, 32, 20, caption);
        number.setMaxLength(3);
        number.setFilter(s -> s.isEmpty() || (s.matches("[0-9]{1,3}") && Integer.parseInt(s) <= AudioMixer.MAX_PERCENT));
        number.setValue(Integer.toString(initial));

        PercentSlider slider = new PercentSlider(sliderX, y, sliderWidth, initial, n -> {
            change.accept(n);
            number.setValue(Integer.toString(n));
        });
        slider.setTooltip(Tooltip.create(entry == null ? caption :
                caption.copy().append(" · ").append(entry.key())));
        number.setResponder(s -> {
            if (!s.isEmpty()) {
                int n = Integer.parseInt(s);
                change.accept(n);
                slider.sync(n);
            }
        });
        addRenderableWidget(slider);
        addRenderableWidget(number);
        int buttonX = sliderX + sliderWidth + 38;
        if (entry != null) {
            var play = addRenderableWidget(Button.builder(text("test"), b -> play(entry))
                    .bounds(buttonX, y, 34, 20).build());
            play.setTooltip(Tooltip.create(text("preview_tip")));
        }
        var clear = addRenderableWidget(Button.builder(Component.literal("100"), b -> reset.run())
                .bounds(buttonX + 37, y, 21, 20).build());
        clear.setTooltip(Tooltip.create(text(entry == null ? "reset_group_tip" : "reset_tip")));
    }

    private void play(AudioMixer.Entry entry) {
        stopPreview();
        if (entry.source() == SoundSource.MUSIC && OrionMusicClient.ownsMusic(minecraft)) return;
        // A near-source audition using the usual base level, without world distance or a specific mob's pitch.
        preview = new SimpleSoundInstance(entry.id(), entry.source(), entry.previewVolume(), 1f,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        minecraft.getSoundManager().play(preview);
    }

    private void stopPreview() {
        if (preview != null && minecraft != null) minecraft.getSoundManager().stop(preview);
        preview = null;
    }

    @Override public void tick() {
        super.tick();
        if (preview != null && preview.getSource() == SoundSource.MUSIC
                && OrionMusicClient.ownsMusic(minecraft)) stopPreview();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        if (AudioMixer.save()) super.onClose();
        else notice = text("save_error");
    }

    @Override public void removed() {
        stopPreview();
        AudioMixer.save();
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xCF111018);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, 7, 0xFFFFFF);
        graphics.drawCenteredString(font, selected.label(), width / 2, 29, 0xEFE8D8);
        graphics.drawCenteredString(font, text("hint"), width / 2, 68, 0xB8B8B8);
        for (Label label : labels) {
            String value = label.text().getString();
            int available = Math.max(0, sliderX - left - 4);
            if (font.width(value) > available) value = font.plainSubstrByWidth(value, Math.max(0, available - font.width("…"))) + "…";
            graphics.drawString(font, value, left, label.y(), 0xFFFFFF);
        }
        Component footer = notice.getString().isEmpty() ? text("page", page + 1, pageCount) : notice;
        graphics.drawCenteredString(font, footer, width / 2, height - 42, 0xD4D0C8);
    }

    private static final class PercentSlider extends AbstractSliderButton {
        private final IntConsumer change;

        PercentSlider(int x, int y, int width, int initial, IntConsumer change) {
            super(x, y, width, 20, Component.literal(initial + "%"), initial / (double) AudioMixer.MAX_PERCENT);
            this.change = change;
        }

        private int percent() { return (int) Math.round(value * AudioMixer.MAX_PERCENT); }
        void sync(int percent) {
            value = percent / (double) AudioMixer.MAX_PERCENT;
            updateMessage();
        }
        @Override protected void updateMessage() { setMessage(Component.literal(percent() + "%")); }
        @Override protected void applyValue() { change.accept(percent()); }
    }
}
