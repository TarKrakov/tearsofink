package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.ModMobs;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.sanctuary.Sanctuary;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Охота (PvE): раз в 15 минут на весь сервер. Рядом со случайным игроком (не в храме) появляется меченая цель
 * (крепче, с лучом в небо) и охотник. Охотник ждёт игроков в стороне, не дождался — дерётся сам.
 * Контракт получает каждый, кто нанёс ≥ 10% урона игроков. Цель растерзала охотника — награда двойная.
 * Если рядом с целью никого — исход разыгрывается «вдали», в цифрах.
 */
public final class Hunt {
	public static final String PREY_TAG = "tearsofink.hunt_prey";
	private static final ResourceKey<LootTable> REWARD = ResourceKey.create(Registries.LOOT_TABLE, TearsOfInk.id("gameplay/hunt_reward"));
	private static final ResourceLocation PREY_BOOST = TearsOfInk.id("hunt_prey");
	/** Цель — не выше и не ниже игрока больше чем на столько блоков. */
	private static final int HEIGHT_GAP = 12;
	private static final String C_LINE = "HunterLine", C_TIER = "HunterTier", C_KIND = "HunterKind";

	/** Луч Охоты и затемнение цели у клиентов. on=false — Охоты нет. */
	public record Beam(boolean on, int entity, double x, double y, double z, int color, boolean engaged) implements CustomPacketPayload {
		public static final Type<Beam> TYPE = new Type<>(TearsOfInk.id("hunt_beam"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Beam> CODEC = StreamCodec.of((buf, b) -> {
			buf.writeBoolean(b.on());
			buf.writeVarInt(b.entity());
			buf.writeDouble(b.x());
			buf.writeDouble(b.y());
			buf.writeDouble(b.z());
			buf.writeInt(b.color());
			buf.writeBoolean(b.engaged());
		}, buf -> new Beam(buf.readBoolean(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
				buf.readInt(), buf.readBoolean()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class Active {
		UUID prey, hunter;
		HunterType type;
		int rank;
		ItemStack weapon;
		long end, engageAt;
		boolean engaged, hunterDead;
		/** Охотник победил, Чернильный бесится до этого времени. */
		long frenzyUntil = -1;
		double x, y, z;
		final Map<UUID, Float> damage = new HashMap<>();
		float hunterDamage;
	}

	@Nullable
	private static Active active;
	private static long nextStart = -1;

	private Hunt() {}

	public static void init() {
		HuntTargets.init();
		PayloadTypeRegistry.playS2C().register(Beam.TYPE, Beam.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Hunt::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			active = null;
			nextStart = -1;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			Active a = active;
			if (a != null && entity.getUUID().equals(a.prey)) {
				Entity by = source.getEntity();
				if (by instanceof ServerPlayer p) a.damage.merge(p.getUUID(), Math.min(amount, entity.getHealth()), Float::sum);
				else if (by instanceof HunterEntity) a.hunterDamage += Math.min(amount, entity.getHealth());
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(Hunt::onDeath);
		// цель Охоты, оставшаяся в мире после перезапуска или выгрузки, — растворяется
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!entity.getTags().contains(PREY_TAG)) return;
			Active a = active;
			if (a == null || !entity.getUUID().equals(a.prey)) entity.discard();
		});
	}

	public static boolean running() {
		return active != null;
	}

	// ===== Запуск =====

	private static void tick(MinecraftServer server) {
		ServerLevel lands = ModDimensions.lands(server);
		if (lands == null) return;
		long now = server.getTickCount();
		if (nextStart < 0) nextStart = now + HuntTuning.FIRST_DELAY;
		Active a = active;
		if (a == null) {
			if (now >= nextStart && now % 20 == 0) start(lands, null);
			return;
		}
		update(lands, a, now);
	}

	/** Начать Охоту. who — рядом с кем (null — случайный подходящий игрок). Ответ — удалось ли. */
	public static boolean start(ServerLevel lands, @Nullable ServerPlayer who) {
		if (active != null) return false;
		List<ServerPlayer> candidates = new ArrayList<>();
		if (who != null) candidates.add(who);
		else for (ServerPlayer p : lands.players()) {
			if (p.isAlive() && !p.isSpectator() && !Sanctuary.contains(p)) candidates.add(p);
		}
		if (candidates.isEmpty()) return false; // все в храмах — ждём
		RandomSource random = lands.random;
		ServerPlayer target = candidates.get(random.nextInt(candidates.size()));
		HuntTargets.Target t = HuntTargets.pick(random);
		if (t == null) return false;
		BlockPos at = spot(lands, target, random);
		if (at == null) return false;
		Entity e = t.type().create(lands);
		if (!(e instanceof Mob prey)) {
			if (e != null) e.discard();
			return false;
		}
		int rank = pickRank(random);
		HunterType type = HunterType.byIndex(random.nextInt(HunterType.values().length));
		prey.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
		prey.finalizeSpawn(lands, lands.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
		prey.setPersistenceRequired();
		prey.addTag(PREY_TAG);
		boost(prey, Attributes.MAX_HEALTH, HuntTuning.PREY_HP[rank]);
		boost(prey, Attributes.ATTACK_DAMAGE, HuntTuning.PREY_DAMAGE[rank]);
		prey.setHealth(prey.getMaxHealth());

		// оружие: один меч / пара одинаковых / клинок + двуручник в левой (правило рук игрока)
		HunterWeaponItem great = (HunterWeaponItem) ModItems.HUNTER_GREATSWORD, saber = (HunterWeaponItem) ModItems.HUNTER_SABER;
		HunterWeaponItem kind = random.nextBoolean() ? great : saber;
		ItemStack weapon = kind.make(type, rank), offWeapon = ItemStack.EMPTY;
		int lw = 0;
		for (int w : HuntTuning.LOADOUT_WEIGHT) lw += w;
		int roll = random.nextInt(lw);
		if (roll >= HuntTuning.LOADOUT_WEIGHT[0] + HuntTuning.LOADOUT_WEIGHT[1]) { // разные
			weapon = saber.make(type, rank);
			offWeapon = great.make(type, rank);
		} else if (roll >= HuntTuning.LOADOUT_WEIGHT[0]) { // пара одинаковых
			offWeapon = kind.make(type, rank);
		}
		HunterEntity hunter = ModMobs.HUNTER.create(lands);
		if (hunter == null) {
			prey.discard();
			return false;
		}
		BlockPos hp = near(lands, at, target, random);
		hunter.moveTo(hp.getX() + 0.5, hp.getY(), hp.getZ() + 0.5, 0, 0);
		hunter.setup(type, rank, weapon, offWeapon);
		hunter.setFoe(prey, false);

		Active n = new Active();
		n.prey = prey.getUUID();
		n.hunter = hunter.getUUID();
		n.type = type;
		n.rank = rank;
		n.weapon = (!offWeapon.isEmpty() && random.nextBoolean() ? offWeapon : weapon).copy(); // контракт помнит одно из двух
		long now = lands.getServer().getTickCount();
		n.end = now + HuntTuning.DURATION;
		n.engageAt = now + HuntTuning.HUNTER_WAIT;
		n.x = prey.getX();
		n.y = prey.getY();
		n.z = prey.getZ();
		active = n; // до появления в мире: иначе цель «чужая» и растворяется сразу
		if (!lands.addFreshEntity(prey)) {
			active = null;
			hunter.discard();
			return false;
		}
		lands.addFreshEntity(hunter);
		Component arms = offWeapon.isEmpty()
				? Component.translatable("hunt.tears-of-ink.weapon_one", weapon.getHoverName())
				: Component.translatable("hunt.tears-of-ink.weapon_two", weapon.getHoverName(), offWeapon.getHoverName());
		announce(lands, Component.translatable("hunt.tears-of-ink.start", hunter.getCustomName(), arms,
				preyName(prey), target.getDisplayName()).withStyle(ChatFormatting.GOLD));
		for (ServerPlayer p : lands.players()) {
			p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.5f, 1.3f);
		}
		return true;
	}

	private static int pickRank(RandomSource random) {
		int total = 0;
		for (int w : HuntTuning.RANK_WEIGHT) total += w;
		int r = random.nextInt(total);
		for (int i = 0; i < HuntTuning.RANK_WEIGHT.length; i++) {
			r -= HuntTuning.RANK_WEIGHT[i];
			if (r < 0) return i;
		}
		return 0;
	}

	private static void boost(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, float mult) {
		AttributeInstance inst = e.getAttribute(attr);
		if (inst == null) return;
		inst.addPermanentModifier(new AttributeModifier(PREY_BOOST, mult - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	public static Component preyName(Entity prey) {
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(prey.getType());
		String key = "hunt.tears-of-ink.prey." + id.getNamespace() + "." + id.getPath();
		return Component.translatableWithFallback(key, prey.getType().getDescription().getString());
	}

	@Nullable
	private static BlockPos spot(ServerLevel level, ServerPlayer player, RandomSource random) {
		InkLandsLayout layout = InkLandsLayout.of(level);
		for (int attempt = 0; attempt < 48; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			// маленькая дальность прорисовки мира — понемногу ближе (цель должна «жить», а не стоять замороженной)
			double shrink = attempt < 24 ? 1 : 0.7;
			double d = (HuntTuning.SPAWN_MIN + random.nextDouble() * (HuntTuning.SPAWN_MAX - HuntTuning.SPAWN_MIN)) * shrink;
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * d), z = (int) Math.floor(player.getZ() + Math.sin(angle) * d);
			BlockPos ground = ground(level, x, z);
			if (ground == null || !level.isPositionEntityTicking(ground)) continue;
			if (Math.abs(ground.getY() - player.getY()) > HEIGHT_GAP) continue; // примерно на уровне игрока
			if (layout != null && (layout.depthInside(x, z) < 20 || Sanctuary.near(layout, x, z, 8))) continue;
			return ground;
		}
		return null;
	}

	/** Место для охотника — в стороне от цели, подальше от игрока. */
	private static BlockPos near(ServerLevel level, BlockPos prey, ServerPlayer player, RandomSource random) {
		double away = Math.atan2(prey.getZ() - player.getZ(), prey.getX() - player.getX());
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = away + (random.nextDouble() - 0.5) * Math.PI;
			double d = HuntTuning.WAIT_MIN + random.nextDouble() * (HuntTuning.WAIT_MAX - HuntTuning.WAIT_MIN);
			BlockPos g = ground(level, (int) Math.floor(prey.getX() + Math.cos(angle) * d), (int) Math.floor(prey.getZ() + Math.sin(angle) * d));
			if (g != null && Math.abs(g.getY() - prey.getY()) <= 4) return g; // охотник — на одном уровне с целью
		}
		return prey;
	}

	/** Настоящая земля: сверху вниз, мимо крон и веток (на дереве цель недосягаема). */
	@Nullable
	private static BlockPos ground(ServerLevel level, int x, int z) {
		BlockPos col = new BlockPos(x, 0, z);
		if (!level.hasChunkAt(col)) return null;
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		for (int y = top; y > top - 48 && y > level.getMinBuildHeight(); y--) {
			BlockPos at = new BlockPos(x, y, z);
			var below = level.getBlockState(at.below());
			if (below.getCollisionShape(level, at.below()).isEmpty()) continue;
			if (below.is(net.minecraft.tags.BlockTags.LOGS) || below.is(net.minecraft.tags.BlockTags.LEAVES)) continue; // ветка — ниже
			if (!level.getFluidState(at.below()).isEmpty() || !level.getFluidState(at).isEmpty()) return null;
			if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()
					|| !level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) continue;
			if (!level.canSeeSky(at) && y < top - 3 && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, at) == 0) return null; // пещера
			return at;
		}
		return null;
	}

	// ===== Ход Охоты =====

	private static void update(ServerLevel lands, Active a, long now) {
		Entity preyE = lands.getEntity(a.prey);
		Entity hunterE = lands.getEntity(a.hunter);
		LivingEntity prey = preyE instanceof LivingEntity l && l.isAlive() ? l : null;
		HunterEntity hunter = hunterE instanceof HunterEntity h && h.isAlive() ? h : null;

		if (a.frenzyUntil >= 0) { // охотник уже победил
			if (hunter == null || now >= a.frenzyUntil) finish(lands, hunter, null);
			else if (now % 20 == 0) {
				ServerPlayer near = hunter.level().getNearestPlayer(hunter, 32) instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator()
						&& !Sanctuary.contains(sp) ? sp : null;
				hunter.setFoe(near, true);
			}
			return;
		}
		if (prey == null) { // цель пропала (выгрузилась) — исход вдали
			remote(lands, a, hunter);
			return;
		}
		a.x = prey.getX();
		a.y = prey.getY();
		a.z = prey.getZ();
		if (now >= a.end) {
			announce(lands, Component.translatable("hunt.tears-of-ink.timeout").withStyle(ChatFormatting.GRAY));
			vanish(lands, prey);
			finish(lands, hunter, prey);
			return;
		}
		boolean playerNear = false, anyoneClose = false;
		for (ServerPlayer p : lands.players()) {
			if (p.isSpectator()) continue;
			double d = p.distanceTo(prey);
			if (d < HuntTuning.PLAYER_ENGAGE) playerNear = true;
			if (d < HuntTuning.REMOTE) anyoneClose = true;
		}
		if (!anyoneClose && now % 20 == 0) {
			remote(lands, a, hunter);
			return;
		}
		if (hunter != null) {
			if (!a.engaged && (playerNear || now >= a.engageAt)) {
				a.engaged = true;
				announce(lands, Component.translatable("hunt.tears-of-ink.engage", hunter.getCustomName()).withStyle(ChatFormatting.GRAY));
			}
			hunter.setFoe(prey, a.engaged);
		}
		if (now % 10 == 0) {
			Beam beam = new Beam(true, prey.getId(), a.x, a.y, a.z, a.type.beamColor, a.engaged);
			for (ServerPlayer p : lands.players()) ServerPlayNetworking.send(p, beam);
		}
		if (now % 4 == 0) lands.sendParticles(ParticleTypes.SQUID_INK, a.x, a.y + prey.getBbHeight() * 0.6, a.z, 2, 0.3, 0.4, 0.3, 0.01);
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		Active a = active;
		if (a == null || !(entity.level() instanceof ServerLevel lands)) return;
		if (entity.getUUID().equals(a.hunter)) {
			if (a.frenzyUntil >= 0) { // Чернильного в бешенстве одолели игроки
				announce(lands, Component.translatable("hunt.tears-of-ink.frenzy_end").withStyle(ChatFormatting.GOLD));
				finish(lands, null, null);
				return;
			}
			a.hunterDead = true;
			announce(lands, Component.translatable("hunt.tears-of-ink.devoured").withStyle(ChatFormatting.DARK_RED));
			return;
		}
		if (!entity.getUUID().equals(a.prey)) return;
		HunterEntity hunter = lands.getEntity(a.hunter) instanceof HunterEntity h && h.isAlive() ? h : null;
		float players = 0;
		for (float f : a.damage.values()) players += f;
		boolean hunterWon = source.getEntity() instanceof HunterEntity && players < a.hunterDamage;
		if (hunterWon || players <= 0) {
			hunterWins(lands, a, hunter);
			return;
		}
		List<Component> names = new ArrayList<>();
		for (var e : a.damage.entrySet()) {
			if (e.getValue() < players * HuntTuning.CONTRIBUTION) continue;
			ServerPlayer p = lands.getServer().getPlayerList().getPlayer(e.getKey());
			if (p == null) continue;
			reward(lands, p, a, entity);
			names.add(p.getDisplayName());
		}
		MutableComponent list = Component.empty();
		for (int i = 0; i < names.size(); i++) {
			if (i > 0) list.append(", ");
			list.append(names.get(i));
		}
		announce(lands, Component.translatable(a.hunterDead ? "hunt.tears-of-ink.won_double" : "hunt.tears-of-ink.won", list)
				.withStyle(ChatFormatting.GOLD));
		finish(lands, hunter, null);
	}

	private static void hunterWins(ServerLevel lands, Active a, @Nullable HunterEntity hunter) {
		Component name = hunter != null ? hunter.getCustomName() : Component.translatable("hunt.tears-of-ink.type." + a.type.id);
		announce(lands, Component.translatable("hunt.tears-of-ink.hunter_won", name).withStyle(ChatFormatting.GRAY));
		if (hunter != null && a.type == HunterType.INK) { // Чернильный, убив цель, бросается на живых
			a.frenzyUntil = lands.getServer().getTickCount() + HuntTuning.INK_FRENZY;
			announce(lands, Component.translatable("hunt.tears-of-ink.frenzy", name).withStyle(ChatFormatting.DARK_PURPLE));
			for (ServerPlayer p : lands.players()) ServerPlayNetworking.send(p, new Beam(false, -1, 0, 0, 0, 0, false));
			return;
		}
		finish(lands, hunter, null);
	}

	private static void remote(ServerLevel lands, Active a, @Nullable HunterEntity hunter) {
		Entity prey = lands.getEntity(a.prey);
		boolean hunterWins = !a.hunterDead && lands.random.nextFloat() < HuntTuning.REMOTE_HUNTER_WINS[a.rank];
		Component name = Component.translatable("hunt.tears-of-ink.hunter_name",
				Component.translatable("hunt.tears-of-ink.type." + a.type.id), Component.translatable("hunt.tears-of-ink.rank." + a.rank));
		announce(lands, Component.translatable(hunterWins ? "hunt.tears-of-ink.remote_hunter" : "hunt.tears-of-ink.remote_prey", name)
				.withStyle(ChatFormatting.GRAY));
		if (prey != null) prey.discard();
		finish(lands, hunter, null);
	}

	private static void finish(ServerLevel lands, @Nullable HunterEntity hunter, @Nullable Entity prey) {
		if (hunter != null) vanish(lands, hunter);
		if (prey != null && prey.isAlive()) prey.discard();
		active = null;
		nextStart = lands.getServer().getTickCount() + HuntTuning.INTERVAL;
		for (ServerPlayer p : lands.players()) ServerPlayNetworking.send(p, new Beam(false, -1, 0, 0, 0, 0, false));
	}

	/** Растворяется в чернилах. */
	private static void vanish(ServerLevel level, Entity e) {
		level.sendParticles(ParticleTypes.SQUID_INK, e.getX(), e.getY() + 1, e.getZ(), 30, 0.4, 0.8, 0.4, 0.03);
		level.playSound(null, e.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 1f, 0.6f);
		e.discard();
	}

	/** Остановить Охоту командой. */
	public static void stop(ServerLevel lands) {
		Active a = active;
		if (a == null) return;
		Entity prey = lands.getEntity(a.prey);
		Entity hunter = lands.getEntity(a.hunter);
		finish(lands, hunter instanceof HunterEntity h ? h : null, prey);
	}

	/** Сдвинуть следующую Охоту (команда). */
	public static void scheduleIn(MinecraftServer server, int ticks) {
		nextStart = server.getTickCount() + ticks;
	}

	public static long ticksUntilNext(MinecraftServer server) {
		return active != null ? 0 : Math.max(0, nextStart - server.getTickCount());
	}

	// ===== Награда =====

	private static void reward(ServerLevel lands, ServerPlayer p, Active a, LivingEntity prey) {
		int contracts = a.hunterDead ? 2 : 1; // цель растерзала охотника — ещё один контракт
		for (int i = 0; i < contracts; i++) give(p, contract(a.weapon));
		LootTable table = lands.getServer().reloadableRegistries().getLootTable(REWARD);
		LootParams params = new LootParams.Builder(lands)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, prey.position())
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, p)
				.create(LootContextParamSets.GIFT);
		for (ItemStack s : table.getRandomItems(params)) give(p, s);
		p.sendSystemMessage(Component.translatable("hunt.tears-of-ink.your_reward", contracts).withStyle(ChatFormatting.GOLD));
		p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 0.8f);
	}

