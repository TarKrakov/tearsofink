package com.tarkrakov.tearsofink.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.client.combat.ThirdPerson;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Третье лицо: во время удара клинок архетипа поворачивается в руке «кистью» (только Чернильные земли, см. ThirdPerson). */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
	@org.spongepowered.asm.mixin.Shadow
	@org.spongepowered.asm.mixin.Final
	private net.minecraft.client.renderer.ItemInHandRenderer itemInHandRenderer;

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void tearsofink$rangedOffhand(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack pose,
                                          MultiBufferSource buffers, int light, CallbackInfo ci) {
        if(entity instanceof net.minecraft.world.entity.player.Player player
                && com.tarkrakov.tearsofink.client.KnifeAnims.isKnife(stack)
                && com.tarkrakov.tearsofink.client.KnifeAnims.hidden(player,arm)){ci.cancel();return;}
        if (entity instanceof net.minecraft.world.entity.player.Player p && arm != p.getMainArm()
                && com.tarkrakov.tearsofink.weapon.ranged.RangedCombat.blocksOffhand(p)) ci.cancel();
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "renderArmWithItem",
        at = @At(value="INVOKE",target="Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void tearsofink$crossbow(net.minecraft.client.renderer.ItemInHandRenderer renderer,LivingEntity entity,ItemStack stack,ItemDisplayContext context,boolean left,PoseStack pose,MultiBufferSource buffers,int light,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        if(entity instanceof net.minecraft.world.entity.player.Player p
            && com.tarkrakov.tearsofink.client.CrossbowAnims.isCrossbow(stack)
            && com.tarkrakov.tearsofink.client.CrossbowAnims.renderThird(p,stack,context,left,pose,buffers,light))return;
        original.call(renderer,entity,stack,context,left,pose,buffers,light);
    }

    /** Ореол щитов Души парой (после самого щита). */
	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void tearsofink$shieldHalo(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack pose,
									   MultiBufferSource buffers, int light, CallbackInfo ci) {
		com.tarkrakov.tearsofink.client.shield.ShieldStanceClient.halo(itemInHandRenderer, entity, stack, context, arm == HumanoidArm.LEFT, pose, buffers);
	}

	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void tearsofink$wrist(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack pose,
								  MultiBufferSource buffers, int light, CallbackInfo ci) {
		ThirdPerson.wrist(entity, arm, pose);
		com.tarkrakov.tearsofink.client.shield.ShieldStanceClient.sideItem(entity, arm, pose);
	}

	/** Щиты Души парой светятся в темноте (ярче с зарядом). */
	@org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderArmWithItem", index = 6, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private int tearsofink$shieldGlow(LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean left, PoseStack pose,
									  MultiBufferSource buffers, int light) {
		return com.tarkrakov.tearsofink.client.shield.ShieldStanceClient.light(entity, stack, light);
	}
}
