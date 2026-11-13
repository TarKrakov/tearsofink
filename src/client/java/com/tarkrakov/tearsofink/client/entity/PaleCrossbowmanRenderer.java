package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.patrol.PaleCrossbowmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class PaleCrossbowmanRenderer extends MobRenderer<PaleCrossbowmanEntity, PaleCrossbowmanModel> {
	private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/pale_crossbowman.png");

	public PaleCrossbowmanRenderer(EntityRendererProvider.Context context) {
		super(context, new PaleCrossbowmanModel(context.bakeLayer(PaleCrossbowmanModel.LAYER)), 0.45f);
	}

	@Override
	public ResourceLocation getTextureLocation(PaleCrossbowmanEntity entity) {
		return TEXTURE;
	}
}
