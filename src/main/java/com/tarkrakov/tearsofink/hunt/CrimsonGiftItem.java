package com.tarkrakov.tearsofink.hunt;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * Дар Багряного Вестника — сумка Алой Жатвы. ПКМ — открыть: лут из loot_table/gameplay/harvest_gift_&lt;0–2&gt;.json
 * (малый Дар лучшему — harvest_bonus_&lt;0–2&gt;). Добычу меняешь в JSON сам (/reload).
 */
public class CrimsonGiftItem extends Item {
	private static final String KIND = "GiftKind";

	public CrimsonGiftItem(Properties properties) {
		super(properties);
	}

	/** kind: сложность 0–2; +10 — малый Дар лучшему. */
	public static void setKind(ItemStack stack, int kind) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt(KIND, kind));
	}

	public static int kind(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d == null ? 0 : d.copyTag().getInt(KIND);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		int k = kind(stack);
		String name = (k >= 10 ? "gameplay/harvest_bonus_" : "gameplay/harvest_gift_") + (k % 10);
		LootTable table = server.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, TearsOfInk.id(name)));
		LootParams params = new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN, sp.position())
				.withParameter(LootContextParams.THIS_ENTITY, sp).create(LootContextParamSets.GIFT);
		for (ItemStack s : table.getRandomItems(params)) if (!sp.getInventory().add(s)) sp.drop(s, false);
		stack.shrink(1);
		level.playSound(null, sp.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1f, 0.7f);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		int k = kind(stack);
		tooltip.add(Component.translatable("harvest.tears-of-ink.diff." + (k % 10)).withStyle(net.minecraft.ChatFormatting.DARK_RED));
		if (k >= 10) tooltip.add(Component.translatable("harvest.tears-of-ink.gift_best").withStyle(net.minecraft.ChatFormatting.GOLD));
	}
}
