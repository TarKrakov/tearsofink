package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import com.tarkrakov.tearsofink.weapon.combat.WeaponLoadouts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PantheonPlayerProtectionMixin {
    /** Block normal player exhaustion at its source, including between FoodData ticks. */
    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void tearsofink$noSanctuaryExhaustion(float amount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player
                && PantheonTemple.isTemple(player.level())) ci.cancel();
    }

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void tearsofink$keepPantheonEquipment(CallbackInfo ci) {
        if (PantheonTemple.isTemple(((Player) (Object) this).level())) ci.cancel();
    }

    @Inject(method = "dropEquipment", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;dropAll()V", shift = At.Shift.AFTER))
    private void tearsofink$dropReserveWeapons(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) WeaponLoadouts.drop(player);
    }

    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void tearsofink$keepPantheonExperience(CallbackInfoReturnable<Integer> cir) {
        if (PantheonTemple.isTemple(((Player) (Object) this).level())) cir.setReturnValue(0);
    }
}
