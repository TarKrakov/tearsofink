package com.tarkrakov.tearsofink.compat.jaber.mixin;

import com.tarkrakov.tearsofink.compat.jaber.JaberCompat;
import com.tarkrakov.tearsofink.compat.jaber.JaberRouter;
import net.minecraft.world.entity.animal.frog.Frog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One verified entry point; no assumptions about JABER's local variable slots. */
@Pseudo
@Mixin(targets = "ru.chaos.duck.jaber.JaberDriver", remap = false)
public abstract class JaberDriverMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$route(Frog frog, CallbackInfo ci) {
        if (!JaberCompat.active(frog.level())) return;
        JaberRouter.tick(frog);
        ci.cancel();
    }
}
