package com.tarkrakov.tearsofink.client.entity.orion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.vow.AuraClient;
import com.tarkrakov.tearsofink.entity.orion.OrionCombat;
import com.tarkrakov.tearsofink.entity.orion.OrionCombat.Clip;
import com.tarkrakov.tearsofink.entity.orion.OrionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class OrionRenderer extends MobRenderer<OrionEntity, OrionModel> {
    private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/orion_texture.png");

    public OrionRenderer(EntityRendererProvider.Context context) {
        super(context, new OrionModel(context.bakeLayer(OrionModel.LAYER)), .64f);
        addLayer(new RenderLayer<>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, OrionEntity orion,
                    float limbSwing, float limbAmount, float partial, float age, float headYaw, float headPitch) {
                var state = orion.performance();
                float time = state.seconds(orion.level().getGameTime(), partial);
                if (state.clip() == Clip.DEATH) {
                    float white = Mth.clamp((time - .6f) / 4.8f, 0, 1);
                    AuraClient.renderSoulLight(getParentModel(), pose, buffers, 3, .55f + white * 1.4f);
                    AuraClient.renderWhiteLight(getParentModel(), pose, buffers, white * white);
                } else if (state.phaseTwo()) {
                    AuraClient.renderSoulLight(getParentModel(), pose, buffers, 3, 1);
                } else if (state.clip() == Clip.EMPOWER) {
                    AuraClient.renderSoulLight(getParentModel(), pose, buffers, 3, Mth.clamp(time / 1.6f, 0, 1));
                }
            }
        });
    }

    @Override protected void scale(OrionEntity orion, PoseStack pose, float partial) {
        pose.scale(OrionCombat.SCALE, OrionCombat.SCALE, OrionCombat.SCALE);
    }

    @Override public ResourceLocation getTextureLocation(OrionEntity orion) { return TEXTURE; }
}
