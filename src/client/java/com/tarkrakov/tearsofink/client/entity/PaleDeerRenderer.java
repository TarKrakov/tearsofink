package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.PaleDeerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class PaleDeerRenderer extends MobRenderer<PaleDeerEntity, PaleDeerModel> {
    private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/pale_deer_texture.png");
    public PaleDeerRenderer(EntityRendererProvider.Context context) {
        super(context, new PaleDeerModel(context.bakeLayer(PaleDeerModel.LAYER)), 0.65f);
    }
    @Override public ResourceLocation getTextureLocation(PaleDeerEntity entity) { return TEXTURE; }
}
