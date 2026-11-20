package com.tarkrakov.tearsofink.compat.jaber.mixin;

import com.tarkrakov.tearsofink.compat.jaber.JaberCompat;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** JABER 1.0.0 uses its own A* and probes solid() on neighboring columns without loaded(). */
@Pseudo
@Mixin(targets = "ru.chaos.duck.jaber.JaberDriver", remap = false)
public abstract class JaberDriverMixin {
    @Inject(method = "loaded", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$loaded(Level level, int x, int z, CallbackInfoReturnable<Boolean> cir) {
        if (JaberCompat.active(level)) cir.setReturnValue(JaberCompat.columnReady(level, x, z));
    }

    @Inject(method = "solid", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$solid(Level level, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (JaberCompat.active(level) && !JaberCompat.shapeReady(level, x, y, z)) cir.setReturnValue(true);
    }

    @Inject(method = "danger", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$danger(Level level, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (JaberCompat.active(level) && !JaberCompat.readable(level, x, y, z)) cir.setReturnValue(true);
    }

    @Inject(method = "isWater", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$water(Level level, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (JaberCompat.active(level) && !JaberCompat.readable(level, x, y, z)) cir.setReturnValue(false);
    }

    @Inject(method = "isLava", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$lava(Level level, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (JaberCompat.active(level) && !JaberCompat.readable(level, x, y, z)) cir.setReturnValue(true);
    }

    @Inject(method = {"walkable", "swimmable"}, at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$validNode(Level level, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (JaberCompat.active(level) && (!JaberCompat.shapeReady(level, x, y, z)
                || !JaberCompat.readable(level, x, y - 1, z) || !JaberCompat.readable(level, x, y + 1, z))) {
            cir.setReturnValue(false);
        }
    }

    // JABER's emergency branch every 160 frog ticks teleports both frog and rider to a
    // heightmap point. Suppress that branch in the adventure world (no wall/cave bypass).
    // At age zero the original State.progress is NaN, so the branch cannot teleport.
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 160), remap = false)
    private static int toi$noSurfaceTeleport(int period, Frog frog) {
        return JaberCompat.active(frog.level()) ? Integer.MAX_VALUE : period;
    }

    @Inject(method = "moveToward", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$movement(Frog frog, double x, double y, double z, double speed, CallbackInfo ci) {
        if (!JaberCompat.active(frog.level())) return;
        double dx = x - frog.getX(), dz = z - frog.getZ();
        double dist = Math.hypot(dx, dz);
        if (!Double.isFinite(dist) || !JaberCompat.columnReady(frog.level(), Mth.floor(x), Mth.floor(z))) {
            frog.setDeltaMovement(0, frog.getDeltaMovement().y, 0);
            ci.cancel();
            return;
        }
        if (dist > 0.001) {
            // Check the body width at the next small step as well (not just its center).
            double nearX = frog.getX() + dx / dist * Math.min(0.6, dist);
            double nearZ = frog.getZ() + dz / dist * Math.min(0.6, dist);
            double half = frog.getBbWidth() * 0.5;
            for (int bx = Mth.floor(nearX - half); bx <= Mth.floor(nearX + half); bx++) {
                for (int bz = Mth.floor(nearZ - half); bz <= Mth.floor(nearZ + half); bz++) {
                    if (!JaberCompat.columnReady(frog.level(), bx, bz)) {
                        frog.setDeltaMovement(0, frog.getDeltaMovement().y, 0);
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }
}
