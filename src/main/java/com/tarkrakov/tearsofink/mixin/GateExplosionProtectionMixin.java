package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.entity.patrol.BarricadeGates;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remove authored gate cells before explosion loot/break handling, not after it. */
@Mixin(Explosion.class)
public abstract class GateExplosionProtectionMixin {
    @Shadow @Final private Level level;
    @Inject(method="finalizeExplosion",at=@At("HEAD"))
    private void tearsofink$shieldGate(boolean particles,CallbackInfo ci) {
        ((Explosion)(Object)this).getToBlow().removeIf(pos->BarricadeGates.protectedAt(level,pos)
                || PantheonTemple.protectedAt(level, pos));
    }
}
