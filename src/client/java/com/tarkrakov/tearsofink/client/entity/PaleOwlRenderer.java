package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.PaleOwlEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class PaleOwlRenderer extends MobRenderer<PaleOwlEntity, PaleOwlModel> {
    private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/pale_owl_texture.png");
    public PaleOwlRenderer(EntityRendererProvider.Context context) {
        super(context, new PaleOwlModel(context.bakeLayer(PaleOwlModel.LAYER)), 0.35f);
    }
    @Override public ResourceLocation getTextureLocation(PaleOwlEntity entity) { return TEXTURE; }
}
