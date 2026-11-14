package com.tarkrakov.tearsofink.client.hunt;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.hunt.HunterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Текстура — по типу охотника: textures/entity/hunter/{spire,blood,ink}.png (одна развёртка). */
public class HunterRenderer extends HumanoidMobRenderer<HunterEntity, HunterModel> {
	public HunterRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new HunterModel(ctx.bakeLayer(HunterModel.LAYER)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(HunterEntity entity) {
		return TearsOfInk.id("textures/entity/hunter/" + entity.hunterType().id + ".png");
	}
}
