package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.tarkrakov.tearsofink.travel.Travel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public abstract class RespawnPlayerCreationMixin {
	@WrapOperation(method = "respawn", at = @At(value = "NEW", target =
			"(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;" +
			"Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/level/ServerPlayer;"))
	private ServerPlayer tearsofink$usePreparedDestination(MinecraftServer server, ServerLevel level, GameProfile profile,
			ClientInformation information, Operation<ServerPlayer> original) {
		return Travel.createRespawnPlayer(level, () -> original.call(server, level, profile, information));
	}
}
