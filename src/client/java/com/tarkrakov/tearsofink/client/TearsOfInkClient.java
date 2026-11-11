package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.client.fog.FogBankSpawner;
import com.tarkrakov.tearsofink.client.fog.FlameDanceSpawner;
import com.tarkrakov.tearsofink.client.particle.FlameDanceParticle;
import com.tarkrakov.tearsofink.client.particle.SheetParticles;
import com.tarkrakov.tearsofink.block.WoodSet;
import com.tarkrakov.tearsofink.client.particle.DripParticle;
import com.tarkrakov.tearsofink.client.particle.FogBankParticle;
import com.tarkrakov.tearsofink.client.particle.TearGlintParticle;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.client.hud.InkHud;
import com.tarkrakov.tearsofink.ink.InkSyncPayload;
import com.tarkrakov.tearsofink.skill.SkillPayloads;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.alchemy.ModAlchemy;
import com.tarkrakov.tearsofink.client.alchemy.AlchemyScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import com.tarkrakov.tearsofink.effect.ModEffects;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.weapon.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
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
		ParticleFactoryRegistry.getInstance().register(ModParticles.FLAME_DANCE, FlameDanceParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.INK_SMOKE, SheetParticles.SmokeProvider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.SPARK_BURST, SheetParticles.SparkProvider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.PAPER_SCRAP, SheetParticles.PaperProvider::new);
		ClientTickEvents.END_CLIENT_TICK.register(FlameDanceSpawner::tick);

		// Душа и Чернила: полоски и надпись при входе/выходе
		ClientPlayNetworking.registerGlobalReceiver(InkSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> InkHud.receive(payload)));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> InkHud.reset());
		HudRenderCallback.EVENT.register(InkHud::render);
		ClientPlayNetworking.registerGlobalReceiver(SkillPayloads.NotEnough.TYPE, (payload, context) -> context.client().execute(() -> InkHud.pulse(payload.soul())));

		// Навыки: клавиши
		SkillKeys.init();
		MenuScreens.register(ModAlchemy.MENU, AlchemyScreen::new);

		// Оружие: волна без модели (видна по брызгам); щиты — модель «в блоке» и «сияет»
		EntityRendererRegistry.register(ModEntities.INK_WAVE, NoopRenderer::new);
		for (Item shield : new Item[]{ModItems.RAM_SHIELD, ModItems.RADIANT_SHIELD, ModItems.MERCY_SHIELD, ModItems.REPEL_SHIELD}) {
			ItemProperties.register(shield, ResourceLocation.withDefaultNamespace("blocking"),
					(stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);
		}
		ItemProperties.register(ModItems.RADIANT_SHIELD, TearsOfInk.id("radiant"),
				(stack, level, entity, seed) -> entity != null && entity.hasEffect(ModEffects.RADIANCE) ? 1f : 0f);

		// Блоки с прозрачными пикселями (листва, саженец)
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_LEAVES, RenderType.cutoutMipped());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_SAPLING, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_DOOR, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TEARBLOOM, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOLDEN_SCARLET_LEAVES, RenderType.cutoutMipped());
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), ModBlocks.BLOODROOT, ModBlocks.FLESHBERRY,
				ModBlocks.INKROOT, ModBlocks.TARWEED, ModBlocks.WATERBLOOM, ModBlocks.BOGLEAF, ModBlocks.DUCKWEED,
				ModBlocks.PALE_GRASS, ModBlocks.TALL_PALE_GRASS, ModBlocks.SCARLET_GRASS, ModBlocks.TALL_SCARLET_GRASS,
				ModBlocks.INK_GRASS, ModBlocks.TALL_INK_GRASS, ModBlocks.SHORE_REED, ModBlocks.TALL_SHORE_REED);
		for (WoodSet set : new WoodSet[]{ModBlocks.SCARLET, ModBlocks.INK}) {
			BlockRenderLayerMap.INSTANCE.putBlock(set.leaves(), RenderType.cutoutMipped());
			BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), set.sapling(), set.door(), set.trapdoor());
		}
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_TEARBLOOM, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PALE_TRAPDOOR, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), ModBlocks.INK_LANTERN, ModBlocks.STONE_LANTERN, ModBlocks.ETERNAL_LANTERN);
	}
}
