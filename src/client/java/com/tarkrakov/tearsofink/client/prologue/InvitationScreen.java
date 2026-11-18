package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.prologue.Prologue;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Авторское письмо-приглашение: весь atlas 256x256, без обрезки подвесок и складок.
 * Текст умещается на чистой части пергамента между рунами, красным узором и чёрным
 * украшением. Подпись ставится над обвязкой, отдельная сургучная роза накладывается
 * после подписи рядом с именем на свободном поле, не поверх авторской печати на свёртке.
 * Логика серверного подписания и формат сохранений не меняются.
 */
public class InvitationScreen extends Screen {
	private static final ResourceLocation PAGE = TearsOfInk.id("textures/gui/invitation.png");
	private static final ResourceLocation BUTTON = TearsOfInk.id("textures/gui/sign_button.png");
	private static final ResourceLocation BUTTON_HOVER = TearsOfInk.id("textures/gui/sign_button_hover.png");
	private static final ResourceLocation SEAL = TearsOfInk.id("textures/gui/wax_seal.png");

	private static final int W = 256, H = 256;
	/** Бумага ниже рун: не заходить на кровавый орнамент слева и железо справа. */
	private static final int TEXT_X = 93, TEXT_Y = 53, TEXT_W = 91, TEXT_H = 102;
	private static final int TEXT_COLOR = 0x39252A, SIGN_COLOR = 0x681C2A;
	private static final int TEXT_LINE = 10, PARAGRAPH_GAP = 4;

	/** Место подписи над верхним сгибом обвязки. */
	private static final int BW = 80, BH = 20, SIGN_X = 93, SIGN_Y = 160;
	/** Отдельная сургучная роза появляется справа от подписи над перевязью. */
	private static final int SEAL_X = 142, SEAL_Y = 153, SEAL_W = 32, SEAL_H = 32;
	private static final int SEAL_TICKS = 24;

	private int signedTicks = -1;

	public InvitationScreen() {
		super(Component.translatable("item.tears-of-ink.invitation_letter"));
	}

	/** На небольшом экране уменьшается целый рисунок, а не только текст или кнопка. */
	private float paperScale() {
		return Math.min(1f, Math.min((width - 8f) / W, (height - 8f) / H));
	}

	private float left() { return (width - W * paperScale()) / 2f; }
	private float top() { return (height - H * paperScale()) / 2f; }
	private double localX(double mouseX) { return (mouseX - left()) / paperScale(); }
	private double localY(double mouseY) { return (mouseY - top()) / paperScale(); }

	/**
	 * У текста нет отдельного экрана/скролла: подбираем наибольший читаемый
	 * масштаб в отведённой части свитка. Абзацы разделены небольшим отступом
	 * вместо полноценной пустой строки; ни одна фраза не отрезается.
	 */
	private void drawLetter(GuiGraphics g) {
		String[] paragraphs = Component.translatable("letter.tears-of-ink.invitation")
				.getString().split("\\R\\s*\\R");
		List<List<FormattedCharSequence>> wrapped = new ArrayList<>();
		float textScale = 1f;
		for (int step = 0; step <= 30; step++) {
			textScale = 1f - step * 0.02f;
			int wrapWidth = Math.max(1, (int) (TEXT_W / textScale));
			wrapped.clear();
			int rows = 0;
			for (String paragraph : paragraphs) {
				List<FormattedCharSequence> lines = font.split(Component.literal(paragraph), wrapWidth);
				wrapped.add(lines);
				rows += lines.size();
			}
			int totalHeight = rows * TEXT_LINE + Math.max(0, paragraphs.length - 1) * PARAGRAPH_GAP;
			if (totalHeight * textScale <= TEXT_H) break;
		}

		g.pose().pushPose();
		g.pose().translate(TEXT_X, TEXT_Y, 0);
		g.pose().scale(textScale, textScale, 1f);
		int y = 0;
		for (int i = 0; i < wrapped.size(); i++) {
			for (FormattedCharSequence line : wrapped.get(i)) {
				g.drawString(font, line, 0, y, TEXT_COLOR, false);
				y += TEXT_LINE;
			}
			if (i + 1 < wrapped.size()) y += PARAGRAPH_GAP;
		}
		g.pose().popPose();
	}


	/** Ник остаётся слева от печати даже если он длинный. */
	private void drawSignature(GuiGraphics g, Component signature) {
		int width = 48;
		int actual = font.width(signature);
		float scale = actual > width ? (float) width / actual : 1f;
		g.pose().pushPose();
		g.pose().translate(SIGN_X + 27 - actual * scale / 2f, SIGN_Y + 6, 0);
		g.pose().scale(scale, scale, 1f);
		g.drawString(font, signature, 0, 0, SIGN_COLOR, false);
		g.pose().popPose();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);

		float scale = paperScale();
		int mx = (int) localX(mouseX), my = (int) localY(mouseY);
		g.pose().pushPose();
		g.pose().translate(left(), top(), 0);
		g.pose().scale(scale, scale, 1f);

		g.blit(PAGE, 0, 0, 0, 0, W, H, W, H);
		drawLetter(g);
		if (signedTicks < 0) {
			boolean hover = over(mx, my);
			g.blit(hover ? BUTTON_HOVER : BUTTON, SIGN_X, SIGN_Y, 0, 0, BW, BH, BW, BH);
			Component label = Component.translatable("letter.tears-of-ink.sign");
			g.drawString(font, label, SIGN_X + (BW - font.width(label)) / 2, SIGN_Y + 6, 0xF0E0D0, false);
		} else {
			g.hLine(SIGN_X + 6, SIGN_X + BW - 6, SIGN_Y + 17, 0x86694F);
			if (minecraft.player != null) {
				Component signature = Component.literal(minecraft.player.getName().getString())
						.withStyle(ChatFormatting.ITALIC);
				drawSignature(g, signature);
			}
			g.blit(SEAL, SEAL_X, SEAL_Y, 0, 0, SEAL_W, SEAL_H, SEAL_W, SEAL_H);
		}
		g.pose().popPose();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(g);
	}

	private boolean over(double mx, double my) {
		return mx >= SIGN_X && mx < SIGN_X + BW && my >= SIGN_Y && my < SIGN_Y + BH;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (signedTicks < 0 && button == 0 && over(localX(mx), localY(my))) {
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
