package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.patrol.PaleShieldbearerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class PaleShieldbearerRenderer extends MobRenderer<PaleShieldbearerEntity, PaleShieldbearerModel> {
	private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/pale_shieldbearer.png");

	public PaleShieldbearerRenderer(EntityRendererProvider.Context context) {
		super(context, new PaleShieldbearerModel(context.bakeLayer(PaleShieldbearerModel.LAYER)), 0.55f);
	}

	@Override
	public ResourceLocation getTextureLocation(PaleShieldbearerEntity entity) {
		return TEXTURE;
	}
}
