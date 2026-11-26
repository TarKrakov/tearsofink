package com.tarkrakov.tearsofink.weapon.ranged;

import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.weapon.AbilityWeapon;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;

/** Prototype ranged weapons are not melee archetypes. */
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
    @Override public boolean isBarVisible(ItemStack stack) { return kind == Kind.DAGGER; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13f * RangedSupply.amount(stack) / RangedSupply.capacity(stack)); }
    @Override public int getBarColor(ItemStack stack) { return 0x9b3545; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (kind == Kind.DAGGER) {
            tooltip.add(Component.translatable("ranged.tears-of-ink.charges", RangedSupply.amount(stack), RangedSupply.capacity(stack)));
            tooltip.add(Component.translatable("ranged.tears-of-ink.knife_damage", KnifeTuning.DAMAGE));
            tooltip.add(Component.translatable("ranged.tears-of-ink.knife_bleed", Math.round(KnifeTuning.BLEED_CHANCE*100), KnifeTuning.COST, RangedCombat.KNIFE_DURATION/20f));
            tooltip.add(Component.translatable("ranged.tears-of-ink.knife_blood", Math.round(KnifeTuning.BLOOD_BLEED_CHANCE*100), KnifeTuning.BLOOD_COST, KnifeTuning.BLOOD_COOLDOWN/20f));
            tooltip.add(Component.translatable("ranged.tears-of-ink.knife_refill", KnifeTuning.REFILL_PER_INGOT));
        }
        else tooltip.add(Component.translatable(RangedCombat.loaded(stack) ? "ranged.tears-of-ink.loaded" : "ranged.tears-of-ink.unloaded"));
        tooltip.add(Component.translatable(ability().nameKey()));
    }
}
