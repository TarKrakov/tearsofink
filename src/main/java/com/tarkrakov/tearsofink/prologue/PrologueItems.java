package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.travel.Travel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Предметы пролога: запечатанное письмо, письмо-приглашение, потёртый саквояж. */
public final class PrologueItems {
	/** Клиент подставляет сюда открытие окна письма (на сервере ничего не делает). */
	public static Consumer<Player> openInvitation = p -> {};
	/** Клиент подставляет сюда открытие дневника. */
	public static Consumer<Player> openDiary = p -> {};

	private PrologueItems() {}

	static void give(ServerPlayer p, Item item) {
		ItemStack stack = new ItemStack(item);
		if (!p.getInventory().add(stack)) p.drop(stack, false);
	}

	/** Запечатанное письмо: ПКМ — распечатать → зеркальце + письмо-приглашение. */
	public static class SealedLetter extends Item {
		public SealedLetter(Properties p) { super(p); }

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (player instanceof ServerPlayer sp) {
				stack.consume(1, sp);
				give(sp, ModItems.INVITATION_LETTER);
				if (!sp.getInventory().contains(new ItemStack(ModItems.DIM_MIRROR))) give(sp, ModItems.DIM_MIRROR);
				level.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.7f);
				level.playSound(null, sp.blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 0.6f);
			}
			// вернуть то, что сейчас в руке: письмо могло лечь в этот же слот — пустой конверт его не затрёт
			return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
		}
	}

	/** Письмо-приглашение: ПКМ — своё окно с текстом и кнопкой «подписать». */
	public static class Invitation extends Item {
		public Invitation(Properties p) { super(p); }

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			if (level.isClientSide) openInvitation.accept(player);
			else level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 0.9f);
			return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
		}
	}

	/** Потёртый саквояж (подарок Г-на Проводника): ПКМ — открыть → дневник, длинный нож, подвеска. */
	public static class Satchel extends Item {
		public Satchel(Properties p) { super(p); }

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (player instanceof ServerPlayer sp) {
				stack.consume(1, sp);
				give(sp, ModItems.DIARY);
				DiaryEntries.unlock(sp, DiaryEntries.INTRO);
				give(sp, ModItems.INK_LONGKNIFE);
				give(sp, ModItems.SALVATION_PENDANT);
				give(sp, ModItems.DIM_HAND_LANTERN);
				level.playSound(null, sp.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 0.8f);
				level.playSound(null, sp.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8f, 0.8f);
			}
			return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
		}
	}

	/** Дневник: ПКМ — окно-книга (записи открываются по событиям). */
	public static class Diary extends Item {
		public Diary(Properties p) { super(p); }

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			if (level.isClientSide) openDiary.accept(player);
			else level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 0.8f);
			return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
		}
	}

	static boolean bound(ServerPlayer p) {
		return Travel.bound(p);
	}
}
