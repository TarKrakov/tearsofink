package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.PaleForestAmbience;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla music yields only in the approved pilot biome. All other dimensions/biomes are unchanged. */
@Mixin(MusicManager.class)
public abstract class PaleForestMusicMixin {
    @Shadow public abstract void stopPlaying();
    @Unique private boolean tearsofink$forestMusic;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tearsofink$replaceForestMusic(CallbackInfo ci) {
        if (PaleForestAmbience.inForest(Minecraft.getInstance())) {
            if (!tearsofink$forestMusic) stopPlaying();
            tearsofink$forestMusic = true;
            ci.cancel();
        } else {
            tearsofink$forestMusic = false;
        }
    }
}
