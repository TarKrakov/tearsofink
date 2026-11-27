package com.tarkrakov.tearsofink.client.entity;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.MorokEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
public final class MorokRenderer extends MobRenderer<MorokEntity,MorokModel>{
    public MorokRenderer(EntityRendererProvider.Context c){super(c,new MorokModel(c.bakeLayer(MorokModel.LAYER)),0f);}
    @Override public ResourceLocation getTextureLocation(MorokEntity e){return TearsOfInk.id("textures/entity/morok_texture.png");}
    @Override protected int getBlockLightLevel(MorokEntity e,BlockPos p){return 15;}
}
