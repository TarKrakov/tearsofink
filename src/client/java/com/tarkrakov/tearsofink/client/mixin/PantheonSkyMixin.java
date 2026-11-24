package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Neutral Pantheon sky profile, independent of the flat void biome and its daytime colors. */
@Mixin(ClientLevel.class)
public abstract class PantheonSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void tearsofink$twilightSky(Vec3 camera, float tick, CallbackInfoReturnable<Vec3> cir) {
        if (PantheonTemple.isTemple((ClientLevel) (Object) this))
            cir.setReturnValue(new Vec3(0.36, 0.20, 0.25));
    }

    @Inject(method = "getSkyDarken", at = @At("RETURN"), cancellable = true)
    private void tearsofink$twilightLight(float tick, CallbackInfoReturnable<Float> cir) {
        if (PantheonTemple.isTemple((ClientLevel) (Object) this))
            cir.setReturnValue(Math.min(cir.getReturnValueF(), 0.64f));
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void tearsofink$twilightClouds(float tick, CallbackInfoReturnable<Vec3> cir) {
        if (PantheonTemple.isTemple((ClientLevel) (Object) this))
            cir.setReturnValue(new Vec3(0.65, 0.36, 0.29));
    }
}
