package com.tarkrakov.tearsofink.weapon.forge;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.hunt.HunterType;
import com.tarkrakov.tearsofink.hunt.HunterWeaponItem;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.vow.Vow;
import com.tarkrakov.tearsofink.vow.Vows;
import com.tarkrakov.tearsofink.weapon.ArsenalWeaponItem;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Купель возвышения — постройка автора (structure/ascension_font.nbt). Мягкая проверка по материалам (поворот ступеней
 * и украшения не важны): вода 3×3, под ней кровавое дерево, вокруг воды бледное кольцо 5×5, по углам кольца —
 * кровавые столбы в 2 блока. Встал в центр — Купель оживает: вода чёрная (Путь Чернил) или белая (Путь Души) по обету
 * того, кто вошёл первым, пока в ней кто-то стоит. Shift+ПКМ оружием в центре — возвышение на ступень
 * (Пигмент + ресурс линии: Кровь — Багряный Цветок, Шпиль — Символ Доблести, Чернила — Частичка Души).
 * Собрано не до конца — Купель подсвечивает, чего не хватает.
 */
public final class AscensionFont {
	// ===== Настройки — крутить здесь =====
	/** Возвышение: Тусклое → Контрастное, Контрастное → Рубрики: Пигмент и ресурс линии. */
	public static final int[] PIGMENT_COST = {8, 16};
	public static final int[] RESOURCE_COST = {1, 2};
	/** Символ Доблести — очки за контракты: средний 1, сложный 2; Символ за каждые 8 очков. */
	public static final int[] SIGIL_POINTS = {0, 1, 2};
	public static final int POINTS_PER_SIGIL = 8;
	/** Подсказка «чего не хватает» — если собрано хотя бы столько долей; не чаще раза в столько тиков. */
	public static final float HINT_FROM = 0.4f;
	public static final int HINT_EVERY = 60;
	/** Цвет воды: 1 — чёрная (Чернила), 2 — белая (Душа). */
	public static final int INK = 1, SOUL = 2;

	private static final Map<BlockPos, Active> ACTIVE = new HashMap<>();
	private static final Map<UUID, Long> LAST_HINT = new HashMap<>();
	/** Клиент: стоит ли свой игрок в живой Купели (чтобы Shift+ПКМ не делал удар эфесом). Ставит клиент. */
	public static Predicate<Player> clientInFont = p -> false;

