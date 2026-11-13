package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.travel.Travel;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Умер в Землях — возрождаешься сразу в Землях (а не в обычном мире с переносом потом). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRespawnMixin {
	@Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
	private void tearsofink$respawnInLands(boolean keepInventory, DimensionTransition.PostDimensionTransition post,
										   CallbackInfoReturnable<DimensionTransition> cir) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (ModDimensions.isLands(self.level()) && ModDimensions.lands(self.server) != null) {
			cir.setReturnValue(Travel.landsRespawn(self, post));
		}
	}
}
