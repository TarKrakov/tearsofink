package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.tarkrakov.tearsofink.ink.SoulPenalty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Проданная душа: еда даёт меньше голода и насыщения. */
@Mixin(Player.class)
public abstract class PlayerEatMixin {
	@WrapOperation(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"))
	private void tearsofink$soulPenalty(FoodData foodData, FoodProperties food, Operation<Void> original,
										 @Local(argsOnly = true) ItemStack stack) {
		Player self = (Player) (Object) this;
		food = SoulPenalty.rawMeatBonus(self, stack, food);
		float m = SoulPenalty.multiplier(self);
		if (m >= 1f) {
			original.call(foodData, food);
			return;
		}
		int nutrition = food.nutrition() > 0 ? Math.max(1, Math.round(food.nutrition() * m)) : 0;
		original.call(foodData, new FoodProperties(nutrition, food.saturation() * m, food.canAlwaysEat(),
				food.eatSeconds(), food.usingConvertsTo(), food.effects()));
	}
}
