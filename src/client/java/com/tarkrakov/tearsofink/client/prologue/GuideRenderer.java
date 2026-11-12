package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.prologue.GuideEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Г-н Проводник: entity/guide.png + светящиеся глаза entity/guide_eyes.png (та же развёртка, всё прозрачное кроме глаз). */
public class GuideRenderer extends MobRenderer<GuideEntity, GuideModel> {
	private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/guide.png");
	private static final RenderType EYES = RenderType.eyes(TearsOfInk.id("textures/entity/guide_eyes.png"));

	public GuideRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new GuideModel(ctx.bakeLayer(GuideModel.LAYER)), 0.6f);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return EYES;
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(GuideEntity entity) {
		return TEXTURE;
	}
}
