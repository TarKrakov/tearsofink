package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.prologue.Prologue;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Окно письма-приглашения: испачканный лист (gui/invitation.png, лист — левый верхний угол 192×232 картинки 256×256),
 * текст из ru_ru.json (letter.tears-of-ink.invitation), кнопка «подписать» (gui/sign_button*.png, 80×20).
 * После подписи — сургучная печать (gui/wax_seal.png, 32×32), письмо уходит и сгорает.
 */
public class InvitationScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/invitation.png");
	private static final ResourceLocation BUTTON = TearsOfInk.id("textures/gui/sign_button.png");
	private static final ResourceLocation BUTTON_HOVER = TearsOfInk.id("textures/gui/sign_button_hover.png");
	private static final ResourceLocation SEAL = TearsOfInk.id("textures/gui/wax_seal.png");
	private static final int W = 192, H = 232, BW = 80, BH = 20, TEXT_MARGIN = 18;
	/** Сколько тиков показывать печать перед закрытием. */
	private static final int SEAL_TICKS = 24;

	private int signedTicks = -1;

	public InvitationScreen() {
		super(Component.translatable("item.tears-of-ink.invitation_letter"));
	}

	private int left() { return (width - W) / 2; }
	private int top() { return (height - H) / 2; }
	private int buttonX() { return left() + (W - BW) / 2; }
	private int buttonY() { return top() + H - BH - 14; }

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int l = left(), t = top();
		g.blit(PAGE, l, t, 0, 0, W, H, 256, 256);
		List<FormattedCharSequence> lines = font.split(Component.translatable("letter.tears-of-ink.invitation"), W - TEXT_MARGIN * 2);
		int y = t + TEXT_MARGIN;
		for (FormattedCharSequence line : lines) {
			g.drawString(font, line, l + TEXT_MARGIN, y, 0x2A1E2C, false);
			y += 10;
		}
		if (signedTicks < 0) {
			boolean hover = over(mouseX, mouseY);
			g.blit(hover ? BUTTON_HOVER : BUTTON, buttonX(), buttonY(), 0, 0, BW, BH, BW, BH);
			Component label = Component.translatable("letter.tears-of-ink.sign");
			g.drawString(font, label, buttonX() + (BW - font.width(label)) / 2, buttonY() + 6, 0xF0E0D0, false);
		} else {
			g.blit(SEAL, buttonX() + (BW - 32) / 2, buttonY() - 6, 0, 0, 32, 32, 32, 32);
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(g);
	}

	private boolean over(double mx, double my) {
		return mx >= buttonX() && mx < buttonX() + BW && my >= buttonY() && my < buttonY() + BH;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (signedTicks < 0 && button == 0 && over(mx, my)) {
			signedTicks = 0;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_PLACE, 0.6f));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PUT, 0.8f));
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public void tick() {
		if (signedTicks >= 0 && ++signedTicks >= SEAL_TICKS) {
			ClientPlayNetworking.send(new Prologue.SignContract());
			onClose();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
