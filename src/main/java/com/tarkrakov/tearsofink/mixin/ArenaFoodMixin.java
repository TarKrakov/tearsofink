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

/** Sanctuary-wide food conservation, independent of the combat session or arena bounds. */
@Mixin(FoodData.class)
public abstract class ArenaFoodMixin {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private float exhaustionLevel;
    @Unique private int tearsofink$foodBefore = -1;
    @Unique private float tearsofink$saturationBefore;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tearsofink$rememberSanctuaryFood(Player player, CallbackInfo ci) {
        tearsofink$foodBefore = -1;
        if (player instanceof ServerPlayer serverPlayer && PantheonCombat.preservesHunger(serverPlayer)) {
            tearsofink$foodBefore = foodLevel;
            tearsofink$saturationBefore = saturationLevel;
            // No accumulated exhaustion may consume food or saturation here.
            exhaustionLevel = 0;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tearsofink$preserveSanctuaryFood(Player player, CallbackInfo ci) {
        if (tearsofink$foodBefore >= 0) {
            foodLevel = Math.max(foodLevel, tearsofink$foodBefore);
            saturationLevel = Math.max(saturationLevel, tearsofink$saturationBefore);
            // FoodData's own natural regeneration adds exhaustion directly.
            exhaustionLevel = 0;
        }
        tearsofink$foodBefore = -1;
    }
}
