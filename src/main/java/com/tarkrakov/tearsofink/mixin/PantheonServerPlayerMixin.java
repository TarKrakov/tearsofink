package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tarkrakov.tearsofink.pantheon.PantheonRecovery;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PantheonServerPlayerMixin {
    @WrapOperation(method = "restoreFrom", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean tearsofink$restorePantheonInventory(GameRules rules, GameRules.Key<GameRules.BooleanValue> key,
                                                        Operation<Boolean> original, ServerPlayer previous, boolean alive) {
        return PantheonTemple.isTemple(previous.level()) || original.call(rules, key);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void tearsofink$returnMenuItemsBeforeDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!PantheonTemple.isTemple(player.level())) return;
        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
        player.inventoryMenu.removed(player);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tearsofink$savePantheonCursor(CompoundTag tag, CallbackInfo ci) {
        PantheonRecovery.writeCursor((ServerPlayer) (Object) this, tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tearsofink$recoverPantheonCursor(CompoundTag tag, CallbackInfo ci) {
        PantheonRecovery.readCursor((ServerPlayer) (Object) this, tag);
    }

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void tearsofink$denyHotbarDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (PantheonTemple.isTemple(player.level())) cir.setReturnValue(false);
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void tearsofink$saveOverflow(ItemStack stack, boolean randomThrow, boolean track,
                                         CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (PantheonTemple.isTemple(player.level())) {
            PantheonRecovery.store(player, stack);
            cir.setReturnValue(null);
        }
    }
}
