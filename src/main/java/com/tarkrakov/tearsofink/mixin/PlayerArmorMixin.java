package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tarkrakov.tearsofink.item.OrdinaryArmorItem;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.VanillaArmorPenalty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerArmorMixin {
	@Mixin(LivingEntity.class)
	public interface EquippedArmor {
		@Invoker("getLastArmorItem")
		ItemStack tearsofink$getAppliedArmor(EquipmentSlot slot);
	}

	@Inject(method = "handleEquipmentChanges", at = @At("TAIL"))
	private void tearsofink$scaleVanillaArmor(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && ModDimensions.isLands(player.level())) {
			VanillaArmorPenalty.update(player);
		}
	}

	// Ваниль передаёт в формулу целое getArmorValue(). Для нашей брони и D-043 нужны дробные очки.
	@WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
	private float tearsofink$useFractionalProtection(LivingEntity entity, float damage, DamageSource source,
													 float armor, float toughness, Operation<Float> original) {
		if (entity instanceof ServerPlayer player && (ModDimensions.isLands(player.level())
				|| player.getInventory().armor.stream().anyMatch(stack -> stack.getItem() instanceof OrdinaryArmorItem))) {
			armor = (float) player.getAttributeValue(Attributes.ARMOR);
		}
		return original.call(entity, damage, source, armor, toughness);
	}
}
