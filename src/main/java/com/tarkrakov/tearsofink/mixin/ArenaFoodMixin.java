package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve the food bar in the arena without refilling saturation or altering healing rates. */
@Mixin(FoodData.class)
public abstract class ArenaFoodMixin {
    @Shadow private int foodLevel;
    @Unique private int tearsofink$arenaFoodBefore = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tearsofink$rememberArenaFood(Player player, CallbackInfo ci) {
        tearsofink$arenaFoodBefore = player instanceof ServerPlayer serverPlayer
                && PantheonCombat.preservesHunger(serverPlayer) ? foodLevel : -1;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tearsofink$preserveArenaFood(Player player, CallbackInfo ci) {
        if (tearsofink$arenaFoodBefore >= 0)
            foodLevel = Math.max(foodLevel, tearsofink$arenaFoodBefore);
        tearsofink$arenaFoodBefore = -1;
    }
}
