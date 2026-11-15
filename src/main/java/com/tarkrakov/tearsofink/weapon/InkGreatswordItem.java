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
 * Клинок Чернил с абилкой по R (тратит Чернила). Бывает двуручным (Палачи: сильно и медленно —
 * пока он в правой руке, щит в левой не блокирует, фонарь работает) и одноручным (Скимитары: быстрее и легче, левая рука свободна).
 */
public class InkGreatswordItem extends SwordItem implements AbilityWeapon {
	// ===== Настройки — крутить здесь =====
	/** Двуручный (Палачи): урон сверх материала (итого: 1 рука + 2 Асфода + это = 9) и скорость атаки (меч −2.4, двуручник медленнее → 1.0). */
	public static final int DAMAGE = 6;
	public static final float ATTACK_SPEED = -3.0f;
	/** Одноручный (Скимитары): итого урон 5, скорость атаки 1.5. */
	public static final int ONE_HANDED_DAMAGE = 2;
	public static final float ONE_HANDED_SPEED = -2.5f;

	private final Supplier<Skill> ability;
	private final boolean twoHanded;

	public InkGreatswordItem(Supplier<Skill> ability, Properties properties) {
		this(ability, properties, true);
	}

	public InkGreatswordItem(Supplier<Skill> ability, Properties properties, boolean twoHanded) {
		this(ability, properties, twoHanded, twoHanded ? DAMAGE : ONE_HANDED_DAMAGE);
	}

	/** damage — урон сверх материала (Тусклый скимитар: ONE_HANDED_DAMAGE − 1 → итого 4). */
	public InkGreatswordItem(Supplier<Skill> ability, Properties properties, boolean twoHanded, int damage) {
		this(ability, properties, twoHanded, damage, twoHanded ? ATTACK_SPEED : ONE_HANDED_SPEED);
	}

	/** Свой урон и скорость атаки (оружие из перечня: Секира и др.). */
	public InkGreatswordItem(Supplier<Skill> ability, Properties properties, boolean twoHanded, int damage, float speed) {
		super(ModTiers.ASPHODA, properties.attributes(SwordItem.createAttributes(ModTiers.ASPHODA,
				damage, speed).withTooltip(false)));
		this.ability = ability;
		this.twoHanded = twoHanded;
	}

	/** Двуручный клинок в этой руке — занимает обе руки (щит в левой молчит). */
	public static boolean twoHanded(ItemStack stack) {
		return stack.getItem() instanceof InkGreatswordItem g && g.twoHanded;
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
		// одноручный клинок, левая рука пустая — удар эфесом (только в Чернильных землях, не по блоку)
		if (com.tarkrakov.tearsofink.weapon.combat.HiltStrike.applies(player, stack, hand)) {
			return com.tarkrakov.tearsofink.weapon.combat.HiltStrike.useFromItem(level, player, stack)
					? net.minecraft.world.InteractionResultHolder.consume(stack) : net.minecraft.world.InteractionResultHolder.fail(stack);
		}
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
		if (a == com.tarkrakov.tearsofink.weapon.combat.Archetype.AXE) { // казнь: держать — топоры расходятся, отпустил — прыжок и двойной удар
			if (player instanceof net.minecraft.server.level.ServerPlayer sp
					&& !com.tarkrakov.tearsofink.skill.Skills.activate(sp, com.tarkrakov.tearsofink.weapon.combat.AxeMoves.EXECUTION, input)) {
				return net.minecraft.world.InteractionResultHolder.fail(stack);
			}
			player.startUsingItem(hand);
			return net.minecraft.world.InteractionResultHolder.consume(stack);
		}
		if (a == com.tarkrakov.tearsofink.weapon.combat.Archetype.DUSSAK) { // усиленная стойка парирования
			if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
				com.tarkrakov.tearsofink.skill.Skills.activate(sp, com.tarkrakov.tearsofink.weapon.blood.BloodAbilities.DOUBLE_PARRY, input);
			}
			return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		}
		if (a == com.tarkrakov.tearsofink.weapon.combat.Archetype.TALWAR) { // крест-накрест кровью и отскок
			if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
				com.tarkrakov.tearsofink.skill.Skills.activate(sp, com.tarkrakov.tearsofink.weapon.blood.BloodAbilities.TALWAR_CROSS, input);
			}
			return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
		if (com.tarkrakov.tearsofink.weapon.combat.Archetype.of(stack) == com.tarkrakov.tearsofink.weapon.combat.Archetype.AXE) {
			if (used >= com.tarkrakov.tearsofink.weapon.combat.AxeMoves.MAX_HOLD_TICKS) sp.releaseUsingItem(); // развёл до предела — бьёт сам
			return;
		}
		int every = com.tarkrakov.tearsofink.weapon.combat.Combat.chopInterval();
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
		if (com.tarkrakov.tearsofink.weapon.combat.Archetype.of(stack) == com.tarkrakov.tearsofink.weapon.combat.Archetype.AXE) {
			com.tarkrakov.tearsofink.weapon.combat.AxeMoves.strike(sp, used);
			return;
		}
		if (used >= 2) com.tarkrakov.tearsofink.weapon.combat.Combat.guillotineEnd(sp); // хоть один удар был — казнь сверху
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Descriptions.add(getDescriptionId(), tooltip);
	}
}
