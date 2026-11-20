package com.tarkrakov.tearsofink.weapon.forge;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.weapon.InkGreatswordItem;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.EquipmentAffinity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Снаряжение Земель: ступени (Тусклое / Контрастное / Рубрики), заточка и деградация, дебафф ванильного оружия.
 * Ступень, заточка и пробуждения действуют в Землях; штраф чужого снаряжения зеркален.
 */
public final class Gear {
	// ===== Настройки — крутить здесь (код → перезапуск игры) =====
	/** Урон за ступень: Тусклое, Контрастное, Рубрики. */
	public static final float[] TIER_MULT = {1.0f, 1.15f, 1.3f};
	/** Запас заточки по ступеням (0 — не тупится): удар связки −1, приём на R / ПКМ −5. */
	public static final int[] SHARP_MAX = {0, 400, 300};
	public static final int WEAR_HIT = 1, WEAR_ABILITY = 5;
	/** Чернила за один успешный удар связки обычным оружием, независимо от числа задетых целей и обета. */
	public static final float ORDINARY_COMBO_INK = 1f;
	/** Ниже этой доли — оружие деградирует на ступень; на этой — предупреждение. */
	public static final float DEGRADE_AT = 0.10f, WARN_AT = 0.25f;
	/** Заточка на Чернильной наковальне: доля шкалы за один Пигмент (по одному в клетку). */
	public static final float SHARPEN_PER_PIGMENT = 0.25f;
	/** Совместимое имя прежнего коэффициента; новый единый источник — EquipmentAffinity. */
	public static final float VANILLA_WEAPON_MULT = (float) EquipmentAffinity.FOREIGN_MULTIPLIER;
	/** Пассивки обычного оружия: Кровь — шанс кровотечения; Чернила — замедление (тики, сила 0 = I). */
	public static final float ORDINARY_BLEED_CHANCE = 0.10f;
	public static final int INK_SLOW_TICKS = 30, INK_SLOW_AMP = 0;

	/** Чернильный металл: шанс с врага, убитого игроком в Чернолесье (руды нет). */
	public static final float INK_METAL_CHANCE = 0.25f;
	/** Чернильный отголосок: редкий шанс с тех же врагов Чернолесья. */
	public static final float INK_ECHO_CHANCE = 0.02f;
	/** Осколки безумия: контракт с Чернильным охотником (в награде) и убитый игроком Чернильный охотник. */
	public static final int SHARDS_CONTRACT_MIN = 1, SHARDS_CONTRACT_MAX = 2, SHARDS_KILL_MIN = 2, SHARDS_KILL_MAX = 3;

	public static final int TIERS = 3;
	private static final String TIER = "Tier", SHARP = "Sharp", LEGACY_TIER = "HunterTier", WARNED = "SharpWarned";

	private Gear() {}

