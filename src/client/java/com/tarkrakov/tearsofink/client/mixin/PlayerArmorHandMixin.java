package com.tarkrakov.tearsofink.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.client.armor.AuthoredArmorRenderer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draw the equipped chestplate's corresponding arm over the visible vanilla FP hand. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerArmorHandMixin {
    @Inject(method = "renderHand", at = @At("TAIL"))
    private void tearsofink$armorHand(PoseStack pose, MultiBufferSource buffers, int light,
                                     AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        var model = ((PlayerRenderer) (Object) this).getModel();
        AuthoredArmorRenderer.renderHand(pose, buffers, player.getItemBySlot(EquipmentSlot.CHEST),
                arm, arm == model.rightArm, light);
    }
}
