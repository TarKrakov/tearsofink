package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Warm the sky contribution, leaving torch/lantern-dominated lightmap texels alone. */
@Mixin(LightTexture.class)
public abstract class PantheonLightTextureMixin {
    @ModifyArgs(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"))
    private void tearsofink$warmSkyLight(Args args) {
        var level = Minecraft.getInstance().level;
        if (level == null || !PantheonTemple.isTemple(level)) return;
        int block = args.get(0), sky = args.get(1), color = args.get(2);
        float weight = Math.max(0, sky - block) / 15f;
        // NativeImage uses ABGR, not ARGB.
        int red = Math.round((color & 255) * (1f - 0.06f * weight));
        int green = Math.round(((color >>> 8) & 255) * (1f - 0.23f * weight));
        int blue = Math.round(((color >>> 16) & 255) * (1f - 0.35f * weight));
        args.set(2, (color & 0xff000000) | (blue << 16) | (green << 8) | red);
    }
}
