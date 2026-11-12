package com.tarkrakov.tearsofink.prologue;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Записи Дневника: открываются по событиям. Сейчас — вступление (саквояж открыт)
 * и первое убийство каждого вида врага. Тексты — diary.tears-of-ink.entry.&lt;id&gt;.title / .text в ru_ru.json.
 * Новая запись о существе = добавить его id (имя сущности без мода) в {@link #CREATURES} и тексты.
 */
public final class DiaryEntries {
	/** О ком пишет дневник (первое убийство). */
	public static final List<String> CREATURES = List.of("caterpillar", "pale_spearman", "pale_shieldbearer",
			"pale_crossbowman", "zombie", "skeleton", "spider");
	public static final String INTRO = "intro";

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
		ServerPlayConnectionEvents.JOIN.register((h, s, server) -> sync(h.getPlayer()));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getEntity() instanceof ServerPlayer p)) return;
			ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
			if (CREATURES.contains(id.getPath())) unlock(p, id.getPath());
		});
	}

	public static void unlock(ServerPlayer p, String entry) {
		List<String> have = p.getAttachedOrCreate(DATA);
		if (have.contains(entry)) return;
		Set<String> next = new LinkedHashSet<>(have);
		next.add(entry);
		p.setAttached(DATA, List.copyOf(next));
		sync(p);
		if (!entry.equals(INTRO)) {
			p.sendSystemMessage(Component.translatable("message.tears-of-ink.diary_new").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9f, 0.7f);
		}
	}

	public static void sync(ServerPlayer p) {
		ServerPlayNetworking.send(p, new Sync(new ArrayList<>(p.getAttachedOrCreate(DATA))));
	}
}
