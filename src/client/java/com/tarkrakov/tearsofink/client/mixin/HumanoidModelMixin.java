package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.combat.ThirdPerson;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** После обычной позы игрока — повороты ударов клинков от третьего лица (рукава повторят руки в PlayerModel). */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void tearsofink$bladeMotion(LivingEntity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch, CallbackInfo ci) {
		if (entity instanceof Player || entity instanceof com.tarkrakov.tearsofink.hunt.HunterEntity) ThirdPerson.apply((HumanoidModel<?>) (Object) this, entity);
	}
}
