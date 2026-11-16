package com.tarkrakov.tearsofink.weapon.forge;

import com.tarkrakov.tearsofink.hunt.HunterType;
import com.tarkrakov.tearsofink.hunt.HunterWeaponItem;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.weapon.ArsenalWeaponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Пробуждения — свойства оружия с Проклятого алтаря. Слоты по ступени (Тусклое 1, Контрастное 2, Рубрики 3; обычное — 1),
 * при деградации верхние слоты засыпают. Своя линия — полная сила, чужая — CROSS_LINE (Кровь — оружию Крови; Чернила — Шпилю и Чернилам).
 * Сила по уровням I / II / III — массивы ниже (крутить здесь).
 */
public enum Awakening {
	// ===== Кровь =====
	/** Шанс кровотечения от удара связки. */
	BLOODLETTING("bloodletting", true, 0.08f, 0.14f, 0.20f),
	/** Доля урона возвращается здоровьем. */
	THIRST("thirst", true, 0.03f, 0.05f, 0.08f),
	/** Урон по врагу ниже 50% здоровья. */
	FINISHER("finisher", true, 0.10f, 0.18f, 0.25f),
	/** Урон, пока у себя меньше 40% здоровья. */
	FURY("fury", true, 0.12f, 0.20f, 0.30f),
	/** Урон по кровоточащему. */
	DEEP_WOUNDS("deep_wounds", true, 0.06f, 0.10f, 0.15f),
	// ===== Чернила =====
	/** Приёмы дешевле (Чернила и здоровье). */
	THRIFT("thrift", false, 0.10f, 0.18f, 0.25f),
	/** Перезарядка приёмов короче. */
	FLOW("flow", false, 0.08f, 0.14f, 0.20f),
	/** Шанс +1 Чернил за удар связки. */
	ABSORB("absorb", false, 0.25f, 0.45f, 0.70f),
	/** Урон приёмов. */
	EDGE("edge", false, 0.10f, 0.18f, 0.25f),
	/** Замедление от удара связки (тики). */
	VISCOUS("viscous", false, 15, 25, 35);

	// ===== Общие настройки =====
	/** Свойство чужой линии работает на 60%. */
	public static final float CROSS_LINE = 0.6f;
	/** Пробуждение: «ничего» 20%, иначе I — 55 из 80, II — 25 из 80. III — только усилением (Кристалл крови / Чернильный отголосок). */
	public static final float NOTHING_CHANCE = 0.20f, LEVEL_TWO_CHANCE = 0.3125f;
	/** Цена: 4 реагента, +2 за каждое прошлое пробуждение этого оружия, не больше 16. */
	public static final int BASE_COST = 4, COST_STEP = 2, MAX_COST = 16;
	/** «Разящий приём»: сколько тиков после приёма его урон считается уроном приёма. */
	public static final int EDGE_WINDOW = 60;

	public final String id;
	public final boolean blood;
	private final float[] power;

	Awakening(String id, boolean blood, float... power) {
		this.id = id;
		this.blood = blood;
		this.power = power;
	}

	public static final int MAX_LEVEL = 3;
	private static final String KEY = "Awake", ROLLS = "AwakeRolls";
	private static final Map<UUID, Long> LAST_ABILITY = new HashMap<>();
	/** Удар связки идёт прямо сейчас (Combat ставит на время t.hurt) — не считать его приёмом. */
	public static boolean comboHit;

	@Nullable
	public static Awakening byId(String id) {
		for (Awakening a : values()) if (a.id.equals(id)) return a;
		return null;
	}

	public static Awakening random(boolean blood, RandomSource r) {
		List<Awakening> pool = new ArrayList<>();
		for (Awakening a : values()) if (a.blood == blood) pool.add(a);
		return pool.get(r.nextInt(pool.size()));
	}

	// ===== Слоты и хранение =====
	public record Slot(Awakening awakening, int level) {}

	/** Сколько слотов даёт ступень (обычное — 1 всегда). */
	public static int slots(ItemStack stack) {
		if (stack.getItem() instanceof OrdinaryWeaponItem) return 1;
		return Gear.tier(stack) + 1;
	}

