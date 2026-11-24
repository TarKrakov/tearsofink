package com.tarkrakov.tearsofink.block.fountain;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.travel.FountainTravel;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import com.tarkrakov.tearsofink.travel.Travel;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

/**
 * Малый чернильный фонтан — малая копия Великого фонтана в Шпиле.
 * ПКМ: каждый НОВЫЙ фонтан навсегда даёт +5 к запасу Чернил (один раз на игрока).
 * Мировой: в выживании не ломается. Работает только внутри Чернильных Земель.
 */
public class InkFountainBlock extends Block {
	// ===== Настройки — крутить здесь =====
	/** Сколько запаса Чернил даёт новый фонтан. */
	public static final int INK_BONUS = 5;

	/** Какие фонтаны игрок уже испил (координаты). Сохраняется навсегда. */
	public static final AttachmentType<Set<Long>> VISITED = AttachmentRegistry.<Set<Long>>builder()
			.persistent(Codec.LONG.listOf().xmap(list -> Set.copyOf(list), set -> set.stream().toList()))
			.copyOnDeath()
			.initializer(Set::of)
			.buildAndRegister(TearsOfInk.id("visited_fountains"));

	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(1, 0, 1, 15, 6, 15),
			Block.box(6, 6, 6, 10, 12, 10));

	public InkFountainBlock(Properties properties) {
		super(properties);
	}

	public static void init() {}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
											  Player player, InteractionHand hand, BlockHitResult hit) {
		if (!ModDimensions.isLands(level) || player.isSecondaryUseActive() || !Travel.isMirror(stack)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer server) FountainTravel.open(server, pos);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		// The Pantheon is an isolated test dimension. Until its travel endpoint is
		// formally connected, this decorative fountain must not award permanent
		// Ink bonuses or overwrite the Ink Lands-only respawn binding.
		if (server.dimension() == PantheonTemple.DIMENSION) {
			sp.displayClientMessage(Component.literal("Фонтан пантеона пока не подключён к сети перемещений."), true);
			return InteractionResult.SUCCESS;
		}
		InkData data = InkPowers.get(sp);
		if (!data.inside()) {
			sp.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
			return InteractionResult.CONSUME;
		}
		FountainTravel.activate(sp, pos);
		Set<Long> visited = sp.getAttachedOrCreate(VISITED);
		long key = pos.asLong();
		boolean alreadyDrunk = visited.contains(key);
		boolean atCap = data.inkMax() >= InkData.INK_CAP;
		if (alreadyDrunk || atCap) {
			// Возрождение не зависит от возможности испить фонтан.
			// Не отмечаем новый фонтан испитым при потолке: бонус останется доступен,
			// если игрок позднее снизит максимум. Доступ к Торговке зависит от личных активаций.
			if (!alreadyDrunk) {
				sp.sendSystemMessage(Component.translatable("message.tears-of-ink.veins_full").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			}
			var travel = com.tarkrakov.tearsofink.travel.Travel.get(sp);
			if (!(travel.respawn().isPresent() && travel.respawn().get().equals(pos) && !travel.respawnBed())) {
				com.tarkrakov.tearsofink.travel.Travel.set(sp, travel.withRespawn(pos.immutable(), false));
				com.tarkrakov.tearsofink.travel.Travel.preloadRespawn(sp, pos.immutable());
				level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8f, 0.7f);
				server.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 12, 0.2, 0.3, 0.2, 0.02);
			}
			sp.sendSystemMessage(Component.translatable("message.tears-of-ink.fountain_bound").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
			com.tarkrakov.tearsofink.trader.SoulTrader.onFountainActivated(sp, server, pos);
			return InteractionResult.SUCCESS;
		}
		Set<Long> copy = new HashSet<>(visited);
		copy.add(key);
		sp.setAttached(VISITED, Set.copyOf(copy));
		InkData after = data.withInkBonus(data.inkBonus() + INK_BONUS);
		InkPowers.update(sp, after.withInk(after.ink() + INK_BONUS));
		com.tarkrakov.tearsofink.advancement.ModAdvancements.grant(sp, com.tarkrakov.tearsofink.advancement.ModAdvancements.CURSED_VESSEL);

		sp.sendSystemMessage(Component.translatable("message.tears-of-ink.fountain_drink", INK_BONUS).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
		level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 0.6f);
		level.playSound(null, pos, SoundEvents.SQUID_SQUIRT, SoundSource.BLOCKS, 0.6f, 0.7f);
		server.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 30, 0.3, 0.4, 0.3, 0.05);
		server.sendParticles(ParticleTypes.SQUID_INK, sp.getX(), sp.getY() + 1.0, sp.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
		com.tarkrakov.tearsofink.trader.SoulTrader.onFountainActivated(sp, server, pos);
		return InteractionResult.SUCCESS;
	}

	/** Брызги и пузыри над чашей. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SQUID_INK, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.8,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.03, 0);
		}
	}
}
