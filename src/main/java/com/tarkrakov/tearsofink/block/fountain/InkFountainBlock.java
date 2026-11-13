package com.tarkrakov.tearsofink.block.fountain;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
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
import net.minecraft.world.InteractionResult;
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
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		InkData data = InkPowers.get(sp);
		if (!data.inside()) {
			sp.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 1.8f);
			return InteractionResult.CONSUME;
		}
		Set<Long> visited = sp.getAttachedOrCreate(VISITED);
		long key = pos.asLong();
		if (visited.contains(key)) {
			// уже испит — теперь привязывает возрождение
			var travel = com.tarkrakov.tearsofink.travel.Travel.get(sp);
			if (!(travel.respawn().isPresent() && travel.respawn().get().equals(pos) && !travel.respawnBed())) {
				com.tarkrakov.tearsofink.travel.Travel.set(sp, travel.withRespawn(pos.immutable(), false));
				com.tarkrakov.tearsofink.travel.Travel.preloadRespawn(sp, pos.immutable());
				level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8f, 0.7f);
				server.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 12, 0.2, 0.3, 0.2, 0.02);
			}
			sp.sendSystemMessage(Component.translatable("message.tears-of-ink.fountain_bound").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
			return InteractionResult.SUCCESS;
		}
		if (data.inkMax() >= InkData.INK_CAP) {
			sp.sendSystemMessage(Component.translatable("message.tears-of-ink.veins_full").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			return InteractionResult.CONSUME;
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
