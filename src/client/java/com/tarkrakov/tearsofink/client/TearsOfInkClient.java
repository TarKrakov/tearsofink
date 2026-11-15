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
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.client.entity.CaterpillarModel;
import com.tarkrakov.tearsofink.client.entity.CaterpillarRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
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
		// /inkfog — выключить/включить туман Чернильных Земель у себя (удобно строить)
		net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("inkfog").executes(ctx -> {
					com.tarkrakov.tearsofink.client.fog.InkFog.disabled = !com.tarkrakov.tearsofink.client.fog.InkFog.disabled;
					ctx.getSource().sendFeedback(net.minecraft.network.chat.Component.translatable(
							com.tarkrakov.tearsofink.client.fog.InkFog.disabled ? "message.tears-of-ink.fog_off" : "message.tears-of-ink.fog_on"));
					return 1;
				})));
		ParticleFactoryRegistry.getInstance().register(ModParticles.FOG_BANK, FogBankParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.FOG_WALL, FogBankParticle.WallProvider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.TEAR_GLINT, TearGlintParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.SALVATION_STAR, TearGlintParticle.Provider::new);
		ParticleFactoryRegistry particles = ParticleFactoryRegistry.getInstance();
		particles.register(ModParticles.BLOOD_DRIP_HANG, s -> new DripParticle.Provider(s, DripParticle.Phase.HANG, ModParticles.BLOOD_DRIP_FALL));
		particles.register(ModParticles.BLOOD_DRIP_FALL, s -> new DripParticle.Provider(s, DripParticle.Phase.FALL, ModParticles.BLOOD_DRIP_LAND));
		particles.register(ModParticles.BLOOD_DRIP_LAND, s -> new DripParticle.Provider(s, DripParticle.Phase.LAND, null));
		particles.register(ModParticles.INK_DRIP_HANG, s -> new DripParticle.Provider(s, DripParticle.Phase.HANG, ModParticles.INK_DRIP_FALL));
		particles.register(ModParticles.INK_DRIP_FALL, s -> new DripParticle.Provider(s, DripParticle.Phase.FALL, ModParticles.INK_DRIP_LAND));
		particles.register(ModParticles.INK_DRIP_LAND, s -> new DripParticle.Provider(s, DripParticle.Phase.LAND, null));
		ClientTickEvents.END_CLIENT_TICK.register(FogBankSpawner::tick);
		ClientTickEvents.END_CLIENT_TICK.register(com.tarkrakov.tearsofink.client.fog.FogWallSpawner::tick);
		ParticleFactoryRegistry.getInstance().register(ModParticles.FLAME_DANCE, FlameDanceParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.INK_SMOKE, SheetParticles.SmokeProvider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.EMBER, SheetParticles.EmberProvider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.INK_SPARK, SheetParticles.InkSparkProvider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.PAPER_SCRAP, SheetParticles.PaperProvider::new);
		ClientTickEvents.END_CLIENT_TICK.register(FlameDanceSpawner::tick);
		ClientTickEvents.END_CLIENT_TICK.register(com.tarkrakov.tearsofink.client.fog.HeraldDrips::tick);

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
		EntityRendererRegistry.register(ModEntities.BLOOD_ARC, NoopRenderer::new);
		EntityRendererRegistry.register(ModEntities.INK_BLOT, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
		// ----- пролог -----
		EntityModelLayerRegistry.registerModelLayer(com.tarkrakov.tearsofink.client.prologue.GuideModel.LAYER,
				com.tarkrakov.tearsofink.client.prologue.GuideModel::createBodyLayer);
		EntityRendererRegistry.register(ModMobs.GUIDE, com.tarkrakov.tearsofink.client.prologue.GuideRenderer::new);
		com.tarkrakov.tearsofink.prologue.PrologueItems.openInvitation = p ->
				net.minecraft.client.Minecraft.getInstance().setScreen(new com.tarkrakov.tearsofink.client.prologue.InvitationScreen());
		com.tarkrakov.tearsofink.prologue.PrologueItems.openDiary = p ->
				net.minecraft.client.Minecraft.getInstance().setScreen(new com.tarkrakov.tearsofink.client.prologue.DiaryScreen());
		ClientPlayNetworking.registerGlobalReceiver(com.tarkrakov.tearsofink.prologue.Prologue.GateTransit.TYPE, (payload, context) ->
				context.client().execute(() -> com.tarkrakov.tearsofink.client.prologue.GateOverlay.start(payload.ticks())));
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(c -> com.tarkrakov.tearsofink.client.prologue.GateOverlay.tick());
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((g, counter) ->
				com.tarkrakov.tearsofink.client.prologue.GateOverlay.render(g, counter.getGameTimeDeltaPartialTick(false)));
		ClientPlayNetworking.registerGlobalReceiver(com.tarkrakov.tearsofink.hunt.HuntContracts.BoardData.TYPE, (payload, context) ->
				context.client().execute(() -> com.tarkrakov.tearsofink.client.trader.SoulTraderScreen.board(payload)));
		ClientPlayNetworking.registerGlobalReceiver(com.tarkrakov.tearsofink.weapon.blood.Parry.Lock.TYPE, (payload, context) -> context.client().execute(() -> {
			com.tarkrakov.tearsofink.weapon.blood.Parry.CLIENT_LOCK = payload.on();
			var pl = context.client().player;
			if (payload.on() && pl != null && pl.isUsingItem()) context.client().gameMode.releaseUsingItem(pl); // щит опускается
		}));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> com.tarkrakov.tearsofink.weapon.blood.Parry.CLIENT_LOCK = false);
		ClientPlayNetworking.registerGlobalReceiver(SkillPayloads.Boost.TYPE, (payload, context) -> context.client().execute(() -> {
			var pl = context.client().player;
			if (pl == null) return;
			var v = pl.getDeltaMovement();
			pl.setDeltaMovement(v.x, payload.up(), v.z);
			pl.fallDistance = 0;
		}));
		ClientPlayNetworking.registerGlobalReceiver(com.tarkrakov.tearsofink.vow.Vows.Sync.TYPE, (payload, context) ->
				context.client().execute(() -> com.tarkrakov.tearsofink.vow.Vow.CLIENT = com.tarkrakov.tearsofink.vow.Vow.byIndex(payload.vow())));
		ClientPlayNetworking.registerGlobalReceiver(com.tarkrakov.tearsofink.vow.Vows.Open.TYPE, (payload, context) ->
				context.client().execute(com.tarkrakov.tearsofink.client.vow.VowScreen::open));
		ClientPlayNetworking.registerGlobalReceiver(com.tarkrakov.tearsofink.trader.SoulTrader.Open.TYPE, (payload, context) ->
				context.client().execute(() -> com.tarkrakov.tearsofink.client.trader.SoulTraderScreen.open(payload)));
		EntityModelLayerRegistry.registerModelLayer(com.tarkrakov.tearsofink.client.trader.SoulTraderModel.LAYER,
				com.tarkrakov.tearsofink.client.trader.SoulTraderModel::createBodyLayer);
		EntityRendererRegistry.register(ModMobs.SOUL_TRADER, com.tarkrakov.tearsofink.client.trader.SoulTraderRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(com.tarkrakov.tearsofink.client.hunt.HunterModel.LAYER,
				com.tarkrakov.tearsofink.client.hunt.HunterModel::createBodyLayer);
		EntityRendererRegistry.register(ModMobs.HUNTER, com.tarkrakov.tearsofink.client.hunt.HunterRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(com.tarkrakov.tearsofink.prologue.DiaryEntries.Sync.TYPE, (payload, context) ->
				context.client().execute(() -> com.tarkrakov.tearsofink.client.prologue.DiaryScreen.entries = new java.util.ArrayList<>(payload.entries())));

		// Фауна
		EntityModelLayerRegistry.registerModelLayer(CaterpillarModel.LAYER, CaterpillarModel::createBodyLayer);
		EntityRendererRegistry.register(ModMobs.CATERPILLAR, CaterpillarRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(com.tarkrakov.tearsofink.client.entity.PaleSpearmanModel.LAYER,
				com.tarkrakov.tearsofink.client.entity.PaleSpearmanModel::createBodyLayer);
		EntityRendererRegistry.register(ModMobs.PALE_SPEARMAN, com.tarkrakov.tearsofink.client.entity.PaleSpearmanRenderer::new);
		// щитовик и арбалетчик — модели автора
		EntityModelLayerRegistry.registerModelLayer(com.tarkrakov.tearsofink.client.entity.PaleShieldbearerModel.LAYER,
				com.tarkrakov.tearsofink.client.entity.PaleShieldbearerModel::createBodyLayer);
		EntityModelLayerRegistry.registerModelLayer(com.tarkrakov.tearsofink.client.entity.PaleCrossbowmanModel.LAYER,
				com.tarkrakov.tearsofink.client.entity.PaleCrossbowmanModel::createBodyLayer);
		EntityRendererRegistry.register(ModMobs.PALE_SHIELDBEARER, com.tarkrakov.tearsofink.client.entity.PaleShieldbearerRenderer::new);
		EntityRendererRegistry.register(ModMobs.PALE_CROSSBOWMAN, com.tarkrakov.tearsofink.client.entity.PaleCrossbowmanRenderer::new);
		EntityRendererRegistry.register(ModMobs.PALE_BOLT, com.tarkrakov.tearsofink.client.entity.PaleBoltRenderer::new);
		// Кровосборник: помпа рисуется моделью с анимацией
		EntityModelLayerRegistry.registerModelLayer(com.tarkrakov.tearsofink.client.blood.BloodPumpModel.LAYER,
				com.tarkrakov.tearsofink.client.blood.BloodPumpModel::createBodyLayer);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.tarkrakov.tearsofink.block.lantern.ModBlockEntities.BLOOD_PUMP,
				com.tarkrakov.tearsofink.client.blood.BloodPumpRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
				com.tarkrakov.tearsofink.block.ModBlocks.BLOOD_PUMP, new com.tarkrakov.tearsofink.client.blood.BloodPumpItemRenderer());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DRYING_INK_PUDDLE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.VESTIBULE_GATE, RenderType.translucent());
		ShieldIcons.init();
		com.tarkrakov.tearsofink.client.combat.CombatClient.init();
		com.tarkrakov.tearsofink.client.hunt.HuntClient.init();
		com.tarkrakov.tearsofink.client.shield.ShieldStanceClient.init();
		com.tarkrakov.tearsofink.client.vow.AuraClient.init();
		for (Item shield : new Item[]{ModItems.RAM_SHIELD, ModItems.RADIANT_SHIELD, ModItems.MERCY_SHIELD, ModItems.REPEL_SHIELD,
				ModItems.WORN_RAM_SHIELD, ModItems.WORN_MERCY_SHIELD}) {
			ItemProperties.register(shield, ResourceLocation.withDefaultNamespace("blocking"),
					(stack, level, entity, seed) -> com.tarkrakov.tearsofink.client.shield.ShieldStanceClient.blocking(stack, entity) ? 1f : 0f);
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
