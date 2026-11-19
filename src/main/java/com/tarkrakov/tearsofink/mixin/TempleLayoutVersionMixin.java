package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.world.InkLandsBiomeSource;
import com.tarkrakov.tearsofink.world.TempleWorldAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
abstract class TempleLayoutVersionMixin {
	@Shadow @Final protected LevelStorageSource.LevelStorageAccess storageSource;

	@Inject(method = "createLevels", at = @At("HEAD"))
	private void tearsofink$bindTempleLayout(ChunkProgressListener progress, CallbackInfo ci) {
		MinecraftServer server = (MinecraftServer) (Object) this;
		InkLandsBiomeSource.bindTempleLayout(server.registryAccess(),
				((TempleWorldAccess) storageSource).tearsofink$storedTempleLayout());
	}

}
