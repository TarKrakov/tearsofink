package com.tarkrakov.tearsofink.block.lantern;

import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.skill.SkillPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import com.tarkrakov.tearsofink.particle.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Общая логика фонарей: зажечь/погасить, лимит самодельных, поиск горящих фонарей рядом. */
public final class Lanterns {
	// ===== Настройки — крутить здесь =====
	/** Сколько Чернил стоит зажечь фонарь. */
	public static final int LIGHT_COST = 10;
	/** Сколько самодельных фонарей может гореть рядом... */
	public static final int PLAYER_MADE_LIMIT = 4;
	/** ...в этом радиусе (блоков). */
	public static final int PLAYER_MADE_LIMIT_RADIUS = 48;
	/** На сколько блоков за радиусом ясность плавно спадает до обычной мглы. */
	public static final int CLEAR_FADE = 8;
	/** Самый большой радиус ясности среди фонарей (для поиска). */
	public static final int MAX_CLEAR_RADIUS = 18;

	private Lanterns() {}

	@FunctionalInterface
	public interface LanternVisitor {
		void visit(BlockPos pos, BlockState state, FogClearing lantern);
	}

	/** Обойти все ГОРЯЩИЕ фонари в квадрате ±reach вокруг (x, z). Работает и на сервере, и на клиенте. */
	public static void forEachLit(Level level, double x, double z, int reach, LanternVisitor visitor) {
		int minCx = ((int) Math.floor(x) - reach) >> 4, maxCx = ((int) Math.floor(x) + reach) >> 4;
		int minCz = ((int) Math.floor(z) - reach) >> 4, maxCz = ((int) Math.floor(z) + reach) >> 4;
		for (int cx = minCx; cx <= maxCx; cx++) {
			for (int cz = minCz; cz <= maxCz; cz++) {
				ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				for (BlockPos pos : chunk.getBlockEntitiesPos()) {
					BlockState state = chunk.getBlockState(pos);
					if (state.getBlock() instanceof FogClearing lantern && lantern.clears(state)) {
						visitor.visit(pos, state, lantern);
					}
				}
			}
		}
	}

	/** Защищено ли место горящим фонарём (наши враги здесь не появляются). */
	public static boolean isSafe(Level level, BlockPos at) {
		boolean[] safe = {false};
		forEachLit(level, at.getX(), at.getZ(), MAX_CLEAR_RADIUS, (pos, state, lantern) -> {
			if (!safe[0] && pos.distSqr(at) <= (double) lantern.clearRadius() * lantern.clearRadius()) safe[0] = true;
		});
		return safe[0];
	}

	/** Сколько самодельных фонарей горит рядом. */
	public static int litPlayerMadeNear(Level level, BlockPos at) {
		int[] count = {0};
		long r2 = (long) PLAYER_MADE_LIMIT_RADIUS * PLAYER_MADE_LIMIT_RADIUS;
		forEachLit(level, at.getX(), at.getZ(), PLAYER_MADE_LIMIT_RADIUS, (pos, state, lantern) -> {
			if (lantern.isPlayerMade(state) && !pos.equals(at)) {
				long dx = pos.getX() - at.getX(), dz = pos.getZ() - at.getZ();
				if (dx * dx + dz * dz <= r2) count[0]++;
			}
		});
		return count[0];
	}

