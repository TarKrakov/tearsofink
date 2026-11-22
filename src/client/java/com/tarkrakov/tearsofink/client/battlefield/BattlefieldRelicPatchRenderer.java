package com.tarkrakov.tearsofink.client.battlefield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.block.BattlefieldRelicPatchBlock;
import com.tarkrakov.tearsofink.block.BattlefieldRelicPatchBlockEntity;
import com.tarkrakov.tearsofink.block.BattlefieldWeaponMeshBlock;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldRelicRecipes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSoulShieldCatalog;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWeaponCatalog;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Batches small static author-mesh renders into one inert block entity per bed. */
public final class BattlefieldRelicPatchRenderer implements BlockEntityRenderer<BattlefieldRelicPatchBlockEntity> {
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

    @Override public boolean shouldRenderOffScreen(BattlefieldRelicPatchBlockEntity entity) {
        return true;
    }
    @Override public int getViewDistance() { return 112; }
}
