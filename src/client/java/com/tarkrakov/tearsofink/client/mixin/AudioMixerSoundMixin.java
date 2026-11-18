package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.audio.AudioMixer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Local gain after event/sample volume, before the vanilla engine's category and distance handling. */
@Mixin(AbstractSoundInstance.class)
public abstract class AudioMixerSoundMixin {
    @Shadow public abstract ResourceLocation getLocation();

    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void tearsofink$applyLocalGain(CallbackInfoReturnable<Float> cir) {
        float gain = AudioMixer.gain(getLocation());
        if (gain == 1f) return;
        float base = cir.getReturnValueF();
        float volume = base * gain;
        // Avoid repeated attempts to restart a muted tickable ambience loop.
        // This is an effectively silent floor, not audible playback.
        if (base > 0f && gain == 0f && (Object) this instanceof TickableSoundInstance) volume = 0.00001f;
        cir.setReturnValue(volume);
    }
}
