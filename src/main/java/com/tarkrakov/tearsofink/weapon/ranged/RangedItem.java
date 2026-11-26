package com.tarkrakov.tearsofink.weapon.ranged;

import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.weapon.AbilityWeapon;
import net.minecraft.world.item.*;

/** Ranged weapons share Gear progression but never become melee archetypes. */
public final class RangedItem extends Item implements AbilityWeapon {
    public enum Kind { CROSSBOW, DAGGER, PISTOL }
    public final Kind kind;
    public RangedItem(Kind kind) { super(new Properties().stacksTo(1)); this.kind = kind; }
    public static Kind kind(ItemStack stack) { return stack.getItem() instanceof RangedItem w ? w.kind : null; }
    @Override public Skill ability() { return switch (kind) {
        case CROSSBOW -> RangedCombat.INK_SHOT;
        case DAGGER -> RangedCombat.BLOOD_SHOT;
        case PISTOL -> RangedCombat.WILD_SHOT;
    }; }
    @Override public boolean isBarVisible(ItemStack stack) {
        return kind==Kind.DAGGER || com.tarkrakov.tearsofink.weapon.forge.Gear.SHARP_MAX[com.tarkrakov.tearsofink.weapon.forge.Gear.tier(stack)]>0;
    }
    @Override public int getBarWidth(ItemStack stack) {
        return kind==Kind.DAGGER?Math.round(13f*RangedSupply.amount(stack)/RangedSupply.capacity(stack))
            :Math.round(13f*com.tarkrakov.tearsofink.weapon.forge.Gear.sharpFraction(stack));
    }
    @Override public int getBarColor(ItemStack stack) {
        if(kind==Kind.DAGGER)return 0x9b3545;
        float f=com.tarkrakov.tearsofink.weapon.forge.Gear.sharpFraction(stack);
        return net.minecraft.util.Mth.hsvToRgb(f/3f,1f,1f);
    }
}
