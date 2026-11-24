package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class PantheonBlockProtectionMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void tearsofink$protectPantheon(BlockPos pos, BlockState state, int flags, int recursion,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!PantheonTemple.allowsBlockWrite((Level) (Object) this, pos)) cir.setReturnValue(false);
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void tearsofink$protectPantheonLoot(BlockPos pos, boolean drops, net.minecraft.world.entity.Entity entity,
                                                int recursion, CallbackInfoReturnable<Boolean> cir) {
        if (PantheonTemple.protectedAt((Level) (Object) this, pos)) cir.setReturnValue(false);
    }
}
