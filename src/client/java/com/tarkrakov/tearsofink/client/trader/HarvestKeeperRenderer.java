package com.tarkrakov.tearsofink.client.trader;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.trader.SoulTraderEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Отключённый Хранитель сохраняет прежнюю модель и совместимую развёртку. */
public class HarvestKeeperRenderer extends MobRenderer<SoulTraderEntity, HarvestKeeperModel> {
	private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/harvest_keeper.png");
	private static final RenderType EYES = RenderType.eyes(TearsOfInk.id("textures/entity/harvest_keeper_eyes.png"));

	public HarvestKeeperRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new HarvestKeeperModel(ctx.bakeLayer(HarvestKeeperModel.LAYER)), 0.5f);
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
