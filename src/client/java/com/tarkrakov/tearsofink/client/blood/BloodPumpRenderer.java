package com.tarkrakov.tearsofink.client.blood;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.blood.BloodPumpBlock;
import com.tarkrakov.tearsofink.block.blood.BloodPumpBlockEntity;
import com.tarkrakov.tearsofink.block.blood.BloodTuning;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * Рисует помпу моделью «как у существа» и крутит анимацию `pump`:
 * один цикл анимации = один удар (BloodTuning.PUMP_INTERVAL), ритм — от времени мира.
 */
public class BloodPumpRenderer implements BlockEntityRenderer<BloodPumpBlockEntity> {
	private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/blood_pump.png");
	private final BloodPumpModel model;
	private final Vector3f cache = new Vector3f();

	public BloodPumpRenderer(BlockEntityRendererProvider.Context context) {
		this.model = new BloodPumpModel(context.bakeLayer(BloodPumpModel.LAYER));
	}

	@Override
	public void render(BloodPumpBlockEntity pump, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (pump.getLevel() == null) return;
		Direction facing = pump.getBlockState().getValue(BloodPumpBlock.FACING);

		model.root().getAllParts().forEach(ModelPart::resetPose);
		float phase = ((pump.getLevel().getGameTime() % BloodTuning.PUMP_INTERVAL) + partialTick) / BloodTuning.PUMP_INTERVAL;
		long ms = (long) (phase * BloodPumpAnimations.PUMP.lengthInSeconds() * 1000f);
		KeyframeAnimations.animate(model, BloodPumpAnimations.PUMP, ms, 1.0f, cache);

		pose.pushPose();
		pose.translate(0.5, 0, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
		pose.scale(-1f, -1f, 1f);
		pose.translate(0, -1.501, 0);
		model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, overlay);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(BloodPumpBlockEntity pump) {
		return true; // модель выше одного блока — не пропадать у края экрана
	}
}
