package com.tarkrakov.tearsofink.client.entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.TrollEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class TrollRenderer extends MobRenderer<TrollEntity,TrollModel>{
    public TrollRenderer(EntityRendererProvider.Context c){super(c,new TrollModel(c.bakeLayer(TrollModel.LAYER)),1.1f);}
    @Override public ResourceLocation getTextureLocation(TrollEntity e){return TearsOfInk.id("textures/entity/troll_texture.png");}
    @Override protected void scale(TrollEntity e,PoseStack stack,float partial){stack.scale(TrollEntity.SCALE,TrollEntity.SCALE,TrollEntity.SCALE);}
    @Override protected float getFlipDegrees(TrollEntity e){return 0;}
}
