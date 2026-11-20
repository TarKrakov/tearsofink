package com.tarkrakov.tearsofink.client.shield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.weapon.shield.ShieldPhantomEntity;
import com.tarkrakov.tearsofink.weapon.shield.ShieldTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import java.util.ArrayList;

/** Reuse actual shield mesh; luminous gold-white translucent faces, no new artwork. */
public final class ShieldPhantomRenderer extends EntityRenderer<ShieldPhantomEntity> {
    public ShieldPhantomRenderer(EntityRendererProvider.Context context) { super(context); }
    private static final ResourceLocation LIGHT_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    @Override public ResourceLocation getTextureLocation(ShieldPhantomEntity entity) { return LIGHT_TEXTURE; }
    @Override public void render(ShieldPhantomEntity e, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        if (e.shield().isEmpty() || e.flightAge() >= e.flightTicks() || e.flightAge() >= ShieldTuning.PHANTOM_MAX_TICKS) return;
        var model = Minecraft.getInstance().getItemRenderer().getModel(e.shield(), e.level(), null, 0);
        var quads = new ArrayList<BakedQuad>();
        for (Direction side : Direction.values()) quads.addAll(model.getQuads(null, side, RandomSource.create(42)));
        quads.addAll(model.getQuads(null, null, RandomSource.create(42)));
        if (quads.isEmpty()) return;
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,minZ=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE,maxZ=-Float.MAX_VALUE;
        for (BakedQuad q : quads) {
            int[] data=q.getVertices(); int stride=data.length/4;
            for(int i=0;i<4;i++) { int o=i*stride;
                float x=Float.intBitsToFloat(data[o]), y=Float.intBitsToFloat(data[o+1]), z=Float.intBitsToFloat(data[o+2]);
                minX=Math.min(minX,x); maxX=Math.max(maxX,x); minY=Math.min(minY,y); maxY=Math.max(maxY,y); minZ=Math.min(minZ,z); maxZ=Math.max(maxZ,z);
            }
        }
        poses.pushPose();
        var offset = e.flightOffset(partial);
        poses.translate(offset.x, offset.y, offset.z);
        poses.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        poses.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        float scale=ShieldTuning.PHANTOM_SIZE/Math.max(.01f,Math.max(maxX-minX,maxY-minY));
        poses.scale(scale,scale,scale);
        poses.translate(-(minX+maxX)/2f,-(minY+maxY)/2f,-(minZ+maxZ)/2f);
        VertexConsumer out=buffers.getBuffer(RenderType.entityTranslucent(LIGHT_TEXTURE));
        int alpha=Math.max(45,170-(int)(100f*(e.flightAge()+partial)/e.flightTicks()));
        for(BakedQuad q:quads) {
            int[] data=q.getVertices(); int stride=data.length/4;
            Direction n=q.getDirection();
            for(int i=0;i<4;i++) {int o=i*stride;
                out.addVertex(poses.last(), Float.intBitsToFloat(data[o]),Float.intBitsToFloat(data[o+1]),Float.intBitsToFloat(data[o+2]))
                    .setColor((alpha<<24)|0xFFF1B8)
                    .setUv(0.5f,0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(poses.last(),n.getStepX(),n.getStepY(),n.getStepZ());
            }
        }
        poses.popPose();
        super.render(e,yaw,partial,poses,buffers,light);
    }
}
