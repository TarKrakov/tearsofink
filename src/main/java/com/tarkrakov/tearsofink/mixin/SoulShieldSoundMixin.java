package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.sound.ModSounds;
import com.tarkrakov.tearsofink.weapon.SoulShieldItem;
import com.tarkrakov.tearsofink.weapon.shield.ShieldAudio;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla shield status packets 29/30 are the ordinary block/disable sound, not dual stance events. */
@Mixin(LivingEntity.class)
public abstract class SoulShieldSoundMixin {
    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void tearsofink$soulShieldSound(byte id, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if ((id != 29 && id != 30) || !self.level().isClientSide || !(self instanceof Player p)) return;
        SoulShieldItem shield;
        if (p.getUseItem().getItem() instanceof SoulShieldItem active) shield = active;
        else {
            // Axe disable can stop use before this packet arrives. Never replace an ambiguous vanilla shield.
            if (!p.getUseItem().isEmpty()) return;
            if (p.getMainHandItem().getItem() instanceof ShieldItem && !(p.getMainHandItem().getItem() instanceof SoulShieldItem)) return;
            if (p.getOffhandItem().getItem() instanceof ShieldItem && !(p.getOffhandItem().getItem() instanceof SoulShieldItem)) return;
            if (p.getMainHandItem().getItem() instanceof SoulShieldItem sh) shield = sh;
            else if (p.getOffhandItem().getItem() instanceof SoulShieldItem sh) shield = sh;
            else return;
        }
        if (!p.isSilent()) p.level().playLocalSound(p.getX(), p.getY() + p.getBbHeight() * 0.55, p.getZ(),
                id == 29 ? ModSounds.SOUL_SHIELD_BLOCK : ModSounds.SOUL_SHIELD_BREAK,
                SoundSource.PLAYERS, ShieldAudio.volume(id == 29 ? 0.44f : 0.55f, shield.power()),
                ShieldAudio.pitch(shield.power()), false);
        ci.cancel();
    }
}
