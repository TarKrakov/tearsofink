package com.tarkrakov.tearsofink.client.battlefield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.block.BattlefieldMonumentBlock;
import com.tarkrakov.tearsofink.block.BattlefieldMonumentBlockEntity;
import com.tarkrakov.tearsofink.block.BattlefieldWeaponMeshBlock;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldMonumentGeometry;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWeaponCatalog;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Rare titan-scale sword. Uses the EXACT existing baked item mesh and aged battlefield
 * texture; source item, UVs and gameplay stats are never changed.
 */
public final class BattlefieldMonumentRenderer implements BlockEntityRenderer<BattlefieldMonumentBlockEntity> {
    private final BlockRenderDispatcher blocks;
    public BattlefieldMonumentRenderer(BlockEntityRendererProvider.Context context) {
        blocks = context.getBlockRenderDispatcher();
    }
    @Override
    public void render(BattlefieldMonumentBlockEntity entity, float partialTick, PoseStack stack,
                       MultiBufferSource buffers, int light, int overlay) {
        var state = entity.getBlockState();
        var blade = BattlefieldMonumentGeometry.Blade.values()[state.getValue(BattlefieldMonumentBlock.BLADE)];
        var style = BattlefieldMonumentGeometry.style(state.getValue(BattlefieldMonumentBlock.VARIANT));
        var mesh = blade.source;
        var facing = state.getValue(HorizontalDirectionalBlock.FACING);
        stack.pushPose();
        stack.translate(.5d, -style.burial(), .5d);
        stack.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot() + style.yaw()));
        stack.mulPose(Axis.ZP.rotationDegrees(style.tilt()));
        stack.scale(style.scale(), style.scale(), style.scale());
        stack.mulPose(Axis.ZP.rotationDegrees(BattlefieldWeaponCatalog.TIP_DOWN_Z_DEGREES));
        stack.translate(-mesh.maxX / 16f, -mesh.maxY / 16f, -mesh.centerZ() / 16f);
        blocks.renderSingleBlock(ModBlocks.BATTLEFIELD_WEAPON_MESH.defaultBlockState()
                .setValue(BattlefieldWeaponMeshBlock.SOURCE, mesh.ordinal()), stack, buffers, light, overlay);
        stack.popPose();

        if (state.getValue(BattlefieldMonumentBlock.TABLEAU)) {
            var relics = BattlefieldMonumentGeometry.tableau(blade, state.getValue(BattlefieldMonumentBlock.VARIANT));
            stack.pushPose();
            stack.translate(.5d, 0, .5d);
            stack.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
            for (int i = 0; i < relics.length; i++) {
                var relic = relics[i];
                int height = entity.offsetAt(i);
                if (height == BattlefieldMonumentBlockEntity.MISSING) continue;
                var small = relic.source();
                stack.pushPose();
                stack.translate(relic.right(), height + relic.sink(), -relic.forward());
                stack.mulPose(Axis.YP.rotationDegrees(relic.yaw()));
                stack.mulPose(Axis.ZP.rotationDegrees(relic.roll()));
                stack.mulPose(Axis.XP.rotationDegrees(relic.pitch()));
                stack.mulPose(Axis.ZP.rotationDegrees(BattlefieldWeaponCatalog.TIP_DOWN_Z_DEGREES));
                stack.translate(-small.maxX / 16f, -small.maxY / 16f, -small.centerZ() / 16f);
                blocks.renderSingleBlock(ModBlocks.BATTLEFIELD_WEAPON_MESH.defaultBlockState()
                        .setValue(BattlefieldWeaponMeshBlock.SOURCE, small.ordinal()),
                        stack, buffers, light, overlay);
                stack.popPose();
            }
            stack.popPose();
        }
    }
    @Override public boolean shouldRenderOffScreen(BattlefieldMonumentBlockEntity entity) {
        // Single anchor, mesh up to ~14 blocks above it; avoid vanilla 1-block culling.
        return true;
    }
    @Override public int getViewDistance() {
        return 160;
    }
}
