package com.tarkrakov.tearsofink.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.weapon.ranged.RangedProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 3D knife in flight, preserving other ranged shot rendering. */
public final class RangedShotRenderer extends ThrownItemRenderer<RangedProjectile> {
    private final ItemRenderer items;
    public RangedShotRenderer(EntityRendererProvider.Context context){super(context);items=context.getItemRenderer();}
    @Override public void render(RangedProjectile entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(!KnifeAnims.isKnife(entity.getItem())){super.render(entity,yaw,partial,pose,buffers,light);return;}
        var v=entity.getDeltaMovement();
        Vector3f direction=new Vector3f((float)v.x,(float)v.y,(float)v.z);
        if(direction.lengthSquared()<1e-8f)direction.set(0,1,0);else direction.normalize();
        pose.pushPose();
        try{
            pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0),direction));
            pose.mulPose(new Quaternionf().rotationY((entity.tickCount+partial)*.8f));
            pose.scale(.8f,.8f,.8f);
            pose.translate(1f/16f,0,1f/16f);
            items.renderStatic(entity.getItem(),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,entity.level(),entity.getId());
        }finally{pose.popPose();}
    }
}
