package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.CaterpillarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Рисует гусеницу: текстура и размер — по варианту. */
public class CaterpillarRenderer extends MobRenderer<CaterpillarEntity, CaterpillarModel> {
	public CaterpillarRenderer(EntityRendererProvider.Context context) {
		super(context, new CaterpillarModel(context.bakeLayer(CaterpillarModel.LAYER)), 0.25f);
	}

	@Override
	public ResourceLocation getTextureLocation(CaterpillarEntity entity) {
		return TearsOfInk.id("textures/entity/caterpillar/" + entity.variant().name + ".png");
	}

	@Override
	protected void scale(CaterpillarEntity entity, PoseStack poseStack, float partialTick) {
		float s = entity.variant().scale;
		poseStack.scale(s, s, s);
	}
}
