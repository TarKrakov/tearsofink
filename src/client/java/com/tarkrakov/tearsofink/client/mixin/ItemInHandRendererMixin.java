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

	/** Щиты Души парой светятся в темноте и от первого лица. */
	@org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderArmWithItem", index = 6, require = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private int tearsofink$shieldGlow(net.minecraft.world.entity.LivingEntity entity, ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
									  boolean left, PoseStack pose, MultiBufferSource buffers, int light) {
		return com.tarkrakov.tearsofink.client.shield.ShieldStanceClient.light(entity, stack, light);
	}

	/** Ореол щитов Души парой и от первого лица. */
	@Inject(method = "renderArmWithItem", require = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void tearsofink$shieldHalo(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress,
									   ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
		boolean left = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
		com.tarkrakov.tearsofink.client.shield.ShieldStanceClient.halo((ItemInHandRenderer) (Object) this, player, stack,
				left ? net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND : net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
				left, poseStack, buffer);
	}

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void tearsofink$ownMotion(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress,
									  ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
		if (hand == InteractionHand.OFF_HAND && com.tarkrakov.tearsofink.weapon.ranged.RangedCombat.blocksOffhand(player)) {
            ci.cancel(); return;
        }
        if (!tearsofink$inner && com.tarkrakov.tearsofink.weapon.ranged.RangedItem.kind(stack) != null
                && com.tarkrakov.tearsofink.client.RangedClient.aimsHand(player, hand == InteractionHand.OFF_HAND)) {
            boolean left = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
            tearsofink$inner = true;
            poseStack.pushPose();
            try {
                poseStack.translate(left ? .23 : -.23, .10, -.12);
                ((ItemInHandRendererInvoker) this).tearsofink$renderArmWithItem(player, partialTicks, pitch, hand, 0, stack, 0, poseStack, buffer, light);
            } finally {
                poseStack.popPose(); tearsofink$inner = false;
            }
            ci.cancel(); return;
        }
        Archetype archetype = Archetype.of(stack);
		if (tearsofink$inner || archetype == null) return;
		// Preserve the legacy contract. Custom pale poses also need equip=0:
		// otherwise cooldown lowers the main hand independently of the off hand.
		boolean pale = archetype == Archetype.PALE_LINEAR_GREATSWORD;
		if (swingProgress == 0 && equippedProgress == 0) return;
		if (archetype == Archetype.BASTION && !com.tarkrakov.tearsofink.weapon.combat.Combat.bastion(player)) return; // один щит — по-ванильному
		if (!pale && player.isUsingItem() && player.getUsedItemHand() == hand) return; // своё использование (если будет) — по-ванильному
		var gm = net.minecraft.client.Minecraft.getInstance().gameMode;
		if (hand == InteractionHand.MAIN_HAND && gm != null && gm.isDestroying()) return; // копает — ванильный мах «палкой-копалкой»
		tearsofink$inner = true;
		try {
			((ItemInHandRendererInvoker) this).tearsofink$renderArmWithItem(player, partialTicks, pitch, hand, 0, stack, 0, poseStack, buffer, light);
		} finally {
			tearsofink$inner = false;
		}
		ci.cancel();
	}
}
