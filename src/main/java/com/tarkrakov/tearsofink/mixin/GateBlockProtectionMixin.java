package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.entity.patrol.BarricadeGates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exact installed gate positions, never the block material globally. Property changes (e.g. LIT) remain legal. */
@Mixin(Level.class)
public abstract class GateBlockProtectionMixin {
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at=@At("HEAD"),cancellable=true)
    private void tearsofink$keepGate(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> cir) {
        Level level=(Level)(Object)this;
        if(BarricadeGates.protectedAt(level,pos)&&level.getBlockState(pos).getBlock()!=state.getBlock())
            cir.setReturnValue(false);
    }
}
