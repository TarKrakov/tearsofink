package com.tarkrakov.tearsofink.client.battlefield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.BattlefieldBannerBlockEntity;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldBannerGeometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** D-128: author's regular-army standard without cloth, replacing rejected first_flag.
 * A later vertically hanging cloth requires its own top-mounted hinge transforms.
 */
public final class BattlefieldBannerRenderer implements BlockEntityRenderer<BattlefieldBannerBlockEntity> {
    private static final ResourceLocation TEXTURE =
            TearsOfInk.id("textures/block/battlefield_regular_standart_withoutbanner1.png");
    private static final float UV_SIZE = BattlefieldBannerGeometry.UV_SIZE;

    public BattlefieldBannerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BattlefieldBannerBlockEntity banner, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (banner.getLevel() == null) return;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        stack.pushPose();
        stack.translate(0.5, 0, 0.5);
        stack.mulPose(Axis.YP.rotationDegrees(180f - banner.getBlockState()
                .getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        for (BattlefieldBannerGeometry.Cuboid box : BattlefieldBannerGeometry.PARTS) {
            drawBox(box, stack.last(), consumer, packedLight, packedOverlay);
        }
        stack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BattlefieldBannerBlockEntity banner) {
        // Authored tall pole reaches Y=98/16 (>6 blocks).
        return true;
    }

    private static void drawBox(BattlefieldBannerGeometry.Cuboid box, PoseStack.Pose pose, VertexConsumer out, int light, int overlay) {
        float x0 = box.x0() / 16f, y0 = box.y0() / 16f, z0 = box.z0() / 16f;
        float x1 = box.x1() / 16f, y1 = box.y1() / 16f, z1 = box.z1() / 16f;
        float[][] u = box.uv();
        // Per-face UVs are read, without repacking, from the author's actual PNG atlas.
        face(out, pose, light, overlay, u[0], 0, 0, -1, x1,y0,z0, x1,y1,z0, x0,y1,z0, x0,y0,z0); // north
        face(out, pose, light, overlay, u[1], 1, 0, 0, x1,y0,z1, x1,y1,z1, x1,y1,z0, x1,y0,z0); // east
        face(out, pose, light, overlay, u[2], 0, 0, 1, x0,y0,z1, x0,y1,z1, x1,y1,z1, x1,y0,z1); // south
        face(out, pose, light, overlay, u[3], -1, 0, 0, x0,y0,z0, x0,y1,z0, x0,y1,z1, x0,y0,z1); // west
        face(out, pose, light, overlay, u[4], 0, 1, 0, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1); // up
        face(out, pose, light, overlay, u[5], 0, -1, 0, x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0); // down
    }

    private static void face(VertexConsumer out, PoseStack.Pose pose, int light, int overlay,
                             float[] uv, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        float u0 = uv[0] / UV_SIZE, v0 = uv[1] / UV_SIZE;
        float u1 = uv[2] / UV_SIZE, v1 = uv[3] / UV_SIZE;
        vertex(out, pose, light, overlay, ax, ay, az, u0, v1, nx, ny, nz);
        vertex(out, pose, light, overlay, bx, by, bz, u0, v0, nx, ny, nz);
        vertex(out, pose, light, overlay, cx, cy, cz, u1, v0, nx, ny, nz);
        vertex(out, pose, light, overlay, dx, dy, dz, u1, v1, nx, ny, nz);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, int light, int overlay,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        out.addVertex(pose, x, y, z).setColor(255, 255, 255, 255)
                .setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