	public static void init() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getEntity() instanceof ServerPlayer)) return;
			// убил Чернильного охотника — Осколки безумия
			if (entity instanceof com.tarkrakov.tearsofink.hunt.HunterEntity h && h.hunterType() == com.tarkrakov.tearsofink.hunt.HunterType.INK) {
				entity.spawnAtLocation(new ItemStack(com.tarkrakov.tearsofink.item.ModItems.MADNESS_SHARD,
						SHARDS_KILL_MIN + entity.getRandom().nextInt(SHARDS_KILL_MAX - SHARDS_KILL_MIN + 1)));
			}
			if (!(entity instanceof net.minecraft.world.entity.monster.Enemy)) return;
			if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level) || !ModDimensions.isLands(level)) return;
			if (!level.getBiome(entity.blockPosition()).is(net.minecraft.resources.ResourceKey.create(
					net.minecraft.core.registries.Registries.BIOME, TearsOfInk.id("inkwood")))) return;
			if (entity.getRandom().nextFloat() < INK_METAL_CHANCE)
				entity.spawnAtLocation(new ItemStack(com.tarkrakov.tearsofink.item.ModItems.RAW_INK_METAL));
			if (entity.getRandom().nextFloat() < INK_ECHO_CHANCE)
				entity.spawnAtLocation(new ItemStack(com.tarkrakov.tearsofink.item.ModItems.INK_ECHO));
		});
	}

	/** Наше оружие со ступенями и заточкой. */
	public static boolean ours(ItemStack stack) {
		return stack.getItem() instanceof InkGreatswordItem;
	}

	public static int tier(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		if (d == null) return 0;
		CompoundTag t = d.copyTag();
		int v = t.contains(TIER) ? t.getInt(TIER) : t.getInt(LEGACY_TIER);
		return Mth.clamp(v, 0, TIERS - 1);
	}

	public static int sharp(ItemStack stack) {
		int max = SHARP_MAX[tier(stack)];
		if (max <= 0) return 0;
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		if (d == null || !d.copyTag().contains(SHARP)) return max;
		return Mth.clamp(d.copyTag().getInt(SHARP), 0, max);
	}

	public static float sharpFraction(ItemStack stack) {
		int max = SHARP_MAX[tier(stack)];
		return max <= 0 ? 1f : sharp(stack) / (float) max;
	}

	/** Поставить ступень; заточка — полная. */
	public static void setTier(ItemStack stack, int tier) {
		int t = Mth.clamp(tier, 0, TIERS - 1);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.remove(LEGACY_TIER);
			tag.putInt(TIER, t);
			tag.putInt(SHARP, SHARP_MAX[t]);
			tag.remove(WARNED);
		});
	}

	public static void setSharp(ItemStack stack, int value) {
		int max = SHARP_MAX[tier(stack)];
		int v = Mth.clamp(value, 0, max);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putInt(SHARP, v);
			if (v > max * WARN_AT) tag.remove(WARNED);
		});
	}

	/** Заточить: pigments Пигментов. Вернёт, изменилось ли что-то. */
	public static boolean sharpen(ItemStack stack, int pigments) {
		int max = SHARP_MAX[tier(stack)];
		if (max <= 0 || pigments <= 0 || sharp(stack) >= max) return false;
		setSharp(stack, sharp(stack) + Math.round(max * SHARPEN_PER_PIGMENT * pigments));
		return true;
	}

	/** Износ (только игроки, только в Землях). Ниже отметки — деградация на ступень, свойства засыпают. */
	public static void wear(LivingEntity user, ItemStack stack, int amount) {
		if (!(user instanceof ServerPlayer p) || !ModDimensions.isLands(p.level()) || !ours(stack) || p.isCreative()) return;
		int tier = tier(stack), max = SHARP_MAX[tier];
		if (max <= 0) return;
		int left = sharp(stack) - amount;
		if (left < max * DEGRADE_AT) {
			setTier(stack, tier - 1);
			p.displayClientMessage(Component.translatable("gear.tears-of-ink.degraded", stack.getHoverName(),
					Component.translatable("gear.tears-of-ink.tier." + (tier - 1))).withStyle(ChatFormatting.DARK_RED), false);
			p.level().playSound(null, p.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);
			return;
		}
		setSharp(stack, left);
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		if (left <= max * WARN_AT && (d == null || !d.copyTag().getBoolean(WARNED))) {
			CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(WARNED, true));
			p.displayClientMessage(Component.translatable("gear.tears-of-ink.dull", stack.getHoverName()).withStyle(ChatFormatting.GOLD), true);
		}
	}

	/** Namespace, not tier/class, determines the birthplace of equipment. */
	public static boolean isModItem(ItemStack stack) {
		return stack != null && !stack.isEmpty()
				&& TearsOfInk.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
	}

	/** Wave uses the existing playerAttack DamageSource for blocking and kill attribution.
	 * Preserve its provenance even if the caster switches hands before impact. */
	private static final ThreadLocal<DamageSource> MOD_WAVE = new ThreadLocal<>();

	public static boolean hurtModWave(LivingEntity target, DamageSource source, float damage) {
		DamageSource previous = MOD_WAVE.get();
		MOD_WAVE.set(source);
		try {
			return target.hurt(source, damage);
		} finally {
			if (previous == null) MOD_WAVE.remove(); else MOD_WAVE.set(previous);
		}
	}

	// Keep recursion context in an ordinary Java class, not a transformed mixin inner class.
	private record HitFrame(LivingEntity target, DamageSource source, HitFrame parent) {}
	private static final ThreadLocal<HitFrame> ACTIVE_HIT = new ThreadLocal<>();

	@FunctionalInterface
	public interface AdjustedHit {
		boolean hurt(float amount);
	}

	public static boolean withAdjustedDamage(LivingEntity target, DamageSource source, float amount, AdjustedHit original) {
		HitFrame previous = ACTIVE_HIT.get();
		for (HitFrame frame = previous; frame != null; frame = frame.parent()) {
			if (frame.target() == target && frame.source() == source) return original.hurt(amount);
		}
		ACTIVE_HIT.set(new HitFrame(target, source, previous));
		try {
			return original.hurt(outgoing(target, source, amount));
		} finally {
			if (previous == null) ACTIVE_HIT.remove(); else ACTIVE_HIT.set(previous);
		}
	}

	/** Apply affinity exactly once at the damage boundary; perks still operate only in Lands. */
	public static float outgoing(LivingEntity target, DamageSource source, float amount) {
		if (!(source.getEntity() instanceof Player user)) return amount;
		boolean lands = ModDimensions.isLands(target.level());
		Entity direct = source.getDirectEntity();
		ItemStack held = com.tarkrakov.tearsofink.weapon.combat.PaleLongswordMoves.damageWeapon(user,
				com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.damageWeapon(user));

		// Bleeding has its own damage type and may outlive the original weapon or dimension.
		if (source.is(com.tarkrakov.tearsofink.weapon.blood.Bleeding.DAMAGE)) {
			return amount * (float) EquipmentAffinity.multiplier(lands, true);
		}

		boolean wave = MOD_WAVE.get() == source;
		boolean melee = direct == user && source.is(DamageTypes.PLAYER_ATTACK);
		ItemStack weapon = held;
		boolean modWeapon;
		if (wave) {
			modWeapon = true;
		} else if (melee) {
			if (!meleeWeapon(held)) return amount; // bare hand, food, blocks, unrelated damage
			modWeapon = isModItem(held);
		} else if (direct != null && direct != user) {
			// In 1.21.1 arrows/tridents retain the firing weapon, unlike the owner's current hand.
			ItemStack fired = direct.getWeaponItem();
			boolean modEntity = TearsOfInk.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType()).getNamespace());
			if (fired != null && !fired.isEmpty()) {
				weapon = fired;
				modWeapon = isModItem(fired);
			} else if (direct instanceof AbstractArrow || modEntity
					|| direct instanceof FireworkRocketEntity rocket && rocket.isShotAtAngle()
					|| source.is(DamageTypes.WIND_CHARGE)) {
				weapon = ItemStack.EMPTY;
				modWeapon = modEntity;
			} else {
				return amount; // no known weapon provenance; do not penalize potions or unrelated damage
			}
		} else {
			return amount;
		}
		float out = amount * (float) EquipmentAffinity.multiplier(lands, modWeapon);
		// Keep the original Lands-only tier/awakening rule; no NPC rank multiplication.
		if (lands && modWeapon && ours(weapon)) {
			out *= TIER_MULT[tier(weapon)];
			if (user instanceof ServerPlayer sp) {
				out *= Awakening.damageMult(sp, weapon, target);
				Awakening.afterDamage(sp, weapon, out);
			}
		}
		return out;
	}

	private static boolean meleeWeapon(ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (stack.getItem() instanceof TieredItem || stack.getItem() instanceof MaceItem
				|| stack.getItem() instanceof TridentItem) return true;
		// Supports the Soul shields and other mods using standard attack attributes.
		var modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		if (modifiers.modifiers().isEmpty()) modifiers = stack.getItem().getDefaultAttributeModifiers();
		boolean[] attack = {false};
		modifiers.forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
			if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.amount() > 0) attack[0] = true;
		});
		return attack[0];
	}

	/**
	 * Одна награда за УСПЕШНЫЙ шаг связки, а не за каждую цель в секторе:
	 * обычное оружие создаёт Чернила; охотничье и прочее оружие не создаёт их попаданиями.
	 * Вызывается после всех целей только при hitAny, на сервере для обоих обетов.
	 */
	public static void onSuccessfulCombo(ServerPlayer player, ItemStack weapon) {
		if (!(weapon.getItem() instanceof OrdinaryWeaponItem)) return;
		var ink = com.tarkrakov.tearsofink.ink.InkPowers.get(player);
		if (!ink.inside() || ink.ink() >= ink.inkMax()) return;
		float gain = ORDINARY_COMBO_INK;
		float absorb = Awakening.power(weapon, Awakening.ABSORB);
		// «Впитывание» также бросает кубик один раз за удар, даже если задеты несколько существ.
		if (absorb > 0f && player.getRandom().nextFloat() < absorb) gain += 1f;
		com.tarkrakov.tearsofink.ink.InkPowers.update(player, ink.withInk(ink.ink() + gain));
	}

	/** Удар по одной цели: износ, кровотечение и замедление; Чернила начисляются отдельно за взмах. */
	public static void onWeaponHit(LivingEntity attacker, ItemStack weapon, LivingEntity target) {
		if (!ours(weapon)) return;
		wear(attacker, weapon, WEAR_HIT);
		if (attacker instanceof ServerPlayer sp) Awakening.onComboHit(sp, weapon, target);
		if (weapon.getItem() instanceof OrdinaryWeaponItem o) {
			switch (o.line()) {
				case BLOOD -> {
					if (attacker.getRandom().nextFloat() < ORDINARY_BLEED_CHANCE)
						com.tarkrakov.tearsofink.weapon.blood.Bleeding.apply(target, attacker, 1);
				}
				case INK -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, INK_SLOW_TICKS, INK_SLOW_AMP), attacker);
				default -> {}
			}
		}
	}
}
