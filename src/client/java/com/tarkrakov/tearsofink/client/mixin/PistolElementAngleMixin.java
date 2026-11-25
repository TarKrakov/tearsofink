package com.tarkrakov.tearsofink.client.mixin;

import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The authored pistol grip is -15 degrees. Preserve it without relaxing normal model validation. */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockElement$Deserializer")
public abstract class PistolElementAngleMixin {
    @Inject(method = "getAngle", at = @At("HEAD"), cancellable = true)
    private void tearsofink$pistolAngle(JsonObject rotation, CallbackInfoReturnable<Float> cir) {
        if (rotation.has("toi_exact_pistol") && rotation.get("toi_exact_pistol").getAsBoolean()
                && rotation.has("angle") && rotation.get("angle").getAsFloat() == -15f
                && rotation.has("axis") && rotation.get("axis").getAsString().equals("z")
                && (!rotation.has("rescale") || !rotation.get("rescale").getAsBoolean())) {
            cir.setReturnValue(-15f);
        }
    }
}
