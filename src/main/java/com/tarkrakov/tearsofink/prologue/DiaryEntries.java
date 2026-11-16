package com.tarkrakov.tearsofink.prologue;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.fountain.InkFountainBlock;
import com.tarkrakov.tearsofink.item.ModItems;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
	public static final String FIRST_STEPS = "first_steps";
	public static final String PALE_METAL = "pale_metal";
	public static final String FOUNTAIN_HUNT = "fountain_hunt";
	public static final String ASCENSION_FONT = "ascension_font";
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
		ServerPlayConnectionEvents.JOIN.register((h, s, server) -> {
			checkArrival(h.getPlayer()); // в том числе старые сохранения уже в Землях
			sync(h.getPlayer());
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p, origin, destination) -> checkArrival(p));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			checkArrival(newPlayer);
			sync(newPlayer);
		});
		// В Fabric 1.21.1 нет общего события получения предмета для всех способов
		// изменения инвентаря. Редкая серверная проверка охватывает подбор, крафт,
		// печь и перенос из контейнеров без нового Mixin.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % INVENTORY_CHECK_TICKS != 0) return;
			for (ServerPlayer p : server.getPlayerList().getPlayers()) checkGuidance(p);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getEntity() instanceof ServerPlayer p)) return;
			if (entity instanceof com.tarkrakov.tearsofink.entity.CaterpillarEntity c) {
				// гусеницы открываются по видам; общая часть — с первой бледной
				String kind = CATERPILLAR + "_" + c.variant().name().toLowerCase(java.util.Locale.ROOT);
				if (c.variant() == com.tarkrakov.tearsofink.entity.CaterpillarEntity.Variant.PALE) unlock(p, CATERPILLAR, kind);
				else unlock(p, kind);
				return;
			}
			ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
			if (CREATURES.contains(id.getPath())) unlock(p, id.getPath());
		});
	}

	private static void checkArrival(ServerPlayer p) {
		if (ModDimensions.isLands(p.level())) unlock(p, FIRST_STEPS);
	}

	/** Небоевые открытия на основе уже сохранённых посещений и текущих предметов. */
	private static void checkGuidance(ServerPlayer p) {
		List<String> have = p.getAttachedOrCreate(DATA);
		boolean needMetal = !have.contains(PALE_METAL);
		boolean needHunt = !have.contains(FOUNTAIN_HUNT);
		boolean needFont = !have.contains(ASCENSION_FONT);
		if (!needMetal && !needHunt && !needFont) return;

		List<String> discovered = new ArrayList<>(3);
		// Первое испитие фонтана даёт возможность призвать Торговку. Проверка
		// сохранённого VISITED открывает запись и для персонажей из старых миров.
		if (needHunt && !p.getAttachedOrCreate(InkFountainBlock.VISITED).isEmpty()) {
			discovered.add(FOUNTAIN_HUNT);
			needHunt = false;
		}

		var inventory = p.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (needMetal && (stack.is(ModItems.RAW_PALE_METAL) || stack.is(ModItems.PALE_INGOT))) {
				discovered.add(PALE_METAL);
				needMetal = false;
			}
			// Контракт или Пигмент также подсказывают, где найти Торговку,
			// если предмет попал к игроку до посещения фонтана.
			if (needHunt && (stack.is(ModItems.FULFILLED_CONTRACT) || stack.is(ModItems.PIGMENT))) {
				discovered.add(FOUNTAIN_HUNT);
				needHunt = false;
			}
			if (needFont && (stack.is(ModItems.PIGMENT) || stack.is(ModItems.VALOR_SIGIL)
					|| stack.is(ModItems.CRIMSON_FLOWER) || stack.is(ModItems.HOPE_MOTE))) {
				discovered.add(ASCENSION_FONT);
				needFont = false;
			}
			if (!needMetal && !needHunt && !needFont) break;
		}
		// За один цикл — один Sync и одно уведомление даже для нескольких страниц.
		if (!discovered.isEmpty()) unlock(p, discovered.toArray(String[]::new));
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
			p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9f, 0.7f);
		}
	}

	public static void sync(ServerPlayer p) {
		ServerPlayNetworking.send(p, new Sync(new ArrayList<>(p.getAttachedOrCreate(DATA))));
	}
}
