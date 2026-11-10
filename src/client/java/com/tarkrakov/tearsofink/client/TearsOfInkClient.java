package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.client.fog.FogBankSpawner;
import com.tarkrakov.tearsofink.block.WoodSet;
import com.tarkrakov.tearsofink.client.particle.DripParticle;
import com.tarkrakov.tearsofink.client.particle.FogBankParticle;
import com.tarkrakov.tearsofink.client.particle.TearGlintParticle;
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
		ParticleFactoryRegistry.getInstance().register(ModParticles.TEAR_GLINT, TearGlintParticle.Provider::new);
		ParticleFactoryRegistry particles = ParticleFactoryRegistry.getInstance();
		particles.register(ModParticles.BLOOD_DRIP_HANG, s -> new DripParticle.Provider(s, DripParticle.Phase.HANG, ModParticles.BLOOD_DRIP_FALL));
		particles.register(ModParticles.BLOOD_DRIP_FALL, s -> new DripParticle.Provider(s, DripParticle.Phase.FALL, ModParticles.BLOOD_DRIP_LAND));
		particles.register(ModParticles.BLOOD_DRIP_LAND, s -> new DripParticle.Provider(s, DripParticle.Phase.LAND, null));
		particles.register(ModParticles.INK_DRIP_HANG, s -> new DripParticle.Provider(s, DripParticle.Phase.HANG, ModParticles.INK_DRIP_FALL));
		particles.register(ModParticles.INK_DRIP_FALL, s -> new DripParticle.Provider(s, DripParticle.Phase.FALL, ModParticles.INK_DRIP_LAND));
		particles.register(ModParticles.INK_DRIP_LAND, s -> new DripParticle.Provider(s, DripParticle.Phase.LAND, null));
		ClientTickEvents.END_CLIENT_TICK.register(FogBankSpawner::tick);

		// Блоки с прозрачными пикселями (листва, саженец)
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_LEAVES, RenderType.cutoutMipped());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_SAPLING, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_DOOR, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TEARBLOOM, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOLDEN_SCARLET_LEAVES, RenderType.cutoutMipped());
		for (WoodSet set : new WoodSet[]{ModBlocks.SCARLET, ModBlocks.INK}) {
			BlockRenderLayerMap.INSTANCE.putBlock(set.leaves(), RenderType.cutoutMipped());
			BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), set.sapling(), set.door(), set.trapdoor());
		}
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_TEARBLOOM, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_TRAPDOOR, RenderType.cutout());
	}
}
