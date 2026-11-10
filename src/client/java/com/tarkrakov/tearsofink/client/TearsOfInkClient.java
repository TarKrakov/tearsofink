package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.client.fog.FogBankSpawner;
import com.tarkrakov.tearsofink.client.particle.FogBankParticle;
import com.tarkrakov.tearsofink.particle.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.renderer.RenderType;

public class TearsOfInkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleFactoryRegistry.getInstance().register(ModParticles.FOG_BANK, FogBankParticle.Provider::new);
		ClientTickEvents.END_CLIENT_TICK.register(FogBankSpawner::tick);

		// Блоки с прозрачными пикселями (листва, саженец)
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_LEAVES, RenderType.cutoutMipped());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_SAPLING, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_DOOR, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_TRAPDOOR, RenderType.cutout());
	}
}
