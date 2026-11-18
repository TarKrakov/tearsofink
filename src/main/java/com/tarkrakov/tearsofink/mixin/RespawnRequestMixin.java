package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.travel.Travel;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class RespawnRequestMixin {
	@Shadow public ServerPlayer player;

	@Inject(method = "handleClientCommand", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"), cancellable = true)
	private void tearsofink$awaitRespawnChunks(ServerboundClientCommandPacket packet, CallbackInfo ci) {
		if (packet.getAction() == ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
				&& player.getHealth() <= 0 && !player.wonGame && ModDimensions.isLands(player.level())
				&& Travel.deferRespawn(player)) ci.cancel();
	}
}
