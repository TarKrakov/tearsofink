package com.tarkrakov.tearsofink.client.battlefield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.block.BattlefieldSceneBlock;
import com.tarkrakov.tearsofink.block.BattlefieldSceneBlockEntity;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Pose;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Scenario;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWeaponCatalog;
import com.tarkrakov.tearsofink.block.BattlefieldWeaponMeshBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * One renderer for ONE small test scene, not a renderer per spear or shield.
 * Renders the EXISTING author-derived JSON block models through Minecraft's
 * baked renderer. Tilt/sink/pivot/yaw are independent and are not limited to
 * 90/45-degree blockstate angles. No retexture, duplicated mesh or server tick.
 */
public final class BattlefieldSceneRenderer implements BlockEntityRenderer<BattlefieldSceneBlockEntity> {
    private static final Scenario[] SCENARIOS = Scenario.values();
    private final BlockRenderDispatcher blocks;

    public BattlefieldSceneRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(BattlefieldSceneBlockEntity entity, float partialTick, PoseStack stack,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState root = entity.getBlockState();
        int sceneIndex = root.getValue(BattlefieldSceneBlock.SCENE);
        if (sceneIndex < 0 || sceneIndex >= SCENARIOS.length) return;
        List<Pose> poses = BattlefieldPosedScenes.plan(SCENARIOS[sceneIndex],
                root.getValue(BattlefieldSceneBlock.VARIANT));

        // Align the entire episode to the player's forward direction.
        Direction facing = root.getValue(HorizontalDirectionalBlock.FACING);
        stack.pushPose();
        stack.translate(0.5, 0, 0.5);
        stack.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
        stack.translate(-0.5, 0, -0.5);
        for (int i = 0; i < poses.size(); i++) {
            Pose p = poses.get(i);
            if (p.banner() || entity.offsetAt(i) == BattlefieldSceneBlockEntity.MISSING) continue;
            stack.pushPose();
            stack.translate(0.5 + p.right(), entity.offsetAt(i) + p.sink(), 0.5 - p.forward());
            // Each physical prop is its own local scene pivot, not a cardinal
            // blockstate rotation. Author's item geometry is baked, not remade.
            stack.mulPose(Axis.YP.rotationDegrees(p.yaw()));
            stack.mulPose(Axis.ZP.rotationDegrees(p.roll()));
            stack.mulPose(Axis.XP.rotationDegrees(p.pitch()));

            if (BattlefieldWeaponCatalog.isWeapon(p.prop())) {
                BattlefieldWeaponCatalog.Source mesh = BattlefieldWeaponCatalog.fromProp(p.prop());
                // Verified from all eight authored meshes: grip at low X+Y,
                // blade tip / axe head at HIGH X+Y. Pivot on the TIP, not the
                // handle. A -135° Z correction puts the hilt above the ground
                // for embedded weapons, or beside the blade for fallen ones.
                stack.mulPose(Axis.ZP.rotationDegrees(BattlefieldWeaponCatalog.TIP_DOWN_Z_DEGREES));
                stack.translate(-mesh.maxX / 16f, -mesh.maxY / 16f, -mesh.centerZ() / 16f);
                blocks.renderSingleBlock(ModBlocks.BATTLEFIELD_WEAPON_MESH.defaultBlockState()
                        .setValue(BattlefieldWeaponMeshBlock.SOURCE, mesh.ordinal()),
                        stack, buffers, light, overlay);
            } else {
                Block block = switch (p.prop()) {
                    case SHIELD -> ModBlocks.BATTLEFIELD_SHIELD;
                    case SPEAR -> ModBlocks.BATTLEFIELD_SPEAR;
                    case CROSSBOW -> ModBlocks.BATTLEFIELD_CROSSBOW;
                    default -> throw new IllegalStateException("Banner placed as original block");
                };
                stack.translate(-0.5, p.prop() == com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop.SHIELD ? 1 : 0, -0.5);
                BlockState modelState = block.defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
                blocks.renderSingleBlock(modelState, stack, buffers, light, overlay);
            }
            stack.popPose();
        }
        stack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BattlefieldSceneBlockEntity entity) {
        // Small preview spreads over several blocks, beyond the anchor AABB.
        // This is deliberately NOT the future mass-decor rendering strategy.
        return true;
    }
}
