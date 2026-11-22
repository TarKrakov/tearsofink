package com.tarkrakov.tearsofink.client.battlefield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.BattlefieldBannerBlockEntity;
import com.tarkrakov.tearsofink.block.BattlefieldBannerBlock;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldBannerGeometry;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldFactionBannerGeometry;
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
    private static final ResourceLocation SOUL_TEXTURE =
            TearsOfInk.id("textures/block/battlefield_soul_flag1.png");
    private static final ResourceLocation HUNTER_TEXTURE =
            TearsOfInk.id("textures/block/battlefield_hunter_flag1.png");

    public BattlefieldBannerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BattlefieldBannerBlockEntity banner, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (banner.getLevel() == null) return;
        boolean bare = banner.getBlockState().getValue(BattlefieldBannerBlock.BARE);
        var faction = banner.getBlockState().getValue(BattlefieldBannerBlock.FACTION);
        if (faction != BattlefieldBannerBlock.Faction.REGULAR) {
            renderFaction(banner, partialTick, stack, buffers, packedLight, packedOverlay, faction, bare);
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(bare ? BARE_TEXTURE : TEXTURE));
        stack.pushPose();
        stack.translate(0.5, 0, 0.5);
        stack.mulPose(Axis.YP.rotationDegrees(180f - banner.getBlockState()
                .getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        // Transform the ENTIRE original pole rig around its planted foot.
        // Saved banners have lean=0, so all existing animation stays identical.
        int lean = banner.getBlockState().getValue(BattlefieldBannerBlock.LEAN);
        if (bare && lean != 0) {
            stack.mulPose(Axis.ZP.rotationDegrees(BattlefieldBannerBlock.leanZ(lean)));
            stack.mulPose(Axis.XP.rotationDegrees(BattlefieldBannerBlock.leanX(lean)));
        }
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

    /** Uses real Blockbench joint ancestry: separate split cloth tails, rigid pole. */
    private static void renderFaction(BattlefieldBannerBlockEntity banner, float partialTick,
                                      PoseStack stack, MultiBufferSource buffers,
                                      int packedLight, int packedOverlay,
                                      BattlefieldBannerBlock.Faction faction, boolean bare) {
        var model = BattlefieldFactionBannerGeometry.model(faction, bare);
        var texture = faction == BattlefieldBannerBlock.Faction.SOUL ? SOUL_TEXTURE : HUNTER_TEXTURE;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        double time = banner.getLevel().getGameTime() + partialTick;
        long hash = banner.getBlockPos().asLong();
        double positionPhase = ((hash ^ (hash >>> 32)) & 1023L) * (Math.PI * 2d / 1024d);
        double phase = time * (Math.PI * 2d / BattlefieldFactionBannerGeometry.WIND_CYCLE_TICKS) + positionPhase;
        stack.pushPose();
        stack.translate(.5, 0, .5);
        stack.mulPose(Axis.YP.rotationDegrees(180f - banner.getBlockState()
                .getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        // Transform the ENTIRE original pole rig around its planted foot.
        // Saved banners have lean=0, so all existing animation stays identical.
        int lean = banner.getBlockState().getValue(BattlefieldBannerBlock.LEAN);
        if (bare && lean != 0) {
            stack.mulPose(Axis.ZP.rotationDegrees(BattlefieldBannerBlock.leanZ(lean)));
            stack.mulPose(Axis.XP.rotationDegrees(BattlefieldBannerBlock.leanX(lean)));
        }
        for (var part : model.parts()) {
            stack.pushPose();
            for (int index : part.joints()) {
                var j = model.joints()[index];
                stack.translate(j.x()/16f, j.y()/16f, j.z()/16f);
                stack.mulPose(Axis.YP.rotation(BattlefieldFactionBannerGeometry.windYaw(j, phase)));
                stack.mulPose(Axis.ZP.rotation(BattlefieldFactionBannerGeometry.windRoll(j, phase)));
                stack.translate(-j.x()/16f, -j.y()/16f, -j.z()/16f);
            }
            drawBox(part.cube(), stack.last(), consumer, packedLight, packedOverlay, model.uvSize());
            stack.popPose();
        }
        stack.popPose();
    }

    /**
     * Static buried standards: called only by the existing inert relic-patch
     * renderer. Uses the SAME model cubes, textures and UV as normal BARE
     * banners, without an extra BE, animation or client-side joint updates.
     * PoseStack already has the ground contact, burying, yaw and both tilts.
     */
    public static void renderBareRelic(int mesh, PoseStack stack,
                                       MultiBufferSource buffers, int light, int overlay) {
        if (mesh == 14) {
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(BARE_TEXTURE));
            for (int index : BattlefieldBannerGeometry.POLE_PARTS) {
                drawBox(BattlefieldBannerGeometry.PARTS[index], stack.last(), consumer, light, overlay);
            }
            return;
        }
        BattlefieldBannerBlock.Faction faction = switch (mesh) {
            case 15 -> BattlefieldBannerBlock.Faction.SOUL;
            case 16 -> BattlefieldBannerBlock.Faction.HUNTER;
            default -> throw new IllegalArgumentException("Not a bare standard mesh: " + mesh);
        };
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(
                faction == BattlefieldBannerBlock.Faction.SOUL ? SOUL_TEXTURE : HUNTER_TEXTURE));
        var model = BattlefieldFactionBannerGeometry.model(faction, true);
        for (var part : model.parts()) {
            drawBox(part.cube(), stack.last(), consumer, light, overlay, model.uvSize());
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BattlefieldBannerBlockEntity banner) {
        // Authored pole reaches Y=98/16; waving cloth exceeds the anchor AABB.
        return true;
    }

    private static void drawBox(BattlefieldBannerGeometry.Cuboid box, PoseStack.Pose pose, VertexConsumer out, int light, int overlay) {
        drawBox(box, pose, out, light, overlay, UV_SIZE);
    }
    private static void drawBox(BattlefieldBannerGeometry.Cuboid box, PoseStack.Pose pose,
                                VertexConsumer out, int light, int overlay, float uvSize) {
        float x0 = box.x0() / 16f, y0 = box.y0() / 16f, z0 = box.z0() / 16f;
        float x1 = box.x1() / 16f, y1 = box.y1() / 16f, z1 = box.z1() / 16f;
        float[][] u = box.uv();
        // Per-face UVs are read, without repacking, from the author's actual PNG atlas.
        face(out, pose, light, overlay, uvSize, u[0], 0, 0, -1, x1,y0,z0, x1,y1,z0, x0,y1,z0, x0,y0,z0); // north
        face(out, pose, light, overlay, uvSize, u[1], 1, 0, 0, x1,y0,z1, x1,y1,z1, x1,y1,z0, x1,y0,z0); // east
        face(out, pose, light, overlay, uvSize, u[2], 0, 0, 1, x0,y0,z1, x0,y1,z1, x1,y1,z1, x1,y0,z1); // south
        face(out, pose, light, overlay, uvSize, u[3], -1, 0, 0, x0,y0,z0, x0,y1,z0, x0,y1,z1, x0,y0,z1); // west
        face(out, pose, light, overlay, uvSize, u[4], 0, 1, 0, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1); // up
        face(out, pose, light, overlay, uvSize, u[5], 0, -1, 0, x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0); // down
    }

    private static void face(VertexConsumer out, PoseStack.Pose pose, int light, int overlay,
                             float uvSize, float[] uv, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        float u0 = uv[0] / uvSize, v0 = uv[1] / uvSize;
        float u1 = uv[2] / uvSize, v1 = uv[3] / uvSize;
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
