package com.tarkrakov.tearsofink.skill;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
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
	private static final Map<UUID, Map<ResourceLocation, Integer>> COOLDOWN_LENGTHS = new HashMap<>();

	public static int remaining(ServerPlayer p, Skill skill) {
		var map = COOLDOWNS.get(p.getUUID());
		return map == null ? 0 : Math.max(0, map.getOrDefault(skill.id(), 0) - p.server.getTickCount());
	}
	public static int cooldownLength(ServerPlayer p, Skill skill) {
		var map = COOLDOWN_LENGTHS.get(p.getUUID());
		return Math.max(1, map == null ? skill.cooldownTicks() : map.getOrDefault(skill.id(), skill.cooldownTicks()));
	}
	public static Skill mobility(ServerPlayer p) {
		return com.tarkrakov.tearsofink.vow.Vows.vow(p) == com.tarkrakov.tearsofink.vow.Vow.SOUL
				&& knows(p, ModSkills.STRIDE) ? ModSkills.STRIDE : ModSkills.DASH;
	}
	public static boolean resourceReady(ServerPlayer p, Skill skill) {
		if (p.getAbilities().instabuild) return true;
		InkData data = InkPowers.get(p);
		float mult = com.tarkrakov.tearsofink.weapon.forge.Awakening.costMult(weaponOf(p, skill));
		return data.ink() >= skill.inkCost() * mult && data.soul() >= skill.soulCost()
				&& (skill.healthCost() <= 0 || p.getHealth() > skill.healthCost() * mult);
	}

	/** Срезать оставшуюся перезарядку навыка: keep — какая доля остаётся (0,5 — вдвое короче). */
	public static void cutCooldown(ServerPlayer player, Skill skill, float keep) {
		Map<ResourceLocation, Integer> cd = COOLDOWNS.get(player.getUUID());
		if (cd == null || !cd.containsKey(skill.id())) return;
		int now = player.server.getTickCount();
		int left = cd.get(skill.id()) - now;
		if (left > 0) cd.put(skill.id(), now + Math.round(left * keep));
	}

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
		PayloadTypeRegistry.playS2C().register(SkillPayloads.Boost.TYPE, SkillPayloads.Boost.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SkillPayloads.Use.TYPE, (payload, context) ->
				context.player().server.execute(() -> use(context.player(), payload)));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			COOLDOWNS.remove(handler.getPlayer().getUUID());
			COOLDOWN_LENGTHS.remove(handler.getPlayer().getUUID());
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			COOLDOWNS.clear(); COOLDOWN_LENGTHS.clear();
		});
		CombatCooldowns.init();
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
		// одна клавиша — у каждого пути своё: на Пути Души рывок — Могучая поступь
		if (skill == ModSkills.DASH) skill = mobility(player);
		if (skill == null || !skill.isActive() || !knows(player, skill)) return;
		if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) return;

		activate(player, skill, new SkillInput(payload.strafe(), payload.forward(), payload.sneaking()));
	}

	/**
	 * Запустить навык или абилку оружия со всеми проверками: внутри Чернильных Земель,
	 * перезарядка, хватает ли Чернил/Души. Списывает цену и ставит перезарядку.
	 * @return сработал ли
	 */
	public static boolean activate(ServerPlayer player, Skill skill, SkillInput input) {
		if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) return false;
		InkData data = InkPowers.get(player);
		if (!data.inside()) {
			// снаружи силы спят — тихий «пшик»
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
			return false;
		}

		if (!com.tarkrakov.tearsofink.vow.Vows.allows(player, skill)) { // книга чужого пути спит
			com.tarkrakov.tearsofink.vow.Vows.asleep(player, skill, false);
			return false;
		}
		int now = player.server.getTickCount();
		Map<ResourceLocation, Integer> cooldowns = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		if (cooldowns.getOrDefault(skill.id(), 0) > now) return false;

		boolean creative = player.getAbilities().instabuild;
		// оружие приёма (Shift+R — левое) и его пробуждения: Скупость — дешевле, Поток — короче перезарядка
		ItemStack gearWeapon = weaponOf(player, skill);
		float costMult = com.tarkrakov.tearsofink.weapon.forge.Awakening.costMult(gearWeapon);
		float inkCost = skill.inkCost() * costMult, healthCost = skill.healthCost() * costMult;
		// Ранги Пути Чернил усиливают мобильность, но не снижают цену охотничьих приёмов.
		var inkRank = com.tarkrakov.tearsofink.vow.InkRank.active(player);
		if (skill == ModSkills.INK_JUMP && inkRank.atLeast(com.tarkrakov.tearsofink.vow.InkRank.TAINTED))
			inkCost = com.tarkrakov.tearsofink.ink.InkTuning.TAINTED_JUMP_COST * costMult;
		if (!creative && (data.ink() < inkCost || data.soul() < skill.soulCost())) {
			ServerPlayNetworking.send(player, new SkillPayloads.NotEnough(data.soul() < skill.soulCost()));
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
			return false;
		}

		if (!creative && healthCost > 0 && player.getHealth() <= healthCost) { // кровью платить нечем
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tears-of-ink.no_blood")
					.withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
			player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
			return false;
		}

		if (!com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.ownsSkill(skill)
				&& skill != ModSkills.INK_JUMP && skill != ModSkills.ASCENSION) {
			if ((skill == ModSkills.DASH || skill == ModSkills.STRIDE) && player.isFallFlying()) return false;
			if (skill == com.tarkrakov.tearsofink.weapon.WeaponAbilities.MERCY
					&& com.tarkrakov.tearsofink.weapon.WeaponAbilities.MERCY.lockoutRemaining(player) > 0) return false;
			com.tarkrakov.tearsofink.weapon.combat.CombatTransitions.begin(player, skill == ModSkills.STRIDE);
		}
		if (!skill.activate(player, input)) return false;

		if (!creative && healthCost > 0) { // заплатить кровью
			player.setHealth(Math.max(1f, player.getHealth() - healthCost));
			player.serverLevel().sendParticles(com.tarkrakov.tearsofink.particle.ModParticles.BLOOD_DRIP_FALL,
					player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.25, 0.4, 0.25, 0);
		}

		int baseCooldown = skill.cooldownTicks();
		if (skill == ModSkills.DASH) {
			if (inkRank.atLeast(com.tarkrakov.tearsofink.vow.InkRank.TAINTED))
				baseCooldown = com.tarkrakov.tearsofink.ink.InkTuning.TAINTED_DASH_COOLDOWN;
			else if (inkRank.atLeast(com.tarkrakov.tearsofink.vow.InkRank.MARKED))
				baseCooldown = com.tarkrakov.tearsofink.ink.InkTuning.MARKED_DASH_COOLDOWN;
		}
		int duration = Math.round(baseCooldown
				* com.tarkrakov.tearsofink.weapon.forge.Awakening.cooldownMult(gearWeapon));
		cooldowns.put(skill.id(), now + duration);
		COOLDOWN_LENGTHS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(skill.id(), duration);
		if (gearWeapon != null) { // приёмы оружия тупят клинок (Gear) и открывают окно «Разящего приёма»
			com.tarkrakov.tearsofink.weapon.forge.Gear.wear(player, gearWeapon, com.tarkrakov.tearsofink.weapon.forge.Gear.WEAR_ABILITY);
			com.tarkrakov.tearsofink.weapon.forge.Awakening.abilityUsed(player);
		}
		if (!creative) {
			InkData fresh = InkPowers.get(player);
			InkPowers.update(player, fresh.withInk(fresh.ink() - inkCost).withSoul(fresh.soul() - skill.soulCost()));
		}
		return true;
	}

	/** Оружие, чей это приём: Shift+R — левое, остальное — правое; null — навык не оружейный. */
	@org.jetbrains.annotations.Nullable
	private static ItemStack weaponOf(ServerPlayer player, Skill skill) {
		if (!skill.getClass().getName().contains(".weapon.")) return null;
		ItemStack off = player.getOffhandItem();
		boolean left = off.getItem() instanceof com.tarkrakov.tearsofink.weapon.AbilityWeapon aw && aw.ability() == skill
				&& !(player.getMainHandItem().getItem() instanceof com.tarkrakov.tearsofink.weapon.AbilityWeapon mw && mw.ability() == skill);
		return left ? off : player.getMainHandItem();
	}
}
