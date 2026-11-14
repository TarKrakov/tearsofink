package com.tarkrakov.tearsofink.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.client.ShieldIcons;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** В окнах (инвентарь, сундуки) наши щиты рисуются плоским рисунком автора, в руке — 3D-моделью. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
	@Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
			at = @At("HEAD"), cancellable = true)
	private void tearsofink$flatShieldIcon(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose,
										   MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
		if (context != ItemDisplayContext.GUI) return;
		BakedModel icon = ShieldIcons.iconFor(stack);
		if (icon == null || icon == model) return;
		((ItemRenderer) (Object) this).render(stack, context, leftHand, pose, buffers, light, overlay, icon);
		ci.cancel();
	}
}