	/**
	 * ПКМ по фонарю. Потухший — зажечь (стоит Чернил, только в Чернильных Землях).
	 * Горящий + Shift — погасить (только самодельный; в креативе — любой).
	 */
	public static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player) {
		if (!state.hasProperty(FogClearing.LIT)) return InteractionResult.PASS;
		boolean lit = state.getValue(FogClearing.LIT);
		boolean creative = player.getAbilities().instabuild;
		boolean playerMade = state.hasProperty(FogClearing.PLAYER_MADE) && state.getValue(FogClearing.PLAYER_MADE);

		if (lit) {
			if (!player.isShiftKeyDown() || !(creative || playerMade)) return InteractionResult.PASS;
			if (level instanceof ServerLevel server) {
				level.setBlock(pos, state.setValue(FogClearing.LIT, false), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.2f);
				server.sendParticles(ModParticles.INK_SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.15, 0.1, 0.15, 0.0);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (!creative) {
			InkData data = InkPowers.get(sp);
			if (!data.inside()) {
				fizzle(sp);
				return InteractionResult.CONSUME;
			}
			if (playerMade && litPlayerMadeNear(level, pos) >= PLAYER_MADE_LIMIT) {
				sp.sendSystemMessage(Component.translatable("message.tears-of-ink.too_many_flames").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
				fizzle(sp);
				return InteractionResult.CONSUME;
			}
			if (data.ink() < LIGHT_COST) {
				ServerPlayNetworking.send(sp, new SkillPayloads.NotEnough(false));
				fizzle(sp);
				return InteractionResult.CONSUME;
			}
			InkPowers.update(sp, data.withInk(data.ink() - LIGHT_COST));
		}
		level.setBlock(pos, state.setValue(FogClearing.LIT, true), Block.UPDATE_ALL);
		com.tarkrakov.tearsofink.advancement.ModAdvancements.grant(sp, com.tarkrakov.tearsofink.advancement.ModAdvancements.FIRST_FLAME);
		level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
		emberBurst(server, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 14);
		return InteractionResult.SUCCESS;
	}

	/**
	 * Shift+ПКМ по горящему фонарю с чем-то в руках. Игра в таком случае не спрашивает блок,
	 * поэтому ловим нажатие раньше и гасим сами. Иначе — PASS (обычное поведение).
	 */
	public static void registerSneakDouse() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || player.isSpectator()) return InteractionResult.PASS;
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof FogClearing)) return InteractionResult.PASS;
			// у каменного фонаря «горит» хранит нижняя половина
			if (state.hasProperty(StoneLanternBlock.HALF) && state.getValue(StoneLanternBlock.HALF) == DoubleBlockHalf.UPPER) {
				pos = pos.below();
				state = level.getBlockState(pos);
				if (!(state.getBlock() instanceof FogClearing)) return InteractionResult.PASS;
			}
			if (!state.hasProperty(FogClearing.LIT) || !state.getValue(FogClearing.LIT)) return InteractionResult.PASS;
			boolean playerMade = state.hasProperty(FogClearing.PLAYER_MADE) && state.getValue(FogClearing.PLAYER_MADE);
			if (!playerMade && !player.getAbilities().instabuild) return InteractionResult.PASS;
			if (state.getBlock() instanceof InkLanternBlock lantern && lantern.isEternal()) return InteractionResult.PASS;
			return use(state, level, pos, player);
		});
	}

	/**
	 * Подхватить авторскую модель для старых дорожных фонарей без регенерации чанков.
	 * Обновляется только визуальный флаг: прежние ID блока, свет, обет и взаимодействия не меняются.
	 */
	public static void registerRoadLanternModelUpgrade() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			if (!ModDimensions.isLands(level)) return;
			InkLandsLayout layout = InkLandsLayout.of(level);
			if (layout == null) return;
			// Снимок списка: setBlock меняет данные чанка, и обход не должен зависеть от этого.
			for (BlockPos pos : java.util.List.copyOf(chunk.getBlockEntitiesPos())) {
				BlockState state = chunk.getBlockState(pos);
				if (!state.is(ModBlocks.INK_LANTERN)
						|| state.getValue(InkLanternBlock.ROAD)
						|| state.getValue(net.minecraft.world.level.block.LanternBlock.HANGING)
						|| state.getValue(FogClearing.PLAYER_MADE)) continue;
				// Старые дорожные опоры: два бледных забора, и только в точке,
				// где генератор действительно ставил фонарь. Самодельные не затрагиваем.
				if (!chunk.getBlockState(pos.below()).is(ModBlocks.PALE_FENCE)
						|| !chunk.getBlockState(pos.below(2)).is(ModBlocks.PALE_FENCE)) continue;
				double[] hit = layout.roadNear(pos.getX(), pos.getZ(), InkLandsLayout.ROAD_SHOULDER + 4);
				if (hit == null) continue;
				int k = (int) Math.round(hit[1] / InkLandsLayout.ROAD_LANTERN_SPACING);
				for (int nearby = k - 1; nearby <= k + 1; nearby++) {
					int[] expected = layout.roadLanternPost((int) hit[0], nearby);
					if (expected != null && expected[0] == pos.getX() && expected[1] == pos.getZ()) {
						level.setBlock(pos, state.setValue(InkLanternBlock.ROAD, true), Block.UPDATE_CLIENTS);
						break;
					}
				}
			}
		});
	}

	/** Искры разлетаются во все стороны и вверх. */
	private static void emberBurst(ServerLevel server, double x, double y, double z, int count) {
		var random = server.getRandom();
		for (int i = 0; i < count; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double speed = 0.05 + random.nextDouble() * 0.1;
			server.sendParticles(ModParticles.EMBER, x, y, z, 0,
					Math.cos(a) * speed, 0.08 + random.nextDouble() * 0.12, Math.sin(a) * speed, 1.0);
		}
	}

	private static void fizzle(ServerPlayer player) {
		player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
	}

	/** Мировой фонарь не ломается в выживании. */
	public static float destroyProgress(BlockState state, Player player, float normal) {
		boolean playerMade = state.hasProperty(FogClearing.PLAYER_MADE) && state.getValue(FogClearing.PLAYER_MADE);
		if (!playerMade && !player.getAbilities().instabuild) return 0f;
		return normal;
	}

	/** Поставлен в выживании — самодельный и потухший; в креативе (стройка автора) — мировой и горящий. */
	public static BlockState placed(BlockState state, Player player) {
		if (state == null) return null;
		boolean creative = player != null && player.getAbilities().instabuild;
		if (state.hasProperty(FogClearing.PLAYER_MADE)) state = state.setValue(FogClearing.PLAYER_MADE, !creative);
		if (state.hasProperty(FogClearing.LIT)) state = state.setValue(FogClearing.LIT, creative);
		return state;
	}
}