	/** Все записанные слоты (включая спящие); пустой — null. */
	public static List<Slot> read(ItemStack stack) {
		List<Slot> out = new ArrayList<>();
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		if (d == null) return out;
		ListTag list = d.copyTag().getList(KEY, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i);
			Awakening a = byId(t.getString("id"));
			out.add(a == null ? null : new Slot(a, Math.max(1, Math.min(MAX_LEVEL, t.getInt("lvl")))));
		}
		return out;
	}

	@Nullable
	public static Slot get(ItemStack stack, int index) {
		List<Slot> all = read(stack);
		return index < all.size() ? all.get(index) : null;
	}

	public static void set(ItemStack stack, int index, @Nullable Slot slot) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			ListTag list = tag.getList(KEY, Tag.TAG_COMPOUND);
			while (list.size() <= index) list.add(new CompoundTag());
			CompoundTag t = new CompoundTag();
			if (slot != null) {
				t.putString("id", slot.awakening().id);
				t.putInt("lvl", slot.level());
			}
			list.set(index, t);
			tag.put(KEY, list);
		});
	}

	public static int rolls(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d == null ? 0 : d.copyTag().getInt(ROLLS);
	}

	public static void addRoll(ItemStack stack) {
		int n = rolls(stack) + 1;
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(ROLLS, n));
	}

	public static int cost(ItemStack stack) {
		return Math.min(MAX_COST, BASE_COST + COST_STEP * rolls(stack));
	}

	/** Линия оружия «кровь» (иначе — Чернила: Шпиль и Чернила). */
	public static boolean bloodWeapon(ItemStack stack) {
		if (stack.getItem() instanceof ArsenalWeaponItem a) return a.blood();
		if (stack.getItem() instanceof OrdinaryWeaponItem o) return o.line() == HunterType.BLOOD;
		if (stack.getItem() instanceof HunterWeaponItem) return HunterWeaponItem.line(stack) == HunterType.BLOOD;
		return false;
	}

	/** Сила свойства на этом оружии (уровень × своя/чужая линия); 0 — нет или спит. */
	public static float power(@Nullable ItemStack stack, Awakening a) {
		if (stack == null || !Gear.ours(stack)) return 0f;
		List<Slot> all = read(stack);
		int active = Math.min(all.size(), slots(stack));
		float best = 0f;
		for (int i = 0; i < active; i++) {
			Slot s = all.get(i);
			if (s == null || s.awakening() != a) continue;
			float v = a.power[s.level() - 1] * (a.blood == bloodWeapon(stack) ? 1f : CROSS_LINE);
			best = Math.max(best, v);
		}
		return best;
	}

	// ===== Действие =====
	public static float costMult(@Nullable ItemStack weapon) {
		return 1f - power(weapon, THRIFT);
	}

	public static float cooldownMult(@Nullable ItemStack weapon) {
		return 1f - power(weapon, FLOW);
	}

	public static void abilityUsed(ServerPlayer p) {
		LAST_ABILITY.put(p.getUUID(), p.serverLevel().getGameTime());
	}

	/** Множитель урона от свойств (только игроки). */
	public static float damageMult(ServerPlayer p, ItemStack weapon, LivingEntity target) {
		float m = 1f;
		if (target.getHealth() < target.getMaxHealth() * 0.5f) m += power(weapon, FINISHER);
		if (p.getHealth() < p.getMaxHealth() * 0.4f) m += power(weapon, FURY);
		if (target.hasEffect(com.tarkrakov.tearsofink.effect.ModEffects.BLEEDING)) m += power(weapon, DEEP_WOUNDS);
		Long last = LAST_ABILITY.get(p.getUUID());
		if (!comboHit && last != null && p.serverLevel().getGameTime() - last <= EDGE_WINDOW) m += power(weapon, EDGE);
		return m;
	}

	/** После расчёта урона: Жажда лечит. */
	public static void afterDamage(ServerPlayer p, ItemStack weapon, float damage) {
		float steal = power(weapon, THIRST);
		if (steal > 0 && damage > 0) p.heal(damage * steal);
	}

	/** Удар связки: Кровопускание, Впитывание, Вязкость. */
	public static void onComboHit(ServerPlayer p, ItemStack weapon, LivingEntity target) {
		float bleed = power(weapon, BLOODLETTING);
		if (bleed > 0 && p.getRandom().nextFloat() < bleed) com.tarkrakov.tearsofink.weapon.blood.Bleeding.apply(target, p, 1);
		float absorb = power(weapon, ABSORB);
		if (absorb > 0 && p.getRandom().nextFloat() < absorb) {
			var d = InkPowers.get(p);
			InkPowers.update(p, d.withInk(d.ink() + 1));
		}
		float slow = power(weapon, VISCOUS);
		if (slow > 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(slow), 0), p);
	}

	// ===== Описание =====
	public static void tooltip(ItemStack stack, List<Component> out) {
		List<Slot> all = read(stack);
		int active = slots(stack);
		for (int i = 0; i < Math.max(all.size(), active); i++) {
			Slot s = i < all.size() ? all.get(i) : null;
			if (s == null) {
				if (i < active) out.add(Component.translatable("awakening.tears-of-ink.empty").withStyle(ChatFormatting.DARK_GRAY));
				continue;
			}
			Component name = Component.translatable("awakening.tears-of-ink." + s.awakening().id)
					.append(" ").append(Component.translatable("awakening.tears-of-ink.level." + s.level()));
			if (i >= active) {
				out.add(Component.translatable("awakening.tears-of-ink.asleep", name).withStyle(ChatFormatting.DARK_GRAY));
			} else {
				boolean own = s.awakening().blood == bloodWeapon(stack);
				out.add(Component.translatable(own ? "awakening.tears-of-ink.line" : "awakening.tears-of-ink.line_foreign", name)
						.withStyle(s.awakening().blood ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE));
			}
		}
	}

	/** Реагент пробуждения: сгустки крови — Кровь, чернила — Чернила; иначе null. */
	@Nullable
	public static Boolean reagentIsBlood(ItemStack reagent) {
		if (reagent.is(ModItems.BLOOD_CLOT)) return true;
		if (reagent.is(ModItems.INK)) return false;
		return null;
	}

	/** Усилитель: Кристалл крови — свойства Крови, Чернильный отголосок — Чернил. */
	@Nullable
	public static Boolean catalystIsBlood(ItemStack catalyst) {
		if (catalyst.is(ModItems.BLOOD_CRYSTAL)) return true;
		if (catalyst.is(ModItems.INK_ECHO)) return false;
		return null;
	}
}
