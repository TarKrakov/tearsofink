package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class PantheonMenuProtectionMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void tearsofink$denyMenuDrops(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        if (PantheonTemple.isTemple(player.level())
                && (type == ClickType.THROW || (slot == -999 && type == ClickType.PICKUP))) ci.cancel();
    }
}
