package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.RangedClient;
import com.tarkrakov.tearsofink.weapon.ranged.KnifeTuning;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Smooth, local first-person zoom; do not change item-render FOV or the saved FOV setting. */
@Mixin(GameRenderer.class)
public abstract class KnifeAimFovMixin {
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true,require=0)
    private void tearsofink$knifeAim(Camera camera,float partialTicks,boolean useFovSetting,CallbackInfoReturnable<Double> cir){
        Minecraft mc=Minecraft.getInstance();
        if(!useFovSetting||mc.player==null||camera.getEntity()!=mc.player||!mc.options.getCameraType().isFirstPerson())return;
        float w=RangedClient.knifeAimAmount(mc.player,partialTicks);
        if(w>0)cir.setReturnValue(cir.getReturnValue()*(1-(1-KnifeTuning.AIM_FOV)*w));
    }
}
