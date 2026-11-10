package com.tarkrakov.tearsofink.item;

import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Книга навыка: ПКМ — навык выучен навсегда, книга расходуется.
 * Читать можно где угодно; работает навык только в Чернильных Землях.
 * Описание в подсказке — строки {@code item.tears-of-ink.<книга>.desc1}, {@code .desc2}, …
 */
public class SkillBookItem extends Item {
	/** Сколько строк описания искать в языковом файле. */
	private static final int MAX_DESC_LINES = 4;

	private final Supplier<Skill> skill;

	public SkillBookItem(Supplier<Skill> skill, Properties properties) {
		super(properties);
		this.skill = skill;
	}

	public Skill skill() {
		return skill.get();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.sidedSuccess(stack, true);
		}
		Skill s = skill();
		Component name = Component.translatable(s.nameKey());
		if (!Skills.learn(serverPlayer, s)) {
			serverPlayer.sendSystemMessage(Component.translatable("message.tears-of-ink.skill_known", name).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			return InteractionResultHolder.fail(stack);
		}
		serverPlayer.sendSystemMessage(Component.translatable("message.tears-of-ink.skill_learned", name).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 0.7f);
		stack.consume(1, player);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		String base = getDescriptionId() + ".desc";
		for (int i = 1; i <= MAX_DESC_LINES; i++) {
			String key = base + i;
			if (!net.minecraft.locale.Language.getInstance().has(key)) break;
			tooltip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		}
	}
}
