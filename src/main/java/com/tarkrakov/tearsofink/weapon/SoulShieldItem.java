package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.item.Descriptions;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.sound.ModSounds;
import com.tarkrakov.tearsofink.weapon.shield.ShieldAudio;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Щит Души: блокирует как ванильный, абилка по Shift+R тратит Душу.
 * Не блокирует, если в правой руке двуручный клинок (Палачи); с одноручным (Скимитары) — блокирует.
 */
public class SoulShieldItem extends ShieldItem implements AbilityWeapon {
	private final Supplier<Skill> ability;

	/** Бастион (два щита): урон удара щитом (итого с кулаком 5) и скорость атаки (1,0 — медленно и крепко). */
	public static final double BASH_DAMAGE = 4, BASH_SPEED = -3.0;

	/** Потёртые щиты (Котомка паломника): во столько раз слабее удар щитом, абилка и Резонанс. */
	public static final float WORN_POWER = 0.7f;

	private final float power;

	public SoulShieldItem(Supplier<Skill> ability, Properties properties) {
		this(ability, properties, 1f);
	}

	/** power — сила щита (1 — обычный, WORN_POWER — потёртый). */
	public SoulShieldItem(Supplier<Skill> ability, Properties properties, float power) {
		super(properties.attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
				.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
						BASE_ATTACK_DAMAGE_ID, (BASH_DAMAGE + 1) * power - 1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
						net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
				.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
						BASE_ATTACK_SPEED_ID, BASH_SPEED, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
						net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
				.build()));
		this.ability = ability;
		this.power = power;
	}

	public float power() {
		return power;
	}

	/** Сила абилки: лучший из щитов в руках с этой абилкой (потёртый — слабее). */
	public static float factor(net.minecraft.world.entity.player.Player p, Skill ability) {
		float best = 0;
		for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
			if (s.getItem() instanceof SoulShieldItem sh && sh.ability() == ability) best = Math.max(best, sh.power);
		}
		return best > 0 ? best : 1f;
	}

	/** Сила пары щитов (Резонанс): среднее двух. */
	public static float pairPower(net.minecraft.world.entity.player.Player p) {
		float a = p.getMainHandItem().getItem() instanceof SoulShieldItem x ? x.power : 1f;
		float b = p.getOffhandItem().getItem() instanceof SoulShieldItem y ? y.power : 1f;
		return (a + b) / 2;
	}

	@Override
	public Skill ability() {
		return ability.get();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (hand == InteractionHand.OFF_HAND && InkGreatswordItem.twoHanded(player.getMainHandItem())) {
			return InteractionResultHolder.pass(player.getItemInHand(hand));
		}
		boolean wasUsing = player.isUsingItem();
		InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
		if (!wasUsing && player instanceof net.minecraft.server.level.ServerPlayer p
				&& p.isUsingItem() && p.getUseItem().getItem() == this)
			ShieldAudio.play(p, ModSounds.SOUL_SHIELD_RAISE, 0.30f);
		return result;
	}

	/** Отпустил ПКМ: в стойке двух щитов — выброс заряда (ShieldStance). */
	@Override
	public void releaseUsing(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity, int timeLeft) {
		if (entity instanceof net.minecraft.server.level.ServerPlayer sp) com.tarkrakov.tearsofink.weapon.shield.ShieldStance.release(sp);
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
		return repair.is(ModItems.ASPHODA);
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getDescriptionId(); // без ванильной приписки про цвет баннера
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Descriptions.add(getDescriptionId(), tooltip);
	}
}
