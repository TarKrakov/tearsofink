package com.tarkrakov.tearsofink.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.client.ShieldIcons;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.tarkrakov.tearsofink.client.combat.WeaponAnims;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** В окнах (инвентарь, сундуки) наши щиты рисуются плоским рисунком автора, в руке — 3D-моделью. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
	@Unique private static ItemDisplayContext tearsofink$context = ItemDisplayContext.NONE;
	@Unique private static ItemStack tearsofink$stack = ItemStack.EMPTY;

	@Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
			at = @At("HEAD"), cancellable = true)
	private void tearsofink$flatShieldIcon(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose,
										   MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
		tearsofink$context = context;
		tearsofink$stack = stack;
		if (context != ItemDisplayContext.GUI) return;
		BakedModel icon = ShieldIcons.iconFor(stack);
		if (icon == null || icon == model) return;
		((ItemRenderer) (Object) this).render(stack, context, leftHand, pose, buffers, light, overlay, icon);
		ci.cancel();
	}

	/** От первого лица у клинков архетипов вместо обычной стойки — поза текущего движения (удар, доставание). */
	@Redirect(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/ItemTransform;apply(ZLcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void tearsofink$weaponMotion(ItemTransform rest, boolean leftHand, PoseStack pose) {
		if (!WeaponAnims.apply(tearsofink$context, tearsofink$stack, rest, leftHand, pose)) rest.apply(leftHand, pose);
	}
}
