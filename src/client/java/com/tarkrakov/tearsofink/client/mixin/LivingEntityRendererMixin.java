package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.hunt.HuntClient;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Цель Охоты рисуется темнее. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Unique
	private static boolean tearsofink$prey;

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"))
	private void tearsofink$which(LivingEntity entity, float yaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose,
								  net.minecraft.client.renderer.MultiBufferSource buffers, int light, CallbackInfo ci) {
		tearsofink$prey = HuntClient.isPrey(entity.getId());
	}

	@ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
			index = 4)
	private int tearsofink$shade(int color) {
		if (!tearsofink$prey) return color;
		float k = HuntClient.PREY_SHADE;
		return FastColor.ARGB32.color(FastColor.ARGB32.alpha(color), (int) (FastColor.ARGB32.red(color) * k),
				(int) (FastColor.ARGB32.green(color) * k), (int) (FastColor.ARGB32.blue(color) * k));
	}
}
