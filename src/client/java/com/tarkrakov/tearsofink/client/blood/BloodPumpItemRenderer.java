package com.tarkrakov.tearsofink.client.blood;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Иконка помпы в инвентаре и в руке — сама 3D-модель автора (в покое, без анимации).
 * Поворот и размер в разных местах — в models/item/blood_pump.json (раздел display).
 * Если автор нарисует плоскую иконку — вернём item/generated.
 */
public class BloodPumpItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	private BloodPumpModel model;

	@Override
	public void render(ItemStack stack, ItemDisplayContext mode, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (model == null) {
			model = new BloodPumpModel(Minecraft.getInstance().getEntityModels().bakeLayer(BloodPumpModel.LAYER));
		}
		model.root().getAllParts().forEach(ModelPart::resetPose);
		BloodPumpRenderer.draw(model, pose, buffers, light, overlay, Direction.SOUTH);
	}
}
