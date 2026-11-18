package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.world.adit.AditStructure;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkStatusTasks.class)
abstract class AditLightingMixin {
	@Inject(method = "light", at = @At("HEAD"))
	private static void tearsofink$finishAdit(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> cache,
											ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> ci) {
		AditStructure.finishDecoration(context.level(), chunk);
	}
}
