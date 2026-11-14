package com.tarkrakov.tearsofink.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Клинки архетипов: без ванильного взмаха и ванильного «опускания» — движения задаёт WeaponAnims. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Unique
	private static boolean tearsofink$inner;

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void tearsofink$ownMotion(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress,
									  ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
		if (tearsofink$inner || Archetype.of(stack) == null || (swingProgress == 0 && equippedProgress == 0)) return;
		if (player.isUsingItem() && player.getUsedItemHand() == hand) return; // своё использование (если будет) — по-ванильному
		tearsofink$inner = true;
		try {
			((ItemInHandRendererInvoker) this).tearsofink$renderArmWithItem(player, partialTicks, pitch, hand, 0, stack, 0, poseStack, buffer, light);
		} finally {
			tearsofink$inner = false;
		}
		ci.cancel();
	}
}
