package com.tarkrakov.tearsofink.client.battlefield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.BattlefieldBannerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Author's first_flag.bbmodel, 7 original cuboids and per-face UVs, no art changes.
 *
 * The authored intermediate cloth pivots are at the Blockbench scene origin.
 * Use each piece's actual connected seam instead; the six nested transforms
 * preserve the undisturbed authored silhouette and generate a gentle traveling wave.
 * All motion is visual, deterministic, client-only and server-tick-free.
 */
public final class BattlefieldBannerRenderer implements BlockEntityRenderer<BattlefieldBannerBlockEntity> {
    private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/block/first_flag_texture.png");
    // The Blockbench project's logical UV size is 64x64; the authored PNG is 128x128.
    private static final float UV_SIZE = 64f;
    // Source cuboids exactly as in blockbench/first_flag.bbmodel, pole + 6 cloth pieces.
    private static final Box[] PARTS = {
    // pole
    new Box(-1.0f, 0.0f, -1.0f, 1.0f, 64.0f, 1.0f, new float[][] {
        {14.0f, 0.0f, 16.0f, 64.0f},
        {16.0f, 0.0f, 18.0f, 64.0f},
        {18.0f, 0.0f, 20.0f, 64.0f},
        {20.0f, 0.0f, 22.0f, 64.0f},
        {36.0f, 19.0f, 38.0f, 21.0f},
        {28.0f, 36.0f, 30.0f, 38.0f}
    }),
    // cloth_root
    new Box(-8.0f, 40.0f, 0.0f, -1.0f, 63.0f, 1.0f, new float[][] {
        {0.0f, 0.0f, 7.0f, 23.0f},
        {10.0f, 23.0f, 11.0f, 46.0f},
        {7.0f, 0.0f, 14.0f, 23.0f},
        {11.0f, 23.0f, 12.0f, 46.0f},
        {34.0f, 23.0f, 41.0f, 24.0f},
        {34.0f, 24.0f, 41.0f, 25.0f}
    }),
    // cloth1
    new Box(-13.0f, 42.0f, 0.0f, -8.0f, 61.0f, 1.0f, new float[][] {
        {0.0f, 23.0f, 5.0f, 42.0f},
        {12.0f, 23.0f, 13.0f, 42.0f},
        {5.0f, 23.0f, 10.0f, 42.0f},
        {13.0f, 23.0f, 14.0f, 42.0f},
        {34.0f, 35.0f, 39.0f, 36.0f},
        {36.0f, 14.0f, 41.0f, 15.0f}
    }),
    // cloth2
    new Box(-19.0f, 43.0f, 0.0f, -13.0f, 60.0f, 1.0f, new float[][] {
        {22.0f, 0.0f, 28.0f, 17.0f},
        {22.0f, 34.0f, 23.0f, 51.0f},
        {22.0f, 17.0f, 28.0f, 34.0f},
        {23.0f, 34.0f, 24.0f, 51.0f},
        {34.0f, 25.0f, 40.0f, 26.0f},
        {35.0f, 21.0f, 41.0f, 22.0f}
    }),
    // cloth3
    new Box(-25.0f, 45.0f, 0.0f, -19.0f, 58.0f, 1.0f, new float[][] {
        {28.0f, 0.0f, 34.0f, 13.0f},
        {24.0f, 34.0f, 25.0f, 47.0f},
        {28.0f, 13.0f, 34.0f, 26.0f},
        {25.0f, 34.0f, 26.0f, 47.0f},
        {35.0f, 22.0f, 41.0f, 23.0f},
        {28.0f, 35.0f, 34.0f, 36.0f}
    }),
    // cloth4
    new Box(-30.0f, 47.0f, 0.0f, -25.0f, 56.0f, 1.0f, new float[][] {
        {28.0f, 26.0f, 33.0f, 35.0f},
        {34.0f, 14.0f, 35.0f, 23.0f},
        {33.0f, 26.0f, 38.0f, 35.0f},
        {26.0f, 34.0f, 27.0f, 43.0f},
        {36.0f, 15.0f, 41.0f, 16.0f},
        {36.0f, 16.0f, 41.0f, 17.0f}
    }),
    // cloth5
    new Box(-35.0f, 48.0f, 0.0f, -30.0f, 55.0f, 1.0f, new float[][] {
        {34.0f, 0.0f, 39.0f, 7.0f},
        {27.0f, 34.0f, 28.0f, 41.0f},
        {34.0f, 7.0f, 39.0f, 14.0f},
        {35.0f, 14.0f, 36.0f, 21.0f},
        {36.0f, 17.0f, 41.0f, 18.0f},
        {36.0f, 18.0f, 41.0f, 19.0f}
    })
    };
    // Cloth mesh seams (in Blockbench pixels) are -1, -8, -13, -19, -25, -30.
    private static final float[] JOINT_X = {-1f, -8f, -13f, -19f, -25f, -30f};
    private static final float[] AMPLITUDE = {0.018f, 0.023f, 0.029f, 0.034f, 0.040f, 0.046f};

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

        // The full 64-pixel pole remains completely stationary.
        drawBox(PARTS[0], stack.last(), consumer, packedLight, packedOverlay);

        double time = banner.getLevel().getGameTime() + partialTick;
        long hash = banner.getBlockPos().asLong();
        float positionPhase = (float) (((hash ^ (hash >>> 32)) & 1023L) * (Math.PI * 2d / 1024d));

        stack.pushPose();
        for (int i = 0; i < JOINT_X.length; i++) {
            // A nested pivot transform: all downstream pieces inherit upstream bending.
            float x = JOINT_X[i] / 16f;
            float y = 51f / 16f;
            float z = 1f / 16f;
            float angle = (float) Math.sin(time * (Math.PI * 2d / 75d) - i * 0.65d + positionPhase) * AMPLITUDE[i];
            stack.translate(x, y, z);
            stack.mulPose(Axis.YP.rotation(angle));
            stack.translate(-x, -y, -z);
            drawBox(PARTS[i + 1], stack.last(), consumer, packedLight, packedOverlay);
        }
        stack.popPose();
        stack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BattlefieldBannerBlockEntity banner) {
        // Four-block-high pole and >2-block cloth extend well beyond the base block AABB.
        return true;
    }

    private record Box(float x0, float y0, float z0, float x1, float y1, float z1, float[][] uv) {}

    private static void drawBox(Box box, PoseStack.Pose pose, VertexConsumer out, int light, int overlay) {
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
