package com.tarkrakov.tearsofink.client.battlefield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.block.BattlefieldSceneBlock;
import com.tarkrakov.tearsofink.block.BattlefieldSceneBlockEntity;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Pose;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Scene;
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
    private final BlockRenderDispatcher blocks;

    public BattlefieldSceneRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(BattlefieldSceneBlockEntity entity, float partialTick, PoseStack stack,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState root = entity.getBlockState();
        int sceneIndex = root.getValue(BattlefieldSceneBlock.SCENE);
        if (sceneIndex < 0 || sceneIndex >= Scene.values().length) return;
        List<Pose> poses = BattlefieldPosedScenes.plan(Scene.values()[sceneIndex],
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
            Block block = switch (p.prop()) {
                case SHIELD -> ModBlocks.BATTLEFIELD_SHIELD;
                case SPEAR -> ModBlocks.BATTLEFIELD_SPEAR;
                case CROSSBOW -> ModBlocks.BATTLEFIELD_CROSSBOW;
                case BANNER -> throw new IllegalStateException("Animated flag is placed separately");
            };

            stack.pushPose();
            stack.translate(0.5 + p.right(), entity.offsetAt(i) + p.sink(), 0.5 - p.forward());
            // Pivot is at ground contact, not the model centre: the shield
            // leans into soil, a spear crosses it, the crossbow lies fallen.
            stack.mulPose(Axis.YP.rotationDegrees(p.yaw()));
            stack.mulPose(Axis.ZP.rotationDegrees(p.roll()));
            stack.mulPose(Axis.XP.rotationDegrees(p.pitch()));
            stack.translate(-0.5, p.prop() == com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop.SHIELD ? 1 : 0, -0.5);
            BlockState modelState = block.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
            blocks.renderSingleBlock(modelState, stack, buffers, light, overlay);
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
