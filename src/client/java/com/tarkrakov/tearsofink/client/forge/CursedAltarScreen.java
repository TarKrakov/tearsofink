package com.tarkrakov.tearsofink.client.forge;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.weapon.forge.Awakening;
import com.tarkrakov.tearsofink.weapon.forge.CursedAltarMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Окно Проклятого алтаря. Фон — gui/cursed_altar.png (176×186, заглушка Claude): слева ячейки оружия (12,20),
 * реагента (12,44), усилителя (12,68); справа — слоты свойств кнопками, выпавшее и кнопки действий.
 */
public class CursedAltarScreen extends AbstractContainerScreen<CursedAltarMenu> {
	private static final ResourceLocation BACKGROUND = TearsOfInk.id("textures/gui/cursed_altar.png");
	private final Button[] slotButtons = new Button[3];
	private Button awaken, keep, empower;

	public CursedAltarScreen(CursedAltarMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageHeight = 186;
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		for (int i = 0; i < 3; i++) {
			int id = i;
			slotButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> click(id))
					.bounds(leftPos + 34, topPos + 16 + i * 14, 136, 13).build());
		}
		awaken = addRenderableWidget(Button.builder(Component.empty(), b -> click(CursedAltarMenu.BTN_AWAKEN))
				.bounds(leftPos + 34, topPos + 74, 62, 13).build());
		keep = addRenderableWidget(Button.builder(Component.translatable("altar.tears-of-ink.keep"), b -> click(CursedAltarMenu.BTN_KEEP))
				.bounds(leftPos + 98, topPos + 74, 34, 13).build());
		empower = addRenderableWidget(Button.builder(Component.translatable("altar.tears-of-ink.empower"), b -> click(CursedAltarMenu.BTN_EMPOWER))
				.bounds(leftPos + 134, topPos + 74, 36, 13).build());
		refresh();
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		refresh();
	}

	private void refresh() {
		if (awaken == null) return;
		ItemStack w = menu.weapon();
		int slots = w.isEmpty() ? 0 : Awakening.slots(w);
		for (int i = 0; i < 3; i++) {
			slotButtons[i].visible = i < Math.max(1, slots);
			slotButtons[i].active = i < slots;
			MutableComponent label = Component.literal(i == menu.selected() && slots > 0 ? "▶ " : "");
			Awakening.Slot s = w.isEmpty() ? null : Awakening.get(w, i);
			if (w.isEmpty()) label.append(Component.translatable("altar.tears-of-ink.put_weapon"));
			else if (s == null) label.append(Component.translatable("awakening.tears-of-ink.empty"));
			else label.append(Component.translatable("awakening.tears-of-ink." + s.awakening().id))
						.append(" ").append(Component.translatable("awakening.tears-of-ink.level." + s.level()));
			slotButtons[i].setMessage(label);
		}
		boolean rolled = menu.pending() != -2;
		awaken.setMessage(Component.translatable(rolled ? "altar.tears-of-ink.reroll" : "altar.tears-of-ink.awaken",
				w.isEmpty() ? 0 : Awakening.cost(w)));
		awaken.active = !w.isEmpty();
		keep.active = menu.pending() >= 0;
		empower.active = !w.isEmpty() && Awakening.get(w, menu.selected()) != null;
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		super.renderLabels(g, mouseX, mouseY);
		Component line;
		int p = menu.pending();
		if (p == -1) line = Component.translatable("altar.tears-of-ink.nothing").withStyle(ChatFormatting.DARK_GRAY);
		else if (p >= 0) {
			Awakening a = Awakening.values()[p];
			line = Component.translatable("altar.tears-of-ink.rolled",
					Component.translatable("awakening.tears-of-ink." + a.id).append(" ")
							.append(Component.translatable("awakening.tears-of-ink.level." + menu.pendingLevel())))
					.withStyle(a.blood ? ChatFormatting.DARK_RED : ChatFormatting.DARK_PURPLE);
		} else line = Component.translatable("altar.tears-of-ink.hint").withStyle(ChatFormatting.GRAY);
		g.drawString(font, line, 34, 61, 0x404040, false);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		g.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

    private void renderPropertyTooltip(GuiGraphics g,int x,int y){
        ItemStack w=menu.weapon();
        if(w.isEmpty())return;
        Awakening.Slot slot=null;
        int pending=menu.pending();
        if(pending>=0&&pending<Awakening.values().length&&isHovering(34,59,136,14,x,y))
            slot=new Awakening.Slot(Awakening.values()[pending],menu.pendingLevel());
        else for(int i=0;i<3;i++){
            if(slotButtons[i].visible&&isHovering(34,16+i*14,136,13,x,y)){slot=Awakening.get(w,i);break;}
        }
        if(slot==null)return;
        Awakening a=slot.awakening();
        List<Component> lines=new ArrayList<>();
        lines.add(Component.translatable("awakening.tears-of-ink."+a.id).append(" ").append(Component.translatable("awakening.tears-of-ink.level."+slot.level())).withStyle(a.blood?ChatFormatting.RED:ChatFormatting.LIGHT_PURPLE));
        if(!Awakening.allowedOn(w,a))lines.add(Component.translatable("altar.tears-of-ink.incompatible").withStyle(ChatFormatting.GRAY));
        else {
            lines.add(a.description(w,slot.level()));
            if(a.blood!=Awakening.bloodWeapon(w))lines.add(Component.translatable("altar.tears-of-ink.foreign").withStyle(ChatFormatting.GRAY));
        }
        List<net.minecraft.util.FormattedCharSequence> wrapped=new ArrayList<>();
        for(Component line:lines)wrapped.addAll(font.split(line,Math.max(48,Math.min(200,width-24))));
        g.renderTooltip(font,wrapped,x,y);
    }

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
        renderPropertyTooltip(g,mouseX,mouseY);
	}
}
