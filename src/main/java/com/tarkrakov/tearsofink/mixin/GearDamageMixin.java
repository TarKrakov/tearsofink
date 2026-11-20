package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tarkrakov.tearsofink.weapon.forge.Gear;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** One equipment damage adjustment per hit; guard handlers may recurse with the same source and a reduced amount. */
@Mixin(LivingEntity.class)
public abstract class GearDamageMixin {
    @Unique
    private static final ThreadLocal<HitFrame> tearsofink$hit = new ThreadLocal<>();

    private record HitFrame(LivingEntity target, DamageSource source, HitFrame parent) {}

    @WrapMethod(method = "hurt")
    private boolean tearsofink$gearDamage(DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity target = (LivingEntity) (Object) this;
        HitFrame previous = tearsofink$hit.get();
        for (HitFrame frame = previous; frame != null; frame = frame.parent()) {
            if (frame.target() == target && frame.source() == source) {
                return original.call(source, amount);
            }
        }
        tearsofink$hit.set(new HitFrame(target, source, previous));
        try {
            return original.call(source, Gear.outgoing(target, source, amount));
        } finally {
            if (previous == null) tearsofink$hit.remove(); else tearsofink$hit.set(previous);
        }
    }
}
