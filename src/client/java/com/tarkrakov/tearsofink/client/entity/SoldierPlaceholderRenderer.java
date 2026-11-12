package com.tarkrakov.tearsofink.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.patrol.PaleSoldier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Рендер фигурки-заглушки (щитовик крупнее). */
public class SoldierPlaceholderRenderer<T extends PaleSoldier> extends MobRenderer<T, SoldierPlaceholderModel<T>> {
	private final ResourceLocation texture;
	private final float scale;

	public SoldierPlaceholderRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, String texture, float scale) {
		super(context, new SoldierPlaceholderModel<>(context.bakeLayer(layer)), 0.45f * scale);
		this.texture = TearsOfInk.id("textures/entity/" + texture + ".png");
		this.scale = scale;
	}

	@Override
	protected void scale(T entity, PoseStack pose, float partialTick) {
		pose.scale(scale, scale, scale);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return texture;
	}
}
