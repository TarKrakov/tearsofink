package com.tarkrakov.tearsofink.prologue;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.fountain.InkFountainBlock;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.vow.Vow;
import com.tarkrakov.tearsofink.vow.Vows;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.tarkrakov.tearsofink.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Записи Дневника: вступление (саквояж открыт), прибытие в Земли, получение
 * бледного металла, знакомство с фонтаном и Пигментом, первое убийство существ.
 * Тексты — diary.tears-of-ink.entry.&lt;id&gt;.title / .text в локализациях.
 * Новая запись о существе = добавить его id (имя сущности без мода) в {@link #CREATURES} и тексты.
 */
public final class DiaryEntries {
	/** О ком пишет дневник (первое убийство). */
	public static final List<String> CREATURES = List.of("pale_spearman", "pale_shieldbearer",
			"pale_crossbowman", "zombie", "skeleton", "spider");
	public static final String INTRO = "intro";
	/** The signed invitation is collected into the Diary; the physical letter was consumed at signing. */
	public static final String COLLECTION_INVITATION = "collection_invitation";
	public static final String FIRST_STEPS = "first_steps";
	public static final String PALE_METAL = "pale_metal";
	public static final String INK_ANVIL = "ink_anvil";
	public static final String CURSED_ALTAR = "cursed_altar";
	public static final String FOUNTAIN_HUNT = "fountain_hunt";
	public static final String ASCENSION_FONT = "ascension_font";
	/** Временные главы: автор заменит текст в ru_ru.json / en_us.json без смены ID. */
	public static final String DRAFT_FOG = "draft_fog";
	public static final String DRAFT_SOUL = "draft_soul";
	public static final String DRAFT_INK = "draft_ink";
	public static final String DRAFT_RUINS = "draft_ruins";
	public static final String DRAFT_MEMORY = "draft_memory";
	/** Проверка новых материалов и знаний не чаще раза в 5 секунд. */
	private static final int INVENTORY_CHECK_TICKS = 100;
	/** Глава о гусеницах: общая часть + страница на каждый вид (pale, blood, ink), открываются по убийствам. */
	public static final String CATERPILLAR = "caterpillar";
	public static final List<String> CATERPILLAR_PAGES = List.of("caterpillar", "caterpillar_pale", "caterpillar_blood", "caterpillar_ink");

	public static final AttachmentType<List<String>> DATA = AttachmentRegistry.<List<String>>builder()
			.persistent(Codec.STRING.listOf())
			.copyOnDeath()
			.initializer(List::of)
			.buildAndRegister(TearsOfInk.id("diary"));

	/** Read state is personal, persistent and independent of discovery. Page parts start at zero. */
	private static final AttachmentType<List<String>> READ = AttachmentRegistry.<List<String>>builder()
			.persistent(Codec.STRING.listOf()).copyOnDeath().initializer(List::of)
			.buildAndRegister(TearsOfInk.id("diary_read"));

	public record ReadSync(List<String> pages) implements CustomPacketPayload {
		public static final Type<ReadSync> TYPE = new Type<>(TearsOfInk.id("diary_read_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ReadSync> CODEC =
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(ReadSync::new, ReadSync::pages).cast();
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record ReadPage(String entry, int part) implements CustomPacketPayload {
		public static final Type<ReadPage> TYPE = new Type<>(TearsOfInk.id("diary_read_page"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ReadPage> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, ReadPage::entry, ByteBufCodecs.VAR_INT, ReadPage::part, ReadPage::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public static String readKey(String entry, int part) { return entry + "#" + part; }

	private static void markRead(ServerPlayer p, ReadPage message) {
		// Only the sender's unlocked diary entries can be acknowledged; bounded parts prevent unbounded storage.
		if (message.part() < 0 || message.part() >= 64 || !p.getAttachedOrCreate(DATA).contains(message.entry())) return;
		String key = readKey(message.entry(), message.part());
		List<String> have = p.getAttachedOrCreate(READ);
		if (have.contains(key)) return;
		List<String> next = new ArrayList<>(have);
		next.add(key);
		p.setAttached(READ, List.copyOf(next));
	}

	private static void syncRead(ServerPlayer p) {
		ServerPlayNetworking.send(p, new ReadSync(p.getAttachedOrCreate(READ)));
	}

	public record Sync(List<String> entries) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(TearsOfInk.id("diary_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(Sync::new, Sync::entries).cast();

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private DiaryEntries() {}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(Sync.TYPE, Sync.CODEC);
		PayloadTypeRegistry.playS2C().register(ReadSync.TYPE, ReadSync.CODEC);
		PayloadTypeRegistry.playC2S().register(ReadPage.TYPE, ReadPage.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ReadPage.TYPE, (message, context) ->
				context.server().execute(() -> markRead(context.player(), message)));
		FateProgress.init();
		ServerPlayConnectionEvents.JOIN.register((h, s, server) -> {
			checkArrival(h.getPlayer()); // в том числе старые сохранения уже в Землях
			sync(h.getPlayer());
			syncRead(h.getPlayer());
			FateProgress.refresh(h.getPlayer(), true);
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p, origin, destination) -> {
			checkArrival(p);
			FateProgress.refresh(p, true);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			checkArrival(newPlayer);
			sync(newPlayer);
			syncRead(newPlayer);
			FateProgress.refresh(newPlayer, true);
		});
		// В Fabric 1.21.1 нет общего события получения предмета для всех способов
		// изменения инвентаря. Редкая серверная проверка охватывает подбор, крафт,
		// печь и перенос из контейнеров без нового Mixin.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			if (tick % 20 != 0) return;
			boolean discover = tick % INVENTORY_CHECK_TICKS == 0;
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (discover) checkGuidance(p);
				FateProgress.refresh(p, false);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getEntity() instanceof ServerPlayer p) || !ModDimensions.isLands(p.level())) return;
			if (entity instanceof com.tarkrakov.tearsofink.entity.CaterpillarEntity c) {
				// гусеницы открываются по видам; общая часть — с первой бледной
				String kind = CATERPILLAR + "_" + c.variant().name().toLowerCase(java.util.Locale.ROOT);
				if (c.variant() == com.tarkrakov.tearsofink.entity.CaterpillarEntity.Variant.PALE) unlock(p, CATERPILLAR, kind);
				else unlock(p, kind);
				return;
			}
			ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
			if (CREATURES.contains(id.getPath()) && (id.getNamespace().equals("tears-of-ink") || id.getNamespace().equals("minecraft"))) {
				if (id.getPath().startsWith("pale_")) unlock(p, id.getPath(), DRAFT_MEMORY);
				else unlock(p, id.getPath());
			} else {
				List<String> discoveries = new ArrayList<>();
				DiaryDiscoveries.observe(entity, p.getAttachedOrCreate(DATA), discoveries);
				if (!discoveries.isEmpty()) unlock(p, discoveries.toArray(String[]::new));
			}
		});
	}

	private static void checkArrival(ServerPlayer p) {
		if (!ModDimensions.isLands(p.level())) return;
		// Backfill old characters: signing already consumed the physical invitation.
		if (com.tarkrakov.tearsofink.travel.Travel.get(p).stage() >=
				com.tarkrakov.tearsofink.travel.LandsTravel.STAGE_SIGNED)
			unlock(p, COLLECTION_INVITATION);
		Vow vow = Vows.vow(p);
		if (vow == Vow.SOUL) unlock(p, FIRST_STEPS, DRAFT_FOG, DRAFT_SOUL);
		else if (vow == Vow.INK) unlock(p, FIRST_STEPS, DRAFT_FOG, DRAFT_INK);
		else unlock(p, FIRST_STEPS, DRAFT_FOG);
	}

	/** Небоевые открытия на основе уже сохранённых посещений и текущих предметов. */
	private static void checkGuidance(ServerPlayer p) {
		if (!ModDimensions.isLands(p.level())) return;
		List<String> have = p.getAttachedOrCreate(DATA);
		boolean needMetal = !have.contains(PALE_METAL);
		boolean needAnvil = !have.contains(INK_ANVIL);
		boolean needAltar = !have.contains(CURSED_ALTAR);
		boolean needHunt = !have.contains(FOUNTAIN_HUNT);
		boolean needFont = !have.contains(ASCENSION_FONT);
		boolean needRuins = !have.contains(DRAFT_RUINS);
		Vow vow = Vows.vow(p);
		boolean needPath = (vow == Vow.SOUL && !have.contains(DRAFT_SOUL))
				|| (vow == Vow.INK && !have.contains(DRAFT_INK));
		boolean needFog = !have.contains(DRAFT_FOG);
		// Новые наблюдения проверяются независимо от старых уже известных глав.

		List<String> discovered = new ArrayList<>(8);
		if (com.tarkrakov.tearsofink.travel.FountainTravel.hasOpenedFountain(p)) {
			guide(have, discovered, PALE_METAL);
			guide(have, discovered, "draft_asphoda");
		}
		if (needFog) discovered.add(DRAFT_FOG);
		if (needPath) discovered.add(vow == Vow.SOUL ? DRAFT_SOUL : DRAFT_INK);
		if (needRuins && nearRuinMasonry(p)) discovered.add(DRAFT_RUINS);
		// Первое испитие фонтана открывает справку о дальнейшем знакомстве. Проверка
		// сохранённого VISITED открывает запись и для персонажей из старых миров.
		if (needHunt && (com.tarkrakov.tearsofink.travel.FountainTravel.hasOpenedFountain(p)
				|| !p.getAttachedOrCreate(InkFountainBlock.VISITED).isEmpty())) {
			discovered.add(FOUNTAIN_HUNT);
			needHunt = false;
		}

		var inventory = p.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (needMetal && !knows(have, discovered, PALE_METAL) && (stack.is(ModItems.RAW_PALE_METAL) || stack.is(ModItems.PALE_INGOT))) {
				discovered.add(PALE_METAL);
				needMetal = false;
			}
			if (needAnvil && stack.is(ModBlocks.INK_ANVIL.asItem())) {
				discovered.add(INK_ANVIL);
				needAnvil = false;
			}
			if (needAltar && (stack.is(ModBlocks.CURSED_ALTAR.asItem()) || stack.is(ModItems.MADNESS_SHARD)
					|| stack.is(ModItems.BLOOD_CLOT) || stack.is(ModItems.BLOOD_CRYSTAL) || stack.is(ModItems.INK_ECHO))) {
				discovered.add(CURSED_ALTAR);
				needAltar = false;
			}
			// Контракт или Пигмент также подсказывают, где найти Торговку,
			// если предмет попал к игроку до посещения фонтана.
			if (needHunt && (stack.is(ModItems.FULFILLED_CONTRACT) || stack.is(ModItems.PIGMENT))) {
				discovered.add(FOUNTAIN_HUNT);
				needHunt = false;
			}
			if (needFont && (stack.is(ModItems.PIGMENT) || stack.is(ModItems.FULFILLED_CONTRACT) || stack.is(ModItems.VALOR_SIGIL)
					|| stack.is(ModItems.CRIMSON_FLOWER) || stack.is(ModItems.HOPE_MOTE))) {
				discovered.add(ASCENSION_FONT);
				needFont = false;
			}
			if (!needMetal && !needAnvil && !needAltar && !needHunt && !needFont) break;
		}
		DiaryDiscoveries.collect(p, have, discovered);
		// Guidance is predictive: unlock the instructions when the relevant resource or
		// prerequisite becomes known, not only after the player makes the result.
		if (knows(have, discovered, PALE_METAL) || knows(have, discovered, INK_ANVIL)
				|| knows(have, discovered, "draft_armor") || knows(have, discovered, "draft_weaponcraft")
				|| knows(have, discovered, "draft_blood_metal") || knows(have, discovered, "draft_ink_metal")) {
			for (String id : List.of(PALE_METAL, "draft_asphoda", INK_ANVIL, "draft_weaponcraft", "draft_armor"))
				guide(have, discovered, id);
		}
		if (com.tarkrakov.tearsofink.trader.SoulTrader.introduced(p)) {
			for (String id : List.of(FOUNTAIN_HUNT, "draft_hunt_field", "draft_soul_bargain", "draft_mad_order"))
				guide(have, discovered, id);
		}
		if (knows(have, discovered, "draft_region_blood")) {
			guide(have, discovered, "draft_blood_metal");
			guide(have, discovered, "draft_blood_collector");
			guide(have, discovered, "draft_harvest");
		}
		if (knows(have, discovered, "draft_region_ink")) guide(have, discovered, "draft_ink_metal");
		if (knows(have, discovered, "draft_asphoda")) guide(have, discovered, "draft_alchemy");
		if (knows(have, discovered, "draft_blood_collector")) guide(have, discovered, CURSED_ALTAR);
		// За один цикл — один Sync и одно уведомление даже для нескольких страниц.
		if (!discovered.isEmpty()) unlock(p, discovered.toArray(String[]::new));
	}


	private static boolean knows(List<String> known, List<String> found, String id) {
		return known.contains(id) || found.contains(id);
	}

	private static void guide(List<String> known, List<String> found, String id) {
		if (!knows(known, found, id)) found.add(id);
	}

	/** Дешёвая однократная проверка ближайшей кладки; не требует искать структуры/загружать чанки. */
	private static boolean nearRuinMasonry(ServerPlayer p) {
		BlockPos c = p.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(c.offset(-6, -3, -6), c.offset(6, 3, 6))) {
			if (!p.serverLevel().hasChunkAt(pos)) continue;
			BlockState state = p.serverLevel().getBlockState(pos);
			if (state.is(ModBlocks.RUIN_BRICKS) || state.is(ModBlocks.CRACKED_RUIN_BRICKS)
					|| state.is(ModBlocks.OVERGROWN_RUIN_BRICKS) || state.is(ModBlocks.RUIN_TILES)
					|| state.is(ModBlocks.CRACKED_RUIN_TILES) || state.is(ModBlocks.RUIN_PILLAR)
					|| state.is(ModBlocks.CRACKED_RUIN_PILLAR) || state.is(ModBlocks.RUIN_PATTERN)
					|| state.is(ModBlocks.CRACKED_RUIN_PATTERN)) return true;
		}
		return false;
	}

	/** Discoveries from direct interaction with world content, never outside the Lands. */
	public static void discover(ServerPlayer p, String... entries) {
		if (ModDimensions.isLands(p.level()) && p.isAlive() && !p.isSpectator()) unlock(p, entries);
	}

	public static void unlock(ServerPlayer p, String... entries) {
		List<String> have = p.getAttachedOrCreate(DATA);
		Set<String> next = new LinkedHashSet<>(have);
		for (String e : entries) next.add(e);
		if (next.size() == have.size()) return;
		p.setAttached(DATA, List.copyOf(next));
		sync(p);
		if (!(entries.length == 1 && entries[0].equals(INTRO))) {
			p.sendSystemMessage(Component.translatable("message.tears-of-ink.diary_new").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			p.playNotifySound(ModSounds.DIARY_QUILL_WRITE, SoundSource.PLAYERS, 0.55f, 1.25f);
		}
	}

	public static void sync(ServerPlayer p) {
		ServerPlayNetworking.send(p, new Sync(new ArrayList<>(p.getAttachedOrCreate(DATA))));
	}
}
