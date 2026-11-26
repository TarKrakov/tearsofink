package com.tarkrakov.tearsofink.weapon.ranged;

import com.tarkrakov.tearsofink.weapon.forge.Awakening;
import com.tarkrakov.tearsofink.weapon.forge.Gear;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Immutable launch snapshot. A swapped loadout cannot change an in-flight bolt.
 * Conditions are evaluated at impact, with the existing +50% awakening cap.
 */
record RangedShotPerks(float tier, float finisher, float fury, float wounds, float edge,
                      float thirst, float bleed, float precision) {
    static final RangedShotPerks NONE=new RangedShotPerks(1,0,0,0,0,0,0,0);
    static RangedShotPerks capture(ItemStack stack,boolean special){
        return new RangedShotPerks(Gear.TIER_MULT[Gear.tier(stack)],
            Awakening.power(stack,Awakening.FINISHER),Awakening.power(stack,Awakening.FURY),
            Awakening.power(stack,Awakening.DEEP_WOUNDS),special?Awakening.power(stack,Awakening.EDGE):0,
            Awakening.power(stack,Awakening.THIRST),Awakening.power(stack,Awakening.BLOODLETTING),
            Awakening.power(stack,Awakening.PRECISION));
    }
    float damage(ServerPlayer owner,LivingEntity target,float base){
        float bonus=edge;
        if(target.getHealth()<target.getMaxHealth()*.5f)bonus+=finisher;
        if(owner.getHealth()<owner.getMaxHealth()*.4f)bonus+=fury;
        if(target.hasEffect(com.tarkrakov.tearsofink.effect.ModEffects.BLEEDING))bonus+=wounds;
        return base*tier*(1+Math.min(bonus,Awakening.MAX_DAMAGE_BONUS));
    }
    void afterHit(ServerPlayer p,float actualLoss){
        if(thirst>0&&actualLoss>0&&p.isAlive())p.heal(actualLoss*thirst);
    }
}
