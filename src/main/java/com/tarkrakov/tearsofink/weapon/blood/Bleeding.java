package com.tarkrakov.tearsofink.weapon.blood;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.effect.ModEffects;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.weapon.ArsenalWeaponItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Кровотечение: наложить, усилить, тикать. Кто наложил — запоминаем (ему идёт возврат здоровья и заслуга в Охоте). */
public final class Bleeding {
	public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, TearsOfInk.id("bleeding"));
	private static final Map<UUID, UUID> OWNER = new HashMap<>();

	private Bleeding() {}

	/** Наложить или усилить на stacks ступеней (до III), обновить длительность. */
	public static void apply(LivingEntity target, @Nullable Entity owner, int stacks) {
		if (!(target.level() instanceof ServerLevel level) || !target.isAlive() || stacks <= 0) return;
		MobEffectInstance cur = target.getEffect(ModEffects.BLEEDING);
		int amp = Math.min(BloodTuning.BLEED_MAX - 1, (cur == null ? -1 : cur.getAmplifier()) + stacks);
		target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, BloodTuning.BLEED_TICKS, amp, false, true, true));
		if (owner != null) OWNER.put(target.getUUID(), owner.getUUID());
		level.sendParticles(ModParticles.BLOOD_DRIP_FALL, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
				6 + amp * 4, target.getBbWidth() * 0.4, target.getBbHeight() * 0.25, target.getBbWidth() * 0.4, 0);
	}

	/** Обычный удар оружием Крови — с шансом кровотечение. */
	public static void onWeaponHit(LivingEntity attacker, ItemStack weapon, LivingEntity target) {
		if (!(weapon.getItem() instanceof ArsenalWeaponItem w) || !w.blood()) return;
		if (attacker.getRandom().nextFloat() < BloodTuning.BLEED_CHANCE) apply(target, attacker, 1);
	}

	static void tick(LivingEntity e, int amplifier) {
		if (!(e.level() instanceof ServerLevel level)) return;
		float dmg = BloodTuning.BLEED_DAMAGE[Math.max(0, Math.min(amplifier, BloodTuning.BLEED_DAMAGE.length - 1))];
		UUID ownerId = OWNER.get(e.getUUID());
		Entity owner = ownerId == null ? null : level.getEntity(ownerId);
		float before = e.getHealth();
		e.hurt(level.damageSources().source(DAMAGE, null, owner), dmg);
		float dealt = Math.max(0, before - e.getHealth());
		if (owner instanceof LivingEntity o && o.isAlive() && dealt > 0) {
			// Кровь — чужая сила: высокая Душа её лечение не усиливает (штраф проданной души — остаётся)
			float m = com.tarkrakov.tearsofink.ink.SoulPenalty.multiplier(o);
			o.heal(dealt * BloodTuning.BLEED_LIFESTEAL / Math.max(1f, m));
		}
		level.sendParticles(ModParticles.BLOOD_DRIP_FALL, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
				3 + amplifier * 2, e.getBbWidth() * 0.35, e.getBbHeight() * 0.2, e.getBbWidth() * 0.35, 0);
		if (!e.isAlive() || e.getEffect(com.tarkrakov.tearsofink.effect.ModEffects.BLEEDING) == null) OWNER.remove(e.getUUID());
	}
}
