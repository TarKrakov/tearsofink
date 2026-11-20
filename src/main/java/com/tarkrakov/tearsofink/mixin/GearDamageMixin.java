package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tarkrakov.tearsofink.weapon.forge.Gear;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** One equipment damage adjustment per hit, including reentrant defensive stances. */
@Mixin(LivingEntity.class)
public abstract class GearDamageMixin {
    @WrapMethod(method = "hurt")
    private boolean tearsofink$gearDamage(DamageSource source, float amount, Operation<Boolean> original) {
        return Gear.withAdjustedDamage((LivingEntity) (Object) this, source, amount,
                adjusted -> original.call(source, adjusted));
    }
}
