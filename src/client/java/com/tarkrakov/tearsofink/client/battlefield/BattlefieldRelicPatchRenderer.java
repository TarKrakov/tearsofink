package com.tarkrakov.tearsofink.client.battlefield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.BattlefieldRelicPatchBlock;
import com.tarkrakov.tearsofink.block.BattlefieldRelicPatchBlockEntity;
import com.tarkrakov.tearsofink.block.BattlefieldWeaponMeshBlock;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldRelicRecipes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSoulShieldCatalog;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWeaponCatalog;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Batches small static author-mesh renders into one inert block entity per bed. */
public final class BattlefieldRelicPatchRenderer implements BlockEntityRenderer<BattlefieldRelicPatchBlockEntity> {
    // The Pale Bolt already uses ArrowRenderer's 32x32 UV layout.
    // Reuse those atlases, not spawning individual arrow/bolt entities.
    private static final ResourceLocation BOLT_TEXTURE =
            TearsOfInk.id("textures/entity/projectiles/pale_bolt.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    private final BlockRenderDispatcher blocks;
    public BattlefieldRelicPatchRenderer(BlockEntityRendererProvider.Context context) {
        blocks = context.getBlockRenderDispatcher();
    }
    @Override public void render(BattlefieldRelicPatchBlockEntity entity, float partial,
                                 PoseStack stack, MultiBufferSource buffers, int light, int overlay) {
        var state = entity.getBlockState();
        var poses = entity.poses();
        var direction = state.getValue(HorizontalDirectionalBlock.FACING);
        stack.pushPose();
        stack.translate(.5d, 0, .5d);
        stack.mulPose(Axis.YP.rotationDegrees(180f - direction.toYRot()));
        for (int i = 0; i < poses.size(); i++) {
            int height = entity.offsetAt(i);
            if (height == BattlefieldRelicPatchBlockEntity.MISSING) continue;
            var p = poses.get(i);
            stack.pushPose();
            stack.translate(p.right(), height + p.sink(), -p.forward());
            stack.mulPose(Axis.YP.rotationDegrees(p.yaw()));
            stack.mulPose(Axis.ZP.rotationDegrees(p.roll()));
            stack.mulPose(Axis.XP.rotationDegrees(p.pitch()));
            if (p.projectile()) {
                drawProjectile(stack.last(), buffers, p.mesh() == 12, light, overlay);
                stack.popPose();
                continue;
            }
            if (p.shield()) {
                var shield = BattlefieldSoulShieldCatalog.Source.values()[p.mesh() - 8];
                stack.scale(BattlefieldSoulShieldCatalog.SCALE,
                        BattlefieldSoulShieldCatalog.SCALE, BattlefieldSoulShieldCatalog.SCALE);
                stack.translate(-shield.centerX() / 16f, -shield.minY / 16f, -shield.centerZ() / 16f);
            } else {
                var mesh = BattlefieldWeaponCatalog.Source.values()[p.mesh()];
                stack.mulPose(Axis.ZP.rotationDegrees(BattlefieldWeaponCatalog.TIP_DOWN_Z_DEGREES));
                stack.translate(-mesh.maxX / 16f, -mesh.maxY / 16f, -mesh.centerZ() / 16f);
            }
            blocks.renderSingleBlock(ModBlocks.BATTLEFIELD_WEAPON_MESH.defaultBlockState()
                    .setValue(BattlefieldWeaponMeshBlock.SOURCE, p.mesh()),
                    stack, buffers, light, overlay);
            stack.popPose();
        }
        stack.popPose();
    }

    /** Two crossed, double-sided shafts: a few quads, no ItemRenderer per shot. */
    private static void drawProjectile(PoseStack.Pose pose, MultiBufferSource buffers,
                                       boolean bolt, int light, int overlay) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(
                bolt ? BOLT_TEXTURE : ARROW_TEXTURE));
        float length = bolt ? .68f : .90f;
        float half = bolt ? .063f : .048f;
        for (int plane = 0; plane < 2; plane++) {
            float x = plane == 0 ? half : 0f;
            float z = plane == 0 ? 0f : half;
            // Cross-plane of the existing arrow entity atlas: shaft in the
            // upper 16x5 pixels, with the tip buried in the surface.
            quad(out, pose, light, overlay, -x,0,-z, x,0,z, x,length,z, -x,length,-z, !bolt);
            quad(out, pose, light, overlay, x,0,z, -x,0,-z, -x,length,-z, x,length,z, !bolt);
        }
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, int light, int overlay,
                             float ax,float ay,float az, float bx,float by,float bz,
                             float cx,float cy,float cz, float dx,float dy,float dz,
                             boolean invertArrowLength) {
        // Vanilla arrow's tip is at the opposite U end from the already
        // accepted Pale Bolt. Reverse only arrow UVs, never geometry or poses.
        // Both newly placed and existing saved projectile patches update.
        float footU = invertArrowLength ? .5f : 0f;
        float tailU = invertArrowLength ? 0f : .5f;
        vertex(out, pose, light, overlay, ax,ay,az,footU,.15625f);
        vertex(out, pose, light, overlay, bx,by,bz,footU,0f);
        vertex(out, pose, light, overlay, cx,cy,cz,tailU,0f);
        vertex(out, pose, light, overlay, dx,dy,dz,tailU,.15625f);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, int light, int overlay,
                               float x,float y,float z,float u,float v) {
        out.addVertex(pose, x,y,z).setColor(255,255,255,255).setUv(u,v)
                .setOverlay(overlay).setLight(light).setNormal(pose,0,1,0);
    }

    @Override public boolean shouldRenderOffScreen(BattlefieldRelicPatchBlockEntity entity) {
        return true;
    }
    @Override public int getViewDistance() { return 112; }
}
