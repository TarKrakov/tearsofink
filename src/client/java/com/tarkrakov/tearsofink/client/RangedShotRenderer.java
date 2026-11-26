package com.tarkrakov.tearsofink.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.weapon.ranged.RangedProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.tarkrakov.tearsofink.item.ModItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Directional 3D bolts and knives; other ranged shot rendering is unchanged. */
public final class RangedShotRenderer extends ThrownItemRenderer<RangedProjectile> {
    private final ItemRenderer items;
    public RangedShotRenderer(EntityRendererProvider.Context context){super(context);items=context.getItemRenderer();}
    @Override public void render(RangedProjectile entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        // Only this mod's ranged projectile uses these bolt visuals. Ammo stays vanilla.
        if(entity.getItem().is(Items.ARROW)||entity.getItem().is(ModItems.INK_CROSSBOW_BOLT_EMPOWERED_VISUAL)){
            renderBolt(entity,pose,buffers,light);
            return;
        }
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
    private void renderBolt(RangedProjectile entity,PoseStack pose,MultiBufferSource buffers,int light){
        var v=entity.getDeltaMovement();
        Vector3f direction=new Vector3f((float)v.x,(float)v.y,(float)v.z);
        if(direction.lengthSquared()<1e-8f)direction.set(1,0,0);else direction.normalize();
        pose.pushPose();
        try{
            // Model is authored in crossbow coordinates, tip along local +X.
            pose.mulPose(new Quaternionf().rotationTo(new Vector3f(1,0,0),direction));
            // Centre of bolt: X=8.045882, shaft Y=12.125, Z=9.5.
            pose.translate(-.045882f/16f,-4.125f/16f,-1.5f/16f);
            var bolt=entity.getItem().is(ModItems.INK_CROSSBOW_BOLT_EMPOWERED_VISUAL)?ModItems.INK_CROSSBOW_BOLT_EMPOWERED_VISUAL:ModItems.INK_CROSSBOW_BOLT_VISUAL;
            items.renderStatic(new ItemStack(bolt),ItemDisplayContext.FIXED,
                    light,OverlayTexture.NO_OVERLAY,pose,buffers,entity.level(),entity.getId());
        }finally{pose.popPose();}
    }
}
