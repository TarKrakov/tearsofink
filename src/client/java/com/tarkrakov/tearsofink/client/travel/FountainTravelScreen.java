package com.tarkrakov.tearsofink.client.travel;

import com.tarkrakov.tearsofink.travel.FountainTravel;
import com.tarkrakov.tearsofink.travel.FountainTravelPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

import static com.tarkrakov.tearsofink.travel.FountainTravelPackets.PAGE_SIZE;

public final class FountainTravelScreen extends Screen {
	private static java.util.UUID latestSession;
	private final FountainTravelPackets.Menu menu;
	private int panelWidth;
	private int top;

	private FountainTravelScreen(FountainTravelPackets.Menu menu) {
		super(text("title"));
		this.menu = menu;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(FountainTravelPackets.Menu.TYPE, (menu, context) ->
				context.client().execute(() -> {
					if (menu.session().equals(latestSession) && !(context.client().screen instanceof FountainTravelScreen)) return;
					latestSession = menu.session();
					context.client().setScreen(new FountainTravelScreen(menu));
				}));
		ClientPlayNetworking.registerGlobalReceiver(FountainTravelPackets.Close.TYPE, (close, context) ->
				context.client().execute(() -> {
					if (context.client().screen instanceof FountainTravelScreen screen && screen.menu.session().equals(close.session())) {
						context.client().setScreen(null);
					}
				}));
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));
			var result = FountainTravel.useMirror(player, level, hand);
			if (result.getResult() == InteractionResult.SUCCESS) {
				// The Fabric callback sends UseItem before the normal slot sync.
				var connection = Minecraft.getInstance().getConnection();
				if (connection != null) connection.send(new ServerboundSetCarriedItemPacket(player.getInventory().selected));
			}
			return result;
		});
	}

	@Override
	protected void init() {
		panelWidth = Math.min(310, width - 20);
		top = Math.max(5, (height - 171) / 2);
		int left = (width - panelWidth) / 2;
		for (int row = 0; row < menu.entries().size(); row++) {
			int index = row;
			BlockPos pos = menu.entries().get(row);
			boolean current = pos.equals(menu.source());
			boolean last = menu.last().filter(pos::equals).isPresent();
			Component suffix = current ? text(last ? "current_last" : "current") : last ? text("last") : Component.empty();
			Component label = text("entry", menu.page() * PAGE_SIZE + row + 1, coords(pos), direction(pos), suffix);
			Button button = addRenderableWidget(Button.builder(label, ignored -> select(menu.page(), index))
					.bounds(left, top + 29 + row * 20, panelWidth, 19).build());
			button.active = !current;
		}
		int bottom = top + 139;
		if (menu.pages() > 1) {
			var previous = addRenderableWidget(Button.builder(text("previous"), ignored -> select(menu.page() - 1, -1))
					.bounds(left, bottom, 65, 20).build());
			var next = addRenderableWidget(Button.builder(text("next"), ignored -> select(menu.page() + 1, -1))
					.bounds(left + panelWidth - 65, bottom, 65, 20).build());
			previous.active = menu.page() > 0;
			next.active = menu.page() + 1 < menu.pages();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, top, 0xFFFFFF);
		graphics.drawCenteredString(font, text("source_line", coords(menu.source())), width / 2, top + 13, 0xCBC2D6);
		if (menu.pages() > 1) {
			graphics.drawCenteredString(font, text("page", menu.page() + 1, menu.pages()), width / 2, top + 145, 0xFFFFFF);
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, width, height, 0x90130E19);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	public void onClose() {
		if (ClientPlayNetworking.canSend(FountainTravelPackets.Close.TYPE)) {
			ClientPlayNetworking.send(new FountainTravelPackets.Close(menu.session()));
		}
		super.onClose();
	}

	private void select(int page, int row) {
		if (ClientPlayNetworking.canSend(FountainTravelPackets.Select.TYPE)) {
			ClientPlayNetworking.send(new FountainTravelPackets.Select(menu.session(), page, row));
		}
	}

	private Component direction(BlockPos destination) {
		int x = destination.getX() - menu.source().getX(), z = destination.getZ() - menu.source().getZ();
		if (x == 0 && z == 0) return Component.literal("—");
		if (Math.abs(x) >= Math.abs(z)) return text(x > 0 ? "east" : "west");
		return text(z > 0 ? "south" : "north");
	}

	private static String coords(BlockPos pos) { return pos.toShortString(); }

	private static Component text(String key, Object... args) {
		return Component.translatable("fountain.tears-of-ink." + key, args);
	}
}
