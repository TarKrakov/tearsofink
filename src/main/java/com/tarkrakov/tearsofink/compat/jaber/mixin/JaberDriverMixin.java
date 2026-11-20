package com.tarkrakov.tearsofink.compat.jaber.mixin;

import com.tarkrakov.tearsofink.compat.jaber.JaberCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    // In the reviewed findPath bytecode, the first free() call scans a destination
    // column from min(oldY,newY) to max(oldY,newY)+1. On an uphill edge that wrongly
    // includes the destination's solid floor, which walkable() already validated.
    // Never ignore clearance at or above newY, and preserve the diagonal/source checks.
    @WrapOperation(method = "findPath", at = @At(value = "INVOKE",
            target = "Lru/chaos/duck/jaber/JaberDriver;free", ordinal = 0, remap = false), remap = false)
    private static boolean toi$uphillClearance(Level level, int x, int y, int z,
                                                Operation<Boolean> original, @Local(index = 36) int nextY) {
        if (JaberCompat.active(level) && y < nextY && JaberCompat.readable(level, x, y, z)) return true;
        return original.call(level, x, y, z);
    }

    // Local 43 is the positive per-edge cost (not g/f or heuristic) in JABER 1.0.0.
    // Add a penalty only after the original collision, fluid and hazard checks.
    @ModifyVariable(method = "findPath", at = @At("STORE"), index = 43, remap = false)
    private static double toi$roadCost(double cost, @Local(index = 4) Level level,
                                        @Local(index = 29) int x, @Local(index = 36) int y,
                                        @Local(index = 30) int z, @Local(index = 38) boolean swimming) {
        if (!JaberCompat.active(level)) return cost;
        return cost + JaberCompat.offRoadCost(level, x, y, z, swimming);
    }

    // The second moveToward() call in tick is the blind three-block fallback
    // after the path is null/exhausted. On land stop until the normal replanner
    // retries; leave the original swimming fallback for passenger air management.
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lru/chaos/duck/jaber/JaberDriver;moveToward", ordinal = 1, remap = false), remap = false)
    private static void toi$waitForPath(Frog frog, double x, double y, double z, double speed, Operation<Void> original) {
        if (!JaberCompat.active(frog.level()) || frog.isInWater()) {
            original.call(frog, x, y, z, speed);
            return;
        }
        frog.getNavigation().stop();
        frog.setDeltaMovement(0, frog.getDeltaMovement().y, 0);
    }

    @ModifyVariable(method = "moveToward", at = @At("HEAD"), argsOnly = true, index = 7, remap = false)
    private static double toi$calmSpeed(double speed, Frog frog) {
        return JaberCompat.active(frog.level()) ? Math.min(speed, 0.24) : speed;
    }

    // A one-block road step must trigger a jump before horizontal collision.
    @ModifyConstant(method = "moveToward", constant = @Constant(doubleValue = 1.1), remap = false)
    private static double toi$roadStep(double threshold, Frog frog) {
        return JaberCompat.active(frog.level()) ? 0.60 : threshold;
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
