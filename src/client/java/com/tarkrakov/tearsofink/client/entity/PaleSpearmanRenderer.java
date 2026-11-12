package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.spearman.PaleSpearmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class PaleSpearmanRenderer extends MobRenderer<PaleSpearmanEntity, PaleSpearmanModel> {
	private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/pale_spearman.png");

	public PaleSpearmanRenderer(EntityRendererProvider.Context context) {
		super(context, new PaleSpearmanModel(context.bakeLayer(PaleSpearmanModel.LAYER)), 0.4f);
	}

	/** Модель собрана лицом на юг (+Z), а игра ждёт лицом на север — разворачиваем на 180°. */
	@Override
	protected void setupRotations(PaleSpearmanEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
		super.setupRotations(entity, pose, bob, yBodyRot, partialTick, scale);
		pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
	}

	@Override
	public ResourceLocation getTextureLocation(PaleSpearmanEntity entity) {
		return TEXTURE;
	}
}
