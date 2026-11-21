package com.tarkrakov.tearsofink.client.battlefield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.BattlefieldBannerBlockEntity;
import com.tarkrakov.tearsofink.block.BattlefieldBannerBlock;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldBannerGeometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** D-129: full authored regular-army standard, including vertical five-piece cloth.
 */
public final class BattlefieldBannerRenderer implements BlockEntityRenderer<BattlefieldBannerBlockEntity> {
    private static final ResourceLocation TEXTURE =
            TearsOfInk.id("textures/block/battlefield_regular_standart1.png");
    private static final ResourceLocation BARE_TEXTURE =
            TearsOfInk.id("textures/block/battlefield_regular_standart_withoutbanner1.png");
    private static final float UV_SIZE = BattlefieldBannerGeometry.UV_SIZE;

    public BattlefieldBannerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BattlefieldBannerBlockEntity banner, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (banner.getLevel() == null) return;
        boolean bare = banner.getBlockState().getValue(BattlefieldBannerBlock.BARE);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(bare ? BARE_TEXTURE : TEXTURE));
        stack.pushPose();
        stack.translate(0.5, 0, 0.5);
        stack.mulPose(Axis.YP.rotationDegrees(180f - banner.getBlockState()
                .getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        // Fixed pole / metal ornaments remain absolutely still.
        for (int index : BattlefieldBannerGeometry.POLE_PARTS) {
            drawBox(BattlefieldBannerGeometry.PARTS[index], stack.last(), consumer, packedLight, packedOverlay);
        }

        if (bare) {
            stack.popPose();
            return;
        }

        // The cloth hangs DOWN from the upper crossbar. Each descendant inherits
        // the previous segment's X bend around its actual TOP joint (not old sideways
        // JOINT_X or Y yaw). World time only drives client visuals; no server tick.
        double time = banner.getLevel().getGameTime() + partialTick;
        long hash = banner.getBlockPos().asLong();
        double positionPhase = ((hash ^ (hash >>> 32)) & 1023L) * (Math.PI * 2d / 1024d);
        double phase = time * (Math.PI * 2d / BattlefieldBannerGeometry.WIND_CYCLE_TICKS) + positionPhase;
        stack.pushPose();
        for (int i = 0; i < BattlefieldBannerGeometry.CLOTH_PARTS.length; i++) {
            float[] pivot = BattlefieldBannerGeometry.TOP_HINGES[i];
            // Same asymmetric, pole-safe curve as the server's placement sweep.
            float angle = BattlefieldBannerGeometry.windAngle(i, phase);
            stack.translate(pivot[0] / 16f, pivot[1] / 16f, pivot[2] / 16f);
            stack.mulPose(Axis.XP.rotation(angle));
            stack.translate(-pivot[0] / 16f, -pivot[1] / 16f, -pivot[2] / 16f);
            drawBox(BattlefieldBannerGeometry.PARTS[BattlefieldBannerGeometry.CLOTH_PARTS[i]],
                    stack.last(), consumer, packedLight, packedOverlay);
        }
        stack.popPose();
        stack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BattlefieldBannerBlockEntity banner) {
        // Authored pole reaches Y=98/16; waving cloth exceeds the anchor AABB.
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
