package com.tarkrakov.tearsofink.weapon.forge;

import com.tarkrakov.tearsofink.hunt.HunterType;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.weapon.InkGreatswordItem;

/**
 * Обычное оружие линии (крафт на верстаке из слитков и палок): лёгкий одноручный и тяжёлый двуручный меч.
 * Удары — своего архетипа. Бледный линейный двуручник имеет собственные бесплатные приёмы.
 * Пассивки: Кровь — шанс кровотечения, Шпиль — быстрее, Чернила — замедляют (Gear).
 */
public class OrdinaryWeaponItem extends InkGreatswordItem {
	private final HunterType line;

	/** damage и speed — итоговые, как в игре (урон 5, скорость 1,5). */
	public OrdinaryWeaponItem(Properties properties, boolean twoHanded, HunterType line, int damage, float speed) {
		super(() -> null, properties, twoHanded, damage - 3, speed - 4f);
		this.line = line;
	}

	public HunterType line() {
		return line;
	}

	@Override
	public Skill ability() {
		return this == com.tarkrakov.tearsofink.item.ModItems.PALE_LINEAR_GREATSWORD
				? com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.SINGLE : null;
	}

	@Override
	public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level,
			net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
		if (hand == net.minecraft.world.InteractionHand.MAIN_HAND
				&& com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.dual(player)) {
			var stack = player.getItemInHand(hand);
			if (player instanceof net.minecraft.server.level.ServerPlayer sp
					&& !com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.charge(sp))
				return net.minecraft.world.InteractionResultHolder.fail(stack);
			player.startUsingItem(hand);
			return net.minecraft.world.InteractionResultHolder.consume(stack);
		}
		if (hand == net.minecraft.world.InteractionHand.MAIN_HAND
                && com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.isWeapon(player.getItemInHand(hand))) {
            var stack = player.getItemInHand(hand);
            // Одиночный ПКМ — защита, пока игрок держит кнопку.
            // Ванильное использование предмета само синхронизирует отпускание ПКМ с сервером.
            // Парный ПКМ выше остался прежней заряжаемой атакой.
            if (player instanceof net.minecraft.server.level.ServerPlayer sp)
                com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.holdGuard(sp);
            player.startUsingItem(hand);
            return net.minecraft.world.InteractionResultHolder.consume(stack);
        }
		return super.use(level, player, hand);
	}

	@Override
	public void onUseTick(net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity,
			net.minecraft.world.item.ItemStack stack, int remaining) {
		if (com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.isWeapon(stack)) {
			if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
				// Одиночная защита удерживается, пока игрок не отпустит ПКМ.
				// Лимит времени существует только для прежней парной заряжаемой атаки.
				if (com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.dual(sp)
						&& getUseDuration(stack, entity) - remaining >= 50) sp.releaseUsingItem();
			}
			return;
		}
		super.onUseTick(level, entity, stack, remaining);
	}

	@Override
	public void releaseUsing(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
			net.minecraft.world.entity.LivingEntity entity, int timeLeft) {
		if (com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.isWeapon(stack)) {
			if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
				com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.releaseHeldGuard(sp);
				com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.release(sp);
			}
			return;
		}
		super.releaseUsing(stack, level, entity, timeLeft);
	}
}
