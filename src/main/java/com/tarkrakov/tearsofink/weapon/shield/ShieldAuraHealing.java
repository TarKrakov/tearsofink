package com.tarkrakov.tearsofink.weapon.shield;

import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.WeakHashMap;

/** Same-type auras share a per-recipient one-second healing allowance.
 * A stronger pulse may add the difference; different aura types remain independent. */
public final class ShieldAuraHealing {
    private record Pulse(long tick, float amount) {}
    private static final Map<LivingEntity, Pulse> MERCY = new WeakHashMap<>();
    private static final Map<LivingEntity, Pulse> HALO = new WeakHashMap<>();
    private ShieldAuraHealing() {}

    public static boolean heal(LivingEntity target, float amount, boolean halo) {
        Map<LivingEntity, Pulse> pulses = halo ? HALO : MERCY;
        long now = target.level().getGameTime();
        Pulse prior = pulses.get(target);
        boolean sameWindow = prior != null && now >= prior.tick() && now - prior.tick() < 20;
        float already = sameWindow ? prior.amount() : 0;
        if (amount <= already) return false;
        pulses.put(target, new Pulse(sameWindow ? prior.tick() : now, amount));
        target.heal(amount - already);
        return true;
    }
}
