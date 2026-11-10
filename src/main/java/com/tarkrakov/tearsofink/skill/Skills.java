package com.tarkrakov.tearsofink.skill;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Реестр навыков, выученные навыки игрока, перезарядки и запуск по клавише.
 * Новый навык: наследник {@link Skill} + {@link #register} в {@link ModSkills} + книга + клавиша.
 */
public final class Skills {
	private static final Map<ResourceLocation, Skill> REGISTRY = new LinkedHashMap<>();

	/** Выученные навыки — хранятся на игроке навсегда (переживают смерть и перезаход). */
	public static final AttachmentType<Set<ResourceLocation>> LEARNED = AttachmentRegistry.<Set<ResourceLocation>>builder()
			.persistent(ResourceLocation.CODEC.listOf().xmap(list -> Set.copyOf(list), set -> set.stream().toList()))
			.copyOnDeath()
			.initializer(Set::of)
			.buildAndRegister(TearsOfInk.id("learned_skills"));

	/** Перезарядки: игрок → навык → тик сервера, когда снова можно. В память, не сохраняются. */
	private static final Map<UUID, Map<ResourceLocation, Integer>> COOLDOWNS = new HashMap<>();

	private Skills() {}

	public static <T extends Skill> T register(T skill) {
		REGISTRY.put(skill.id(), skill);
		return skill;
	}

	public static Skill get(ResourceLocation id) {
		return REGISTRY.get(id);
	}

	public static Collection<Skill> all() {
		return Collections.unmodifiableCollection(REGISTRY.values());
	}

	public static void init() {
		PayloadTypeRegistry.playC2S().register(SkillPayloads.Use.TYPE, SkillPayloads.Use.CODEC);
		PayloadTypeRegistry.playS2C().register(SkillPayloads.NotEnough.TYPE, SkillPayloads.NotEnough.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SkillPayloads.Use.TYPE, (payload, context) ->
				context.player().server.execute(() -> use(context.player(), payload)));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> COOLDOWNS.remove(handler.getPlayer().getUUID()));
		ModSkills.init();
	}

	// ----- Выученные навыки -----
	public static boolean knows(ServerPlayer player, Skill skill) {
		return player.getAttachedOrCreate(LEARNED).contains(skill.id());
	}

	public static Set<ResourceLocation> learned(ServerPlayer player) {
		return player.getAttachedOrCreate(LEARNED);
	}

	/** @return {@code false}, если навык уже был выучен. */
	public static boolean learn(ServerPlayer player, Skill skill) {
		Set<ResourceLocation> set = learned(player);
		if (set.contains(skill.id())) return false;
		Set<ResourceLocation> copy = new HashSet<>(set);
		copy.add(skill.id());
		player.setAttached(LEARNED, Set.copyOf(copy));
		return true;
	}

	public static void forgetAll(ServerPlayer player) {
		player.setAttached(LEARNED, Set.of());
	}

	// ----- Запуск по клавише -----
	private static void use(ServerPlayer player, SkillPayloads.Use payload) {
		Skill skill = get(payload.skill());
		if (skill == null || !skill.isActive() || !knows(player, skill)) return;
		if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) return;

		InkData data = InkPowers.get(player);
		if (!data.inside()) {
			// снаружи силы спят — тихий «пшик»
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
			return;
		}

		int now = player.server.getTickCount();
		Map<ResourceLocation, Integer> cooldowns = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		if (cooldowns.getOrDefault(skill.id(), 0) > now) return;

		boolean creative = player.getAbilities().instabuild;
		if (!creative && (data.ink() < skill.inkCost() || data.soul() < skill.soulCost())) {
			ServerPlayNetworking.send(player, new SkillPayloads.NotEnough(data.soul() < skill.soulCost()));
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
			return;
		}

		SkillInput input = new SkillInput(payload.strafe(), payload.forward(), payload.sneaking());
		if (!skill.activate(player, input)) return;

		cooldowns.put(skill.id(), now + skill.cooldownTicks());
		if (!creative) {
			InkPowers.update(player, data.withInk(data.ink() - skill.inkCost()).withSoul(data.soul() - skill.soulCost()));
		}
	}
}
