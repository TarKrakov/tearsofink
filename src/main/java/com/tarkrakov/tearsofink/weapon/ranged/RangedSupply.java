package com.tarkrakov.tearsofink.weapon.ranged;

import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import java.util.List;

/** Count is independent of durability; empty containers and daggers are refillable. */
public final class RangedSupply extends Item {
    private static final String COUNT = "toi_ranged_charges";
    public final int max;
    public RangedSupply(int max) { super(new Properties().stacksTo(1)); this.max=max; }
    public static int capacity(ItemStack s) {
        if (s.getItem() instanceof RangedSupply supply) return supply.max;
        return RangedItem.kind(s)==RangedItem.Kind.DAGGER ? 24 : 0;
    }
    public static int amount(ItemStack s) {
        int max=capacity(s);
        if (max==0) return 0;
        CustomData d=s.get(DataComponents.CUSTOM_DATA);
        if (d==null || !d.copyTag().contains(COUNT)) return RangedItem.kind(s)==RangedItem.Kind.DAGGER ? max : 0;
        return Math.clamp(d.copyTag().getInt(COUNT),0,max);
    }
    public static void set(ItemStack s,int n) {
        int clamped=Math.clamp(n,0,capacity(s));
        CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putInt(COUNT,clamped));
    }
    public static Item supply(RangedItem.Kind kind) {
        return kind==RangedItem.Kind.CROSSBOW ? ModItems.INK_QUIVER : ModItems.HOLY_BANDOLIER;
    }
    public static int available(Player p,RangedItem.Kind kind) {
        int total=0;
        for(int i=0;i<36;i++) { ItemStack s=p.getInventory().getItem(i); if(s.is(supply(kind))) total+=amount(s); }
        return total;
    }
    public static boolean take(Player p,RangedItem.Kind kind,int count) {
        if(p.getAbilities().instabuild) return true;
        if(available(p,kind)<count) return false;
        for(int i=0;i<36 && count>0;i++) {
            ItemStack s=p.getInventory().getItem(i);
            if(!s.is(supply(kind))) continue;
            int use=Math.min(count,amount(s));
            if(use>0){ set(s,amount(s)-use); count-=use; }
        }
        p.getInventory().setChanged();
        return true;
    }
    /** Anvil consumes one item per occupied grid cell, preserving the returned stack's components. */
    public static ItemStack refill(Container grid) {
        ItemStack receiver=ItemStack.EMPTY;
        Item material=null;
        int units=0;
        for(int i=0;i<grid.getContainerSize();i++) {
            ItemStack s=grid.getItem(i);
            if(s.isEmpty()) continue;
            if(capacity(s)>0) {
                if(!receiver.isEmpty() || s.getCount()!=1) return ItemStack.EMPTY;
                receiver=s;
            } else {
                if(material!=null && material!=s.getItem()) return ItemStack.EMPTY;
                material=s.getItem(); units++;
            }
        }
        if(receiver.isEmpty() || units==0 || amount(receiver)>=capacity(receiver)) return ItemStack.EMPTY;
        int rate=receiver.is(ModItems.INK_QUIVER) && material==ModItems.INK ? 16
            : receiver.is(ModItems.HOLY_BANDOLIER) && material==ModItems.PALE_BULLET ? 1
            : RangedItem.kind(receiver)==RangedItem.Kind.DAGGER && material==ModItems.BLOOD_INGOT ? 12 : 0;
        if(rate==0) return ItemStack.EMPTY;
        ItemStack out=receiver.copyWithCount(1);
        set(out,amount(out)+rate*units);
        return out;
    }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13f*amount(stack)/max); }
    @Override public int getBarColor(ItemStack stack) { return 0x9caa70; }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("ranged.tears-of-ink.charges",amount(stack),max));
    }
}