	private static void give(ServerPlayer p, ItemStack s) {
		if (!p.getInventory().add(s)) p.drop(s, false);
	}

	/** Исполненный контракт помнит оружие охотника той Охоты (Торговка может его отдать). */
	public static ItemStack contract(ItemStack weapon) {
		ItemStack c = new ItemStack(ModItems.FULFILLED_CONTRACT);
		CompoundTag tag = new CompoundTag();
		tag.putString(C_KIND, BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString());
		tag.putInt(C_LINE, HunterWeaponItem.line(weapon).ordinal());
		tag.putInt(C_TIER, HunterWeaponItem.tier(weapon));
		c.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return c;
	}

	/** Оружие охотника из контракта (или пусто, если контракт без памяти). */
	public static ItemStack weaponOf(ItemStack contract) {
		CustomData d = contract.get(DataComponents.CUSTOM_DATA);
		if (d == null) return ItemStack.EMPTY;
		CompoundTag tag = d.copyTag();
		if (!tag.contains(C_KIND)) return ItemStack.EMPTY;
		var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString(C_KIND)));
		if (!(item instanceof HunterWeaponItem w)) return ItemStack.EMPTY;
		return w.make(HunterType.byIndex(tag.getInt(C_LINE)), tag.getInt(C_TIER));
	}

	private static void announce(ServerLevel lands, Component text) {
		for (ServerPlayer p : lands.players()) p.sendSystemMessage(text);
	}
}
