package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.entity.patrol.BarricadeGates;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A piston must fail the whole move, so cancellation cannot duplicate gate blocks. */
@Mixin(PistonStructureResolver.class)
public abstract class GatePistonProtectionMixin {
    @Shadow @Final private Level level;
    @Inject(method="resolve",at=@At("HEAD"),cancellable=true)
    private void tearsofink$preventTemplePiston(CallbackInfoReturnable<Boolean> cir) {
        if (PantheonTemple.isTemple(level)) cir.setReturnValue(false);
    }

    @Inject(method="resolve",at=@At("RETURN"),cancellable=true)
    private void tearsofink$keepGate(CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue())return;
        var self=(PistonStructureResolver)(Object)this;
        if (self.getToPush().stream().anyMatch(pos -> BarricadeGates.protectedAt(level, pos)
                || PantheonTemple.protectedAt(level, pos)
                || PantheonTemple.protectedAt(level, pos.relative(self.getPushDirection())))
                || self.getToDestroy().stream().anyMatch(pos -> BarricadeGates.protectedAt(level, pos)
                || PantheonTemple.protectedAt(level, pos)))
            cir.setReturnValue(false);
    }
}
