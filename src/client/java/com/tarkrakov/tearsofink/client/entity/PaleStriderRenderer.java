package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.PaleStriderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class PaleStriderRenderer extends MobRenderer<PaleStriderEntity, PaleStriderModel> {
    private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/pale_strider_texture.png");
    public PaleStriderRenderer(EntityRendererProvider.Context context) {
        super(context, new PaleStriderModel(context.bakeLayer(PaleStriderModel.LAYER)), 0.45f);
    }
    @Override public ResourceLocation getTextureLocation(PaleStriderEntity entity) { return TEXTURE; }
}