	public static final AttachmentType<Integer> HARD_CONTRACTS = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.INT).copyOnDeath().initializer(() -> 0).buildAndRegister(TearsOfInk.id("hard_contracts"));

	private static final class Active {
		final int color;
		long seen;

		Active(int color, long seen) {
			this.color = color;
			this.seen = seen;
		}
	}

	/** Сервер → клиент: Купель с центром в pos ожила (color 1/2) или уснула (0). */
	public record Sync(BlockPos pos, int color) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(TearsOfInk.id("font_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Sync::pos, ByteBufCodecs.VAR_INT, Sync::color, Sync::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private AscensionFont() {}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(Sync.TYPE, Sync.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(AscensionFont::tick);
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (hand != net.minecraft.world.InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !Gear.ours(stack))
				return InteractionResultHolder.pass(stack);
			if (level.isClientSide) return clientInFont.test(player) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
			if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
			BlockPos c = sp.blockPosition();
			if (!ACTIVE.containsKey(c)) return InteractionResultHolder.pass(stack);
			ascend(sp, stack, c);
			return InteractionResultHolder.success(stack);
		});
	}

	// ===== Постройка =====
	private static boolean family(ServerLevel level, BlockPos pos, String prefix) {
		var key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
		if (!TearsOfInk.MOD_ID.equals(key.getNamespace())) return false;
		String p = key.getPath();
		if (!p.contains(prefix)) return false;
		return p.contains("planks") || p.contains("log") || p.contains("wood") || p.contains("stairs") || p.contains("slab") || p.contains("fence");
	}

	private static boolean water(ServerLevel level, BlockPos pos) {
		return level.getFluidState(pos).is(FluidTags.WATER);
	}

	/** Чего не хватает (пустой список — Купель собрана). center — вода в центре, на уровне ног. */
	public static List<BlockPos> missing(ServerLevel level, BlockPos c) {
		List<BlockPos> out = new ArrayList<>();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				int ring = Math.max(Math.abs(dx), Math.abs(dz));
				BlockPos p = c.offset(dx, 0, dz);
				if (ring <= 1) {
					if (!water(level, p)) out.add(p);
					if (!family(level, p.below(), "scarlet")) out.add(p.below());
				} else {
					if (!family(level, p, "pale")) out.add(p);
					if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
						if (!family(level, p.above(), "scarlet")) out.add(p.above());
						if (!family(level, p.above(2), "scarlet")) out.add(p.above(2));
					}
				}
			}
		}
		return out;
	}

	/** Всего проверяемых мест: 9 воды + 9 пола + 16 кольца + 8 столбов. */
	private static final int TOTAL = 42;

	// ===== Ход =====
	private static void tick(MinecraftServer server) {
		ServerLevel lands = ModDimensions.lands(server);
		if (lands == null || server.getTickCount() % 10 != 0) return;
		long now = lands.getGameTime();
		for (ServerPlayer p : lands.players()) {
			BlockPos feet = p.blockPosition();
			if (!water(lands, feet)) continue;
			// уже в живой Купели (в любой из 9 клеток воды) — продлить
			Active in = null;
			for (int dx = -1; dx <= 1 && in == null; dx++)
				for (int dz = -1; dz <= 1 && in == null; dz++) {
					Active a = ACTIVE.get(feet.offset(dx, 0, dz));
					if (a != null) in = a;
				}
			if (in != null) {
				in.seen = now;
				com.tarkrakov.tearsofink.prologue.FateProgress.mark(p, com.tarkrakov.tearsofink.prologue.FateProgress.Goal.FONT);
				continue;
			}
			// встал в центр — проверить постройку
			if (!family(lands, feet.below(), "scarlet")) continue;
			List<BlockPos> miss = missing(lands, feet);
			if (miss.isEmpty()) {
				Vow v = Vows.vow(p);
				int color = v == Vow.INK ? INK : v == Vow.SOUL ? SOUL : 0;
				if (color == 0) continue;
				ACTIVE.put(feet.immutable(), new Active(color, now));
				com.tarkrakov.tearsofink.prologue.FateProgress.mark(p, com.tarkrakov.tearsofink.prologue.FateProgress.Goal.FONT);
				broadcast(lands, feet.immutable(), color);
				lands.playSound(null, feet, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, color == INK ? 0.6f : 1.4f);
				p.displayClientMessage(Component.translatable("font.tears-of-ink.awake").withStyle(ChatFormatting.GRAY), true);
			} else if (TOTAL - miss.size() >= TOTAL * HINT_FROM) {
				hint(p, miss, now);
			}
		}
		for (Iterator<Map.Entry<BlockPos, Active>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			if (now - e.getValue().seen > 15) {
				it.remove();
				broadcast(lands, e.getKey(), 0);
				continue;
			}
			BlockPos c = e.getKey();
			if (now % 20 == 0) {
				boolean ink = e.getValue().color == INK;
				lands.sendParticles(ink ? ParticleTypes.SQUID_INK : ParticleTypes.END_ROD,
						c.getX() + 0.5, c.getY() + 0.9, c.getZ() + 0.5, ink ? 4 : 2, 1.0, 0.1, 1.0, 0.01);
			}
		}
		// раз в 2 с — повторить живые Купели всем в Землях (вошедшим позже)
		if (server.getTickCount() % 40 == 0) for (var e : ACTIVE.entrySet()) broadcast(lands, e.getKey(), e.getValue().color);
	}

	private static void broadcast(ServerLevel lands, BlockPos pos, int color) {
		Sync s = new Sync(pos, color);
		for (ServerPlayer p : lands.players()) ServerPlayNetworking.send(p, s);
	}

	private static void hint(ServerPlayer p, List<BlockPos> miss, long now) {
		Long last = LAST_HINT.get(p.getUUID());
		if (last != null && now - last < HINT_EVERY) return;
		LAST_HINT.put(p.getUUID(), now);
		p.displayClientMessage(Component.translatable("font.tears-of-ink.incomplete", miss.size()).withStyle(ChatFormatting.GOLD), true);
		var dust = new DustParticleOptions(new Vector3f(1f, 0.6f, 0.1f), 1.2f);
		for (BlockPos m : miss) {
			p.serverLevel().sendParticles(p, dust, true, m.getX() + 0.5, m.getY() + 0.5, m.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0);
		}
	}

	// ===== Возвышение =====
	/** Ресурс линии: Кровь — Багряный Цветок, Чернила — Частичка Души, Шпиль — Символ Доблести. */
	public static Item resource(ItemStack weapon) {
		HunterType line = HunterType.SPIRE;
		if (weapon.getItem() instanceof ArsenalWeaponItem a) line = a.line();
		else if (weapon.getItem() instanceof OrdinaryWeaponItem o) line = o.line();
		else if (weapon.getItem() instanceof HunterWeaponItem) line = HunterWeaponItem.line(weapon);
		return switch (line) {
			case BLOOD -> ModItems.CRIMSON_FLOWER;
			case INK -> ModItems.HOPE_MOTE;
			default -> ModItems.VALOR_SIGIL;
		};
	}

	private static void ascend(ServerPlayer p, ItemStack weapon, BlockPos c) {
		int tier = Gear.tier(weapon);
		if (tier >= Gear.TIERS - 1) {
			p.displayClientMessage(Component.translatable("font.tears-of-ink.max").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		Item res = resource(weapon);
		int pig = PIGMENT_COST[tier], need = RESOURCE_COST[tier];
		boolean creative = p.getAbilities().instabuild;
		if (!creative && (count(p, ModItems.PIGMENT) < pig || count(p, res) < need)) {
			p.displayClientMessage(Component.translatable("font.tears-of-ink.cost", pig, need, new ItemStack(res).getHoverName())
					.withStyle(ChatFormatting.RED), true);
			p.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3f, 1.6f);
			return;
		}
		if (!creative) {
			take(p, ModItems.PIGMENT, pig);
			take(p, res, need);
		}
		Gear.setTier(weapon, tier + 1);
		com.tarkrakov.tearsofink.prologue.FateProgress.mark(p, com.tarkrakov.tearsofink.prologue.FateProgress.Goal.FONT, com.tarkrakov.tearsofink.prologue.FateProgress.Goal.GEAR, com.tarkrakov.tearsofink.prologue.FateProgress.Goal.CONTRAST);
		if (tier + 1 >= 2) com.tarkrakov.tearsofink.prologue.FateProgress.mark(p, com.tarkrakov.tearsofink.prologue.FateProgress.Goal.RUBRICS);
		ServerLevel level = p.serverLevel();
		boolean ink = ACTIVE.get(c).color == INK;
		level.sendParticles(ink ? ParticleTypes.SQUID_INK : ParticleTypes.END_ROD, c.getX() + 0.5, c.getY() + 1.2, c.getZ() + 0.5,
				40, 0.6, 0.8, 0.6, 0.05);
		level.playSound(null, c, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, ink ? 0.7f : 1.3f);
		p.displayClientMessage(Component.translatable("font.tears-of-ink.ascended", weapon.getHoverName(),
				Component.translatable("gear.tears-of-ink.tier." + (tier + 1))).withStyle(ChatFormatting.AQUA), false);
	}

	private static int count(ServerPlayer p, Item item) {
		int n = 0;
		for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
		return n;
	}

	private static void take(ServerPlayer p, Item item, int amount) {
		for (ItemStack s : p.getInventory().items) {
			if (amount <= 0) return;
			if (!s.is(item)) continue;
			int t = Math.min(amount, s.getCount());
			s.shrink(t);
			amount -= t;
		}
	}

	/** Контракт выполнен: очки к Символу Доблести (средний 1, сложный 2), каждые POINTS_PER_SIGIL — Символ. */
	public static void onContract(ServerPlayer p, int rank) {
		int add = SIGIL_POINTS[Math.min(rank, SIGIL_POINTS.length - 1)];
		if (add <= 0) return;
		int before = p.getAttachedOrCreate(HARD_CONTRACTS), n = before + add;
		p.setAttached(HARD_CONTRACTS, n);
		if (n / POINTS_PER_SIGIL > before / POINTS_PER_SIGIL) {
			ItemStack s = new ItemStack(ModItems.VALOR_SIGIL);
			if (!p.getInventory().add(s)) p.drop(s, false);
			p.sendSystemMessage(Component.translatable("font.tears-of-ink.sigil").withStyle(ChatFormatting.GOLD));
		} else {
			p.sendSystemMessage(Component.translatable("font.tears-of-ink.sigil_progress", n % POINTS_PER_SIGIL,
					POINTS_PER_SIGIL).withStyle(ChatFormatting.GRAY));
		}
	}
}
