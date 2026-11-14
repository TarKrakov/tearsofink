package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.item.Descriptions;
import com.tarkrakov.tearsofink.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Supplier;

/**
 * Двуручный меч Чернил: бьёт сильно и медленно. Абилка по R тратит Чернила.
 * Пока он в правой руке, щит в левой не блокирует (фонарь — работает).
 */
public class InkGreatswordItem extends SwordItem implements AbilityWeapon {
	// ===== Настройки — крутить здесь =====
	/** Урон сверх материала (итого: 1 рука + 2 Асфода + это = 9) и скорость атаки (меч −2.4, двуручник медленнее). */
	public static final int DAMAGE = 6;
	public static final float ATTACK_SPEED = -3.0f;

	private final Supplier<Skill> ability;

	public InkGreatswordItem(Supplier<Skill> ability, Properties properties) {
		super(ModTiers.ASPHODA, properties.attributes(SwordItem.createAttributes(ModTiers.ASPHODA, DAMAGE, ATTACK_SPEED).withTooltip(false)));
		this.ability = ability;
	}

	@Override
	public Skill ability() {
		return ability.get();
	}

	// ===== Парные приёмы: два одинаковых клинка, ПКМ =====
	private static boolean dual(net.minecraft.world.entity.player.Player player, ItemStack stack) {
		var a = com.tarkrakov.tearsofink.weapon.combat.Archetype.of(stack);
		return a != null && a == com.tarkrakov.tearsofink.weapon.combat.Archetype.of(player.getOffhandItem())
				&& player.getMainHandItem() == stack;
	}

	@Override
	public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
			net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != net.minecraft.world.InteractionHand.MAIN_HAND || !dual(player, stack)) return net.minecraft.world.InteractionResultHolder.pass(stack);
		var a = com.tarkrakov.tearsofink.weapon.combat.Archetype.of(stack);
		var input = new com.tarkrakov.tearsofink.skill.SkillInput(0, 0, false);
		if (a == com.tarkrakov.tearsofink.weapon.combat.Archetype.EXECUTIONER) {
			// Гильотина: держать ПКМ
			if (player instanceof net.minecraft.server.level.ServerPlayer sp
					&& !com.tarkrakov.tearsofink.skill.Skills.activate(sp, WeaponAbilities.GUILLOTINE, input)) {
				return net.minecraft.world.InteractionResultHolder.fail(stack);
			}
			player.startUsingItem(hand);
			return net.minecraft.world.InteractionResultHolder.consume(stack);
		}
		// Чернильный Росчерк: одно нажатие
		if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
			com.tarkrakov.tearsofink.skill.Skills.activate(sp, WeaponAbilities.FLOURISH, input);
		}
		return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
		return 72000;
	}

	@Override
	public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
		return net.minecraft.world.item.UseAnim.NONE;
	}

	@Override
	public void onUseTick(net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity, ItemStack stack, int remaining) {
		if (!(entity instanceof net.minecraft.server.level.ServerPlayer sp)) return;
		if (!dual(sp, stack)) {
			sp.stopUsingItem();
			return;
		}
		int used = getUseDuration(stack, entity) - remaining;
		int every = com.tarkrakov.tearsofink.weapon.combat.Combat.GUILLOTINE_CHOP.duration();
		if (used % every != 1) return;
		int n = used / every;
		if (n >= com.tarkrakov.tearsofink.weapon.combat.Combat.GUILLOTINE_MAX_CHOPS) {
			sp.releaseUsingItem(); // устал — завершающий удар сам
			return;
		}
		com.tarkrakov.tearsofink.weapon.combat.Combat.guillotineChop(sp, n % 2 == 1);
	}

	@Override
	public void releaseUsing(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity, int timeLeft) {
		if (!(entity instanceof net.minecraft.server.level.ServerPlayer sp) || !dual(sp, stack)) return;
		int used = getUseDuration(stack, entity) - timeLeft;
		if (used >= 2) com.tarkrakov.tearsofink.weapon.combat.Combat.guillotineEnd(sp); // хоть один удар был — казнь сверху
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Descriptions.add(getDescriptionId(), tooltip);
	}
}
