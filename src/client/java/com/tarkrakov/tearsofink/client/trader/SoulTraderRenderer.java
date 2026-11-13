package com.tarkrakov.tearsofink.client.trader;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.trader.SoulTraderEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Торговка душами: entity/soul_trader.png + светящиеся глаза entity/soul_trader_eyes.png (та же развёртка). */
public class SoulTraderRenderer extends MobRenderer<SoulTraderEntity, SoulTraderModel> {
	private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/soul_trader.png");
	private static final RenderType EYES = RenderType.eyes(TearsOfInk.id("textures/entity/soul_trader_eyes.png"));

	public SoulTraderRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new SoulTraderModel(ctx.bakeLayer(SoulTraderModel.LAYER)), 0.5f);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return EYES;
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(SoulTraderEntity entity) {
		return TEXTURE;
	}
}
